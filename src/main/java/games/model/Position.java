package games.model;

import lombok.*;

@Getter
@Setter
@ToString
public class Position {
	private int x;
	private int y;
	//deviation from center of square, between -100 and 100
	private int devX;
	private int devY;
}
