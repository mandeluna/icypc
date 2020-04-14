package oocl.icypc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import icypc.Const;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import oocl.icypc.Seeker.Player;
import oocl.icypc.Seeker.Point;
import oocl.icypc.Seeker.Point3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

@TestInstance(Lifecycle.PER_CLASS)
class SeekerTest {

  String initialMap = "3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a * * * * * * * * * * * * * * * * * * * *\n"
      + "3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a * * * * * * * * * * * * * * * * * * * *\n"
      + "3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a * * * * * * * * * * * * * * * * * * * *\n"
      + "3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a * * * * * * * * * * * * * * * * * * * *\n"
      + "3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a * * * * * * * * * * * * * * * * * * * *\n"
      + "3a 3a 3a 3a 3a 3a 3a 3a 3a 3a * * * * * * * * * * * * * * * * * * * * *\n"
      + "3a 3a 3a 3a 3a 3a 3a 3a 3a 3a * * * * * * * * * * * * * * * * * * * * *\n"
      + "3a 3a 3a 3a 3a 3a 3a 3a 3a * * * * * * * * * * * * * * * * * * * * * *\n"
      + "3a 3a 3a 3a 3a 3a 3a 3a * * * * * * * * * * * * * * * * * * * * * * *\n"
      + "3a 3a 3a 3a 3a 3a 3a * * * * * * * * * * * * * * * * * * * * * * * *\n"
      + "3a 3a 3a 3a 3a * * * * * * * * * * * * * * * * * * * * * * * * * *\n"
      + "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\n"
      + "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\n"
      + "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\n"
      + "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\n"
      + "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\n"
      + "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\n"
      + "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\n"
      + "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\n"
      + "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\n"
      + "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\n"
      + "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\n"
      + "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\n"
      + "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\n"
      + "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\n"
      + "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\n"
      + "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\n"
      + "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\n"
      + "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\n"
      + "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\n"
      + "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\n"
      + "0 2 S a 0\n"
      + "1 2 S a 0\n"
      + "2 1 S a 0\n"
      + "2 0 S a 0\n"
      + "* *\n"
      + "* * \n"
      + "* * \n"
      + "* *";

  String busyMap = "* 3a 3a 2a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a * * *\n"
      + "* 3a 3a 2a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a * * *\n"
      + "* 3a 3a 3a 3a 3a 3a 3a 3k 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a * *\n"
      + "* 2a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a * *\n"
      + "* 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a * *\n"
      + "3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3k 3a 3a 3a 3a 3a 3a 3a * *\n"
      + "3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 9i 0a 3a 3a 3a 3a 3a 3a * *\n"
      + "3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 4c 3a 3a 3a 3a * *\n"
      + "3a 3a 3a 3a 3a 3a 3a 6i 0a 3a 3a 3a 3a 3a 3a 0b 3a 3a 3a 3a 3a 3a 3a 0a 3a 3a 3a 3a 3a * *\n"
      + "3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a * *\n"
      + "3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 0a 3a * * *\n"
      + "3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 0a 0a 0a 0a 2a 3a 3a 3a 3a * * *\n"
      + "* 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a * * 3a 3a 3a 0a 3k 3a 3k 3a 3a 3a 3a * * * *\n"
      + "* 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a * * * * 3a 3a 3a 0a 2a 3a 3a * * * * * *\n"
      + "* * 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a * * * * * * * * * * * * * * * * * *\n"
      + "3a 3a 3a 3a 3a 3a 3a 3a 0b 3a 3a * * * * * * * * * * * * * * * * * * * *\n"
      + "3a 3a 3a 3a 3a 3a 3a 3a * * * * * * * * * * * * * * * * * * * * * * *\n"
      + "3a 3a 3a 3a 3a 3a 3a 3a 3a * * * * 3a 3a 3a 3a 3a 3a 3a * * * * * * * * * * *\n"
      + "3a 3a 3a 3a 3a 3a 3a 3a 3a * * 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a * * * * * * * * *\n"
      + "3a 3a 3a 3a 3a 3a 3a 3a 3a 2a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a * * * * * * * *\n"
      + "3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 2a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a * * * * * * * *\n"
      + "3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 2a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 2a 3a * * * * * * *\n"
      + "3a 3a 3k 3a 3a 3a 3a 3a 4c 3a 3a 3a 0a 2a 3a 0b 3a 3a 3a 3a 3a 3a 3a 0k * * * * * * *\n"
      + "3a 3a 3a 3a 3a 8g 0a 1a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3k * * * * * * *\n"
      + "3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3k 3a 3a 3a 3a 3a 3a 3a * * * * * * *\n"
      + "3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a * * * * * * *\n"
      + "3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a * * * * * * *\n"
      + "3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a * * * * * * *\n"
      + "3a 3a 3a 3a 3a 3a 3a 3a * * 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a * * * * * * * *\n"
      + "3a 3a 3a 3a 3a 3a * * * * 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a * * * * * * * *\n"
      + "* * * * * * * * * * * 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a * * * * * * * * *";

