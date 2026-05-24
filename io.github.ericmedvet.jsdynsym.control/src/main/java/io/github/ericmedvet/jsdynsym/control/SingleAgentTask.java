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
package io.github.ericmedvet.jsdynsym.control;

import io.github.ericmedvet.jnb.datastructure.DoubleRange;
import io.github.ericmedvet.jnb.datastructure.Listener;
import io.github.ericmedvet.jsdynsym.control.SingleAgentTask.Step;
import io.github.ericmedvet.jsdynsym.core.DynamicalSystem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public interface SingleAgentTask<C extends DynamicalSystem<O, A, ? extends CS>, O, A, CS, TS> extends Simulation<C, Step<O, A, TS>, Simulation.Outcome<Step<O, A, TS>>> {

  static <C extends DynamicalSystem<O, A, ? extends CS>, O, A, CS, TS> SingleAgentTask<C, O, A, CS, TS> sequential(
      List<? extends SingleAgentTask<C, O, A, CS, TS>> tasks,
      boolean resetFirst,
      boolean resetAll
  ) {
    return new SingleAgentTask<>() {
      @Override
      public Optional<C> example() {
        return tasks.getFirst().example();
      }

      @Override
      public Outcome<Step<O, A, TS>> simulate(
          C c,
          double dT,
          DoubleRange tRange,
          Listener<Timed<CS>> agentStateListener
      ) {
        if (resetFirst) {
          c.reset();
        }
        double singleDuration = tRange.extent() / tasks.size();
        double currentInitT = tRange.min();
        List<Outcome<Step<O, A, TS>>> outcomes = new ArrayList<>(tasks.size());
        for (SingleAgentTask<C, O, A, CS, TS> task : tasks) {
          if (resetAll) {
            c.reset();
          }
          DoubleRange localTRange = new DoubleRange(currentInitT, currentInitT + singleDuration);
          outcomes.add(task.simulate(c, dT, localTRange, agentStateListener));
          currentInitT = currentInitT + singleDuration;
        }
        TreeMap<Double, Step<O, A, TS>> snapshots = outcomes.stream()
            .flatMap(o -> o.snapshots().entrySet().stream())
            .collect(
                Collectors.toMap(
                    Entry::getKey,
                    Entry::getValue,
                    (s1, s2) -> s1,
                    TreeMap::new
                )
            );
        return Outcome.of(snapshots);
      }

      @Override
      public String toString() {
        return tasks.toString();
      }
    };
  }

  record Step<O, A, S>(O observation, A action, S state) {

  }

  Outcome<Step<O, A, TS>> simulate(
      C c,
      double dT,
      DoubleRange tRange,
      Listener<Timed<CS>> agentStateListener
  );

  @Override
  default Outcome<Step<O, A, TS>> simulate(C c, double dT, DoubleRange tRange) {
    return simulate(c, dT, tRange, Listener.deaf());
  }

  static <C extends DynamicalSystem<O, A, ? extends CS>, O, A, CS, TS> SingleAgentTask<C, O, A, CS, TS> fromEnvironment(
      Supplier<? extends DynamicalSystem<A, O, TS>> environmentSupplier,
      O initialObservation,
      C exampleAgent,
      Predicate<TS> stopCondition,
      boolean resetAgent
  ) {
    return new SingleAgentTask<>() {
      @Override
      public Outcome<Step<O, A, TS>> simulate(
          C agent,
          double dT,
          DoubleRange tRange,
          Listener<Timed<CS>> agentStateListener
      ) {
        DynamicalSystem<A, O, TS> environment = environmentSupplier.get();
        environment.reset();
        if (resetAgent) {
          agent.reset();
        }
        double t = tRange.min();
        Map<Double, Step<O, A, TS>> steps = new HashMap<>();
        O observation = initialObservation;
        while (t <= tRange.max() && !stopCondition.test(environment.getState())) {
          A action = agent.step(t, observation);
          agentStateListener.listen(new Timed<>(t, agent.getState()));
          observation = environment.step(t, action);
          steps.put(t, new Step<>(observation, action, environment.getState()));
          t = t + dT;
        }
        agentStateListener.done();
        return Outcome.of(new TreeMap<>(steps));
      }

      @Override
      public Optional<C> example() {
        return Optional.of(exampleAgent);
      }
    };
  }

  static <C extends DynamicalSystem<O, A, ? extends CS>, O, A, CS, TS> SingleAgentTask<C, O, A, CS, TS> fromEnvironment(
      Supplier<Environment<O, A, TS, C>> environmentSupplier,
      Predicate<TS> stopCondition,
      boolean resetAgent
  ) {
    return fromEnvironment(
        environmentSupplier,
        environmentSupplier.get().defaultObservation(),
        environmentSupplier.get().exampleAgent(),
        stopCondition,
        resetAgent
    );
  }

  record Timed<S>(double t, S state) {

  }
}