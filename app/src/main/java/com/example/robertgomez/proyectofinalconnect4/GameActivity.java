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
import android.util.Log;
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

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;

import java.util.Random;

public class GameActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int NUM_ROWS = 6;
    private static final int NUM_COLS = 7;

    private ImageView[][] mCells;
    private View mTableView;
    private Table mTable;
    private TextView mWinnerTextView;
    private ImageView mTurnIndicatorImageView;
    private String mCellFrameColor; // Keep the current cell frame color
    private SharedPreferences mSharedPrefStatistics;
    private SharedPreferences.Editor mEditorSharedPrefStatistics;

    // Determines whether each of these should be activated
    private boolean mPlaySound;
    private boolean mVibrate;

    // Keep track the last move for the undo function
    private Button mUndoButton;
    private int mColForUndo;
    private int mRowForUndo;
    private ImageView mCellForUndo;

    // AdMob
    private InterstitialAd mInterstitialAd;
    private int mNumberOfPlays = 0;
    private static final String ADMOB_APP_ID = "ca-app-pub-3940256099942544~3347511713"; // Sample
    private static final String ADMOB_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"; // Sample
    private static final int MIN_PLAYS_NEEDED_ADMOB = 2;

    // To know if the user wants to play with the Machine
    public static boolean withMachine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // See if the user wants to play with the Machine
        if (getIntent().getExtras() != null)
            withMachine = getIntent().getExtras().getBoolean("withMachine");

        // AdMob
        MobileAds.initialize(this, ADMOB_APP_ID);

        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(ADMOB_AD_UNIT_ID);
        mInterstitialAd.loadAd(new AdRequest.Builder().build());

        // Loads a new interstitial after displaying the previous one
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                // Load the next interstitial
                mInterstitialAd.loadAd(new AdRequest.Builder()
                        .addTestDevice(AdRequest.DEVICE_ID_EMULATOR) // Enable test ads during development
                        .build());
            }
        });

        // Statistics SharedPreferences
        mSharedPrefStatistics = this.getPreferences(Context.MODE_PRIVATE);
        // Editor Statistics SharedPreferences
        mEditorSharedPrefStatistics = mSharedPrefStatistics.edit();

        // Initializing SharedPreferences file with default values for each Preference
        // The last parameter, false, tells the PreferenceManager to only apply the default values
        // the first time the method is called. (This way it will not overwrite the settings at a later time)
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        mTable = new Table(NUM_COLS, NUM_ROWS);
        mTableView = findViewById(R.id.gameTable);

        buildCells();

        mTableView.setOnTouchListener(new View.OnTouchListener() {
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
        mUndoButton = findViewById(R.id.undoButton);

        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                reset();
            }
        });

        if (!withMachine) {
            mUndoButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mCellForUndo != null) {
                        mCellForUndo.setImageResource(android.R.color.transparent);
                        mTable.undoCell(mColForUndo, mRowForUndo);
                        changeTurn();
                        mCellForUndo = null;
                        mUndoButton.setEnabled(false);
                    }
                }
            });
        } else mUndoButton.setVisibility(View.INVISIBLE); // Hide the Undo button while playing with the Machine

        mTurnIndicatorImageView = findViewById(R.id.turnIndicatorImageView);
        mTurnIndicatorImageView.setImageResource(resourceForTurn());
        mWinnerTextView = findViewById(R.id.winnerTextView);
        mWinnerTextView.setVisibility(View.GONE);

        mCellFrameColor = getString(R.string.pref_blue_table_color_value);

        setupSharedPreferences();
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

        // Set the Background Color
        loadBackgroundColorFromPreferences(sharedPreferences);

        // Set the Table Color
        loadTableColorFromPreferences(sharedPreferences);

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
        this.mPlaySound = playSound;
    }

    /**
     * Set the Vibrate function
     * @param vibrate A boolean to know if the user has this function activated
     */
    private void setVibrate(boolean vibrate) {
        this.mVibrate = vibrate;
    }

    /**
     * Set the Background Color
     * @param sharedPreferences The SharedPreferences that received the change
     */
    private void loadBackgroundColorFromPreferences(SharedPreferences sharedPreferences) {
        String backgroundColor;
        backgroundColor = sharedPreferences.getString(getString(R.string.pref_background_color_key),
                getString(R.string.pref_white_background_color_value));

        if (backgroundColor.equals(getString(R.string.pref_white_background_color_value)))
            mTableView.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorWhite));

        else
            mTableView.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorBlack));
    }

    /**
     * Set the Table Color
     * @param sharedPreferences The SharedPreferences that received the change
     */
    private void loadTableColorFromPreferences(SharedPreferences sharedPreferences) {
        String tableColor;
        tableColor = sharedPreferences.getString(getString(R.string.pref_table_color_key),
                getString(R.string.pref_blue_table_color_value));
        setTableColor(tableColor, true);
    }

    /**
     * Set the Color function of the first player
     * @param sharedPreferences The SharedPreferences that received the change
     */
    private void loadColorFromPreferences(SharedPreferences sharedPreferences) {
        mTable.setColor((sharedPreferences.getString(getString(R.string.pref_color_key),
                getString(R.string.pref_color_red_value)).equals("red")) ? true : false);
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

        } else if (key.equals(getString(R.string.pref_background_color_key))) {

            loadBackgroundColorFromPreferences(sharedPreferences);

        } else if (key.equals(getString(R.string.pref_table_color_key))) {

            loadTableColorFromPreferences(sharedPreferences);

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
                ColorDrawable viewColor = (ColorDrawable) mTableView.getBackground();
                int currentColorCode = viewColor.getColor();

                // Toggle the background color
                if (currentColorCode == ContextCompat.getColor(getApplicationContext(), R.color.colorBlack)) {
                    mTableView.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorWhite));
                } else {
                    mTableView.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorBlack));
                }

                return true;

            case R.id.colorFrame:

                setTableColor(mCellFrameColor, false);

                return true;

            case R.id.statistics:
                // Read from shared preferences
                int redWins = readFromSharedPreferences(R.string.sharedPref_red_wins_key);
                int yellowWins = readFromSharedPreferences(R.string.sharedPref_yellow_wins_key);
                int machineWins = readFromSharedPreferences(R.string.sharedPref_machine_wins_key);
                int drawWins = readFromSharedPreferences(R.string.sharedPref_draw_key);

                AlertDialog.Builder statisticsBuilder = new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.statistics))
                        .setMessage(getString(R.string.red_wins) + " " + redWins +
                                "\n" + getString(R.string.yellow_wins) + " " + yellowWins +
                                "\n" + getString(R.string.machine_wins) + " " + machineWins +
                                "\n" + getString(R.string.statistics_draw) + " " + drawWins)
                        .setPositiveButton(getString(R.string.positive_button), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setNeutralButton(getString(R.string.reset), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mEditorSharedPrefStatistics.clear().apply();
                                dialog.dismiss();
                                Toast.makeText(GameActivity.this, getString(R.string.statistics_reseted), Toast.LENGTH_SHORT).show();
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
                        .setTitle(getString(R.string.about_title))
                        .setMessage(getString(R.string.about_message))
                        .setPositiveButton(getString(R.string.about_positive_button), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setNeutralButton(getString(R.string.about_neutral_button), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent emailIntent = new Intent(Intent.ACTION_SEND);
                                emailIntent.setType("text/plain");
                                emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {getString(R.string.about_EXTRA_EMAIL)});
                                emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.about_EXTRA_SUBJECT));
                                emailIntent.setType("message/rfc822"); // Prompts email client only

                                try {
                                    // Allowing the user to choose which application will handle the Intent
                                    startActivity(Intent.createChooser(emailIntent, getString(R.string.about_email_chooser_title)));
                                } catch (ActivityNotFoundException e) {
                                    Toast.makeText(GameActivity.this, getString(R.string.about_email_intent_failed), Toast.LENGTH_SHORT).show();
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
     * Set a new table color
     * @param cellFrameColor Value of the color
     * @param isToSet If it's to set the color from SharedPreferences or from Action Bar
     */
    private void setTableColor(String cellFrameColor, Boolean isToSet) {
        Log.i("color", cellFrameColor);
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

        String finalCellFrameColor = "";
        for (int i = 0; i < frontTableRowArray.length; i++) {

            for (int j = 0; j < cellFrameArray.length; j++) {

                ImageView cellFrame = frontTableRowArray[i].findViewById(cellFrameArray[j]);

                if (isToSet) { // If it's to set the color from SharedPreferences

                    if (cellFrameColor.equals(getString(R.string.pref_blue_table_color_value)))
                        cellFrame.setImageResource(R.drawable.cell_frame_blue);

                    else if (cellFrameColor.equals(getString(R.string.pref_gray_table_color_value)))
                        cellFrame.setImageResource(R.drawable.cell_frame_gray);

                    else
                        cellFrame.setImageResource(R.drawable.cell_frame_green);

                } else { // If it's to set the color from Action Bar

                    if (this.mCellFrameColor.equals(getString(R.string.pref_blue_table_color_value))) {

                        cellFrame.setImageResource(R.drawable.cell_frame_gray);
                        finalCellFrameColor = getString(R.string.pref_gray_table_color_value);

                    } else if (this.mCellFrameColor.equals(getString(R.string.pref_gray_table_color_value))) {

                        cellFrame.setImageResource(R.drawable.cell_frame_green);
                        finalCellFrameColor = getString(R.string.pref_green_table_color_value);

                    } else {

                        cellFrame.setImageResource(R.drawable.cell_frame_blue);
                        finalCellFrameColor = getString(R.string.pref_blue_table_color_value);

                    }

                }
            }
        }

        // Save the new current color
        if (isToSet)
            this.mCellFrameColor = cellFrameColor;
        else
            this.mCellFrameColor = finalCellFrameColor;
    }

    /**
     * Build the cells of the table
     */
    private void buildCells() {
        mCells = new ImageView[NUM_ROWS][NUM_COLS];

        for (int r = 0; r < NUM_ROWS; r++) {

            // Save each view in a ViewGroup
            // getChildAt(int index): Returns the view at the specified position in the group
            ViewGroup row = (ViewGroup) ((ViewGroup) mTableView).getChildAt(r);

            // Allows each child to draw outside of its own bounds, within the parent
            row.setClipChildren(false);

            for (int c = 0; c < NUM_COLS; c++) {

                ImageView imageView = (ImageView) row.getChildAt(c);
                imageView.setImageResource(android.R.color.transparent);
                mCells[r][c] = imageView;
            }
        }
    }

    /**
     * Drops the image at the specified column
     * @param col The column
     */
    private void drop(int col) {

        // Check if there is a winner already
        if (mTable.hasWinner)
            return;

        // Search an available row
        int row = mTable.lastAvailableRow(col);

        // Check if the row is full
        if (row == -1)
            return;

        // Get the position of the cell that is going to be used
        final ImageView cell = mCells[row][col];

        // How far it will be moved
        float move = -(cell.getHeight() * row + cell.getHeight() + 15);

        // Move the view
        cell.setY(move);

        // Sets a drawable as the content of this view
        cell.setImageResource(resourceForTurn());

        // Apply animation to the view
        cell.animate().translationY(0).setInterpolator(new BounceInterpolator()).start();

        // Save in the records that this cell is occupied
        mTable.occupyCell(col, row);

        if (!withMachine) {
            // Keep track the last move for the undo function
            mColForUndo = col;
            mRowForUndo = row;
            mCellForUndo = mCells[row][col];
            mUndoButton.setEnabled(true);
        }

        // Check if there is 4 views together
        if (mTable.checkForWin()) {
            win();
        } else {
            changeTurn();
        }

        // Check the user preferences to activate or not the sound
        if (mPlaySound) {
            playSound(R.raw.drop_sound);
        }

        // Check if the table is full, if it is then, there is a draw
        if (mTable.isTableFull()) {
            mUndoButton.setEnabled(false);
            mWinnerTextView.setVisibility(View.VISIBLE);
            mWinnerTextView.setText(getResources().getString(R.string.draw));
            writeToSharedPreferences(R.string.sharedPref_draw_key);

            // Check the user preferences to activate or not the sound
            if (mPlaySound) {
                playSound(R.raw.draw_sound);
            }

            // Check the user preferences to activate or not the vibration
            if (mVibrate) {
                vibrate();
            }
        }

        // Drop the Machine after 1000 milliseconds
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (withMachine) dropMachine();
            }
        }, 1000);

    }

    /**
     * Drops the image of the Machine at a random column
     * TODO: Add more complexity to the Machine (Easy, Medium, Hard)
     */
    private void dropMachine() {
        // Use a random number to make the Machine unpredictable
        Random random = new Random();

        // Formula: .nextInt((max - min) + 1) + min
        int col = random.nextInt(NUM_ROWS - 0 + 1) + 0; // Between 0 and 6

        // Check if there is a winner already
        if (mTable.hasWinner)
            return;

        // Search an available row
        int row = mTable.lastAvailableRow(col);

        // Check if the row is full
        if (row == -1)
            return;

        // Get the position of the cell that is going to be used
        final ImageView cell = mCells[row][col];

        // How far it will be moved
        float move = -(cell.getHeight() * row + cell.getHeight() + 15);

        cell.setY(move);
        cell.setImageResource(resourceForTurn());

        // Drop after 1000 milliseconds
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                cell.animate().translationY(0).setInterpolator(new BounceInterpolator()).start();

                // Check the user preferences to activate or not the sound
                if (mPlaySound) {
                    playSound(R.raw.drop_sound);
                }


            }
        }, 1000);

        // Save in the records that this cell is occupied
        mTable.occupyCell(col, row);

        // Check if there is 4 views together
        if (mTable.checkForWin()) {
            win();
        } else {
            changeTurn();
        }

        // Check if the table is full, if it is then, there is a draw
        if (mTable.isTableFull()) {
            mUndoButton.setEnabled(false);
            mWinnerTextView.setVisibility(View.VISIBLE);
            mWinnerTextView.setText(getResources().getString(R.string.draw));
            writeToSharedPreferences(R.string.sharedPref_draw_key);

            // Check the user preferences to activate or not the sound
            if (mPlaySound) {
                playSound(R.raw.draw_sound);
            }

            // Check the user preferences to activate or not the vibration
            if (mVibrate) {
                vibrate();
            }
        }

    }

    /**
     * When a winner is detected
     */
    private void win() {

        // Increase the number of plays (to show AdMod after)
        mNumberOfPlays++;

        if (withMachine) { // If the user is playing with the Machine
            // Check which player has won
            if (mTable.turn == Table.Turn.RED) {
                delayedDialog();
                writeToSharedPreferences(R.string.sharedPref_red_wins_key);
            } else {
                delayedDialog();
                writeToSharedPreferences(R.string.sharedPref_machine_wins_key);
            }
        } else { // If the user is playing with a Friend
            // Check which player has won
            if (mTable.turn == Table.Turn.RED) {
                delayedDialog();
                writeToSharedPreferences(R.string.sharedPref_red_wins_key);
            } else {
                delayedDialog();
                writeToSharedPreferences(R.string.sharedPref_yellow_wins_key);
            }
        }

        mWinnerTextView.setVisibility(View.VISIBLE);
        mWinnerTextView.setText(getResources().getString(R.string.winner));

        // Check the user preferences to activate or not the sound
        if (mPlaySound) {
            playSound(R.raw.winner_sound);
        }

        // Check the user preferences to activate or not the vibration
        if (mVibrate) {
            vibrate();
        }

    }

    /**
     * Write to SharedPreferences
     * @param key The key of the preference
     */
    private void writeToSharedPreferences(int key) {

        int wins = 0;
        switch (key) {
            case R.string.sharedPref_red_wins_key:
                wins = readFromSharedPreferences(R.string.sharedPref_red_wins_key);
                break;

            case R.string.sharedPref_yellow_wins_key:
                wins = readFromSharedPreferences(R.string.sharedPref_yellow_wins_key);
                break;

            case R.string.sharedPref_machine_wins_key:
                wins = readFromSharedPreferences(R.string.sharedPref_machine_wins_key);
                break;

            case R.string.sharedPref_draw_key:
                wins = readFromSharedPreferences(R.string.sharedPref_draw_key);
                break;
        }

        mEditorSharedPrefStatistics.putInt(getString(key), ++wins);
        mEditorSharedPrefStatistics.apply();

    }

    /**
     * Read from SharedPreferences
     * @param key The key of the preference
     * @return The value saved
     */
    private int readFromSharedPreferences(int key) {
        return mSharedPrefStatistics.getInt(getString(key), 0);
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

        dialogBuilder.setTitle(getString(R.string.winner_dialog_title));
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
        mTable.toggleTurn();
        mTurnIndicatorImageView.setImageResource(resourceForTurn());
    }

    /**
     * Column to use to drop the image
     * @param x Coordinates
     * @return The column
     */
    private int colAtX(float x) {
        float colWidth = mCells[0][0].getWidth();
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
        switch (mTable.turn) {
            case RED:
                return R.drawable.red;
            case YELLOW:
                return R.drawable.yellow;
            case MACHINE:
                return R.drawable.machine;
        }
        return R.drawable.red;
    }

    /**
     * Which drawable needs to be shown in the AlertDialog about the winner
     * @return The drawable
     */
    private int winnerResourceForTurn() {
        switch (mTable.turn) {
            case RED:
                return R.drawable.red_won;
            case YELLOW:
                return R.drawable.yellow_won;
            case MACHINE:
                return R.drawable.machine_won;
        }
        return R.drawable.red_won;
    }

    /**
     * Resets all the game
     */
    private void reset() {

        // Show AdMob
        if (mNumberOfPlays > MIN_PLAYS_NEEDED_ADMOB) {

            // Check for internet connection
            if (NetworkUtils.isNetworkConnected(this)) {

                if (mInterstitialAd != null && mInterstitialAd.isLoaded()) {
                    mInterstitialAd.show();

                    // Clean the number of plays to start again
                    mNumberOfPlays = 0;
                }
            }
        }

        mTable.reset();
        mUndoButton.setEnabled(false);
        mWinnerTextView.setVisibility(View.GONE);
        mTurnIndicatorImageView.setImageResource(resourceForTurn());
        for (int r = 0; r < NUM_ROWS; r++) {
            for (int c = 0; c < NUM_COLS; c++) {
                mCells[r][c].setImageResource(android.R.color.transparent);
            }
        }

    }

    /**
     * Catch device back button to go to MainActivity
     */
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(GameActivity.this, MainActivity.class);
        startActivity(intent);
    }

}