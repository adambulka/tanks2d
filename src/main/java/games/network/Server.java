package games.network;

import games.model.ActionType;
import games.model.Affiliation;
import games.model.Board;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {

	private static final int MAX_PLAYERS = 2;
	private static final int CYCLE_PERIOD_MILLIS = 50;

	private ScheduledExecutorService timer = Executors.newScheduledThreadPool(1);

	private Board board;

	private DatagramSocket serverSocket;
	private DatagramSocket syncSocket;

	private int serverListenPort;

	private Map<InetSocketAddress, PlayerInfo> addressToPlayer = new HashMap<>(MAX_PLAYERS);
	private Set<Affiliation> assignedPlayers = new HashSet<>();

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
		for(PlayerInfo player : addressToPlayer.values()) {
			try {
				syncPacket.setAddress(player.getAddress());
				syncPacket.setPort(player.responseToPort);
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
			InetSocketAddress clientFromAddress = (InetSocketAddress) receivePacket.getSocketAddress();
			Protocol.PacketType packetType = Protocol.readPacketType(receivePacket);
			try {
				switch (packetType) {
					case JOIN:
						//TODO check game version
						int clientResponsePort = Protocol.readJoinPort(receivePacket);
						if(clientResponsePort <= 0) {
							//ignore bad port, cannot send response anyway
							break;
						}
						if(addressToPlayer.containsKey(clientFromAddress)) {
							PlayerInfo playerInfo = addressToPlayer.get(clientFromAddress);
							playerInfo.responseToPort = clientResponsePort;
							sendResponse(responsePacket, playerInfo.getAddress(), playerInfo.responseToPort, Protocol.PacketType.JOINED, addressToPlayer.get(clientFromAddress).affiliation.getValue());
						} else if (addressToPlayer.size() < MAX_PLAYERS) {
							Affiliation newPlayer;
							if (!assignedPlayers.contains(Affiliation.PLAYER1)) {
								newPlayer = Affiliation.PLAYER1;
							} else if (!assignedPlayers.contains(Affiliation.PLAYER2)) {
								newPlayer = Affiliation.PLAYER2;
							} else {
								throw new RuntimeException("Less than two players but none free");
							}
							assignedPlayers.add(newPlayer);
							PlayerInfo playerInfo = new PlayerInfo(clientFromAddress, clientResponsePort, newPlayer);
							addressToPlayer.put(clientFromAddress, playerInfo);
							sendResponse(responsePacket, playerInfo.getAddress(), playerInfo.responseToPort, Protocol.PacketType.JOINED, newPlayer.getValue());
						} else {
							sendResponse(responsePacket, clientFromAddress.getAddress(), clientResponsePort, Protocol.PacketType.DENY, "Server is full");
						}
						break;
					case LEAVE:
						if(addressToPlayer.containsKey((InetSocketAddress) receivePacket.getSocketAddress())) {
							PlayerInfo playerInfo = addressToPlayer.remove((InetSocketAddress) receivePacket.getSocketAddress());
							assignedPlayers.remove(playerInfo.affiliation);
						}
						break;
					case ACTION:
						if(addressToPlayer.containsKey(clientFromAddress)) {
							ActionType actionType = Protocol.readActionType(receivePacket);
							currentActions.put(addressToPlayer.get(clientFromAddress).affiliation, actionType);
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

	private void sendResponse(DatagramPacket responsePacket, InetAddress clientAddress, int clienPort, Protocol.PacketType packetType, int message) throws IOException {
		sendResponse(responsePacket, clientAddress, clienPort, packetType, Integer.toString(message));
	}

	private void sendResponse(DatagramPacket responsePacket, InetAddress clientAddress, int clienPort, Protocol.PacketType packetType, String message) throws IOException {
		responsePacket.setAddress(clientAddress);
		responsePacket.setPort(clienPort);
		Protocol.prepareMsg(responsePacket, packetType, message);
		serverSocket.send(responsePacket);
	}

	private static class PlayerInfo {
		InetSocketAddress clientFromAddress;
		int responseToPort;
		Affiliation affiliation;

		PlayerInfo(InetSocketAddress clientFromAddress, int responseToPort, Affiliation affiliation) {
			this.clientFromAddress = clientFromAddress;
			this.responseToPort = responseToPort;
			this.affiliation = affiliation;
		}

		InetAddress getAddress() {
			return clientFromAddress.getAddress();
		}
	}
}
