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
import io.github.ericmedvet.jnb.datastructure.Pair;
import io.github.ericmedvet.jsdynsym.control.Simulation;
import io.github.ericmedvet.jsdynsym.control.SimulationWithExample;
import io.github.ericmedvet.jsdynsym.control.SingleAgentTask;
import io.github.ericmedvet.jsdynsym.core.numerical.NumericalDynamicalSystem;
import io.github.ericmedvet.jsdynsym.core.numerical.NumericalStatelessSystem;
import java.util.Collections;
import java.util.List;

/**
 * @author "Eric Medvet" on 2024/07/24 for jgea
 */
public class VariableSensorPositionsNavigation
    implements SimulationWithExample<
        Pair<List<Double>, NumericalDynamicalSystem<?>>,
        SingleAgentTask.Step<double[], double[], NavigationEnvironment.State>,
        Simulation.Outcome<SingleAgentTask.Step<double[], double[], NavigationEnvironment.State>>> {
  private final NavigationEnvironment.Configuration configuration;
  private final int nOfSensors;
  private final DoubleRange tRange;
  private final double dT;

  public VariableSensorPositionsNavigation(
      NavigationEnvironment.Configuration configuration, int nOfSensors, DoubleRange tRange, double dT) {
    this.configuration = configuration;
    this.nOfSensors = nOfSensors;
    this.tRange = tRange;
    this.dT = dT;
  }

  @Override
  public Outcome<SingleAgentTask.Step<double[], double[], NavigationEnvironment.State>> simulate(
      Pair<List<Double>, NumericalDynamicalSystem<?>> pair) {
    if (pair.first().size() != nOfSensors) {
      throw new IllegalArgumentException("Wrong number of sensor angles: %d found, %d expected"
          .formatted(pair.first().size(), nOfSensors));
    }
    return SingleAgentTask.fromEnvironment(new NavigationEnvironment(configuration(pair.first())), tRange, dT)
        .simulate(pair.second());
  }

  private NavigationEnvironment.Configuration configuration(List<Double> angles) {
    return new NavigationEnvironment.Configuration(
        configuration.initialRobotXRange(),
        configuration.initialRobotYRange(),
        configuration.initialRobotDirectionRange(),
        configuration.targetXRange(),
        configuration.targetYRange(),
        configuration.robotRadius(),
        configuration.robotMaxV(),
        angles,
        configuration.sensorRange(),
        configuration.senseTarget(),
        configuration.arena(),
        configuration.rescaleInput(),
        configuration.randomGenerator());
  }

  @Override
  public Pair<List<Double>, NumericalDynamicalSystem<?>> example() {
    List<Double> angles = Collections.nCopies(nOfSensors, 0d);
    NavigationEnvironment env = new NavigationEnvironment(configuration(angles));
    return new Pair<>(
        angles,
        NumericalStatelessSystem.from(
            env.nOfOutputs(), env.nOfInputs(), (t, in) -> new double[env.nOfOutputs()]));
  }
}
