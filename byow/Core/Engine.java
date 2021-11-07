package byow.Core;

import byow.InputDemo.InputSource;
import byow.InputDemo.KeyboardInputSource;
import byow.InputDemo.StringInputDevice;
import byow.TileEngine.TERenderer;
import byow.TileEngine.TETile;
import byow.TileEngine.Tileset;
import edu.princeton.cs.introcs.StdDraw;
import java.awt.Color;
import java.awt.Font;
import java.io.File;

import static byow.Core.PersistenceUtils.*;
import static java.lang.Math.max;

public class Engine {
    TERenderer ter = new TERenderer();
    World world;
    boolean gameOver;

    public static final int WIDTH = 100;
    public static final int HEIGHT = 50;
    public static final File CWD = join(join(new File(System.getProperty("user.dir")),
            "byow"), "Core");
    public static final File SAVE_LOCATION = join(CWD, "world.txt");

    public Engine() {
        world = new World(WIDTH, HEIGHT);
        gameOver = false;
    }

    /**
     * Method used for exploring a fresh world. This method should handle all inputs,
     * including inputs from the main menu.
     */
    public void interactWithKeyboard() {
        InputSource keyboardInput = new KeyboardInputSource();
        interact(keyboardInput, "keyboard");
    }

    public void interact(InputSource inputSource, String inputSourceType) {
        boolean startNew = false, mainMenu = true, gameStarted = false;
        boolean aboutToQuit = false;
        StringBuilder strSeed = new StringBuilder();
        if (inputSourceType.equals("keyboard")) {
            initStdDraw();
        }
        while (inputSource.possibleNextInput() && !gameOver) {
            if (mainMenu) {
                if (inputSourceType.equals("keyboard")) {
                    showMainMenu();
                }
                char input = inputSource.getNextKey();
                if (input == 'N') {
                    startNew = true;
                    mainMenu = false;
                } else if (input == 'L') {
                    GameState saveState = readObject(SAVE_LOCATION, GameState.class);
                    String interactString = "N" + saveState.seed.toString() + "S"
                            + saveState.keystrokeHistory;
                    interactWithInputString(interactString);
                    if (inputSourceType.equals("keyboard")) {
                        ter.initialize(world.worldWidth + 1, world.worldHeight + 6);
                        render();
                    }
                    mainMenu = false;
                    gameStarted = true;
                } else if (input == 'Q') {
                    if (inputSourceType.equals("keyboard")) {
                        System.exit(0);
                    } else if (inputSourceType.equals("string")) {
                        break;
                    }
                } else if (input == 'B') {
                    showLoreMenu();
                }
            }
            if (startNew) {
                if (inputSourceType.equals("keyboard")) {
                    showSeedMenu(strSeed.toString());
                }
                char input = inputSource.getNextKey();
                if (Character.isDigit(input)) {
                    strSeed.append(input);
                }
                if (input == 'S') {
                    if (inputSourceType.equals("keyboard")) {
                        ter.initialize(world.worldWidth + 1, world.worldHeight + 6);
                    }
                    world.generate(Long.parseLong(strSeed.toString()));
                    if (inputSourceType.equals("keyboard")) {
                        render();
                    }
                    startNew = false;
                    gameStarted = true;
                }
            }
            if (gameStarted) {
                String gameOverResult = checkGameOver();
                if (gameOverResult != null) {
                    if (inputSourceType.equals("keyboard")) {
                        gameOverScreen(gameOverResult);
                    } else if (inputSourceType.equals("string")) {
                        break;
                    }
                }
                if (world.inEncounter) {
                    world.checkEncounter();
                    if (inputSourceType.equals("keyboard")) {
                        render();
                    }
                }
                if (inputSourceType.equals("keyboard")) {
                    showUI();
                    if (StdDraw.hasNextKeyTyped()) {
                        char input = inputSource.getNextKey();
                        if (!aboutToQuit) {
                            if (input == ':') {
                                aboutToQuit = true;
                            }
                        } else {
                            if (input == 'Q') {
                                quitWorld(world, true);
                            }
                        }
                        handleMovement(input);
                        render();
                    }
                } else if (inputSourceType.equals("string")) {
                    try {
                        char input = inputSource.getNextKey();
                        if (!aboutToQuit) {
                            if (input == ':') {
                                aboutToQuit = true;
                            }
                        } else {
                            if (input == 'Q') {
                                quitWorld(world, false);
                            }
                        }
                        handleMovement(input);
                    } catch (StringIndexOutOfBoundsException e) {
                        break;
                    }
                }
            }
        }
    }

