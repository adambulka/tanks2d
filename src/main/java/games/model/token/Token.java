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

	private boolean isBlockingMovement;

	public Token(TokenType tokenType, boolean isBlockingMovement) {
		this.tokenType = tokenType;
		this.isBlockingMovement = isBlockingMovement;
	}

	public void setPosition(int posX, int posY) {
		this.position.setX(posX);
		this.position.setY(posY);
	}

	public void setPosition(int posX, int posY, int devX, int devY) {
		this.position.setX(posX);
		this.position.setY(posY);
		this.position.setDevX(devX);
		this.position.setDevY(devY);
	}

	public void setDevPosition(int devX, int devY) {
		this.position.setDevX(devX);
		this.position.setDevY(devY);
	}
}
