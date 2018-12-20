package games.view;

import games.model.*;
import games.network.Client;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class KeyboardController implements KeyListener {

	private Client client;
	private Affiliation player;
	private ActionType lastAction = ActionType.NONE;
	private Board currentBoard;

	public KeyboardController(Client client, Affiliation player) {
		this.client = client;
		this.player = player;
	}

	@Override
	public void keyTyped(KeyEvent e) {

	}

	@Override
	public void keyPressed(KeyEvent e) {
		int code = e.getKeyCode();
		switch(code) {
			case KeyEvent.VK_UP:
				turnOrMove(Direction.UP);
				break;
			case KeyEvent.VK_RIGHT:
				turnOrMove(Direction.RIGHT);
				break;
			case KeyEvent.VK_DOWN:
				turnOrMove(Direction.DOWN);
				break;
			case KeyEvent.VK_LEFT:
				turnOrMove(Direction.LEFT);
				break;
			case KeyEvent.VK_ESCAPE:
				client.leave();
				break;
			case KeyEvent.VK_SPACE:
				shot();
				break;
			default:
				//do nothing
		}
	}

	private void turnOrMove(Direction direction) {
		GameObject tank = currentBoard.getPlayer(player);
		ActionType newAction;
		if(tank != null && tank.canPerformNewAction()) {
			if(direction.equals(tank.getDirection())) {
				newAction = ActionType.MOVE_OUT;
			} else {
				newAction = Direction.directionToTurnAction(direction);
			}
		} else {
			newAction = ActionType.NONE;
		}
		if(lastAction != newAction && newAction != ActionType.NONE) {
			client.performAction(newAction);
		}
		lastAction = newAction;
	}

	private void shot() {
		GameObject tank = currentBoard.getPlayer(player);
		ActionType newAction;
		if(tank != null && tank.canPerformNewAction()) {
			newAction = ActionType.SHOT;
		} else {
			newAction = ActionType.NONE;
		}
		if(lastAction != newAction && newAction != ActionType.NONE) {
			client.performAction(newAction);
		}
		lastAction = newAction;
	}

	@Override
	public void keyReleased(KeyEvent e) {

	}

	public void setBoard(Board board) {
		lastAction = ActionType.NONE;
		currentBoard = board;
	}
}
