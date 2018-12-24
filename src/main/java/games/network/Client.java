package games.network;

import games.model.ActionType;
import games.model.Affiliation;
import games.model.Board;
import games.view.KeyboardController;
import games.view.Window;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Client {

	private Board currentBoard;
	private Board nextBoard;

	private DatagramSocket listenSocket;
	private DatagramSocket sendSocket;

	private InetAddress serverAddress;
	private int serverListenPort;
	private int clientListenPort;

	private Serializer serializer = new Serializer();

	private byte[] syncBuf = new byte[Protocol.SYNC_BUFFER_LENGHT];
	private byte[] sendBuf = new byte[Protocol.MSG_BUFFER_LENGHT];

	private ScheduledExecutorService sender = Executors.newScheduledThreadPool(1);

	private Affiliation player = Affiliation.NEUTRAL;

	private Window window;

	public Client(InetAddress serverAddress) {
		this(serverAddress, Protocol.DEFAULT_SERVER_LISTEN_PORT, Protocol.DEFAULT_CLIENT_LISTEN_PORT);
	}

	public Client(InetAddress serverAddress, int serverListenPort, int clientListenPort) {
		this.serverAddress = serverAddress;
		this.serverListenPort = serverListenPort;
		this.clientListenPort = clientListenPort;
		try {
			currentBoard = new Board();
			nextBoard = new Board();

			listenSocket = new DatagramSocket(clientListenPort);
			sendSocket = new DatagramSocket();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void start() {
		try {
			sender.schedule(() -> send(Protocol.PacketType.JOIN, clientListenPort), 1, TimeUnit.SECONDS);
			listen();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setWindow(Window window) {
		this.window = window;
	}

	public Board getCurrentBoard() {
		return currentBoard;
	}

	public void performAction(ActionType actionType) {
		sender.schedule(() -> send(Protocol.PacketType.ACTION, actionType.getValue()), 0, TimeUnit.MILLISECONDS);
	}

	public void leave() {
		sender.schedule(() -> {
			send(Protocol.PacketType.LEAVE);
			window.dispose();
			System.exit(0);
		}, 0, TimeUnit.MILLISECONDS);
	}

	private void send(Protocol.PacketType protocol) {
		send(protocol, "");
	}

	private void send(Protocol.PacketType packetType, int message) {
		send(packetType, Integer.toString(message));
	}

	private void send(Protocol.PacketType packetType, String message) {
		DatagramPacket packet = new DatagramPacket(sendBuf, sendBuf.length, serverAddress, serverListenPort);
		Protocol.prepareMsg(packet, packetType, message);
		try {
			sendSocket.send(packet);
		} catch (IOException e) {
			throw new RuntimeException("Could not send packet", e);
		}
	}

	private void listen() throws IOException {
		DatagramPacket receivePacket = new DatagramPacket(syncBuf, syncBuf.length);
		while(true) {
			listenSocket.receive(receivePacket);
			Protocol.PacketType packetType = Protocol.readPacketType(receivePacket);
			try {
				switch (packetType) {
					case SYNC:
						//System.out.println("Received sync package");
						if(!Affiliation.NEUTRAL.equals(player)) {
							serializer.deserializeToBoard(receivePacket.getData(), Protocol.PacketType.SYNC.getBeginText().length(), nextBoard);
							if (nextBoard.getCurrentCycle() >= currentBoard.getCurrentCycle()) {
								Board temp = nextBoard;
								nextBoard = currentBoard;
								currentBoard = temp;
								window.drawBoard(currentBoard);
							}
							nextBoard.clearBoard();
						}
						break;
					case JOINED:
						player = Protocol.readJoinedPlayer(receivePacket);
						KeyboardController keyboardController = new KeyboardController(this, player);
						window.setKeyboardController(keyboardController);
						break;
					case DENY:
						window.showJoinFailedMessage(Protocol.readDenyMessage(receivePacket));
						leave();
						break;
					case LEAVE:
					case JOIN:
					case ACTION:
					case BAD_PACKET:
						//do nothing, ignore bad packet
						break;
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
	}
}
