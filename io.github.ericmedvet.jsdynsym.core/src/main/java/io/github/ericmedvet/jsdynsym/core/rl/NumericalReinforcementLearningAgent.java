
package io.github.ericmedvet.jsdynsym.core.rl;

public interface NumericalReinforcementLearningAgent<S>
    extends ReinforcementLearningAgent<double[], double[], S> {

  int nOfInputs();

  int nOfOutputs();

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
