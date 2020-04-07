/*
 * A player based on Sturgill, Baylor University example code
 *
 * Implements some of the strategies described at
 * https://observablehq.com/@mandeluna/icpc-strategy-notes
 *
 * Keith Kwan, Margaret Del Mundo, Arumanthian Peter, Steven Wart, OOCL (USA), 2020
 */

package oocl.icypc;

import icypc.Const;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Seeker {
  // Constant used to mark child locations in the map.
  public static final int GROUND_CHILD = 10;

  // Constant used to limit iterations in finding optimal targets for snowman locations
  public static final int MAX_ITERATIONS = Const.SIZE;

  /**
   * Current game score for self (red) and opponent (blue).
   */
  private int[] score = new int[2];

  /**
   * Current snow height in each cell.
   */
  private int[][] height = new int[Const.SIZE][Const.SIZE];

  /**
   * Accessor for testing and path-finding.
   * Do not allow mutation of internal state.
   *
   * @return the height map
   */
  public int[][] getHeight() {
    return copyBoard(height);
  }

  /**
   * Contents of each cell.
   */
  private int[][] ground = new int[Const.SIZE][Const.SIZE];

  /**
   * Accessor for testing and path-finding.
   * Do not allow mutation of internal state.
   *
   * @return the ground map
   */
  public int[][] getGround() {
    return copyBoard(ground);
  }

  /**
   * List of children on the field, half for each team.
   */
  private Player[] cList = new Player[2 * Const.CCOUNT];

  public Seeker() {
    for (int i = 0; i < 2 * Const.CCOUNT; i++) {
      cList[i] = new Player();
    }
  }

  /* --- Geometry --- */

  public static class Point {
    public int x;
    public int y;

    public Point() {}

    public Point(int x, int y) {
      this.x = x;
      this.y = y;
    }

    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o.getClass() != Point.class) {
        return false;
      }
      Point p = (Point)o;
      return (this.x == p.x && this.y == p.y);
    }

    public int hashCode() {
      return Integer.hashCode(x) << 32 + Integer.hashCode(y);
    }

    public Point plus(Point p) {
      return new Point(p.x + this.x, p.y + this.y);
    }

    public String toString() {
      return String.format("[%d, %d]", this.x, this.y);
    }
  }

  // first 4 permutations are crawl directions, last 8 are run directions
  static Point[] permutations = {

      new Point(0, 1),    // north
      new Point(1, 0),    // east
      new Point(-1, -1),  // south
      new Point(-1, 0),   // west

      new Point(1, 1),    // northeast
      new Point(1, -1),   // southwest
      new Point(-1, -1),  // southeast
      new Point(-1, 1),   // northwest

      new Point(0, 2),    // north
      new Point(2, 0),    // east
      new Point(-2, -2),  // south
      new Point(-2, 0)    // west
  };

  /**
   * The player locations used for path finding etc.
   *
   * @return a list of the red team locations
   */
  public List<Point> playerLocations() {
    return Arrays.stream(cList).map(c -> c.pos)
        .filter(pos -> pos.x >= 0 && pos.y >= 0)
        .collect(Collectors.toList());
  }

  public List<Player> players() {
    return Arrays.stream(cList).filter(c -> c.pos.x >= 0 && c.pos.y >= 0)
        .collect(Collectors.toList());
  }

  /**
   * Find the cells neighboring the argument within a distance of 1
   *
   * @param pos the index of the cell with neighbors
   * @return a list of neighboring cell indexes
   */
  public List<Point> neighbors(Point pos) {
    List<Point> destinations = new ArrayList<>();
    int range = 8;

    for (int i = 0; i < range; i++) {
      Point c = pos.plus(permutations[i]);

      if (c.x >= 0 && c.x < Const.SIZE && c.y >= 0 && c.y < Const.SIZE) {
        destinations.add(c);
      }
    }
    return destinations;
  }

  /*
   * Return the value of x, clamped to the [ a, b ] range.
   */
  public static int clamp(int x, int a, int b) {
    if (x < a) {
      return a;
    }
    if (x > b) {
      return b;
    }
    return x;
  }

  public static int round(double x) {
    return (x < 0) ? -(int)(Math.round(-x)) : (int)(Math.round(x));
  }

  public static int euclidean(Point p1, Point p2) {
    return euclidean(p1.x, p1.y, p2.x, p2.y);
  }

  public static int euclidean(int x1, int y1, int x2, int y2) {
    return (int)(Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1)));
  }

  /**
   * Interpolate the linear path between p0 and p1
   *
   * @param p0 origin point
   * @param p1 destination point (target)
   * @return a list of all the cells visited on a linear path between p0 and p1
   */
  public static List<Point> interpolate(Point p0, Point p1) {

    List<Point> points = new ArrayList<>();

    // number of steps required to move
    int n = Math.max(Math.abs(p1.x - p0.x), Math.abs(p1.y - p0.y));

    for (int t = 0; t <= n; t++) {
      int xt = p0.x + round(t * (float)(p1.x - p0.x) / n);
      int yt = p0.y + round(t * (float)(p1.y - p0.y) / n);
      points.add(new Point(xt, yt));
    }
    return points;
  }

  public class Player {

    Point pos = new Point();
    boolean standing;
    int color;
    int holding;
    int dazed;

    public String toString() {
      return String.format("Player(%d, %d)", pos.x, pos.y);
    }

    /**
     * Current instruction this child is executing.
     */
    int state = 0;

    Point runTarget = new Point();

    /**
     * Current set of moves being executed
     */
    Activity activity;

    /**
     * List of friendly players
     *
     * @return a list of children on the receiver's team, excluding itself
     */
    public List<Player> friends() {
      List<Player> friends = new ArrayList<>();
      for (int i = 0; i < Const.CCOUNT; i++) {
        Player c = cList[i];
        if (c == this) {
          continue;
        }
        friends.add(c);
      }
      return friends;
    }

    /**
     * Sort nearby things by distance - good for finding useful items
     *
     * @param cover ground constant to match
     * @return a list of items on the ground matching cover
     */
    public List<Point> nearbyItems(int cover) {
      List<Point> nearby = new ArrayList<>();
      for (int i = 0; i < Const.CCOUNT; i++) {
        for (int j = 0; j < Const.CCOUNT; j++) {
          if (ground[i][j] == cover) {
            nearby.add(new Point(i, j));
          }
        }
      }
      nearby.sort(Comparator.comparingInt(a -> euclidean(pos, a)));
      return nearby;
    }

    /**
     * Just because you can't see someone, doesn't mean they can't see you.
     * This method finds whom the receiver can target (or vice-versa)
     *
     * TODO this method needs some test coverage
     *      1) are we sorting in the correct order
     *      2) are we handling all cases of obstacles
     *
     * @return a collection of visible opponents, sorted by distance
     */
    public List<Player> visibleOpponents() {
      List<Player> visible = new ArrayList<>();
      List<Player> friends = friends();
      int max_range = 20;

      // last 4 children in the array represent opponents
      for (int i = Const.CCOUNT; i < Const.CCOUNT * 2; i++) {
        if (cList[i].pos.x > 0 && cList[i].pos.y > 0) {

          if (euclidean(cList[i].pos, this.pos) > max_range) {
            continue;
          }

          // filter out opponents who are obstructed
          List<Point> obstacles = interpolate(pos, cList[i].pos).stream()
              .filter(p -> ((height[p.x][p.y] >= 6) || (ground[p.x][p.y] != Const.GROUND_EMPTY)))
              .collect(Collectors.toList());

          if (obstacles.isEmpty()) {
            visible.add(cList[i]);
          }
        }
      }

      // sort from nearest to farthest (ascending)
      visible.sort((a, b) -> {
        int ax = a.pos.x - pos.x;
        int ay = a.pos.y - pos.y;
        int bx = a.pos.x - pos.x;
        int by = a.pos.y - pos.y;

        return (ax * ax + ay * ay) - (bx * bx + by * by);
      });

      return visible;
    }

    /**
     * Find the cells neighboring the receiver's position
     *
     * @return a list of neighboring cells
     */
    public List<Point> neighbors() {
      return (standing) ? neighbors8() : neighbors4();
    }

    /**
     * Order the results clockwise from the from assuming the receiver is facing away from the origin
     *
     * @return a list of neighboring cells in 4 cardinal directions (N, E, S, W)
     */
    public List<Point> neighbors4() {
      return neighborsByAngle(Math.PI / 2);
    }

    /**
     * Order the results clockwise from the from assuming the receiver is facing away from the origin
     *
     * @return a list of neighboring cells in 8 directions (N, NE, E, SE, S, SW, W, NW)
     */
    public List<Point> neighbors8() {
      return neighborsByAngle(Math.PI / 4);
    }

    public List<Point> neighborsByAngle(double angle) {
      double offset = Math.atan2(pos.y, pos.x);
      List<Point> neighbors = new ArrayList<>();
      for (double direction = Math.PI * 2 + offset; direction > offset; direction -= angle) {
        Point vec = new Point(pos.x + (int)Math.round(Math.cos(direction)), pos.y + (int)Math.round(Math.sin(direction)));

        if (vec.x >= 0 && vec.x < Const.SIZE && vec.y >= 0 && vec.y < Const.SIZE) {
          neighbors.add(vec);
        }
      }
      return neighbors;
    }

    /**
     * Return a list of the furthest points that can be reached in one turn
     * from the player's current location, without encountering
     * obstacles (trees, snow deeper than 6 units, or other players).
     *
     * If the player is crouching, look only n-w-s-e directions
     * If the player is standing, look 2 units to n-w-s-e and
     * 1 unit to nw-sw-se-nw directions.
     *
     * TODO needs unit tests
     *
     * @return the furthest points that can be safely reached in one turn
     */
    public List<Point> moveDestinations() {
      List<Point> friends = friends().stream().map(ea -> ea.pos).collect(Collectors.toList());
      List<Point> opponents = visibleOpponents().stream().map(ea -> ea.pos).collect(Collectors.toList());

      return neighbors().stream()
          .filter(dest -> !friends.contains(dest) && !opponents.contains(dest))
          .collect(Collectors.toList());
    }

    /**
     * Return a move to get this child closer to target.
     * TODO check for obstacles
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
          return new Move("run",pos.x + clamp(target.x - pos.x, -2, 2), pos.y);
        }

        if (pos.y != target.y) {
          // Run up or down.
          return new Move("run", pos.x,pos.y + clamp(target.y - pos.y, -2, 2));
        }
      }
      else {
        // Crawl to the destination
        if (pos.x != target.x)
          // crawl left or right
          return new Move("crawl",pos.x + clamp(target.x - pos.x, -1, 1), pos.y);

        if (pos.y != target.y)
          // crawl up or down.
          return new Move("crawl", pos.x,pos.y + clamp(target.y - pos.y, -1, 1));
      }

      // Nowhere to move, just return the idle move.
      return new Move();
    }

    /**
     * Run the OODA (observe-orient-decide-act) loop
     *
     * Activities should be as fine-grained as possible (e.g. not "build a snowman" but
     * "build a snowball", "stack a snowball")
     *
     * 1. Observe - find the highest priority opportunity or threat
     * 2. Orient - move to optimal location
     * 3. Decide - reassess opportunity/threat, assign new activity
     * 4. Act - execute activity
     *
     * @return the move to support orientation or activity
     */
    public Move chooseMove() {
      if (dazed > 0) {
        // unable to act
        return new Move();
      }

      // which opponents are potential targets/threats?
      // NB: if we can't see them, it doesn't mean they can't see us
      for (Player threat: visibleOpponents()) {
        if (threat.holding == Const.HOLD_S1 || activity == null || activity.isComplete) {
          // clear and present danger
          if (holding == Const.HOLD_S1) {

            // Stand up if the child is armed.
            if (!standing) {
              // TODO only stand if the target is too far away to hit from a crouching position
              return new Move("stand");
            }
            else {
              // excellent, we are prepared
              System.err.println(this + " is selecting target " + threat);
              int dx = threat.pos.x - pos.x;
              int dy = threat.pos.y - pos.y;

              return new Move("throw", threat.pos.x + dx * 2, threat.pos.y + dy * 2);
            }
          }
          else {
            System.err.println(this + " needs ammunition");
            // TODO crouch down? find cover?
            activity = new AcquireSnowball();
          }
        }
      }

      // TODO we aren't facing threats, let's look for opportunities
      // 1. make a snowball if we haven't got one
      // 2. look for enemy snowmen to decapitate
      // 3. look for partially-built snowmen to finish
      // 4. if there are no threats or opportunities, continue to the runTarget
      if (activity == null || activity.isComplete) {
        if (holding != Const.HOLD_S1) {
          System.err.println(this + " is making a snowball");
          activity = new AcquireSnowball();
        }
        // no point in ducking if there's nobody around
        else if (!standing) {
          return new Move("stand");
        }
        else if (pos.x != runTarget.x && pos.y != runTarget.y) {
          System.err.println(this + " is moving towards " + runTarget);
          return moveToward(runTarget);
        }
        // make a snowman if nobody nearby has started one
        else {
          System.err.println(this + " has arrived at " + runTarget);
          Player nearbyBuilder = null;
          for (Player f : friends()) {
            if (f.activity instanceof BuildSnowman && euclidean(f.pos, pos) < 8) {
              nearbyBuilder = f;
            }
          }
          if (nearbyBuilder == null) {
            System.err.println(this + " is building a snowman");
            activity = new BuildSnowman();
          }
          else {
            // we have a snowball, we don't see any threats
            System.err.println(this + " has a snowball, doesn't see any threats, " + nearbyBuilder + " is building a snowman nearby");
            return new Move();
          }
        }
      }
      // Step 4. execute activity
      return activity.nextMove(this);
    }
  }

  // Simple representation for a child's action
  static class Move {
    String action;
    Point dest;

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
  }

  /*
   * An activity is a named list of moves carried out according to some policy (maybe ad-hoc)
   */
  abstract static class Activity {
    boolean isComplete = false;
    abstract public Move nextMove(Player c);
  }

  class TakePosition extends Activity {

    public Move nextMove(Player c) {
      if (c.pos.x == c.runTarget.x && c.pos.y == c.runTarget.y) {
        isComplete = true;
      }

      return c.chooseMove();
    }
  }

  class BuildSnowman extends Activity {

    Move[] instructions = {
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

    public Move nextMove(Player c) {

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
      if (c.state > 0 && !isComplete) {
        // Stamp out a move from our instruction template and return it.
        Move m = new Move(instructions[c.state].action);
        if (instructions[c.state].dest != null) {
          m.dest = new Point(c.pos.x + instructions[c.state].dest.x,
              c.pos.y + instructions[c.state].dest.y);
        }
        c.state = c.state + 1;
        isComplete = (c.state == instructions.length);
        return m;
      }

      assert(isComplete);
      System.err.println("BuildSnowman is complete, should not be idling");
      return new Move();
    }
  }

  class AcquireSnowball extends Activity {

    public Move nextMove(Player c) {

      Move m = new Move();

      // first look for a nearby snowball
      List<Point> nearby = c.nearbyItems(Const.GROUND_S);
      if (!nearby.isEmpty()) {
        System.err.println(c + " sees nearby snowballs at " +
            nearby.stream().map(ea -> ea.toString()).collect(Collectors.joining(", ")));
        Point nearest = nearby.get(0);
        int distance = euclidean(nearest, c.pos);
        if (distance <= 2) {
          System.err.println(c + " is picking up a snowball at " + nearby);
          m.action = "pickup";
          m.dest = nearest;
        }
        // we can get there in 2 turns or so
        else if (euclidean(nearest, c.pos) < 4) {
          m.action = c.standing ? "run" : "crawl";
          m.dest = new Point(Math.max(nearest.x, c.pos.x), Math.max(nearest.y, c.pos.y));
          System.err.println(c + " is moving to " + m.dest + " to pick up a snowball at " + nearby);
        }
      }
      // otherwise just make one
      else if (c.holding != Const.HOLD_S1) {
        // Crush into a snowball, if we have snow.
        if (c.holding == Const.HOLD_P1) {
          m.action = "crush";
          isComplete = true;
        }
        else {
          // We don't have snow, see if there is some nearby.
          int sx = -1, sy = -1;
          // Look in front of us first
          for (Point n : c.neighbors8()) {
            System.err.println(c + " is checking for snow at " + n);
            if (ground[n.x][n.y] == Const.GROUND_EMPTY && height[n.x][n.y] > 0) {
              sx = n.x;
              sy = n.y;
              break;
            }
          }

          // If there is snow, try to get it.
          if (sx >= 0) {
            if (c.standing) {
              m.action = "crouch";
            }
            else {
              System.err.println(c + " is picking up snow at [" + sx + ", " + sy + "]");
              m.action = "pickup";
              m.dest = new Point(sx, sy);
            }
          }
          // run or crawl if there's no snow nearby
          else {
            m.action = (c.standing) ? "run" : "crawl";
            // move one over
            // TODO could get stuck in a loop here (how?), might want to check for visited points
            //      write some tests to confirm
            Point dest = c.moveDestinations().get(0);
            System.err.println(c + " is moving to find snow at [" + dest.x + ", " + dest.y + "]");
            m.dest = dest;
          }
        }
      }
      return m;
    }
  }

  public boolean areAllPlayersIdle() {
    for (Player p : cList) {
      if (p.activity != null && !p.activity.isComplete) {
        return false;
      }
    }
    return true;
  }

  public void run() {

    for (int i = 0; i < cList.length; i++) {
      Player player = new Player();
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
      readCurrentMap(in);

//      if (visibleOpponents().isEmpty()) {
//        recordMapVisibilityWithPrefix("Map Visibility at turn " + turnNum);
//      }

      // Read the states of all the children.
      readChildrenStates(in);

      // Mark all the children in the map, so they are easy to
      // look up.
      markChildren();

      /*
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
       */

      // only reset visibility targets for snowmen if we don't know where all opponents are
      List<Player> threats = visibleOpponents();

      if (threats.isEmpty() && areAllPlayersIdle()) {
        System.err.println("No opponents visible - looking for optimal snowman positions");
        int iterations = 0;
        int previous_score = -1;
        int best_score = 0;
        List<Point> targets = Arrays.stream(cList)
            .map(c -> c.pos).collect(Collectors.toList());

        while (iterations < MAX_ITERATIONS && best_score > previous_score) {
          previous_score = best_score;
          best_score = iterateVisibilityTargets(targets, ground);
          iterations++;
        }

        for (int i = 0; i < Const.CCOUNT; i++) {
          Player player = cList[i];
          if (player.activity == null || player.activity.isComplete) {
            player.runTarget = targets.get(i);
          }
        }
      }
      else {
        String threatLocations = threats.stream().map(ea -> ea.pos.toString())
            .collect(Collectors.joining(", "));
        System.err.println("Enemies spotted at " + threatLocations);
      }

      // Decide what each child should do
      for (int i = 0; i < Const.CCOUNT; i++) {
        Move m = cList[i].chooseMove();
//        System.err.println(String.format("child %d move is %s", i, m.action));

        /* Write out the child's move */
        if (m.dest == null) {
          System.out.println(m.action);
        }
        else {
          System.out.println(m.action + " " + m.dest.x + " " + m.dest.y);
        }
      }

      turnNum = in.nextInt();
    }
  }

  int[][] copyBoard(int[][] board) {
    return Arrays.stream(board).map(int[]::clone).toArray(int[][]::new);
  }

  public int visibleCount(int[][] board) {
    int count = 0;
    for (int i = 0; i < Const.SIZE; i++) {
      for (int j = 0; j < Const.SIZE; j++) {
        if (board[i][j] >= 0) {
          count++;
        }
      }
    }
    return count;
  }

  /**
   * Create a pro-forma visibility map, ignoring current player locations
   * and assuming that the target indexes provided contain new snowmen.
   *
   * @param board ground visibility map
   * @param targets array
   */
  public void markVisibles(int[][] board, List<Point> targets) {
    for (int i = 0; i < Const.SIZE; i++) {
      for (int j = 0; j < Const.SIZE; j++) {
        board[i][j] = -1;
      }
    }
    for (Point t : targets) {
      for (int i = 0; i < Const.SIZE; i++) {
        for (int j = 0; j < Const.SIZE; j++) {
          if ((t.x - j) * (t.x - j) + (t.y - i) * (t.y - i) < 64) {
            board[i][j] = Const.GROUND_EMPTY;
          }
        }
      }
    }
  }

  /**
   * Conduct an iteration of a random walk of the positions on the board, starting
   * with the players current locations. If the new target locations provide improved
   * visibility, return the improved score.
   *
   * The locations of the targets will be updated in-place
   *
   * @return the visibility count of the modified target list
   */
  public int iterateVisibilityTargets(List<Point> targets, int[][] board) {

    markVisibles(board, targets);
    int best_score = visibleCount(board);

    for (int i = 0; i < targets.size(); i++) {
      Point target = targets.get(i);
      Point best_target = target;

      for (Point neighbor : neighbors(target)) {
        targets.set(i, neighbor);
        markVisibles(board, targets);
        int new_count = visibleCount(board);

        // higher score is better
        if (new_count > best_score) {
          // lock in any improvement, ignore subsequent neighbors
          targets.set(i, neighbor);
          markVisibles(board, targets);
          best_score = new_count;
          best_target = neighbor;
        }
        else {
          // revert changes
          targets.set(i, target);
        }
      }
      targets.set(i, best_target);
    }
    return best_score;
  }

  public List<Player> visibleOpponents() {
    List<Player> visible = new ArrayList<>();
    // last 4 children in the array represent opponents
    for (int i = Const.CCOUNT; i < Const.CCOUNT * 2; i++) {
      if (cList[i].pos.x > 0 && cList[i].pos.y > 0) {
        visible.add(cList[i]);
      }
    }
    return visible;
  }

  public String printVisibility(int[][] board) {
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < Const.SIZE; i++) {
      for (int j = 0; j < Const.SIZE; j++) {
        // translate ground map back to original format
        if (j > 0) b.append(' ');
        if (board[i][j] < 0) {
          b.append("*");
        }
        else {
          String code = String.format("%d%s", height[i][j], (char)(board[i][j] + 'a'));
          b.append(code);
        }
      }
      b.append('\n');
    }
    return b.toString();
  }

  public void recordMapVisibilityWithPrefix(String prefixMessage) {
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
        Player c = cList[i];
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
    Seeker seeker = new Seeker();
    seeker.run();
  }

  void markChildren() {
    for (int i = 0; i < Const.CCOUNT * 2; i++) {
      Player c = cList[i];
      if (c.pos.x >= 0) {
        ground[c.pos.x][c.pos.y] = GROUND_CHILD;
      }
    }
  }

  void readCurrentMap(Scanner in) {
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

  void readChildrenStates(Scanner in) {
    String token;
    for (int i = 0; i < Const.CCOUNT * 2; i++) {
      Player c = cList[i];

      // Can we see this child?
      token = in.next();
      if (token.equals("*")) {
        c.pos.x = -1;
        c.pos.y = -1;
      }
      else {
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
