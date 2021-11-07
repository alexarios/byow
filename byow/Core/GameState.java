package byow.Core;

import java.io.Serializable;

public class GameState implements Serializable {
    Long seed;
    World.Coordinate currPosP1, currPosP2;
    String keystrokeHistory;

    public GameState(Long seed, World.Coordinate currPosP1,
                     World.Coordinate currPosP2, String keystrokeHistory) {
        this.seed = seed;
        this.currPosP1 = currPosP1;
        this.currPosP2 = currPosP2;
        this.keystrokeHistory = keystrokeHistory;
    }
}
