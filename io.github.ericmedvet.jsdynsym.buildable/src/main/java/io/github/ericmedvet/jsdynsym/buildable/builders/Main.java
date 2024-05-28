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

package io.github.ericmedvet.jsdynsym.buildable.builders;

import io.github.ericmedvet.jnb.core.NamedBuilder;
import io.github.ericmedvet.jnb.datastructure.DoubleRange;
import io.github.ericmedvet.jsdynsym.control.Simulation;
import io.github.ericmedvet.jsdynsym.control.SingleAgentTask;
import io.github.ericmedvet.jsdynsym.control.navigation.NavigationDrawer;
import io.github.ericmedvet.jsdynsym.control.navigation.NavigationEnvironment;
import io.github.ericmedvet.jsdynsym.control.navigation.PointNavigationDrawer;
import io.github.ericmedvet.jsdynsym.control.navigation.PointNavigationEnvironment;
import io.github.ericmedvet.jsdynsym.core.DynamicalSystem;
import io.github.ericmedvet.jsdynsym.core.numerical.NumericalStatelessSystem;
import io.github.ericmedvet.jsdynsym.core.numerical.ann.MultiLayerPerceptron;
import io.github.ericmedvet.jviz.core.drawer.ImageBuilder;
import java.io.IOException;
import java.util.Random;

public class Main {
  public static void main(String[] args) throws IOException {
    pointNavigation();
  }

  @SuppressWarnings("unchecked")
  public static void pointNavigation() throws IOException {
    NamedBuilder<?> nb = NamedBuilder.fromDiscovery();
    PointNavigationEnvironment environment =
        (PointNavigationEnvironment) nb.build("ds.e.pointNavigation(arena = E_MAZE)");
    /*MultiLayerPerceptron controller = ((NumericalDynamicalSystems.Builder<MultiLayerPerceptron, ?>)
    nb.build("ds.num.mlp()"))
    .apply(environment.nOfOutputs(), environment.nOfInputs());
    controller.randomize(new Random(), DoubleRange.SYMMETRIC_UNIT);*/
    Random random = new Random();
    NumericalStatelessSystem controller = NumericalStatelessSystem.from(2, 2, (t, a) ->
        new double[] {Math.cos(t) + random.nextGaussian(0d, .5), Math.sin(t) - random.nextGaussian(.1, .5)});
    SingleAgentTask<DynamicalSystem<double[], double[], ?>, double[], double[], PointNavigationEnvironment.State>
        task = SingleAgentTask.fromEnvironment(environment, new double[2], new DoubleRange(0, 60), 0.1);
    Simulation.Outcome<SingleAgentTask.Step<double[], double[], PointNavigationEnvironment.State>> outcome =
        task.simulate(controller);
    PointNavigationDrawer d = new PointNavigationDrawer(PointNavigationDrawer.Configuration.DEFAULT);
    d.show(new ImageBuilder.ImageInfo(500, 500), outcome);
    // d.save(new ImageBuilder.ImageInfo(500, 500), new File("/home/francescorusin/Downloads/E_MAZE.png"), outcome);
  }

  public static void navigation() throws IOException {
    NamedBuilder<?> nb = NamedBuilder.fromDiscovery();
    NavigationEnvironment environment = (NavigationEnvironment) nb.build("ds.e.navigation(arena = E_MAZE)");
    @SuppressWarnings("unchecked")
    MultiLayerPerceptron mlp = ((NumericalDynamicalSystems.Builder<MultiLayerPerceptron, ?>)
            nb.build("ds.num.mlp()"))
        .apply(environment.nOfOutputs(), environment.nOfInputs());
    mlp.randomize(new Random(), DoubleRange.SYMMETRIC_UNIT);
    SingleAgentTask<DynamicalSystem<double[], double[], ?>, double[], double[], NavigationEnvironment.State> task =
        SingleAgentTask.fromEnvironment(environment, new double[2], new DoubleRange(0, 60), 0.1);
    Simulation.Outcome<SingleAgentTask.Step<double[], double[], NavigationEnvironment.State>> outcome =
        task.simulate(mlp);
    NavigationDrawer d = new NavigationDrawer(NavigationDrawer.Configuration.DEFAULT);
    d.show(new ImageBuilder.ImageInfo(500, 500), outcome);
    System.out.println(
        outcome.snapshots().get(outcome.snapshots().lastKey()).state().nOfCollisions());
    // d.save(new ImageBuilder.ImageInfo(500, 500), new File("/home/melsalib/Downloads/E_MAZE.png"), outcome);
  }
}
