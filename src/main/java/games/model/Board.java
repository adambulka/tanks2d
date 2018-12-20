package games.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Top left corner of the board is x=0 and y=0 to avoid translation for graphics
 */
public class Board {

	private static final int BOARD_LENGHT = 20;

	private BoardSquare[][] squares = new BoardSquare[BOARD_LENGHT][BOARD_LENGHT];

	private GameObjectPool gameObjectPool = GameObjectPool.INSTANCE;

	private Map<Affiliation, GameObject> playerToTank = new HashMap<>();

	private long currentCycle = 0;

	private List<GameObject> movingObjects = new ArrayList<>(1000);
	private List<GameObject> removalList = new ArrayList<GameObject>(1000);

	public Board() {
		for(int i = 0; i < BOARD_LENGHT; i++) {
			for(int j = 0; j < BOARD_LENGHT; j++) {
				squares[i][j] = new BoardSquare();
			}
		}
		setupWalls();
	}

	//TODO something else for view?
	public BoardSquare[][] getSquares() {
		return squares;
	}

	public long getCurrentCycle() {
		return currentCycle;
	}

	public void setCurrentCycle(long currentCycle) {
		this.currentCycle = currentCycle;
	}

	/**
	 * may return null (player dead or does not exist)
	 * @param affiliation
	 * @return
	 */
	public GameObject getPlayer(Affiliation affiliation) {
		if(!affiliation.isPlayer()) {
			throw new RuntimeException("Get player for non player affiliation");
		}
		return playerToTank.get(affiliation);
	}

	public void clearBoard() {
		movingObjects.clear();
		removalList.clear();
		playerToTank.clear();
		for(int i = 0; i < BOARD_LENGHT; i++) {
			for (int j = 0; j < BOARD_LENGHT; j++) {
				squares[i][j].clear();
			}
		}
	}

	public void addObject(GameObjectType gameObjectType, Direction direction, Affiliation affiliation, ActionType actionType, int currentFrame, int maxFrame, int x, int y) {
		GameObject gameObject = gameObjectPool.getGameObject(gameObjectType, direction, affiliation, actionType, x, y);
		gameObject.getAction().setCurrentFrame(currentFrame);
		gameObject.getAction().setMaxFrame(maxFrame);
		squares[x][y].addObject(gameObject);
		if(GameObjectType.TANK.equals(gameObjectType) && affiliation.isPlayer()) {
			playerToTank.put(affiliation, gameObject);
		}
	}

	private void setupWalls() {
		int max = BOARD_LENGHT - 1;
		for(int i = 1; i < max; i++) {
			squares[i][0].addObject(gameObjectPool.getWall(i, 0));
			squares[0][i].addObject(gameObjectPool.getWall(0, i));
			squares[i][max].addObject(gameObjectPool.getWall(i, max));
			squares[max][i].addObject(gameObjectPool.getWall(max, i));
		}
		squares[0][0].addObject(gameObjectPool.getWall(0, 0));
		squares[0][max].addObject(gameObjectPool.getWall(0, max));
		squares[max][0].addObject(gameObjectPool.getWall(max, 0));
		squares[max][max].addObject(gameObjectPool.getWall(max, max));
	}

	//TODO remove after tests
	public void setTestEnvironemt() {
		GameObject player1 = gameObjectPool.getTank(Direction.RIGHT, Affiliation.PLAYER1, 1, 1);
		playerToTank.put(Affiliation.PLAYER1, player1);
		squares[1][1].addObject(player1);

		GameObject player2 = gameObjectPool.getTank(Direction.LEFT, Affiliation.PLAYER2, 18, 18);
		playerToTank.put(Affiliation.PLAYER2, player2);
		squares[18][18].addObject(player2);

		GameObject enemy1 = gameObjectPool.getTank(Direction.DOWN, Affiliation.ENEMY, 10, 10);
		squares[10][10].addObject(enemy1);

	}

	public void cycle(Map<Affiliation, ActionType> actions) {
		processMoves(actions);
		gatherMovingObjects();
		continueActions();
		missileCollision();
		for(GameObject gameObject : removalList) {
			removeObjectFromBoard(gameObject);
		}
		movingObjects.clear();
		removalList.clear();
		currentCycle++;
	}

