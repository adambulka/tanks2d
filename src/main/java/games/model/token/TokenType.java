package games.model.token;

public enum TokenType {
	WALL(1), TANK(2), MISSILE(3), MOVE_LOCK(4);

	private int value;

	private TokenType(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	public static TokenType getEnumForValue(int value) {
		for(TokenType tokenType : TokenType.values()) {
			if(tokenType.value == value) {
				return tokenType;
			}
		}
		throw new RuntimeException("TokenType value for " + value + " does not exist");
	}
}
