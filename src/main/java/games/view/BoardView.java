package games.view;

import games.model.*;
import games.model.token.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.util.EnumMap;
import java.util.Map;

public class BoardView extends JPanel {

	private static final int SQUARE_SIZE_PIXELS = 40;

	private static final int NEUTRAL_COLOR = 0xff969696;
	private static final int WHITE = 0xffffffff;
	private static final int TRANSLUCENT = 0x00ffffff;

	private BufferedImage wallImage;
	private BufferedImage emptyImage;
	private BufferedImage tankImage;
	private BufferedImage missileImage;

	private Map<Affiliation, BufferedImage> affiliationToTankImage = new EnumMap<>(Affiliation.class);
	private Map<Affiliation, BufferedImage> affiliationToMissileImage = new EnumMap<>(Affiliation.class);

	private Board board;

	public BoardView() {
		initBoardView();
		loadImages();
	}

	public void setBoard(Board board) {
		this.board = board;
	}

	private void loadImages() {
		wallImage = loadImage("wall.bmp");
		emptyImage = loadImage("empty.bmp");
		tankImage = loadImage("tank.bmp");
		missileImage = loadImage("missile.bmp");

		tankImage = imageToBufferedImage(toTransparentImage(tankImage));
		missileImage = imageToBufferedImage(toTransparentImage(missileImage));

		BufferedImage tankP1Image = copyImage(tankImage);
		recolor(tankP1Image, Color.GREEN);
		BufferedImage tankP2Image = copyImage(tankImage);
		recolor(tankP2Image, Color.RED);

		BufferedImage missileP1Image = copyImage(missileImage);
		recolor(missileP1Image, Color.GREEN);
		BufferedImage missileP2Image = copyImage(missileImage);
		recolor(missileP2Image, Color.RED);

		affiliationToTankImage.put(Affiliation.ENEMY, tankImage);
		affiliationToTankImage.put(Affiliation.PLAYER1, tankP1Image);
		affiliationToTankImage.put(Affiliation.PLAYER2, tankP2Image);

		affiliationToMissileImage.put(Affiliation.ENEMY, missileImage);
		affiliationToMissileImage.put(Affiliation.PLAYER1, missileP1Image);
		affiliationToMissileImage.put(Affiliation.PLAYER2, missileP2Image);
	}

	private Image toTransparentImage(BufferedImage image) {
		ImageFilter imageFilter = new RGBImageFilter() {
			@Override
			public int filterRGB(int x, int y, int rgb) {
				if(WHITE == rgb) {
					return TRANSLUCENT;
				} else {
					return rgb;
				}
			}
		};
		ImageProducer ip = new FilteredImageSource(image.getSource(), imageFilter);
		return Toolkit.getDefaultToolkit().createImage(ip);
	}

	private BufferedImage imageToBufferedImage(Image image) {
		BufferedImage bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = bufferedImage.createGraphics();
		g2.drawImage(image, 0, 0, null);
		g2.dispose();
		return bufferedImage;
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
				for(Token token : squares[i][j].getTokens()) {
					if(TokenType.TANK.equals(token.getTokenType())) {
						drawTank(g, (Tank) token);
					} else if(TokenType.WALL.equals(token.getTokenType())) {
						drawWall(g, (Wall) token);
					} else if(TokenType.MISSILE.equals(token.getTokenType())) {
						drawMissile(g, (Missile) token);
					}
				}
			}
		}
		Toolkit.getDefaultToolkit().sync();
	}

	private void drawTank(Graphics g, Tank tank) {
		BufferedImage image = affiliationToTankImage.get(tank.getAffiliation());
		draw(g, getTransform(tank.getDirection()), image, getDrawX(tank), getDrawY(tank));
	}

	private void drawMissile(Graphics g, Missile missile) {
		BufferedImage image = affiliationToMissileImage.get(missile.getAffiliation());
		draw(g, getTransform(missile.getDirection()), image, getDrawX(missile), getDrawY(missile));
	}

	private AffineTransformOp getTransform(Direction direction) {
		AffineTransform at = AffineTransform.getQuadrantRotateInstance(directionToRotationNumber(direction), SQUARE_SIZE_PIXELS / 2, SQUARE_SIZE_PIXELS / 2);
		return new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
	}

	private void drawWall(Graphics g, Wall wall) {
		draw(g, null, wallImage, wall.getPosition().getX() * SQUARE_SIZE_PIXELS, wall.getPosition().getY() * SQUARE_SIZE_PIXELS);
	}

	private void draw(Graphics g, AffineTransformOp ato, BufferedImage image, int x, int y) {
		Graphics2D g2d = (Graphics2D) g;
		if(ato == null) {
			g2d.drawImage(image, x, y, this);
		} else {
			g2d.drawImage(ato.filter(image, null), x, y, this);
		}
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

	private int getDrawX(MovingToken movingToken) {
		return movingToken.getPosition().getX() * SQUARE_SIZE_PIXELS + movingToken.getPosition().getDevX() * SQUARE_SIZE_PIXELS / 200;
	}

	private int getDrawY(MovingToken movingToken) {
		return movingToken.getPosition().getY() * SQUARE_SIZE_PIXELS + movingToken.getPosition().getDevY() * SQUARE_SIZE_PIXELS / 200;
	}
}
