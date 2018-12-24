package games.network;

import games.model.ActionType;
import games.model.Affiliation;
import games.model.Board;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class Server {

	private static final int MAX_PLAYERS = 2;
	private static final int CYCLE_PERIOD_MILLIS = 50;

	private ScheduledExecutorService timer = Executors.newScheduledThreadPool(1);

	private Board board;

	private DatagramSocket serverSocket;
	private DatagramSocket syncSocket;

	private int serverListenPort;

	private Map<InetAddress, Affiliation> addressToPlayer = new HashMap<>(MAX_PLAYERS);

	private Map<Affiliation, ActionType> currentActions = new EnumMap<>(Affiliation.class);
	private Map<Affiliation, ActionType> prevActions = new EnumMap<>(Affiliation.class);

	private Serializer serializer = new Serializer();

	private byte[] syncBuf = new byte[Protocol.SYNC_BUFFER_LENGHT];

	public Server() {
		this(Protocol.DEFAULT_SERVER_LISTEN_PORT);
	}

	public Server(int serverListenPort) {
		this.serverListenPort = serverListenPort;
		initServer();
		try {
			serverSocket = new DatagramSocket(serverListenPort);
			syncSocket = new DatagramSocket();
			listen();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void initServer() {
		board = new Board();
		//TODO change board setup
		board.setTestEnvironemt();
		timer.scheduleAtFixedRate(() -> {
			if(addressToPlayer.size() > 0) {
				try {
					cycleBoard();
					syncBoardToClients();
				} catch(Exception e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			}
		}, 0, CYCLE_PERIOD_MILLIS, TimeUnit.MILLISECONDS);
	}

	private void cycleBoard() {
		Map<Affiliation, ActionType> temp = prevActions;
		prevActions = currentActions;
		currentActions = temp;
		board.cycle(prevActions);
		prevActions.clear();
	}

	private void syncBoardToClients() {
		StringBuilder sb = serializer.serializeBoard(board);
		DatagramPacket syncPacket = new DatagramPacket(syncBuf, sb.length());
		Protocol.prepareMsg(syncPacket, Protocol.PacketType.SYNC, sb.toString());
		for(InetAddress clientAddress : addressToPlayer.keySet()) {
			try {
				syncPacket.setAddress(clientAddress);
				syncPacket.setPort(Protocol.DEFAULT_CLIENT_LISTEN_PORT);
				syncSocket.send(syncPacket);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void listen() throws IOException {
		byte[] receiveBuf = new byte[Protocol.MSG_BUFFER_LENGHT];
		DatagramPacket receivePacket = new DatagramPacket(receiveBuf, receiveBuf.length);
		byte[] sendBuf = new byte[Protocol.MSG_BUFFER_LENGHT];
		DatagramPacket responsePacket = new DatagramPacket(sendBuf, sendBuf.length);
		System.out.println("Server start listening");
		while(true) {
			serverSocket.receive(receivePacket);
			InetAddress clientAddress = receivePacket.getAddress();
			Protocol.PacketType packetType = Protocol.readPacketType(receivePacket);
			try {
				switch (packetType) {
					case JOIN:
						//TODO check game version
						if(addressToPlayer.containsKey(clientAddress)) {
							sendResponse(responsePacket, clientAddress, Protocol.PacketType.JOINED, addressToPlayer.get(clientAddress).getValue());
						} else if (addressToPlayer.size() < MAX_PLAYERS) {
							Affiliation newPlayer;
							if (!addressToPlayer.values().contains(Affiliation.PLAYER1)) {
								newPlayer = Affiliation.PLAYER1;
							} else if (!addressToPlayer.values().contains(Affiliation.PLAYER2)) {
								newPlayer = Affiliation.PLAYER2;
							} else {
								throw new RuntimeException("Less than two players but none free");
							}
							addressToPlayer.put(clientAddress, newPlayer);
							sendResponse(responsePacket, clientAddress, Protocol.PacketType.JOINED, newPlayer.getValue());
						} else {
							sendResponse(responsePacket, clientAddress, Protocol.PacketType.DENY, "Server is full");
						}
						break;
					case LEAVE:
						addressToPlayer.remove(receivePacket.getAddress());
						break;
					case ACTION:
						ActionType actionType = Protocol.readActionType(receivePacket);
						if(addressToPlayer.containsKey(clientAddress)) {
							currentActions.put(addressToPlayer.get(clientAddress), actionType);
						}
						break;
					case DENY:
					case SYNC:
					case JOINED:
					case BAD_PACKET:
						//do nothing, ignore bad packet
						break;
				}
			} catch (Exception e) {
				//bad or malicious packets should not break server
				e.printStackTrace();
			}
		}
	}

	private void sendResponse(DatagramPacket responsePacket, InetAddress clientAddress, Protocol.PacketType packetType, int message) throws IOException {
		sendResponse(responsePacket, clientAddress, packetType, Integer.toString(message));
	}

	private void sendResponse(DatagramPacket responsePacket, InetAddress clientAddress, Protocol.PacketType packetType, String message) throws IOException {
		responsePacket.setAddress(clientAddress);
		responsePacket.setPort(Protocol.DEFAULT_CLIENT_LISTEN_PORT);
		Protocol.prepareMsg(responsePacket, packetType, message);
		serverSocket.send(responsePacket);
	}
}
