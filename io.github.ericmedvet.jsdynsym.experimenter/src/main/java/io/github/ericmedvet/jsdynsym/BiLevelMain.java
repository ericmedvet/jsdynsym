/*-
 * ========================LICENSE_START=================================
 * jsdynsym-experimenter
 * %%
 * Copyright (C) 2023 - 2026 Eric Medvet
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
package io.github.ericmedvet.jsdynsym;

import io.github.ericmedvet.jnb.core.NamedBuilder;
import io.github.ericmedvet.jnb.datastructure.DoubleRange;
import io.github.ericmedvet.jsdynsym.control.Environment;
import io.github.ericmedvet.jsdynsym.control.Simulation;
import io.github.ericmedvet.jsdynsym.control.SingleAgentTask;
import io.github.ericmedvet.jsdynsym.control.navigation.NavigationDrawer;
import io.github.ericmedvet.jsdynsym.control.navigation.NavigationEnvironment;
import io.github.ericmedvet.jsdynsym.core.StatelessSystem;
import io.github.ericmedvet.jsdynsym.core.numerical.BiLevelNumericalDynamicalSystem;
import io.github.ericmedvet.jsdynsym.core.numerical.NumericalDynamicalSystem;
import io.github.ericmedvet.jsdynsym.core.numerical.ann.MultiLayerPerceptron;
import java.util.Random;

public class BiLevelMain {
  public static void main(String[] args) {
    //    MultivariateRealFunction sumF = UnivariateRealFunction.from(
    //        inputs -> inputs[0] + inputs[1],
    //        2
    //    );
    //    MultivariateRealFunction productF = UnivariateRealFunction.from(
    //        inputs -> inputs[0] * inputs[1],
    //        2
    //    );
    //    BiLevelNumericalDynamicalSystem<StatelessSystem.State, StatelessSystem.State> bilevelNDS = new BiLevelNumericalDynamicalSystem<>(
    //        sumF,
    //        productF,
    //        3
    //    );
    //    System.out.printf("nIn=%d nOut=%d%n", bilevelNDS.nOfInputs(), bilevelNDS.nOfOutputs());
    //    IntStream.range(0, 10)
    //        .forEach(
    //            t -> System.out.printf(
    //                "t=%.0f in=%s out=%s state=%s%n",
    //                (double) t,
    //                Arrays.toString(MLPUtils.nCopies(bilevelNDS.nOfInputs(), t)),
    //                Arrays.toString(bilevelNDS.step(t, MLPUtils.nCopies(bilevelNDS.nOfInputs(), t))),
    //                bilevelNDS.getState()
    //            )
    //        );

    // 1) create navigation environment
    NamedBuilder<?> nb = NamedBuilder.fromDiscovery();
    @SuppressWarnings("unchecked") Environment<double[], double[], NavigationEnvironment.State, NumericalDynamicalSystem<?>> environment = (Environment<double[], double[], NavigationEnvironment.State, NumericalDynamicalSystem<?>>) nb
        .build(
            """
                ds.e.navigation(
                  arena = ds.arena.prepared(
                    name = u_barrier;
                    initialRobotXRange = m.range(min=0.5;max=0.5);
                    initialRobotYRange = m.range(min=0.8;max=0.8)
                  );
                  relativeV = true;
                  robotMaxV = 0.1;
                  robotRadius = 0.01;
                  nOfSensors = 5
                )
                """
        );

    // 2) create MLP for policies
    MultiLayerPerceptron highMlp = new MultiLayerPerceptron(
        MultiLayerPerceptron.ActivationFunction.TANH,
        7,
        new int[]{},
        1
    );
    highMlp.randomize(new Random(), DoubleRange.SYMMETRIC_UNIT);

    MultiLayerPerceptron lowMlp = new MultiLayerPerceptron(
        MultiLayerPerceptron.ActivationFunction.TANH,
        6,
        new int[]{},
        2
    );
    lowMlp.randomize(new Random(), DoubleRange.SYMMETRIC_UNIT);

    // 3) create agent
    BiLevelNumericalDynamicalSystem<StatelessSystem.State, StatelessSystem.State> agent = new BiLevelNumericalDynamicalSystem<>(
        highMlp,
        lowMlp,
        5,
        0,
        2,
        false
    );

    // 4) task and simulation
    SingleAgentTask<NumericalDynamicalSystem<?>, double[], double[], ?, NavigationEnvironment.State> task = SingleAgentTask
        .fromEnvironment(() -> environment, s -> false, true);
    Simulation.Outcome<SingleAgentTask.Step<double[], double[], NavigationEnvironment.State>> outcome = task.simulate(
        agent,
        0.1,
        new DoubleRange(0, 100)
    );

    // 5) display
    NavigationDrawer d = new NavigationDrawer(NavigationDrawer.Configuration.DEFAULT);
    d.show(outcome);
  }
}
