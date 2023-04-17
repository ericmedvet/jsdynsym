package io.github.ericmedvet.jsdynsym.core.numerical;

import io.github.ericmedvet.jsdynsym.core.DynamicalSystem;

/**
 * @author "Eric Medvet" on 2023/02/25 for jsdynsym
 */
public interface NumericalDynamicalSystem<S> extends DynamicalSystem<double[], double[], S> {

  interface Composed<S> extends NumericalDynamicalSystem<S>,
      io.github.ericmedvet.jsdynsym.core.composed.Composed<NumericalDynamicalSystem<S>> {}

  int nOfInputs();

  int nOfOutputs();

  static <S1> NumericalDynamicalSystem<S1> from(
      DynamicalSystem<double[], double[], S1> inner,
      int nOfInputs,
      int nOfOutputs
  ) {
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
}
