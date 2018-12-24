package games.model;

import games.model.token.*;

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

	private TokenPool tokenPool = TokenPool.INSTANCE;

	private Map<Affiliation, Tank> playerToTank = new HashMap<>();

	private long currentCycle = 0;

	private List<MovingToken> movingObjects = new ArrayList<>(1000);
	private List<Token> removalList = new ArrayList<>(1000);

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
	public Tank getPlayer(Affiliation affiliation) {
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

	public void addWall(int posX, int posY) {
		Wall wall = tokenPool.getWall();
		wall.setPosition(posX, posY);
		squares[posX][posY].addToken(wall);
	}

	public void addMoveLock(int posX, int posY) {
		MoveLock moveLock = tokenPool.getMoveLock();
		moveLock.setPosition(posX, posY);
		squares[posX][posY].addToken(moveLock);
	}

	public void addTank(Direction direction, Affiliation affiliation, ActionType actionType, int currentFrame, int maxFrame, int posX, int posY, int devX, int devY) {
		Tank tank = tokenPool.getTank();
		tank.setDirection(direction);
		tank.setAffiliation(affiliation);
		tank.getAction().setActionType(actionType);
		tank.getAction().setCurrentFrame(currentFrame);
		tank.getAction().setMaxFrame(maxFrame);
		tank.setPosition(posX, posY);
		tank.setDevPosition(devX, devY);
		squares[posX][posY].addToken(tank);

		if(affiliation.isPlayer()) {
			playerToTank.put(affiliation, tank);
		}
	}

	public void addMissile(Direction direction, Affiliation affiliation, int posX, int posY, int devX, int devY) {
		Missile missile = tokenPool.getMissile();
		missile.setDirection(direction);
		missile.setAffiliation(affiliation);
		missile.setPosition(posX, posY);
		missile.setDevPosition(devX, devY);
		squares[posX][posY].addToken(missile);
	}

	private void setupWalls() {
		int max = BOARD_LENGHT - 1;
		for(int i = 1; i < max; i++) {
			addWall(i, 0);
			addWall(0, i);
			addWall(i, max);
			addWall(max, i);
		}
		addWall(0, 0);
		addWall(0, max);
		addWall(max, 0);
		addWall(max, max);
	}

	//TODO remove after tests
	public void setTestEnvironemt() {
		addTank(Direction.RIGHT, Affiliation.PLAYER1, ActionType.NONE, 0, 0, 1, 1, 0, 0);
		addTank(Direction.LEFT, Affiliation.PLAYER2, ActionType.NONE, 0, 0, 18, 18, 0, 0);
		addTank(Direction.DOWN, Affiliation.ENEMY, ActionType.NONE, 0, 0, 10, 10, 0, 0);
	}

	public void cycle(Map<Affiliation, ActionType> actions) {
		processMoves(actions);
		gatherMovingObjects();
		continueActions();
		missileCollision();
		for(Token token : removalList) {
			removeObjectFromBoard(token);
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
				Tank tank = playerToTank.get(player);
				if(tank.canPerformNewAction()) {
					switch (actionType) {
						case MOVE:
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
				ArrayList<Token> list = squares[i][j].getTokens();
				for(Token token : list) {
					TokenType type = token.getTokenType();
					if(TokenType.TANK.equals(type) || TokenType.MISSILE.equals(type)) {
						movingObjects.add((MovingToken) token);
					}
				}
			}
		}
	}

	private void continueActions() {
		for(MovingToken token : movingObjects) {
			continueAction(token);
		}
	}

	private void missileCollision() {
		for(MovingToken missile : movingObjects) {
			if(TokenType.MISSILE.equals(missile.getTokenType()) && !removalList.contains(missile)) {
				for(Token other : squares[missile.getPosition().getX()][missile.getPosition().getY()].getTokens()) {
					if(TokenType.WALL.equals(other.getTokenType())) {
						removalList.add(missile);
					} else if(Boolean.logicalXor(missile.getAffiliation().isPlayer(), other.getAffiliation().isPlayer())
							&& TokenType.TANK.equals(other.getTokenType())) {
						//TODO damage the other tank instead of destroying it
						removalList.add(missile);
						removalList.add(other);
					}
				}
			}
		}
	}

	private void removeObjectFromBoard(Token token) {
		squares[token.getPosition().getX()][token.getPosition().getY()].removeToken(token);
		tokenPool.returnToPool(token);
		if(TokenType.TANK.equals(token.getTokenType())) {
			Tank tank = (Tank) token;
			if(ActionType.MOVE.equals(tank.getAction().getActionType())) {
				int moveX = token.getPosition().getX();
				int moveY = token.getPosition().getY();
				switch (tank.getDirection()) {
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
				MoveLock moveLock = squares[moveX][moveY].removeMoveLock();
				tokenPool.returnToPool(moveLock);
			}
			if(token.getAffiliation().isPlayer()) {
				playerToTank.remove(token.getAffiliation());
			}
		}
	}

	private void continueAction(MovingToken token) {
		TokenType tokenType = token.getTokenType();
		if(TokenType.MISSILE.equals(tokenType)) {
			continueActionMissile((Missile) token);
		} else if(TokenType.TANK.equals(tokenType)) {
			continueActionTank((Tank) token);
		} else {
			throw new RuntimeException("Only tanks and missiles have actions");
		}
	}

	private void continueActionMissile(Missile missile) {
		if(willChangeSquare(missile) && isOutOfBoundaries(missile.getDirection(), missile.getPosition().getX(), missile.getPosition().getY())) {
			removalList.add(missile);
		} else {
			moveDev(missile);
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

	private void continueActionTank(Tank tank) {
		Action action = tank.getAction();
		ActionType actionType = action.getActionType();
		switch(actionType) {
			case MOVE:
				if(action.getCurrentFrame() == 0) {
					if(!tryMoveTankWithLock(tank)) {
						action.transitionToNone();
						return;
					}
				}
				if(willPassSquareCenter(tank)) {
					action.transitionToNone();
					tank.setDevPosition(0, 0);
				} else {
					action.increaseCurrentFrame();
					moveDev(tank);
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
						addMissile(tank.getDirection(), tank.getAffiliation(), tank.getPosition().getX(), tank.getPosition().getY(), 0, 0);
					}
				}
				break;
			default:
				throw new RuntimeException("Unhandled tank action");
		}
	}

	private boolean willPassSquareCenter(MovingToken movingToken) {
		int devX = movingToken.getDevPosition().getX();
		int devY = movingToken.getDevPosition().getY();
		switch (movingToken.getDirection()) {
			case DOWN:
				if(devY < 0 && devY + movingToken.getSpeed() >= 0) {
					return true;
				}
				break;
			case UP:
				if(devY > 0 && devY - movingToken.getSpeed() <= 0) {
					return true;
				}
				break;
			case RIGHT:
				if(devX < 0 && devX + movingToken.getSpeed() >= 0) {
					return true;
				}
				break;
			case LEFT:
				if(devX > 0 && devX - movingToken.getSpeed() <= 0) {
					return true;
				}
				break;
		}
		return false;
	}

	private boolean willChangeSquare(MovingToken movingToken) {
		int devX = movingToken.getDevPosition().getX();
		int devY = movingToken.getDevPosition().getY();
		switch (movingToken.getDirection()) {
			case DOWN:
				if(devY + movingToken.getSpeed() >= 100) {
					return true;
				}
				break;
			case UP:
				if(devY - movingToken.getSpeed() <= -100) {
					return true;
				}
				break;
			case RIGHT:
				if(devX + movingToken.getSpeed() >= 100) {
					return true;
				}
				break;
			case LEFT:
				if(devX - movingToken.getSpeed() <= -100) {
					return true;
				}
				break;
		}
		return false;
	}

	private void moveDev(MovingToken movingToken) {
		int devX = movingToken.getDevPosition().getX();
		int devY = movingToken.getDevPosition().getY();
		switch (movingToken.getDirection()) {
			case DOWN:
				devY = devY + movingToken.getSpeed();
				break;
			case UP:
				devY = devY - movingToken.getSpeed();
				break;
			case RIGHT:
				devX = devX + movingToken.getSpeed();
				break;
			case LEFT:
				devX = devX - movingToken.getSpeed();
				break;
		}
		if(devY > 100) {
			devY = devY - 200;
			moveSquare(movingToken);
		} else if(devY < -100) {
			devY = 200 + devY;
			moveSquare(movingToken);
		} else if(devX > 100) {
			devX = devX - 200;
			moveSquare(movingToken);
		} else if(devX < -100) {
			devX = 200 + devX;
			moveSquare(movingToken);
		}
		movingToken.setDevPosition(devX, devY);
	}

	private void moveSquare(MovingToken movingToken) {
		int x = movingToken.getPosition().getX();
		int y = movingToken.getPosition().getY();
		squares[x][y].removeToken(movingToken);
		if(Direction.DOWN.equals(movingToken.getDirection())) {
			y++;
		} else if(Direction.UP.equals(movingToken.getDirection())) {
			y--;
		} else if(Direction.LEFT.equals(movingToken.getDirection())) {
			x--;
		} else if(Direction.RIGHT.equals(movingToken.getDirection())) {
			x++;
		}
		movingToken.setPosition(x, y);
		squares[x][y].addToken(movingToken);
		if(TokenType.TANK.equals(movingToken.getTokenType())) {
			MoveLock moveLock = squares[x][y].removeMoveLock();
			tokenPool.returnToPool(moveLock);
		}
	}

	private boolean tryMoveTankWithLock(Tank tank) {
		int x = tank.getPosition().getX();
		int y = tank.getPosition().getY();
		if(Direction.DOWN.equals(tank.getDirection())) {
			y++;
		} else if(Direction.UP.equals(tank.getDirection())) {
			y--;
		} else if(Direction.LEFT.equals(tank.getDirection())) {
			x--;
		} else if(Direction.RIGHT.equals(tank.getDirection())) {
			x++;
		}
		if(x < 0 || y < 0 || x >= BOARD_LENGHT || y >= BOARD_LENGHT) {
			return false;
		}
		if(squares[x][y].containsBlockingObjects()) {
			return false;
		} else {
			addMoveLock(x, y);
			return true;
		}
	}
}
