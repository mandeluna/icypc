package oocl.icypc;// A simple player that just tries to hit children on the opponent's
// team with snowballs.
//
// Feel free to use this as a starting point for your own player.
//
// ICPC Challenge
// Sturgill, Baylor University

import icypc.Const;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.Random;
import java.awt.Point;

public class Hybrid {
  // Constant used to mark child locations in the map.
  public static final int GROUND_CHILD = 10;

  /**
   * Current game score for self (red) and opponent (blue).
   */
  private static int[] score = new int[2];

  /**
   * Current snow height in each cell.
   */
  private static int[][] height = new int[Const.SIZE][Const.SIZE];

  /**
   * Contents of each cell.
   */
  private static int[][] ground = new int[Const.SIZE][Const.SIZE];

  /**
   * List of children on the field, half for each team.
   */
  private static Child[] cList = new Child[2 * Const.CCOUNT];

  /*
   * Return the value of x, clamped to the [ a, b ] range.
   */
  static int clamp(int x, int a, int b) {
    if (x < a) {
      return a;
    }
    if (x > b) {
      return b;
    }
    return x;
  }

  static class Child {

    Point pos = new Point();
    boolean standing;
    int color;
    int holding;
    int dazed;

    /**
     * Current instruction this child is executing.
     */
    int state = 0;

    /**
     * Current destination of this child.
     */
    Point runTarget = new Point();

    /**
     * How many more turns is this child going to run toward the target.
     */
    int runTimer;

    Activity activity;

    /**
     * Return a move to get this child closer to target.
     */
    Move moveToward(Point target) {
      if (standing) {
        // Run to the destination
        if (pos.x != target.x) {
          if (pos.y != target.y) {
            // Run diagonally.
            return new Move("run",
                pos.x + clamp(target.x - pos.x, -1, 1),
                pos.y + clamp(target.y - pos.y, -1, 1));
          }
          // Run left or right
          return new Move("run",
              pos.x + clamp(target.x - pos.x, -2, 2),
              pos.y);
        }

        if (pos.y != target.y)
          // Run up or down.
          return new Move("run",
              pos.x,
              pos.y + clamp(target.y - pos.y, -2, 2));
      } else {
        // Crawl to the destination
        if (pos.x != target.x)
          // crawl left or right
          return new Move("crawl",
              pos.x + clamp(target.x - pos.x, -1, 1),
              pos.y);

        if (pos.y != target.y)
          // crawl up or down.
          return new Move("crawl",
              pos.x,
              pos.y + clamp(target.y - pos.y, -1, 1));
      }

      // Nowhere to move, just return the idle move.
      return new Move();
    }

    public Move chooseMove() {
      return activity == null ? new Move() : activity.chooseMove(this);
    }
  }

  // Simple representation for a child's action
  static class Move {
    Move() {
      action = "idle";
    }

    Move(String act) {
      action = act;
    }

    Move(String act, int x, int y) {
      action = act;
      dest = new Point(x, y);
    }

    String action = "idle";

    Point dest = null;
  }

  /*
   * An activity is a named list of moves carried out according to some policy (maybe ad-hoc)
   */
  abstract static class Activity {
    Move[] instructions;
    Point runTarget = new Point();

    // How long the child has left to run toward its destination.
    int runTimer;

    Activity(Move[] instructions) {
      this.instructions = instructions;
    }

    abstract public Move chooseMove(Child c);
  }

  static class PlanterActivity extends Activity {

    static Move[] snowmanMoves = {
        new Move("idle"),
        new Move("crouch"),
        new Move("pickup", 1, 0),
        new Move("pickup", 1, 0),
        new Move("pickup", 1, 0),
        new Move("crush"),
        new Move("drop", 1, 0),
        new Move("pickup", 1, 1),
        new Move("pickup", 1, 1),
        new Move("crush"),
        new Move("drop", 1, 0),
        new Move("pickup", 1, 1),
        new Move("crush"),
        new Move("drop", 1, 0),
        new Move("stand"),
    };

    PlanterActivity() {
      super(snowmanMoves);
    }