	private void processMoves(Map<Affiliation, ActionType> actions) {
		for(Map.Entry entry : actions.entrySet()) {
			Affiliation player = (Affiliation) entry.getKey();
			ActionType actionType = (ActionType) entry.getValue();
			if(playerToTank.containsKey(player)) {
				GameObject tank = playerToTank.get(player);
				if(tank.canPerformNewAction()) {
					switch (actionType) {
						case MOVE_OUT:
							tank.getAction().initTankMoveOut();
							break;
						case TURN_UP:
						case TURN_RIGHT:
						case TURN_DOWN:
						case TURN_LEFT:
							if(!Direction.turnActionToDirection(actionType).equals(tank.getDirection())) {
								tank.getAction().initTankTurn(actionType);
							}
							break;
						case MOVE_IN:
						case NONE:
							//do nothing
							break;
						case SHOT:
							tank.getAction().initTankShot();
							break;
						default:
							throw new RuntimeException("Unexpected player action");
					}
				}
			}
		}


	}

	private void gatherMovingObjects() {
		int max = BOARD_LENGHT;
		for(int i = 0; i < max; i++) {
			for(int j = 0; j < max; j++) {
				ArrayList<GameObject> list = squares[i][j].getGameObjects();
				for(GameObject gameObject : list) {
					GameObjectType type = gameObject.getGameObjectType();
					if(GameObjectType.TANK.equals(type) || GameObjectType.MISSILE.equals(type)) {
						movingObjects.add(gameObject);
					}
				}
			}
		}
	}

	private void continueActions() {
		for(GameObject gameObject : movingObjects) {
			continueAction(gameObject);
		}
	}

	private void missileCollision() {
		for(GameObject missile : movingObjects) {
			if(GameObjectType.MISSILE.equals(missile.getGameObjectType()) && !removalList.contains(missile)) {
				for(GameObject other : squares[missile.posX][missile.posY].getGameObjects()) {
					if(GameObjectType.WALL.equals(other.getGameObjectType())) {
						removalList.add(missile);
					} else if(Boolean.logicalXor(missile.getAffiliation().isPlayer(), other.getAffiliation().isPlayer())
							&& GameObjectType.TANK.equals(other.getGameObjectType())) {
						removalList.add(missile);
						removalList.add(other);
					}
				}
			}
		}
	}

	private void removeObjectFromBoard(GameObject gameObject) {
		squares[gameObject.posX][gameObject.posY].removeObject(gameObject);
		gameObjectPool.returnToPool(gameObject);
		if(GameObjectType.TANK.equals(gameObject.getGameObjectType())) {
			if(ActionType.MOVE_OUT.equals(gameObject.getAction().getActionType())) {
				int moveX = gameObject.posX;
				int moveY = gameObject.posY;
				switch (gameObject.getDirection()) {
					case UP:
						moveY--;
						break;
					case DOWN:
						moveY++;
						break;
					case RIGHT:
						moveX++;
						break;
					case LEFT:
						moveX--;
						break;
				}
				GameObject moveLock = squares[moveX][moveY].removeMoveLock();
				gameObjectPool.returnToPool(moveLock);
			}
			if(gameObject.getAffiliation().isPlayer()) {
				playerToTank.remove(gameObject.getAffiliation());
			}
		}
	}

	private void continueAction(GameObject gameObject) {
		GameObjectType gameObjectType = gameObject.getGameObjectType();
		if(GameObjectType.MISSILE.equals(gameObjectType)) {
			continueActionMissile(gameObject);
		} else if(GameObjectType.TANK.equals(gameObjectType)) {
			continueActionTank(gameObject);
		} else {
			throw new RuntimeException("Only tanks and missiles have actions");
		}
	}

	private void continueActionMissile(GameObject missile) {
		Action action = missile.getAction();
		ActionType actionType = action.getActionType();
		if(ActionType.MOVE_OUT.equals(actionType)) {
			if(action.isLastFrame()) {
				if(isOutOfBoundaries(missile.getDirection(), missile.posX, missile.posY)) {
					removalList.add(missile);
				} else {
					action.transitionToMissileMoveIn();
					moveObject(missile);
				}
			} else {
				action.increaseCurrentFrame();
			}
		} else if(ActionType.MOVE_IN.equals(actionType)) {
			if(action.isLastFrame()) {
				action.transitionToMissileMoveOut();
			} else {
				action.increaseCurrentFrame();
			}
		} else {
			throw new RuntimeException("Missiles can only move");
		}
	}

