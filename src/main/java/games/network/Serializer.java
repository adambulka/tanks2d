package games.network;

import games.model.*;
import games.model.token.*;

public class Serializer {

	private static final char BOARD_START = '@';
	private static final char BOARD_END = '!';
	private static final char TOKEN_START = '{';
	private static final char TOKEN_END = '}';

	public StringBuilder serializeBoard(Board board) {
		StringBuilder sb = new StringBuilder();
		sb.append(BOARD_START);
		sb.append(board.getCurrentCycle());
		BoardSquare[][] squares = board.getSquares();
		int max = squares.length;
		for(int i = 0; i < max; i++) {
			for (int j = 0; j < max; j++) {
				for(Token token : squares[i][j].getTokens()) {
					serializeToken(sb, token);
				}
			}
		}
		sb.append(BOARD_END);
		return sb;
	}

	private void serializeToken(StringBuilder sb, Token token) {
		sb.append(TOKEN_START);
		sb.append(token.getTokenType().getValue());
		sb.append(',');
		sb.append(token.getAffiliation().getValue());
		sb.append(',');
		sb.append(token.getPosition().getX());
		sb.append(',');
		sb.append(token.getPosition().getY());
		if(token instanceof Wall || token instanceof MoveLock) {
			sb.append(TOKEN_END);
		} else if(token instanceof Tank) {
			Tank tank = (Tank) token;
			sb.append(',');
			serializeMovingToken(sb, tank);
			sb.append(',');
			sb.append(tank.getAction().getActionType().getValue());
			sb.append(',');
			sb.append(tank.getAction().getCurrentFrame());
			sb.append(',');
			sb.append(tank.getAction().getMaxFrame());
			sb.append(',');
			sb.append(tank.getMaxHp());
			sb.append(',');
			sb.append(tank.getCurrentHp());
			sb.append(',');
			sb.append(tank.getShotStrenght());
			sb.append(TOKEN_END);
		} else if(token instanceof Missile) {
			Missile missile = (Missile) token;
			sb.append(',');
			serializeMovingToken(sb, missile);
			sb.append(',');
			sb.append(missile.getShotStrenght());
			sb.append(TOKEN_END);
		} else {
			throw new RuntimeException("Unexpected object to serialize");
		}
	}

	private void serializeMovingToken(StringBuilder sb, MovingToken movingToken) {
		sb.append(movingToken.getDirection().getValue());
		sb.append(',');
		sb.append(movingToken.getDevPosition().getX());
		sb.append(',');
		sb.append(movingToken.getDevPosition().getY());
		sb.append(',');
		sb.append(movingToken.getSpeed());
	}

	public void deserializeToBoard(byte[] buf, int startOffset, Board board) {
		board.clearBoard();
		ByteBufferHelper helper = new ByteBufferHelper(buf, startOffset);
		if(helper.currentChar() != BOARD_START) {
			throw new RuntimeException("Unexpected serialized content, expected '" + BOARD_START + "' but received '" + helper.currentChar() + "'");
		}
		helper.currentIndex++;
		board.setCurrentCycle(readLongUntil(helper, TOKEN_START, BOARD_END));
		while(helper.currentIndex < buf.length) {
			char c = helper.currentChar();
			if(c == BOARD_END) {
				return;
			} else if(c == TOKEN_START) {
				deserializeObject(helper, board);
			} else {
				throw new RuntimeException("Unexpected token in serialized content " + c);
			}
		}
		throw new RuntimeException("Board serialized content not terminated propertly");
	}

	private void deserializeObject(ByteBufferHelper helper, Board board) {
		helper.currentIndex++;
		TokenType tokenType = TokenType.getEnumForValue(readNumberUntil(helper, ','));
		helper.currentIndex++;
		Affiliation affiliation = Affiliation.getEnumForValue(readNumberUntil(helper, ','));
		helper.currentIndex++;
		int posX = readNumberUntil(helper, ',');
		helper.currentIndex++;
		int posY = readNumberUntil(helper, ',', TOKEN_END);
		switch (tokenType) {
			case WALL:
				board.addWall(posX, posY);
				break;
			case MOVE_LOCK:
				board.addMoveLock(posX, posY);
				break;
			case MISSILE:
				deserializeMissile(helper, board, affiliation, posX, posY);
				break;
			case TANK:
				deserializeTank(helper, board, affiliation, posX, posY);
				break;
		}
		helper.currentIndex++;
	}

