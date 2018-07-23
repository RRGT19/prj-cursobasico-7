package com.example.robertgomez.proyectofinalconnect4;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.BounceInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class GameActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private ImageView[][] cells;
    private View tableView;
    private Table table;
    private TextView winnerTextView;
    private ImageView turnIndicatorImageView;
    private static int NUM_ROWS = 6;
    private static int NUM_COLS = 7;
    private String cellFrameColor = "blue"; // Keep the current cell frame color
    private SharedPreferences sharedPrefStatistics;
    private SharedPreferences.Editor editorSharedPrefStatistics;

    // Determines whether each of these should be activated
    private boolean playSound;
    private boolean vibrate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // Statistics SharedPreferences
        sharedPrefStatistics = this.getPreferences(Context.MODE_PRIVATE);
        // Editor Statistics SharedPreferences
        editorSharedPrefStatistics = sharedPrefStatistics.edit();

        // Initializing SharedPreferences file with default values for each Preference
        // The last parameter, false, tells the PreferenceManager to only apply the default values
        // the first time the method is called. (This way it will not overwrite the settings at a later time)
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        setupSharedPreferences();

        table = new Table(NUM_COLS, NUM_ROWS);
        tableView = findViewById(R.id.gameTable);

        buildCells();

        tableView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {

                    // Is sent when a finger leaves the screen but at least one finger is still touching it
                    case MotionEvent.ACTION_POINTER_UP:

                    // Is sent when the last finger leaves the screen
                    case MotionEvent.ACTION_UP: {

                        int col = colAtX(motionEvent.getX()); // getX() returns your coordinates

                        if (col != -1)
                            drop(col);

                    }
                }
                return true;
            }
        });

        Button resetButton = findViewById(R.id.resetButton);

        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                reset();
            }
        });

        turnIndicatorImageView = findViewById(R.id.turnIndicatorImageView);
        turnIndicatorImageView.setImageResource(resourceForTurn());
        winnerTextView = findViewById(R.id.winnerTextView);
        winnerTextView.setVisibility(View.GONE);
    }

    /**
     * Read the user preferences from SharedPreferences and set them up
     */
    private void setupSharedPreferences() {

        // Get all of the values from shared preferences to set it up
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Set the Play Sound function
        setPlaySound(sharedPreferences.getBoolean(getString(R.string.pref_play_sound_key),
                getResources().getBoolean(R.bool.pref_play_sound_default)));

        // Set the Vibrate function
        setVibrate(sharedPreferences.getBoolean(getString(R.string.pref_vibrate_key),
                getResources().getBoolean(R.bool.pref_vibrate_default)));

        // Set the Color function of the first player
        loadColorFromPreferences(sharedPreferences);

        // Register the listener
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

    }

    /**
     * Set the Play Sound function
     * @param playSound A boolean to know if the user has this function activated
     */
    private void setPlaySound(boolean playSound) {
        this.playSound = playSound;
    }

    /**
     * Set the Vibrate function
     * @param vibrate A boolean to know if the user has this function activated
     */
    private void setVibrate(boolean vibrate) {
        this.vibrate = vibrate;
    }

    /**
     * Set the Color function of the first player
     * @param sharedPreferences The SharedPreferences that received the change
     */
    private void loadColorFromPreferences(SharedPreferences sharedPreferences) {
        table.setColor(getApplicationContext(), sharedPreferences.getString(getString(R.string.pref_color_key),
                getString(R.string.pref_color_red_value)));
    }

    /**
     * Updates the settings if the shared preferences change
     * This method is required when you make a class implement OnSharedPreferenceChangedListener
     * @param sharedPreferences The SharedPreferences that received the change
     * @param key The key of the preference that was changed, added, or removed
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_play_sound_key))) {

            setPlaySound(sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.pref_play_sound_default)));

        } else if (key.equals(getString(R.string.pref_vibrate_key))) {

            setVibrate(sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.pref_vibrate_default)));

        } else if (key.equals(getString(R.string.pref_color_key))) {

            loadColorFromPreferences(sharedPreferences);

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Unregister GameActivity as an OnPreferenceChangedListener to avoid any memory leaks
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.tonality:
                // Get background color code
                ColorDrawable viewColor = (ColorDrawable) tableView.getBackground();
                int currentColorCode = viewColor.getColor();

                // Toggle the background color
                if (currentColorCode == ContextCompat.getColor(getApplicationContext(), R.color.colorBlack)) {
                    tableView.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorWhite));
                } else {
                    tableView.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorBlack));
                }

                return true;

            case R.id.colorFrame:

                LinearLayout[] frontTableRowArray = new LinearLayout[] {
                        findViewById(R.id.front_table_row1),
                        findViewById(R.id.front_table_row2),
                        findViewById(R.id.front_table_row3),
                        findViewById(R.id.front_table_row4),
                        findViewById(R.id.front_table_row5),
                        findViewById(R.id.front_table_row6)
                };

                int[] cellFrameArray = new int[] {
                        R.id.cellFrame1,
                        R.id.cellFrame2,
                        R.id.cellFrame3,
                        R.id.cellFrame4,
                        R.id.cellFrame5,
                        R.id.cellFrame6,
                        R.id.cellFrame7
                };

                String cellFrameColorTemp = cellFrameColor;
                String finalcellFrameColor = null;
                for (int i = 0; i < frontTableRowArray.length; i++) {

                    for (int j = 0; j < cellFrameArray.length; j++) {

                        ImageView cellFrame = frontTableRowArray[i].findViewById(cellFrameArray[j]);

                        switch (cellFrameColorTemp) {
                            case "blue":
                                cellFrame.setImageResource(R.drawable.cell_frame_gray);
                                finalcellFrameColor = "gray";
                                break;

                            case "gray":
                                cellFrame.setImageResource(R.drawable.cell_frame_green);
                                finalcellFrameColor = "green";
                                break;

                                default:
                                    cellFrame.setImageResource(R.drawable.cell_frame_blue);
                                    finalcellFrameColor = "blue";
                                    break;
                        }
                    }
                }
                cellFrameColor = finalcellFrameColor;

                return true;

            case R.id.statistics:
                // Read from shared preferences
                int redWins = readFromSharedPreferences(R.string.sharedPref_red_wins_key);
                int yellowWins = readFromSharedPreferences(R.string.sharedPref_yellow_wins_key);

                AlertDialog.Builder statisticsBuilder = new AlertDialog.Builder(this)
                        .setTitle("Statistics")
                        .setMessage("Red Wins: " + redWins + "\nYellow Wins: " + yellowWins)
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setNeutralButton("Reset", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                editorSharedPrefStatistics.clear().apply();
                                dialog.dismiss();
                                Toast.makeText(GameActivity.this, "Statistics reseted", Toast.LENGTH_SHORT).show();
                            }
                        });
                statisticsBuilder.create().show();
                return true;

            case R.id.settings:
                Intent startSettingsActivity = new Intent(this, SettingsActivity.class);
                startActivity(startSettingsActivity);
                return true;

            case R.id.about:
                AlertDialog.Builder builder = new AlertDialog.Builder(this)
                        .setTitle("About")
                        .setMessage("Developed by Robert Gomez.\nFinal project of Altice Academy.")
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setNeutralButton("Contact me", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent emailIntent = new Intent(Intent.ACTION_SEND);
                                emailIntent.setType("text/plain");
                                emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {"rrgt19@gmail.com"});
                                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Feedback for \"Connect 4\" application on Android");
                                emailIntent.setType("message/rfc822"); // Prompts email client only

                                try {
                                    // Allowing the user to choose which application will handle the Intent
                                    startActivity(Intent.createChooser(emailIntent, "Send your email in:"));
                                } catch (ActivityNotFoundException e) {
                                    Toast.makeText(GameActivity.this, "There is no email client installed", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                builder.create().show();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Build the cells of the table
     */
    private void buildCells() {
        cells = new ImageView[NUM_ROWS][NUM_COLS];

        for (int r = 0; r < NUM_ROWS; r++) {

            // Save each view in a ViewGroup
            // getChildAt(int index): Returns the view at the specified position in the group
            ViewGroup row = (ViewGroup) ((ViewGroup) tableView).getChildAt(r);

            // Allows each child to draw outside of its own bounds, within the parent
            row.setClipChildren(false);

            for (int c = 0; c < NUM_COLS; c++) {

                ImageView imageView = (ImageView) row.getChildAt(c);
                imageView.setImageResource(android.R.color.transparent);
                cells[r][c] = imageView;
            }
        }
    }

    /**
     * Drops the image at the specified column
     * @param col The column
     */
    private void drop(int col) {

        // Check if there is a winner already
        if (table.hasWinner)
            return;

        // Search an available row
        int row = table.lastAvailableRow(col);

        // Check if the row is full
        if (row == -1)
            return;

        // Get the position of the cell that is going to be used
        final ImageView cell = cells[row][col];

        // How far it will be moved
        float move = -(cell.getHeight() * row + cell.getHeight() + 15);

        // Move the view
        cell.setY(move);

        // Sets a drawable as the content of this view
        cell.setImageResource(resourceForTurn());

        // Apply animation to the view
        cell.animate().translationY(0).setInterpolator(new BounceInterpolator()).start();

        // Save in the records that this cell is occupied
        table.occupyCell(col, row);

        // Check if there is 4 views together
        if (table.checkForWin()) {
            win();
        } else {
            changeTurn();
        }

        // Check the user preferences to activate or not the sound
        if (playSound) {
            playSound(R.raw.drop_sound);
        }

        // Check if the table is full, if it is then, there is a draw
        if (table.isTableFull()) {
            winnerTextView.setVisibility(View.VISIBLE);
            winnerTextView.setText(getResources().getString(R.string.draw));

            // Check the user preferences to activate or not the sound
            if (playSound) {
                playSound(R.raw.draw_sound);
            }

            // Check the user preferences to activate or not the vibration
            if (vibrate) {
                vibrate();
            }
        }

    }

    /**
     * When a winner is detected
     */
    private void win() {

        // Check which player has won
        if (table.turn == Table.Turn.RED) {
            delayedDialog();
            writeToSharedPreferences(R.string.sharedPref_red_wins_key);
        } else {
            delayedDialog();
            writeToSharedPreferences(R.string.sharedPref_yellow_wins_key);
        }

        winnerTextView.setVisibility(View.VISIBLE);
        winnerTextView.setText(getResources().getString(R.string.winner));

        // Check the user preferences to activate or not the sound
        if (playSound) {
            playSound(R.raw.winner_sound);
        }

        // Check the user preferences to activate or not the vibration
        if (vibrate) {
            vibrate();
        }

    }

    /**
     * Write to SharedPreferences
     * @param key The key of the preference
     */
    private void writeToSharedPreferences(int key) {

        int wins;
        if (key == R.string.sharedPref_red_wins_key) {
            wins = readFromSharedPreferences(R.string.sharedPref_red_wins_key);
        } else {
            wins = readFromSharedPreferences(R.string.sharedPref_yellow_wins_key);
        }

        editorSharedPrefStatistics.putInt(getString(key), ++wins);
        editorSharedPrefStatistics.apply();

    }

    /**
     * Read from SharedPreferences
     * @param key The key of the preference
     * @return The value saved
     */
    private int readFromSharedPreferences(int key) {
        return sharedPrefStatistics.getInt(getString(key), 0);
    }

    /**
     * Play a sound
     * @param resource The file to be reproduced
     */
    private void playSound(int resource) {
        final MediaPlayer dropSound = MediaPlayer.create(GameActivity.this, resource);
        dropSound.start();

        dropSound.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                // Resets the MediaPlayer to its uninitialized state
                dropSound.reset();
            }
        });
    }

    /**
     * Vibrate the phone
     */
    private void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        boolean hasVibrator = vibrator.hasVibrator();

        // Check if the device has a vibrator to avoid 'java.lang.NullPointerException'
        if (hasVibrator) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(500,VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                // Deprecated in API 26
                vibrator.vibrate(500);
            }

        }
    }

    /**
     * Wait 1000 milliseconds to execute winnerDialog()
     */
    private void delayedDialog() {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                winnerDialog();
            }
        }, 1000);
    }

    /**
     * Show an AlertDialog about the winner
     */
    private void winnerDialog() {

        final AlertDialog dialog;
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.winner_alert_dialog, null);
        dialogBuilder.setView(dialogView);

        dialogBuilder.setTitle("The winner is");
        dialogBuilder.setCancelable(false);

        Button resetButton = dialogView.findViewById(R.id.reset_button);
        ImageView winnerImageView = dialogView.findViewById(R.id.winnerImageView);
        winnerImageView.setImageResource(winnerResourceForTurn());

        dialog = dialogBuilder.create();
        dialog.show();

        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                reset();
            }
        });

    }

    /**
     * Toggle the turns between players
     */
    private void changeTurn() {
        table.toggleTurn();
        turnIndicatorImageView.setImageResource(resourceForTurn());
    }

    /**
     * Column to use to drop the image
     * @param x Coordinates
     * @return The column
     */
    private int colAtX(float x) {
        float colWidth = cells[0][0].getWidth();
        int col = (int) x / (int) colWidth;
        if (col < 0 || col > 6)
            return -1;
        return col;
    }

    /**
     * Which drawable needs to be shown in the "Turn:" section
     * @return The drawable
     */
    private int resourceForTurn() {
        switch (table.turn) {
            case RED:
                return R.drawable.red;
            case YELLOW:
                return R.drawable.yellow;
        }
        return R.drawable.red;
    }

    /**
     * Which drawable needs to be shown in the AlertDialog about the winner
     * @return The drawable
     */
    private int winnerResourceForTurn() {
        switch (table.turn) {
            case RED:
                return R.drawable.red_won;
            case YELLOW:
                return R.drawable.yellow_won;
        }
        return R.drawable.red_won;
    }

    /**
     * Resets all the game
     */
    private void reset() {

        table.reset();
        winnerTextView.setVisibility(View.GONE);
        turnIndicatorImageView.setImageResource(resourceForTurn());
        for (int r = 0; r < NUM_ROWS; r++) {
            for (int c = 0; c < NUM_COLS; c++) {
                cells[r][c].setImageResource(android.R.color.transparent);
            }
        }

    }

}
