/*
 * Copyright 2023 eric
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.ericmedvet.jsdynsym.core.rl;

import io.github.ericmedvet.jsdynsym.core.TimeInvariantDynamicalSystem;

public interface TimeInvariantReinforcementLearningAgent<I,O,S> extends ReinforcementLearningAgent<I,O,S>, TimeInvariantDynamicalSystem<ReinforcementLearningAgent.RewardedInput<I>, O, S> {

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