	private void deserializeMissile(ByteBufferHelper helper, Board board, Affiliation affiliation, int posX, int posY) {
		helper.currentIndex++;
		Direction direction = Direction.getEnumForValue(readNumberUntil(helper, ','));
		helper.currentIndex++;
		int devX = readNumberUntil(helper, ',');
		helper.currentIndex++;
		int devY = readNumberUntil(helper, ',');
		helper.currentIndex++;
		int speed = readNumberUntil(helper, ',');
		helper.currentIndex++;
		int shotStrenght = readNumberUntil(helper, TOKEN_END);
		//TODO shot strenght and speed
		board.addMissile(direction, affiliation, posX, posY, devX, devY);
	}

	private void deserializeTank(ByteBufferHelper helper, Board board, Affiliation affiliation, int posX, int posY) {
		helper.currentIndex++;
		Direction direction = Direction.getEnumForValue(readNumberUntil(helper, ','));
		helper.currentIndex++;
		int devX = readNumberUntil(helper, ',');
		helper.currentIndex++;
		int devY = readNumberUntil(helper, ',');
		helper.currentIndex++;
		int speed = readNumberUntil(helper, ',');
		helper.currentIndex++;

		ActionType actionType = ActionType.getEnumForValue(readNumberUntil(helper, ','));
		helper.currentIndex++;
		int currentFrame = readNumberUntil(helper, ',');
		helper.currentIndex++;
		int maxFrame = readNumberUntil(helper, ',');
		helper.currentIndex++;
		int maxHp = readNumberUntil(helper, ',');
		helper.currentIndex++;
		int currentHp = readNumberUntil(helper, ',');
		helper.currentIndex++;
		int shotStrenght = readNumberUntil(helper, TOKEN_END);
		//TODO shot strenght, speed, hp
		board.addTank(direction, affiliation, actionType, currentFrame, maxFrame, posX, posY, devX, devY);
	}

	private int readNumberUntil(ByteBufferHelper helper, char end) {
		return readNumberUntil(helper, end, end);
	}

	private int readNumberUntil(ByteBufferHelper helper, char end1, char end2) {
		boolean readDigit = false;
		int number = 0;
		int signum = 1;
		while(helper.currentIndex < helper.buf.length) {
			char c = helper.currentChar();
			if(signum == 1 && '-' == c) {
				signum = -1;
				helper.currentIndex++;
			} else if(Character.isDigit(c)) {
				readDigit = true;
				number = number * 10 + Character.digit(c, 10);
				helper.currentIndex++;
			} else if(end1 == c || end2 == c) {
				if(readDigit) {
					return number * signum;
				} else {
					throw new RuntimeException("Expected to find at least one digit in serialized content");
				}
			} else {
				throw new RuntimeException("Expected to find digit or '" + + end1 + "' or '" + end2 + "' but found '" + c + "'");
			}
		}
		throw new RuntimeException("End of buffer reached while reading number");
	}

	private long readLongUntil(ByteBufferHelper helper, char end1, char end2) {
		boolean readDigit = false;
		long number = 0;
		long signum = 1;
		while(helper.currentIndex < helper.buf.length) {
			char c = helper.currentChar();
			if(signum == 1 && '-' == c) {
				signum = -1;
				helper.currentIndex++;
			} else if(Character.isDigit(c)) {
				readDigit = true;
				number = number * 10 + Character.digit(c, 10);
				helper.currentIndex++;
			} else if(end1 == c || end2 == c) {
				if(readDigit) {
					return number;
				} else {
					throw new RuntimeException("Expected to find at least one digit in serialized content");
				}
			} else {
				throw new RuntimeException("Expected to find digit or '" + end1 + "' or '" + end2 + "' but found '" + c + "'");
			}
		}
		throw new RuntimeException("End of buffer reached while reading number");
	}

	private static class ByteBufferHelper {
		public byte[] buf;
		public int currentIndex;
		public ByteBufferHelper(byte[] buf, int currentIndex) {
			this.buf = buf;
			this.currentIndex = currentIndex;
		}

		public char currentChar() {
			return (char) buf[currentIndex];
		}
	}
}