  Seeker seeker = new Seeker();

  @BeforeEach
  public void readInitialMap() {
    InputStream stream = new ByteArrayInputStream(initialMap.getBytes());
    Scanner in = new Scanner(stream);
    seeker.readCurrentMap(in);
    seeker.readChildrenStates(in);
    seeker.markChildren();
  }

  @Test
  public void testVisibility() {
    int[][] board = seeker.getGround();
    assertEquals(seeker.visibleCount(board), 104);
  }

  @Test
  public void testEmptyVisibilityMarkers() {
    // test empty target list
    List<Point> locations = new ArrayList<>();
    int[][] board = seeker.getGround();
    seeker.markVisibles(board, locations);
    assertEquals(seeker.visibleCount(board), 88);
  }

  @Test
  public void testOneVisibilityMarker() {
    // One player in the center of the board
    List<Point> locations = new ArrayList<>();
    int[][] board = seeker.getGround();
    locations.add(new Point(15, 15));
    seeker.markVisibles(board, locations);
    assertEquals(seeker.visibleCount(board), 88+193);
  }

  @Test
  public void testOneSnowmanVisibilityMarker() {
    int[][] board = seeker.getGround();
    // put a snowman at [7, 7]
    board[7][7] = Const.GROUND_SMR;
    List<Player> players = seeker.players();
    Player player0 = players.get(0);
    Player player1 = players.get(1);
    Player player2 = players.get(2);
    Player player3 = players.get(3);

    assertEquals(player0.pos, new Point(0, 2));
    assertEquals(player1.pos, new Point(1, 2));
    assertEquals(player2.pos, new Point(2, 1));
    assertEquals(player3.pos, new Point(2, 0));

    // reposition everyone after a snowman has been planted
    player0.pos = new Point(7, 23);
    player1.pos = new Point(12, 13);
    player2.pos = new Point(22, 7);
    player3.pos = new Point(7, 7);

    seeker.markVisibles(players.stream().map(e -> e.pos).collect(Collectors.toList()));
    for (Player player : players) {
      player.reposition();
    }
    // TODO repositioning logic needs work
    assertEquals(player3.runTarget, new Point(6, 22));
  }

  @Test
  public void testInitialLocationVisibility() {
    // One player in the center of the board
    List<Point> locations = seeker.playerLocations();
    int[][] board = seeker.getGround();
    seeker.markVisibles(board, locations);
    // 104 is what we get back in visibility.txt from the engine
    // visible rows: 11+11+11+11+11+10+10+9+8+7+5 = 104
    // internally we see: 10+10+10+10+10+9+9+8+7+5 = 88
    // XXX why does initial_map vs. markVisibles give a different result?
    assertEquals(seeker.visibleCount(board), 88);
  }

  @Test
  public void testInitialPlayerLocations() {
    List<Point> locations = seeker.playerLocations();
    // [0, 2], [1, 2], [2, 1], [2, 0]
    assertEquals(Seeker.euclidean(locations.get(0), locations.get(1)), 1);
    assertEquals(Seeker.euclidean(locations.get(0), locations.get(2)), 2);
    assertEquals(Seeker.euclidean(locations.get(0), locations.get(3)), 2);
    assertEquals(Seeker.euclidean(locations.get(1), locations.get(2)), 1);
    assertEquals(Seeker.euclidean(locations.get(1), locations.get(3)), 2);
    assertEquals(Seeker.euclidean(locations.get(2), locations.get(3)), 1);
  }

  @Test
  public void test4Neighbors() {
    Point center = new Point(15, 15);
    Point[] nearby = {
        new Point(15, 16),  // N
        new Point(16, 15),  // E
        new Point(15, 14),  // S
        new Point(14, 15),  // W
    };
    List<Point> neighbors = seeker.neighbors4(center);
    assertEquals(neighbors, Arrays.asList(nearby));
  }

  @Test
  public void test8Neighbors() {
    Point center = new Point(15, 15);
    Player player = seeker.players().get(0);
    player.pos = center;

    Point[] nearby = {
        new Point(15, 16),  // N
        new Point(16, 15),  // E
        new Point(15, 14),  // S
        new Point(14, 15),  // W
        new Point(16, 16),  // NE
        new Point(16, 14),  // SW
        new Point(14, 14),  // SE
        new Point(14, 16),  // NW
    };
    assertEquals(seeker.neighbors8(center), Arrays.asList(nearby));
  }

  @Test
  public void testClone() {
    Point p = new Point(3, 2);
    Point q = p.clone();
    p.x = 4;
    assertEquals(q.x, 3);
    assertEquals(q.y, 2);
  }

  @Test
  public void testPlayer0Neighbors() {
    Point[] neighbors0_4 = {
        new Point(0, 3),
        new Point(1, 2),
        new Point(0, 1),
    };
    Point[] neighbors0_8 = {
        new Point(0, 3),
        new Point(1, 2),
        new Point(0, 1),
        new Point(1, 3),
        new Point(1, 1),
    };

    List<Player> players = seeker.players();
    assertEquals(seeker.neighbors4(players.get(0).pos), Arrays.asList(neighbors0_4));
    assertEquals(seeker.neighbors8(players.get(0).pos), Arrays.asList(neighbors0_8));
  }

