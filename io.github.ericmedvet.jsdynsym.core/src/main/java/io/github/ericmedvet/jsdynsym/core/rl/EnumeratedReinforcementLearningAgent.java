
package io.github.ericmedvet.jsdynsym.core.rl;

public interface EnumeratedReinforcementLearningAgent<S>
    extends ReinforcementLearningAgent<Integer, Integer, S> {

  int nOfInputs();

  int nOfOutputs();
}
