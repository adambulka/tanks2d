package games.model;

public enum Direction {
	UP(1), RIGHT(2), DOWN(3), LEFT(4), NONE(5);

	private int value;

	private Direction(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	public static Direction getEnumForValue(int value) {
		for(Direction direction : Direction.values()) {
			if(direction.value == value) {
				return direction;
			}
		}
		throw new RuntimeException("Direction value for " + value + " does not exist");
	}

	public static Direction turnActionToDirection(ActionType actionType) {
		switch (actionType) {
			case TURN_UP:
				return Direction.UP;
			case TURN_RIGHT:
				return Direction.RIGHT;
			case TURN_LEFT:
				return Direction.LEFT;
			case TURN_DOWN:
				return Direction.DOWN;
			default:
				throw new RuntimeException("Expected turn action but received " + actionType);
		}
	}

	public static ActionType directionToTurnAction(Direction direction) {
		switch (direction) {
			case UP:
				return ActionType.TURN_UP;
			case RIGHT:
				return  ActionType.TURN_RIGHT;
			case DOWN:
				return  ActionType.TURN_DOWN;
			case LEFT:
				return  ActionType.TURN_LEFT;
			default:
				throw new RuntimeException("Expected direction other than none");
		}
	}
}
