package games.model;

import games.model.token.MoveLock;
import games.model.token.Token;
import games.model.token.TokenType;

import java.util.ArrayList;

public class BoardSquare {

	private static final int MAX_SIZE = 10;
	private ArrayList<Token> tokens = new ArrayList<>(MAX_SIZE);

	public void addToken(Token token) {
		tokens.add(token);
		if(tokens.size() > MAX_SIZE) {
			throw new RuntimeException("Too many tokens on one BoardSquare while trying to add: " + token + ". Current tokens: " + tokens);
		}
	}

	public void removeToken(Token token) {
		if(!tokens.remove(token)) {
			throw new RuntimeException("Could not remove token from BoardSquare: " + token);
		}
	}

	public MoveLock removeMoveLock() {
		for(int i = 0; i < tokens.size(); i++) {
			if(TokenType.MOVE_LOCK.equals(tokens.get(i).getTokenType())) {
				return (MoveLock) tokens.remove(i);
			}
		}
		throw new RuntimeException("No move lock present");
	}

	public ArrayList<Token> getTokens() {
		return tokens;
	}

	public boolean containsMovementBlockingTokens() {
		for(Token token : tokens) {
			if(token.isBlockingMovement()) {
				return true;
			}
		}
		return false;
	}

	public void clear() {
		tokens.forEach(TokenPool.INSTANCE::returnToPool);
		tokens.clear();
	}
}
