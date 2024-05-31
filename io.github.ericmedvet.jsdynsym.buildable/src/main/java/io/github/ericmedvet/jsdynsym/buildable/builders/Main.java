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
import io.github.ericmedvet.jsdynsym.control.navigation.*;
import io.github.ericmedvet.jsdynsym.core.DynamicalSystem;
import io.github.ericmedvet.jsdynsym.core.numerical.NumericalTimeInvariantStatelessSystem;
import io.github.ericmedvet.jsdynsym.core.numerical.ann.MultiLayerPerceptron;
import io.github.ericmedvet.jviz.core.drawer.ImageBuilder;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

public class Main {
  public static void main(String[] args) throws IOException {
    pointNavVisual();
  }

  @SuppressWarnings("unchecked")
  public static void pointNavVisual() {
    NamedBuilder<?> nb = NamedBuilder.fromDiscovery();
    String genotype =
        "rO0ABXNyABNqYXZhLnV0aWwuQXJyYXlMaXN0eIHSHZnHYZ0DAAFJAARzaXpleHAAAAAWdwQAAAAWc3IAEGphdmEubGFuZy5Eb3VibGWAs8JKKWv7BAIAAUQABXZhbHVleHIAEGphdmEubGFuZy5OdW1iZXKGrJUdC5TgiwIAAHhwP+HfAxn4soBzcQB+AAI/5/cWGhuXynNxAH4AAr/VPpwJHdyAc3EAfgACv8jBAcrQY6BzcQB+AAK/5cwX9vm3MnNxAH4AAj/desmMrqzYc3EAfgACv8/9cWhZJnhzcQB+AAK/z1669BZtQHNxAH4AAj+/BbKVlmMwc3EAfgACv+biExNRExBzcQB+AAI/2NINXFEfoHNxAH4AAr/n8MgDN+0Mc3EAfgACP8gJ2fyu00hzcQB+AAK/7FiZq3ZpsHNxAH4AAr/sodjxwbdqc3EAfgACP8oN6i+X/JhzcQB+AAK/zzJxehFIqHNxAH4AAr/mXy4s9PQic3EAfgACP4xC3kyxYYBzcQB+AAI/4yais6r7EHNxAH4AAj/YYcjqIOGgc3EAfgACP+HNfeh3wOZ4";
    Function<String, Object> decoder = (Function<String, Object>) nb.build("f.fromBase64()");
    List<Double> actualGenotype = (List<Double>) decoder.apply(genotype);
    PointNavigationEnvironment environment = (PointNavigationEnvironment)
        nb.build(
            "ds.e.pointNavigation(arena = E_MAZE;initialRobotXRange = m.range(min = 0.5; max = 0.55);initialRobotYRange = m.range(min = 0.75; max = 0.75);robotMaxV = 0.05)");
    MultiLayerPerceptron mlp = ((NumericalDynamicalSystems.Builder<MultiLayerPerceptron, ?>)
            nb.build("ds.num.mlp(innerLayerRatio = 2.0)"))
        .apply(environment.nOfOutputs(), environment.nOfInputs());
    mlp.setParams(actualGenotype.stream().mapToDouble(d -> d).toArray());
    SingleAgentTask<DynamicalSystem<double[], double[], ?>, double[], double[], PointNavigationEnvironment.State>
        task = SingleAgentTask.fromEnvironment(
            environment,
            new double[2],
            s -> s.robotPosition().distance(s.targetPosition()) < .01,
            new DoubleRange(0, 100),
            0.1);
    Simulation.Outcome<SingleAgentTask.Step<double[], double[], PointNavigationEnvironment.State>> outcome =
        task.simulate(mlp);
    PointNavigationDrawer d = new PointNavigationDrawer(PointNavigationDrawer.Configuration.DEFAULT);
    d.show(new ImageBuilder.ImageInfo(500, 500), outcome);
    Function<Simulation.Outcome<SingleAgentTask.Step<double[], double[], PointNavigationEnvironment.State>>, Double>
        fitness = (Function<
                Simulation.Outcome<
                    SingleAgentTask.Step<double[], double[], PointNavigationEnvironment.State>>,
                Double>)
            nb.build("ds.e.n.finalTimePlusD()");
    System.out.println(fitness.apply(outcome));
    /*VectorFieldDrawer vfd =
    new VectorFieldDrawer(Arena.Prepared.E_MAZE.arena(), VectorFieldDrawer.Configuration.DEFAULT);
    vfd.show(new ImageBuilder.ImageInfo(500, 500), mlp);*/
  }

  public static void pointNavigation() {
    NamedBuilder<?> nb = NamedBuilder.fromDiscovery();
    PointNavigationEnvironment environment =
        (PointNavigationEnvironment) nb.build("ds.e.pointNavigation(arena = E_MAZE)");
    /*MultiLayerPerceptron mlp = ((NumericalDynamicalSystems.Builder<MultiLayerPerceptron, ?>)
    nb.build("ds.num.mlp()"))
    .apply(environment.nOfOutputs(), environment.nOfInputs());
    mlp.randomize(new Random(), DoubleRange.SYMMETRIC_UNIT);*/
    NumericalTimeInvariantStatelessSystem dynSys = new NumericalTimeInvariantStatelessSystem() {
      @Override
      public double[] step(double[] input) {
        return new double[] {Math.cos(input[0]) * input[0], Math.sin(input[0]) * input[1]};
      }

      @Override
      public int nOfInputs() {
        return 2;
      }

      @Override
      public int nOfOutputs() {
        return 2;
      }
    };
    VectorFieldDrawer vfd =
        new VectorFieldDrawer(Arena.Prepared.E_MAZE.arena(), VectorFieldDrawer.Configuration.DEFAULT);
    vfd.show(new ImageBuilder.ImageInfo(500, 500), dynSys);
    /*Random random = new Random();
    NumericalStatelessSystem controller = NumericalStatelessSystem.from(2, 2, (t, a) ->
    new double[] {Math.cos(t) + random.nextGaussian(0d, .5), Math.sin(t) - random.nextGaussian(.1, .5)});
    SingleAgentTask<DynamicalSystem<double[], double[], ?>, double[], double[], PointNavigationEnvironment.State>
    task = SingleAgentTask.fromEnvironment(environment, new double[2], new DoubleRange(0, 60), 0.1);
    Simulation.Outcome<SingleAgentTask.Step<double[], double[], PointNavigationEnvironment.State>> outcome =
    task.simulate(controller);
    PointNavigationDrawer d = new PointNavigationDrawer(PointNavigationDrawer.Configuration.DEFAULT);
    d.show(new ImageBuilder.ImageInfo(500, 500), outcome);*/
    // d.save(new ImageBuilder.ImageInfo(500, 500), new File("/home/francescorusin/Downloads/E_MAZE.png"), outcome);
  }

  public static void navigation() {
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
