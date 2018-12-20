package games.model;

import java.util.ArrayList;

public class GameObjectPool {

	private static final int MAX_POOL = 10000;

	private ArrayList<GameObject> pool = new ArrayList<GameObject>(MAX_POOL);

	public static final GameObjectPool INSTANCE = new GameObjectPool();

	private GameObjectPool() {
		initializePool();
	}

	private void initializePool() {
		for(int i = 0; i < MAX_POOL; i++) {
			pool.add(new GameObject());
		}
	}

	public GameObject getTank(Direction direction, Affiliation affiliation, int posX, int posY) {
		return getGameObject(GameObjectType.TANK, direction, affiliation, ActionType.NONE, posX, posY);
	}

	public GameObject getWall(int posX, int posY) {
		return getGameObject(GameObjectType.WALL, Direction.NONE, Affiliation.NEUTRAL, ActionType.NONE, posX, posY);
	}

	public GameObject getMissile(Direction direction, Affiliation affiliation, int posX, int posY) {
		return getGameObject(GameObjectType.MISSILE, direction, affiliation, ActionType.MOVE_OUT, posX, posY);
	}

	public GameObject getMoveLock(int posX, int posY) {
		return getGameObject(GameObjectType.MOVE_LOCK, Direction.NONE, Affiliation.NEUTRAL, ActionType.NONE, posX, posY);
	}

	public void returnToPool(GameObject gameObject) {
		pool.add(gameObject);
	}

	public GameObject getGameObject(GameObjectType gameObjectType, Direction direction, Affiliation affiliation, ActionType actionType, int posX, int posY) {
		GameObject gameObject = pool.remove(pool.size() - 1);
		gameObject.setGameObjectType(gameObjectType);
		gameObject.setDirection(direction);
		gameObject.setAffiliation(affiliation);
		gameObject.getAction().setActionType(actionType);
		gameObject.getAction().setCurrentFrame(0);
		gameObject.getAction().setMaxFrame(0);
		gameObject.posX = posX;
		gameObject.posY = posY;
		return gameObject;
	}
}
