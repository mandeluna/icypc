package oocl.icypc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import icypc.Const;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import oocl.icypc.Seeker.Player;
import oocl.icypc.Seeker.Point;
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

  String busyMap = "* * * * 3a 3a 3a 3a 3a 3a 3a * * * * * * * * * 3a 3a 3a 3a 3a 3a 3a * * * *\n"
      + "* * 3a 2a 3a 3a 3a 3a 3a 3a 3a 3a 3a * * * * * 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a * *\n"
      + "* 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a * * * 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a *\n"
      + "* 2a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a * 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a *\n"
      + "3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a\n"
      + "3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a\n"
      + "3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a\n"
      + "3a 3a 3a 3a 3a 3a 3a 3k 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3k 3a 3a 3a 3a 3a 3a 3a\n"
      + "3a 3a 3a 3a 3a 3a 3a 6i 0a 3a 3a 3a 3a 3a 3a 0b 3a 3a 3a 3a 3a 3a 3a 6i 0a 3a 3a 3a 3a 3a 3a\n"
      + "3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3k 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a\n"
      + "3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a\n"
      + "3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a\n"
      + "* 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a *\n"
      + "3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a *\n"
      + "3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a * 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a * *\n"
      + "3a 3a 3a 3a 3a 3a 3a 3a 0b 3a 3a 3a 3a 3a 3a 0b * * * * 3a 3a 0b 3a 3a 3a 3a * * * *\n"
      + "3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a * * * * * * * * * * * * * * * * *\n"
      + "3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a * * * * * * * * * * * * * * * * * * *\n"
      + "3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a * * * * * * * * * * * * * * * * * * *\n"
      + "3a 3a 3a 3a 3k 3a 3a 3a 3a 3a 3a 3a * * * * * * * * * * * * * * * * * * *\n"
      + "3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3a * * * * * * * * * * * * * * * * * * *\n"
      + "3a 3a 3a 3a 3a 6f 3a 3a 3a 3a 3a 3a * * * * * * * * * * * * * * * * * * *\n"
      + "3a 3a 3a 3a 3a 3a 3a 3a 4c 3a 3a 3a * * * * * * * * * * * * * * * * * * *\n"
      + "3a 3a 3a 3a 3a 3a 3a 0a 3a 0a 2a * * * * * * * * * * * * * * * * * * * *\n"
      + "3a 3a 3a 3a 3a 3a 3a 3a 3a 3a 3k * * * * * * * * * * * * * * * * * * * *\n"
      + "3a 3a 3a 3a 3a 3a 3a 3a 3a 3a * * * * * * * * * * * * * * * * * * * * *\n"
      + "* 3a 3a 3a 3a 3a 3a 3a * * * * * * * * * * * * * * * * * * * * * * *\n"
      + "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\n"
      + "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\n"
      + "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\n"
      + "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *";

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
    assertEquals(seeker.visibleCount(board), 0);
  }

  @Test
  public void testOneVisibilityMarker() {
    // One player in the center of the board
    List<Point> locations = new ArrayList<>();
    int[][] board = seeker.getGround();
    locations.add(new Point(15, 15));
    seeker.markVisibles(board, locations);
    assertEquals(seeker.visibleCount(board), 193);
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
  public void testNeighbors() {
    Point center = new Point(15, 15);
    Point[] nearby = {
        new Point(15, 16),
        new Point(16, 15),
        new Point(14, 14),
        new Point(14, 15),
        new Point(16, 16),
        new Point(16, 14),
        new Point(14, 14),
        new Point(14, 16),
    };
    assertEquals(seeker.neighbors(center), Arrays.asList(nearby));
  }

  @Test
  public void testNeighborsByAngle() {
    Point center = new Point(15, 15);
    Player player = seeker.players().get(0);
    player.pos = center;

    Point[] nearby = {
        new Point(16, 16),
        new Point(16, 15),
        new Point(16, 14),
        new Point(15, 14),
        new Point(14, 14),
        new Point(14, 15),
        new Point(14, 16),
        new Point(15, 16),
    };
    assertEquals(player.neighbors8(), Arrays.asList(nearby));
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
        new Point(1, 3),
        new Point(1, 2),
        new Point(1, 1),
        new Point(0, 1),
    };

    List<Player> players = seeker.players();
    assertEquals(players.get(0).neighbors4(), Arrays.asList(neighbors0_4));
    assertEquals(players.get(0).neighbors8(), Arrays.asList(neighbors0_8));
  }

  @Test
  public void testInitialPaths() {
    List<Point> targets = seeker.playerLocations();
    int previous_score = -1;
    int score = 0;
    int[][] board = seeker.getGround();
    int max_iterations = 30;
    int iterations = 0;

    while (iterations < max_iterations && score > previous_score) {
      previous_score = score;
      // each iteration should discard board visibility changes made during previous iterations
      score = seeker.iterateVisibilityTargets(targets, board);
      iterations++;
    }

    Point[] points = {
      new Point(7, 23),
      new Point(22, 22),
      new Point(22, 7),
      new Point(7, 7),
    };
    assertEquals(score, 772);
    assertEquals(targets, Arrays.asList(points));
  }

  @Test
  public void testBusyMap() {
    InputStream stream = new ByteArrayInputStream(busyMap.getBytes());
    Scanner in = new Scanner(stream);
    seeker.readCurrentMap(in);
    int[][] board = seeker.getGround();

    List<Point> locs = new ArrayList<>();
    for (int i = 0; i < Const.SIZE; i++) {
      for (int j = 0; j < Const.SIZE; j++) {
        if (board[i][j] == Seeker.GROUND_CHILD) {
          locs.add(new Point(i, j));
        }
      }
    }

    assertEquals(locs.size(), 5);

    List<Player> players = seeker.players();
    for (int i = 0; i < players.size(); i++) {
      players.get(i).pos = locs.get(i);
    }

    Point point3 = new Point(7, 7);
    Player player3 = players.stream().filter(p -> p.pos.equals(point3)).findAny().get();
    seeker.repositionPlayer(player3);

    assertEquals(player3.pos, new Point(7, 23));
  }

} // class
