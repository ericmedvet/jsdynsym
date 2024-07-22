/*-
 * ========================LICENSE_START=================================
 * jsdynsym-control
 * %%
 * Copyright (C) 2023 - 2024 Eric Medvet
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package io.github.ericmedvet.jsdynsym.control.navigation;

import io.github.ericmedvet.jsdynsym.control.geometry.Point;
import io.github.ericmedvet.jsdynsym.core.numerical.NumericalTimeInvariantStatelessSystem;
import io.github.ericmedvet.jviz.core.drawer.Drawer;
import io.github.ericmedvet.jviz.core.util.GraphicsUtils;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;

public class VectorFieldDrawer implements Drawer<NumericalTimeInvariantStatelessSystem> {

  private static final int DEFAULT_SIDE_LENGTH = 500;

  private final Configuration configuration;
  private final Arena arena;

  public record Configuration(
      Color arrowColor,
      Color segmentColor,
      double arrowHeadSize,
      double arrowHeadWidth,
      float arrowThickness,
      float segmentThickness,
      double step,
      double marginRate,
      boolean rescale) {

    public static final Configuration DEFAULT =
        new Configuration(Color.RED, Color.DARK_GRAY, .02, .01, .002f, 3, .05, .01, true);
  }

  public VectorFieldDrawer(Arena arena, Configuration configuration) {
    this.configuration = configuration;
    this.arena = arena;
  }

  @Override
  public ImageInfo imageInfo(NumericalTimeInvariantStatelessSystem ntiss) {
    return new ImageInfo(
        (int)
            (arena.xExtent() > arena.yExtent()
                ? DEFAULT_SIDE_LENGTH * arena.xExtent() / arena.yExtent()
                : DEFAULT_SIDE_LENGTH),
        (int)
            (arena.xExtent() > arena.yExtent()
                ? DEFAULT_SIDE_LENGTH
                : DEFAULT_SIDE_LENGTH * arena.yExtent() / arena.xExtent()));
  }

  @Override
  public void draw(Graphics2D g, NumericalTimeInvariantStatelessSystem dynSys) {
    if (dynSys.nOfInputs() != 2 || dynSys.nOfOutputs() != 2) {
      throw new IllegalArgumentException(String.format(
          "Requested 2 inputs and 2 outputs, found %d and %d", dynSys.nOfInputs(), dynSys.nOfOutputs()));
    }
    AffineTransform previousTransform = setTransform(g, arena);
    // draw arena
    g.setStroke(new BasicStroke(
        (float) (configuration.segmentThickness / g.getTransform().getScaleX())));
    g.setColor(configuration.segmentColor);
    arena.segments().forEach(s -> g.draw(new Line2D.Double(s.p1().x(), s.p1().y(), s.p2().x(), s.p2().y())));
    int stepsOnX = (int) Math.floor(1d / configuration.step);
    int stepsOnY = (int) Math.floor(1d / configuration.step);
    double topLeftX = configuration.marginRate + (1d - configuration.step * (stepsOnX - 1)) / 2;
    double topLeftY = configuration.marginRate + (1d - configuration.step * (stepsOnY - 1)) / 2;
    double max = 0d;
    for (int i = 0; i < stepsOnX; ++i) {
      for (int j = 0; j < stepsOnY; ++j) {
        double[] input = new double[] {topLeftX + i * configuration.step, topLeftY + j * configuration.step};
        if (configuration.rescale) {
          input[0] = 2 * input[0] - 1;
          input[1] = 2 * input[1] - 1;
        }
        double[] output = dynSys.apply(input);
        max = Math.max(max, Math.sqrt(output[0] * output[0] + output[1] * output[1]));
      }
    }
    for (int i = 0; i < stepsOnX; ++i) {
      for (int j = 0; j < stepsOnY; ++j) {
        double[] input = new double[] {topLeftX + i * configuration.step, topLeftY + j * configuration.step};
        Point inputPoint = new Point(input[0], input[1]);
        // rescale input
        if (configuration.rescale) {
          input[0] = 2 * input[0] - 1;
          input[1] = 2 * input[1] - 1;
        }
        double[] output = dynSys.apply(input);
        drawArrow(g, inputPoint, new Point(output[0] / max, output[1] / max));
      }
    }
    g.setTransform(previousTransform);
  }

  private AffineTransform setTransform(Graphics2D g, Arena arena) {
    double cX = g.getClipBounds().x;
    double cY = g.getClipBounds().y;
    double cW = g.getClipBounds().width;
    double cH = g.getClipBounds().height;
    // compute transformation
    double scale = Math.min(
        cW / (1 + 2 * configuration.marginRate) / arena.xExtent(),
        cH / (1 + 2 * configuration.marginRate) / arena.yExtent());
    AffineTransform previousTransform = g.getTransform();
    AffineTransform transform = AffineTransform.getScaleInstance(scale, scale);
    transform.translate(
        (cX / scale + cW / scale - arena.xExtent()) / 2d, (cY / scale + cH / scale - arena.yExtent()) / 2d);
    g.setTransform(transform);
    return previousTransform;
  }

  private void drawArrow(Graphics2D g, Point startingPoint, Point extension) {
    Point endingPoint = startingPoint.sum(extension.scale(configuration.step));
    double headLength = Math.min(configuration.arrowHeadSize, extension.magnitude());
    Point headBase = endingPoint.sum(extension.scale(-headLength));
    g.setStroke(new BasicStroke(configuration.arrowThickness));
    g.setColor(GraphicsUtils.alphaed(configuration.arrowColor, extension.magnitude()));
    g.draw(new Line2D.Double(startingPoint.x(), startingPoint.y(), endingPoint.x(), endingPoint.y()));
    Point headHeight = endingPoint.diff(headBase).scale(.2);
    Point vertex1 = headBase.sum(new Point(headHeight.y(), -headHeight.x()));
    Point vertex2 = headBase.sum(new Point(-headHeight.y(), headHeight.x()));
    g.draw(new Line2D.Double(vertex1.x(), vertex1.y(), endingPoint.x(), endingPoint.y()));
    g.draw(new Line2D.Double(vertex2.x(), vertex2.y(), endingPoint.x(), endingPoint.y()));
  }
}
