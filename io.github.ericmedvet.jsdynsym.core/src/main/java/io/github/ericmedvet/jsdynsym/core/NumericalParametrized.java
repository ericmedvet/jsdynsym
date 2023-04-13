package io.github.ericmedvet.jsdynsym.core;

import java.util.random.RandomGenerator;

public interface NumericalParametrized extends Parametrized<double[]> {
  default void randomize(RandomGenerator randomGenerator, DoubleRange range) {
    double[] oldParams = getParams();
    double[] newParams = new double[oldParams.length];
    for (int i = 0; i < newParams.length; i++) {
      newParams[i] = range.denormalize(randomGenerator.nextDouble());
    }
    setParams(newParams);
  }
}
