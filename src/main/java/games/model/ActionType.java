package games.model;

public enum ActionType {
	TURN_UP(1), TURN_RIGHT(2), TURN_DOWN(3), TURN_LEFT(4),
	MOVE_OUT(5), MOVE_IN(6), SHOT(7), NONE(8);

	private int value;

	private ActionType(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	public static ActionType getEnumForValue(int value) {
		for(ActionType actionType : ActionType.values()) {
			if(actionType.value == value) {
				return actionType;
			}
		}
		throw new RuntimeException("ActionType value for " + value + " does not exist");
	}
}