	private void continueActionTank(GameObject tank) {
		Action action = tank.getAction();
		ActionType actionType = action.getActionType();
		switch(actionType) {
			case MOVE_OUT:
				if(action.getCurrentFrame() == 0) {
					if(!tryMoveTankWithLock(tank)) {
						action.transitionToNone();
					} else {
						action.increaseCurrentFrame();
					}
				} else if(action.isLastFrame()) {
					action.transitionToTankMoveIn();
					moveObject(tank);
					GameObject moveLock = squares[tank.posX][tank.posY].removeMoveLock();
					gameObjectPool.returnToPool(moveLock);
				} else {
					action.increaseCurrentFrame();
				}
				break;
			case MOVE_IN:
				if(action.isLastFrame()) {
					action.transitionToNone();
				} else {
					action.increaseCurrentFrame();
				}
				break;
			case TURN_UP:
			case TURN_DOWN:
			case TURN_LEFT:
			case TURN_RIGHT:
				if(action.isLastFrame()) {
					action.transitionToNone();
					tank.setDirection(Direction.turnActionToDirection(actionType));
				} else {
					tank.getAction().increaseCurrentFrame();
				}
				break;
			case NONE:
				//do nothing
				break;
			case SHOT:
				if(action.isLastFrame()) {
					action.transitionToNone();
				} else {
					action.increaseCurrentFrame();
					if(action.getCurrentFrame() == 1) {
						GameObject missile = gameObjectPool.getMissile(tank.getDirection(), tank.getAffiliation(), tank.posX, tank.posY);
						missile.getAction().initMissileMoveOut();
						squares[tank.posX][tank.posY].addObject(missile);
					}
				}
				break;
			default:
				throw new RuntimeException("Unhandled tank action");
		}
	}

	private boolean isOutOfBoundaries(Direction direction, int posX, int posY) {
		int moveX = posX;
		int moveY = posY;
		if(Direction.DOWN.equals(direction)) {
			moveY++;
		} else if(Direction.UP.equals(direction)) {
			moveY--;
		} else if(Direction.LEFT.equals(direction)) {
			moveX--;
		} else if(Direction.RIGHT.equals(direction)) {
			moveX++;
		}
		return moveX < 0 || moveY < 0 || moveX >= BOARD_LENGHT || moveY >= BOARD_LENGHT;
	}

	private void moveObject(GameObject gameObject) {
		squares[gameObject.posX][gameObject.posY].removeObject(gameObject);
		if(Direction.DOWN.equals(gameObject.getDirection())) {
			gameObject.posY++;
		} else if(Direction.UP.equals(gameObject.getDirection())) {
			gameObject.posY--;
		} else if(Direction.LEFT.equals(gameObject.getDirection())) {
			gameObject.posX--;
		} else if(Direction.RIGHT.equals(gameObject.getDirection())) {
			gameObject.posX++;
		}
		squares[gameObject.posX][gameObject.posY].addObject(gameObject);
	}

	private boolean tryMoveTankWithLock(GameObject gameObject) {
		int moveX = gameObject.posX;
		int moveY = gameObject.posY;
		if(Direction.DOWN.equals(gameObject.getDirection())) {
			moveY++;
		} else if(Direction.UP.equals(gameObject.getDirection())) {
			moveY--;
		} else if(Direction.LEFT.equals(gameObject.getDirection())) {
			moveX--;
		} else if(Direction.RIGHT.equals(gameObject.getDirection())) {
			moveX++;
		}
		if(moveX < 0 || moveY < 0 || moveX >= BOARD_LENGHT || moveY >= BOARD_LENGHT) {
			return false;
		}
		if(squares[moveX][moveY].containsBlockingObjects()) {
			return false;
		} else {
			squares[moveX][moveY].addObject(gameObjectPool.getMoveLock(moveX, moveY));
			return true;
		}
	}
}
