
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
