package io.github.ericmedvet.jsdynsym.core.numerical;

import io.github.ericmedvet.jsdynsym.core.DynamicalSystem;

/**
 * @author "Eric Medvet" on 2023/02/25 for jsdynsym
 */
public interface NumericalDynamicalSystem<S> extends DynamicalSystem<double[], double[], S> {
  int nOfInputs();

  int nOfOutputs();

  static <S1> NumericalDynamicalSystem<S1> from(
      DynamicalSystem<double[], double[], S1> inner,
      int nOfInputs,
      int nOfOutputs
  ) {
    return new NumericalDynamicalSystem<S1>() {
      @Override
      public int nOfInputs() {
        return nOfInputs;
      }

      @Override
      public int nOfOutputs() {
        return nOfOutputs;
      }

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
    };
  }
}
