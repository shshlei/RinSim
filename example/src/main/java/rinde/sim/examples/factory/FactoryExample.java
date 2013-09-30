package rinde.sim.examples.factory;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;

import java.math.RoundingMode;
import java.util.List;
import java.util.Set;

import javax.measure.Measure;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

import rinde.sim.core.Simulator;
import rinde.sim.core.graph.Graph;
import rinde.sim.core.graph.Graphs;
import rinde.sim.core.graph.LengthData;
import rinde.sim.core.graph.MultimapGraph;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.examples.demo.SwarmDemo;
import rinde.sim.ui.View;
import rinde.sim.ui.renderers.GraphRoadModelRenderer;
import rinde.sim.ui.renderers.RoadUserRenderer;
import rinde.sim.ui.renderers.UiSchema;

import com.google.common.collect.ImmutableList;
import com.google.common.math.DoubleMath;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class FactoryExample {

  static final double POINT_DISTANCE = 5d;
  static final long SERVICE_DURATION = 120000;
  static final int NUM_VECHICLES = 20;
  static final double AGV_SPEED = 10;

  static final int FONT_SIZE = 14;
  // spacing between text pixels
  static final double SPACING = 30d;
  // spacing between vertical lines in line units
  static final int VERTICAL_LINE_SPACING = 8;
  static final List<String> WORDS = asList("Agent\nWise", "Distri\nNet");

  private FactoryExample() {}

  public static void main(String[] args) {
    final RandomGenerator rng = new MersenneTwister(123);
    final Simulator simulator = new Simulator(rng, Measure.valueOf(1000L,
        SI.MILLI(SI.SECOND)));

    final ImmutableList.Builder<ImmutableList<Point>> pointBuilder = ImmutableList
        .builder();

    for (final String word : WORDS) {
      pointBuilder.add(SwarmDemo.measureString(word, FONT_SIZE, SPACING, 2));
    }
    final ImmutableList<ImmutableList<Point>> points = pointBuilder.build();
    int max = 0;
    double xMax = 0;
    double yMax = 0;
    for (final List<Point> ps : points) {
      max = Math.max(max, ps.size());
      for (final Point p : ps) {
        xMax = Math.max(p.x, xMax);
        yMax = Math.max(p.y, yMax);
      }
    }

    int width = DoubleMath.roundToInt(xMax / SPACING, RoundingMode.CEILING);
    width += VERTICAL_LINE_SPACING - width % VERTICAL_LINE_SPACING;
    width += (DoubleMath.roundToInt(width / VERTICAL_LINE_SPACING,
        RoundingMode.UNNECESSARY) % 2) == 0 ? VERTICAL_LINE_SPACING : 0;

    int height = DoubleMath.roundToInt(yMax / SPACING, RoundingMode.CEILING) + 2;
    height += height % 2;
    final Graph<?> g = createGrid(width, height, 1, VERTICAL_LINE_SPACING,
        SPACING);

    final RoadModel roadModel = new BlockingGraphRoadModel(g, SI.METER,
        NonSI.KILOMETERS_PER_HOUR);
    final PDPModel pdpModel = new PDPModel();
    simulator.register(roadModel);
    simulator.register(pdpModel);
    simulator.register(new AgvModel(rng, points, getBorderNodes(g)));
    simulator.configure();

    for (int i = 0; i < NUM_VECHICLES; i++) {
      simulator.register(new AGV(rng));
    }

    final UiSchema uis = new UiSchema(false);
    uis.add(AGV.class, "/graphics/flat/forklift2.png");
    View.startGui(simulator, 1, new GraphRoadModelRenderer(30, false, false,
        false), new BoxRenderer(), new RoadUserRenderer(uis, false));
  }

  static void addPath(Graph<?> graph, Point... points) {
    final List<Point> newPoints = newArrayList();
    for (int i = 0; i < points.length - 1; i++) {
      final double dist = Point.distance(points[i], points[i + 1]);
      final Point unit = Point.divide(Point.diff(points[i + 1], points[i]),
          dist);
      final int numPoints = DoubleMath.roundToInt(dist / POINT_DISTANCE,
          RoundingMode.FLOOR);
      for (int j = 0; j < numPoints; j++) {
        final double factor = j * POINT_DISTANCE;
        newPoints.add(new Point(points[i].x + factor * unit.x, points[i].y
            + factor * unit.y));
      }
    }
    newPoints.add(points[points.length - 1]);
    Graphs.addPath(graph, newPoints.toArray(new Point[newPoints.size()]));
  }

  static ImmutableList<Point> getBorderNodes(Graph<?> g) {
    final Set<Point> points = g.getNodes();
    double xMin = Double.MAX_VALUE;
    double yMin = Double.MAX_VALUE;
    double xMax = Double.MIN_VALUE;
    double yMax = Double.MIN_VALUE;

    for (final Point p : points) {
      xMin = Math.min(xMin, p.x);
      yMin = Math.min(yMin, p.y);
      xMax = Math.max(xMax, p.x);
      yMax = Math.max(yMax, p.y);
    }
    final ImmutableList.Builder<Point> builder = ImmutableList.builder();
    for (final Point p : points) {
      if (p.x == xMin || p.x == xMax || p.y == yMin || p.y == yMax) {
        builder.add(p);
      }
    }
    return builder.build();
  }

  static Graph<LengthData> createGrid(int width, int height, int hLines,
      int vLines, double distance) {
    final Graph<LengthData> graph = new MultimapGraph<LengthData>();

    int v = 0;
    // draw vertical lines
    for (int i = 0; i < width + 1; i++) {
      Point prev = new Point(i * distance, 0);
      if (i % vLines == 0) {
        for (int j = 1; j < height; j++) {
          final Point cur = new Point(i * distance, j * distance);
          if (v % 2 == 0) {
            graph.addConnection(prev, cur);
          } else {
            graph.addConnection(cur, prev);
          }
          prev = cur;
        }
        v++;
      }
    }

    int y = 1;
    for (int i = 0; i < height; i++) {
      Point prev = new Point(0, i * distance);
      if (i % hLines == 0) {
        for (int j = 1; j < width + 1; j++) {
          final Point cur = new Point(j * distance, i * distance);
          if (y % 2 == 0) {
            graph.addConnection(prev, cur);
          } else {
            graph.addConnection(cur, prev);
          }
          prev = cur;
        }
      }
      y++;
    }
    return graph;
  }

  static Graph<LengthData> createGraph() {
    final Graph<LengthData> graph = new MultimapGraph<LengthData>();
    addPath(graph, new Point(10, 10), new Point(10, 50), new Point(10, 80),
        new Point(10, 100), new Point(50, 100), new Point(100, 100), new Point(
            100, 80), new Point(100, 10), new Point(50, 10), new Point(15, 10),
        new Point(10, 10));

    addPath(graph, new Point(100, 10), new Point(100, 5));

    addPath(graph, new Point(10, 50), new Point(15, 50), new Point(15, 15),
        new Point(50, 15), new Point(55, 15), new Point(55, 60), new Point(50,
            60));

    addPath(graph, new Point(15, 15), new Point(15, 10), new Point(15, 5),
        new Point(50, 5));

    addPath(graph, new Point(50, 100), new Point(50, 60), new Point(40, 60),
        new Point(40, 80), new Point(50, 80));

    addPath(graph, new Point(50, 60), new Point(50, 15), new Point(50, 10),
        new Point(50, 5), new Point(100, 5), new Point(120, 5), new Point(120,
            10), new Point(120, 20), new Point(115, 20), new Point(115, 40),
        new Point(120, 40), new Point(120, 80), new Point(100, 80), new Point(
            50, 80));

    addPath(graph, new Point(100, 10), new Point(120, 10));
    addPath(graph, new Point(120, 20), new Point(120, 40));
    addPath(graph, new Point(40, 80), new Point(10, 80));
    return graph;
  }
}