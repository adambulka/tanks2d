package games.model.token;

import games.model.Affiliation;
import games.model.Position;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public abstract class Token {

	private TokenType tokenType;
	private Affiliation affiliation;

	private Position position = new Position();

	public Token(TokenType tokenType) {
		this.tokenType = tokenType;
	}

	public void setPosition(int posX, int posY) {
		this.position.setX(posX);
		this.position.setY(posY);
	}
}
