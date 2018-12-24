package games.model.token;

import games.model.Direction;
import games.model.Position;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public abstract class MovingToken extends Token {

	private Direction direction = Direction.NONE;
	//deviation from center of square, between -100 and 100
	private Position devPosition = new Position();
	//should be between 0 and 100
	private int speed;

	public MovingToken(TokenType tokenType) {
		super(tokenType);
	}

	public void setDevPosition(int devX, int devY) {
		this.devPosition.setX(devX);
		this.devPosition.setY(devY);
	}
}