    public Move chooseMove(Child c) {
      if (c.dazed > 0) {
        return new Move();
      }

      if (c.state == 0) {
        // Not building a snowman.

        // If we didn't get to finish the last snowman, maybe we're holding something.
        // We should drop it.
        if (c.holding != Const.HOLD_EMPTY &&
            c.pos.y < Const.SIZE - 1 &&
            height[c.pos.x][c.pos.y + 1] <= Const.MAX_PILE - 3) {
          return new Move("drop", c.pos.x, c.pos.y + 1);
        }

        // Find the nearest neighbor.
        int nearDist = 1000;
        for (int i = 0; i < Const.SIZE; i++)
          for (int j = 0; j < Const.SIZE; j++)
            if ((i != c.pos.x || j != c.pos.y) &&
                (ground[i][j] == GROUND_CHILD ||
                    ground[i][j] == Const.GROUND_SMR)) {

              int dx = (c.pos.x - i);
              int dy = (c.pos.y - j);

              if (dx * dx + dy * dy < nearDist) {
                nearDist = dx * dx + dy * dy;
              }
            }

//        System.err.println("   --- planter nearest neighbor is " + nearDist);

        // See if we should start running our build script.
        // Are we far from other things, is the ground empty
        // and do we have enough snow to build a snowman.
        if (nearDist > 5 * 5 &&
            c.pos.x < Const.SIZE - 1 &&
            c.pos.y < Const.SIZE - 1 &&
            ground[c.pos.x + 1][c.pos.y] == Const.GROUND_EMPTY &&
            ground[c.pos.x + 1][c.pos.y + 1] == Const.GROUND_EMPTY &&
            height[c.pos.x + 1][c.pos.y] >= 3 &&
            height[c.pos.x + 1][c.pos.y + 1] >= 3 &&
            c.holding == Const.HOLD_EMPTY) {
          // Start trying to build a snowman.
          c.state = 1;
        }
      }

      // Are we building a snowman?
      if (c.state > 0) {
        // Stamp out a move from our instruction template and return it.
        Move m = new Move(instructions[c.state].action);
        if (instructions[c.state].dest != null) {
          m.dest = new Point(c.pos.x + instructions[c.state].dest.x,
              c.pos.y + instructions[c.state].dest.y);
        }
        c.state = (c.state + 1) % instructions.length;

        return m;
      }

      // Run around looking for a good place to build

      // See if the child needs a new, random destination.
      while (runTimer <= 0 ||
          runTarget.equals(c.pos)) {
        // Pick somewhere to run, omit the top and righmost edges.
        runTarget.setLocation(rnd.nextInt(Const.SIZE - 1),  rnd.nextInt(Const.SIZE - 1));
        runTimer = 1 + rnd.nextInt(14);
      }

      runTimer--;
//      System.err.println("   --- planter is moving towards " + runTarget);
      return c.moveToward(runTarget);
    }
  }

  static class HunterActivity extends Activity {

    public HunterActivity() {
      super(null);
    }

    public Move chooseMove(Child c) {
      Move m = new Move();

      if (c.dazed == 0) {
        // See if the child needs a new destination.
        while (runTimer <= 0 || runTarget.equals(c.pos)) {
          runTarget.setLocation(rnd.nextInt(Const.SIZE), rnd.nextInt(Const.SIZE));
          runTimer = 1 + rnd.nextInt(14);
        }

        // Try to acquire a snowball if we need one.
        if (c.holding != Const.HOLD_S1) {
          // Crush into a snowball, if we have snow.
          if (c.holding == Const.HOLD_P1) {
            m.action = "crush";
          } else {
            // We don't have snow, see if there is some nearby.
            int sx = -1, sy = -1;
            for (int ox = c.pos.x - 1; ox <= c.pos.x + 1; ox++)
              for (int oy = c.pos.y - 1; oy <= c.pos.y + 1; oy++) {
                // Is there snow to pick up?
                if (ox >= 0 && ox < Const.SIZE &&
                    oy >= 0 && oy < Const.SIZE &&
                    (ox != c.pos.x || oy != c.pos.y) &&
                    ground[ox][oy] == Const.GROUND_EMPTY &&
                    height[ox][oy] > 0) {
                  sx = ox;
                  sy = oy;
                }
              }

//            System.err.println("   --- hunter looking for snow nearby " + sx);

            // If there is snow, try to get it.
            if (sx >= 0) {
              if (c.standing) {
                m.action = "crouch";
              } else {
                m.action = "pickup";
                m.dest = new Point(sx, sy);
              }
            }
          }
        } else {
          // Stand up if the child is armed.
          if (!c.standing) {
            m.action = "stand";
          } else {
            // Try to find a victim.
            boolean victimFound = false;
            for (int j = Const.CCOUNT; !victimFound && j < Const.CCOUNT * 2; j++) {
              if (cList[j].pos.x >= 0) {
                int dx = cList[j].pos.x - c.pos.x;
                int dy = cList[j].pos.y - c.pos.y;
                int dsq = dx * dx + dy * dy;
                if (dsq < 8 * 8) {
                  victimFound = true;
                  m.action = "throw";
                  // throw past the victim, so we will probably hit them
                  // before the snowball falls into the snow.
                  m.dest = new Point(c.pos.x + dx * 2, c.pos.y + dy * 2);
                }
              }
            }
          }
        }

        // Try to run toward the destination.
        if (m.action.equals("idle")) {
//          System.err.println("   --- hunter is moving towards " + runTarget);
          m = c.moveToward(runTarget);
          runTimer--;
        }
      }

      return m;
    }
  }

  static Random rnd = new Random();

