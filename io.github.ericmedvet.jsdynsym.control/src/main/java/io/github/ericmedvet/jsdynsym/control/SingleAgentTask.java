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
package io.github.ericmedvet.jsdynsym.control;

import io.github.ericmedvet.jnb.datastructure.DoubleRange;
import io.github.ericmedvet.jsdynsym.core.DynamicalSystem;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Supplier;

public interface SingleAgentTask<C extends DynamicalSystem<O, A, ?>, O, A, S> {

  record Step<O, A, S>(O observation, A action, S state) {}

  SortedMap<Double, Step<O, A, S>> simulate(C agent);

  static <C extends DynamicalSystem<O, A, ?>, O, A, S> SingleAgentTask<C, O, A, S> fromEnvironment(
      Environment<O, A, S> environment, DoubleRange tRange, double dT) {
    return fromEnvironment(environment, environment.defaultAgentAction(), tRange, dT);
  }

  static <C extends DynamicalSystem<O, A, ?>, O, A, S> SingleAgentTask<C, O, A, S> fromEnvironment(
      Supplier<Environment<O, A, S>> environmentSupplier, DoubleRange tRange, double dT) {
    return agent -> {
      Environment<O, A, S> environment = environmentSupplier.get();
      return fromEnvironment(environment, environment.defaultAgentAction(), tRange, dT)
          .simulate(agent);
    };
  }

  static <C extends DynamicalSystem<O, A, ?>, O, A, S> SingleAgentTask<C, O, A, S> fromEnvironment(
      DynamicalSystem<A, O, S> environment, A initialAction, DoubleRange tRange, double dT) {
    return agent -> {
      environment.reset();
      agent.reset();
      double t = tRange.min();
      Map<Double, Step<O, A, S>> steps = new HashMap<>();
      O observation = null;
      while (t <= tRange.max()) {
        if (observation == null) {
          observation = environment.step(t, initialAction);
        }
        A action = agent.step(t, observation);
        observation = environment.step(t, action);
        steps.put(t, new Step<>(observation, action, environment.getState()));
        t = t + dT;
      }
      return new TreeMap<>(steps);
    };
  }

  static <C extends DynamicalSystem<O, A, ?>, O, A, S> SingleAgentTask<C, O, A, S> fromEnvironment(
      Supplier<DynamicalSystem<A, O, S>> environmentSupplier, A initialAction, DoubleRange tRange, double dT) {
    return agent -> fromEnvironment(environmentSupplier.get(), initialAction, tRange, dT)
        .simulate(agent);
  }
}
