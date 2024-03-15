/*-
 * ========================LICENSE_START=================================
 * jsdynsym-core
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

package io.github.ericmedvet.jsdynsym.core.numerical;

import io.github.ericmedvet.jnb.datastructure.NamedFunction;
import java.util.Arrays;
import java.util.List;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.stream.IntStream;

public interface MultivariateRealFunction extends NumericalTimeInvariantStatelessSystem {

  double[] compute(double... input);

  static MultivariateRealFunction from(Function<double[], double[]> f, int nOfInputs, int nOfOutputs) {
    return new MultivariateRealFunction() {
      @Override
      public double[] compute(double... input) {
        return f.apply(input);
      }

      @Override
      public int nOfInputs() {
        return nOfInputs;
      }

      @Override
      public int nOfOutputs() {
        return nOfOutputs;
      }

      @Override
      public String toString() {
        return f.toString();
      }
    };
  }

  static List<String> varNames(String name, int number) {
    int digits = (int) Math.ceil(Math.log10(number + 1));
    return IntStream.range(1, number + 1)
        .mapToObj((name + "%0" + digits + "d")::formatted)
        .toList();
  }

  @Override
  default double[] step(double[] input) {
    return compute(input);
  }

  default MultivariateRealFunction andThen(MultivariateRealFunction other) {
    if (other.nOfInputs() != nOfOutputs()) {
      throw new IllegalArgumentException(
          "Incompatible input/output size: input=%d, output=%d".formatted(other.nOfInputs(), nOfOutputs()));
    }
    MultivariateRealFunction thisMrf = this;
    return MultivariateRealFunction.from(
        NamedFunction.from(in -> other.compute(thisMrf.compute(in)), this + "[then:%s]".formatted(other)),
        thisMrf.nOfInputs(),
        other.nOfOutputs());
  }

  default MultivariateRealFunction andThen(DoubleUnaryOperator f) {
    return MultivariateRealFunction.from(
        NamedFunction.from(in -> Arrays.stream(compute(in)).map(f).toArray(), "[all:%s]".formatted(f)),
        nOfInputs(),
        nOfOutputs());
  }
}
