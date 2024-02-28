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
import io.github.ericmedvet.jsdynsym.control.geometry.Semiline;
import io.github.ericmedvet.jsdynsym.control.navigation.NavigationEnvironment.State;
import io.github.ericmedvet.jsdynsym.core.numerical.NumericalDynamicalSystem;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;

public class NavigationEnvironment implements NumericalDynamicalSystem<State>, Environment<double[], double[], State> {

  public record Configuration(
      DoubleRange initialRobotXRange,
      DoubleRange initialRobotYRange,
      DoubleRange initialRobotDirectionRange,
      DoubleRange targetXRange,
      DoubleRange targetYRange,
      double robotRadius,
      double robotMaxV,
      DoubleRange sensorsAngleRange,
      int nOfSensors,
      double sensorRange,
      boolean senseTarget,
      Arena arena,
      RandomGenerator randomGenerator) {}

  public record State(
      Configuration configuration,
      Point targetPosition,
      Point robotPosition,
      double robotDirection,
      int nOfCollisions) {}

  private final Configuration configuration;
  private State state;

  public NavigationEnvironment(Configuration configuration) {
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
        configuration.initialRobotDirectionRange.denormalize(configuration.randomGenerator.nextDouble()),
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
    DoubleRange sensorsRange = new DoubleRange(configuration.robotRadius, configuration.sensorRange);
    // apply action
    double v1 = DoubleRange.SYMMETRIC_UNIT.clip(action[0]) * configuration.robotMaxV;
    double v2 = DoubleRange.SYMMETRIC_UNIT.clip(action[1]) * configuration.robotMaxV;
    // compute new pose
    Point newRobotP = state.robotPosition.sum(new Point(state.robotDirection).scale((v1 + v2) / 2d));
    double deltaA = Math.asin((v2 - v1) / 2d / configuration.robotRadius);
    // check collision and update pose
    double minD = segments.stream().mapToDouble(newRobotP::distance).min().orElseThrow();
    state = new State(
        configuration,
        state.targetPosition,
        (minD > configuration.robotRadius) ? newRobotP : state.robotPosition,
        state.robotDirection + deltaA,
        state.nOfCollisions + ((minD > configuration.robotRadius) ? 0 : 1));
    // compute observation
    double[] sInputs = configuration
        .sensorsAngleRange
        .delta(state.robotDirection)
        .points(configuration.nOfSensors - 1)
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
    double[] observation = configuration.senseTarget ? new double[configuration.nOfSensors + 2] : sInputs;
    if (configuration.senseTarget) {
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
    return configuration.nOfSensors + (configuration.senseTarget ? 2 : 0);
  }
}
