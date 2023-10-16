
/*
 * Copyright 2023 eric
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

package io.github.ericmedvet.jsdynsym.core.numerical;

import io.github.ericmedvet.jsdynsym.core.DynamicalSystem;

public interface NumericalDynamicalSystem<S> extends DynamicalSystem<double[], double[], S> {

  interface Composed<S>
      extends NumericalDynamicalSystem<S>,
          io.github.ericmedvet.jsdynsym.core.composed.Composed<NumericalDynamicalSystem<S>> {}

  int nOfInputs();

  int nOfOutputs();

  static <S1> NumericalDynamicalSystem<S1> from(
      DynamicalSystem<double[], double[], S1> inner, int nOfInputs, int nOfOutputs) {
    if (inner instanceof io.github.ericmedvet.jsdynsym.core.composed.Composed<?> composed) {
      return new Composed<>() {
        @Override
        public S1 getState() {
          return inner.getState();
        }

        @Override
        public void reset() {
          inner.reset();
        }

        @Override
        public double[] step(double t, double[] input) {
          return inner.step(t, input);
        }

        @Override
        public NumericalDynamicalSystem<S1> inner() {
          //noinspection unchecked
          return (NumericalDynamicalSystem<S1>) composed.inner();
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
          return inner.toString();
        }
      };
    }
    return new NumericalDynamicalSystem<>() {
      @Override
      public S1 getState() {
        return inner.getState();
      }

      @Override
      public void reset() {
        inner.reset();
      }

      @Override
      public double[] step(double t, double[] input) {
        return inner.step(t, input);
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
        return inner.toString();
      }
    };
  }

  default void checkDimension(int nOfInputs, int nOfOutputs) {
    if (nOfInputs() != nOfInputs) {
      throw new IllegalArgumentException(
          "Wrong number of inputs: %d found, %d expected".formatted(nOfInputs(), nOfInputs));
    }
    if (nOfOutputs() != nOfOutputs) {
      throw new IllegalArgumentException(
          "Wrong number of outputs: %d found, %d expected".formatted(nOfOutputs(), nOfOutputs));
    }
  }
}
