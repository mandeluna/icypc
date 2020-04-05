package oocl.icypc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
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
  public void testInitialPaths() {
    List<Point> targets = seeker.playerLocations();
    int previous_score = 0;

    for (int i = 0; i < 16; i++) {
      // each iteration should discard board visibility changes made during previous iterations
      int new_score = seeker.iterateVisibilityTargets(targets, seeker.getGround());
      assertTrue(new_score > previous_score);
      previous_score = new_score;
    }
    assertEquals(Seeker.euclidean(targets.get(0), targets.get(1)), 16);
  }
}