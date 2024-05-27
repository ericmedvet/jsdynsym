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

import io.github.ericmedvet.jnb.datastructure.DoubleRange;
import io.github.ericmedvet.jsdynsym.control.Environment;
import io.github.ericmedvet.jsdynsym.control.geometry.Point;
import io.github.ericmedvet.jsdynsym.control.geometry.Segment;
import io.github.ericmedvet.jsdynsym.control.navigation.PointNavigationEnvironment.State;
import io.github.ericmedvet.jsdynsym.core.numerical.NumericalDynamicalSystem;
import java.util.List;
import java.util.random.RandomGenerator;

public class PointNavigationEnvironment
    implements NumericalDynamicalSystem<State>, Environment<double[], double[], State> {

  public record Configuration(
      DoubleRange initialRobotXRange,
      DoubleRange initialRobotYRange,
      DoubleRange targetXRange,
      DoubleRange targetYRange,
      double robotMaxV,
      double collisionBounce,
      Arena arena,
      RandomGenerator randomGenerator)
      implements io.github.ericmedvet.jsdynsym.control.navigation.Configuration {}

  public record State(Configuration configuration, Point targetPosition, Point robotPosition, int nOfCollisions)
      implements io.github.ericmedvet.jsdynsym.control.navigation.State {}

  private final Configuration configuration;
  private State state;

  public PointNavigationEnvironment(Configuration configuration) {
    this.configuration = configuration;
    reset();
  }

  @Override
  public double[] defaultAgentAction() {
    return new double[nOfInputs()];
  }

  @Override
  public State getState() {
    return state;
  }

  @Override
  public void reset() {
    state = new State(
        configuration,
        new Point(
            configuration.targetXRange.denormalize(configuration.randomGenerator.nextDouble()),
            configuration.targetYRange.denormalize(configuration.randomGenerator.nextDouble())),
        new Point(
            configuration.initialRobotXRange.denormalize(configuration.randomGenerator.nextDouble()),
            configuration.initialRobotYRange.denormalize(configuration.randomGenerator.nextDouble())),
        0);
  }

  @Override
  public double[] step(double t, double[] action) {
    // check consistency
    if (action.length != nOfInputs()) {
      throw new IllegalArgumentException("Agent action has wrong number of elements: %d found, %d expected"
          .formatted(action.length, nOfInputs()));
    }
    // prepare
    List<Segment> segments = configuration.arena.segments();
    // apply action
    double v1 = DoubleRange.SYMMETRIC_UNIT.clip(action[0]) * configuration.robotMaxV;
    double v2 = DoubleRange.SYMMETRIC_UNIT.clip(action[1]) * configuration.robotMaxV;
    // compute new pose
    Point newRobotP = state.robotPosition.sum(new Point(v1, v2));
    Segment robotPath = new Segment(state.robotPosition, newRobotP);
    // check collision and update pose
    double endPositionT = segments.stream()
        .map(s -> collide(s, robotPath))
        .filter(p -> DoubleRange.UNIT.contains(p.x()) && DoubleRange.UNIT.contains(p.y()))
        .mapToDouble(Point::y)
        .min()
        .orElse(1d);
    if (endPositionT < 1) {
      double normalizedT = endPositionT - configuration.collisionBounce / robotPath.length();
      newRobotP = state.robotPosition.scale(1 - normalizedT).sum(newRobotP.scale(normalizedT));
    }
    state = new State(
        configuration, state.targetPosition, newRobotP, state.nOfCollisions + (endPositionT < 1 ? 1 : 0));
    // compute observation
    return new double[] {newRobotP.x(), newRobotP.y()};
  }

  private static Point collide(Segment s1, Segment s2) {
    Point v1 = s1.p2().diff(s1.p1());
    Point v2 = s2.p2().diff(s2.p1());
    double cramerDet = v1.y() * v2.x() - v1.x() * v2.y();
    if (cramerDet == 0) {
      if (Math.abs(s2.p2().diff(s1.p1()).direction())
          != Math.abs(s1.p2().diff(s1.p1()).direction())) {
        return new Point(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
      }
      if (v1.x() > 0 == s2.p2().x() > s1.p2().x()) {
        return new Point((s2.p2().x() - s1.p1().x()) / v1.x(), 1d);
      }
      return new Point(1d, (s1.p2().x() - s2.p1().x()) / v2.x());
    }
    return new Point(
        ((s2.p1().y() - s1.p1().y()) * v2.x() - (s2.p1().x() - s1.p1().x()) * v2.y()) / cramerDet,
        ((s2.p1().y() - s1.p1().y()) * v1.x() - (s2.p1().x() - s1.p1().x()) * v1.y()) / cramerDet);
  }

  @Override
  public int nOfInputs() {
    return 2;
  }

  @Override
  public int nOfOutputs() {
    return 2;
  }
}
