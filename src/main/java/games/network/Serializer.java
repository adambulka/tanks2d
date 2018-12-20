package games.network;

import games.model.*;

public class Serializer {

	private static final char BOARD_START = '@';
	private static final char BOARD_END = '!';
	private static final char SQUARE_START = '[';
	private static final char SQUARE_END = ']';
	private static final char OBJECT_START = '{';
	private static final char OBJECT_END = '}';

	public StringBuilder serializeBoard(Board board) {
		StringBuilder sb = new StringBuilder();
		sb.append(BOARD_START);
		sb.append(board.getCurrentCycle());
		BoardSquare[][] squares = board.getSquares();
		int max = squares.length;
		for(int i = 0; i < max; i++) {
			for (int j = 0; j < max; j++) {
				serializeSquare(sb, squares[i][j], i, j);
			}
		}
		sb.append(BOARD_END);
		return sb;
	}

	private void serializeSquare(StringBuilder sb, BoardSquare boardSquare, int x, int y) {
		if(boardSquare.getGameObjects().isEmpty()) {
			return;
		}
		sb.append(SQUARE_START);
		sb.append(x);
		sb.append(',');
		sb.append(y);
		for(GameObject gameObject : boardSquare.getGameObjects()) {
			serializeGameObject(sb, gameObject);
		}
		sb.append(SQUARE_END);
	}

	private void serializeGameObject(StringBuilder sb, GameObject gameObject) {
		sb.append(OBJECT_START);
		sb.append(gameObject.getGameObjectType().getValue());
		sb.append(',');
		sb.append(gameObject.getDirection().getValue());
		sb.append(',');
		sb.append(gameObject.getAffiliation().getValue());
		sb.append(',');
		sb.append(gameObject.getAction().getActionType().getValue());
		sb.append(',');
		sb.append(gameObject.getAction().getCurrentFrame());
		sb.append(',');
		sb.append(gameObject.getAction().getMaxFrame());
		sb.append(OBJECT_END);
	}

	public void deserializeToBoard(byte[] buf, int startOffset, Board board) {
		board.clearBoard();
		ByteBufferHelper helper = new ByteBufferHelper(buf, startOffset);
		if(helper.currentChar() != BOARD_START) {
			throw new RuntimeException("Unexpected serialized content, expected '" + BOARD_START + "' but received '" + helper.currentChar() + "'");
		}
		helper.currentIndex++;
		board.setCurrentCycle(readLongUntil(helper, SQUARE_START, BOARD_END));
		while(helper.currentIndex < buf.length) {
			char c = helper.currentChar();
			if(c == BOARD_END) {
				return;
			} else if(c == SQUARE_START) {
				deserializeSquare(helper, board);
			} else {
				throw new RuntimeException("Unexpected token in serialized content " + c);
			}
		}
		throw new RuntimeException("Board serialized content not terminated propertly");
	}

	private void deserializeSquare(ByteBufferHelper helper, Board board) {
		helper.currentIndex++;
		int x = readNumberUntil(helper, ',');
		helper.currentIndex++;
		int y = readNumberUntil(helper, OBJECT_START);
		while(helper.currentChar() == OBJECT_START) {
			deserializeObject(helper, board, x, y);
		}
		if(!(helper.currentChar() == SQUARE_END)) {
			throw new RuntimeException("Expected square end but got '" + helper.currentChar() + "'");
		}
		helper.currentIndex++;
	}

	private void deserializeObject(ByteBufferHelper helper, Board board, int x, int y) {
		helper.currentIndex++;
		GameObjectType gameObjectType = GameObjectType.getEnumForValue(readNumberUntil(helper, ','));
		helper.currentIndex++;
		Direction direction = Direction.getEnumForValue(readNumberUntil(helper, ','));
		helper.currentIndex++;
		Affiliation affiliation = Affiliation.getEnumForValue(readNumberUntil(helper, ','));
		helper.currentIndex++;
		ActionType actionType = ActionType.getEnumForValue(readNumberUntil(helper, ','));
		helper.currentIndex++;
		int currentFrame = readNumberUntil(helper, ',');
		helper.currentIndex++;
		int maxFrame = readNumberUntil(helper, OBJECT_END);
		helper.currentIndex++;

		board.addObject(gameObjectType, direction, affiliation, actionType, currentFrame, maxFrame, x, y);
	}

	private int readNumberUntil(ByteBufferHelper helper, char end) {
		boolean readDigit = false;
		int number = 0;
		while(helper.currentIndex < helper.buf.length) {
			char c = helper.currentChar();
			if(Character.isDigit(c)) {
				readDigit = true;
				number = number * 10 + Character.digit(c, 10);
				helper.currentIndex++;
			} else if(end == c) {
				if(readDigit) {
					return number;
				} else {
					throw new RuntimeException("Expected to find at least one digit in serialized content");
				}
			} else {
				throw new RuntimeException("Expected to find digit or '" + end + "' but found '" + c + "'");
			}
		}
		throw new RuntimeException("End of buffer reached while reading number");
	}

	private long readLongUntil(ByteBufferHelper helper, char end1, char end2) {
		boolean readDigit = false;
		int number = 0;
		while(helper.currentIndex < helper.buf.length) {
			char c = helper.currentChar();
			if(Character.isDigit(c)) {
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
