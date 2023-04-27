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
import io.github.ericmedvet.jsdynsym.core.rl.ProtoQLearning;
import io.github.ericmedvet.jsdynsym.core.rl.ReinforcementLearningAgent;

import java.util.Random;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;

public class GuessTheNumber implements Runnable {

  private final int maxNumber;
  private final int target;
  private final int maxTrials;
  private final RandomGenerator randomGenerator;
  private final ReinforcementLearningAgent<Integer, Action, ?> player;
  public GuessTheNumber(
      int target,
      int maxNumber,
      int maxTrials,
      RandomGenerator randomGenerator,
      ReinforcementLearningAgent<Integer, Action, ?> player
  ) {
    this.target = target;
    this.maxNumber = maxNumber;
    this.maxTrials = maxTrials;
    this.randomGenerator = randomGenerator;
    this.player = player;
  }

  public enum Action {DEC, SAME, INC}

  public static class Adapter<S> implements ReinforcementLearningAgent<Integer, Action, S> {
    private final EnumeratedDiscreteTimeInvariantReinforcementLearningAgent<S> inner;

    public Adapter(int maxNumber, EnumeratedDiscreteTimeInvariantReinforcementLearningAgent<S> inner) {
      if (inner.nOfInputs() != maxNumber) {
        throw new IllegalArgumentException(
            "The inner agent expects an observation space with %d elements, instead of %d".formatted(
                inner.nOfInputs(),
                maxNumber
            ));
      }
      if (inner.nOfOutputs() != 3) {
        throw new IllegalArgumentException(
            "The inner agent expects an action space with %d elements, instead of 3".formatted(
                inner.nOfOutputs()
            ));
      }
      this.inner = inner;
    }

    @Override
    public S getState() {
      return inner.getState();
    }

    @Override
    public void reset() {
      inner.reset();
    }

    @Override
    public Action step(double t, double reward, Integer input) {
      Integer iAction = inner.step(reward, input);
      return switch (iAction) {
        case 0 -> Action.DEC;
        case 1 -> Action.SAME;
        case 2 -> Action.INC;
        default -> throw new IllegalArgumentException("Unmappable action");
      };
    }
  }

  public static void main(String[] args) {
    int max = 10;
    int maxTrials = 100;
    RandomGenerator rg = new Random();
    //EnumeratedDiscreteTimeInvariantReinforcementLearningAgent<?> agent = new RandomEnumeratedDiscreteAgent(max, 3,
    // rg);
    EnumeratedDiscreteTimeInvariantReinforcementLearningAgent<?> agent = new ProtoQLearning(max, 3, 0.1, rg);
    System.out.println(agent.getState());
    IntStream.range(0, 1000).forEach(i -> {
      GuessTheNumber game = new GuessTheNumber(max / 2, max, maxTrials, rg, new Adapter<>(max, agent));
      game.run();
    });
    System.out.println(agent.getState());
  }

  @Override
  public void run() {
    int currentNumber = randomGenerator.nextInt(maxNumber);
    System.out.printf("Starting game with target %d from %d%n", target, currentNumber);
    int trials = 0;
    double previousReward = 0d;
    double cumulativeReward = 0d;
    while (trials < maxTrials) {
      trials = trials + 1;
      Action action = player.step(trials, previousReward, currentNumber);
      if (currentNumber == target) {
        break;
      }
      //System.out.printf("Current number: %d\tAgent action: %s%n", currentNumber, action);
      currentNumber = currentNumber + switch (action) {
        case DEC -> -1;
        case SAME -> 0;
        case INC -> +1;
      };
      currentNumber = Math.max(0, Math.min(currentNumber, maxNumber - 1));
      cumulativeReward = cumulativeReward + previousReward;
      previousReward = 1 - Math.abs(currentNumber - target);
      //previousReward = currentNumber == target ? 1 : -0.1;
    }
    System.out.printf("Game ended in %d trials with %d=%d and cumulative reward of %f%n".formatted(
        trials,
        currentNumber,
        target,
        cumulativeReward
    ));
  }
}
