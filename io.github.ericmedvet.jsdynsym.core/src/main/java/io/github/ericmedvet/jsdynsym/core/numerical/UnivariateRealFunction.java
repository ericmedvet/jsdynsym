package io.github.ericmedvet.jsdynsym.core.numerical;

import java.util.function.ToDoubleFunction;

/**
 * @author "Eric Medvet" on 2023/04/29 for jsdynsym
 */
public interface UnivariateRealFunction extends MultivariateRealFunction, ToDoubleFunction<double[]> {
  static UnivariateRealFunction from(ToDoubleFunction<double[]> f, int nOfInputs) {
    return new UnivariateRealFunction() {
      @Override
      public int nOfInputs() {
        return nOfInputs;
      }

      @Override
      public double[] step(double[] input) {
        return new double[]{f.applyAsDouble(input)};
      }
    };
  }

  @Override
  default double applyAsDouble(double[] value) {
    return apply(value)[0];
  }

  @Override
  default int nOfOutputs() {
    return 1;
  }
}
