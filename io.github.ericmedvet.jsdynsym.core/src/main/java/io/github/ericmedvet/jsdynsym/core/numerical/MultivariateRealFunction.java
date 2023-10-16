
package io.github.ericmedvet.jsdynsym.core.numerical;

import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

public interface MultivariateRealFunction extends NumericalTimeInvariantStatelessSystem {

  double[] compute(double... input);

  static MultivariateRealFunction from(
      Function<double[], double[]> f, int nOfInputs, int nOfOutputs) {
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
}
