/*-
 * ========================LICENSE_START=================================
 * jsdynsym-control
 * %%
 * Copyright (C) 2023 - 2025 Eric Medvet
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

import io.github.ericmedvet.jsdynsym.control.Simulation;
import io.github.ericmedvet.jsdynsym.control.SimulationOutcomeDrawer;
import io.github.ericmedvet.jsdynsym.control.SingleAgentTask;
import io.github.ericmedvet.jviz.core.geometry.Point;
import io.github.ericmedvet.jviz.core.util.GraphicsUtils;
import java.awt.*;
import java.awt.geom.*;
import java.util.SortedMap;

public class PointNavigationDrawer implements SimulationOutcomeDrawer<SingleAgentTask.Step<double[], double[], PointNavigationEnvironment.State>> {

  private static final int DEFAULT_SIDE_LENGTH = 500;

  private final Configuration configuration;
  private final ArenaDrawer arenaDrawer;

  public PointNavigationDrawer(Configuration configuration) {
    this.configuration = configuration;
    arenaDrawer = new ArenaDrawer(configuration.arenaConfiguration);
  }

  public record Configuration(
      Color robotColor,
      Color infoColor,
      double robotThickness,
      double robotFillAlpha,
      double landmarkThickness,
      double landmarkSize,
      double robotDotSize,
      double trajectoryThickness,
      double sensorsFillAlpha,
      NavigationDrawer.Configuration.IOType ioType,
      ArenaDrawer.Configuration arenaConfiguration
  ) {

    public static final Configuration DEFAULT = new Configuration(
        NavigationDrawer.Configuration.DEFAULT.robotColor(),
        NavigationDrawer.Configuration.DEFAULT.infoColor(),
        NavigationDrawer.Configuration.DEFAULT.robotThickness(),
        NavigationDrawer.Configuration.DEFAULT.robotFillAlpha(),
        NavigationDrawer.Configuration.DEFAULT.landmarkThickness(),
        NavigationDrawer.Configuration.DEFAULT.landmarkSize(),
        5,
        NavigationDrawer.Configuration.DEFAULT.trajectoryThickness(),
        NavigationDrawer.Configuration.DEFAULT.sensorsFillAlpha(),
        NavigationDrawer.Configuration.DEFAULT.ioType(),
        NavigationDrawer.Configuration.DEFAULT.arenaConfiguration()
    );
  }

  private void drawRobot(Graphics2D g, Color c, double alpha, double th, Point p) {
    g.setStroke(new BasicStroke((float) th));
    Shape shape = new Ellipse2D.Double(
        p.x() - configuration.robotDotSize,
        p.y() - configuration.robotDotSize,
        2d * configuration.robotDotSize,
        2d * configuration.robotDotSize
    );
    g.setColor(GraphicsUtils.alphaed(c, alpha));
    g.fill(shape);
    g.setColor(c);
    g.draw(shape);
  }

  @Override
  public void drawSingle(
      Graphics2D g,
      double t,
      SingleAgentTask.Step<double[], double[], PointNavigationEnvironment.State> step
  ) {
    Arena arena = step.state().configuration().arena();
    // draw arena
    arenaDrawer.draw(g, arena);
    // set transform
    AffineTransform previousTransform = arenaDrawer.setTransform(g, arena);
    // draw robot
    drawRobot(
        g,
        configuration.robotColor,
        configuration.robotFillAlpha,
        configuration.robotThickness / g.getTransform().getScaleX(),
        step.state().robotPosition()
    );
    // draw target
    NavigationDrawer.drawLandmark(
        g,
        configuration.arenaConfiguration.targetColor(),
        configuration.landmarkThickness / g.getTransform().getScaleX(),
        configuration.landmarkSize / g.getTransform().getScaleX(),
        step.state().targetPosition()
    );
    // restore transformation
    g.setTransform(previousTransform);
    // draw info
    g.setStroke(new BasicStroke(1f));
    g.setColor(configuration.infoColor);
    g.drawString("%.2fs".formatted(t), 5, 5 + g.getFontMetrics().getHeight());
    // draw input and output
    if (!configuration.ioType.equals(NavigationDrawer.Configuration.IOType.OFF)) {
      NavigationDrawer.drawIO(
          g,
          configuration.infoColor,
          configuration.sensorsFillAlpha,
          configuration.ioType,
          step.observation(),
          step.action(),
          true,
          step.state().configuration().rescaleInput()
      );
    }
  }

  @Override
  public void drawAll(
      Graphics2D g,
      SortedMap<Double, SingleAgentTask.Step<double[], double[], PointNavigationEnvironment.State>> map
  ) {
    Arena arena = map.values().iterator().next().state().configuration().arena();
    // set transform
    AffineTransform previousTransform = arenaDrawer.setTransform(g, arena);
    // draw robot and trajectory
    NavigationDrawer.drawTrajectory(
        g,
        configuration.robotColor,
        configuration.trajectoryThickness / g.getTransform().getScaleX(),
        map.values().stream().map(s -> s.state().robotPosition()).toList()
    );
    // draw target and trajectory
    NavigationDrawer.drawTrajectory(
        g,
        configuration.arenaConfiguration.targetColor(),
        configuration.trajectoryThickness / g.getTransform().getScaleX(),
        map.values().stream().map(s -> s.state().targetPosition()).toList()
    );
    // restore transformation
    g.setTransform(previousTransform);
  }

  @Override
  public ImageInfo imageInfo(
      Simulation.Outcome<SingleAgentTask.Step<double[], double[], PointNavigationEnvironment.State>> o
  ) {
    Arena arena = o.snapshots()
        .get(o.snapshots().firstKey())
        .state()
        .configuration()
        .arena();
    return arenaDrawer.imageInfo(arena);
  }

}