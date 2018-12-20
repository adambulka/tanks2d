package games.view;

import games.model.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

public class BoardView extends JPanel {

	private static final int SQUARE_SIZE_PIXELS = 40;

	private static final int NEUTRAL_COLOR = 0xff969696;
	private static final int WHITE = 0xffffffff;
	private static final int TRANSLUCENT = 0x00ffffff;

	private BufferedImage wall;
	private BufferedImage empty;
	private BufferedImage tank;
	private BufferedImage missile;

	private BufferedImage tankP1;
	private BufferedImage tankP2;
	private BufferedImage missileP1;
	private BufferedImage missileP2;

	private Board board;

	public BoardView() {
		initBoardView();
		loadImages();
	}

	public void setBoard(Board board) {
		this.board = board;
	}

	private void loadImages() {
		wall = loadImage("wall.bmp");
		empty = loadImage("empty.bmp");
		tank = loadImage("tank.bmp");
		missile = loadImage("missile.bmp");

		recolor(missile, NEUTRAL_COLOR);

		tankP1 = copyImage(tank);
		recolor(tankP1, Color.GREEN);
		tankP2 = copyImage(tank);
		recolor(tankP2, Color.RED);

		missileP1 = copyImage(missile);
		recolor(missileP1, Color.GREEN);
		missileP2 = copyImage(missile);
		recolor(missileP2, Color.RED);
	}

	private BufferedImage copyImage(BufferedImage image) {
		return new BufferedImage(image.getColorModel(), image.copyData(null), image.isAlphaPremultiplied(), null);
	}

	private void recolor(BufferedImage image, Color color) {
		recolor(image, color.getRGB());
	}

	private void recolor(BufferedImage image, int color) {
		for(int i = 0; i < image.getWidth(); i++) {
			for(int j = 0; j < image.getHeight(); j++) {
				if(NEUTRAL_COLOR == image.getRGB(i, j)) {
					image.setRGB(i, j, color);
				} else if(WHITE == image.getRGB(i, j)) {
					image.setRGB(i, j, TRANSLUCENT);
				}
			}
		}
	}

	private BufferedImage loadImage(String filename) {
		try {
			return ImageIO.read(this.getClass().getClassLoader().getResource(filename));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void initBoardView() {
		setBackground(Color.BLACK);
		setPreferredSize(new Dimension(800, 800));
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		paintBoard(g);
	}

	private void paintBoard(Graphics g) {
		if(board == null) {
			return;
		}
		BoardSquare[][] squares = board.getSquares();
		for(int i = 0; i < squares.length; i++) {
			for(int j = 0; j < squares[i].length; j++) {
				for(GameObject gameObject : squares[i][j].getGameObjects()) {
					if(GameObjectType.TANK.equals(gameObject.getGameObjectType())) {
						drawAt(g, affiliationToTankImage(gameObject.getAffiliation()), i, j, gameObject);
					} else if(GameObjectType.WALL.equals(gameObject.getGameObjectType())) {
						drawAt(g, wall, i, j, gameObject);
					} else if(GameObjectType.MISSILE.equals(gameObject.getGameObjectType())) {
						drawAt(g, affiliationToMissileImage(gameObject.getAffiliation()), i, j, gameObject);
					}
				}
			}
		}
		Toolkit.getDefaultToolkit().sync();
	}

	private void drawAt(Graphics g, BufferedImage image, int x, int y, GameObject gameObject) {
		Graphics2D g2d = (Graphics2D) g;
		AffineTransform at = AffineTransform.getQuadrantRotateInstance(directionToRotationNumber(gameObject.getDirection()), SQUARE_SIZE_PIXELS / 2, SQUARE_SIZE_PIXELS / 2);
		AffineTransformOp ato = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
		g2d.drawImage(ato.filter(image, null), x * SQUARE_SIZE_PIXELS, y * SQUARE_SIZE_PIXELS, this);
	}

	private int directionToRotationNumber(Direction direction) {
		switch (direction) {
			case NONE:
			case UP:
				return 0;
			case RIGHT:
				return 1;
			case DOWN:
				return 2;
			case LEFT:
				return 3;
			default:
				return 0;
		}
	}

	private BufferedImage affiliationToTankImage(Affiliation affiliation) {
		switch (affiliation) {
			case PLAYER1:
				return tankP1;
			case PLAYER2:
				return tankP2;
			default:
				return tank;
		}
	}

	private BufferedImage affiliationToMissileImage(Affiliation affiliation) {
		switch (affiliation) {
			case PLAYER1:
				return missileP1;
			case PLAYER2:
				return missileP2;
			default:
				return missile;
		}
	}
}
