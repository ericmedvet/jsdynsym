
package io.github.ericmedvet.jsdynsym.core.rl;

import io.github.ericmedvet.jsdynsym.core.TimeInvariantDynamicalSystem;

public interface TimeInvariantReinforcementLearningAgent<I, O, S>
    extends ReinforcementLearningAgent<I, O, S>,
        TimeInvariantDynamicalSystem<ReinforcementLearningAgent.RewardedInput<I>, O, S> {

  O step(I input, double reward);

  @Override
  default O step(RewardedInput<I> rewardedInput) {
    return step(rewardedInput.input(), rewardedInput.reward());
  }

  @Override
  default O step(double t, I input, double reward) {
    return step(input, reward);
  }

  @Override
  default O step(double t, RewardedInput<I> rewardedInput) {
    return step(rewardedInput);
  }
}
