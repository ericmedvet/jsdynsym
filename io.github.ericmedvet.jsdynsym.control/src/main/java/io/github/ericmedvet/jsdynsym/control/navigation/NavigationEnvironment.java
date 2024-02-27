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
import io.github.ericmedvet.jsdynsym.control.geometry.Point;
import io.github.ericmedvet.jsdynsym.control.geometry.Segment;
import io.github.ericmedvet.jsdynsym.control.geometry.Semiline;
import io.github.ericmedvet.jsdynsym.control.navigation.NavigationEnvironment.State;
import io.github.ericmedvet.jsdynsym.core.numerical.NumericalDynamicalSystem;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;

public class NavigationEnvironment implements NumericalDynamicalSystem<State> {

  public record State(
      Arena arena,
      Point targetPosition,
      Point robotPosition,
      double robotDirection,
      double robotRadius,
      int nOfCollisions) {}

  private final DoubleRange initialRobotXRange;
  private final DoubleRange initialRobotYRange;
  private final DoubleRange initialRobotDirection;
  private final DoubleRange targetXRange;
  private final DoubleRange targetYRange;
  private final double robotRadius;
  private final double robotMaxV;
  private final DoubleRange sensorsAngleRange;
  private final int nOfSensors;
  private final double sensorRange;
  private final boolean senseTarget;
  private final Arena arena;
  private final RandomGenerator randomGenerator;

  private State state;

  public NavigationEnvironment(
      DoubleRange initialRobotXRange,
      DoubleRange initialRobotYRange,
      DoubleRange initialRobotDirection,
      DoubleRange targetXRange,
      DoubleRange targetYRange,
      double robotRadius,
      double robotMaxV,
      DoubleRange sensorsAngleRange,
      int nOfSensors,
      double sensorRange,
      boolean senseTarget,
      Arena arena,
      RandomGenerator randomGenerator) {
    this.initialRobotXRange = initialRobotXRange;
    this.initialRobotYRange = initialRobotYRange;
    this.initialRobotDirection = initialRobotDirection;
    this.targetXRange = targetXRange;
    this.targetYRange = targetYRange;
    this.robotRadius = robotRadius;
    this.robotMaxV = robotMaxV;
    this.sensorsAngleRange = sensorsAngleRange;
    this.nOfSensors = nOfSensors;
    this.sensorRange = sensorRange;
    this.senseTarget = senseTarget;
    this.arena = arena;
    this.randomGenerator = randomGenerator;
  }

  @Override
  public State getState() {
    return state;
  }

  @Override
  public void reset() {
    state = new State(
        arena,
        new Point(
            targetXRange.denormalize(randomGenerator.nextDouble()),
            targetYRange.denormalize(randomGenerator.nextDouble())),
        new Point(
            initialRobotXRange.denormalize(randomGenerator.nextDouble()),
            initialRobotYRange.denormalize(randomGenerator.nextDouble())),
        initialRobotDirection.denormalize(randomGenerator.nextDouble()),
        robotRadius,
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
    List<Segment> segments = arena.segments();
    DoubleRange sensorsRange = new DoubleRange(robotRadius, sensorRange);
    // apply action
    double v1 = DoubleRange.SYMMETRIC_UNIT.clip(action[0]) * robotMaxV;
    double v2 = DoubleRange.SYMMETRIC_UNIT.clip(action[1]) * robotMaxV;
    // compute new pose
    Point newRobotP = state.robotPosition.sum(new Point(state.robotDirection).scale((v1 + v2) / 2d));
    double deltaA = Math.asin((v2 - v1) / 2d / robotRadius);
    // check collision and update pose
    double minD = segments.stream().mapToDouble(newRobotP::distance).min().orElseThrow();
    state = new State(
        arena,
        state.targetPosition,
        (minD > robotRadius) ? newRobotP : state.robotPosition,
        state.robotDirection + deltaA,
        robotRadius,
        state.nOfCollisions + ((minD > robotRadius) ? 0 : 1));
    // compute observation
    double[] sInputs = sensorsAngleRange
        .delta(state.robotDirection)
        .points(nOfSensors - 1)
        .map(a -> {
          Semiline sl = new Semiline(state.robotPosition, a);
          double d = segments.stream()
              .map(sl::interception)
              .filter(Optional::isPresent)
              .mapToDouble(op -> op.orElseThrow().distance(state.robotPosition))
              .min()
              .orElse(Double.POSITIVE_INFINITY);
          return sensorsRange.normalize(d);
        })
        .toArray();
    double[] observation = senseTarget ? new double[nOfSensors + 2] : sInputs;
    if (senseTarget) {
      System.arraycopy(sInputs, 0, observation, 2, sInputs.length);
      observation[0] = sensorsRange.normalize(state.robotPosition.distance(state.targetPosition));
      observation[1] = state.targetPosition.diff(state.robotPosition).direction() - state.robotDirection;
      if (observation[1] > Math.PI) {
        observation[1] = observation[1] - Math.PI;
      }
      if (observation[1] < -Math.PI) {
        observation[1] = observation[1] + Math.PI;
      }
      observation[1] = observation[1] / Math.PI;
    }
    return observation;
  }

  @Override
  public int nOfInputs() {
    return 2;
  }

  @Override
  public int nOfOutputs() {
    return nOfSensors + (senseTarget ? 2 : 0);
  }
}