  @Test
  public void testInitialPaths() {
    List<Player> players = seeker.players();
    int[][] board = seeker.getGround();

    Point[] points = {
      new Point(7, 23),
      new Point(22, 22),
      new Point(22, 7),
      new Point(7, 7),
    };

    for (int i = 0; i < points.length; i++) {
      players.get(i).pos = points[i];
    }

    seeker.markVisibles(board, Arrays.asList(points));
    int score = seeker.visibleCount(board);
    assertEquals(score, 780);
  }

  @Test
  public void testPathFinding() {
    InputStream stream = new ByteArrayInputStream(busyMap.getBytes());
    Scanner in = new Scanner(stream);
    seeker.readCurrentMap(in);
    List<Point> trees = seeker.itemsMatching(Const.GROUND_TREE);

    // should be 3 trees on this map (need visible obstacles)
    // at [8, 15], [15, 8], [22, 15]
    assertEquals(trees.size(), 3);

    Point origin = new Point(8, 10);
    Point dest = new Point(8, 23);
    // path should route around the tree at [8, 15]

    Point[] routes = {
        new Point(8,10),
        new Point(9,11),
        new Point(10,12),
        new Point(11,13),
        new Point(10,14),
        new Point(9,15),
        new Point(8,16),
        new Point(8,17),
        new Point(8,18),
        new Point(8,19),
        new Point(8,20),
        new Point(8,21),
        new Point(8,22),
        new Point(8, 23),
    };

    List<Point> path = seeker.freePath(origin, dest, true);

    assertEquals(path.get(0), routes[0]);
    assertEquals(path.get(path.size() - 1), routes[routes.length - 1]);
    // tree is not on the path
    assertFalse(path.contains(new Point(8, 15)));
  }

  @Test
  public void testBusyMap() {
    InputStream stream = new ByteArrayInputStream(busyMap.getBytes());
    Scanner in = new Scanner(stream);
    seeker.readCurrentMap(in);
    int[][] board = seeker.getGround();

    List<Point> locs = new ArrayList<>();
    List<Point> sm_locs = new ArrayList<>();
    for (int i = 0; i < Const.SIZE; i++) {
      for (int j = 0; j < Const.SIZE; j++) {
        if (board[i][j] == Seeker.GROUND_CHILD) {
          locs.add(new Point(i, j));
        }
        else if (board[i][j] == Const.GROUND_SMR) {
          sm_locs.add(new Point(i, j));
        }
      }
    }
    assertEquals(locs.size(), 8);
    assertEquals(board[8][7], Const.GROUND_SMR);
    assertEquals(board[6][21], Const.GROUND_SMR);

    // all players are visible in the map above
    // assume the colors are as indicated below
    // "[2, 8]"    Red
    // "[5, 21]"   Red
    // "[12, 20]"  Blue
    // "[12, 22]"  Blue
    // "[22, 2]"   Red
    // "[22, 23]"  Blue
    // "[23, 23]"  Blue
    // "[24, 16]"  Red
    List<Player> players = seeker.players();
    players.get(0).pos = locs.get(0);
    players.get(1).pos = locs.get(1);
    players.get(2).pos = locs.get(4);
    players.get(3).pos = locs.get(7);

    Player player1 = players.get(1);
    player1.reposition();

    // snowmen should still be intact
    assertEquals(board[8][7], Const.GROUND_SMR);
    assertEquals(board[6][21], Const.GROUND_SMR);
  }

  @Test
  public void testTrajectory() {
//    Player 0 {pos:[29, 27], dest:[28, 28], hold:4, stand:s} targets: [29, 23] at 4 units away
//  => interpolate [29, 27] to [29, 15] is [29, 27, 9]-[29, 26, 9]-[29, 25, 8]-[29, 24, 7]-[29, 23, 6]-[29, 22, 6]-[29, 21, 5]-[29, 20, 4]-[29, 19, 3]-[29, 18, 3]-[29, 17, 2]-[29, 16, 1]-[29, 15, 0]
//  => interpolate [29, 27] to [29, 19] is [29, 27, 9]-[29, 26, 8]-[29, 25, 7]-[29, 24, 6]-[29, 23, 5]-[29, 22, 4]-[29, 21, 3]-[29, 20, 2]-[29, 19, 0]

    Point start = new Point(29, 27);
    Point target = new Point(29, 23);

    int dx = target.x - start.x;
    int dy = target.y - start.y;

    double angle = Math.atan2(dy, dx);
    int dist = seeker.euclidean(start, target); // TODO rounding error is being magnified

    Point focus = new Point((int) (start.x + (dist * 3) * Math.cos(angle)),
        (int) (start.y + (dist * 3) * Math.sin(angle)));

    List<Point3> path = seeker.interpolate(start, focus, 9);
    assertTrue(seeker.isAccurateTrajectory(path, target));
  }

} // class
