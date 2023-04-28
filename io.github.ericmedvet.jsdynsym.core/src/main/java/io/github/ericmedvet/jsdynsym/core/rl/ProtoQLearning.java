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

import java.util.*;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ProtoQLearning implements EnumeratedTimeInvariantReinforcementLearningAgent<ProtoQLearning.State> {

  private final int nOfInputs;
  private final int nOfOutputs;
  private final double explorationRate;
  private final RandomGenerator randomGenerator;
  private final State state;
  private ObservationActionPair previousPair;

  public ProtoQLearning(int nOfInputs, int nOfOutputs, double explorationRate, RandomGenerator randomGenerator) {
    this.nOfInputs = nOfInputs;
    this.nOfOutputs = nOfOutputs;
    this.explorationRate = explorationRate;
    this.randomGenerator = randomGenerator;
    state = new State(nOfInputs, nOfOutputs, new HashMap<>());
  }

  public record ObservationActionPair(int observation, int action) {}

  public record State(int nOfInputs, int nOfOutputs, Map<ObservationActionPair, Double> table) {
    @Override
    public String toString() {
      return IntStream.range(0, nOfOutputs)
          .mapToObj(o -> IntStream.range(0, nOfInputs)
              .mapToObj(i -> "%5.1f".formatted(table().getOrDefault(new ObservationActionPair(i, o), Double.NaN)))
              .collect(Collectors.joining(" "))
          )
          .collect(Collectors.joining("\n"));
    }
  }

  @Override
  public State getState() {
    return state;
  }

  @Override
  public void reset() {
    state.table().clear();
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
    //update the state based on previous O-A pair
    if (previousPair != null) {
      state.table().merge(previousPair, reward, Double::sum);
    }
    //choose A based on current state
    int output;
    if (randomGenerator.nextDouble() < explorationRate) {
      //choose random action
      output = randomGenerator.nextInt(nOfOutputs);
    } else {
      //choose action based on the table
      List<Map.Entry<ObservationActionPair, Double>> oEntries = state.table().entrySet().stream()
          .filter(e -> e.getKey().observation() == input)
          .toList();
      Optional<Map.Entry<ObservationActionPair, Double>> oEntry = oEntries.stream()
          .max(Map.Entry.comparingByValue());
      if (oEntry.isEmpty()) {
        output = randomGenerator.nextInt(nOfOutputs);
      } else {
        double value = oEntry.get().getValue();
        if (value > 0 || oEntries.size() == nOfOutputs) {
          //the best action has a value greater than the default one (0)
          output = oEntry.get().getKey().action();
        } else {
          //choose a random action among the one never chosen
          List<Integer> chosenActions = oEntries.stream().map(e -> e.getKey().action()).toList();
          List<Integer> allActions = IntStream.range(0, nOfOutputs).boxed().toList();
          List<Integer> availableActions = new ArrayList<>(allActions);
          availableActions.removeAll(chosenActions);
          output = availableActions.get(randomGenerator.nextInt(availableActions.size()));
        }
      }
    }
    previousPair = new ObservationActionPair(input, output);
    return output;
  }

}
