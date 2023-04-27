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

import io.github.ericmedvet.jsdynsym.core.rl.EnumeratedDiscreteTimeInvariantReinforcementLearningAgent;

import java.util.random.RandomGenerator;

public class RandomEnumeratedDiscreteAgent implements EnumeratedDiscreteTimeInvariantReinforcementLearningAgent<Void> {
  private final int nOfInputs;
  private final int nOfOutputs;
  private final RandomGenerator randomGenerator;

  public RandomEnumeratedDiscreteAgent(int nOfInputs, int nOfOutputs, RandomGenerator randomGenerator) {
    this.nOfInputs = nOfInputs;
    this.nOfOutputs = nOfOutputs;
    this.randomGenerator = randomGenerator;
  }

  @Override
  public Void getState() {
    return null;
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
  public Integer step(double reward, Integer input) {
    return randomGenerator.nextInt(nOfOutputs);
  }
}
