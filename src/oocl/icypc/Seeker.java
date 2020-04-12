/*
 * A player based on Sturgill, Baylor University example code
 *
 * Implements some of the strategies described at
 * https://observablehq.com/@mandeluna/icpc-strategy-notes
 *
 * Steven Wart, OOCL (USA), 2020
 */

package oocl.icypc;

import icypc.Const;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Seeker {
  // Constant used to mark child locations in the map.
  public static final int GROUND_CHILD = 10;

  /**
   * Current game score for self (red) and opponent (blue).
   */
  private int[] score = new int[2];

  /**
   * Current snow height in each cell.
   */
  private int[][] height = new int[Const.SIZE][Const.SIZE];

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

  /**
   * Mostly a 2d point class but sometimes we need to know the height
   */
  public static class Point implements Cloneable {
    public int x;
    public int y;

    public Point() {}

    public Point(int x, int y) {
      this.x = x;
      this.y = y;
    }

    public Point clone() {
      try {
        return(Point)super.clone();
      }
      catch (CloneNotSupportedException e) {
        // cannot happen (thanks Java!)
        throw new AssertionError();
      }
    }

    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null) {
        return false;
      }
      if (o.getClass() != Point.class) {
        return false;
      }
      Point p = (Point)o;
      return (this.x == p.x && this.y == p.y);
    }

    public int hashCode() {
      return Integer.hashCode(x) << 16 + Integer.hashCode(y);
    }

    public Point plus(Point p) {
      return new Point(p.x + this.x, p.y + this.y);
    }

    public String toString() {
      return String.format("[%d, %d]", x, y);
    }
  }

  static Point[] permutations = {

      new Point(0, 1),    // north
      new Point(1, 0),    // east
      new Point(0, -1),  // south
      new Point(-1, 0),   // west

      new Point(1, 1),    // northeast
      new Point(1, -1),   // southwest
      new Point(-1, -1),  // southeast
      new Point(-1, 1),   // northwest
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

  /*
   * Return the value of x, clamped to the [ a, b ] range.
   */
  public static int clamp(int x, int a, int b) {
    if (x < a) {
      return a;
    }
    return Math.min(x, b);
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
//      int ht = round(p0.h - t * p0.h / n);

      points.add(new Point(xt, yt));
    }
    return points;
  }

  /**
   * Sort nearby things by distance - good for finding useful items
   *
   * @param cover ground constant to match
   * @return a list of items on the ground matching cover
   */
  public List<Point> itemsMatching(int cover) {
    List<Point> nearby = new ArrayList<>();
    for (int i = 0; i < Const.SIZE; i++) {
      for (int j = 0; j < Const.SIZE; j++) {
        if (ground[i][j] == cover) {
          nearby.add(new Point(i, j));
        }
      }
    }
    return nearby;
  }

  /**
   * Order the results clockwise from the from assuming the receiver is facing away from the origin
   *
   * @return a list of neighboring cells in 4 cardinal directions (N, E, S, W)
   */
  public List<Point> neighbors4(Point origin) {
    return Arrays.stream(Arrays.copyOfRange(permutations, 0, 4))
        .map(origin::plus)
        .filter(pt -> pt.x >= 0 && pt.x < Const.SIZE && pt.y >= 0 && pt.y < Const.SIZE)
        .collect(Collectors.toList());
  }

  /**
   * Order the results clockwise from the from assuming the receiver is facing away from the origin
   *
   * @return a list of neighboring cells in 8 directions (N, NE, E, SE, S, SW, W, NW)
   */
  public List<Point> neighbors8(Point origin) {
    return Arrays.stream(Arrays.copyOfRange(permutations, 0, 8))
        .map(origin::plus)
        .filter(pt -> pt.x >= 0 && pt.x < Const.SIZE && pt.y >= 0 && pt.y < Const.SIZE)
        .collect(Collectors.toList());
  }

  public List<Point> visibleNeighbors(Point origin, boolean standing, boolean avoidPlayers) {

    List<Point> neighbors = standing ? neighbors4(origin) : neighbors8(origin);

    // awkward logic for running positions
    Point[] run_cardinals = {
        new Point(0, 2),    // north
        new Point(2, 0),    // east
        new Point(0, -2),   // south
        new Point(-2, 0),   // west
    };

    if (standing) {
      for (int i = 0; i < 4; i++) { // N, E, S, W are cardinal directions
        Point r = origin.plus(run_cardinals[i]);
        Point n = origin.plus(permutations[i]);

        if (r.x >= 0 && r.x < Const.SIZE &&
            r.y >= 0 && r.y < Const.SIZE &&
            (ground[n.x][n.y] == Const.GROUND_EMPTY ||
                (avoidPlayers && ground[n.x][n.y] == GROUND_CHILD))) {

          neighbors.add(r);
        }
      }
      // prefer the far distances to the near ones
      // but keep the close ones just in case the far ones lead to dead-ends
      Collections.reverse(neighbors);
    }

    return neighbors.stream()
        .filter(pt -> ground[pt.x][pt.y] == Const.GROUND_EMPTY ||
            (avoidPlayers && ground[pt.x][pt.y] == GROUND_CHILD))
        .collect(Collectors.toList());
  }

  /**
   * Simple BFS to get us to the destination
   * It's a small map, no need to get fancy
   *
   * Complicated by the fact that a cell might be invisible!
   * This method will treat invisible locations as inaccessible
   * If the target is invisible, or unreachable because parts of the path
   * are invisible, try finding a path to the furthest visible point instead.
   *
   * See this article for a wonderful introduction
   * https://www.redblobgames.com/pathfinding/a-star/introduction.html
   *
   * @param start starting point on the map
   * @param target destination point on the map
   * @param standing if true, look at 8 neighbors, otherwise 4
   * @return a list of points representing the shortest path
   */
  List<Point> freePath(Point start, Point target, boolean standing) {

    // let the caller deal with this bad decision
    if (ground[target.x][target.y] != Const.GROUND_EMPTY) {
      return Collections.emptyList();
    }

    Queue<Point> frontier = new LinkedList<>();
    frontier.add(start);
    Map<Point, Point> cameFrom = new HashMap<>();
    cameFrom.put(start, null);

    while (!frontier.isEmpty()) {
      Point current = frontier.remove();

      // early exit (we are looking only for one destination)
      if (current.equals(target)) {
        break;
      }

      List<Point> neighbors = visibleNeighbors(current, standing, true);

      for (Point next : neighbors) {
        if (!cameFrom.containsKey(next)) {
          frontier.add(next);
          cameFrom.put(next, current);
        }
      }
    }

//    log("found path from %s to %s", start, target);

    // found the path, now unwrap it
    Point current = target;
    List<Point> path = new ArrayList<>();
    while (!current.equals(start)) {
      path.add(current);
      current = cameFrom.get(current);
      if (current == null) {
        // this may happen if the destination is unreachable
        return Collections.emptyList();
      }
    }
    path.add(start);
    Collections.reverse(path);

    String pathString = path.stream().map(Point::toString)
        .collect(Collectors.joining("-"));
    log(" => path from %s to %s is %s", start, target, pathString);

    return path;
  }

  public class Player {

    public Point pos = new Point();
    boolean standing;
    int color;
    int holding;
    int dazed;
    // avoid returning to the same destinations repeatedly
    List<Point> visited = new ArrayList<>();

    public String toString() {
      int id = Arrays.asList(cList).indexOf(this);
      return String.format("Player %d {pos:%s, dest:%s, hold:%d, stand:%s}",
          id, pos, runTarget, holding, standing ? "s" : "c");
    }

    Point runTarget = new Point(-1, -1);

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
      List<Point> nearby = itemsMatching(cover);
      nearby.sort(Comparator.comparingInt(a -> euclidean(pos, a)));
      return nearby;
    }

    /**
     * Find the cells neighboring the receiver's position
     *
     * @return a list of neighboring cells
     */
    public List<Point> neighbors() {
      return (standing) ? neighbors8(this.pos) : neighbors4(this.pos);
    }

    /**
     * Just because you can't see someone, doesn't mean they can't see you.
     * This method finds whom the receiver can target (or vice-versa)
     *
     * TODO this method needs some test coverage
     *
     * @return a collection of visible opponents, sorted by distance
     */
    public List<Player> visibleOpponents() {
      List<Player> visible = new ArrayList<>();
      int max_range = 9;
      List<Point> friendly = friends().stream().map(p -> p.pos).collect(Collectors.toList());

      // last 4 children in the array represent opponents
      for (int i = Const.CCOUNT; i < Const.CCOUNT * 2; i++) {
        if (cList[i].pos.x > 0 && cList[i].pos.y > 0) {

          if (euclidean(cList[i].pos, this.pos) > max_range) {
            continue;
          }

          // filter out opponents who are obstructed
          List<Point> obstacles = interpolate(pos, cList[i].pos).stream()
              .filter(p -> (!friendly.contains(p) &&
                  ground[p.x][p.y] != Const.GROUND_EMPTY &&
                  ground[p.x][p.y] != GROUND_CHILD))
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
        int bx = b.pos.x - pos.x;
        int by = b.pos.y - pos.y;

        return (ax * ax + ay * ay) - (bx * bx + by * by);
      });

      return visible;
    }

    /**
     * Sometimes we are looking for something which is far way from others points
     *
     * @param p a point of interest
     * @param others a collection of points to measure
     * @return the distance of the furthest point from p in others
     */
    int furthestDistance(Point p, List<Point> others) {
      int furthest = 0;
      for (Point o : others) {
        int dist = euclidean(p, o);
        if (dist > furthest) {
          furthest = dist;
        }
      }
      return furthest;
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

    // return the center of the 8x8 region of squares that is
    // the furthest from any snowman
    public List<Point> centers(int granularity) {
      List<Point> snowmen = nearbyItems(Const.GROUND_SMR);

      List<Point> centers = new ArrayList<>();
      for (int i = granularity / 2; i < Const.SIZE; i += granularity) {
        for (int j = granularity / 2; j < Const.SIZE; j += granularity) {
          centers.add(new Point(i, j));
        }
      }

      centers.sort(Comparator.comparingInt(pt -> furthestDistance(pt, snowmen)));

      log("candidate destinations: " + centers.stream().map(pt -> String.format("%s -> %s", pt, furthestDistance(pt, snowmen)))
          .collect(Collectors.joining(", ")));

      Collections.reverse(centers);

      return centers;
    }

    /**
     * Call this after some snowmen have been built and we are mostly dealing
     * with snowball attacks from our opponents...
     *
     * heuristics:
     *   forbidden - stay away from our snowmen (do not draw fire)
     *   stay close to our friends
     *   stay closer to our enemies
     *   be attracted to invisible parts of the map
     *   don't go too far
     *
     *               - OR -
     *
     *  Call this after we've built a snowman and there is an opportunity to
     *  build more snowmen elsewhere...
     *
     *  heuristics:
     *    forbidden - try to avoid other players
     *    forbidden - always stay away from our completed snowmen
     *    stay close to our friends - they can provide cover while we build
     *    be attracted to invisible parts of the map
     *    go from [7, 7] to [7, 23] or [23, 7] or [15, 15] but maybe not [23, 23] (although sure why not)
     *    - let's set a range of between 8-15 units in each dimension
     */
    // the current destination is bad - don't use it
    public void reposition() {
      List<Point> snowmen = nearbyItems(Const.GROUND_SMR);
      Set<Point> forbidden = new HashSet<>(visited);

      forbidden.addAll(itemsMatching(Const.GROUND_TREE));

      // 1. Create a disjoint set of all the unexplored parts of the map
      // 2. Move to the centroid of the largest unexplored area
      // 3. Ensure multiple players aren't moving to exactly the same place,
      //    by adding 8 neighbors of other player's locations and runTargets
      //    to forbidden set

      for (Point p : snowmen) {
        forbidden.addAll(neighbors8(p));
      }
      for (Player p : players()) {
        forbidden.addAll(neighbors8(p.runTarget));
      }

      // don't want everyone going to the same place
      // centers() will always return 4 interesting spots
      int granularity = snowmen.isEmpty() ? 16 : 8;
      int index = Arrays.asList(cList).indexOf(this);
      Point dest = centers(granularity).get(index);

      // don't send everyone to the same point
      Iterator<Point> n = neighbors8(dest).iterator();
      while (forbidden.contains(dest) && n.hasNext()) {
        Point neighbor = n.next();
        dest = new Point(clamp(0, Const.SIZE - 1, neighbor.x + 8),
            clamp(0, Const.SIZE - 1, neighbor.y + 8));
      }

      log("%s is repositioning to %s", this, dest);
      this.runTarget = bestDestinationCloseTo(dest, pos, false);
    }


    /**
     * Return a move to get this child closer to target.
     */
    Move moveToward(Point target) {
      Point dest = target;

      if (target.equals(pos)) {
        visited.add(target);
        this.reposition();
        log("%s => moveToward(%s): invalid move (already there), repositioning", this, target);
        dest = runTarget;
      }

      log(this + " is moving toward " + dest);
      if (ground[dest.x][dest.y] < 0) {
        // temporary destination
        dest = furthestVisiblePointBetween(pos, target);
        log(" => %s is invisible, temporary dest is %s", target, dest);
      }
      // don't walk into trees
      if (ground[dest.x][dest.y] == Const.GROUND_TREE) {
        log(" => %s is occupied by a tree (%d)", dest, ground[dest.x][dest.y]);
        Iterator<Point> n_iter = visibleNeighbors(dest, standing, false).iterator();

        while (ground[dest.x][dest.y] == Const.GROUND_TREE && n_iter.hasNext()) {
          log(" => %s is occupied by a tree (%d)", dest, ground[dest.x][dest.y]);
          dest = n_iter.next();
        }
      }

      List<Point> path = freePath(pos, dest, standing);

      List<Point> interpolation = interpolate(pos, dest);
      int lastIndex = interpolation.size() - 1;

      while (path.size() < 2 && lastIndex >= 1) {
        log(" => %s is unreachable, temporary dest is %s", target, dest);
        dest = interpolation.get(lastIndex);
        path = freePath(pos, dest, standing);

        if (lastIndex == 1) {
          log("%s is unable to move to %s (furthest visible is %s), idling", this, target, dest);
          // Nowhere to move, give up on this destination
          visited.add(runTarget);
          this.reposition();
          return new Move();
        }
        lastIndex--;
      }
      if (path.size() < 2) {
        log(this + " => has no where to go, let's just idle and see if things improve");
        return new Move();
      }
      dest = path.get(1);

      if (ground[dest.x][dest.y] > 0) {
        log(" => %s is occupied (%d)", dest, ground[dest.x][dest.y]);
        return new Move(); // just idle
      }
      // the first element of the path is the starting point
      log(this + " is moving toward " + dest);
      return new Move(standing ? "run" : "crawl", dest.x, dest.y);
    }

    boolean retreat = false;
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
        if (!retreat) {
          // when we wake up, stop what we are doing and arm ourselves
          activity.isComplete = true;

          // can't target enemy, move to the nearest obstacle
          List<Point> obstacles = nearbyItems(Const.GROUND_TREE);
          // don't hide behind our own snowmen
          obstacles.addAll(nearbyItems(Const.GROUND_SMR));
          obstacles.sort(Comparator.comparingInt(a -> euclidean(pos, a)));

          log(this + " is dazed, retreating...");
          if (!obstacles.isEmpty() && euclidean(obstacles.get(0), pos) < 8) {
            Point obstacle = obstacles.get(0);
            log(" => changing runTarget to obstacle (%d) at %s", ground[obstacle.x][obstacle.y], obstacle);
            runTarget = bestDestinationCloseTo(obstacle, pos, false);
          }
          else { // into the fray!
            List<Player> threats = visibleOpponents();
            if (threats.size() > 0) {
              Point t = threats.get(0).pos;
              log(" => changing runTarget to ", t);
              runTarget = bestDestinationCloseTo(t, pos, false);
            }
            // just back up
            else {
              Point reverse = new Point(pos.x - runTarget.x, pos.y - runTarget.y);
              // reverse our direction and reassess our priorities
              Point p = new Point(pos.x + (int) Math.signum(reverse.x) * 4,
                  pos.y + (int) Math.signum(reverse.y) * 4);
              log(" => changing runTarget to ", p);
              runTarget = p;
            }
          }
          // only do this once
          retreat = true;
        }
        // unable to act for now
        return new Move();
      }
      retreat = false;

      // which opponents are potential targets/threats?
      // NB: if we can't see them, it doesn't mean they can't see us
      List<Player> threats = visibleOpponents();
      if (!threats.isEmpty()) {
        String threatString = threats.stream().map(p -> p.pos.toString())
            .collect(Collectors.joining(", "));
        log(this + " sees enemies at " + threatString);
      }
      for (Player threat: threats) {

        // clear and present danger
        if (holding == Const.HOLD_S1) {

          // Stand up if the child is armed.
          if (!standing) {
            // TODO only stand if the target is too far away to hit from a crouching position
            return new Move("stand");
          }
          else {
            // excellent, we are prepared
            int dist = euclidean(this.pos, threat.pos);
            double angle = Math.atan2(threat.pos.y - pos.y, threat.pos.x - pos.y);

            // TODO this is Hunter.java logic - why the 8 square radius?
            // if pos = [15, 0] and target = [14, 15]
            // then dx = -1, dy = 15, dx*dx + dy*dy = 226
            // if we don't make this check, we will target [13, 30], which is aimless
            int dx = threat.pos.x - pos.x;
            int dy = threat.pos.y - pos.y;

            // TODO sometimes we hit our own players

            // overthrow target at least 2 units beyond the target (can be off board)
            // incrementally adjust until we know that the target cell is included in the
            // linear interpolation path of the throw
            log("%s targets: %s at %d units away", this, threat, dist);
//            pos.h = standing ? 9 : 6;

            if (dx * dx + dy * dy < 16 * 16) {
              Point p1 = new Point((int) (pos.x + (dist * 1.25) * Math.cos(angle)),
                  (int) (pos.y + (dist * 1.25) * Math.sin(angle)));

              List<Point> path = interpolate(pos, p1);
              String pathString = path.stream().map(Point::toString)
                  .collect(Collectors.joining("-"));
              log("  -> interpolate %s to %s is %s", pos, p1, pathString);

              if (path.stream().anyMatch(el -> el.x == threat.pos.x && el.y == threat.pos.y)) {
                return new Move("throw", p1.x, p1.y);
              }
            }

            if (dx * dx + dy * dy < 8 * 8) {
              // that didn't work, try Hunter heuristic
              Point p1 = new Point(pos.x + dx * 2, pos.y + dy * 2);

              List<Point> path = interpolate(pos, p1);
              String pathString = path.stream().map(Point::toString)
                  .collect(Collectors.joining("-"));
              log("  -> interpolate %s to %s is %s", pos, p1, pathString);

              log(this + " *** wild? throw at " + p1);
              return new Move("throw", p1.x, p1.y);
            }
            return moveToward(runTarget);
          }
        }
        else {
          log(this + " needs ammunition");
          // TODO crouch down? find cover?
          activity = new AcquireSnowball();
          // we are being targeted, can we catch instead of getting hit?
          if (threat.holding == Const.HOLD_S1 &&
              euclidean(threat.pos, pos) <= 8 &&
              holding == Const.HOLD_EMPTY) {
            return new Move("catch", threat.pos.x, threat.pos.y);
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
          log(this + " is looking for a snowball");
          activity = new AcquireSnowball();
        }
        // no point in ducking if there's nobody around
        else if (!standing) {
          return new Move("stand");
        }
        else if (!pos.equals(runTarget)) {
          log(this + " is moving towards " + runTarget);
          return moveToward(runTarget);
        }
        else {
          List<Point> nearbySnowmen = nearbyItems(Const.GROUND_SMR);

          if (nearbySnowmen.isEmpty()) {
            log(this + " is starting a new snowman (first!)");
            activity = new Build();
          }
          else {
            String snowmenString = nearbySnowmen.stream().map(Point::toString)
                .collect(Collectors.joining(", "));

            log(this + " found nearby snowmen: " + snowmenString);
            int dist = euclidean(pos, nearbySnowmen.get(0));
            // build a snowman is there's not one nearby
            if (dist > 7) {
              log(this + " is starting a new snowman");
              activity = new Build();
            } else {
              log(this + " nearby snowmen are too close: " + snowmenString);
              // find somewhere new
              this.reposition();
              return moveToward(runTarget);
            }
          }
        }
      }
      // Step 4. execute activity
      if (activity.isComplete) {
        log("Should not happen to player " + this + ", activity: " + this.activity);
        return null;  // just throw an NPE - an activity should have been selected
      }
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
    abstract public void cancelMove();
  }

  class Build extends Activity {

    /**
     * Current instruction this activity is executing.
     */
    int state = 0;

    // we need 2 nearby sources of snow with height >= 3, and an unblocked dest
    Point source1, source2, dest, drop;
    int idleCount = 0;

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

    public String toString() {
      return (String.format("Build {state:%d, complete:%b}", state, isComplete));
    }

    // last move failed, reset the index
    public void cancelMove() {
      if (state > 0) state--;
      if (isComplete) isComplete = false;
    }

    public Move nextMove(Player c) {

      if (state == 0) {
        // Not building a snowman.

        // If we didn't get to finish the last snowman, maybe we're holding something.
        // We should drop it.
        if (c.holding != Const.HOLD_EMPTY &&
            c.pos.y < Const.SIZE - 1 &&
            height[c.pos.x][c.pos.y + 1] <= Const.MAX_PILE - 3) {
          drop = bestDestinationCloseTo(c.pos, c.pos, true);
          log("%s dropping at %s", c, drop);
          return new Move("drop", drop.x, drop.y);
        }

        // See if we should start running our build script.
        // Are we far from other things, is the ground empty
        // and do we have enough snow to build a snowman.
        // Make sure we have at least 4 empty squares around us
        int squareCount = 0;
        String neighborString = neighbors8(c.pos).stream().map(Point::toString)
            .collect(Collectors.joining(", "));
        log(" => %s neighbors are %s", c, neighborString);

        for (Point p : neighbors8(c.pos)) {
          if (ground[p.x][p.y] == Const.GROUND_EMPTY) {
            squareCount++;
          }
          else continue;

          if (p.equals(drop)) {
            continue;
          }
          if (source1 == null) {
            log(" => %s setting source1 to %s", c, p);
            source1 = p;
          }
          else if (source2 == null) {
            log(" => %s setting source2 to %s", c, p);
            source2 = p;
          }
          else if (dest == null) {
            log(" => %s setting dest to %s", c, p);
            dest = p;
          }
        }

        if (source1 != null && source2 != null && dest != null &&
            c.holding == Const.HOLD_EMPTY && squareCount >= 4) {
          // Start trying to build a snowman.
          log(" => %s building a snowman at %s, taking snow from %s and %s", c, dest, source1, source2);
          state = 1;
        }
        else {
          // if it's a bad spot, abort the operation
          c.reposition();
          isComplete = true;
        }
      }

      // Are we building a snowman?
      if (state > 0 && !isComplete) {
        // Stamp out a move from our instruction template and return it.
        Move m = new Move(instructions[state].action);
        if (instructions[state].dest != null) {
          m.dest = instructions[state].dest.y > 0 ? source1 : source2;
          if (ground[m.dest.x][m.dest.y] == GROUND_CHILD) {
            if (idleCount++ > 4) {
              // give up on waiting
              isComplete = true;
              c.reposition();
            }
            // if someone has stepped on our pickup or build destination, wait before continuing
            return new Move();
          }
        }
        state = state + 1;
        this.isComplete = (state == instructions.length);
        log("%s activity step %d: move is %s", c, state, m.action);
        return m;
      }

      assert(this.isComplete);
      log(c.activity + " is complete, should not be idling");
      return new Move();
    }
  }

  class AcquireSnowball extends Activity {

    // no need to do anything, acquisition is stateless
    public void cancelMove() {}

    public Move nextMove(Player c) {
      Move m = new Move();

      // look for a nearby snowball
      List<Point> nearby = c.nearbyItems(Const.GROUND_S);
      nearby.addAll(c.nearbyItems(Const.GROUND_SMB)); // pick up a snowball from enemy snowman?

      // If we already had a snowball why is this task being invoked?
      if (c.holding == Const.HOLD_S1 || c.holding == Const.HOLD_S2 || c.holding == Const.HOLD_S3) {
        log(c + " already has a snowball, should not have AcquireSnowball activity");
        isComplete = true;
        m.action = "idle";
      }
      // Crush into a snowball, if we have snow.
      else if (c.holding == Const.HOLD_P1) {
        m.action = "crush";
        isComplete = true;
      }
      // TODO the next three if statements are basically builder logic
      //      when a build is canceled it should drop everything first
      // holding something that won't help build a snowball, put it down
      else if (c.holding == Const.HOLD_L) {
        m.action = "drop";
        Optional<Point> dest = neighbors8(c.pos).stream()
            .filter(p -> ground[p.x][p.y] == Const.GROUND_EMPTY).findAny();
        if (dest.isPresent()) {
          m.dest = dest.get();
          log("%s is dropping a large snowball at %s", c, m.dest);
        }
      }
      // holding something that won't help build a snowball, put it down
      else if (c.holding == Const.HOLD_M) {
        m.action = "drop";
        // first choice is to drop on a large snowball if there is one
        Optional<Point> dest = neighbors8(c.pos).stream()
            .filter(p -> ground[p.x][p.y] == Const.GROUND_L).findAny();
        if (dest.isPresent()) {
          m.dest = dest.get();
          log("%s is dropping a medium snowball onto a large snowball at %s", c, m.dest);
        }
        else {
          m.dest = neighbors8(c.pos).get(0);
          log("%s is dropping a medium snowball at %s", c, m.dest);
        }
      }
      // holding something that won't help build a snowball, put it down anywhere
      else if (c.holding > 0) {
        m.action = "drop";
        Optional<Point> dest = neighbors8(c.pos).stream()
            .filter(p -> ground[p.x][p.y] == Const.GROUND_EMPTY).findAny();
        if (dest.isPresent()) {
          m.dest = dest.get();
          log("%s is dropping %d at %s", c, c.holding, m.dest);
        }
        else {
          m.dest = neighbors8(c.pos).get(0);
          log("%s is dropping %d at %s", c, c.holding, m.dest);
        }
      }
      // we can get there in 2 turns or so it's cheaper than building a new one
      else if (!nearby.isEmpty()) {
        Point nearest = nearby.get(0);
        log(c + " sees a nearby snowball at " + nearest);
        if (neighbors8(c.pos).contains(nearest)) {
          if (c.standing) {
            log(c + " is crouching");
            m.action = "crouch";
          }
          else {
            log(c + " is picking up a snowball at " + nearest);
            m.action = "pickup";
            m.dest = nearest;
            isComplete = true;
          }
        }
        else if (euclidean(nearest, c.pos) < 8) {
          if (!c.standing) {
            log(c + " is standing");
            m.action = "stand";
          }
          else {
            m.action = "run";
            // don't step on the snowball
            Point sb = new Point(Math.max(nearest.x, c.pos.x), Math.max(nearest.y, c.pos.y));
            m.dest = bestDestinationCloseTo(sb, c.pos, true);
            log("%s %s to %s to pick up a snowball at %s", c, m.action, m.dest, nearby);
          }
        }
      }
      // otherwise just make one
      else {
        // We don't have snow, see if there is some nearby.
        int sx = -1, sy = -1;
        // Look in front of us first
        for (Point n : neighbors8(c.pos)) {
//            log(c + " is checking for snow at " + n);
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
            log(c + " is picking up snow at [" + sx + ", " + sy + "]");
            m.action = "pickup";
            m.dest = new Point(sx, sy);
            isComplete = true;
          }
        }
        // run or crawl if there's no snow nearby
        else {
          m.action = (c.standing) ? "run" : "crawl";
          // move one over
          // TODO could get stuck in a loop here (how?), might want to check for visited points
          //      write some tests to confirm
          Point dest = c.moveDestinations().get(0);
          log("%s %s to find snow at %s", c, m.action, dest);
          m.dest = dest;
        }
      }
      return m;
    }
  }

  public List<Player> players() {
    return Arrays.asList(Arrays.copyOfRange(cList, 0, Const.CCOUNT));
  }

  boolean initialized = false;

  public void initializePlayerPositions() {
    if (!initialized) {
      // get to strategic board locations quickly, build snowmen first, then engage opponents
      // these spots may be occupied or difficult to reach, rely on path-finding to get close
      cList[0].runTarget = new Point(7, 22);
      cList[1].runTarget = new Point(7, 7);    // easiest place to build a snowman
      cList[2].runTarget = new Point(15, 15);  // likely encounters mid-board
      cList[3].runTarget = new Point(23, 7);
      initialized = true;
    }
  }

  /**
   * Find suitable target cells close to the origin provided
   *
   * @param o the center, also included if unblocked
   * @param n the number of cells on either side to consider
   * @return a list of unblocked cells within the radius
   */
  List<Point> unblockedCellsInRange(Point o, int n) {
    List<Point> cells = new ArrayList<>();
    for (int i = o.x - n; i < o.x + n; i++) {
      for (int j = o.y - n; j < o.y + n; j++) {
        if (i >= 0 && i < Const.SIZE && j >= 0 && j < Const.SIZE) {
          if (ground[i][j] == Const.GROUND_EMPTY) {
            cells.add(new Point(i, j));
          }
        }
      }
    }
    return cells;
  }

  /**
   * Find a visible point (may be blocked) suitable as a runTarget
   *
   * @param c ideal destination (may or may not be visible)
   * @param s starting point (should be visible)
   * @return the furthest point from s that is not invisible
   */
  public Point furthestVisiblePointBetween(Point s, Point c) {
    // naive approach is to search from s to c, finding the first visible square
    // - BUT -
    // if path is partially obstructed, this will lead to a confusing outcome
    // better to follow c to s until we hit the first invisible
    List<Point> path = interpolate(s, c);
    Point furthest = s;
    for (Point p : path) {
      if (ground[p.x][p.y] >= 0) {
        furthest = p;
      }
    }
    log(" => furthest visible point between %s and %s is %s", s, c, furthest);
    return furthest;
  }

  /**
   * Find a destination to set as a player's runTarget
   *
   * Assumes the destination is visible. Fools don't rush in.
   *
   * @param c ideal destination - but it may not be available
   * @param s starting point - needed to help optimize choice
   * @return the closest unblocked point near c
   */
  public Point xxxbestDestinationCloseTo(Point c, Point s) {
    int range = 1;

    List<Point> nearestUnblocked = new ArrayList<>();
    while (range < 10 && nearestUnblocked.isEmpty()) {
      nearestUnblocked = unblockedCellsInRange(c, range++);
    }
    if (nearestUnblocked.isEmpty()) {
      log(this + " can't find a clear destination near " + c);
      return c;
    }
    nearestUnblocked.sort(Comparator.comparingInt(a -> euclidean(s, a)));
    Point dest = nearestUnblocked.get(0);
    log("best destination near %s is %s (contains %d)", c, dest, ground[dest.x][dest.y]);
    return dest;
  }

  /**
   * Find the point closest to end that is visible
   * with the shortest path from start.
   *
   * @param end
   * @param start
   * @return
   */
  public Point bestDestinationCloseTo(Point end, Point start, boolean avoidPlayers) {

    if (ground[end.x][end.y] == Const.GROUND_EMPTY) {
      return end;
    }

    Queue<Point> frontier = new LinkedList<>();
    frontier.add(start);
    Map<Point, Point> cameFrom = new HashMap<>();
    cameFrom.put(start, null);

    // "flood fill" algorithm -- all points reachable from start
    while (!frontier.isEmpty()) {
      Point current = frontier.remove();

      List<Point> neighbors = visibleNeighbors(current, true, avoidPlayers);

      for (Point next : neighbors) {
        if (!cameFrom.containsKey(next)) {
          frontier.add(next);
          cameFrom.put(next, current);
        }
      }
    }

    int closest = Const.SIZE * 2;
    Point best = null;
    Set<Point> fill = cameFrom.keySet();
    fill.remove(start);
    for (Point p : fill) {
      int dist = euclidean(p, end);
      if (dist < closest) {
        best = p;
        closest = dist;
      }
    }

//    String allVisible = cameFrom.keySet().stream()
//        .map(Point::toString).collect(Collectors.joining(", "));

    log("  => Best choice is " + best);
    return best;
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

      log("---------------- Turn %d ---------------- ", turnNum);

      // Read current game score.
      score[Const.RED] = in.nextInt();
      score[Const.BLUE] = in.nextInt();

      // Parse the current map.
      readCurrentMap(in);

      // Read the states of all the children.
      readChildrenStates(in);

      // Mark all the children in the map, so they are easy to
      // look up.
      markChildren();

      // initialize run targets, if required
      initializePlayerPositions();

      if (verboseDebug) {
        System.err.println("Height Map");
        for (int i = 0; i < Const.SIZE; i++) {
          for (int j = 0; j < Const.SIZE; j++) {
            System.err.print(" " + height[i][j]);
          }
          System.err.println();
        }

        System.err.println("Ground Map");
        System.err.println(printVisibility(ground));
      }

      // check for conflicts
      Point[] dests = new Point[Const.CCOUNT];

      // Decide what each child should do
      for (int i = 0; i < Const.CCOUNT; i++) {

        Move m = cList[i].chooseMove();
        for (int j = 0; j <= i; j++) {
          if (dests[j] != null && dests[j].equals(m.dest)) {
            // this way at least one of our players gets to act (priority to lower player numbers)
            log("%s action conflicts with %s", cList[i], cList[j]);
            m.action = "idle";
            m.dest = null;
            cList[i].activity.cancelMove();
          }
        }
        dests[i] = m.dest;

        /* Write out the child's move */
        if (m.dest == null) {
//          log("%s action %d is %s", cList[i], i, m.action);
          System.out.println(m.action);
        }
        else {
//          log("%s action %d is %s %s", cList[i], i, m.action, m.dest);
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

  public void markVisibles(List<Point> targets) {
    markVisibles(ground, targets);
  }

  /**
   * Create a pro-forma visibility map, ignoring current player locations
   * and assuming that the target indexes provided contain new snowmen.

   * @param board ground visibility map
   * @param targets array
   */
  public void markVisibles(int[][] board, List<Point> targets) {

    List<Point> domain = new ArrayList<>(targets);

    for (int i = 0; i < Const.SIZE; i++) {
      for (int j = 0; j < Const.SIZE; j++) {
        // if we have a snowman on this square, include it in our domain
        if (board[i][j] == GROUND_CHILD ||
            board[i][j] == Const.GROUND_SMR) {
          domain.add(new Point(i, j));
        }
        // otherwise ignore it
        else {
          board[i][j] = -1;
        }
      }
    }

    for (Point t : domain) {
      for (int i = 0; i < Const.SIZE; i++) {
        for (int j = 0; j < Const.SIZE; j++) {
          int distSq = (t.x - j) * (t.x - j) + (t.y - i) * (t.y - i);
          Point current = new Point(i, j);
          // erase the original location of the targets because they are moving
          if (distSq < 64 && (board[i][j] < 0 || targets.contains(current))) {
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

      for (Point neighbor : neighbors8(target)) {
        int dx = neighbor.x - target.x;
        int dy = neighbor.y - target.y;

        Point next = new Point(target.x + 2 * dx, target.y + 2 * dy);

        if (next.x < 0 || next.x >= Const.SIZE || next.y < 0 || next.y >= Const.SIZE) {
          continue;
        }

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
          String code = String.format("%d%s", Math.max(height[i][j], 0), (char)(board[i][j] + 'a'));
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

  static boolean verboseDebug;
  static boolean debug;

  static void log(String format, Object ... args) {
    if (debug) {
      System.err.println(String.format(format, args));
    }
  }

  public static void main(String[] args) {
    Map<String, String> environment = System.getenv();

    // need to use environment variables because the command line is inaccessible
    verboseDebug = environment.containsKey("SEEKER_VERBOSE_DEBUG");
    debug = environment.containsKey("SEEKER_DEBUG");

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
        }
        else {
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

        // set initial runTargets
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
