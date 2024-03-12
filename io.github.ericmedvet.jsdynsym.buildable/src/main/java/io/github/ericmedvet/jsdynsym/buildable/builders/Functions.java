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

import io.github.ericmedvet.jnb.core.Discoverable;
import io.github.ericmedvet.jnb.core.Param;
import io.github.ericmedvet.jnb.datastructure.FormattedNamedFunction;
import io.github.ericmedvet.jnb.datastructure.NamedFunction;
import io.github.ericmedvet.jsdynsym.control.Simulation;
import io.github.ericmedvet.jsdynsym.core.numerical.ann.MultiLayerPerceptron;
import java.util.SortedMap;
import java.util.function.Function;

@Discoverable(prefixTemplate = "dynamicalSystem|dynSys|ds.function|f")
public class Functions {
  private Functions() {}

  @SuppressWarnings("unused")
  public static <X> FormattedNamedFunction<X, Double> doubleOp(
      @Param(value = "of", dNPM = "f.identity()") Function<X, Double> beforeF,
      @Param(value = "activationF", dS = "identity") MultiLayerPerceptron.ActivationFunction activationF) {

    Function<Double, Double> f = activationF::applyAsDouble;
    return FormattedNamedFunction.from(f, "%.1f", activationF.name().toLowerCase())
        .compose(beforeF);
  }

  @SuppressWarnings("unused")
  public static <X, S> NamedFunction<X, SortedMap<Double, S>> simOutcome(
      @Param(value = "of", dNPM = "f.identity()") Function<X, Simulation.Outcome<S>> beforeF,
      @Param(value = "format", dS = "%s") String format) {
    Function<Simulation.Outcome<S>, SortedMap<Double, S>> f = Simulation.Outcome::snapshots;
    return NamedFunction.from(f, "sim.outcome").compose(beforeF);
  }
}
