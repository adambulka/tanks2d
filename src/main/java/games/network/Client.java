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

	private static final int SERVER_LISTEN_PORT = 8085;
	private static final int CLIENT_LISTEN_PORT = 8088;
	private static final int BUFFER_LENGHT = 1000;

	private Board currentBoard;
	private Board nextBoard;

	private DatagramSocket listenSocket;
	private DatagramSocket sendSocket;

	private InetAddress serverAddress;

	private Serializer serializer = new Serializer();

	private byte[] syncbuf = new byte[10000];

	private ScheduledExecutorService sender = Executors.newScheduledThreadPool(1);

	private Affiliation player = Affiliation.NEUTRAL;

	private Window window;

	public Client(InetAddress serverAddress) {
		this.serverAddress = serverAddress;
		try {
			currentBoard = new Board();
			nextBoard = new Board();

			listenSocket = new DatagramSocket(CLIENT_LISTEN_PORT);
			sendSocket = new DatagramSocket();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void start() {
		try {
			sender.schedule(() -> send(Protocol.JOIN), 1, TimeUnit.SECONDS);
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
		sender.schedule(() -> send(Protocol.ACTION, actionType.getValue()), 0, TimeUnit.MILLISECONDS);
	}

	public void leave() {
		sender.schedule(() -> {
			send(Protocol.LEAVE);
			window.dispose();
			System.exit(0);
		}, 0, TimeUnit.MILLISECONDS);
	}

	private void send(Protocol protocol) {
		send(protocol, "");
	}

	private void send(Protocol protocol, int message) {
		send(protocol, Integer.toString(message));
	}

	private void send(Protocol protocol, String message) {
		byte[] sendBuf = new byte[BUFFER_LENGHT];
		DatagramPacket packet = new DatagramPacket(sendBuf, sendBuf.length, serverAddress, SERVER_LISTEN_PORT);
		Protocol.prepareMsg(packet, protocol, message);
		try {
			sendSocket.send(packet);
		} catch (IOException e) {
			throw new RuntimeException("Could not send packet", e);
		}
	}

	private void listen() throws IOException {
		DatagramPacket receivePacket = new DatagramPacket(syncbuf, syncbuf.length);
		while(true) {
			listenSocket.receive(receivePacket);
			Protocol protocol = Protocol.readPacketType(receivePacket);
			try {
				switch (protocol) {
					case SYNC:
						//System.out.println("Received sync package");
						if(!Affiliation.NEUTRAL.equals(player)) {
							serializer.deserializeToBoard(receivePacket.getData(), Protocol.SYNC.getBeginText().length(), nextBoard);
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
