
package io.github.ericmedvet.jsdynsym.core.rl;

import io.github.ericmedvet.jsdynsym.core.DynamicalSystem;

public interface ReinforcementLearningAgent<I, O, S>
    extends DynamicalSystem<ReinforcementLearningAgent.RewardedInput<I>, O, S> {
  record RewardedInput<I>(I input, double reward) {}

  O step(double t, I input, double reward);

  @Override
  default O step(double t, RewardedInput<I> rewardedInput) {
    return step(t, rewardedInput.input(), rewardedInput.reward());
  }
}
