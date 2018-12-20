package games.model;

public enum GameObjectType {
	WALL(1), TANK(2), MISSILE(3), MOVE_LOCK(4);

	private int value;

	private GameObjectType(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	public static GameObjectType getEnumForValue(int value) {
		for(GameObjectType gameObjectType : GameObjectType.values()) {
			if(gameObjectType.value == value) {
				return gameObjectType;
			}
		}
		throw new RuntimeException("GameObjectType value for " + value + " does not exist");
	}
}
