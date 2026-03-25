/*-
 * ========================LICENSE_START=================================
 * jsdynsym-experimenter
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

package io.github.ericmedvet.jsdynsym;

import io.github.ericmedvet.jnb.core.NamedBuilder;
import io.github.ericmedvet.jnb.datastructure.DoubleRange;
import io.github.ericmedvet.jnb.datastructure.FormattedNamedFunction;
import io.github.ericmedvet.jsdynsym.control.Environment;
import io.github.ericmedvet.jsdynsym.control.Simulation;
import io.github.ericmedvet.jsdynsym.control.Simulation.Outcome;
import io.github.ericmedvet.jsdynsym.control.SingleAgentTask;
import io.github.ericmedvet.jsdynsym.control.SingleAgentTask.Step;
import io.github.ericmedvet.jsdynsym.control.SingleRLAgentTask;
import io.github.ericmedvet.jsdynsym.control.drawer.VectorFieldDrawer;
import io.github.ericmedvet.jsdynsym.control.drawer.VectorialTrajectoryDrawer;
import io.github.ericmedvet.jsdynsym.control.navigation.Arena;
import io.github.ericmedvet.jsdynsym.control.navigation.NavigationDrawer;
import io.github.ericmedvet.jsdynsym.control.navigation.NavigationEnvironment;
import io.github.ericmedvet.jsdynsym.control.navigation.PointNavigationDrawer;
import io.github.ericmedvet.jsdynsym.control.navigation.PointNavigationEnvironment;
import io.github.ericmedvet.jsdynsym.control.synthetic.BooleanUtils;
import io.github.ericmedvet.jsdynsym.control.synthetic.BooleanUtils.ScoreType;
import io.github.ericmedvet.jsdynsym.control.synthetic.SequentialBooleanFunction;
import io.github.ericmedvet.jsdynsym.control.synthetic.SequentialBooleanFunction.State;
import io.github.ericmedvet.jsdynsym.control.synthetic.SequentialBooleanFunctionDrawer;
import io.github.ericmedvet.jsdynsym.core.bool.BooleanFunction;
import io.github.ericmedvet.jsdynsym.core.numerical.LinearCombination;
import io.github.ericmedvet.jsdynsym.core.numerical.MultivariateRealFunction;
import io.github.ericmedvet.jsdynsym.core.numerical.NumericalDynamicalSystem;
import io.github.ericmedvet.jsdynsym.core.numerical.ann.HebbianMultiLayerPerceptron;
import io.github.ericmedvet.jsdynsym.core.numerical.ann.MLPUtils;
import io.github.ericmedvet.jsdynsym.core.numerical.ann.MultiLayerPerceptron;
import io.github.ericmedvet.jsdynsym.core.rl.FreeFormPlasticMLPRLAgent;
import io.github.ericmedvet.jsdynsym.core.rl.NumericalReinforcementLearningAgent;
import io.github.ericmedvet.jsdynsym.core.rl.ReinforcementLearningAgent.RewardedInput;
import io.github.ericmedvet.jviz.core.drawer.Drawer;
import io.github.ericmedvet.jviz.core.drawer.Drawer.Arrangement;
import io.github.ericmedvet.jviz.core.plot.TrajectoryPlot.Data.ReductionType;
import io.github.ericmedvet.jviz.core.plot.image.Configuration;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

public class Main {

  public static void freeFormNavigation() {
    NamedBuilder<?> nb = NamedBuilder.fromDiscovery();
    @SuppressWarnings("unchecked") SingleRLAgentTask<NumericalReinforcementLearningAgent<?>, double[], double[], ?, ?> task = (SingleRLAgentTask<NumericalReinforcementLearningAgent<?>, double[], double[], ?, ?>) nb
        .build(
            """
                ds.srlat.fromNumericalEnvironment(
                  environment = ds.e.navigation(
                    arena = ds.arena.prepared(name = empty)
                  );
                  reward = ds.e.nav.reward.reaching()
                )
                """
        );
    @SuppressWarnings("unchecked") FreeFormPlasticMLPRLAgent ffmlp = ((Function<NumericalReinforcementLearningAgent<?>, FreeFormPlasticMLPRLAgent>) nb
        .build(
            "ds.rl.num.freeFormMlp(innerLayers = [2]; weightInitializationType = zeros)"
        )).apply(task.example().orElseThrow());
    int runs = 10;
    for (int r = 0; r < runs; r++) {
      long startTime = System.nanoTime();
      for (int i = 0; i < 1000; i++) {
        ffmlp.reset();
        //      Simulation.Outcome<? extends SingleAgentTask.Step<ReinforcementLearningAgent.RewardedInput<double[]>, double[], ?>> outcome =
        task.simulate(ffmlp, 0.1, new DoubleRange(0, 30));
        //        Point finalRobotPosition = ((NavigationEnvironment.State) outcome.snapshots()
        //            .get(outcome.snapshots().lastKey())
        //            .state()).robotPosition();
        //        System.out.printf("%.2f;%.2f%n", finalRobotPosition.x(), finalRobotPosition.y());
      }
      long elapsedNanos = System.nanoTime() - startTime;
      System.out.printf("%.2f%n", elapsedNanos / 1e9);
    }
  }

  public static void main(String[] args) throws IOException {
    // navigation();
    // testMlp();
    // pointNavigation();
    // pointNavVisual();
    // hebbianNavigation();
    // testHebbian();
    // freeFormNavigation();
    // manualNavigation();
    // rlNavigation();
    // sequentialXor();
    xorTest();
  }

  public static void xorTest() {
    int nOfInputs = 2;
    int nOfOutputs = 1;
    int[] innerNeurons = {2};
    double[][][] weights = new double[][][]{{{1, 2, 2}, {1, -1, -1}}, {{-2, 2, 2}}
    };
    int[] neurons = MLPUtils.countNeurons(nOfInputs, innerNeurons, nOfOutputs);
    double[][] inputs = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
    MultiLayerPerceptron mlp = new MultiLayerPerceptron(MultiLayerPerceptron.ActivationFunction.TANH, weights, neurons);
    double[][][] zeros = MLPUtils.zeroWeights(
        MLPUtils.countNeurons(nOfInputs, innerNeurons, nOfOutputs)
    );
    HebbianMultiLayerPerceptron hmlp = new HebbianMultiLayerPerceptron(
        MultiLayerPerceptron.ActivationFunction.TANH,
        zeros,
        zeros,
        zeros,
        zeros,
        weights,
        neurons,
        0,
        1,
        new DoubleRange(-0.0, 0.0),
        10,
        new Random(2),
        HebbianMultiLayerPerceptron.ParametrizationType.NETWORK,
        HebbianMultiLayerPerceptron.WeightInitializationType.PARAMS
    );
    System.out.println("MLP test:");
    for (double[] input : inputs) {
      double output = mlp.compute(input)[0];
      // double roundOut = (output >= 0) ? 1 : -1;
      System.out.printf(
          "x1=%2.0f x2=%2.0f -> y=%2.2f%n",
          input[0],
          input[1],
          output
      );
    }
    System.out.println("hMLP test:");
    for (double[] input : inputs) {
      double output = hmlp.step(input)[0];
      // double roundOut = (output >= 0) ? 1 : -1;
      System.out.printf(
          "x1=%2.0f x2=%2.0f -> y=%2.2f%n",
          input[0],
          input[1],
          output
      );
    }
  }

  public static void hebbianNavigation() {
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
                  robotRadius = 0.01
                )
                """
        );
    @SuppressWarnings("unchecked") HebbianMultiLayerPerceptron hmlp = ((Function<NumericalDynamicalSystem<?>, HebbianMultiLayerPerceptron>) nb
        .build(
            "ds.num.hebbianMlp(innerLayers = [16]; learningRate = 0.02; weightInitializationType = params; parametrizationType = synapse)"
        )).apply(environment.exampleAgent());
    hmlp.randomize(new Random(2), DoubleRange.SYMMETRIC_UNIT);
    SingleAgentTask<NumericalDynamicalSystem<?>, double[], double[], ?, NavigationEnvironment.State> task = SingleAgentTask
        .fromEnvironment(() -> environment, s -> false, true);
    Simulation.Outcome<SingleAgentTask.Step<double[], double[], NavigationEnvironment.State>> outcome = task.simulate(
        hmlp,
        0.1,
        new DoubleRange(0, 30)
    );
    NavigationDrawer d = new NavigationDrawer(NavigationDrawer.Configuration.DEFAULT);
    @SuppressWarnings("unchecked") Function<Simulation.Outcome<SingleAgentTask.Step<double[], double[], NavigationEnvironment.State>>, Double> fitness = (Function<Simulation.Outcome<SingleAgentTask.Step<double[], double[], NavigationEnvironment.State>>, Double>) nb
        .build("ds.e.n.arenaCoverage()");
    System.out.println(fitness.apply(outcome));
    Function<Double, Simulation.Outcome<SingleAgentTask.Step<double[], double[], NavigationEnvironment.State>>> tResF = dT -> SingleAgentTask
        .fromEnvironment(
            () -> environment,
            s -> false,
            true
        )
        .simulate(hmlp, dT, new DoubleRange(0, 30));
    d.multi(Arrangement.HORIZONTAL)
        .show(
            DoubleStream.iterate(0.05, v -> v <= 0.25, v -> v + 0.025).boxed().map(tResF).toList()
        );
  }

  public static void pointNavigation() {
    NamedBuilder<?> nb = NamedBuilder.fromDiscovery();
    @SuppressWarnings("unchecked") Environment<double[], double[], PointNavigationEnvironment.State, NumericalDynamicalSystem<?>> environment = (Environment<double[], double[], PointNavigationEnvironment.State, NumericalDynamicalSystem<?>>) nb
        .build(
            "ds.e.pointNavigation(arena = ds.arena.prepared(name = e_maze))"
        );
    @SuppressWarnings("unchecked") MultiLayerPerceptron mlp = ((Function<NumericalDynamicalSystem<?>, MultiLayerPerceptron>) nb
        .build(
            "ds.num.mlp()"
        ))
        .apply(environment.exampleAgent());
    mlp.randomize(new Random(), DoubleRange.SYMMETRIC_UNIT);
    VectorFieldDrawer vfd = new VectorFieldDrawer(
        Arena.Prepared.E_MAZE.arena(),
        VectorFieldDrawer.Configuration.DEFAULT
    );
    vfd.show(mlp);
    SingleAgentTask<NumericalDynamicalSystem<?>, double[], double[], ?, PointNavigationEnvironment.State> task = SingleAgentTask
        .fromEnvironment(() -> environment, s -> false, true);
    Simulation.Outcome<SingleAgentTask.Step<double[], double[], PointNavigationEnvironment.State>> outcome = task
        .simulate(mlp, 0.1, new DoubleRange(0, 10));
    new PointNavigationDrawer(PointNavigationDrawer.Configuration.DEFAULT)
        .videoBuilder()
        .save(new File("../point-navigation.mp4"), outcome);
  }

  public static void manualNavigation() {
    NamedBuilder<?> nb = NamedBuilder.fromDiscovery();
    @SuppressWarnings("unchecked") Environment<double[], double[], NavigationEnvironment.State, NumericalDynamicalSystem<?>> environment = (Environment<double[], double[], NavigationEnvironment.State, NumericalDynamicalSystem<?>>) nb
        .build(
            """
                ds.e.navigation(
                  arena = ds.arena.fromString(
                    name = "deceptive-corridor";
                    s = "s            |             |             |wwww     wwww|   w w w w   |   w w w w   |   w w w w   |   w w w w   |   www w w   |   w   w w   |wwww   wwwwww|             |           t "
                  );
                  robotRadius = 0.05
                )
                """
        );
    MultivariateRealFunction agent = MultivariateRealFunction.from(inputs -> {
      if (inputs[inputs.length / 2] < 0.5) {
        return new double[]{-1, 1};
      }
      return new double[]{1, 0.9};
    }, environment.exampleAgent().nOfInputs(), 2);
    NavigationDrawer d = new NavigationDrawer(NavigationDrawer.Configuration.DEFAULT);
    @SuppressWarnings("unchecked") FormattedNamedFunction<Outcome<Step<double[], double[], NavigationEnvironment.State>>, String> sTraj = (FormattedNamedFunction<Simulation.Outcome<SingleAgentTask.Step<double[], double[], NavigationEnvironment.State>>, String>) nb
        .build("ds.e.n.symbolicTrajectory()");
    @SuppressWarnings("unchecked") FormattedNamedFunction<Outcome<Step<double[], double[], NavigationEnvironment.State>>, Double> gap = (FormattedNamedFunction<Simulation.Outcome<SingleAgentTask.Step<double[], double[], NavigationEnvironment.State>>, Double>) nb
        .build("ds.e.n.avgGapToObstacle()");
    @SuppressWarnings("unchecked") FormattedNamedFunction<Outcome<Step<double[], double[], NavigationEnvironment.State>>, Double> collapsedSTraj = (FormattedNamedFunction<Simulation.Outcome<SingleAgentTask.Step<double[], double[], NavigationEnvironment.State>>, Double>) nb
        .build("ds.e.n.symbolicTrajectory(collapse = true)");
    SingleAgentTask<NumericalDynamicalSystem<?>, double[], double[], ?, NavigationEnvironment.State> task = SingleAgentTask
        .fromEnvironment(() -> environment, s -> false, true);
    Simulation.Outcome<SingleAgentTask.Step<double[], double[], NavigationEnvironment.State>> outcome = task.simulate(
        agent,
        0.1,
        new DoubleRange(0, 30)
    );
    d.show(outcome);
    System.out.println(sTraj + " = " + sTraj.applyFormatted(outcome));
    System.out.println(gap + " = " + gap.applyFormatted(outcome));
    System.out.println(collapsedSTraj + " = " + collapsedSTraj.applyFormatted(outcome));
    d.videoBuilder().save(new File("../nav.mp4"), outcome);
  }

  public static void navigation() {
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
                  robotRadius = 0.01
                )
                """
        );
    @SuppressWarnings("unchecked") MultiLayerPerceptron mlp = ((Function<NumericalDynamicalSystem<?>, MultiLayerPerceptron>) nb
        .build(
            "ds.num.mlp(innerLayers = [16; 16])"
        )).apply(environment.exampleAgent());
    mlp.randomize(new Random(2), DoubleRange.SYMMETRIC_UNIT);
    LinearCombination linear = new LinearCombination(mlp.nOfInputs(), mlp.nOfOutputs(), false);
    linear.randomize(new Random(2), DoubleRange.SYMMETRIC_UNIT);
    NumericalDynamicalSystem<?> agent = linear;
    SingleAgentTask<NumericalDynamicalSystem<?>, double[], double[], ?, NavigationEnvironment.State> task = SingleAgentTask
        .fromEnvironment(() -> environment, s -> false, true);
    Simulation.Outcome<SingleAgentTask.Step<double[], double[], NavigationEnvironment.State>> outcome = task.simulate(
        agent,
        0.1,
        new DoubleRange(0, 30)
    );
    NavigationDrawer d = new NavigationDrawer(NavigationDrawer.Configuration.DEFAULT);
    @SuppressWarnings("unchecked") FormattedNamedFunction<Outcome<Step<double[], double[], NavigationEnvironment.State>>, Double> fitness = (FormattedNamedFunction<Simulation.Outcome<SingleAgentTask.Step<double[], double[], NavigationEnvironment.State>>, Double>) nb
        .build("ds.e.n.arenaCoverage()");
    @SuppressWarnings("unchecked") FormattedNamedFunction<Outcome<Step<double[], double[], NavigationEnvironment.State>>, String> sTraj = (FormattedNamedFunction<Simulation.Outcome<SingleAgentTask.Step<double[], double[], NavigationEnvironment.State>>, String>) nb
        .build("ds.e.n.symbolicTrajectory()");
    System.out.println(fitness + " = " + fitness.applyFormatted(outcome));
    System.out.println(sTraj + " = " + sTraj.applyFormatted(outcome));
    d.show(outcome);
    Function<Double, Simulation.Outcome<SingleAgentTask.Step<double[], double[], NavigationEnvironment.State>>> tResF = dT -> SingleAgentTask
        .fromEnvironment(
            () -> environment,
            s -> false,
            true
        )
        .simulate(agent, dT, new DoubleRange(0, 30));
    d.multi(Arrangement.HORIZONTAL)
        .show(
            DoubleStream.iterate(0.05, v -> v <= 0.25, v -> v + 0.10).boxed().map(tResF).toList()
        );
  }

  @SuppressWarnings("unchecked")
  public static void pointNavVisual() {
    NamedBuilder<?> nb = NamedBuilder.fromDiscovery();
    String genotype = "rO0ABXNyABNqYXZhLnV0aWwuQXJyYXlMaXN0eIHSHZnHYZ0DAAFJAARzaXpleHAAAAAWdwQAAAAWc3IAEGphdmEubGFuZy5Eb3VibGWAs8JKKWv7BAIAAUQABXZhbHVleHIAEGphdmEubGFuZy5OdW1iZXKGrJUdC5TgiwIAAHhwP+HfAxn4soBzcQB+AAI/5/cWGhuXynNxAH4AAr/VPpwJHdyAc3EAfgACv8jBAcrQY6BzcQB+AAK/5cwX9vm3MnNxAH4AAj/desmMrqzYc3EAfgACv8/9cWhZJnhzcQB+AAK/z1669BZtQHNxAH4AAj+/BbKVlmMwc3EAfgACv+biExNRExBzcQB+AAI/2NINXFEfoHNxAH4AAr/n8MgDN+0Mc3EAfgACP8gJ2fyu00hzcQB+AAK/7FiZq3ZpsHNxAH4AAr/sodjxwbdqc3EAfgACP8oN6i+X/JhzcQB+AAK/zzJxehFIqHNxAH4AAr/mXy4s9PQic3EAfgACP4xC3kyxYYBzcQB+AAI/4yais6r7EHNxAH4AAj/YYcjqIOGgc3EAfgACP+HNfeh3wOZ4";
    Function<String, Object> decoder = (Function<String, Object>) nb.build("f.fromBase64()");
    List<Double> actualGenotype = (List<Double>) decoder.apply(genotype);
    Environment<double[], double[], PointNavigationEnvironment.State, NumericalDynamicalSystem<?>> environment = (Environment<double[], double[], PointNavigationEnvironment.State, NumericalDynamicalSystem<?>>) nb
        .build(
            "ds.e.pointNavigation(arena = ds.arena.prepared(name = e_maze);initialRobotXRange = m.range(min = 0.5; max = 0.55);" + "initialRobotYRange = m.range(min = 0.75; max = 0.75);robotMaxV = 0.05)"
        );
    MultiLayerPerceptron mlp = ((Function<NumericalDynamicalSystem<?>, MultiLayerPerceptron>) nb.build(
        "ds.num.mlp(innerLayerRatio = 2.0)"
    ))
        .apply(environment.exampleAgent());
    mlp.setParams(actualGenotype.stream().mapToDouble(d -> d).toArray());
    SingleAgentTask<NumericalDynamicalSystem<?>, double[], double[], ?, PointNavigationEnvironment.State> task = SingleAgentTask
        .fromEnvironment(
            () -> environment,
            s -> s.robotPosition().distance(s.targetPosition()) < .01,
            true
        );
    Simulation.Outcome<SingleAgentTask.Step<double[], double[], PointNavigationEnvironment.State>> outcome = task
        .simulate(mlp, 0.1, new DoubleRange(0, 100));
    PointNavigationDrawer d = new PointNavigationDrawer(
        PointNavigationDrawer.Configuration.DEFAULT
    );
    d.show(new Drawer.ImageInfo(500, 500), outcome);
    Function<Simulation.Outcome<SingleAgentTask.Step<double[], double[], PointNavigationEnvironment.State>>, Double> fitness = (Function<Simulation.Outcome<SingleAgentTask.Step<double[], double[], PointNavigationEnvironment.State>>, Double>) nb
        .build("ds.e.n.finalTimePlusD()");
    System.out.println(fitness.apply(outcome));
    VectorFieldDrawer vfd = new VectorFieldDrawer(
        Arena.Prepared.E_MAZE.arena(),
        VectorFieldDrawer.Configuration.DEFAULT
    );
    vfd.show(mlp);
  }

  public static void sequentialXor() {
    NamedBuilder<?> nb = NamedBuilder.fromDiscovery();
    SingleRLAgentTask<NumericalReinforcementLearningAgent<?>, double[], double[], ?, State> sim = new SequentialBooleanFunction<>(
        Collections.nCopies(10, List.of("00", "01", "10", "11"))
            .stream()
            .flatMap(Collection::stream)
            .map(BooleanUtils::stringToBitString)
            .toList(),
        BooleanFunction.from(
            bits -> new boolean[]{bits[0] == bits[1], bits[0] != bits[1]
            },
            2,
            2
        ),
        ScoreType.UNLIMITED,
        true
    );
    @SuppressWarnings("unchecked") NumericalReinforcementLearningAgent<?> lac = ((Function<NumericalReinforcementLearningAgent<?>, NumericalReinforcementLearningAgent<?>>) nb
        .build(
            """
                ds.rl.num.linearActorCritic()
                """
        )).apply(NumericalReinforcementLearningAgent.from(MultivariateRealFunction.from(2, 2)));
    @SuppressWarnings("unchecked") NumericalReinforcementLearningAgent<?> ffMlp = ((Function<NumericalReinforcementLearningAgent<?>, NumericalReinforcementLearningAgent<?>>) nb
        .build(
            """
                ds.rl.num.freeFormMlp(innerLayers = [2])
                """
        )).apply(NumericalReinforcementLearningAgent.from(MultivariateRealFunction.from(2, 2)));
    SequentialBooleanFunctionDrawer drawer = new SequentialBooleanFunctionDrawer(
        Configuration.DEFAULT,
        Set.of(ScoreType.LIMITED)
    );
    @SuppressWarnings("unchecked") Function<NumericalReinforcementLearningAgent<?>, SortedMap<Double, double[]>> trajF = (Function<NumericalReinforcementLearningAgent<?>, SortedMap<Double, double[]>>) nb
        .build(
            """
                ds.f.agentStateTrajectory(
                  stateF = ds.f.params();
                  sat = ds.s.sequentialXor(cases = 100 * ["01"]; resetAgent = true);
                  dT = 1;
                  tRange = m.range(min = 0; max = 1000)
                )
                """
        );
    drawer.show(sim.simulate(lac, 1, new DoubleRange(0, 30)));
    drawer.show(sim.simulate(ffMlp, 1, new DoubleRange(0, 30)));
    new VectorialTrajectoryDrawer(Configuration.DEFAULT, ReductionType.TSNE).show(trajF.apply(lac));
    new VectorialTrajectoryDrawer(Configuration.DEFAULT, ReductionType.TSNE).show(
        trajF.apply(ffMlp)
    );
  }

  public static void rlNavigation() {
    NamedBuilder<?> nb = NamedBuilder.fromDiscovery();
    @SuppressWarnings("unchecked") SingleRLAgentTask<NumericalReinforcementLearningAgent<?>, double[], double[], ?, NavigationEnvironment.State> rlTask = (SingleRLAgentTask<NumericalReinforcementLearningAgent<?>, double[], double[], ?, NavigationEnvironment.State>) nb
        .build(
            """
                ds.srlat.fromNumericalEnvironment(
                  reward = ds.e.nav.reward.reaching();
                  environment = ds.e.navigation(
                    arena = ds.arena.fromString(
                      name = "deceptive-corridor";
                      s = "             |    s        |             |wwww     wwww|   w w w w   |   w w w w   |   w w w w   |   w w w w   |   www w w   |   w   w w   |wwww   wwwwww|             |           t "
                    );
                    robotRadius = 0.05
                   )
                )
                """
        );
    MultivariateRealFunction agent = MultivariateRealFunction.from(inputs -> {
      if (inputs[inputs.length / 2] < 0.5) {
        return new double[]{-1};
      }
      return new double[]{0.01};
    }, rlTask.example().orElseThrow().nOfInputs(), 1);
    agent = agent.andThen(
        MultivariateRealFunction.from(
            d -> new double[]{1d + 2d * Math.min(0d, d[0]), 1d - 2d * Math.max(0d, d[0])
            },
            1,
            2
        )
    );
    NavigationDrawer d = new NavigationDrawer(NavigationDrawer.Configuration.DEFAULT);
    @SuppressWarnings("unchecked") FormattedNamedFunction<Outcome<Step<double[], double[], NavigationEnvironment.State>>, String> sTraj = (FormattedNamedFunction<Simulation.Outcome<SingleAgentTask.Step<double[], double[], NavigationEnvironment.State>>, String>) nb
        .build("ds.e.n.symbolicTrajectory()");
    @SuppressWarnings("unchecked") FormattedNamedFunction<Outcome<Step<double[], double[], NavigationEnvironment.State>>, Double> gap = (FormattedNamedFunction<Simulation.Outcome<SingleAgentTask.Step<double[], double[], NavigationEnvironment.State>>, Double>) nb
        .build("ds.e.n.avgGapToObstacle()");
    @SuppressWarnings("unchecked") FormattedNamedFunction<Outcome<Step<double[], double[], NavigationEnvironment.State>>, Double> collapsedSTraj = (FormattedNamedFunction<Simulation.Outcome<SingleAgentTask.Step<double[], double[], NavigationEnvironment.State>>, Double>) nb
        .build("ds.e.n.symbolicTrajectory(collapse = true)");
    Simulation.Outcome<Step<RewardedInput<double[]>, double[], NavigationEnvironment.State>> outcome = rlTask.simulate(
        NumericalReinforcementLearningAgent.from(agent),
        0.1,
        new DoubleRange(0, 30)
    );
    Outcome<Step<double[], double[], NavigationEnvironment.State>> sOutcome = Outcome.of(
        new TreeMap<>(
            outcome.snapshots()
                .entrySet()
                .stream()
                .collect(
                    Collectors.toMap(
                        Entry::getKey,
                        e -> new Step<>(
                            e.getValue().observation().input(),
                            e.getValue().action(),
                            e.getValue().state()
                        )
                    )
                )
        )
    );
    d.show(sOutcome);
    outcome.snapshots()
        .forEach(
            (t, s) -> System.out.printf(
                "%4.1fs -> action=(%+5.3f,%+5.3f) reward=%.4f sAction=%s%n",
                t,
                s.action()[0],
                s.action()[1],
                s.observation().reward(),
                s.state().symbolicAction(0.1, Math.PI / 20)
            )
        );
  }

  public static void testHebbian() {
    NamedBuilder<?> nb = NamedBuilder.fromDiscovery();
    @SuppressWarnings("unchecked") Environment<double[], double[], NavigationEnvironment.State, NumericalDynamicalSystem<?>> environment = (Environment<double[], double[], NavigationEnvironment.State, NumericalDynamicalSystem<?>>) nb
        .build(
            """
                ds.e.navigation(
                    arena = ds.arena.prepared(name = empty)
                )
                """
        );
    @SuppressWarnings("unchecked") HebbianMultiLayerPerceptron hmlp = ((Function<NumericalDynamicalSystem<?>, HebbianMultiLayerPerceptron>) nb
        .build(
            "ds.num.hebbianMlp(innerLayers = [8]; weightInitializationType = zeros)"
        )).apply(environment.exampleAgent());
    SingleAgentTask<NumericalDynamicalSystem<?>, double[], double[], ?, NavigationEnvironment.State> task = SingleAgentTask
        .fromEnvironment(() -> environment, s -> false, true);
    int runs = 10;
    for (int r = 0; r < runs; r++) {
      long startTime = System.nanoTime();
      for (int i = 0; i < 1000; i++) {
        hmlp.reset();
        task.simulate(hmlp, 0.1, new DoubleRange(0, 30));
      }
      long elapsedNanos = System.nanoTime() - startTime;
      System.out.printf("%.2f%n", elapsedNanos / 1e9);
    }
  }

  public static void testMlp() {
    NamedBuilder<?> nb = NamedBuilder.fromDiscovery();
    @SuppressWarnings("unchecked") Environment<double[], double[], NavigationEnvironment.State, NumericalDynamicalSystem<?>> environment = (Environment<double[], double[], NavigationEnvironment.State, NumericalDynamicalSystem<?>>) nb
        .build(
            """
                ds.e.navigation(
                    arena = ds.arena.prepared(name = empty)
                )
                """
        );
    @SuppressWarnings("unchecked") MultiLayerPerceptron mlp = ((Function<NumericalDynamicalSystem<?>, MultiLayerPerceptron>) nb
        .build(
            "ds.num.mlp(innerLayers = [8])"
        )).apply(environment.exampleAgent());
    SingleAgentTask<NumericalDynamicalSystem<?>, double[], double[], ?, NavigationEnvironment.State> task = SingleAgentTask
        .fromEnvironment(() -> environment, s -> false, true);
    int runs = 10;
    for (int r = 0; r < runs; r++) {
      long startTime = System.nanoTime();
      for (int i = 0; i < 1000; i++) {
        task.simulate(mlp, 0.1, new DoubleRange(0, 30));
      }
      long elapsedNanos = System.nanoTime() - startTime;
      System.out.printf("%.2f%n", elapsedNanos / 1e9);
    }
  }
}