package games.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@ToString
public class Action {

	private static final int MISSILE_MAX_MOVE_FRAMES = 1;
	private static final int TANK_MAX_MOVE_FRAMES = 6;
	private static final int TANK_MAX_SHOT_FRAMES = 12;

	private ActionType actionType;
	private int currentFrame;
	private int maxFrame;

	public void increaseCurrentFrame() {
		currentFrame++;
	}

	public boolean isLastFrame() {
		return currentFrame == maxFrame;
	}

	public void initTankMoveOut() {
		actionType = ActionType.MOVE_OUT;
		currentFrame = 0;
		maxFrame = TANK_MAX_MOVE_FRAMES;
	}

	public void initMissileMoveOut() {
		actionType = ActionType.MOVE_OUT;
		currentFrame = 0;
		maxFrame = MISSILE_MAX_MOVE_FRAMES;
	}

	public void initTankTurn(ActionType turnType) {
		switch (turnType) {
			case TURN_UP:
			case TURN_RIGHT:
			case TURN_DOWN:
			case TURN_LEFT:
				break;
			default:
				throw new RuntimeException("Expected action type turn but received " + turnType);
		}
		actionType = turnType;
		currentFrame = 0;
		maxFrame = 1;
	}

	public void initTankShot() {
		actionType = ActionType.SHOT;
		currentFrame = 0;
		maxFrame = TANK_MAX_SHOT_FRAMES;
	}

	public void transitionToNone() {
		actionType = ActionType.NONE;
		currentFrame = 0;
		maxFrame = 0;
	}

	public void transitionToMissileMoveIn() {
		actionType = ActionType.MOVE_IN;
		currentFrame = 1;
		maxFrame = MISSILE_MAX_MOVE_FRAMES;
	}

	public void transitionToMissileMoveOut() {
		actionType = ActionType.MOVE_OUT;
		currentFrame = 1;
		maxFrame = MISSILE_MAX_MOVE_FRAMES;
	}

	public void transitionToTankMoveIn() {
		actionType = ActionType.MOVE_IN;
		currentFrame = 1;
		maxFrame = TANK_MAX_MOVE_FRAMES;
	}
}
