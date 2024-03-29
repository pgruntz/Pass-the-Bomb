package ch.ethz.inf.vs.gruntzp.passthebomb.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;

import ch.ethz.inf.vs.gruntzp.passthebomb.Communication.MessageListener;
import ch.ethz.inf.vs.gruntzp.passthebomb.gameModel.AudioService;
import ch.ethz.inf.vs.gruntzp.passthebomb.gameModel.Bomb;
import ch.ethz.inf.vs.gruntzp.passthebomb.gameModel.Game;
import ch.ethz.inf.vs.gruntzp.passthebomb.gameModel.Player;
import ch.ethz.inf.vs.gruntzp.passthebomb.newmodel.GameView;


import org.passthebomb.library.MessageFactory;
import org.passthebomb.library.Constants;

//TODO make the bomb and the layout for the players and the score
public class GameActivity extends AppCompatActivity implements MessageListener {

    private int currentApiVersion;
    private Game game;
    private Player thisPlayer;
    private RelativeLayout gameViewLayout;
    private ImageView bomb;
    private final int[] centerPos = new int[2];
    private int screenBombLevel;
    private Boolean bombExplode = false;
    private View.OnTouchListener touchListener;
    private CountDownTimer timer;

    private GameView gameView = new GameView(this);

    @Override
    @SuppressLint("NewApi")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_game);

        // initialize global variables
        bomb = (ImageView) findViewById(R.id.bomb);
        Bundle extras = getIntent().getExtras();

        game = extras.getParcelable("game");
        game.setPlayersAndRoles(game.getPlayers(),game.getCreator().getUuid(),game.getBombOwner().getUuid());
        game.setNumberOfPlayers(game.getPlayers().size());
        game.getBombOwner().setHasBomb(true);
        thisPlayer = extras.getParcelable("thisPlayer");
        thisPlayer = game.getPlayerByID(thisPlayer.getUuid()); //Want a reference, not a copy

         //for testing only
