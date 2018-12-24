package games.model;

import games.model.token.*;

import java.util.ArrayList;

public class TokenPool {

	private static final int MAX_POOL = 1000;

	private ArrayList<Tank> tanks = new ArrayList<>(MAX_POOL);
	private ArrayList<Missile> missiles = new ArrayList<>(MAX_POOL);
	private ArrayList<MoveLock> moveLocks = new ArrayList<>(MAX_POOL);
	private ArrayList<Wall> walls = new ArrayList<>(MAX_POOL);

	public static final TokenPool INSTANCE = new TokenPool();

	private TokenPool() {
		initializePool();
	}

	private void initializePool() {
		for(int i = 0; i < MAX_POOL; i++) {
			tanks.add(new Tank());
			missiles.add(new Missile());
			moveLocks.add(new MoveLock());
			walls.add(new Wall());
		}
	}

	public Tank getTank() {
		return tanks.remove(tanks.size() - 1);
	}

	public Missile getMissile() {
		return missiles.remove(missiles.size() - 1);
	}

	public MoveLock getMoveLock() {
		return moveLocks.remove(moveLocks.size() - 1);
	}

	public Wall getWall() {
		return walls.remove(walls.size() - 1);
	}

	public void returnToPool(Token token) {
		switch (token.getTokenType()) {
			case TANK:
				returnToPool((Tank) token);
				break;
			case MISSILE:
				returnToPool((Missile) token);
				break;
			case MOVE_LOCK:
				returnToPool((MoveLock) token);
				break;
			case WALL:
				returnToPool((Wall) token);
				break;
		}
	}

	public void returnToPool(Tank tank) {
		tanks.add(tank);
	}

	public void returnToPool(Missile missile) {
		missiles.add(missile);
	}

	public void returnToPool(MoveLock moveLock) {
		moveLocks.add(moveLock);
	}

	public void returnToPool(Wall wall) {
		walls.add(wall);
	}
}
