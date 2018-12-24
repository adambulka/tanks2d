package games.model.token;

import games.model.Affiliation;

public class MoveLock extends Token {

	public MoveLock() {
		super(TokenType.MOVE_LOCK);
		setAffiliation(Affiliation.NEUTRAL);
	}
}
