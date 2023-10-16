
package io.github.ericmedvet.jsdynsym.problem;

import io.github.ericmedvet.jsdynsym.core.StatelessSystem;
import io.github.ericmedvet.jsdynsym.core.rl.EnumeratedTimeInvariantReinforcementLearningAgent;

import java.util.random.RandomGenerator;

public class RandomEnumeratedAgent
    implements EnumeratedTimeInvariantReinforcementLearningAgent<StatelessSystem.State> {
  private final int nOfInputs;
  private final int nOfOutputs;
  private final RandomGenerator randomGenerator;

  public RandomEnumeratedAgent(int nOfInputs, int nOfOutputs, RandomGenerator randomGenerator) {
    this.nOfInputs = nOfInputs;
    this.nOfOutputs = nOfOutputs;
    this.randomGenerator = randomGenerator;
  }

  @Override
  public StatelessSystem.State getState() {
    return StatelessSystem.State.EMPTY;
  }

  @Override
  public void reset() {}

  @Override
  public int nOfInputs() {
    return nOfInputs;
  }

  @Override
  public int nOfOutputs() {
    return nOfOutputs;
  }

  @Override
  public Integer step(Integer input, double reward) {
    return randomGenerator.nextInt(nOfOutputs);
  }
}
