package ch.ethz.inf.vs.gruntzp.passthebomb.activities;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class LobbyActivity extends AppCompatActivity {

    private int numberOfPlayers; //TODO count players
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);
        setLobbyTitle();

        //TODO add the list of players
    }

    private void setLobbyTitle(){
        Bundle extras = getIntent().getExtras();
        setTitle(extras.getString("game_name"));
    }

    /* Starts the game.
    ** All other intents should be destroyed,
    ** because they won't be called though the back button anymore
    ** and thus would be stuck on the stack
     */
    public void onClickStart(View view) {
        if(numberOfPlayers<2){
            Toast toast = Toast.makeText(this, R.string.too_little_players, Toast.LENGTH_SHORT);
            toast.show();
        }else {
            //TODO send start command to server
            //TODO? do we need to send anything to the next activity?
            Intent myIntent = new Intent(this, GameActivity.class);
            this.startActivity(myIntent);

            // destroy intent with MainActivity
            getParent().getParent().finish();
            // destroy intent with CreateActivity/LobbyActivity
            getParent().finish();
        }
    }
}
