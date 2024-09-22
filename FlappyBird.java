package flappyBird;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.*;

public class FlappyBird extends JPanel implements ActionListener, KeyListener {
    int boardWidth = 360;
    int boardHeight = 640;
    double highScore = 0;

    // Images
    Image backgroundImg;
    Image birdImg;
    Image topPipeImg;
    Image bottomPipeImg;

    // Bird class
    int birdX = boardWidth / 8;
    int birdY = boardWidth / 2;
    int birdWidth = 34;
    int birdHeight = 24;

    class Bird {
        int x = birdX;
        int y = birdY;
        int width = birdWidth;
        int height = birdHeight;
        Image img;

        Bird(Image img) {
            this.img = img;
        }
    }

    // Pipe class
    int pipeX = boardWidth;
    int pipeY = 0;
    int pipeWidth = 64;  //scaled by 1/6
    int pipeHeight = 512;

    class Pipe {
        int x = pipeX;
        int y = pipeY;
        int width = pipeWidth;
        int height = pipeHeight;
        Image img;
        boolean passed = false;

        Pipe(Image img) {
            this.img = img;
        }
    }

    // Game logic
    Bird bird;
    int velocityX = -4; // Move pipes to the left (simulates bird moving right)
    int velocityY = 0; // Move bird up/down speed
    int gravity = 1;

    ArrayList<Pipe> pipes;
    Random random = new Random();

    Timer gameLoop;
    Timer placePipeTimer;
    boolean gameOver = false;
    double score = 0;

    FlappyBird() {
        setPreferredSize(new Dimension(boardWidth, boardHeight));
        setFocusable(true);
        addKeyListener(this);

        // Load images
        backgroundImg = new ImageIcon(getClass().getResource("./flappybirdbg.png")).getImage();
        birdImg = new ImageIcon(getClass().getResource("./flappybird.png")).getImage();
        topPipeImg = new ImageIcon(getClass().getResource("./toppipe.png")).getImage();
        bottomPipeImg = new ImageIcon(getClass().getResource("./bottompipe.png")).getImage();

        // Initialize bird and pipes
        bird = new Bird(birdImg);
        pipes = new ArrayList<Pipe>();

        // Load the high score from a file
        loadHighScore();

        // Timer to place pipes
        placePipeTimer = new Timer(1500, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                placePipes();
            }
        });
        placePipeTimer.start();

        // Game loop timer
        gameLoop = new Timer(1000 / 60, this); // 60 FPS
        gameLoop.start();
    }

    // Method to load the high score from a file
    void loadHighScore() {
        try (BufferedReader reader = new BufferedReader(new FileReader("highscore.txt"))) {
            String line = reader.readLine();
            if (line != null) {
                highScore = Double.parseDouble(line);
            }
        } catch (IOException e) {
            System.out.println("No high score file found. Creating a new one.");
        }
    }

    // Method to save the high score to a file
    void saveHighScore() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("highscore.txt"))) {
            writer.write(String.valueOf(highScore));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void placePipes() {
        int randomPipeY = (int) (pipeY - pipeHeight / 4 - Math.random() * (pipeHeight / 2));
        int openingSpace = boardHeight / 4;

        Pipe topPipe = new Pipe(topPipeImg);
        topPipe.y = randomPipeY;
        pipes.add(topPipe);

        Pipe bottomPipe = new Pipe(bottomPipeImg);
        bottomPipe.y = topPipe.y + pipeHeight + openingSpace;
        pipes.add(bottomPipe);
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }

    public void draw(Graphics g) {
        // Background
        g.drawImage(backgroundImg, 0, 0, this.boardWidth, this.boardHeight, null);

        // Bird
        g.drawImage(birdImg, bird.x, bird.y, bird.width, bird.height, null);

        // Pipes
        for (int i = 0; i < pipes.size(); i++) {
            Pipe pipe = pipes.get(i);
            g.drawImage(pipe.img, pipe.x, pipe.y, pipe.width, pipe.height, null);
        }

        g.setColor(Color.white);
        g.setFont(new Font("Arial", Font.PLAIN, 32));

        // Display score
        if (gameOver) {
            g.drawString("Game Over: " + String.valueOf((int) score), 10, 35);
            if (score == highScore) {
                g.drawString("New High Score!", 10, 115);
            }
        } else {
            g.drawString(String.valueOf((int) score), 10, 35);
        }

        // Display high score
        g.drawString("Best: " + String.valueOf((int) highScore), 10, 75);
    }

    public void move() {
        // Bird
        velocityY += gravity;
        bird.y += velocityY;
        bird.y = Math.max(bird.y, 0); // Apply gravity, limit bird to top of the canvas

        // Pipes
        for (int i = 0; i < pipes.size(); i += 2) { // Loop through pipes in pairs (top and bottom)
            Pipe topPipe = pipes.get(i);
            Pipe bottomPipe = pipes.get(i + 1);

            // Move both pipes to the left
            topPipe.x += velocityX;
            bottomPipe.x += velocityX;

            // Check if bird has passed the pipe pair (only check once per pair)
            if (!topPipe.passed && bird.x > topPipe.x + topPipe.width) {
                score += 1; // 1 point per pipe pair passed
                topPipe.passed = true;
                bottomPipe.passed = true;
            }

            // Check for collisions
            if (collision(bird, topPipe) || collision(bird, bottomPipe)) {
                gameOver = true;
            }
        }

        // Check if the bird falls below the canvas
        if (bird.y > boardHeight) {
            gameOver = true;
        }
    }

    boolean collision(Bird a, Pipe b) {
        return a.x < b.x + b.width &&   // a's top-left corner doesn't reach b's top-right corner
               a.x + a.width > b.x &&   // a's top-right corner passes b's top-left corner
               a.y < b.y + b.height &&  // a's top-left corner doesn't reach b's bottom-left corner
               a.y + a.height > b.y;    // a's bottom-left corner passes b's top-left corner
    }

    @Override
    public void actionPerformed(ActionEvent e) { // Called every x milliseconds by gameLoop timer
        move();
        repaint();
        if (gameOver) {
            // Update high score if current score is higher
            if (score > highScore) {
                highScore = score;
                saveHighScore(); // Save the new high score
            }

            placePipeTimer.stop();
            gameLoop.stop();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            velocityY = -9;

            if (gameOver) {
                // Restart game by resetting conditions
                bird.y = birdY;
                velocityY = 0;
                pipes.clear();
                gameOver = false;
                score = 0;
                gameLoop.start();
                placePipeTimer.start();
            }
        }
    }

    // Not needed
    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {}
}
