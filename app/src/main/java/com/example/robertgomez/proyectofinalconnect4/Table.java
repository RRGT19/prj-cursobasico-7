package com.example.robertgomez.proyectofinalconnect4;

import android.content.Context;

public class Table {
    private int numColumns;
    private int numRows;
    public boolean hasWinner;
    private Cell[][] cells;
    private static boolean isRed;

    /**
     * Set the Color of the first player
     * @param context The ApplicationContext
     * @param colorKey The key of the preference
     */
    public static void setColor(Context context, String colorKey) {
        if (colorKey.equals(context.getString(R.string.pref_color_red_value))) {
            isRed = true;
        } else {
            isRed = false;
        }
    }

    /**
     * Definition of the players
     */
    public enum Turn {
        RED, YELLOW
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
        numColumns = cols;
        numRows = rows;
        cells = new Cell[cols][rows];
        reset();
    }

    /**
     * Resets all the game
     */
    public void reset() {
        hasWinner = false;

        if (isRed) {
            turn = Turn.RED;
        } else {
            turn = Turn.YELLOW;
        }

        for (int col = 0; col < numColumns; col++) {
            for (int row = 0; row < numRows; row++) {
                cells[col][row] = new Cell();
            }
        }
    }

    /**
     * Search an available row
     * @param col The column to search
     * @return The row
     */
    public int lastAvailableRow(int col) {
        for (int row = numRows - 1; row >= 0; row--) {
            if (cells[col][row].empty) {
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
        for (int col = 0; col < numColumns; col++) {
            for (int row = 0; row < numRows; row++) {
                if (cells[col][row].empty) {
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
        cells[col][row].setPlayer(turn);
    }

    /**
     * Toggle the turns between players
     */
    public void toggleTurn() {
        if (turn == Turn.RED) {
            turn = Turn.YELLOW;
        } else {
            turn = Turn.RED;
        }
    }

    /**
     * Check if there is 4 views together
     * @return A boolean
     */
    public boolean checkForWin() {
        // Vertical
        for (int col = 0; col < numColumns; col++) {
            if (isTogether(turn, 0, 1, col, 0, 0) || isTogether(turn, 1, 1, col, 0, 0) || isTogether(turn, -1, 1, col, 0, 0)) {
                hasWinner = true;
                return true;
            }
        }

        // Horizontal
        for (int row = 0; row < numRows; row++) {
            if (isTogether(turn, 1, 0, 0, row, 0) || isTogether(turn, 1, 1, 0, row, 0) || isTogether(turn, -1, 1, numColumns - 1, row, 0)) {
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
        if (col < 0 || col >= numColumns || row < 0 || row >= numRows) {
            return false;
        }

        Cell cell = cells[col][row];

        if (cell.player == player) {
            return isTogether(player, dirX, dirY, col + dirX, row + dirY, count + 1);
        } else {
            return isTogether(player, dirX, dirY, col + dirX, row + dirY, 0);
        }
    }
}
