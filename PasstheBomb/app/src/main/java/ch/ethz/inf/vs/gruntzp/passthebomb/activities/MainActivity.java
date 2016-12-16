package ch.ethz.inf.vs.gruntzp.passthebomb.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.UUID;

import ch.ethz.inf.vs.gruntzp.passthebomb.Communication.MessageFactory;
import ch.ethz.inf.vs.gruntzp.passthebomb.Communication.MessageListener;
import ch.ethz.inf.vs.gruntzp.passthebomb.gamelogic.Bomb;
import ch.ethz.inf.vs.gruntzp.passthebomb.gamelogic.Game;
import ch.ethz.inf.vs.gruntzp.passthebomb.gamelogic.Player;

public class MainActivity extends AppCompatActivity implements MessageListener {

    EditText mEdit;
    private SharedPreferences preferences;
    private boolean registered;
    private boolean creating;
    private boolean joining;
    private MediaPlayer bgm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mEdit   = (EditText)findViewById(R.id.text_field);

        //change status bar colour
        Window window = getWindow();
        // clear FLAG_TRANSLUCENT_STATUS flag:
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        // finally change the color
        if(Build.VERSION.SDK_INT >= 23) {
            window.setStatusBarColor(getColor(R.color.black));
        }else{
            //noinspection deprecation
            window.setStatusBarColor(getResources().getColor(R.color.black));
        }

        registered = false;
        creating = false;
        joining = false;

        //TODO: save the username in the text field -> use preferences or something
        preferences = getSharedPreferences("Pref", Context.MODE_PRIVATE);
        String username = preferences.getString("user_name", "");
        mEdit.setText(username);
        /*bgm = MediaPlayer.create(this, R.raw.bomb_stage1);
        bgm.setLooping(true);
        bgm.start();*/
    }

    public void onClickCreate(View view) {
        if(mEdit.getText().toString().isEmpty()) {
            Toast toast = Toast.makeText(this, R.string.username_required, Toast.LENGTH_SHORT);
            toast.show();
        }else{
            // save username
            SharedPreferences.Editor editor = preferences.edit();
            String userName = mEdit.getText().toString();
            editor.putString("user_name", userName);
            editor.commit();

            creating = true; //So we know in onMessage

            //attempt to register if not yet registered
            if(!registered)
                tryRegister(userName);
            else {
                //Start next Activity
                Intent myIntent = new Intent(this, CreateActivity.class);
                myIntent.putExtra("creator_name", userName);
                this.startActivity(myIntent);
            }

        }
    }

    public void onClickJoin(View view) {
        if(mEdit.getText().toString().isEmpty()) {
            Toast toast = Toast.makeText(this, R.string.username_required, Toast.LENGTH_SHORT);
            toast.show();
        }else{
            // save username
            SharedPreferences.Editor editor = preferences.edit();
            String userName = mEdit.getText().toString();
            editor.putString("user_name", userName);
            editor.commit();

            joining = true;

            if(!registered)
                tryRegister(userName);
            else {
                //Start next Activity
                Intent myIntent = new Intent(this, JoinActivity.class);
                myIntent.putExtra("player_name", userName);
                this.startActivity(myIntent);
            }




        }
    }

    public void onClickTutorial(View view) {
        // save username
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("user_name", mEdit.getText().toString());
        editor.commit();

        //Start next Activity
        Intent myIntent = new Intent(this, TutorialActivity.class);
        this.startActivity(myIntent);

    }

    @Override
    public void onBackPressed(){
        // save username
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("user_name", mEdit.getText().toString());
        editor.commit();

        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        controller.startService(this); // Only do this in MainActivity
        controller.bind(this);
    }

    @Override
    public void onMessage(int type, JSONObject body) {
        //TODO
        switch(type) {
            case 0:
                Toast toast = Toast.makeText(this, "Message receipt parsing error", Toast.LENGTH_SHORT);
                toast.show();
                break;
            case MessageFactory.SC_RECONNECT_DENIED_ERROR: //Already registered, don't care and fall through
            case MessageFactory.SC_REGISTER_SUCCESSFUL: //Newly registered
                registered = true;
                if (creating) {
                    creating = false; //For the next time
                    String userName = preferences.getString("user_name", "");
                    Intent myIntent = new Intent(this, CreateActivity.class);
                    myIntent.putExtra("creator_name", userName);
                    this.startActivity(myIntent);
                } else { //Joining
                    joining = false;
                    String userName = preferences.getString("user_name", "");
                    Intent myIntent = new Intent(this, JoinActivity.class);
                    myIntent.putExtra("player_name", userName);
                    this.startActivity(myIntent);
                }
                break;
            case MessageFactory.SC_GAME_UPDATE: //Server sends a game update if register is accepted
                Game game = Game.createFromJSON(body);
                if(game.hasStarted()) {//hasStarted implies that the game has a bomb owner and a bomb
                    try {
                        game.newBomb(new Bomb(body.getJSONObject("game").getInt("bomb"),body.getJSONObject("game").getInt("initial_bomb")));
                        game.setBombOwner(game.getPlayerByID(body.getJSONObject("game").getString("bombOwner")));
                    } catch(JSONException e) {
                        e.printStackTrace();
                    }
                    Intent myIntent = new Intent(this, GameActivity.class);

                    // give the next activity extra information
                    myIntent.putExtra("game", game);
                    myIntent.putExtra("thisPlayer", new Player(preferences.getString("user_name",""),preferences.getString("userID","")));

                    this.startActivity(myIntent);

                } else { //Disconnected while in Lobby
                    Intent myIntent = new Intent(this, LobbyActivity.class);
                    myIntent.putExtra("message", body.toString());
                    this.startActivity(myIntent);
                }
                break;
            default:
                break;
        }


    }

    @Override
    protected void onStop() {
        super.onStop();
        controller.unbind(this);
    }

    protected void tryRegister(String userName) {
        String userID = preferences.getString("userID","");
        if(userID.equals("")) { //There was no prior userID
            userID = UUID.randomUUID().toString();
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("userID", userID);
            editor.apply();
        }
        controller.sendMessage(MessageFactory.register(userID, userName));
    }

}
