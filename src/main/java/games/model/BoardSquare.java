package games.model;

import java.util.ArrayList;

public class BoardSquare {

	private static final int MAX_SIZE = 10;
	private ArrayList<GameObject> gameObjects = new ArrayList<>(MAX_SIZE);

	public void addObject(GameObject gameObject) {
		gameObjects.add(gameObject);
		if(gameObjects.size() > MAX_SIZE) {
			throw new RuntimeException("Too many objects on one BoardSquare while trying to add: " + gameObject + ". Current objects: " + gameObjects);
		}
	}

	public void removeObject(GameObject gameObject) {
		if(!gameObjects.remove(gameObject)) {
			throw new RuntimeException("Could not remove object from BoardSquare: " + gameObject);
		}
	}

	public GameObject removeMoveLock() {
		for(int i = 0; i < gameObjects.size(); i++) {
			if(GameObjectType.MOVE_LOCK.equals(gameObjects.get(i).getGameObjectType())) {
				return gameObjects.remove(i);
			}
		}
		throw new RuntimeException("No move lock present");
	}

	public ArrayList<GameObject> getGameObjects() {
		return gameObjects;
	}

	public boolean containsBlockingObjects() {
		for(GameObject gameObject : gameObjects) {
			if(GameObjectType.MOVE_LOCK.equals(gameObject.getGameObjectType())
					|| GameObjectType.WALL.equals(gameObject.getGameObjectType())
					|| GameObjectType.TANK.equals(gameObject.getGameObjectType())
					) {
				return true;
			}
		}
		return false;
	}

	public void clear() {
		gameObjects.forEach(GameObjectPool.INSTANCE::returnToPool);
		gameObjects.clear();
	}
}
