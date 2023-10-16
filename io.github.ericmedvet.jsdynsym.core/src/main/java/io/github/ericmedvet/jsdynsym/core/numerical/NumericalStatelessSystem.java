/*-
 * ========================LICENSE_START=================================
 * jsdynsym-core
 * %%
 * Copyright (C) 2023 Eric Medvet
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

import io.github.ericmedvet.jsdynsym.core.StatelessSystem;
import java.util.function.BiFunction;

public interface NumericalStatelessSystem
    extends NumericalDynamicalSystem<StatelessSystem.State>, StatelessSystem<double[], double[]> {

  @SuppressWarnings("unused")
  static NumericalStatelessSystem from(
      int nOfInputs, int nOfOutputs, BiFunction<Double, double[], double[]> function) {
    return new NumericalStatelessSystem() {
      @Override
      public int nOfInputs() {
        return nOfInputs;
      }

      @Override
      public int nOfOutputs() {
        return nOfOutputs;
      }

      @Override
      public double[] step(double t, double[] input) {
        if (input.length != nOfInputs) {
          throw new IllegalArgumentException(
              String.format("Unsupported input size: %d instead of %d", input.length, nOfInputs));
        }
        double[] output = function.apply(t, input);
        if (output.length != nOfOutputs) {
          throw new IllegalArgumentException(
              String.format(
                  "Unsupported output size: %d instead of %d", output.length, nOfOutputs));
        }
        return output;
      }
    };
  }

  @SuppressWarnings("unused")
  static NumericalStatelessSystem zeros(int nOfInputs, int nOfOutputs) {
    return from(nOfInputs, nOfOutputs, (t, in) -> new double[nOfOutputs]);
  }
}
