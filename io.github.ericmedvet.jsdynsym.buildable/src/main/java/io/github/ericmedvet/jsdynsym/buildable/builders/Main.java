/*-
 * ========================LICENSE_START=================================
 * jsdynsym-buildable
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
/*
 * Copyright 2024 eric
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.ericmedvet.jsdynsym.buildable.builders;

import io.github.ericmedvet.jnb.core.NamedBuilder;
import io.github.ericmedvet.jnb.datastructure.DoubleRange;
import io.github.ericmedvet.jsdynsym.control.Simulation;
import io.github.ericmedvet.jsdynsym.control.SingleAgentTask;
import io.github.ericmedvet.jsdynsym.control.navigation.NavigationDrawer;
import io.github.ericmedvet.jsdynsym.control.navigation.NavigationEnvironment;
import io.github.ericmedvet.jsdynsym.core.DynamicalSystem;
import io.github.ericmedvet.jsdynsym.core.numerical.ann.MultiLayerPerceptron;
import java.io.IOException;
import java.util.Random;
import java.util.stream.IntStream;

public class Main {
  public static void main(String[] args) throws IOException {
    NamedBuilder<?> nb = NamedBuilder.fromDiscovery();
    NavigationEnvironment environment = (NavigationEnvironment) nb.build("ds.e.navigation()");
    @SuppressWarnings("unchecked")
    MultiLayerPerceptron mlp = ((NumericalDynamicalSystems.Builder<MultiLayerPerceptron, ?>)
            nb.build("ds.num.mlp()"))
        .apply(
            IntStream.range(0, environment.nOfOutputs())
                .mapToObj("x%02d"::formatted)
                .toList(),
            IntStream.range(0, environment.nOfInputs())
                .mapToObj("y%02d"::formatted)
                .toList());
    mlp.randomize(new Random(), DoubleRange.SYMMETRIC_UNIT);
    SingleAgentTask<DynamicalSystem<double[], double[], ?>, double[], double[], NavigationEnvironment.State> task =
        SingleAgentTask.fromEnvironment(environment, new double[2], new DoubleRange(0, 60), 0.1);
    Simulation.Outcome<SingleAgentTask.Step<double[], double[], NavigationEnvironment.State>> outcome =
        task.simulate(mlp);
    NavigationDrawer d = new NavigationDrawer(NavigationDrawer.Configuration.DEFAULT);
    d.showImage(500, 500, outcome.snapshots());
  }
}
