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

import io.github.ericmedvet.jnb.datastructure.DoubleRange;
import io.github.ericmedvet.jsdynsym.control.Environment;
import io.github.ericmedvet.jsdynsym.control.navigation.NavigationEnvironment.Configuration.TargetSensing;
import io.github.ericmedvet.jsdynsym.control.navigation.NavigationEnvironment.State;
import io.github.ericmedvet.jsdynsym.core.numerical.NumericalDynamicalSystem;
import io.github.ericmedvet.jviz.core.geometry.Point;
import io.github.ericmedvet.jviz.core.geometry.Segment;
import io.github.ericmedvet.jviz.core.geometry.Semiline;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;

public class NavigationEnvironment<CS> implements NumericalDynamicalSystem<State>, Environment<double[], double[], State, NumericalDynamicalSystem<CS>> {

  @Override
  public int nOfOutputs() {
    return configuration.sensorAngles.size() + (!configuration.targetSensing.equals(TargetSensing.NONE) ? 2 : 0);
  }

  @Override
  public double[] step(double t, double[] action) {
    // check consistency
    if (action.length != nOfInputs()) {
      throw new IllegalArgumentException(
          "Agent action has wrong number of elements: %d found, %d expected"
              .formatted(action.length, nOfInputs())
      );
    }
    // prepare
    List<Segment> segments = configuration.arena.segments();
    DoubleRange sensorsRange = new DoubleRange(
        configuration.robotRadius,
        configuration.sensorRange
    );
    double dT = t - state.t;
    // apply action
    double maxV = configuration.robotMaxV * (configuration.relativeSpeed ? dT : 1d);
    double v1 = DoubleRange.SYMMETRIC_UNIT.clip(action[0]) * maxV;
    double v2 = DoubleRange.SYMMETRIC_UNIT.clip(action[1]) * maxV;
    v1 = Double.isNaN(v1) ? 0 : v1;
    v2 = Double.isNaN(v2) ? 0 : v2;
    // compute new pose
    Point newRobotP = state.robotPosition.sum(
        new Point(state.robotDirection).scale((v1 + v2) / 2d)
    );
    double deltaA = Math.asin(
        ((v2 - v1) / 2d % configuration.robotRadius) / configuration.robotRadius
    );
    // check collision and update pose
    double minD = segments.stream()
        .mapToDouble(newRobotP::distance)
        .min()
        .orElseThrow();
    boolean collision = minD <= configuration.robotRadius;
    if (!collision && minD < 5d * maxV) { // the comparison with minD is an optimization
      Segment robotPath = new Segment(state.robotPosition, newRobotP);
      collision = segments.stream().anyMatch(os -> os.intersect(robotPath));
    }
    state = new State(
        t,
        state.t,
        configuration,
        state.targetPosition,
        collision ? state.robotPosition : newRobotP,
        state.robotPosition,
        state.robotDirection + deltaA,
        state.robotDirection,
        collision
    );
    // compute observation
    double[] sInputs = configuration.sensorAngles.stream()
        .mapToDouble(a -> {
          Semiline sl = new Semiline(state.robotPosition, a + state.robotDirection);
          return segments.stream()
              .map(sl::intersection)
              .filter(Optional::isPresent)
              .mapToDouble(
                  op -> sensorsRange.normalize(op.orElseThrow().distance(state.robotPosition))
              )
              .min()
              .orElse(Double.POSITIVE_INFINITY);
        })
        .toArray();
    double[] observation = !configuration.targetSensing.equals(
        TargetSensing.NONE
    ) ? new double[configuration.sensorAngles.size() + 2] : sInputs;
    if (!configuration.targetSensing.equals(TargetSensing.NONE)) {
      System.arraycopy(sInputs, 0, observation, 2, sInputs.length);
      double d = state.robotPosition.distance(state.targetPosition);
      double a = (state.targetPosition.diff(state.robotPosition).direction() - state.robotDirection) % (2d * Math.PI);
      observation[0] = switch (configuration.targetSensing) {
        case LIMITED -> sensorsRange.normalize(d);
        case UNLIMITED -> new DoubleRange(
            0,
            Math.sqrt(
                configuration.arena().xExtent() * configuration.arena().xExtent() + configuration.arena()
                    .yExtent() * configuration.arena().yExtent()
            )
        ).normalize(d);
        default -> 0d; // it is not actually reachable
      };
      observation[1] = new DoubleRange(-2d * Math.PI, 2d * Math.PI).normalize(a);
    }
    if (configuration.rescaleInput) {
      observation = Arrays.stream(observation)
          .map(DoubleRange.SYMMETRIC_UNIT::denormalize)
          .toArray();
    }
    return observation;
  }

