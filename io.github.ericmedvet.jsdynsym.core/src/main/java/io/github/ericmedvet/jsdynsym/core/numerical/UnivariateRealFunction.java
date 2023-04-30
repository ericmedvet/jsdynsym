package io.github.ericmedvet.jsdynsym.core.numerical;

import java.util.function.ToDoubleFunction;

/**
 * @author "Eric Medvet" on 2023/04/29 for jsdynsym
 */
public interface UnivariateRealFunction extends MultivariateRealFunction, ToDoubleFunction<double[]> {

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
    };
  }

  static UnivariateRealFunction from(MultivariateRealFunction multivariateRealFunction) {
    return from(xs -> multivariateRealFunction.compute(xs)[0], multivariateRealFunction.nOfInputs());
  }

  @Override
  default double[] compute(double... input) {
    return new double[]{applyAsDouble(input)};
  }

  @Override
  default int nOfOutputs() {
    return 1;
  }

  default UnivariateRealFunction scaledOutput(double slope, double intercept) {
    return UnivariateRealFunction.from(xs -> slope * applyAsDouble(xs) + intercept, nOfInputs());
  }
}
