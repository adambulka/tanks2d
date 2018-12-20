package games.view;

import games.model.Board;

import javax.swing.*;

public class Window extends JFrame {

	private BoardView currentView = new BoardView();

	private KeyboardController keyboardController;

	public Window() {
		initWindow();
	}

	private void initWindow() {
		setResizable(false);
		setTitle("Tanks");
		setLocationRelativeTo(null);
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		add(currentView);
		pack();
		setVisible(true);
	}

	public void drawBoard(Board board) {
		if(keyboardController != null) {
			keyboardController.setBoard(board);
		}
		currentView.setBoard(board);
		SwingUtilities.invokeLater(this::repaint);
	}

	public void setKeyboardController(KeyboardController keyboardController) {
		if(this.keyboardController != null) {
			removeKeyListener(this.keyboardController);
		}
		this.keyboardController = keyboardController;
		addKeyListener(this.keyboardController);
	}

	public void showJoinFailedMessage(String message) {
		JOptionPane.showMessageDialog(this, message, "Join failed", JOptionPane.ERROR_MESSAGE);
	}
}
