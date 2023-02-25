package io.github.ericmedvet.jsdynsym.core;

/**
 * @author "Eric Medvet" on 2023/02/25 for jsdynsym
 */
public interface ReinforcementLearningAgent<I, O, S> extends DynamicalSystem<ReinforcementLearningAgent.RewardedInput<I>, O, S> {
  record RewardedInput<I>(I input, double reward) {}

  O step(double t, double reward, I input);

  @Override
  default O step(double t, RewardedInput<I> rewardedInput) {
    return step(t, rewardedInput.reward(), rewardedInput.input());
  }
}
