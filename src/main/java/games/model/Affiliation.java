package games.model;

public enum Affiliation {
	NEUTRAL(10, false), ENEMY(11, false), PLAYER1(1, true), PLAYER2(2, true);

	private int value;
	private boolean isPlayer;

	private Affiliation(int value, boolean isPlayer) {
		this.value = value;
		this.isPlayer = isPlayer;
	}

	public int getValue() {
		return value;
	}

	public boolean isPlayer() {
		return isPlayer;
	}

	public static Affiliation getEnumForValue(int value) {
		for(Affiliation affiliation : Affiliation.values()) {
			if(affiliation.value == value) {
				return affiliation;
			}
		}
		throw new RuntimeException("Affiliation value for " + value + " does not exist");
	}
}
