/*-
 * ========================LICENSE_START=================================
 * jsdynsym-core
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

package io.github.ericmedvet.jsdynsym.core.rl;

import io.github.ericmedvet.jnb.datastructure.Composed;
import io.github.ericmedvet.jnb.datastructure.Copyable;
import io.github.ericmedvet.jsdynsym.core.numerical.NumericalDynamicalSystem;

public interface NumericalReinforcementLearningAgent<S> extends ReinforcementLearningAgent<double[], double[], S> {

  int nOfInputs();

  int nOfOutputs();

  default void checkDimension(int nOfInputs, int nOfOutputs) {
    if (nOfInputs() != nOfInputs) {
      throw new IllegalArgumentException(
          "Wrong number of inputs: %d found, %d expected".formatted(nOfInputs(), nOfInputs)
      );
    }
    if (nOfOutputs() != nOfOutputs) {
      throw new IllegalArgumentException(
          "Wrong number of outputs: %d found, %d expected".formatted(nOfOutputs(), nOfOutputs)
      );
    }
  }

  static <S> NumericalReinforcementLearningAgent<S> from(NumericalDynamicalSystem<S> dynamicalSystem) {
    record HardNRLA<S>(
        NumericalDynamicalSystem<S> numericalDynamicalSystem
    ) implements NumericalReinforcementLearningAgent<S>, FrozenableNumericalRLAgent<S>, Composed<NumericalDynamicalSystem<S>>, Copyable<HardNRLA<S>> {

      @Override
      public HardNRLA<S> copyOf() {
        if (numericalDynamicalSystem instanceof Copyable<?> copyableNDS) {
          //noinspection unchecked
          return new HardNRLA<>((NumericalDynamicalSystem<S>) copyableNDS.copyOf());
        }
        throw new UnsupportedOperationException("Inner dynamical system is not copyable");
      }

      @Override
      public NumericalDynamicalSystem<S> inner() {
        return numericalDynamicalSystem;
      }

      @Override
      public NumericalDynamicalSystem<?> dynamicalSystem() {
        return numericalDynamicalSystem;
      }

      @Override
      public int nOfInputs() {
        return numericalDynamicalSystem.nOfInputs();
      }

      @Override
      public int nOfOutputs() {
        return numericalDynamicalSystem.nOfOutputs();
      }

      @Override
      public double[] step(double t, double[] input, double reward) {
        return numericalDynamicalSystem.step(t, input);
      }

      @Override
      public S getState() {
        return numericalDynamicalSystem.getState();
      }

      @Override
      public void reset() {
        numericalDynamicalSystem.reset();
      }

      @Override
      public String toString() {
        return numericalDynamicalSystem.toString();
      }
    }
    return new HardNRLA<>(dynamicalSystem);
  }

}