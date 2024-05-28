package io.github.ericmedvet.jsdynsym.control.navigation;

import io.github.ericmedvet.jsdynsym.control.geometry.Point;
import io.github.ericmedvet.jsdynsym.core.numerical.NumericalTimeInvariantStatelessSystem;
import io.github.ericmedvet.jviz.core.drawer.Drawer;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;

public class VectorFieldDrawer implements Drawer<NumericalTimeInvariantStatelessSystem> {

    private final Configuration configuration;
    private final Arena arena;

    public record Configuration(
            Color arrowColor,
            Color targetColor,
            Color segmentColor,
            Color infoColor,
            double arrowHeadSize,
            double arrowHeadWidth,
            float arrowThickness,
            float segmentThickness,
            double stepX,
            double stepY,
            double marginRate,
            boolean rescale) {

        public static final Configuration DEFAULT =
                new Configuration(Color.BLACK, Color.RED, Color.DARK_GRAY, Color.BLUE, .02, .01, .02f, .03f, .1, .1, .01, true);
    }

    public VectorFieldDrawer(Arena arena, Configuration configuration) {
        this.configuration = configuration;
        this.arena = arena;
    }

    @Override
    public void draw(Graphics2D g, NumericalTimeInvariantStatelessSystem dynSys) {
        if (dynSys.nOfInputs() != 2 || dynSys.nOfOutputs() != 2) {
            throw new IllegalArgumentException(
                    String.format("Requested 2 inputs and 2 outputs, found %d and %d", dynSys.nOfInputs(), dynSys.nOfOutputs())
            );
        }
        AffineTransform previousTransform = setTransform(g, arena);
        // draw arena
        g.setStroke(new BasicStroke(
                (float) (configuration.segmentThickness / g.getTransform().getScaleX())));
        g.setColor(configuration.segmentColor);
        arena.segments().forEach(s -> g.draw(new Line2D.Double(s.p1().x(), s.p1().y(), s.p2().x(), s.p2().y())));
        int stepsOnX = (int) Math.floor(g.getClipBounds().width / configuration.stepX);
        int stepsOnY = (int) Math.floor(g.getClipBounds().height / configuration.stepY);
        double topLeftX = g.getClipBounds().x + (1 - configuration.stepX * stepsOnX) / 2;
        double topLeftY = g.getClipBounds().y + (1 - configuration.stepY * stepsOnY) / 2;
        for (int i = 0; i < 2 * stepsOnX + 1; ++i) {
            for (int j = 0; j < 2 * stepsOnY + 1; ++j) {
                double[] input = new double[]{topLeftX + i * configuration.stepX, topLeftY + j * configuration.stepY};
                if (configuration.rescale) {
                    input[0] = 2 * input[0] - 1;
                    input[1] = 2 * input[1] - 1;
                }
                double[] output = dynSys.apply(input);
                drawArrow(g, new Point(input[0], input[1]), new Point(output[0], output[1]));
            }
        }
        g.setTransform(previousTransform);
    }

    private AffineTransform setTransform(Graphics2D g, Arena arena) {
        double cX = g.getClipBounds().x;
        double cY = g.getClipBounds().y;
        double cW = g.getClipBounds().width;
        double cH = g.getClipBounds().getHeight();
        // compute transformation
        double scale = Math.min(
                cW / 1.02 / arena.xExtent(),
                cH / 1.02 / arena.yExtent());
        AffineTransform previousTransform = g.getTransform();
        AffineTransform transform = AffineTransform.getScaleInstance(scale, scale);
        transform.translate(
                (cX / scale + cW / scale - arena.xExtent()) / 2d, (cY / scale + cH / scale - arena.yExtent()) / 2d);
        g.setTransform(transform);
        return previousTransform;
    }

    private void drawArrow(Graphics2D g, Point startingPoint, Point magnitude) {
        Point endingPoint = startingPoint.sum(magnitude);
        double headLength = configuration.arrowHeadSize < magnitude.magnitude() ?
                configuration.arrowHeadSize / magnitude.magnitude() :
                magnitude.magnitude();
        Point headBase = endingPoint.sum(magnitude.scale(-headLength));
        g.setStroke(new BasicStroke(configuration.arrowThickness));
        g.draw(new Line2D.Double(startingPoint.x(), startingPoint.y(), headBase.x(), headBase.y()));
        Point headHeight = endingPoint.diff(headBase).scale(2);
        Point vertex1 = headBase.sum(new Point(headHeight.y(), -headHeight.x()));
        Point vertex2 = headBase.sum(new Point(-headHeight.y(), headHeight.x()));
        g.draw(new Line2D.Double(vertex1.x(), vertex1.y(), endingPoint.x(), endingPoint.y()));
        g.draw(new Line2D.Double(vertex2.x(), vertex2.y(), endingPoint.x(), endingPoint.y()));
    }
}
