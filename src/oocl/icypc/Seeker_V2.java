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
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

public class Seeker_V2 {
  // Constant used to mark child locations in the map.
  public static final int GROUND_CHILD = 10;

  private final Random rnd = new Random();

  /**
   * Current game score for self (red) and opponent (blue).
   */
  private final int[] score = new int[2];

  /**
   * Current snow height in each cell.
   */
  private final int[][] height = new int[Const.SIZE][Const.SIZE];

  /**
   * Contents of each cell.
   */
  private final int[][] ground = new int[Const.SIZE][Const.SIZE];

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
  private final Player[] cList = new Player[2 * Const.CCOUNT];

  public Seeker_V2() {
    for (int i = 0; i < 2 * Const.CCOUNT; i++) {
      cList[i] = new Player();
    }
    initializeZones();
  }

  /**
   * Minimum and maximum extents of each zone/sector on the playfield
   * Used for repositioning and player defense
   */
  static final public Point[] ZoneBoundaries = {
      new Point(0, 0), new Point(9, 9),     // Zone 0 (10x10)
      new Point(10, 0), new Point(20, 9),   // Zone 1 (11x10)
      new Point(21, 0), new Point(30, 9),   // Zone 2 (10x10)
      new Point(0, 10), new Point(9, 20),   // Zone 3 (10x11)
      new Point(10, 10), new Point(20, 20), // Zone 4 (11x11)
      new Point(21, 10), new Point(30, 20), // Zone 5 (10x11)
      new Point(0, 21), new Point(9, 30),   // Zone 6 (10x10)
      new Point(10, 21), new Point(20, 30), // Zone 7 (11x10)
      new Point(21, 21), new Point(30, 30), // Zone 8 (10x10)
  };

  static final public List<Zone> Zones = new ArrayList<>();

  /**
   * Find the zone containing the point provided
   *
   * @param pt the point to look up
   * @return the zone containing the point p
   */
  public static Zone zoneContaining(Point pt) {
    if (pt.x < 0 || pt.x >= Const.SIZE || pt.y < 0 || pt.y >= Const.SIZE) {
      throw new IndexOutOfBoundsException("Out of bounds " + pt);
    }
    int index = -1;
    for (int zb = 0; zb < ZoneBoundaries.length; zb += 2) {
      Point min = ZoneBoundaries[zb];
      Point max = ZoneBoundaries[zb + 1];
      if (pt.x >= min.x && pt.x <= max.x && pt.y >= min.y && pt.y <= max.y) {
        index = zb / 2;
        break;
      }
    }
    if (index < 0) {
      throw new IndexOutOfBoundsException(pt + " is not found in any zone");
    }
    return Zones.get(index);
  }

  private void initializeZones() {
    for (int zb = 0; zb < ZoneBoundaries.length; zb += 2) {
      Point min = ZoneBoundaries[zb];
      Point max = ZoneBoundaries[zb + 1];
      List<Point> points = new ArrayList<>();

      for (int i = min.x; i <= max.x; i++) {
        for (int j = min.y; j <= max.y; j++) {
          points.add(new Point(i, j));
        }
      }

      Zone zone = new Zone(points);
      zone.centroid = new Point((max.x + min.x + 1) / 2, (max.y + min.y + 1) / 2);

      Zones.add(zone);
    }
  }

  public class Zone {
    public List<Point> points;
    public Point centroid;

    public String toString() {
      int index = Zones.indexOf(this);
      int size = points.size();
      Point min = ZoneBoundaries[index * 2];
      Point max = ZoneBoundaries[index * 2 + 1];
      return String.format("Zone %d: %d points, extent:%s-%s, center:%s", index, size, min, max, centroid);
    }

    public int hiddenCount() {
      int count = 0;
      for (Point p : points) {
        if (ground[p.x][p.y] < 0) {
          count++;
        }
      }
      return count;
    }

    public Zone(List<Point> points) {
      this.points = points;
    }
  }

  /* --- Geometry --- */

  /**
   * 2d point class
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

  /**
   * dd point class
   */
  public static class Point3 implements Cloneable {
    public int x;
    public int y;
    public int h;

    public Point3(int x, int y, int h) {
      this.x = x;
      this.y = y;
      this.h = h;
    }

