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

package io.github.ericmedvet.jsdynsym.problem;

import io.github.ericmedvet.jsdynsym.core.StatelessSystem;
import io.github.ericmedvet.jsdynsym.core.rl.EnumeratedTimeInvariantReinforcementLearningAgent;

import java.util.random.RandomGenerator;

public class RandomEnumeratedAgent implements EnumeratedTimeInvariantReinforcementLearningAgent<StatelessSystem.State> {
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
  public void reset() {
  }

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
