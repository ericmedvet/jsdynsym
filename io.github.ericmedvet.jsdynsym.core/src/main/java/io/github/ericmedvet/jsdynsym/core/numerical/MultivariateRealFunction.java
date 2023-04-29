package io.github.ericmedvet.jsdynsym.core.numerical;

import java.util.function.Function;

/**
 * @author "Eric Medvet" on 2023/04/29 for jsdynsym
 */
public interface MultivariateRealFunction extends NumericalTimeInvariantStatelessSystem {
  static MultivariateRealFunction from(Function<double[], double[]> f, int nOfInputs, int nOfOutputs) {
    return new MultivariateRealFunction() {
      @Override
      public int nOfInputs() {
        return nOfInputs;
      }

      @Override
      public int nOfOutputs() {
        return nOfOutputs;
      }

      @Override
      public double[] step(double[] input) {
        return f.apply(input);
      }
    };
  }

}