    public TETile[][] interactWithInputString(String input) {
        System.out.println(input.toUpperCase());
        InputSource stringInput = new StringInputDevice(input.toUpperCase());
        interact(stringInput, "string");
        return world.tiles;
    }


    public void showMainMenu() {
        StdDraw.clear(Color.BLACK);
        StdDraw.setFont(new Font("Monaco", Font.BOLD, 50));
        StdDraw.text(WIDTH / 2, HEIGHT * 3 / 4, "Fireguy and Waterlady");
        StdDraw.setFont(new Font("Monaco", Font.BOLD, 30));
        StdDraw.text(WIDTH / 2, (HEIGHT / 2) + 2, "New Game (N)");
        StdDraw.text(WIDTH / 2, (HEIGHT / 2) - 2, "Load Game (L)");
        StdDraw.text(WIDTH / 2, (HEIGHT / 2) - 6, "Lore/Instructions (B)");
        StdDraw.text(WIDTH / 2, (HEIGHT / 2) - 10, "Quit (Q)");
        StdDraw.show();
    }

    public void showSeedMenu(String currSeed) {
        StdDraw.clear(Color.BLACK);
        StdDraw.text(WIDTH / 2, HEIGHT * 2 / 3, "Enter Seed!");
        StdDraw.text(WIDTH / 2, HEIGHT / 2, currSeed);
        StdDraw.show();
    }

    public void showLoreMenu() {
        StdDraw.clear(Color.BLACK);
        StdDraw.text(WIDTH / 2, (HEIGHT * 3 / 4) + 9, "Lore: ");
        StdDraw.text(WIDTH / 2, (HEIGHT * 3 / 4) + 6, "Fireguy and "
                + "Waterlady (not to be confused with Fireboy and Watergirl)");
        StdDraw.text(WIDTH / 2, (HEIGHT * 3 / 4) + 3, "are stuck inside "
                + "an underground temple filled with fire and water (go figure).");
        StdDraw.text(WIDTH / 2, (HEIGHT * 3 / 4) - 0, "The only way"
                + " out is through their respective doors, but reaching them will be");
        StdDraw.text(WIDTH / 2, (HEIGHT * 3 / 4) - 3, "no easy task."
                + " They must work together and use their");
        StdDraw.text(WIDTH / 2, (HEIGHT * 3 / 4) - 6, "elemental abilities "
                + "if they want to make it out alive.");
        StdDraw.text(WIDTH / 2, (HEIGHT * 3 / 4) - 9, "Instructions: ");
        StdDraw.text(WIDTH / 2, (HEIGHT * 3 / 4) - 12, "Control Fireguy "
                + "with WASD and Waterlady with IJKL.");
        StdDraw.text(WIDTH / 2, (HEIGHT * 3 / 4) - 15, "Fireguy absorbs "
                + "fire but also leaves a fire trail behind.");
        StdDraw.text(WIDTH / 2, (HEIGHT * 3 / 4) - 18, "Waterlady absorbs "
                + "water but also leaves a water trail behind.");
        StdDraw.text(WIDTH / 2, (HEIGHT * 3 / 4) - 21, "Picking up armor"
                + " grants both players immunity to either element");
        StdDraw.text(WIDTH / 2, (HEIGHT * 3 / 4) - 24, "but only lasts"
                + " for 100 steps combined.");
        StdDraw.text(WIDTH / 2, (HEIGHT * 3 / 4) - 27, "If either player"
                + " walks into the opposite element,");
        StdDraw.text(WIDTH / 2, (HEIGHT * 3 / 4) - 30, "the other can"
                + " revive them by collecting all the gems");
        StdDraw.text(WIDTH / 2, (HEIGHT * 3 / 4) - 33, "before running "
                + "out of steps. If they run out of steps, it's game over!");
        StdDraw.show();
        StdDraw.pause(30000);
        showMainMenu();
    }

