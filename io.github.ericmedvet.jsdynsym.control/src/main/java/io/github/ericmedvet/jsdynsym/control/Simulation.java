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
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

public interface Simulation<T, S, O extends Simulation.Outcome<S>> {

  static <T, S, O extends Simulation.Outcome<S>> Simulation<T, S, Simulation.Outcome<S>> sequential(
      List<? extends Simulation<T, S, O>> simulations
  ) {
    return new Simulation<>() {
      @Override
      public Optional<T> example() {
        return simulations.getFirst().example();
      }

      @Override
      public Simulation.Outcome<S> simulate(T t, double dT, DoubleRange tRange) {
        double singleDuration = tRange.extent() / simulations.size();
        double currentInitT = tRange.min();
        List<O> outcomes = new ArrayList<>(simulations.size());
        for (Simulation<T, S, O> simulation : simulations) {
          DoubleRange localTRange = new DoubleRange(currentInitT, currentInitT + singleDuration);
          outcomes.add(simulation.simulate(t, dT, localTRange));
          currentInitT = currentInitT + singleDuration;
        }
        TreeMap<Double, S> snapshots = outcomes.stream()
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
        return simulations.toString();
      }
    };
  }

  O simulate(T t, double dT, DoubleRange tRange);

  default Optional<T> example() {
    return Optional.empty();
  }

  interface Outcome<S> {

    SortedMap<Double, S> snapshots();

    static <S> Outcome<S> of(SortedMap<Double, S> snapshots) {
      return () -> snapshots;
    }
  }
}