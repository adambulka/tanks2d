package games.model.token;

import games.model.Action;
import games.model.ActionType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Tank extends MovingToken {

	private Action action;
	private int maxHp;
	private int currentHp;
	private int shotStrenght;

	public Tank() {
		super(TokenType.TANK, true);
		this.action = new Action(ActionType.NONE, 0, 0);
		this.setSpeed(10);
	}

	public boolean canPerformNewAction() {
		return ActionType.NONE.equals(action.getActionType());
	}
}
