/*-
 * ========================LICENSE_START=================================
 * jsdynsym-core
 * %%
 * Copyright (C) 2023 - 2025 Eric Medvet
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package io.github.ericmedvet.jsdynsym.core.rl;

import io.github.ericmedvet.jnb.datastructure.Copyable;
import io.github.ericmedvet.jnb.datastructure.DoubleRange;
import io.github.ericmedvet.jnb.datastructure.NumericalParametrized;
import io.github.ericmedvet.jnb.datastructure.Parametrized;
import io.github.ericmedvet.jsdynsym.core.numerical.FrozenableNumericalDynamicalSystem;
import io.github.ericmedvet.jsdynsym.core.numerical.NumericalStatelessSystem;
import io.github.ericmedvet.jsdynsym.core.numerical.ann.HebbianMultiLayerPerceptron;
import io.github.ericmedvet.jsdynsym.core.numerical.ann.MLPUtils;
import io.github.ericmedvet.jsdynsym.core.numerical.ann.MultiLayerPerceptron;
import io.github.ericmedvet.jsdynsym.core.numerical.named.NamedUnivariateRealFunction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

public class FreeFormPlasticMLPRLAgent implements NumericalTimeInvariantReinforcementLearningAgent<FreeFormPlasticMLPRLAgent.State>, Parametrized<FreeFormPlasticMLPRLAgent, NamedUnivariateRealFunction>, FrozenableNumericalRLAgent<FreeFormPlasticMLPRLAgent.State>, Copyable<FreeFormPlasticMLPRLAgent> {
  private static final String AVERAGE = "average";
  private static final String STD_DEV = "stdDev";
  private static final String CURRENT = "current";
  private static final String TREND = "trend";
  private static final String ACTIVATION = "activation";
  private static final String AGE = "age";
  private static final String PRE_SYNAPTIC_NEURON_INDEX = "preSynapticNeuronIdx";
  private static final String POST_SYNAPTIC_NEURON_INDEX = "postSynapticNeuronIdx";
  private static final String LAYER_INDEX = "layerIdx";
  private final double[] biasActivationsHistory;
  private final MultiLayerPerceptron.ActivationFunction activationFunction;
  private final int weightsUpdateInterval;
  private final int[] neurons;
  private final int historyLength;
  private final DoubleRange initialWeightRange;
  private final HebbianMultiLayerPerceptron.WeightInitializationType weightInitializationType;
  private final RandomGenerator randomGenerator;
  private final DoubleRange weightRange;
  private NamedUnivariateRealFunction plasticityFunction;
  private int stepCounter;
  private State state;

  public FreeFormPlasticMLPRLAgent(
      MultiLayerPerceptron.ActivationFunction activationFunction,
      NamedUnivariateRealFunction plasticityFunction,
      int[] neurons,
      int historyLength,
      int weightsUpdateInterval,
      HebbianMultiLayerPerceptron.WeightInitializationType weightInitializationType,
      DoubleRange initialWeightRange,
      double maxWeightMagnitude,
      RandomGenerator randomGenerator
  ) {
    if (weightInitializationType.equals(HebbianMultiLayerPerceptron.WeightInitializationType.PARAMS)) {
      throw new IllegalArgumentException("Unsupported weight initialization type.");
    }
    this.activationFunction = activationFunction;
    this.plasticityFunction = plasticityFunction;
    this.neurons = neurons;
    this.historyLength = historyLength;
    this.weightsUpdateInterval = weightsUpdateInterval;
    this.weightInitializationType = weightInitializationType;
    this.initialWeightRange = initialWeightRange;
    this.weightRange = new DoubleRange(-maxWeightMagnitude, maxWeightMagnitude);
    this.biasActivationsHistory = new double[historyLength];
    this.randomGenerator = randomGenerator;
    Arrays.fill(biasActivationsHistory, 1.0);
    reset();
  }

  public FreeFormPlasticMLPRLAgent(
      MultiLayerPerceptron.ActivationFunction activationFunction,
      NamedUnivariateRealFunction plasticityFunction,
      int nOfInput,
      int[] innerNeurons,
      int nOfOutput,
      int historyLength,
      int weightsUpdateInterval,
      HebbianMultiLayerPerceptron.WeightInitializationType weightInitializationType,
      DoubleRange initialWeightRange,
      double maxWeightMagnitude,
      RandomGenerator randomGenerator
  ) {
    this(
        activationFunction,
        plasticityFunction,
        MLPUtils.countNeurons(nOfInput, innerNeurons, nOfOutput),
        historyLength,
        weightsUpdateInterval,
        weightInitializationType,
        initialWeightRange,
        maxWeightMagnitude,
        randomGenerator
    );
  }

  private static double[][][] emptyActivations(int historyLength, int[] neurons) {
    double[][][] emptyActivations = new double[neurons.length][][];
    for (int i = 0; i < neurons.length; i++) {
      emptyActivations[i] = new double[neurons[i]][];
      for (int j = 0; j < neurons[i]; j++) {
        emptyActivations[i][j] = new double[historyLength];
      }
    }
    return emptyActivations;
  }

  public static List<String> getVariableNames() {
    String[] statisticTypes = {AVERAGE, STD_DEV, CURRENT, TREND};
    List<String> variableNames = new ArrayList<>();
    for (Statistics.StatisticsScope ss : Statistics.StatisticsScope.values()) {
      for (String st : statisticTypes) {
        if (ss.equals(Statistics.StatisticsScope.REWARD)) {
          variableNames.add(String.format("%s_%s", st, Statistics.StatisticsScope.REWARD));
        } else {
          variableNames.add(String.format("%s_%s_%s", st, ss, ACTIVATION));
        }
      }
    }
    variableNames.add(LAYER_INDEX);
    variableNames.add(POST_SYNAPTIC_NEURON_INDEX);
    variableNames.add(PRE_SYNAPTIC_NEURON_INDEX);
    variableNames.add(AGE);
    return variableNames;
  }

  private static StateAndOutput step(
      double[] input,
      double reward,
      State state,
      MultiLayerPerceptron.ActivationFunction activationFunction,
      NamedUnivariateRealFunction plasticityFunction,
      int[] neurons,
      DoubleRange weightRange,
      double[] biasActivationsHistory,
      boolean isUpdateStep
  ) {
    long age = state.age;
    double[][][] newWeights = state.weights;
    if (age > 0 && isUpdateStep) {
      Map<String, Double> inputParameters = new HashMap<>();
      // statistics network wise
      inputParameters.put(AGE, (double) age);
      Statistics.from(state.rewardsHistory, age - 1).insert(inputParameters, Statistics.StatisticsScope.REWARD);
      Statistics.from(state.networkHistory, age - 1).insert(inputParameters, Statistics.StatisticsScope.NETWORK);
      for (int i = 1; i < neurons.length; i++) {
        // statistics layer wise
        Statistics.from(state.layersHistory[i], age - 1)
            .insert(inputParameters, Statistics.StatisticsScope.LAYER_POST);
        Statistics.from(state.layersHistory[i - 1], age - 1)
            .insert(inputParameters, Statistics.StatisticsScope.LAYER_PRE);
        for (int j = 0; j < newWeights[i - 1].length; j++) {
          // statistics post synapse
          Statistics.from(state.activationsHistory[i][j], age - 1)
              .insert(inputParameters, Statistics.StatisticsScope.NEURON_POST);
          for (int k = 0; k < newWeights[i - 1][j].length; k++) {
            // statistics pre synapse
            Statistics.from((k == 0) ? biasActivationsHistory : state.activationsHistory[i - 1][k - 1], age - 1)
                .insert(inputParameters, Statistics.StatisticsScope.NEURON_PRE);
            inputParameters.put(LAYER_INDEX, (double) i);
            inputParameters.put(PRE_SYNAPTIC_NEURON_INDEX, (double) k);
            inputParameters.put(POST_SYNAPTIC_NEURON_INDEX, (double) j);
            // update weights
            newWeights[i - 1][j][k] = weightRange.clip(
                newWeights[i - 1][j][k] + plasticityFunction.computeAsDouble(inputParameters)
            );
          }
        }
      }
    }
    // compute output
    double[][] newActivations = MLPUtils.computeActivations(
        input,
        newWeights,
        activationFunction,
        state.getCurrentAgeActivations()
    );
    return new StateAndOutput(
        state.update(newActivations, reward),
        newActivations[neurons.length - 1]
    );
  }

  @Override
  public FreeFormPlasticMLPRLAgent copyOf() {
    NamedUnivariateRealFunction copyOfPlasticityFunction = plasticityFunction;
    if (plasticityFunction instanceof Copyable<?> copyable) {
      copyOfPlasticityFunction = (NamedUnivariateRealFunction) copyable.copyOf();
    }
    FreeFormPlasticMLPRLAgent copy = new FreeFormPlasticMLPRLAgent(
        activationFunction,
        copyOfPlasticityFunction,
        MLPUtils.copy1D(neurons),
        historyLength,
        weightsUpdateInterval,
        weightInitializationType,
        initialWeightRange,
        weightRange.max(),
        randomGenerator
    );
    copy.stepCounter = stepCounter;
    copy.state = state.copyOf();
    return copy;
  }

  @Override
  public FrozenableNumericalDynamicalSystem<?> dynamicalSystem() {
    final State initialState = state.copyOf();
    return new FrozenableNumericalDynamicalSystem<State>() {
      private State innerState = initialState;
      private int innerStepCounter;

      @Override
      public State getState() {
        return innerState;
      }

      @Override
      public int nOfInputs() {
        return neurons[0];
      }

      @Override
      public int nOfOutputs() {
        return neurons[neurons.length - 1];
      }

      @Override
      public void reset() {
        innerStepCounter = 0;
        innerState = initialState;
      }

      @Override
      public NumericalStatelessSystem stateless() {
        return new MultiLayerPerceptron(
            activationFunction,
            MLPUtils.copy3D(innerState.weights),
            MLPUtils.copy1D(neurons)
        );
      }

      @Override
      public double[] step(double t, double[] input) {
        boolean isUpdateStep = innerStepCounter > 0 && innerStepCounter % weightsUpdateInterval == 0;
        StateAndOutput step = FreeFormPlasticMLPRLAgent.step(
            input,
            0,
            innerState,
            activationFunction,
            plasticityFunction,
            neurons,
            weightRange,
            biasActivationsHistory,
            isUpdateStep
        );
        innerStepCounter += 1;
        innerState = step.state;
        return step.output;
      }
    };
  }

  @Override
  public NamedUnivariateRealFunction getParams() {
    return plasticityFunction;
  }

  @Override
  public void setParams(NamedUnivariateRealFunction namedUnivariateRealFunction) {
    plasticityFunction = namedUnivariateRealFunction;
  }

  @Override
  public State getState() {
    return state;
  }

  @Override
  public int nOfInputs() {
    return neurons[0];
  }

  @Override
  public int nOfOutputs() {
    return neurons[neurons.length - 1];
  }

  @Override
  public void reset() {
    stepCounter = 0;
    state = new State(
        0,
        MLPUtils.zeroWeights(neurons),
        emptyActivations(historyLength, neurons),
        new double[historyLength],
        new double[emptyActivations(historyLength, neurons).length][historyLength],
        new double[historyLength]
    );
    if (weightInitializationType.equals(HebbianMultiLayerPerceptron.WeightInitializationType.RANDOM)) {
      for (int i = 1; i < neurons.length; i++) {
        for (int j = 0; j < neurons[i]; j++) {
          for (int k = 0; k < neurons[i - 1] + 1; k++) {
            state.weights[i - 1][j][k] = initialWeightRange.denormalize(randomGenerator.nextDouble());
          }
        }
      }
    }
  }

  @Override
  public double[] step(double[] input, double reward) {
    boolean isUpdateStep = stepCounter > 0 && stepCounter % weightsUpdateInterval == 0;
    StateAndOutput step = step(
        input,
        reward,
        state,
        activationFunction,
        plasticityFunction,
        neurons,
        weightRange,
        biasActivationsHistory,
        isUpdateStep
    );
    stepCounter += 1;
    state = step.state;
    return step.output;
  }

  @Override
  public String toString() {
    return "Free-Form Plastic MLP RL Agent-%s-%s"
        .formatted(
            activationFunction.toString().toLowerCase(),
            Arrays.stream(neurons).mapToObj(Integer::toString).collect(Collectors.joining(">"))
        );
  }

  public record State(
      long age,
      double[][][] weights,
      double[][][] activationsHistory,
      double[] rewardsHistory,
      double[][] layersHistory,
      double[] networkHistory
  ) implements NumericalParametrized<State>, Copyable<State> {
    @Override
    public State copyOf() {
      return new State(
          age,
          MLPUtils.copy3D(weights),
          MLPUtils.copy3D(activationsHistory),
          MLPUtils.copy1D(rewardsHistory),
          MLPUtils.copy2D(layersHistory),
          MLPUtils.copy1D(networkHistory)
      );
    }

    public double[][] getCurrentAgeActivations() {
      int historyIndex = (int) (age % rewardsHistory.length);
      double[][] activations = new double[activationsHistory.length][];
      for (int i = 0; i < activations.length; i++) {
        activations[i] = new double[activationsHistory[i].length];
        for (int j = 0; j < activationsHistory[i].length; j++) {
          activations[i][j] = activationsHistory[i][j][historyIndex];
        }
      }
      return activations;
    }

    @Override
    public double[] getParams() {
      return MLPUtils.flat(weights);
    }

    @Override
    public void setParams(double[] param) {
      throw new UnsupportedOperationException("Params cannot be set this way");
    }

    public State update(double[][] newActivations, double reward) {
      int nOfNeurons = 0;
      int historyIndex = (int) (age % rewardsHistory.length);
      networkHistory[historyIndex] = 0;
      for (int i = 0; i < newActivations.length; i++) {
        layersHistory[i][historyIndex] = 0;
        for (int j = 0; j < newActivations[i].length; j++) {
          activationsHistory[i][j][historyIndex] = newActivations[i][j];
          layersHistory[i][historyIndex] += newActivations[i][j];
          networkHistory[historyIndex] += newActivations[i][j];
        }
        layersHistory[i][historyIndex] /= activationsHistory[i].length;
        nOfNeurons += activationsHistory[i].length;
      }
      networkHistory[historyIndex] /= nOfNeurons;
      rewardsHistory[historyIndex] = reward;
      return new State(age + 1, weights, activationsHistory, rewardsHistory, layersHistory, networkHistory);
    }
  }

  private record StateAndOutput(State state, double[] output) {
  }

  private record Statistics(
      double current,
      double trend,
      double average,
      double stdDev
  ) {

    private static Statistics from(double[] history, long age) {
      int n = history.length;
      int currentIdx = (int) (age) % n;
      int oldestIdx = (age < n) ? 0 : (int) (age + 1) % n;
      double avg = 0;
      for (double v : history) {
        avg += v;
      }
      avg /= n;
      double numerator = 0;
      for (double v : history) {
        numerator += (v - avg) * (v - avg);
      }
      double stdDev = Math.sqrt(numerator / n);
      double current = history[currentIdx];
      double trend = current - history[oldestIdx]; // newest - oldest
      return new Statistics(current, trend, avg, stdDev);
    }

    private void insert(Map<String, Double> container, StatisticsScope statisticsScope) {
      if (statisticsScope.equals(StatisticsScope.REWARD)) {
        container.put(AVERAGE + "_" + statisticsScope, average);
        container.put(STD_DEV + "_" + statisticsScope, stdDev);
        container.put(CURRENT + "_" + statisticsScope, current);
        container.put(TREND + "_" + statisticsScope, trend);
      } else {
        container.put(AVERAGE + "_" + statisticsScope + "_" + ACTIVATION, average);
        container.put(STD_DEV + "_" + statisticsScope + "_" + ACTIVATION, stdDev);
        container.put(CURRENT + "_" + statisticsScope + "_" + ACTIVATION, current);
        container.put(TREND + "_" + statisticsScope + "_" + ACTIVATION, trend);
      }
    }

    private enum StatisticsScope {
      NEURON_POST("neuronPost"), NEURON_PRE("neuronPre"), LAYER_POST("layerPost"), LAYER_PRE("layerPre"), NETWORK(
          "network"
      ), REWARD("reward");

      private final String name;

      StatisticsScope(String name) {
        this.name = name;
      }

      @Override
      public String toString() {
        return name;
      }
    }
  }
}