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
	//should be between 0 and 100
	private int speed;

	public MovingToken(TokenType tokenType, boolean isBlockingMovement) {
		super(tokenType, isBlockingMovement);
	}

	public boolean isMovingOut() {
		int devX = getPosition().getDevX();
		int devY = getPosition().getDevY();
		if(devX >= 0 && direction == Direction.RIGHT) {
			return true;
		} else if(devX <= 0 && direction == Direction.LEFT) {
			return true;
		} else if(devY >= 0 && direction == Direction.DOWN) {
			return true;
		} else if(devY <= 0 && direction == Direction.UP) {
			return true;
		}
		return false;
	}

	public Position calculateNextPosition() {
		int x = this.getPosition().getX();
		int y = this.getPosition().getY();
		int devX = this.getPosition().getDevX();
		int devY = this.getPosition().getDevY();
		switch (this.getDirection()) {
			case DOWN:
				devY = devY + this.getSpeed();
				break;
			case UP:
				devY = devY - this.getSpeed();
				break;
			case RIGHT:
				devX = devX + this.getSpeed();
				break;
			case LEFT:
				devX = devX - this.getSpeed();
				break;
		}
		if(devY > 100) {
			devY = devY - 200;
			y++;
		} else if(devY < -100) {
			devY = 200 + devY;
			y--;
		} else if(devX > 100) {
			devX = devX - 200;
			x++;
		} else if(devX < -100) {
			devX = 200 + devX;
			x--;
		}
		Position pos = new Position();
		pos.setX(x);
		pos.setY(y);
		pos.setDevX(devX);
		pos.setDevY(devY);
		return pos;
	}

	public boolean willPassSquareCenter() {
		int devX = this.getPosition().getDevX();
		int devY = this.getPosition().getDevY();
		switch (this.getDirection()) {
			case DOWN:
				if(devY < 0 && devY + this.getSpeed() >= 0) {
					return true;
				}
				break;
			case UP:
				if(devY > 0 && devY - this.getSpeed() <= 0) {
					return true;
				}
				break;
			case RIGHT:
				if(devX < 0 && devX + this.getSpeed() >= 0) {
					return true;
				}
				break;
			case LEFT:
				if(devX > 0 && devX - this.getSpeed() <= 0) {
					return true;
				}
				break;
		}
		return false;
	}
}