    public void showUI() {
        if ((int) StdDraw.mouseX() < WIDTH && (int) StdDraw.mouseY() < HEIGHT) {
            TETile currTile = world.tiles[(int) StdDraw.mouseX()][(int) StdDraw.mouseY()];
            StdDraw.setPenColor(Color.BLACK);
            StdDraw.filledRectangle(WIDTH - 3, HEIGHT + 3, 4, 3);
            StdDraw.setPenColor(Color.WHITE);
            StdDraw.textRight(WIDTH - 1, HEIGHT + 3, currTile.description());

        }
        if (world.inEncounter) {
            StdDraw.setPenColor(Color.BLACK);
            StdDraw.filledRectangle(3, HEIGHT + 3, 6, 2);
            StdDraw.setPenColor(Color.WHITE);
            StdDraw.text(WIDTH / 2, HEIGHT + 4, "Steps Remaining: " + world.encounterStepsLeft);
            StdDraw.text(WIDTH / 2, HEIGHT + 2, "Gems Collected: " + world.gemsCollected);
        }
        StdDraw.setPenColor(Color.BLACK);
        StdDraw.filledRectangle(3, HEIGHT + 3, 6, 2);
        if (world.armorStepsLeft > 0) {
            StdDraw.setPenColor(Color.WHITE);
            StdDraw.textLeft(1, HEIGHT + 2, "Armor durability: " + world.armorStepsLeft);
        }
        StdDraw.setPenColor(Color.WHITE);
        StdDraw.textLeft(1, HEIGHT + 4, "Steps till Trails: " + max((
                world.getTrailStepsLimit() - world.totalStepsTaken), 0));
        StdDraw.show();
    }



    public void quitWorld(World inWorld, boolean sysExit) {
        System.out.println(CWD.toString());
        System.out.println(SAVE_LOCATION.toString());
        writeObject(SAVE_LOCATION, new GameState(inWorld.worldSeed,
                world.currPosP1,
                world.currPosP2,
                world.keystrokeHistory.toString()));
        if (sysExit) {
            System.exit(0);
        }
    }

    public void handleMovement(char input) {
        // Fire guy movement
        if (input == 'W') {
            //move up
            world.move(Tileset.FIRE_GUY, world.currPosP1, input);
        } else if (input == 'A') {
            //move left
            world.move(Tileset.FIRE_GUY, world.currPosP1, input);
        } else if (input == 'S') {
            //move down
            world.move(Tileset.FIRE_GUY, world.currPosP1, input);
        } else if (input == 'D') {
            //move right
            world.move(Tileset.FIRE_GUY, world.currPosP1, input);
        }

        // Water lady movement
        if (input == 'I') {
            //move up
            world.move(Tileset.WATER_LADY, world.currPosP2, input);
        } else if (input == 'J') {
            //move left
            world.move(Tileset.WATER_LADY, world.currPosP2, input);
        } else if (input == 'K') {
            //move down
            world.move(Tileset.WATER_LADY, world.currPosP2, input);
        } else if (input == 'L') {
            //move right
            world.move(Tileset.WATER_LADY, world.currPosP2, input);
        }
    }

    public void render() {
        ter.renderFrame(world.tiles);
    }

    public void initStdDraw() {
        StdDraw.setCanvasSize(WIDTH * 16, HEIGHT * 16);
        Font font = new Font("Monaco", Font.BOLD, 30);
        StdDraw.setFont(font);
        StdDraw.setPenColor(StdDraw.WHITE);
        StdDraw.setXscale(0, WIDTH);
        StdDraw.setYscale(0, HEIGHT);
        StdDraw.enableDoubleBuffering();
        StdDraw.clear(Color.BLACK);
    }

    public String checkGameOver() {
        if (world.currPosP1.equals(world.fireDoor) && world.currPosP2.equals(world.waterDoor)) {
            gameOver = true;
            return "win";
        }
        if (world.gameLost) {
            gameOver = true;
            return "lose";
        }
        return null;
    }

    public void gameOverScreen(String result) {
        if (result.equals("win")) {
            StdDraw.clear(Color.BLACK);
            StdDraw.setPenColor(Color.WHITE);
            StdDraw.setFont(new Font("Monaco", Font.BOLD, 50));
            StdDraw.text(WIDTH / 2, (HEIGHT / 2) + 6, "GAME OVER");
            StdDraw.setFont(new Font("Monaco", Font.BOLD, 20));
            StdDraw.text((WIDTH / 2), (HEIGHT / 2) + 3, "YOU WIN!");
            StdDraw.show();
            StdDraw.pause(3000);
            System.exit(0);
        }
        if (result.equals("lose")) {
            StdDraw.clear(Color.BLACK);
            StdDraw.setPenColor(Color.WHITE);
            StdDraw.setFont(new Font("Monaco", Font.BOLD, 50));
            StdDraw.text(WIDTH / 2, (HEIGHT / 2) + 6, "GAME OVER");
            StdDraw.setFont(new Font("Monaco", Font.BOLD, 20));
            StdDraw.text((WIDTH / 2), (HEIGHT / 2) + 3, "YOU LOSE! :(");
            StdDraw.show();
            StdDraw.pause(3000);
            System.exit(0);
        }
    }

}

