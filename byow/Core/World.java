package byow.Core;
import byow.TileEngine.TETile;
import byow.TileEngine.Tileset;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.Random;

import static java.lang.Math.min;

public class World {

    int worldWidth, worldHeight;
    long worldSeed;
    TETile[][] tiles, prevWorld;
    LinkedList<Room> rooms;
    Coordinate currPosP1, currPosP2;
    StringBuilder keystrokeHistory;
    Coordinate fireDoor, waterDoor;
    Coordinate prevPosP1, prevPosP2;
    Coordinate armor;
    int armorStepsLeft = 0;
    int gemsCollected = 0;
    int totalStepsTaken = 0;
    int totalEncounterSteps = 40;
    int encounterStepsLeft = totalEncounterSteps;
    boolean gameLost = false, inEncounter = false;
    private final int TRAIL_STEPS_LIMIT = 120;

    public int getTrailStepsLimit() {
        return TRAIL_STEPS_LIMIT;
    }

    Random worldRand = new Random(worldSeed);

    public World(int width, int height) {
        this.worldWidth = width - 1;
        this.worldHeight = height - 1;
        this.rooms = new LinkedList<>();
        createWorld(width, height);
    }

    public static class Coordinate implements Serializable {
        int x;
        int y;

        public Coordinate(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public Coordinate shift(int xShift, int yShift) {
            return new Coordinate(x + xShift, y + yShift);
        }
        public boolean equals(Coordinate other) {
            if (other != null) {
                return this.x == other.x && this.y == other.y;
            }
            return false;
        }

        public Coordinate copy() {
            return new Coordinate(this.x, this.y);
        }
    }

    public static class Room {
        Coordinate bottomLeft, topRight, center;
        int width;
        int height;

        public Room(Coordinate bottomLeft, int width, int height) {
            this.bottomLeft = bottomLeft;
            this.topRight = new Coordinate(bottomLeft.x + width - 1, bottomLeft.y + height - 1);
            this.center = new Coordinate((bottomLeft.x + topRight.x) / 2,
                    (bottomLeft.y + topRight.y) / 2);
            this.width = width;
            this.height = height;
        }
    }

    public void createWorld(int width, int height) {
        tiles = new TETile[width][height];
        initWorld(width, height);
    }

    public void generate(long seed) {
        this.worldSeed = seed;
        this.keystrokeHistory = new StringBuilder();
        createRoomObjects(seed);
        addHallways();
        addRooms();
        spawnPlayer("P1");
        spawnPlayer("P2");

    }

