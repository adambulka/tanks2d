package games.network;

import games.model.ActionType;
import games.model.Affiliation;

import java.net.DatagramPacket;

public enum Protocol {
	JOIN("join:"), JOINED("joined:"), DENY("deny:"), LEAVE("leave"), ACTION("action:"), SYNC("sync:"), BAD_PACKET("unrecognized packet");

	private String beginText;

	private Protocol(String s) {
		this.beginText = s;
	}

	public String getBeginText() {
		return beginText;
	}

	public static Protocol readPacketType(DatagramPacket packet) {
		outer:
		for(Protocol protocol : Protocol.values()) {
			int msgLenght = packet.getLength();
			if(protocol.beginText.length() > msgLenght) {
				continue;
			}
			byte[] data = packet.getData();
			for(int i = 0; i < protocol.beginText.length(); i++) {
				if(!(protocol.beginText.charAt(i) == data[i])) {
					continue outer;
				}
			}
			return protocol;
		}
		return BAD_PACKET;
	}

	public static ActionType readActionType(DatagramPacket packet) {
		int num = 0;
		byte[] data = packet.getData();
		for(int i = ACTION.beginText.length(); i < packet.getLength(); i++) {
			num = num * 10 + byteToDigit(data[i]);
		}
		return ActionType.getEnumForValue(num);
	}

	public static Affiliation readJoinedPlayer(DatagramPacket packet) {
		int num = 0;
		byte[] data = packet.getData();
		for(int i = JOINED.beginText.length(); i < packet.getLength(); i++) {
			num = num * 10 + byteToDigit(data[i]);
		}
		return Affiliation.getEnumForValue(num);
	}

	public static String readDenyMessage(DatagramPacket packet) {
		StringBuilder sb = new StringBuilder();
		byte[] data = packet.getData();
		for(int i = DENY.beginText.length(); i < packet.getLength(); i++) {
			sb.append((char) data[i]);
		}
		return sb.toString();
	}

	private static int byteToDigit(byte b) {
		return Character.digit((char) b, 10);
	}

	public static void prepareMsg(DatagramPacket packet, Protocol protocol, String message) {
		byte[] buf = packet.getData();
		String msg = (protocol.getBeginText()) + message;
		writeToBuffer(buf, msg);
		packet.setLength(msg.length());
	}

	private static void writeToBuffer(byte[] buf, String s) {
		for(int i = 0; i < s.length(); i++) {
			buf[i] = (byte) s.charAt(i);
		}
	}
}
