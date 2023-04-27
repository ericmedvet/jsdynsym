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

public interface EnumeratedDiscreteTimeInvariantReinforcementLearningAgent<S> extends EnumeratedDiscreteReinforcementLearningAgent<S>, TimeInvariantDynamicalSystem<ReinforcementLearningAgent.RewardedInput<Integer>, Integer, S> {

  Integer step(double reward, Integer input);

  @Override
  default Integer step(double t, RewardedInput<Integer> rewardedInput) {
    return step(t, rewardedInput.reward(), rewardedInput.input());
  }

  @Override
  default Integer step(double t, double reward, Integer input) {
    return step(reward, input);
  }

  @Override
  default Integer step(RewardedInput<Integer> input) {
    return step(input.reward(), input.input());
  }
}
