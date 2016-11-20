package ch.ethz.inf.vs.gruntzp.passthebomb.gamelogic;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.LinkedList;

/**
 * Created by Michelle on 19.11.2016.
 */

// implements Parcelable so that it can be put in putExtra()
public class Game implements Parcelable{

    private String name;
    private String creatorName;
    private LinkedList<Player> players;
    private Boolean locked;
    private String password;

    public Game(String name, String creatorName, Boolean locked, String password){
        this.name = name;
        this.creatorName = creatorName;

        this.players = new LinkedList<>();
        players.addFirst(new Player(creatorName));

        this.locked = locked;
        this.password = password;
    }

    public Game(Parcel in){
        name = in.readString();
        creatorName = in.readString();
        players = new LinkedList<>();
        in.readList(players, Player.class.getClassLoader());
        locked = in.readByte() != 0;
        password = in.readString();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public LinkedList<Player> getPlayers() {
        return players;
    }

    public void setPlayers(LinkedList<Player> players) {
        this.players = players;
    }

    public Boolean getLocked() {
        return locked;
    }

    public void setLocked(Boolean locked) {
        this.locked = locked;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    // leave this empty please
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(creatorName);
        dest.writeList(players);
        dest.writeByte((byte) (locked ? 1 : 0));
        dest.writeString(password);
    }

    public static final Parcelable.Creator<Game> CREATOR = new Parcelable.Creator<Game>()
    {
        public Game createFromParcel(Parcel in)
        {
            return new Game(in);
        }
        public Game[] newArray(int size)
        {
            return new Game[size];
        }
    };
}