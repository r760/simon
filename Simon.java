import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import javax.imageio.*;
import javax.sound.sampled.*;
import javax.swing.*;

/**
 * Game -> Simon Says!
 * 
 * @author https://github.com/r760/
 */
public class Simon extends JPanel {
	private static final long serialVersionUID = 1;
	private Font regularFont;
	private JFrame frame;
	private JLabel levelLabel;
	private JLabel bestLevelLabel;
	private JButton playButton;

	private ArrayList<String> audioPaths;
	private ArrayList<JButton> buttons;
	private ArrayList<Integer> expected, actual;

	private int level, bestLevel;
	private boolean isPlaying, isPlayer;

	/**
	 * An action listener for the play button
	 */
	private class PlayButtonListener implements ActionListener {
		/**
		 * If the game has not been started then the game is started, If the game is
		 * started, and the player has finished the current level, they are given the
		 * option to move to the next level
		 */
		public void actionPerformed(ActionEvent e) {
			if (!isPlaying) {
				isPlaying = true;
				isPlayer = false;
				playButton.setText("Playing");

				expected = new ArrayList<>();
				actual = new ArrayList<>();

				Thread thread = new Thread() {
					public void run() {
						try {
							sleep(250);
							int currentLevel = 1;
							while (currentLevel <= level) {
								int r = Math.abs(new Random().nextInt()) % 4;
								expected.add(r);
								JButton button = buttons.get(r);
								button.doClick();
								currentLevel++;
								sleep(1000);
							}
							isPlayer = true;
						} catch (Exception e) {
							System.out.println(e.toString());
						}
					}
				};
				thread.start();
			}
		}
	}

	private PlayButtonListener playButtonListener;

	/**
	 * Read in bestLevel from the file: "log/best_level.txt", If the file exists
	 * 
	 * @return void
	 */
	private void readInBestLevel() {
		try {
			File file = new File("log/best_level.txt");
			if (file.exists()) {
				Scanner scanner = new Scanner(file);
				bestLevel = scanner.nextInt();
				scanner.close();
			}
		} catch (Exception e) {
			System.out.println(e.toString());
		}
	}

	/**
	 * Write bestLevel to the file: "log/best_level.txt", If the file exists,
	 * overwrite it
	 * 
	 * @return void
	 */
	private void writeOutBestLevel() {
		try {
			File file = new File("log/best_level.txt");
			if (file.exists()) {
				file.delete();
				file.createNewFile();
			}
			FileWriter fileWriter = new FileWriter(file);
			fileWriter.write(Integer.toString(bestLevel));
			fileWriter.close();
		} catch (Exception e) {
			System.out.println(e.toString());
		}
	}

	/**
	 * Constructor, initializes variables, and calls init()
	 */
	Simon() {
		regularFont = new Font("Arial", Font.BOLD, 17);
		frame = new JFrame("Simon");
		playButton = new JButton("Play");

		level = 1;
		bestLevel = 1;
		readInBestLevel();

		levelLabel = new JLabel("Level " + level);
		bestLevelLabel = new JLabel("Best Level " + bestLevel);

		audioPaths = new ArrayList<>();
		audioPaths.add("audio/Piano_C5_Key.wav");
		audioPaths.add("audio/Piano_D5_Key.wav");
		audioPaths.add("audio/Piano_E5_Key.wav");
		audioPaths.add("audio/Piano_F5_Key.wav");

		buttons = new ArrayList<>();
		expected = new ArrayList<>();
		actual = new ArrayList<>();

		isPlaying = false;
		isPlayer = false;
		playButtonListener = new PlayButtonListener();
		init();
	}

	/**
	 * Reloads the frame
	 * 
	 * @return void
	 */
	private void reloadFrame() {
		frame.setVisible(false);
		frame.setVisible(true);
	}

	/**
	 * Checks if the user passed the current level
	 * 
	 * @return true if the user passed the current level
	 * @return false if the user failed the current level
	 */
	private boolean userPassedCurrentLevel() {
		boolean userPassedCurrentLevel = expected.size() == actual.size();
		for (int i = 0; userPassedCurrentLevel && i < expected.size(); i++) {
			userPassedCurrentLevel = userPassedCurrentLevel && (expected.get(i) == actual.get(i));
		}
		return userPassedCurrentLevel;
	}

	/**
	 * If the user passed the current level, the user moves onto level = level + 1,
	 * If the user did not pass the current level, the user moves back to level one
	 * 
	 * @return void
	 */
	private void nextLevel() {
		boolean userPassedCurrentLevel = userPassedCurrentLevel();
		String s;

		if (userPassedCurrentLevel) {
			level++;
			if (level > bestLevel) {
				bestLevel = level;
				writeOutBestLevel();
				bestLevelLabel.setText("Best Level " + bestLevel);
			}
			s = "Next Level";
		} else {
			level = 1;
			s = "Play";
		}

		isPlayer = false;
		isPlaying = false;
		levelLabel.setText("Level " + level);
		playButton.setText(s);
	}

