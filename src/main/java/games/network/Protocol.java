package games.network;

import games.model.ActionType;
import games.model.Affiliation;

import java.net.DatagramPacket;

public class Protocol {

	static final int DEFAULT_SERVER_LISTEN_PORT = 8085;
	static final int DEFAULT_CLIENT_LISTEN_PORT = 8088;
	static final int SYNC_BUFFER_LENGHT = 10000;
	static final int MSG_BUFFER_LENGHT = 1000;

	public enum PacketType {
		//client msg
		JOIN("join:"), LEAVE("leave"), ACTION("action:"),
		//server msg
		JOINED("joined:"), DENY("deny:"), SYNC("sync:"),
		BAD_PACKET("unrecognized packet");


		private String beginText;

		private PacketType(String s) {
			this.beginText = s;
		}

		public String getBeginText() {
			return beginText;
		}
	}

	public static PacketType readPacketType(DatagramPacket packet) {
		outer:
		for(PacketType packetType : PacketType.values()) {
			int msgLenght = packet.getLength();
			if(packetType.beginText.length() > msgLenght) {
				continue;
			}
			byte[] data = packet.getData();
			for(int i = 0; i < packetType.beginText.length(); i++) {
				if(!(packetType.beginText.charAt(i) == data[i])) {
					continue outer;
				}
			}
			return packetType;
		}
		return PacketType.BAD_PACKET;
	}

	public static ActionType readActionType(DatagramPacket packet) {
		int num = 0;
		byte[] data = packet.getData();
		for(int i = PacketType.ACTION.beginText.length(); i < packet.getLength(); i++) {
			num = num * 10 + byteToDigit(data[i]);
		}
		return ActionType.getEnumForValue(num);
	}

	public static int readJoinPort(DatagramPacket packet) {
		int num = 0;
		byte[] data = packet.getData();
		for(int i = PacketType.JOIN.beginText.length(); i < packet.getLength(); i++) {
			num = num * 10 + byteToDigit(data[i]);
		}
		return num;
	}

	public static Affiliation readJoinedPlayer(DatagramPacket packet) {
		int num = 0;
		byte[] data = packet.getData();
		for(int i = PacketType.JOINED.beginText.length(); i < packet.getLength(); i++) {
			num = num * 10 + byteToDigit(data[i]);
		}
		return Affiliation.getEnumForValue(num);
	}

	public static String readDenyMessage(DatagramPacket packet) {
		StringBuilder sb = new StringBuilder();
		byte[] data = packet.getData();
		for(int i = PacketType.DENY.beginText.length(); i < packet.getLength(); i++) {
			sb.append((char) data[i]);
		}
		return sb.toString();
	}

	private static int byteToDigit(byte b) {
		return Character.digit((char) b, 10);
	}

	public static void prepareMsg(DatagramPacket packet, PacketType packetType, String message) {
		byte[] buf = packet.getData();
		String msg = (packetType.getBeginText()) + message;
		writeToBuffer(buf, msg);
		packet.setLength(msg.length());
	}

	private static void writeToBuffer(byte[] buf, String s) {
		for(int i = 0; i < s.length(); i++) {
			buf[i] = (byte) s.charAt(i);
		}
	}
}