/*
        Player creator = new Player("Senpai", "0");
        game = new Game("herp derp", creator, false, true);
        game.addPlayer(creator);
        game.addPlayer(new Player("OOOOOOOOOOOOOOOOOOOOOOOOO...", "1"));
        game.addPlayer(new Player("derp", "2"));
        game.addPlayer(new Player("somebody", "3"));
        game.getPlayers().get(1).setScore(20);
        thisPlayer = game.getPlayers().get(0);
        thisPlayer.setHasBomb(true);
        thisPlayer.setScore(50);
*/
        //endGame();


        //GUI stuff
        hideNavigationBar();
        gameViewLayout = (RelativeLayout) findViewById(R.id.game);
        setUpPlayers();
        enableOnTouchAndDragging();

        //set bomb to centre for the first time
        FrameLayout.LayoutParams par=(FrameLayout.LayoutParams)bomb.getLayoutParams();
        par.gravity = Gravity.CENTER;
        bomb.setLayoutParams(par);

        setUpBomb();
    }


    /* When a player gets disconnected call this method.
     * This method greys out the given player's field.
     */
    public void showPlayerAsDisconnected(String uuid) {


        Player disco = game.getPlayerByID(uuid);
        if(disco != null) {
            int i = game.getPlayers().indexOf(disco);
            int pos = game.getPlayers().indexOf(thisPlayer);
            Button playerField = (Button) gameViewLayout.getChildAt((i < pos) ? i : (i - 1));
            playerField.setBackground((i != 3) ?
                    getDrawable(R.drawable.greyed_out_field) : getDrawable(R.drawable.greyed_out_field_upsidedown));

        }


        /* Testing that the positions of the fields don't get messed up
         *
          for(int i = 0; i<game.getPlayers().size()-1; i++){
            Button playerField = (Button) gameViewLayout.getChildAt(i);
            if(i!=3) {
                playerField.setBackground(getDrawable(R.drawable.greyed_out_field));
            }else{
                playerField.setBackground(getDrawable(R.drawable.greyed_out_field_upsidedown));
            }
         }
         *
         */

    }
    //Issue: If names are too long, then they overlap with the borders of the buttons
    //probably because the button is actually a rectangle, but the visual button is a trapezoid
    private void setUpPlayers(){
        for (int j = 0; j < 4; j++)
        {
            Button player_field = (Button) gameViewLayout.getChildAt(j);
            player_field.clearAnimation();
            player_field.setVisibility(View.INVISIBLE);
        }

        int guiPos = 0; //index for player field
        for(int i = 0; i < game.getPlayers().size(); i++){
            Player curr = game.getPlayers().get(i);
            if (curr != null && !thisPlayer.equals(curr)) {
                Button player_field = (Button) gameViewLayout.getChildAt(guiPos);
                player_field.setVisibility(View.VISIBLE);
                String playerName = curr.getName();
                if(playerName.length()>17){
                    playerName = playerName.substring(0,15) + "...";
                }
                player_field.setText(playerName + "\n" + curr.getScore());
                if(curr.isHasBomb())
                    addBombIcon(guiPos);
                else
                    removeDrawableIcon(guiPos);
                guiPos++;
            }
            if(curr == null)
                guiPos++;
        }
        TextView own_score = (TextView) findViewById(R.id.Score_number);
        own_score.setText(thisPlayer.getScore()+"");
    }

    //adds "has bomb"-Icon to player_field
    public void addBombIcon (int player_number) {
        Button player_field = (Button) gameViewLayout.getChildAt(player_number);
        player_field.setCompoundDrawablesWithIntrinsicBounds(R.drawable.bomb_36dp, 0, 0, 0);
    }

    //removes (not invisible, but removed!) "has bomb"-Icon or "target"-icon from player_field
    public void removeDrawableIcon (int player_number){
        Button player_field = (Button) gameViewLayout.getChildAt(player_number);
        player_field.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
    }

    //analogous to addBombIcon, This icon shows that the bomb is currently on it's way to target player
    //but has not arrived yet
    public void addTargetIcon (int player_number) {
        Button player_field = (Button) gameViewLayout.getChildAt(player_number);
        player_field.setCompoundDrawablesWithIntrinsicBounds(R.drawable.target_36dp, 0, 0, 0);
    }

    public void updateScores(){
        int guiPos = 0; //index for player field
        for(int i=0; i<game.getPlayers().size(); i++){
            if (game.getPlayers().get(i) != null && thisPlayer != game.getPlayers().get(i)) {
                Button player_field = (Button) gameViewLayout.getChildAt(guiPos);
                //we include score in the name-string
                player_field.setText(game.getPlayers().get(i).getName() + "\n" + game.getPlayers().get(i).getScore());
                guiPos++;
            }
            if (game.getPlayers().get(i) == null){
                guiPos++;
            }
        }
        TextView own_score = (TextView) findViewById(R.id.Score_number);
        own_score.setText(Integer.toString(thisPlayer.getScore()));
    }

    private void setUpBomb(){
        setBombVisibility();
        setBombInCenter();

    }

    //TODO: wird bei jedem Afruf von setBombVisibility() ein neuer Timer erstellt, der dann eventuell parallel zum alten läuft?
    private void setBombVisibility(){
        if(!thisPlayer.isHasBomb()){
            bomb.setAnimation(null);
            bomb.clearAnimation();
            bomb.setVisibility(View.INVISIBLE);
            bomb.invalidate();
            bomb.setVisibility(View.INVISIBLE);
        } else {
            //Adjust looks of the bomb
            screenBombLevel = game.bombLevel();
            changeBombImage(screenBombLevel);
            setBombAnimation(screenBombLevel);
            gameView.Sound().setBackgroundMusicByBombLevel(screenBombLevel);
            bomb.setVisibility(View.VISIBLE);

            timer = new CountDownTimer(game.getBombValue()*1000 /*max ticks*/, 1000) {

                public void onTick(long millisUntilFinished) {
                    onFinish();
                }

                public void onFinish() {//Bomb explodes
                    game.bombLock.lock();
                    int bombResponse = ScoreActionHandleBomb(game.IDLE_VALUE);
                    if (bombResponse == game.DEC_ERROR || bombResponse == game.DEC_LAST)
                        this.cancel();
                    game.bombLock.unlock();
                }
            }.start();

        }
    }

    private final int ScoreActionHandleBomb(int score) {
        // game.bombLock needs to be hold before entering and to be released after returning from this method
        // Maybe this synchronized method is enough?
        final int bombResponse = game.decreaseBomb();
        switch (bombResponse) {
            case Game.DEC_OKAY: //Bomb was decreased and game can go on
                if (score > 0) {
                    thisPlayer.changeScore(score);
                    controller.sendMessage(MessageFactory.updateScore(game.getBombValue(), thisPlayer.getScore()));
                }
                break;
            case Game.DEC_LAST: //Bomb was decreased for the last time, it explodes now. New scores given by server
                if (score > 0)
                    thisPlayer.changeScore(score);
                explodeBomb();
                break;
            case Game.DEC_ERROR: //Bomb already zero, other thread sent message to server
                break;
        }
        return bombResponse;
    }

    private void onBombTapped() {
        game.bombLock.lock();
        gameView.Sound().playTapSound();
        int ret = ScoreActionHandleBomb(game.TAP_VALUE);
        game.bombLock.unlock();
    }


    private void explodeBomb()
    {
        bombExplode = true;
        controller.sendMessage(MessageFactory.updateScore(game.getBombValue(), thisPlayer.getScore())); //TODO: nötig?
        controller.sendMessage(MessageFactory.exploded());

        Log.d("bomb","exploding");
        moveBombToCenter(50);
    }

    private void changeBombImage(int level) {
        switch(level) {
            case 1:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    bomb.setImageDrawable(getResources().getDrawable(R.drawable.bomb_stage1, getApplicationContext().getTheme()));
                } else {
                    bomb.setImageDrawable(getResources().getDrawable(R.drawable.bomb_stage1));
                }
                break;
            case 2:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    bomb.setImageDrawable(getResources().getDrawable(R.drawable.bomb_stage2, getApplicationContext().getTheme()));
                } else {
                    bomb.setImageDrawable(getResources().getDrawable(R.drawable.bomb_stage2));
                }
                break;
            case 3:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    bomb.setImageDrawable(getResources().getDrawable(R.drawable.bomb_stage3, getApplicationContext().getTheme()));
                } else {
                    bomb.setImageDrawable(getResources().getDrawable(R.drawable.bomb_stage3));
                }
                break;
            case 4:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    bomb.setImageDrawable(getResources().getDrawable(R.drawable.bomb_stage4, getApplicationContext().getTheme()));
                } else {
                    bomb.setImageDrawable(getResources().getDrawable(R.drawable.bomb_stage4));
                }
                break;
            case 5:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    bomb.setImageDrawable(getResources().getDrawable(R.drawable.bomb_stage5, getApplicationContext().getTheme()));
                } else {
                    bomb.setImageDrawable(getResources().getDrawable(R.drawable.bomb_stage5));
                }
                break;
            default:
                break;
        }
    }


    private void setBombAnimation(int level){
        Animation anim;
        switch(level) {
            case 1:
                anim = AnimationUtils.loadAnimation(bomb.getContext(), R.anim.bomb_stage1);
                break;
            case 2:
                anim = AnimationUtils.loadAnimation(bomb.getContext(), R.anim.bomb_stage2);
                break;
            case 3:
                anim = AnimationUtils.loadAnimation(bomb.getContext(), R.anim.bomb_stage3);
                break;
            case 4:
                anim = AnimationUtils.loadAnimation(bomb.getContext(), R.anim.bomb_stage4);
                break;
            case 5:
                anim = AnimationUtils.loadAnimation(bomb.getContext(), R.anim.bomb_stage5);
                break;
            default:
                anim = AnimationUtils.loadAnimation(bomb.getContext(), R.anim.bomb_stage1);
                break;
        }
        anim.setFillAfter(true);
        bomb.startAnimation(anim);
    }

    private void setBombInCenter(){
        FrameLayout.LayoutParams par=(FrameLayout.LayoutParams)bomb.getLayoutParams();
        par.leftMargin = centerPos[0];
        par.topMargin = centerPos[1];
        bomb.setLayoutParams(par);
    }

    /**
     * @param x - is the offset on the x-axis from the centre
     */
    private void moveBombToCenter(final int x){
        if(bombExplode){
            disableOnTouchAndDragging();
        }

        RelativeLayout root = (RelativeLayout) findViewById( R.id.game );

        FrameLayout.LayoutParams par = (FrameLayout.LayoutParams) bomb.getLayoutParams();
        if(par.gravity == Gravity.CENTER) {
            int[] location = new int[2];
            bomb.getLocationOnScreen(location);
            centerPos[0] = location[0];
            centerPos[1] = location[1];
        }

        int originalPos[] = new int[2];
        bomb.getLocationOnScreen( originalPos );

        TranslateAnimation anim = new TranslateAnimation( 0, centerPos[0]+ x - originalPos[0] , 0, centerPos[1] - originalPos[1] );
        anim.setDuration(500);
        anim.setFillAfter(true);

        final ImageView explosionView = (ImageView) findViewById(R.id.explosion_view);
        explosionView.setBackgroundResource(R.drawable.explosion);
        final AnimationDrawable explosionAnimation =(AnimationDrawable) explosionView.getBackground();
        anim.setAnimationListener(
                new Animation.AnimationListener() {

                    @Override
                    public void onAnimationStart(Animation animation) {}

                    @Override
                    public void onAnimationRepeat(Animation animation) {}

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        //make the bomb movable again and set the coordinates right
                        bomb.clearAnimation();

                        FrameLayout.LayoutParams par=(FrameLayout.LayoutParams)bomb.getLayoutParams();
                        par.leftMargin = centerPos[0]+x;
                        par.topMargin = centerPos[1];
                        bomb.setLayoutParams(par);
                        setBombAnimation(screenBombLevel);
                    }
                }
        );
        bomb.startAnimation(anim);

        if (bombExplode) {

            explosionView.setVisibility(View.VISIBLE);
            explosionView.post(new Runnable() {
                @Override
                public void run() {
                    explosionAnimation.start();
                    checkIfAnimationDone(explosionAnimation);

                    gameView.Sound().playSound(R.raw.bomb_explode);
                    bomb.setVisibility(View.INVISIBLE);
                }
            });
        }


    }

    private void checkIfAnimationDone(AnimationDrawable anim){
        final AnimationDrawable a = anim;
        int timeBetweenChecks = 66;
        Handler h = new Handler();
        h.postDelayed(new Runnable(){
            public void run(){
                if (a.getCurrent() != a.getFrame(a.getNumberOfFrames() - 1)){
                    checkIfAnimationDone(a);
                } else {
                    bombExplode = false;
                    final ImageView explosionView = (ImageView) findViewById(R.id.explosion_view);
                    explosionView.setVisibility(View.INVISIBLE);
                    enableOnTouchAndDragging();
                    FrameLayout.LayoutParams par=(FrameLayout.LayoutParams)bomb.getLayoutParams();
                    par.gravity = Gravity.NO_GRAVITY;
                    bomb.setLayoutParams(par);
                    setBombInCenter();

                }
            }
        }, timeBetweenChecks);
    };

    private void disableOnTouchAndDragging(){
        touchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        };
    }

    private void enableOnTouchAndDragging(){
        touchListener = new View.OnTouchListener() {
            private Boolean[] touch = {false, false, false, false};
            private int missedPlayer;
            int eq;
            int prevX,prevY;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                missedPlayer = 0;

                final FrameLayout.LayoutParams par=(FrameLayout.LayoutParams)v.getLayoutParams();

                for(int i=0; i<game.getPlayers().size()-1; i++) {
                    Button playerfield = (Button) gameViewLayout.getChildAt(i);
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN: {

                            //start dragging
                            prevX=(int)event.getRawX();
                            prevY=(int)event.getRawY();
                            par.bottomMargin=-2*v.getHeight();
                            par.rightMargin=-2*v.getWidth();

                            v.setLayoutParams(par);
                            // makes sure that the bomb image doesn't jump around
                            if(par.gravity == Gravity.CENTER) {
                                int[] location = new int[2];
                                bomb.getLocationOnScreen(location);
                                centerPos[0] = location[0];
                                centerPos[1] = location[1];
                                par.gravity = Gravity.NO_GRAVITY;
                                par.leftMargin = centerPos[0];
                                par.topMargin = centerPos[1];
                            }

                            v.setLayoutParams(par);
                            scaleIn(bomb, 5);
                            setBombAnimation(screenBombLevel);

                            //check intersection
                            if(checkInterSection(playerfield, i,  event.getRawX(), event.getRawY())
                                    && playerfield.getVisibility() == View.VISIBLE) {
                                scaleIn(playerfield, i);
                                touch[i] = true;
                                Log.i("down", "yes!");
                            }
                            break;
                        }
                        case MotionEvent.ACTION_MOVE: {

                            //drag
                            par.topMargin+=(int)event.getRawY()-prevY;
                            prevY=(int)event.getRawY();
                            par.leftMargin+=(int)event.getRawX()-prevX;
                            prevX=(int)event.getRawX();
                            v.setLayoutParams(par);

                            //check touch
                            if(checkInterSection(playerfield, i, event.getRawX(), event.getRawY()) &&
                                    !touch[i] && playerfield.getVisibility() == View.VISIBLE) {
                                scaleIn(playerfield, i);
                                touch[i] = true;

                                Log.i("move", "yes!");
                            } else if(!checkInterSection(playerfield, i, event.getRawX(), event.getRawY()) && touch[i]) {
                                // run scale animation and make it smaller
                                scaleOut(playerfield, i);
                                touch[i] = false;

                                Log.i("move", "no!");
                            }
                            break;
                        }
                        case MotionEvent.ACTION_UP: {

                            //stop dragging
                            par.topMargin+=(int)event.getRawY()-prevY;
                            par.leftMargin+=(int)event.getRawX()-prevX;
                            v.setLayoutParams(par);
                            scaleOut(bomb, 5);

                            //check if touching
                            if (touch[i] && playerfield.getVisibility() == View.VISIBLE) {
                                // run scale animation and make it smaller
                                scaleOut(playerfield, i);
                                touch[i] = false;

                                game.bombLock.lock();
                                if(game.getBombValue() > 0) {//The timer might have run out in the meantime

                                    //Kill timer. It has to exist here, because time is greater than zero and we have the bomb
                                    timer.cancel();
                                    //find out index of the player who you're sending the bomb to
                                    //because index i does not count thisPlayer
                                    if(game.getPlayers().indexOf(thisPlayer)  <= i )
                                        ++i;
                                    controller.sendMessage(MessageFactory.passBomb(game.getPlayers().get(i).getUuid(), game.getBombValue()));
                                    Animation anim = AnimationUtils.loadAnimation(bomb.getContext(), R.anim.super_scale_out);
                                    anim.setFillAfter(false);
                                    anim.setAnimationListener(new Animation.AnimationListener() {
                                        @Override
                                        public void onAnimationStart(Animation animation) {
                                            gameView.Sound().playSound(R.raw.bomb_send);
                                        }

                                        @Override
                                        public void onAnimationEnd(Animation animation) {
                                            thisPlayer.setHasBomb(false);
                                            setUpBomb();
                                        }

                                        @Override
                                        public void onAnimationRepeat(Animation animation) {

                                        }
                                    });
                                    bomb.clearAnimation();
                                    bomb.startAnimation(anim);
                                } else {
                                    thisPlayer.setHasBomb(false);
                                    setUpBomb();
                                }
                                game.bombLock.unlock();
                            } else {
                                missedPlayer++;
                                moveBombToCenter(0);
                            }
                            break;
                        }

                    }
                }
                eq = game.getPlayers().size()-1;
                if(missedPlayer == eq) { //No player hit, decrease bomb and update score;
                    double dist = Math.sqrt(Math.pow(par.leftMargin - centerPos[0], 2) + Math.pow(par.topMargin - centerPos[1], 2));
                    if (dist < 100) {
                        Log.d("distance", Double.toString(dist));
                        onBombTapped();
                    }
                }

                return true;
            }
        };
        bomb.setOnTouchListener(touchListener);
    }


    private void scaleIn(View v, int childID){
        Animation anim;
        switch (childID){
            case 2:
            {
                anim = AnimationUtils.loadAnimation(v.getContext(), R.anim.scale_in_green);
                break;
            }
            case 1:
            {
                anim = AnimationUtils.loadAnimation(v.getContext(), R.anim.scale_in_yellow);
                break;
            }
            default:
            {
                anim = AnimationUtils.loadAnimation(v.getContext(), R.anim.scale_in_normally);
                break;
            }
        }

        v.startAnimation(anim);
        anim.setFillAfter(true);
    }

    private void scaleOut(View v, int childID){
        Animation anim;
        switch (childID){
            case 2:
            {
                anim = AnimationUtils.loadAnimation(v.getContext(), R.anim.scale_out_green);
                break;
            }
            case 1:
            {
                anim = AnimationUtils.loadAnimation(v.getContext(), R.anim.scale_out_yellow);
                break;
            }
            default:
            {
                anim = AnimationUtils.loadAnimation(v.getContext(), R.anim.scale_out_normally);
                break;
            }
        }

        v.startAnimation(anim);
        anim.setFillAfter(true);
    }

    //used to check the intersection between bomb and playerfields
    private boolean checkInterSection(View view, int childID, float rawX, float rawY) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int x = location[0];
        int y = location[1];
        int width = view.getWidth();
        int height = view.getHeight();
        switch (childID){
            case 1: //yellow field
            {
                y -= width ;
                width = view.getHeight();
                height = view.getWidth();
                break;
            }
            case 2: //green field
            {
                x -= height;
                width = view.getHeight();
                height = view.getWidth();
                break;
            }
            default: //case 0: red field or blue field
            {
                break;
            }
        }
        //Check the intersection of point with rectangle achieved
        return rawX > x && rawX < (x+width) && rawY>y && rawY <(y+height);
    }

    private GameState gameState = GameState.Running;
    private enum GameState {
        Running,
        Finished
    }

    public void endGame(){
        gameState = GameState.Finished;

        if(thisPlayer.isHasBomb()){
            //TODO make bomb explode <-- is this still necessary? (the bomb explodes before the game is ended, right?
        }

        //darken rest of the screen
        RelativeLayout darkBG = (RelativeLayout) findViewById(R.id.game_over_screen);
        darkBG.setVisibility(View.VISIBLE);

        //show winner/loser
        Boolean isWinner = true;
        for(int i=0; i<game.getNoPlayers(); i++){
            if(game.getPlayers().get(i) != null)
                isWinner &= (thisPlayer.getScore() >= game.getPlayers().get(i).getScore());
        }
        if(isWinner){
            ImageView showWinner = (ImageView) findViewById(R.id.you_win_image);
            showWinner.setVisibility(View.VISIBLE);
        } else {
            ImageView showLoser = (ImageView) findViewById(R.id.you_lose_image);
            showLoser.setVisibility(View.VISIBLE);
        }
        //show button to continue
        Button toScoreboard = (Button) findViewById(R.id.to_scoreboard);
        Typeface font = Typeface.createFromAsset(getAssets(), "fonts/sensei_medium.otf");
        toScoreboard.setTypeface(font);
        if(Build.VERSION.SDK_INT >= 23) {
            toScoreboard.getBackground().setColorFilter(getColor(R.color.orange), PorterDuff.Mode.OVERLAY);

        } else {
            //noinspection deprecation
            toScoreboard.getBackground().setColorFilter(getResources().getColor(R.color.orange), PorterDuff.Mode.OVERLAY);
        }
        toScoreboard.setVisibility(View.VISIBLE);
        gameView.Sound().setBackgroundMusicByBombLevel(1);
    }


    // makes sure navigation bar doesn't appear again
    @SuppressLint("NewApi")
    @Override
    public void onWindowFocusChanged(boolean hasFocus)
    {
        super.onWindowFocusChanged(hasFocus);
        if(currentApiVersion >= Build.VERSION_CODES.KITKAT && hasFocus)
        {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    // Suppress navigation bar
    private void hideNavigationBar(){
        currentApiVersion = android.os.Build.VERSION.SDK_INT;

        final int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

        // This work only for android 4.4+
        if(currentApiVersion >= Build.VERSION_CODES.KITKAT)
        {

            getWindow().getDecorView().setSystemUiVisibility(flags);

            // Code below is to handle presses of Volume up or Volume down.
            // Without this, after pressing volume buttons, the navigation bar will
            // show up and won't hide
            final View decorView = getWindow().getDecorView();
            decorView
                    .setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener()
                    {

                        @Override
                        public void onSystemUiVisibilityChange(int visibility)
                        {
                            if((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0)
                            {
                                decorView.setSystemUiVisibility(flags);
                            }
                        }
                    });
        }
    }

    /** TODO? if user somehow manages to bring back the navigation bar,
     ** should it not do anything, or
     ** should it bring them back to the main menu or something
     ** and kick him out of the game?
     **/
    boolean doubleBackToExitPressedOnce = false;


    @Override
    public void onBackPressed(){
        switch (gameState) {

            case Finished:
                onClickContinue(null);
                break;

            default:
                //super.onBackPressed();
                //TODO: Maybe implement "Are you sure?"
                if (doubleBackToExitPressedOnce) {
                    controller.sendMessage(MessageFactory.leaveGame()); //Pressing back is surrendering
                    finish();
                }

                this.doubleBackToExitPressedOnce = true;
                Toast.makeText(this, "Click back again to exit", Toast.LENGTH_SHORT).show();

                new Handler().postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        doubleBackToExitPressedOnce=false;
                    }
                }, 2000);
                break;
        }


    }


    public void onClickContinue(View view) {
        Intent myIntent = new Intent(this, ScoreboardActivity.class);

        //give extra information to the next activity
        myIntent.putExtra("game", game);
        myIntent.putExtra("thisPlayer", thisPlayer);

        //start next activity
        this.startActivity(myIntent);

        if(this.getParent() != null) //Trying to get rid of lingering activity
            this.getParent().finish();

        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        controller.bind(this);
        if (!gameView.Sound().isSoundServiceBound()) {
            Intent intent = new Intent(this, AudioService.class);
            startService(intent);
            bindService(intent, gameView.Sound().getSoundServiceConnection(), Context.BIND_AUTO_CREATE);
        }
        System.out.println("GameActivity bound to audioservice");

    }

    @Override
    public void onMessage(int type, JSONObject body) {
        Game newGame;
        switch(type) {
            case 0:
                Toast toast = Toast.makeText(this, "Message receipt parsing error", Toast.LENGTH_SHORT);
                toast.show();
                break;
            case MessageFactory.SC_PLAYER_LEFT:
                newGame = Game.createFromJSON(body);
                String oldBombOwner = game.getBombOwner().getUuid();
                game.setPlayersAndRoles(newGame.getPlayers(),newGame.getCreator().getUuid(), newGame.getBombOwner().getUuid());
                game.setNumberOfPlayers(game.getNoPlayers()-1);
                thisPlayer = game.getPlayerByID(thisPlayer.getUuid());
                setUpPlayers();
                if(!oldBombOwner.equals(game.getBombOwner().getUuid()))
                    setUpBomb();
                break;
            case MessageFactory.SC_UPDATE_SCORE:
                newGame = Game.createFromJSON(body);
                game.adoptScore(newGame);
                try {
                    game.newBomb(new Bomb(body.getJSONObject("game").getInt("bomb"),body.getJSONObject("game").getInt("initial_bomb")));
                } catch(JSONException e) {
                    e.printStackTrace();
                }
                if (!thisPlayer.isHasBomb())
                {
                    gameView.Sound().playSound(R.raw.bomb_tap);
                }

                updateScores();
                break;
            case MessageFactory.SC_PLAYER_MAYBEDC:
                newGame = Game.createFromJSON(body);
                game.setPlayersAndRoles(newGame.getPlayers(),game.getCreator().getUuid(), game.getCreator().getUuid());
                thisPlayer = game.getPlayerByID(thisPlayer.getUuid());
                for(Player p : game.getPlayers()) {
                    if(p != null && p.getMaybeDC()) {
                        showPlayerAsDisconnected(p.getUuid());
                    }
                }
                break;
            case MessageFactory.SC_BOMB_PASSED:
                newGame = Game.createFromJSON(body);
                try {
                    game.newBomb(new Bomb(body.getJSONObject("game").getInt("bomb"),body.getJSONObject("game").getInt("initial_bomb")));
                    game.setPlayersAndRoles(newGame.getPlayers(), body.getJSONObject("game").getString("owner"), body.getJSONObject("game").getString("bombOwner"));
                    thisPlayer = game.getPlayerByID(thisPlayer.getUuid());
                    if (thisPlayer.isHasBomb())
                    {
                        gameView.Sound().playSound(R.raw.bomb_receive);
                        //TODO: check if exploded
                        //game.bombLock.lock();
                        //int ret = ScoreActionHandleBomb(Game.RECEIVE_DECREASE);
                        //game.bombLock.unlock();
                    }
                    else
                    {
                        gameView.Sound().playSound(R.raw.bomb_send);
                    }
                } catch(JSONException e) {
                    e.printStackTrace();
                }
                setUpPlayers();
                setUpBomb();
                break;
            case MessageFactory.SC_BOMB_EXPLODED: //Have to check if game is over or just new round
                if(!thisPlayer.isHasBomb())
                    gameView.Sound().playSound(R.raw.bomb_explode);
                gameView.Sound().setBackgroundMusicByBombLevel(1);
                newGame = Game.createFromJSON(body);
                game.setPlayersAndRoles(newGame.getPlayers(), newGame.getCreator().getUuid(), "" /*No bomb owner*/);
                thisPlayer = game.getPlayerByID(thisPlayer.getUuid());
                game.adoptScore(newGame);
                thisPlayer.setHasBomb(false);
                setUpBomb();
                for(Player p : game.getPlayers()) {
                    if(p!=null && p.getScore() >= Constants.FINAL_SCORE) {
                        endGame();
                        break;
                    }
                }
                //No player won, so we have to wait for next gameupdate
                break;
            case MessageFactory.SC_GAME_STARTED:
                toast = Toast.makeText(this, "The bomb exploded, new round started.", Toast.LENGTH_SHORT);
                toast.show();
                newGame = Game.createFromJSON(body);
                try {
                    game.newBomb(new Bomb(body.getJSONObject("game").getInt("bomb"),body.getJSONObject("game").getInt("initial_bomb")));
                    game.setPlayersAndRoles(newGame.getPlayers(), body.getJSONObject("game").getString("owner"), body.getJSONObject("game").getString("bombOwner"));
                    thisPlayer = game.getPlayerByID(thisPlayer.getUuid());
                } catch(JSONException e) {
                    e.printStackTrace();
                }
                setUpPlayers();
                setUpBomb();
                break;
            case MessageFactory.SC_InstantWin:
                game.setPlayers(new LinkedList<Player>());
                game.addPlayer(thisPlayer);
                game.setNumberOfPlayers(1);
                endGame();
                break;
            case MessageFactory.CONNECTION_FAILED:
                Toast.makeText(this.getApplicationContext(), "Connection lost", Toast.LENGTH_SHORT).show();
                Intent retMain = new Intent(this, MainActivity.class);
                this.startActivity(retMain);
                finish();
            default:
                break;
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        controller.unbind(this);
        if (gameView.Sound().isSoundServiceBound()){
            unbindService(gameView.Sound().getSoundServiceConnection());
        }
        gameView.Sound().unboundSoundService();
        System.out.println("GameActivity unbound from AudioService");

    }

}