	/**
	 * Handles button clicks
	 * 
	 * On a button click, a button press animation and button press sound are
	 * produced, If the user is playing, the button presses are recorded, and
	 * checked against the expected order of button presses
	 * 
	 * @param index - index of button (to be handled) from the buttons list
	 * @return void
	 */
	private void buttonClickHandler(int index) {
		boolean available = isPlaying;
		for (JButton j : buttons) {
			available = available && j.isEnabled();
		}

		if (available) {
			JButton button = buttons.get(index);
			int width = button.getWidth(), height = button.getHeight();
			Thread thread = new Thread() {
				public void run() {
					try {
						int i = actual.size() - 1;
						if (isPlaying && isPlayer && i >= 0 && actual.get(i) == index) {
							sleep(250);
						}

						button.setSize((int) (width * 1.05), (int) (height * 1.05));

						Clip clip = AudioSystem.getClip();
						clip.open(AudioSystem.getAudioInputStream(new File(audioPaths.get(index))));

						clip.addLineListener(new LineListener() {
							public void update(LineEvent e) {
								if (e.getType() == LineEvent.Type.STOP) {
									e.getLine().close();
								}
							}
						});
						clip.start();

						if (isPlayer && actual.size() <= level) {
							actual.add(index);
							if (actual.size() == level) {
								nextLevel();
							}
						}

						sleep(150);
						button.setSize(width, height);
					} catch (Exception e) {
						System.out.println(e.toString());
					}
				}
			};
			for (JButton j : buttons) {
				j.setEnabled(false);
			}

			thread.start();

			for (JButton j : buttons) {
				j.setEnabled(true);
			}
		}

	}

	/**
	 * Initializes Swing components, and their listeners
	 * 
	 * @return void
	 */
	private void init() {
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(550, 805);
		frame.setResizable(false);
		this.setBackground(Color.BLACK);

		int width = 175, height = width, xOffset = 95, yOffset = 330;

		try {
			File file = new File("images/simon.png");
			BufferedImage image = ImageIO.read(file);
			ImageIcon imageIcon = new ImageIcon(image);
			JLabel pic = new JLabel(imageIcon);
			pic.setBounds(50, -30, 400, 400);
			frame.add(pic);
		} catch (Exception e) {
			System.out.println(e.toString());
		}

		levelLabel.setFont(regularFont);
		levelLabel.setForeground(Color.WHITE);
		levelLabel.setBounds(100, 285, 100, 30);
		frame.add(levelLabel);

		bestLevelLabel.setFont(regularFont);
		bestLevelLabel.setForeground(Color.WHITE);
		bestLevelLabel.setBounds(350, 285, 100, 30);
		frame.add(bestLevelLabel);

		JButton greenButton = new JButton("");
		greenButton.setBounds(xOffset, yOffset, width, height);
		greenButton.setFocusPainted(false);
		greenButton.setFont(regularFont);
		greenButton.setBackground(Color.GREEN);
		greenButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// green();
				buttonClickHandler(0);
			}
		});

		JButton redButton = new JButton("");
		redButton.setBounds(xOffset + width + 10, yOffset, width, height);
		redButton.setFocusPainted(false);
		redButton.setFont(regularFont);
		redButton.setBackground(Color.RED);
		redButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// red();
				buttonClickHandler(1);
			}
		});

		JButton yellowButton = new JButton("");
		yellowButton.setBounds(xOffset, yOffset + height + 10, width, height);
		yellowButton.setFocusPainted(false);
		yellowButton.setFont(regularFont);
		yellowButton.setBackground(Color.YELLOW);
		yellowButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// yellow();
				buttonClickHandler(2);
			}
		});

		JButton blueButton = new JButton("");
		blueButton.setBounds(xOffset + width + 10, yOffset + height + 10, width, height);
		blueButton.setFocusPainted(false);
		blueButton.setFont(regularFont);
		blueButton.setBackground(Color.BLUE);
		blueButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// blue();
				buttonClickHandler(3);
			}
		});

		playButton.setBounds(xOffset + (int) (0.5 * width), yOffset + height + height + 22, width, height / 3);
		playButton.setFocusPainted(false);
		playButton.setFont(regularFont);
		playButton.addActionListener(playButtonListener);

		buttons.add(greenButton);
		buttons.add(redButton);
		buttons.add(yellowButton);
		buttons.add(blueButton);

		frame.add(greenButton);
		frame.add(redButton);
		frame.add(yellowButton);
		frame.add(blueButton);

		frame.add(playButton);
		frame.add(this);
		reloadFrame();
	}

	public static void main(String[] args) {
		new Simon();
	}
}