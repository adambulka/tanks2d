package games.model.token;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Missile extends MovingToken {

	private int shotStrenght;

	public Missile() {
		super(TokenType.MISSILE, false);
		this.setSpeed(50);
	}
}
