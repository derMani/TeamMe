package de.pasligh.android.teamme;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import de.pasligh.android.teamme.backend.BackendFacade;
import de.pasligh.android.teamme.objects.Game;
import de.pasligh.android.teamme.objects.Player;
import de.pasligh.android.teamme.objects.PlayerAssignemnt;
import de.pasligh.android.teamme.tools.Flags;
import de.pasligh.android.teamme.tools.HoloCircleSeekBar;
import de.pasligh.android.teamme.tools.TTS_Tool;
import de.pasligh.android.teamme.tools.TeamReactor;

public class TeamChooser extends AppCompatActivity implements SensorEventListener,
        OnEditorActionListener, AnimationListener, OnClickListener {

    private SensorManager sensorManager;
    private long lastUpdate;
    private Animation animationShake1;
    private Animation animationShake2;
    private Animation animationSlideOutLeft;
    private Animation animationSlideOutRight;
    private Animation animationGlow;
    private MediaPlayer mPlayerLeft;
    private MediaPlayer mPlayerRight;
    private PlayerAssignemnt myAssignment;
    private BackendFacade facade;
    private boolean autoShake = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_team_chooser);
        Typeface tf = Typeface.createFromAsset(getAssets(),
                "fonts/Roboto-Thin.ttf");
        ((TextView) findViewById(R.id.TeamCaptionTextView)).setTypeface(tf);
        ((TextView) findViewById(R.id.TeamNumberTextView)).setTypeface(tf);
        ((TextView) findViewById(R.id.TeamIntroductionTextView))
                .setTypeface(tf);

        ((HoloCircleSeekBar) findViewById(R.id.GameProgessCircleBar))
                .setEnabled(false);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        animationShake1 = AnimationUtils.loadAnimation(this, R.anim.shake1);
        animationShake2 = AnimationUtils.loadAnimation(this, R.anim.shake2);
        animationSlideOutLeft = AnimationUtils.loadAnimation(this, R.anim.left);
        animationSlideOutRight = AnimationUtils.loadAnimation(this,
                R.anim.right);

        animationShake2.setAnimationListener(this);
        mPlayerLeft = MediaPlayer.create(getApplicationContext(), R.raw.left);
        mPlayerRight = MediaPlayer.create(getApplicationContext(), R.raw.right);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line, getFacade()
                .getPlayersAsStringArray());

        AutoCompleteTextView playerNameTextView = (AutoCompleteTextView) findViewById(R.id.PlayerNameAutoCompleteTextView);
        playerNameTextView.setHint("Player #"
                + (TeamReactor.getAssignmentsDone() + 1));
        playerNameTextView.setOnEditorActionListener(this);
        playerNameTextView.setAdapter(adapter);

        ((HoloCircleSeekBar) findViewById(R.id.GameProgessCircleBar))
                .setMax(TeamReactor.getAssignments().size());

        findViewById(R.id.NextPlayerButton).setOnClickListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:

                // Use the Builder class for convenient dialog construction
                AlertDialog.Builder builder = new AlertDialog.Builder(
                        TeamChooser.this);
                builder.setMessage(R.string.cancelDialog_question)
                        .setPositiveButton(R.string.cancelDialog_positive,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int id) {
                                        Intent upIntent = NavUtils
                                                .getParentActivityIntent(TeamChooser.this);
                                        NavUtils.navigateUpTo(TeamChooser.this,
                                                upIntent);
                                    }
                                })
                        .setNegativeButton(R.string.cancelDialog_negative,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int id) {
                                        showSoftkeyboard_if_needed();
                                    }
                                });
                // Create the AlertDialog object and return it
                builder.create().show();

                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerSensorListener();
        showSoftkeyboard_if_needed();
    }

    private void registerSensorListener() {
        if (myAssignment == null) {
            AutoCompleteTextView playerNameTextview = ((AutoCompleteTextView) findViewById(R.id.PlayerNameAutoCompleteTextView));
            if (playerNameTextview.getText().toString().length() > 0
                    && !checkIfAlreadyAssigned()) {
                startShakeCall();
                sensorManager.registerListener(this, sensorManager
                                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                        SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    private void getAccelerometer(SensorEvent event) {
        float[] values = event.values;
        // Movement
        float x = values[0];
        float y = values[1];
        float z = values[2];

        float accelationSquareRoot = (x * x + y * y + z * z)
                / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);
        long actualTime = System.currentTimeMillis();
        if (accelationSquareRoot >= 2) //
        {
            if (x < 0) {
                findViewById(R.id.BumperLeftImageView).startAnimation(
                        animationShake1);
                mPlayerLeft.start();
            } else {
                findViewById(R.id.BumperRightImageView).startAnimation(
                        animationShake2);
                mPlayerRight.start();
                vibrate();
            }
            if (actualTime - lastUpdate < 200) {
                return;
            }

            Log.d(Flags.LOGTAG, "Bewegung nach " + x + "-" + y + "-" + z);
            acceptPlayername();
            lastUpdate = actualTime;
            findViewById(R.id.PlayerNameAutoCompleteTextView).setEnabled(false);
            stopShakeCall();
        }
    }

    private void vibrate() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(50);
    }

    private void stopShakeCall() {
        findViewById(R.id.ShakeTextView).clearAnimation();
        findViewById(R.id.ShakeTextView).setVisibility(View.GONE);
    }

    private void acceptPlayername() {
        AutoCompleteTextView playerNameTextview = ((AutoCompleteTextView) findViewById(R.id.PlayerNameAutoCompleteTextView));
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(playerNameTextview.getWindowToken(), 0);
        if (autoShake) {
            // auto shake - directly start animations and sound effects
            vibrate();
            findViewById(R.id.BumperLeftImageView).startAnimation(
                    animationShake1);
            mPlayerLeft.start();
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    vibrate();
                    findViewById(R.id.BumperRightImageView).startAnimation(
                            animationShake2); // ending of animation 2 will automatically lead to #onAnimationEnd
                    mPlayerRight.start();
                    stopShakeCall();
                }
            }, 200);

        } else {
            // manual shake - only if enabled by user settings
            registerSensorListener();
        }
    }

    private boolean checkIfAlreadyAssigned() {
        AutoCompleteTextView v = ((AutoCompleteTextView) findViewById(R.id.PlayerNameAutoCompleteTextView));
        String playerName = v.getText().toString().trim();
        for (PlayerAssignemnt checkAssignment : TeamReactor.getAssignments()) {
            if (null != checkAssignment.getPlayer()
                    && checkAssignment.getPlayer().getName()
                    .equalsIgnoreCase(playerName)) {
                v.setText(playerName);
                v.requestFocus();
                Toast.makeText(getApplicationContext(),
                        getString(R.string.playerAlreadyAssigned),
                        Toast.LENGTH_LONG).show();
                return true;
            }
        }

        return false;
    }

    private void startShakeCall() {
        findViewById(R.id.ShakeTextView).startAnimation(getAnimationGlow());
        findViewById(R.id.ShakeTextView).setVisibility(View.VISIBLE);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        getAccelerometer(event);
    }

    @Override
    public void onAnimationEnd(Animation animation) {
        chooseTeamAssignment();
    }

    private void chooseTeamAssignment() {
        sensorManager.unregisterListener(this);
        stopShakeCall();

        findViewById(R.id.BumperLeftImageView).startAnimation(
                animationSlideOutLeft);
        findViewById(R.id.BumperRightImageView).startAnimation(
                animationSlideOutRight);
        findViewById(R.id.TeamNumberTextView).setVisibility(View.VISIBLE);
        findViewById(R.id.TeamCaptionTextView).setVisibility(View.VISIBLE);
        findViewById(R.id.TeamIntroductionTextView).setVisibility(View.VISIBLE);
        findViewById(R.id.BumperLeftImageView).setVisibility(View.GONE);
        findViewById(R.id.BumperRightImageView).setVisibility(View.GONE);
        findViewById(R.id.NextPlayerIncludeLayout).setVisibility(View.VISIBLE);
        if (null != findViewById(R.id.InputPlayerIncludeLayout)) {
            findViewById(R.id.InputPlayerIncludeLayout)
                    .setVisibility(View.GONE);
        }

        findViewById(R.id.GameProgessCircleBar).startAnimation(
                getAnimationGlow());
        String playername = ((AutoCompleteTextView) findViewById(R.id.PlayerNameAutoCompleteTextView))
                .getText().toString();

        String teamHeaderText = playername + ", "
                + getString(R.string.assignmenttext_part1) + " "
                + getMyAssignment().getOrderNumber() + " "
                + getString(R.string.assignmenttext_part2);
        String speakText = playername + " - " + getString(R.string.team) + " "
                + getMyAssignment().getTeam();
        if (getMyAssignment().getOrderNumber() == 1) {
            speakText = getString(R.string.captain) + " " + playername + " "
                    + getString(R.string.assignmenttext_captain) + " "
                    + getString(R.string.team) + " "
                    + getMyAssignment().getTeam();
        }
        ((TextView) findViewById(R.id.TeamIntroductionTextView))
                .setText(teamHeaderText);
        ((TextView) findViewById(R.id.TeamNumberTextView)).setText(String
                .valueOf(getMyAssignment().getTeam()));

        TeamReactor.getAssignments().remove(getMyAssignment());
        Player playerNew = new Player(playername);
        getMyAssignment().setPlayer(playerNew);
        TeamReactor.getAssignments().add(getMyAssignment());
        try {
            getFacade().persistPlayer(playerNew);
        } catch (Exception e) {
            Log.d(Flags.LOGTAG, playerNew + " already known.");
        }

        ((HoloCircleSeekBar) findViewById(R.id.GameProgessCircleBar))
                .setProgress(TeamReactor.getAssignmentsDone());

        if (TeamReactor.hasAssignmentsLeft()) {
            int playersLeft = TeamReactor.getAssignments().size()
                    - TeamReactor.getAssignmentsDone();
            ((TextView) findViewById(R.id.GameProgressTextView))
                    .setText(playersLeft + " "
                            + getString(R.string.playersleftwithoutteam));
        } else {
            ((TextView) findViewById(R.id.GameProgressTextView))
                    .setText(getString(R.string.decisiontext));
        }

        TTS_Tool.getInstance(this)
                .sprechen(speakText, TextToSpeech.QUEUE_FLUSH);
        invalidateOptionsMenu();
    }

    @Override
    public void onAnimationRepeat(Animation animation) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onAnimationStart(Animation animation) {

    }

    private void completeTeams() {
        Game saveGame = new Game(TeamReactor.getAssignments());
        getFacade().persistGame(saveGame);
        Intent callChooser = new Intent(getApplicationContext(),
                TeamOverview.class);
        startActivity(callChooser);
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE
                || event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
            acceptPlayername();
            return true;
        }
        return false;
    }

    /**
     * @return the facade
     */
    public BackendFacade getFacade() {
        if (null == facade) {
            facade = new BackendFacade(getApplicationContext());
        }
        return facade;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getFacade().getObjDB_API().close();
    }

    /**
     * @return the myAssignment
     */
    public PlayerAssignemnt getMyAssignment() {
        if (null == myAssignment) {
            myAssignment = TeamReactor.revealNextAssignment();
        }
        return myAssignment;
    }

    /**
     * @return the animationGlow
     */
    private Animation getAnimationGlow() {
        if (null == animationGlow) {
            animationGlow = AnimationUtils.loadAnimation(this, R.anim.glow);
        }
        return animationGlow;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.NextPlayerButton) {
            if (TeamReactor.hasAssignmentsLeft()) {
                Intent callChooser = new Intent(getApplicationContext(),
                        TeamChooser.class);
                callChooser.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(callChooser);
                overridePendingTransition(R.anim.enter_from_right, R.anim.left);
            } else {
                completeTeams();
            }
        }
    }

    public void showSoftkeyboard_if_needed() {
        AutoCompleteTextView playerNameTextView = (AutoCompleteTextView) findViewById(R.id.PlayerNameAutoCompleteTextView);
        if (playerNameTextView.isShown() && playerNameTextView.isEnabled()) {
            playerNameTextView.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(playerNameTextView,
                    InputMethodManager.SHOW_IMPLICIT);
            playerNameTextView.selectAll();
        }
    }

}