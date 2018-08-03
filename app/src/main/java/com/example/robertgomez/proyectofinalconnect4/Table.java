package com.example.robertgomez.proyectofinalconnect4;

public class Table {
    private int mNumColumns;
    private int mNumRows;
    public boolean hasWinner;
    private Cell[][] mCells;
    private boolean isRed;

    /**
     * Set the Color of the first player
     * @param isRed If it's red or not
     */
    public void setColor(boolean isRed) {
        this.isRed = isRed;
    }

    /**
     * Definition of the players
     */
    public enum Turn {
        RED, YELLOW, MACHINE
    }

    /**
     * Keep track of the current turn
     */
    public Turn turn;

    /**
     * Constructor
     * @param cols Number of columns
     * @param rows Number of rows
     */
    public Table(int cols, int rows) {
        mNumColumns = cols;
        mNumRows = rows;
        mCells = new Cell[cols][rows];
        reset();
    }

    /**
     * Resets all the game
     */
    public void reset() {
        hasWinner = false;

        // TODO: Check why isRed is always false here
        /*Log.i("color-Reset", Boolean.toString(isRed));
        if (isRed) {
            turn = Turn.RED;
        } else {
            turn = Turn.YELLOW;
        }*/

        if (GameActivity.withMachine) {
            turn = Turn.RED; // RED always go first while playing with the Machine
        } else {
            if (isRed) {
                turn = Turn.RED;
            } else {
                turn = Turn.YELLOW;
            }
        }

        for (int col = 0; col < mNumColumns; col++) {
            for (int row = 0; row < mNumRows; row++) {
                mCells[col][row] = new Cell();
            }
        }
    }

    /**
     * Search an available row
     * @param col The column to search
     * @return The row
     */
    public int lastAvailableRow(int col) {
        for (int row = mNumRows - 1; row >= 0; row--) {
            if (mCells[col][row].empty) {
                return row;
            }
        }
        return -1;
    }

    /**
     * Check if the table is full
     * @return A boolean
     */
    public boolean isTableFull() {
        boolean isFull = true;
        for (int col = 0; col < mNumColumns; col++) {
            for (int row = 0; row < mNumRows; row++) {
                if (mCells[col][row].empty) {
                    isFull = false;
                }
            }
        }
        return isFull;
    }

    /**
     * Save in the records that this cell is occupied
     * @param col The column
     * @param row The row
     */
    public void occupyCell(int col, int row) {
        mCells[col][row].setPlayer(turn);
    }

    /**
     * Undo the last move
     * @param col The column
     * @param row The row
     */
    public void undoCell(int col, int row) {
        mCells[col][row] = new Cell();
    }

    /**
     * Toggle the turns between players
     */
    public void toggleTurn() {
        if (GameActivity.withMachine) { // If the user wants to play with the Machine
            if (turn == Turn.RED) {
                turn = Turn.MACHINE;
            } else {
                turn = Turn.RED;
            }
        } else { // If the user wants to play with a Friend
            if (turn == Turn.RED) {
                turn = Turn.YELLOW;
            } else {
                turn = Turn.RED;
            }
        }
    }

    /**
     * Check if there is 4 views together
     * @return A boolean
     */
    public boolean checkForWin() {
        // Vertical
        for (int col = 0; col < mNumColumns; col++) {
            if (isTogether(turn, 0, 1, col, 0, 0) || isTogether(turn, 1, 1, col, 0, 0) || isTogether(turn, -1, 1, col, 0, 0)) {
                hasWinner = true;
                return true;
            }
        }

        // Horizontal
        for (int row = 0; row < mNumRows; row++) {
            if (isTogether(turn, 1, 0, 0, row, 0) || isTogether(turn, 1, 1, 0, row, 0) || isTogether(turn, -1, 1, mNumColumns - 1, row, 0)) {
                hasWinner = true;
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if the views are together
     * @param player The current player
     * @param dirX X position
     * @param dirY Y position
     * @param col The column
     * @param row The row
     * @param count Number of views together
     * @return A boolean
     */
    private boolean isTogether(Turn player, int dirX, int dirY, int col, int row, int count) {
        if (count >= 4) {
            return true;
        }
        if (col < 0 || col >= mNumColumns || row < 0 || row >= mNumRows) {
            return false;
        }

        Cell cell = mCells[col][row];

        if (cell.player == player) {
            return isTogether(player, dirX, dirY, col + dirX, row + dirY, count + 1);
        } else {
            return isTogether(player, dirX, dirY, col + dirX, row + dirY, 0);
        }
    }
}
