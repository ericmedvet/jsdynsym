/*-
 * ========================LICENSE_START=================================
 * jsdynsym-buildable
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

package io.github.ericmedvet.jsdynsym.buildable.builders;

import io.github.ericmedvet.jnb.core.Cacheable;
import io.github.ericmedvet.jnb.core.Discoverable;
import io.github.ericmedvet.jnb.core.Param;
import io.github.ericmedvet.jnb.datastructure.DoubleRange;
import io.github.ericmedvet.jnb.datastructure.FormattedNamedFunction;
import io.github.ericmedvet.jnb.datastructure.Parametrized;
import io.github.ericmedvet.jsdynsym.control.HomogeneousBiSimulation;
import io.github.ericmedvet.jsdynsym.control.Simulation;
import io.github.ericmedvet.jsdynsym.control.Simulation.Outcome;
import io.github.ericmedvet.jsdynsym.control.SingleAgentTask;
import io.github.ericmedvet.jsdynsym.control.SingleAgentTask.Step;
import io.github.ericmedvet.jsdynsym.core.DynamicalSystem;
import io.github.ericmedvet.jsdynsym.core.FrozenableDynamicalSystem;
import io.github.ericmedvet.jsdynsym.core.StatelessSystem;
import io.github.ericmedvet.jsdynsym.core.numerical.NumericalDynamicalSystem;
import io.github.ericmedvet.jsdynsym.core.numerical.ann.MLPUtils;
import io.github.ericmedvet.jsdynsym.core.numerical.ann.MultiLayerPerceptron;
import io.github.ericmedvet.jsdynsym.core.rl.FrozenableRLAgent;
import io.github.ericmedvet.jsdynsym.core.rl.NumericalReinforcementLearningAgent;
import io.github.ericmedvet.jsdynsym.core.rl.ReinforcementLearningAgent;
import io.github.ericmedvet.jsdynsym.core.rl.ReinforcementLearningAgent.RewardedInput;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Discoverable(prefixTemplate = "dynamicalSystem|dynSys|ds.function|f")
public class Functions {

  private Functions() {
  }

  @Cacheable
  public static <X, C extends DynamicalSystem<?, ?, ? extends CS>, CS> FormattedNamedFunction<X, SortedMap<Double, double[]>> agentStateTrajectory(
      @Param(value = "name", iS = "states.in[{simulation.name}]") String name,
      @Param(value = "of", dNPM = "f.identity()") Function<X, C> beforeF,
      @Param("stateF") Function<CS, double[]> stateF,
      @Param("sat") SingleAgentTask<C, ?, ?, CS, ?> sat,
      @Param("tRange") DoubleRange tRange,
      @Param("dT") double dT,
      @Param(value = "format", dS = "%s") String format
  ) {
    Function<C, SortedMap<Double, double[]>> f = c -> {
      SortedMap<Double, double[]> data = new TreeMap<>();
      sat.simulate(c, dT, tRange, timed -> data.put(timed.t(), stateF.apply(timed.state())));
      return data;
    };
    return FormattedNamedFunction.from(f, format, name).compose(beforeF);
  }

  @Cacheable
  public static <X, S> FormattedNamedFunction<X, NumericalReinforcementLearningAgent<S>> asNumericalRLAgent(
      @Param(value = "name", dS = "as.numrl.agent") String name,
      @Param(value = "of", dNPM = "f.identity()") Function<X, NumericalDynamicalSystem<S>> beforeF,
      @Param(value = "format", dS = "%s") String format
  ) {
    Function<NumericalDynamicalSystem<S>, NumericalReinforcementLearningAgent<S>> f = NumericalReinforcementLearningAgent::from;
    return FormattedNamedFunction.from(f, format, name).compose(beforeF);
  }

  @Cacheable
  public static <X, I, O, S> FormattedNamedFunction<X, ReinforcementLearningAgent<I, O, S>> asRLAgent(
      @Param(value = "name", dS = "as.rl.agent") String name,
      @Param(value = "of", dNPM = "f.identity()") Function<X, DynamicalSystem<I, O, S>> beforeF,
      @Param(value = "format", dS = "%s") String format
  ) {
    Function<DynamicalSystem<I, O, S>, ReinforcementLearningAgent<I, O, S>> f = ReinforcementLearningAgent::from;
    return FormattedNamedFunction.from(f, format, name).compose(beforeF);
  }

  @Cacheable
  public static <X> FormattedNamedFunction<X, Double> cumulatedReward(
      @Param(value = "name", iS = "cumulated.reward") String name,
      @Param(value = "of", dNPM = "f.identity()") Function<X, Outcome<SingleAgentTask.Step<RewardedInput<?>, ?, ?>>> beforeF,
      @Param(value = "format", dS = "%+6.3f") String format
  ) {
    Function<Outcome<Step<RewardedInput<?>, ?, ?>>, Double> f = o -> o.snapshots()
        .values()
        .stream()
        .mapToDouble(step -> step.observation().reward())
        .sum();
    return FormattedNamedFunction.from(f, format, name).compose(beforeF);
  }

  @Cacheable
  public static <X> FormattedNamedFunction<X, Double> doubleOp(
      @Param(value = "name", iS = "{activationF}") String name,
      @Param(value = "of", dNPM = "f.identity()") Function<X, Double> beforeF,
      @Param(value = "activationF", dS = "identity") MultiLayerPerceptron.ActivationFunction activationF,
      @Param(value = "format", dS = "%.1f") String format
  ) {
    Function<Double, Double> f = activationF::applyAsDouble;
    return FormattedNamedFunction.from(f, format, name)
        .compose(beforeF);
  }

  @Cacheable
  public static <X, I, O> FormattedNamedFunction<X, DynamicalSystem<I, O, ?>> nonLearning(
      @Param(value = "name", dS = "non.learning") String name,
      @Param(value = "of", dNPM = "f.identity()") Function<X, FrozenableRLAgent<I, O, ?>> beforeF,
      @Param(value = "format", dS = "%s") String format
  ) {
    Function<FrozenableRLAgent<I, O, ?>, DynamicalSystem<I, O, ?>> f = FrozenableRLAgent::dynamicalSystem;
    return FormattedNamedFunction.from(f, format, name).compose(beforeF);
  }

  @Cacheable
  public static <X, S, B extends Simulation.Outcome<SS>, SS> FormattedNamedFunction<X, Simulation.Outcome<SS>> opponentBiSimulator(
      @Param(value = "name", iS = "opponent.sim") String name,
      @Param(value = "of", dNPM = "f.identity()") Function<X, S> beforeF,
      @Param("simulation") HomogeneousBiSimulation<S, SS, B> biSimulation,
      @Param("opponent") S opponent,
      @Param(value = "home", dB = true) boolean home,
      @Param("tRange") DoubleRange tRange,
      @Param("dT") double dT,
      @Param(value = "format", dS = "%s") String format
  ) {
    Function<S, Simulation.Outcome<SS>> f = s -> home ? biSimulation.simulate(
        s,
        opponent,
        dT,
        tRange
    ) : biSimulation
        .simulate(opponent, s, dT, tRange);
    return FormattedNamedFunction.from(f, format, name).compose(beforeF);
  }

  @Cacheable
  public static <X, P> FormattedNamedFunction<X, P> params(
      @Param(value = "name", iS = "params") String name,
      @Param(value = "of", dNPM = "f.identity()") Function<X, Parametrized<?, P>> beforeF,
      @Param(value = "format", dS = "%s") String format
  ) {
    Function<Parametrized<?, P>, P> f = Parametrized::getParams;
    return FormattedNamedFunction.from(f, format, name).compose(beforeF);
  }

  @Cacheable
  public static <X, S, B extends Simulation.Outcome<SS>, SS> FormattedNamedFunction<X, Simulation.Outcome<SS>> selfBiSimulator(
      @Param(value = "name", iS = "self.sim") String name,
      @Param(value = "of", dNPM = "f.identity()") Function<X, S> beforeF,
      @Param("simulation") HomogeneousBiSimulation<S, SS, B> biSimulation,
      @Param("tRange") DoubleRange tRange,
      @Param("dT") double dT,
      @Param(value = "format", dS = "%s") String format
  ) {
    Function<S, Simulation.Outcome<SS>> f = s -> biSimulation.simulate(s, s, dT, tRange);
    return FormattedNamedFunction.from(f, format, name).compose(beforeF);
  }

  @Cacheable
  public static <X, S> FormattedNamedFunction<X, SortedMap<Double, S>> simOutcome(
      @Param(value = "name", iS = "sim.outcome") String name,
      @Param(value = "of", dNPM = "f.identity()") Function<X, Simulation.Outcome<S>> beforeF,
      @Param(value = "format", dS = "%s") String format
  ) {
    Function<Simulation.Outcome<S>, SortedMap<Double, S>> f = Simulation.Outcome::snapshots;
    return FormattedNamedFunction.from(f, format, name).compose(beforeF);
  }

  @Cacheable
  public static <X, SS, O extends Simulation.Outcome<SS>, S extends Simulation<T, SS, O>, T> Function<X, O> simulate(
      @Param(value = "name", iS = "sim[{simulation.name}]") String name,
      @Param(value = "of", dNPM = "f.identity()") Function<X, T> beforeF,
      @Param("simulation") S simulation,
      @Param("tRange") DoubleRange tRange,
      @Param("dT") double dT,
      @Param(value = "format", dS = "%s") String format
  ) {
    Function<T, O> f = t -> simulation.simulate(t, dT, tRange);
    return FormattedNamedFunction.from(f, format, name).compose(beforeF);
  }

  @Cacheable
  public static <X, I, O> FormattedNamedFunction<X, StatelessSystem<I, O>> stateless(
      @Param(value = "name", dS = "stateless") String name,
      @Param(value = "of", dNPM = "f.identity()") Function<X, FrozenableDynamicalSystem<I, O, ?>> beforeF,
      @Param(value = "format", dS = "%s") String format
  ) {
    Function<FrozenableDynamicalSystem<I, O, ?>, StatelessSystem<I, O>> f = FrozenableDynamicalSystem::stateless;
    return FormattedNamedFunction.from(f, format, name).compose(beforeF);
  }

  @Cacheable
  public static <X, O, A, S> FormattedNamedFunction<X, Outcome<SingleAgentTask.Step<O, A, S>>> unwrappedRl(
      @Param(value = "name", dS = "unwrapped.rl") String name,
      @Param(value = "of", dNPM = "f.identity()") Function<X, Outcome<SingleAgentTask.Step<RewardedInput<O>, A, S>>> beforeF,
      @Param(value = "format", dS = "%s") String format
  ) {
    Function<Outcome<SingleAgentTask.Step<RewardedInput<O>, A, S>>, Outcome<SingleAgentTask.Step<O, A, S>>> f = rlO -> Outcome
        .of(
            rlO.snapshots()
                .entrySet()
                .stream()
                .collect(
                    Collectors.toMap(
                        Entry::getKey,
                        e -> new Step<>(
                            e.getValue().observation().input(),
                            e.getValue().action(),
                            e.getValue().state()
                        ),
                        (s1, s2) -> s1,
                        TreeMap::new
                    )
                )
        );
    return FormattedNamedFunction.from(f, format, name).compose(beforeF);
  }

  @Cacheable
  public static <X> FormattedNamedFunction<X, List<List<List<Double>>>> weights(
      @Param(value = "name", iS = "weights") String name,
      @Param(value = "of", dNPM = "f.identity()") Function<X, MultiLayerPerceptron> beforeF,
      @Param(value = "format", dS = "%s") String format
  ) {
    Function<MultiLayerPerceptron, List<List<List<Double>>>> f = mlp -> {
      int[] neurons = new int[mlp.nOfLayers()];
      for (int i = 0; i < neurons.length; i++) {
        neurons[i] = mlp.sizeOfLayer(i);
      }
      double[][][] unflat = MLPUtils.unflat(mlp.getParams(), neurons);
      return Arrays.stream(unflat)
          .map(
              layerWs -> Arrays.stream(layerWs)
                  .map(
                      neuronWs -> Arrays.stream(
                          neuronWs
                      ).boxed().toList()
                  )
                  .toList()
          )
          .toList();
    };
    return FormattedNamedFunction.from(f, format, name).compose(beforeF);
  }

}