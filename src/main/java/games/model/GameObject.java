package games.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@ToString
public class GameObject {

	public GameObject() {
		this.direction = Direction.NONE;
		this.affiliation = Affiliation.NEUTRAL;
		this.gameObjectType = GameObjectType.WALL;
		this.action = new Action(ActionType.NONE, 0, 0);
	}

	@Setter
	private Direction direction;
	@Setter
	private Affiliation affiliation;
	@Setter
	private GameObjectType gameObjectType;
	private Action action;
	public int posX;
	public int posY;

	public boolean canPerformNewAction() {
		return ActionType.NONE.equals(action.getActionType())
				|| (!ActionType.MOVE_OUT.equals(action.getActionType()) && action.getCurrentFrame() == action.getMaxFrame());
	}
}
