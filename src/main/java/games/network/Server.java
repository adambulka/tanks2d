package games.network;

import games.model.ActionType;
import games.model.Affiliation;
import games.model.Board;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class Server {

	private static final int SERVER_LISTEN_PORT = 8085;
	private static final int CLIENT_LISTEN_PORT = 8088;
	private static final int BUFFER_LENGHT = 1000;
	private static final int MAX_PLAYERS = 2;
	private static final int CYCLE_PERIOD_MILLIS = 50;

	private ScheduledExecutorService timer = Executors.newScheduledThreadPool(1);

	private Board board;

	private DatagramSocket serverSocket;
	private DatagramSocket syncSocket;

	private Map<InetAddress, Affiliation> addressToPlayer = new HashMap<>(MAX_PLAYERS);

	private Map<Affiliation, ActionType> currentActions = new HashMap<>(MAX_PLAYERS);
	private Map<Affiliation, ActionType> prevActions = new HashMap<>(MAX_PLAYERS);

	private Serializer serializer = new Serializer();

	private byte[] syncbuf = new byte[10000];

	public Server() {
		initServer();
		try {
			serverSocket = new DatagramSocket(SERVER_LISTEN_PORT);
			syncSocket = new DatagramSocket();
			listen();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public Board getBoard() {
		return board;
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
		sb.insert(0, Protocol.SYNC.getBeginText());
		writeToBuffer(syncbuf, sb);
		for(InetAddress clientAddress : addressToPlayer.keySet()) {
			try {
				DatagramPacket syncPacket = new DatagramPacket(syncbuf, sb.length(), clientAddress, CLIENT_LISTEN_PORT);
				syncSocket.send(syncPacket);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void writeToBuffer(byte[] buf, StringBuilder sb) {
		for(int i = 0; i < sb.length(); i++) {
			buf[i] = (byte) sb.charAt(i);
		}
	}

	private void writeToBuffer(byte[] buf, String s) {
		for(int i = 0; i < s.length(); i++) {
			buf[i] = (byte) s.charAt(i);
		}
	}

	private void listen() throws IOException {
		byte[] receiveBuf = new byte[BUFFER_LENGHT];
		DatagramPacket receivePacket = new DatagramPacket(receiveBuf, receiveBuf.length);
		byte[] sendBuf = new byte[BUFFER_LENGHT];
		DatagramPacket responsePacket = new DatagramPacket(sendBuf, sendBuf.length);
		System.out.println("Server start listening");
		while(true) {
			serverSocket.receive(receivePacket);
			InetAddress clientAddress = receivePacket.getAddress();
			Protocol protocol = Protocol.readPacketType(receivePacket);
			try {
				switch (protocol) {
					case JOIN:
						//TODO check game version
						if(addressToPlayer.containsKey(clientAddress)) {
							sendResponse(responsePacket, clientAddress, Protocol.JOINED, addressToPlayer.get(clientAddress).getValue());
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
							sendResponse(responsePacket, clientAddress, Protocol.JOINED, newPlayer.getValue());
						} else {
							sendResponse(responsePacket, clientAddress, Protocol.DENY, "Server is full");
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

	private void sendResponse(DatagramPacket responsePacket, InetAddress clientAddress, Protocol protocol, int message) throws IOException {
		sendResponse(responsePacket, clientAddress, protocol, Integer.toString(message));
	}

	private void sendResponse(DatagramPacket responsePacket, InetAddress clientAddress, Protocol protocol, String message) throws IOException {
		responsePacket.setAddress(clientAddress);
		responsePacket.setPort(CLIENT_LISTEN_PORT);
		Protocol.prepareMsg(responsePacket, protocol, message);
		serverSocket.send(responsePacket);
	}
}