  public void run() {

    for (int i = 0; i < cList.length; i++) {
      Child player = new Child();
      if (i == 0 || i == 1) {
        player.activity = new HunterActivity();
      }
      if (i == 2 || i == 3) {
        player.activity = new PlanterActivity();
      }
      cList[i] = player;
    }

    // Scanner to parse input from the game engine.
    Scanner in = new Scanner(System.in);

    // Keep reading states until the game ends.
    int turnNum = in.nextInt();
    while (turnNum >= 0) {
      String token;

      // Read current game score.
      score[Const.RED] = in.nextInt();
      score[Const.BLUE] = in.nextInt();

      // Parse the current map.
      readCurrentMap(height, ground, in);

      if (!areAllOpponentsVisible()) {
        recordMapVisibilityWithPrefix("Map Visibility at turn " + turnNum);
      }

      // Read the states of all the children.
      readChildrenStates(cList, in);

      // Mark all the children in the map, so they are easy to
      // look up.
      markChildren(ground, cList);

      System.err.println("Height Map");
      for (int i = 0; i < Const.SIZE; i++) {
        for (int j = 0; j < Const.SIZE; j++) {
          System.err.print(" " + height[i][j]);
        }
        System.err.println();
      }

      System.err.println("Ground Map");
      for (int i = 0; i < Const.SIZE; i++) {
        for (int j = 0; j < Const.SIZE; j++) {
          // translate ground map back to original format
          char code = (ground[i][j] == -1) ? '*' : (char) (ground[i][j] + 'a');
          System.err.print(" " + code);
        }
        System.err.println();
      }

      // Decide what each child should do
      for (int i = 0; i < Const.CCOUNT; i++) {
        Move m = cList[i].chooseMove();
//        System.err.println(String.format("child %d move is %s", i, m.action));

        /** Write out the child's move */
        if (m.dest == null) {
          System.out.println(m.action);
        } else {
          System.out.println(m.action + " " + m.dest.x + " " + m.dest.y);
        }
      }

      turnNum = in.nextInt();
    }
  }

  public static boolean areAllOpponentsVisible() {
    // last 4 children in the array represent opponents
    for (int i = Const.CCOUNT; i < Const.CCOUNT * 2; i++) {
      if (cList[i].pos.x == -1 || cList[i].pos.y == -1) {
        return false;
      }
    }
    return true;
  }

  public static void recordMapVisibilityWithPrefix(String prefixMessage) {
    String fileName = "visibility.txt";

    try {
      // Open given file in append mode.
      BufferedWriter out = new BufferedWriter(
          new FileWriter(fileName, true));
      out.write(prefixMessage);
      out.write('\n');
      for (int i = 0; i < Const.SIZE; i++) {
        for (int j = 0; j < Const.SIZE; j++) {
          // translate ground map back to original format
          if (j > 0) out.write(' ');
          if (ground[i][j] == -1) {
            out.write("*");
          }
          else {
            String code = String.format("%d%s", height[i][j], (char)(ground[i][j] + 'a'));
            out.write(code);
          }
        }
        out.write('\n');
      }

      // now write children state
      for (int i = 0; i < Const.CCOUNT * 2; i++) {
        // {x:0, y:2, stance:'S', carry:'a', dazed:0}
        Child c = cList[i];
        String status = String.format("%d %d %s %s %d\n", c.pos.x, c.pos.y,
            c.standing ? "S" : "C", (char)(c.holding + 'a'), c.dazed);
        out.write(status);
      }

      out.close();
    }
    catch (IOException e) {
      System.err.println("exception occurred" + e);
    }
  }

  public static void main(String[] args) {
    Hybrid hybrid = new Hybrid();
    hybrid.run();
  }

  private static void markChildren(int[][] ground, Child[] cList) {
    for (int i = 0; i < Const.CCOUNT * 2; i++) {
      Child c = cList[i];
      if (c.pos.x >= 0) {
        ground[c.pos.x][c.pos.y] = GROUND_CHILD;
      }
    }
  }

  private static void readCurrentMap(int[][] height, int[][] ground, Scanner in) {
    String token;
    for (int i = 0; i < Const.SIZE; i++) {
      for (int j = 0; j < Const.SIZE; j++) {
        // Can we see this cell?
        token = in.next();
        if (token.charAt(0) == '*') {
          height[i][j] = -1;
          ground[i][j] = -1;
        } else {
          height[i][j] = token.charAt(0) - '0';
          ground[i][j] = token.charAt(1) - 'a';
        }
      }
    }
  }

  private static void readChildrenStates(Child[] cList, Scanner in) {
    String token;
    for (int i = 0; i < Const.CCOUNT * 2; i++) {
      Child c = cList[i];

      // Can we see this child?
      token = in.next();
      if (token.equals("*")) {
        c.pos.x = -1;
        c.pos.y = -1;
      } else {
        // Record the child's location.
        c.pos.x = Integer.parseInt(token);
        c.pos.y = in.nextInt();

        // Compute child color based on it's index.
        c.color = (i < Const.CCOUNT ? Const.RED : Const.BLUE);

        // Read the stance, what the child is holding and how much
        // longer he's dazed.
        token = in.next();
        c.standing = token.equals("S");

        token = in.next();
        c.holding = token.charAt(0) - 'a';

        c.dazed = in.nextInt();
      }
    }
  }
}