    public void initWorld(int width, int height) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                tiles[x][y] = Tileset.NOTHING;
            }
        }
        for (int x = 0; x < width; x++) {
            tiles[x][0] = Tileset.WALL;
            tiles[x][height - 1] = Tileset.WALL;
        }
        for (int y = 0; y < height; y++) {
            tiles[0][y] = Tileset.WALL;
            tiles[width - 1][y] = Tileset.WALL;
        }
    }

    private void createRoomObjects(long seed) {
        Random rand = new Random(seed);
        int roomWidth, roomHeight;
        Coordinate roomBl;
        int numRooms = rand.nextInt(20);
        if (numRooms < 15) {
            numRooms = 15;
        }
        int i = 0;
        while (i < numRooms) {
            roomWidth = ensureCorrectRoomDim(rand.nextInt(20));
            roomHeight = ensureCorrectRoomDim(rand.nextInt(15));
            roomBl = new Coordinate(rand.nextInt(worldWidth - roomWidth - 1),
                    rand.nextInt(worldHeight - roomHeight - 1));
            Room toAdd = new Room(roomBl, roomWidth, roomHeight);

            if (!isOverlap(toAdd, rooms)) {
                rooms.addLast(toAdd);
                if (fireDoor == null) {
                    fireDoor = toAdd.center;
                } else if (waterDoor == null) {
                    waterDoor = toAdd.center;
                } else {
                    armor = toAdd.center;
                }
                i += 1;
            }
        }
    }

    private boolean isOverlap(Room toAdd, LinkedList<Room> roomsToAdd) {
        if (rooms.isEmpty()) {
            return false;
        }
        for (Room room : roomsToAdd) {
            if (!(toAdd.bottomLeft.x > room.topRight.x
                    || toAdd.topRight.x < room.bottomLeft.x)
                    && !(toAdd.bottomLeft.y > room.topRight.y
                    || toAdd.topRight.y < room.bottomLeft.y)) {
                return true;
            }
        }
        return false;
    }

    private int ensureCorrectRoomDim(int dim) {
        if (dim < 5) {
            dim = 5;
        }
        if (dim % 2 == 0) {
            dim += 1;
        }
        return dim;
    }


    private void addRoom(Room room, int floorType) {
        for (int x = room.bottomLeft.x; x <= room.topRight.x; x++) {
            for (int y = room.bottomLeft.y; y <= room.topRight.y; y++) {
                if (x == room.bottomLeft.x
                        || x == room.topRight.x || y == room.bottomLeft.y
                        || y == room.topRight.y) {
                    if (tiles[x][y].equals(Tileset.NOTHING)) {
                        tiles[x][y] = Tileset.WALL;
                    }
                } else {
                    if (floorType == 0) {
                        tiles[x][y] = Tileset.FIRE;
                    } else if (floorType == 1) {
                        tiles[x][y] = Tileset.WATER;
                    } else {
                        tiles[x][y] = Tileset.FLOOR;
                    }
                    if (!inEncounter) {
                        if (x == fireDoor.x && y == fireDoor.y) {
                            tiles[x][y] = Tileset.FIRE_DOOR;
                        } else if (x == waterDoor.x && y == waterDoor.y) {
                            tiles[x][y] = Tileset.WATER_DOOR;
                        } else if (x == armor.x && y == armor.y) {
                            tiles[x][y] = Tileset.ARMOR;
                        }
                    }
                }
            }
        }
    }

    private void addRooms() {
        Random rand = new Random(worldSeed);
        for (Room room : rooms) {
            addRoom(room, rand.nextInt(3));
        }
        unblock();
    }

    private void makeHorizHallwayFragment(int x, int y) {
        tiles[x][y] = Tileset.FLOOR;
        tiles[x][y + 1] = Tileset.WALL;
        tiles[x][y - 1] = Tileset.WALL;

    }

    private void addHorizHallway(Coordinate start, Coordinate end) {
        if (end.x < start.x) {
            Coordinate temp = start;
            start = end;
            end = temp;
        }
        for (int x = start.x; x <= end.x; x++) {
            if (x < worldWidth) {
                makeHorizHallwayFragment(x, start.y);
            } else {
                break;
            }
        }
    }

    private void makeVertHallwayFragment(int x, int y) {
        tiles[x][y] = Tileset.FLOOR;
        tiles[x + 1][y] = Tileset.WALL;
        tiles[x - 1][y] = Tileset.WALL;
    }

    private void addVertHallway(Coordinate start, Coordinate end) {
        if (start.y > end.y) {
            Coordinate temp = start;
            start = end;
            end = temp;
        }
        for (int y = start.y; y <= end.y; y++) {
            if (y < worldHeight) {
                makeVertHallwayFragment(start.x, y);
            } else {
                break;
            }
        }
    }

    private void addHallway(Coordinate start, Coordinate end) {
        addHorizHallway(new Coordinate(start.x, end.y), end);
        addVertHallway(start, new Coordinate(start.x, end.y));
    }

    private void unblock() {
        for (int x = 1; x < worldWidth; x++) {
            for (int y = 1; y < worldHeight; y++) {
                if (tiles[x][y].equals(Tileset.WALL)) {
                    if ((tiles[x + 1][y].equals(Tileset.FLOOR)
                            && tiles[x - 1][y].equals(Tileset.FLOOR))
                            || (tiles[x][y + 1].equals(Tileset.FLOOR)
                            && tiles[x][y - 1].equals(Tileset.FLOOR))) {
                        tiles[x][y] = Tileset.FLOOR;
                    }
                }
            }
        }
    }

    private void addHallways() {
        Room currRoom = null;
        Room nextRoom = null;
        LinkedList<Room> copy = new LinkedList<>(rooms);
        while (!copy.isEmpty()) {
            nextRoom = copy.poll();
            if (!(currRoom == null)) {
                addHallway(currRoom.center, nextRoom.center);
            }
            currRoom = nextRoom;
        }
        unblock();
    }


    public Boolean movePlayer(TETile player, Coordinate currPos, Coordinate targetPos) {
        TETile targetTile = tiles[targetPos.x][targetPos.y];
        if (!targetTile.equals(Tileset.WALL) && !targetTile.equals(Tileset.NOTHING)
                && !targetTile.equals(Tileset.FIRE_GUY) && !targetTile.equals(Tileset.WATER_LADY)) {
            if (currPos.equals(fireDoor)) {
                tiles[currPos.x][currPos.y] = Tileset.FIRE_DOOR;
            } else if (currPos.equals((waterDoor))) {
                tiles[currPos.x][currPos.y] = Tileset.WATER_DOOR;
            } else {
                tiles[currPos.x][currPos.y] = Tileset.FLOOR;
            }
            if (inEncounter) {
                if (targetTile.equals(Tileset.GEM)) {
                    gemsCollected += 1;
                }
                encounterStepsLeft -= 1;
            }
            if (targetTile.equals(Tileset.ARMOR)) {
                armorStepsLeft += 101;

            }
            if (armorStepsLeft > 0) {
                armorStepsLeft -= 1;
            }
            if (player.equals(Tileset.FIRE_GUY)) {
                if (totalStepsTaken > TRAIL_STEPS_LIMIT) {
                    tiles[currPos.x][currPos.y] = Tileset.FIRE;
                } else {
                    tiles[currPos.x][currPos.y] = Tileset.FLOOR;
                }
                if (targetTile.equals(Tileset.WATER) && armorStepsLeft == 0) {
                    //Encounter
                    createEncounter(Tileset.WATER_LADY);
                    inEncounter = true;
                } else {
                    tiles[targetPos.x][targetPos.y] = player;
                }
                currPosP1 = targetPos.copy();
            } else if (player.equals(Tileset.WATER_LADY)) {
                if (totalStepsTaken > TRAIL_STEPS_LIMIT) {
                    tiles[currPos.x][currPos.y] = Tileset.WATER;
                } else {
                    tiles[currPos.x][currPos.y] = Tileset.FLOOR;
                }
                if (targetTile.equals(Tileset.FIRE) && armorStepsLeft == 0) {
                    //Encounter
                    createEncounter(Tileset.FIRE_GUY);
                    inEncounter = true;
                } else {
                    tiles[targetPos.x][targetPos.y] = player;
                }

                currPosP2 = targetPos.copy();
            }
            if (currPos.equals(fireDoor)) {
                tiles[currPos.x][currPos.y] = Tileset.FIRE_DOOR;
            } else if (currPos.equals((waterDoor))) {
                tiles[currPos.x][currPos.y] = Tileset.WATER_DOOR;
            }
            return true;
        }
        return false;
    }


    public void move(TETile player, Coordinate currPos, char input) {
        totalStepsTaken += 1;
        keystrokeHistory.append(input);
        if (input == 'W' || input == 'I') {
            movePlayer(player, currPos, currPos.shift(0, 1));
        } else if (input == 'S' || input == 'K') {
            movePlayer(player, currPos, currPos.shift(0, -1));
        } else if (input == 'A' || input == 'J') {
            movePlayer(player, currPos, currPos.shift(-1, 0));
        } else if (input == 'D' || input == 'L') {
            movePlayer(player, currPos, currPos.shift(1, 0));
        }
    }

    public void spawnPlayer(String player) {
        Random rand = new Random(worldSeed);
        while (true) {
            int randX = rand.nextInt(worldWidth);
            int randY = rand.nextInt(worldHeight);

            if (tiles[randX][randY].equals(Tileset.FLOOR)) {
                if (player.equals("P1")) {
                    tiles[randX][randY] = Tileset.FIRE_GUY;
                    currPosP1 = new Coordinate(randX, randY);
                } else if (player.equals("P2")) {
                    tiles[randX][randY] = Tileset.WATER_LADY;
                    currPosP2 = new Coordinate(randX, randY);
                }
                break;
            }
        }
    }

    private TETile[][] copyWorld(TETile[][] inTiles) {
        int xLength = inTiles.length, yLength = inTiles[0].length;
        TETile[][] copy = new TETile[xLength][yLength];
        for (int x  = 0; x < xLength; x++) {
            System.arraycopy(inTiles[x], 0, copy[x], 0, yLength);
        }
        return copy;
    }

    public void checkEncounter() {
        if (gemsCollected == 5) {
            gemsCollected = 0;
            totalEncounterSteps -= 5;
            encounterStepsLeft = totalEncounterSteps;
            inEncounter = false;
            tiles = prevWorld;
            movePlayer(Tileset.FIRE_GUY, prevPosP1, prevPosP1);
            movePlayer(Tileset.WATER_LADY, prevPosP2, prevPosP2);
        } else if (encounterStepsLeft <= 0) {
            gameLost = true;
        }
    }

    public void createEncounter(TETile player) {
        tiles[currPosP1.x][currPosP1.y] = Tileset.FLOOR;
        tiles[currPosP2.x][currPosP2.y] = Tileset.FLOOR;
        prevPosP1 = currPosP1.copy();
        prevPosP2 = currPosP2.copy();
        prevWorld = copyWorld(tiles);
        Coordinate location = new Coordinate((worldWidth / 2) - 9, (worldHeight / 2) - 4);
        initWorld(worldWidth + 1, worldHeight + 1);
        Room encounterRoom = new Room(location, 20, 10);
        addRoom(encounterRoom, 3);
        movePlayer(player, encounterRoom.center, encounterRoom.center);
        addGems(encounterRoom);
    }
    private void addGems(Room room) {
        int numGems = 5;
        while (numGems > 0) {
            int randX = worldRand.nextInt(room.topRight.x);
            int randY = worldRand.nextInt(room.topRight.y);
            int xPos = min(randX, randX + room.bottomLeft.x);
            int yPos = min(randY, randY + room.bottomLeft.y);
            if (tiles[xPos][yPos].equals(Tileset.FLOOR)) {
                tiles[xPos][yPos] = Tileset.GEM;
                numGems -= 1;
            }
        }
    }
}
