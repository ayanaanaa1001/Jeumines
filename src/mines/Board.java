package mines;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class Board extends JPanel {
    private static final long serialVersionUID = 6195235521361212179L;

    private final int NUM_IMAGES = 13;
    private final int CELL_SIZE = 15;

    private final int COVER_FOR_CELL = 10;
    private final int MARK_FOR_CELL = 10;
    private final int EMPTY_CELL = 0;
    private final int MINE_CELL = 9;
    private final int COVERED_MINE_CELL = MINE_CELL + COVER_FOR_CELL;
    private final int MARKED_MINE_CELL = COVERED_MINE_CELL + MARK_FOR_CELL;

    private final int DRAW_MINE = 9;
    private final int DRAW_COVER = 10;
    private final int DRAW_MARK = 11;
    private final int DRAW_WRONG_MARK = 12;

    private int[] field;
    private boolean inGame;
    private int mines_left;
    private Image[] img;
    private int mines = 40;
    private int rows = 16;
    private int cols = 16;
    private int all_cells;
    private JLabel statusbar;

    public Board(JLabel statusbar) {
        this.statusbar = statusbar;
        img = new Image[NUM_IMAGES];

        for (int i = 0; i < NUM_IMAGES; i++) {
            img[i] = (new ImageIcon(getClass().getClassLoader().getResource((i) + ".gif"))).getImage();
        }

        setDoubleBuffered(true);
        addMouseListener(new MinesAdapter());
        newGame();
    }

    public void newGame() {
        Random random = new Random();
        inGame = true;
        mines_left = mines;
        all_cells = rows * cols;
        field = new int[all_cells];

        // Initialize all cells as covered
        for (int i = 0; i < all_cells; i++) {
            field[i] = COVER_FOR_CELL;
        }

        statusbar.setText(Integer.toString(mines_left));

        placeMines(random);
        incrementAdjacentCells();
    }

    // Helper method to place mines randomly
    private void placeMines(Random random) {
        int placedMines = 0;
        while (placedMines < mines) {
            int position = random.nextInt(all_cells);
            if (field[position] != COVERED_MINE_CELL) {
                field[position] = COVERED_MINE_CELL;
                placedMines++;
            }
        }
    }

    // Helper method to increment numbers in cells adjacent to mines
    private void incrementAdjacentCells() {
        for (int position = 0; position < all_cells; position++) {
            if (field[position] == COVERED_MINE_CELL) {
                int row = position / cols;
                int col = position % cols;
                updateAdjacentCells(row, col);
            }
        }
    }

    // Helper method to update adjacent cells around a mine
    private void updateAdjacentCells(int row, int col) {
        int[] delta = {-1, 0, 1}; // Check neighboring rows and columns
        for (int dRow : delta) {
            for (int dCol : delta) {
                if (dRow == 0 && dCol == 0) continue; // Skip the mine cell itself
                int newRow = row + dRow;
                int newCol = col + dCol;
                if (isWithinBounds(newRow, newCol)) {
                    int newPosition = newRow * cols + newCol;
                    if (field[newPosition] != COVERED_MINE_CELL) {
                        field[newPosition] += 1;
                    }
                }
            }
        }
    }

    // Helper method to check if a cell is within the board bounds
    private boolean isWithinBounds(int row, int col) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    public void find_empty_cells(int j) {
        int current_col = j % cols;
        Queue<Integer> toVisit = new LinkedList<>();
        toVisit.add(j);

        while (!toVisit.isEmpty()) {
            int current = toVisit.poll();
            int cell;

            // Process all adjacent cells around the current cell
            for (int dRow = -1; dRow <= 1; dRow++) {
                for (int dCol = -1; dCol <= 1; dCol++) {
                    if (dRow == 0 && dCol == 0) continue; // Skip the cell itself

                    int newRow = (current / cols) + dRow;
                    int newCol = (current % cols) + dCol;

                    if (isWithinBounds(newRow, newCol)) {
                        int newPosition = newRow * cols + newCol;
                        cell = field[newPosition];

                        if (cell > MINE_CELL) {
                            field[newPosition] -= COVER_FOR_CELL;
                            if (field[newPosition] == EMPTY_CELL) {
                                toVisit.add(newPosition);  // Add to queue for further checking
                            }
                        }
                    }
                }
            }
        }
    }

    public void paint(Graphics g) {
        int cell;
        int uncover = 0;

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                cell = field[(i * cols) + j];
                if (inGame && cell == MINE_CELL) inGame = false;

                if (!inGame) {
                    if (cell == COVERED_MINE_CELL) cell = DRAW_MINE;
                    else if (cell == MARKED_MINE_CELL) cell = DRAW_MARK;
                    else if (cell > COVERED_MINE_CELL) cell = DRAW_WRONG_MARK;
                    else if (cell > MINE_CELL) cell = DRAW_COVER;
                } else {
                    if (cell > COVERED_MINE_CELL) cell = DRAW_MARK;
                    else if (cell > MINE_CELL) {
                        cell = DRAW_COVER;
                        uncover++;
                    }
                }
                g.drawImage(img[cell], (j * CELL_SIZE), (i * CELL_SIZE), this);
            }
        }

        if (uncover == 0 && inGame) {
            inGame = false;
            statusbar.setText("Game won");
        } else if (!inGame) {
            statusbar.setText("Game lost");
        }
    }

    class MinesAdapter extends MouseAdapter {
        public void mousePressed(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();
            int cCol = x / CELL_SIZE;
            int cRow = y / CELL_SIZE;
            boolean rep = false;

            if (!inGame) {
                newGame();
                repaint();
                return;
            }

            if (x < cols * CELL_SIZE && y < rows * CELL_SIZE) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    if (field[(cRow * cols) + cCol] > MINE_CELL) {
                        rep = true;
                        if (field[(cRow * cols) + cCol] <= COVERED_MINE_CELL) {
                            if (mines_left > 0) {
                                field[(cRow * cols) + cCol] += MARK_FOR_CELL;
                                mines_left--;
                                statusbar.setText(Integer.toString(mines_left));
                            } else {
                                statusbar.setText("No marks left");
                            }
                        } else {
                            field[(cRow * cols) + cCol] -= MARK_FOR_CELL;
                            mines_left++;
                            statusbar.setText(Integer.toString(mines_left));
                        }
                    }
                } else {
                    if (field[(cRow * cols) + cCol] > COVERED_MINE_CELL) return;
                    if (field[(cRow * cols) + cCol] > MINE_CELL && field[(cRow * cols) + cCol] < MARKED_MINE_CELL) {
                        field[(cRow * cols) + cCol] -= COVER_FOR_CELL;
                        rep = true;
                        if (field[(cRow * cols) + cCol] == MINE_CELL) inGame = false;
                        if (field[(cRow * cols) + cCol] == EMPTY_CELL) find_empty_cells((cRow * cols) + cCol);
                    }
                }

                if (rep) repaint();
            }
        }
    }
}