    public Point3 clone() {
      try {
        return(Point3)super.clone();
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
      if (o.getClass() != Point3.class) {
        return false;
      }
      Point3 p = (Point3)o;
      return (this.x == p.x && this.y == p.y && this.h == p.h);
    }

    public int hashCode() {
      return (Integer.hashCode(x) << 8 + Integer.hashCode(y)) << 16 + Integer.hashCode(h);
    }

    public String toString() {
      return String.format("[%d, %d, %d]", x, y, h);
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
   * @param h0 initial height
   * @return a list of all the cells visited on a linear path between p0 and p1
   */
  public static List<Point3> interpolate(Point p0, Point p1, int h0) {

    List<Point3> points = new ArrayList<>();

    // number of steps required to move
    int n = Math.max(Math.abs(p1.x - p0.x), Math.abs(p1.y - p0.y));

    for (int t = 0; t <= n; t++) {
      int xt = p0.x + round(t * (float)(p1.x - p0.x) / n);
      int yt = p0.y + round(t * (float)(p1.y - p0.y) / n);
      int ht = round(h0 - (float)t * h0 / n);

      points.add(new Point3(xt, yt, ht));
    }
    return points;
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

  /**
   * Interpolate the trajectory of a snowball
   *
   * @param path a list of points from interpolate()
   * @return the interpolated path including the heights of the destination points
   */
  String pathString(List<Point3> path) {
    return path.stream().map(Point3::toString)
        .collect(Collectors.joining("-"));
  }

  /**
   * Check for obstacles and accuracy of the path provided
   *
   * @param path a list of 3d points (including height) in case a snowman is the target
   * @param target the 2d point for the target
   * @return true if the target is in the path provided and there are no obstacles
   */
  public boolean isAccurateTrajectory(List<Point3> path, Point target) {
    boolean isSnowman = ground[target.x][target.y] == Const.GROUND_SMB;
    List<Point> friendlies = players().stream().map(ea -> ea.pos).collect(Collectors.toList());

    // assume the start point of the path contains a player, so don't check it
    for (int i = 1; i < path.size(); i++) {
      Point3 pt = path.get(i);
      // start with the happy case
      if (pt.x == target.x && pt.y == target.y) {
        return true;
      }
      // it's okay if the path goes off the board
      if (pt.x < 0 || pt.x >= Const.SIZE || pt.y < 0 || pt.y >= Const.SIZE) {
        continue;
      }
      if (friendlies.stream().anyMatch(p -> p.x == pt.x && p.y == pt.y)) {
        log("invalid trajectory: friendly at " + pt);
        return false;
      }
      // don't worry about snowballs blocking our shots
      if (ground[pt.x][pt.y] == Const.GROUND_TREE || ground[pt.x][pt.y] == Const.GROUND_SMR) {
        log("invalid trajectory: obstacle at " + pt);
        return false;
      }
    }

    if (isSnowman) {
      int h = height[target.x][target.y];
      return path.stream().anyMatch(el -> el.x == target.x && el.y == target.y && el.h == h);
    }
    else {
      return path.stream().anyMatch(el -> el.x == target.x && el.y == target.y);
    }
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

  public List<Point> neighbors12(Point origin, boolean standing) {

    List<Point> neighbors = standing ? neighbors8(origin) : neighbors4(origin);

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
            (ground[n.x][n.y] == Const.GROUND_EMPTY)) {

          neighbors.add(r);
        }
      }
      // prefer the far distances to the near ones
      // but keep the close ones just in case the far ones lead to dead-ends
      Collections.reverse(neighbors);
    }

    return neighbors.stream()
        .filter(pt -> ground[pt.x][pt.y] == Const.GROUND_EMPTY)
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

      List<Point> neighbors = neighbors12(current, standing);

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
      int zone = Zones.indexOf(zoneContaining(pos));

      List<String> status = new ArrayList<>();
      status.add(String.format("pos:%s", pos));
      status.add(String.format("dest:%s", runTarget));
      status.add(String.format("zone:%s", zone));
      status.add(String.format("hold:%s", holding));
      status.add(String.format("stand:%s", standing ? "S" : "C"));
      if (dazed > 0) status.add(String.format("dazed:%d", dazed));
      if (activity != null) status.add(String.format("act:%s", activity.code()));

      return String.format("Player %d {%s}", id, String.join(", ", status));
    }

    /**
     * Destination for movement, prior to executing current activity
     */
    Point runTarget = new Point(-1, -1);

    /**
     * To avoid repeatedly hitting the same target (due to some bugs with targeting)
     */
    Point lastTarget = null;

    /**
     * Current set of moves being executed
     */
    Build activity;

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

          // TODO this friendly test is ineffective
          //  need to check that *no* points contain a teammate,
          //  this will only be true if *every* point contains a teammate
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
     * Enemy snowmen will reduce the visibility of our own snowmen.
     * This is a tricky problem because we won't actually know they
     * are there, unless one of our players is nearby.
     *
     * If there are points on the board where we expect visibility,
     * we can send our players there to better address the threat.
     *
     * @param pos the origin of one of our snowmen
     * @return a list of points with unexpected zero visibility
     */
    List<Point> encroachments(Point pos) {
      int range = 8;
      List<Point> encroachments = new ArrayList<>();
      for (int i = pos.x - range; i < pos.x + range; i++) {
        for (int j = pos.y - range; j < pos.y + range; j++) {
          if (i < 0 || i >= Const.SIZE || j < 0 || j >= Const.SIZE ||
              (pos.x - i) * (pos.x - i) + (pos.y - j) * (pos.y - j) >= 8*8) {
            continue;
          }
          // we should be able to see this, but if we can't there is probably an enemy snowman nearby
          if (ground[i][j] < 0) {
            encroachments.add(new Point(i, j));
          }
        }
      }
      return encroachments;
    }

    /**
     *
     */
    public void reposition() {
      List<Point> our_snowmen = nearbyItems(Const.GROUND_SMR);
      Set<Point> forbidden = new HashSet<>(visited);

      forbidden.addAll(itemsMatching(Const.GROUND_TREE));

      for (Point p : our_snowmen) {
        forbidden.addAll(neighbors8(p));
      }
      for (Player p : players()) {
        forbidden.addAll(neighbors8(p.runTarget));
      }

      for (Point ours : our_snowmen) {
        List<Point> encroachments = encroachments(ours);
        if (!encroachments.isEmpty()) {
          encroachments.sort(Comparator.comparingInt(e -> euclidean(e, pos)));
          Collections.reverse(encroachments);
          String encroachmentString = encroachments.stream().map(Point::toString)
              .collect(Collectors.joining(", "));
          log("%s is encroached at %s", ours, encroachmentString);
          for (Point enc : encroachments) {
            if (!forbidden.contains(enc)) {
              log("%s is repositioning to encroachment at %s", this, enc);
              runTarget = enc;
              return;
            }
          }
        }
      }

      // no encroachment - not good if are we losing snowmen
      // return a random square in the nearest dark zone (>30% hidden)
      // that does not contain one of our teammates

      List<Point> team = friends().stream()
          .map(f -> f.pos)
          .collect(Collectors.toList());

      log("%s is repositioning. Current zone is %s", this, zoneContaining(pos));

      List<Zone> darkZones = Zones.stream()
          .filter(z -> (float) z.hiddenCount() / z.points.size() > 0.30 &&
              !z.points.contains(team.get(0)) &&
              !z.points.contains(team.get(1)) &&
              !z.points.contains(team.get(2)))
          .sorted(Comparator.comparingInt(z -> euclidean(pos, z.centroid)))
          .collect(Collectors.toList());

      // if there are not dark zones, just continue to patrol current zone
      Zone zone = darkZones.isEmpty() ? zoneContaining(pos) : darkZones.get(0);

      log("%s is repositioning. New zone is %s", this, zone);

      this.runTarget = zone.points.get(rnd.nextInt(zone.points.size()));
    }

    /**
     * Return a move to get this child closer to target.
     */
    Move moveToward(Point target) {
      Point dest = target;

      log(this + " is moving toward " + dest);
      if (ground[dest.x][dest.y] < 0) {
        // temporary destination
        dest = furthestVisiblePointBetween(pos, target);
        log(" => %s is invisible, temporary dest is %s", target, dest);
      }
      // don't walk into trees
      if (ground[dest.x][dest.y] == Const.GROUND_TREE) {
        log(" => %s is occupied by a tree (%d)", dest, ground[dest.x][dest.y]);
        Iterator<Point> n_iter = neighbors12(dest, standing).iterator();

        while (ground[dest.x][dest.y] == Const.GROUND_TREE && n_iter.hasNext()) {
          log(" => %s is occupied by a tree (%d)", dest, ground[dest.x][dest.y]);
          dest = n_iter.next();
        }
      }

      // could be crouched to build a snowman and bumped away from the site
      // otherwise, stand up to continue
      boolean doneBuilding = activity == null || activity.isComplete();
      if (!standing && !neighbors4(pos).contains(target) && doneBuilding) {
        return new Move("stand");
      }

      List<Point> path = freePath(pos, dest, standing);

      List<Point> interpolation = interpolate(pos, dest);
      int lastIndex = interpolation.size() - 1;

      while (path.size() < 2 && lastIndex >= 0) {
        log(" => %s is unreachable, temporary dest is %s", target, dest);
        dest = interpolation.get(lastIndex);
        path = freePath(pos, dest, standing);

        if (lastIndex == 0) {
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
        visited.add(runTarget);
        this.reposition();
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

    /**
     * We are unable to act, but we can reassess our current activities
     */
    void handleRetreat() {

      // our position has likely changed, so let the main loop deal with threats
      // move back to runTarget afterwards, then resume building snowman
      if (activity != null && !activity.isComplete()) {
        return;
      }

      // can't target enemy, move to the nearest obstacle
      List<Point> obstacles = nearbyItems(Const.GROUND_TREE);
      // don't hide behind our own snowmen
      obstacles.addAll(nearbyItems(Const.GROUND_SMR));
      obstacles.sort(Comparator.comparingInt(a -> euclidean(pos, a)));

      log(this + " is dazed, retreating...");
      if (!obstacles.isEmpty() && euclidean(obstacles.get(0), pos) < 2) {
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
    }

    /**
     * The nearest player on our team to a point
     *
     * @param p0 the point
     * @return the teammate nearest to the point p
     */
    Player nearestPlayer(Point p0) {
      Optional<Player> closest = players().stream()
          .min(Comparator.comparingInt(p1 -> euclidean(p0, p1.pos)));

      if (closest.isPresent()) {
        return closest.get();
      }
      log("There must be a player close to %s", p0);
      return null;
    }

    /**
     * Don't tolerate being next to enemies
     *
     * @return a move instruction for a random destination
     */
    Move dodge() {
      log("  => ✅ dodge");
      List<Point> dodges = neighbors12(pos, standing);
      if (dodges.isEmpty() && !standing) {
        return new Move("stand");
      }
      Point dodge = dodges.get(rnd.nextInt(dodges.size()));
      return new Move(standing ? "run" : "crawl", dodge.x, dodge.y);
    }

    /**
     * Consider the threats on the board and return a suitable response
     *
     * @return move to address the threat -- null if no suitable response is available
     */
    Move handleThreats() {
      // which opponents are potential targets/threats?
      // NB: if we can't see them, it doesn't mean they can't see us
      List<Player> threats = visibleOpponents();
      List<Point> targets = threats.stream()
          .map(ea -> ea.pos)
          .collect(Collectors.toList());

      List<Point> snowmen = nearbyItems(Const.GROUND_SMB).stream()
          .filter(p -> (ground[p.x][p.y] != Const.GROUND_EMPTY &&
              ground[p.x][p.y] != GROUND_CHILD))
          .collect(Collectors.toList());

      targets.addAll(snowmen);
      targets.sort(Comparator.comparingInt(p -> euclidean(this.pos, p)));

      if (!targets.isEmpty()) {
        String threatString = targets.stream().map(Point::toString)
            .collect(Collectors.joining(", "));
        log(this + " sees targets at " + threatString);
      }

      for (Point target : targets) {
        boolean isSnowman = ground[target.x][target.y] == Const.GROUND_SMB;

        int dist = euclidean(this.pos, target);
        int dx = target.x - pos.x;
        int dy = target.y - pos.y;

        double angle = Math.atan2(dy, dx);

        // height of the player throwing the snowball
        int h0 = standing ? Const.STANDING_HEIGHT : Const.CROUCHING_HEIGHT;
        // height of the target's head (only matters for snowmen; for players we just need h > 0)
        int h = height[target.x][target.y];

        // it's a snowball fight, not hand-to-hand combat
        if (dist < 2 && !isSnowman) {
          return dodge();
        }

        // if the snowman is 9 units tall, we need to be within 2 units
        if (isSnowman && h0 == h && nearestPlayer(target) == this) {
          return moveToward(target);
        }

        boolean isDecap = isSnowman && h0 > h;

        int overthrow = isDecap ? dist * h0 / (h0 - h) : dist * 3;
        int max_range = isDecap ? Const.THROW_LIMIT / (h0 / (h0 - h)) : 8;

        // clear and present danger
        if (holding >= Const.HOLD_S1 && holding <= Const.HOLD_S3) {
          // stand up if the player is armed
          if (!standing) {
            return new Move("stand");
          }
          // excellent, we are prepared
          if (dx * dx + dy * dy < max_range * max_range) {
            Point p1 = new Point((int) (pos.x + overthrow * Math.cos(angle)),
                (int) (pos.y + overthrow * Math.sin(angle)));

            List<Point3> path = interpolate(pos, p1, h0);
            log("  => overthrow = %d, interpolate %s to %s is %s", overthrow, pos, p1, pathString(path));

            if (isAccurateTrajectory(path, target)) {
              // sometimes we have an accurate trajectory but we fail to kill the enemy snowman
              // don't get stuck on the same pattern over and over
              if (target.equals(lastTarget)) {
                // it's a snowball fight, not hand-to-hand combat
                if (dist < 2) {
                  // target must be a snowman because of dodge check, above
                  // this should happen in the main loop anyhow
                  return new Move("pickup", target.x, target.y);
                }
                lastTarget = null;
                return moveToward(target);
              }

              log("%s *** target %s: %s, p1:%s, h:%d", this, (isSnowman ? "snowman" : "player"),
                  target, p1, h);
              lastTarget = target;

              return new Move("throw", p1.x, p1.y);
            }
          }
        }
        else {
          // we are being targeted, can we catch instead of getting hit?
          for (Player threat : threats) {
            if (threat.holding == Const.HOLD_S1 &&
                euclidean(threat.pos, pos) <= 8 &&
                holding == Const.HOLD_EMPTY) {
              log("%s getting ready to catch from %s", this, threat);
              return new Move("catch", threat.pos.x, threat.pos.y);
            }
          }
        }
      }
      return null;
    }

    /**
     * Ensure players are armed
     *
     * @return optimal next move for the player to obtain a snowball
     */
    Move acquireSnowball() {
      Move m = new Move();

      // look for a nearby snowball
      List<Point> nearby = nearbyItems(Const.GROUND_S);
      nearby.addAll(nearbyItems(Const.GROUND_MS));  // some of these might be lying around
      nearby.addAll(nearbyItems(Const.GROUND_LS));
      nearby.addAll(nearbyItems(Const.GROUND_SMB)); // pick up a snowball from enemy snowman?
      nearby.sort(Comparator.comparingInt(pt -> euclidean(pt, pos)));

      // Crush into a snowball, if we have snow.
      if (holding == Const.HOLD_P1) {
        m.action = "crush";
      }
      // holding something that won't help build a snowball, put it down
      else if (holding == Const.HOLD_L) {
        m.action = "drop";
        Optional<Point> dest = neighbors8(pos).stream()
            .filter(p -> ground[p.x][p.y] == Const.GROUND_EMPTY).findAny();
        if (dest.isPresent()) {
          m.dest = dest.get();
          log("%s is dropping a large snowball at %s", this, m.dest);
        }
      }
      // holding something that won't help build a snowball, put it down
      else if (holding == Const.HOLD_M) {
        m.action = "drop";
        // first choice is to drop on a large snowball if there is one
        Optional<Point> dest = neighbors8(pos).stream()
            .filter(p -> ground[p.x][p.y] == Const.GROUND_L).findAny();
        m.dest = dest.orElseGet(() -> neighbors8(pos).get(0));
      }
      // holding something that won't help build a snowball, put it down anywhere
      else if (holding > 0) {
        Optional<Point> dest = neighbors8(pos).stream()
            .filter(p -> ground[p.x][p.y] == Const.GROUND_EMPTY)
            .findAny();
        m.action = "drop";
        m.dest = dest.orElseGet(() -> neighbors8(pos).get(0));
      }
      // we can get there in 2 turns or so it's cheaper than building a new one
      else if (!nearby.isEmpty() && neighbors8(pos).contains(nearby.get(0))) {
        if (standing) {
          m.action = "crouch";
        }
        else {
          m.action = "pickup";
          m.dest = nearby.get(0);
        }
      }
      else if (!nearby.isEmpty() && neighbors12(pos, standing).contains(nearby.get(0))) {
        if (!standing) {
          m.action = "stand";
        }
        else {
          m.action = "run";
          // don't step on the snowball
          Point sb = new Point(Math.max(nearby.get(0).x, pos.x), Math.max(nearby.get(0).y, pos.y));
          m.dest = bestDestinationCloseTo(sb, pos, true);
          log("%s %s to %s to pick up a snowball at %s", this, m.action, m.dest, nearby);
        }
      }
      // otherwise just make one
      else {
        // We don't have snow, see if there is some nearby.
        int sx = -1, sy = -1;
        // Look in front of us first
        for (Point n : neighbors8(pos)) {
          if (ground[n.x][n.y] == Const.GROUND_EMPTY && height[n.x][n.y] > 0) {
            sx = n.x;
            sy = n.y;
            break;
          }
        }
        // If there is snow, try to get it.
        if (sx >= 0) {
          if (standing) {
            m.action = "crouch";
          }
          else {
            m.action = "pickup";
            m.dest = new Point(sx, sy);
          }
        }
        // go somewhere else if there's no snow nearby
        else {
          reposition();
          return moveToward(runTarget);
        }
      }
      return m;
    }

    /**
     * Return true if the current zone does not have a red snowman
     *
     * @return true if there is no snowman in the current zone
     */
    boolean zoneNeedsSnowman() {
      List<Point> ours = nearbyItems(Const.GROUND_SMR);
      Zone current = zoneContaining(pos);
      for (Point sm : ours) {
        Zone smz = zoneContaining(sm);
        if (smz == current) {
          return false;
        }
      }
      return true;
    }

    /**
     * If the target square is empty, don't return true unless we are there
     * otherwise it's okay to be adjacent to it
     *
     * TODO the runTarget and all of its neighbors could be covered by obstacles!
     *
     * @return true if we are in the zone specified by the runTarget
     */
    boolean reachedTarget() {
      if (pos.equals(runTarget)) {
        return true;
      }
      else if (ground[runTarget.x][runTarget.y] != Const.GROUND_EMPTY ||
               height[runTarget.x][runTarget.y] >= Const.OBSTACLE_HEIGHT) {
        return neighbors8(runTarget).contains(pos);
      }
      else {
        return false;
      }
    }

    Move decap(Point snowman) {
      if (standing) {
        return new Move("crouch");
      }
      else {
        log("%s decap snowman at %s", this, snowman);
        return new Move("pickup", snowman.x, snowman.y);
      }
    }

    /**
     * Ensure player isn't holding powdered snow, L or M snow balls
     *
     * @return true if there is room for another snowball in the players hands
     */
    boolean canEquipSnowball() {
      return holding == Const.HOLD_EMPTY || holding == Const.HOLD_S1 || holding == Const.HOLD_S2;
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
          // only do this once
          handleRetreat();
          retreat = true;
        }
        // unable to act for now
        return new Move();
      }
      retreat = false;

      Move threatResponse = handleThreats();

      // Opportunities might be a higher priority than threats
      // if the cost of exploiting them is low and the risk of interruption is small

      // 1. make a snowball if we haven't got one
      // 2. look for enemy snowmen to decapitate
      // 3. look for partially-built snowmen to finish
      // 4. if there are no threats or opportunities, continue to the runTarget

      // handle adjacent snowman and partial snowman as special cases
      Optional<Point> adjacentSnowman = neighbors8(pos).stream()
          .filter(pt -> ground[pt.x][pt.y] == Const.GROUND_SMB)
          .findAny();

      Optional<Point> adjacentPartial = neighbors8(pos).stream()
          .filter(pt -> ground[pt.x][pt.y] == Const.GROUND_LM || ground[pt.x][pt.y] == Const.GROUND_L)
          .findAny();

      // maybe there's a nearby unfinished snowman we can work on
      List<Point> nearestPartials = nearestIncomplete(pos);

      // where are teammates working on snowmen
      List<Point> inProgress = friends().stream()
          .filter(p -> p.activity != null)
          .map(p -> p.activity.site)
          .collect(Collectors.toList());

      Optional<Point> nearestPartial = nearestPartials.stream()
          .filter(p -> !inProgress.contains(p) && euclidean(p, pos) < 5)
          .findAny();

      // check if we are adjacent to the target, no need to waste a snowball
      if (canEquipSnowball() && adjacentSnowman.isPresent()) {
        return decap(adjacentSnowman.get());
      }
      else if (activity == null || activity.isComplete()) {
        if (holding < Const.HOLD_S1 || holding > Const.HOLD_S3) {
          log("%s is acquiring a snowball", this);
          return acquireSnowball();
        }
        else if (activity != null && adjacentPartial.isPresent()) {
          log("%s choice is to finish existing snowman", this);
          activity = new Build();
          return activity.nextMove(this);
        }
        else if (nearestPartial.isPresent() && zoneNeedsSnowman()) {
          log("%s choice is opportunistic build", this);
          activity = new Build();
          return moveToward(nearestPartial.get());
        }
        else if (threatResponse != null) {
          log("%s choice is threat response", this);
          return threatResponse;
        }
        else if (!standing) {
          log("%s choice is to stand for navigation", this);
          return new Move("stand");
        }
        else if (!reachedTarget()) {
          log("%s choice is navigation", this);
          return moveToward(runTarget);
        }
        else if (zoneNeedsSnowman()) {
          log("%s choice is new build", this);
          activity = new Build();
          return activity.nextMove(this);
        }
        else {
          reposition();
          return moveToward(runTarget);
        }
      }
      else {
        if (!zoneNeedsSnowman()) {
          log("%s has a snowman in progress but it isn't needed here, repositioning", this);
          activity = null;
          reposition();
          return moveToward(runTarget);
        }
        // get rid of extra snowballs from decapping, they will confuse the builder
        if (holding > Const.HOLD_S1 && holding <= Const.HOLD_S3) {
          if (threatResponse != null) {
            return threatResponse;
          }
          else if (activity.state == 11) {
            for (Point p : neighbors8(pos)) {
              if (ground[p.x][p.y] == Const.GROUND_EMPTY && height[p.x][p.y] <= Const.MAX_PILE - 3) {
                return new Move("drop", p.x, p.y);
              }
            }
            log("%s nowhere to drop snowball", this);
            return new Move("throw", 15, 15);
          }
        }
        log("%s choice is continue building", this);
        return activity.nextMove(this);
      }
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

  class Build {

    Move[] instructions = {
        new Move("idle"), // placeholder: state 0 does not generate a move
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

    int state = 0;
    int start_m = 6;     // offset in instruction list when L is complete
    int start_lm = 10;    // offset in instruction list when L, M are complete

    Point site = null;

    public String code() {
      return String.format("B[%d]", state);
    }

    public String toString() {
      return (String.format("Build {state:%d, site:%s}", state, site));
    }

    public boolean isComplete() {
      return state >= instructions.length - 1;
    }

    public Move nextMove(Player c) {
      // See if we should start running our build script.
      // Are we far from other things, is the ground empty
      // and do we have enough snow to build a snowman.
      // Make sure we have at least 4 empty squares around us

      // we need 2 nearby sources of snow with height >= 3, and an unblocked dest

      // maybe there's a nearby unfinished snowman we can work on
      List<Point> nearestPartials = nearestIncomplete(c.pos);
      Point partial = nearestPartials.size() > 0 ? nearestPartials.get(0) : null;

      // 2. check for a partially completed snowman nearby
      if (site == null && partial != null) {
        if (neighbors8(c.pos).contains(partial)) {
          site = partial;
          state = ground[partial.x][partial.y] == Const.GROUND_L ? start_m : start_lm;
          log("%s found partial (%d) at %s, activity = %s", c, ground[partial.x][partial.y], partial, this);
        }
      }

      if (state == 0) {
        // If we didn't get to finish the last snowman, maybe we're holding something.
        if (c.holding != Const.HOLD_EMPTY) {
          for (Point p : neighbors8(c.pos)) {
            if (ground[p.x][p.y] == Const.GROUND_EMPTY && height[p.x][p.y] <= Const.MAX_PILE - 3) {
              return new Move("drop", p.x, p.y);
            }
          }
        }

        // 3. start a new snowman if there isn't a partial in progress
        if (site == null) {
          int powder = 0;
          for (Point p : neighbors8(c.pos)) {
            powder += height[p.x][p.y];
            if (site == null && ground[p.x][p.y] == Const.GROUND_EMPTY) {
              log("%s building snowman at %s", c, p);
              site = p;
            }
            if (site != null && powder >= 6) {
              state = 1;
            }
          }
        }

        if (state == 0) {
          c.reposition();
          log("%s => no suitable snowman site found nearby", c);
        }
      }

      // are we building a snowman?
      if (state > 0) {
        // return a move from our instruction template
        Move m = new Move(instructions[state].action);

        // don't add to instruction list because we could be working on a partial
        if (m.action.equals("pickup") && c.standing) {
          return new Move("crouch");
        }

        // ensure we did not move away from our building site
        if (!neighbors8(c.pos).contains(site)) {
          return c.moveToward(site);
        }

        if (instructions[state].dest != null) {
          if (m.action.equals("drop")) {
            m.dest = site;
          }
          else if (m.action.equals("pickup")) {
            for (Point p : neighbors8(c.pos)) {
              if (ground[p.x][p.y] == Const.GROUND_EMPTY && height[p.x][p.y] > 0) {
                m.dest = p;
              }
            }
            if (m.dest == null) {
              log("%s: No snow near %s", c, site);
              c.reposition();
              state = 0;
              return new Move("stand");
            }
          }

          if (m.dest != null && ground[m.dest.x][m.dest.y] == GROUND_CHILD) {
            // if someone has stepped on our pickup or build destination, wait before continuing
            log("%s someone is in the way (%s) at activity step %d: idling", c, m.dest, state);
            return new Move();
          }
        }
        state++;
        return m;
      }

      log(c.activity + " is complete, should not be idling");
      return new Move();
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
   * Sorted list of partially complete snowmen, sorted by distance
   *
   * @param p point of interest
   * @return a list of points containing L balls or LM stacks
   */
  public List<Point> nearestIncomplete(Point p) {
    List<Point> nearestLMs = itemsMatching(Const.GROUND_LM);
    List<Point> nearestLs = itemsMatching(Const.GROUND_L);
    List<Point> nearestPartials = new ArrayList<>();
    nearestPartials.addAll(nearestLMs);
    nearestPartials.addAll(nearestLs);
    nearestPartials.sort(Comparator.comparingInt(a -> euclidean(p, a)));
    return nearestPartials;
  }

  /**
   * Find the point closest to end that is visible
   * with the shortest path from start.
   *
   * @param end desired destination
   * @param start starting point (usually a player's current position)
   * @param avoidPlayers true if squares containing other players are valid destinations
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

      List<Point> neighbors = neighbors12(current, true);

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
    // TODO sometimes returns null - this algorithm seems overly complicated
    return best == null ? start : best;
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
        logVisibility("visibility.txt", "Turn " + turnNum);
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
          }
        }
        dests[i] = m.dest;

        /* Write out the child's move */
        if (m.dest == null) {
          log("%s action is %s", cList[i], m.action);
          System.out.println(m.action);
        }
        else {
          log("%s action is %s %s", cList[i], m.action, m.dest);
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
    // now write children state
    for (int i = 0; i < Const.CCOUNT * 2; i++) {
      // {x:0, y:2, stance:'S', carry:'a', dazed:0}
      Player c = cList[i];
      String status = String.format("%d %d %s %s %d\n", c.pos.x, c.pos.y,
          c.standing ? "S" : "C", (char)(c.holding + 'a'), c.dazed);
      b.append(status);
    }
    return b.toString();
  }

  public void logVisibility(String fileName, String prefixMessage) {
    try {
      // Open given file in append mode.
      BufferedWriter out = new BufferedWriter(
          new FileWriter(fileName, true));

      out.write(prefixMessage + '\n');
      out.write(printVisibility(ground));
      out.write('\n');

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
    verboseDebug = environment.containsKey("SEEKER_V2_VERBOSE_DEBUG");
    debug = environment.containsKey("SEEKER_V2_DEBUG");

    Seeker_V2 seeker = new Seeker_V2();
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
