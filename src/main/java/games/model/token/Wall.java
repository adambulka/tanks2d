package games.model.token;

import games.model.Affiliation;

public class Wall extends Token {

	public Wall() {
		super(TokenType.WALL, true);
		setAffiliation(Affiliation.NEUTRAL);
	}
}
