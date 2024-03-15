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

import java.util.function.DoubleUnaryOperator;
import java.util.function.ToDoubleFunction;

public interface UnivariateRealFunction extends MultivariateRealFunction, ToDoubleFunction<double[]> {

  record NamedToDoubleFunction<T>(ToDoubleFunction<T> f, String name) implements ToDoubleFunction<T> {
    @Override
    public String toString() {
      return name;
    }

    @Override
    public double applyAsDouble(T in) {
      return f.applyAsDouble(in);
    }
  }

  static UnivariateRealFunction from(ToDoubleFunction<double[]> f, int nOfInputs) {
    return new UnivariateRealFunction() {
      @Override
      public double applyAsDouble(double[] input) {
        return f.applyAsDouble(input);
      }

      @Override
      public int nOfInputs() {
        return nOfInputs;
      }

      @Override
      public String toString() {
        return f.toString();
      }
    };
  }

  static UnivariateRealFunction from(MultivariateRealFunction multivariateRealFunction) {
    return from(
        new NamedToDoubleFunction<>(
            (double[] xs) -> multivariateRealFunction.compute(xs)[0], multivariateRealFunction.toString()),
        multivariateRealFunction.nOfInputs());
  }

  @Override
  default double[] compute(double... input) {
    return new double[] {applyAsDouble(input)};
  }

  @Override
  default int nOfOutputs() {
    return 1;
  }

  default UnivariateRealFunction scaledOutput(double slope, double intercept) {
    return from(
        new NamedToDoubleFunction<>(
            (double[] xs) -> slope * applyAsDouble(xs) + intercept,
            this + "[scaled:m=%f;q=%f]".formatted(slope, intercept)),
        nOfInputs());
  }

  @Override
  default UnivariateRealFunction andThen(DoubleUnaryOperator f) {
    return from(
        new NamedToDoubleFunction<>(
            (double[] xs) -> f.applyAsDouble(applyAsDouble(xs)), this + "[then:%s]".formatted(f)),
        nOfInputs());
  }
}