  private final Configuration configuration;
  private State state;

  public NavigationEnvironment(Configuration configuration) {
    this.configuration = configuration;
    reset();
  }

  @Override
  public NumericalDynamicalSystem<CS> exampleAgent() {
    return NumericalDynamicalSystem.from(nOfOutputs(), nOfInputs());
  }

  @Override
  public double[] defaultObservation() {
    return new double[nOfOutputs()];
  }

  @Override
  public State getState() {
    return state;
  }

  @Override
  public void reset() {
    Point inititalRobotPosition = new Point(
        configuration.arena.startXRange()
            .denormalize(configuration.randomGenerator.nextDouble()),
        configuration.arena.startYRange()
            .denormalize(configuration.randomGenerator.nextDouble())
    );
    double initialRobotDirection = configuration.initialRobotDirectionRange.denormalize(
        configuration.randomGenerator.nextDouble()
    );
    state = new State(
        0d,
        0d,
        configuration,
        new Point(
            configuration.arena.targetXRange()
                .denormalize(configuration.randomGenerator.nextDouble()),
            configuration.arena.targetYRange()
                .denormalize(configuration.randomGenerator.nextDouble())
        ),
        inititalRobotPosition,
        inititalRobotPosition,
        initialRobotDirection,
        initialRobotDirection,
        false
    );
  }

  public record Configuration(
      DoubleRange initialRobotDirectionRange,
      double robotRadius,
      double robotMaxV,
      List<Double> sensorAngles,
      double sensorRange,
      TargetSensing targetSensing,
      NavigationArena arena,
      boolean rescaleInput,
      boolean relativeSpeed,
      RandomGenerator randomGenerator
  ) implements io.github.ericmedvet.jsdynsym.control.navigation.Configuration {

    public enum TargetSensing { NONE, LIMITED, UNLIMITED }
  }

  @Override
  public int nOfInputs() {
    return 2;
  }

  public record State(
      double t,
      double previousT,
      Configuration configuration,
      Point targetPosition,
      Point robotPosition,
      Point robotPreviousPosition,
      double robotDirection,
      double robotPreviousDirection,
      boolean hasCollided
  ) implements io.github.ericmedvet.jsdynsym.control.navigation.State {

    public enum SymbolicAction {
      STOP("o"), ROTATE_LEFT("↶"), ROTATE_RIGHT("↷"), FORWARD("↑"), FORWARD_LEFT(
          "↖"
      ), FORWARD_RIGHT("↗"), BACKWARD(
          "↓"
      ), BACKWARD_LEFT("↙"), BACKWARD_RIGHT("↘");

      private final String s;

      SymbolicAction(String s) {
        this.s = s;
      }

      @Override
      public String toString() {
        return s;
      }
    }

    public SymbolicAction symbolicAction(double movementThresholdRate, double turnThreshold) {
      if (previousT == t) {
        return SymbolicAction.STOP;
      }
      double maxV = configuration.robotMaxV() * (configuration.relativeSpeed() ? (t - previousT) : 1d);
      double vT = maxV * movementThresholdRate;
      Point dP = robotPosition.diff(robotPreviousPosition);
      double sign = Math.min(
          dP.direction() - robotDirection,
          2d * Math.PI - dP.direction() - robotDirection
      ) < Math.PI / 2d ? 1d : -1d;
      double dV = dP.magnitude() * sign;
      double dA = (robotDirection - robotPreviousDirection - 2d * Math.PI) % Math.PI;
      dA = dA > Math.PI ? (dA - 2d * Math.PI) : dA;
      double turnR = Math.abs(dV / Math.sin(dA));
      if (dV > vT) {
        if (turnR < turnThreshold && dA < 0) {
          return SymbolicAction.FORWARD_RIGHT;
        }
        if (turnR < turnThreshold && dA > 0) {
          return SymbolicAction.FORWARD_LEFT;
        }
        return SymbolicAction.FORWARD;
      }
      if (dV < -vT) {
        if (turnR < turnThreshold && dA < 0) {
          return SymbolicAction.BACKWARD_LEFT;
        }
        if (turnR < turnThreshold && dA > 0) {
          return SymbolicAction.BACKWARD_RIGHT;
        }
        return SymbolicAction.BACKWARD;
      }
      if (turnR < turnThreshold && dA < 0) {
        return SymbolicAction.ROTATE_RIGHT;
      }
      if (turnR < turnThreshold && dA > 0) {
        return SymbolicAction.ROTATE_RIGHT;
      }
      return SymbolicAction.STOP;
    }
  }
}