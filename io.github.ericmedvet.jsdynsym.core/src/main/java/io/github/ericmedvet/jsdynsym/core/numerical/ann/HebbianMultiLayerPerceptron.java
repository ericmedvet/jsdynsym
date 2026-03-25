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
package io.github.ericmedvet.jsdynsym.core.numerical.ann;

import io.github.ericmedvet.jnb.datastructure.Copyable;
import io.github.ericmedvet.jnb.datastructure.DoubleRange;
import io.github.ericmedvet.jnb.datastructure.NumericalParametrized;
import io.github.ericmedvet.jsdynsym.core.numerical.FrozenableNumericalDynamicalSystem;
import io.github.ericmedvet.jsdynsym.core.numerical.NumericalStatelessSystem;
import io.github.ericmedvet.jsdynsym.core.numerical.NumericalTimeInvariantDynamicalSystem;
import java.util.Arrays;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class HebbianMultiLayerPerceptron implements NumericalTimeInvariantDynamicalSystem<HebbianMultiLayerPerceptron.State>, NumericalParametrized<HebbianMultiLayerPerceptron>, FrozenableNumericalDynamicalSystem<HebbianMultiLayerPerceptron.State>, Copyable<HebbianMultiLayerPerceptron> {
  private final MultiLayerPerceptron.ActivationFunction activationFunction;
  private final double[][][] as;
  private final double[][][] bs;
  private final double[][][] cs;
  private final double[][][] ds;
  private final double[][][] initialWeights;
  private final int[] neurons;
  private final double learningRate;
  private final int weightsUpdateInterval;
  private final DoubleRange initialWeightRange;
  private final DoubleRange weightRange;
  private final ParametrizationType parametrizationType;
  private final WeightInitializationType weightInitializationType;
  private final RandomGenerator randomGenerator;
  private int stepCounter;
  private State state;

  public HebbianMultiLayerPerceptron(
      MultiLayerPerceptron.ActivationFunction activationFunction,
      double[][][] as,
      double[][][] bs,
      double[][][] cs,
      double[][][] ds,
      double[][][] initialWeights,
      int[] neurons,
      double learningRate,
      int weightsUpdateInterval,
      DoubleRange initialWeightRange,
      double maxWeightMagnitude,
      RandomGenerator randomGenerator,
      ParametrizationType parametrizationType,
      WeightInitializationType weightInitializationType
  ) {
    this.activationFunction = activationFunction;
    this.as = as;
    this.bs = bs;
    this.cs = cs;
    this.ds = ds;
    this.initialWeights = initialWeights;
    this.neurons = neurons;
    this.learningRate = learningRate;
    this.weightsUpdateInterval = weightsUpdateInterval;
    this.initialWeightRange = initialWeightRange;
    this.weightRange = new DoubleRange(-maxWeightMagnitude, maxWeightMagnitude);
    this.randomGenerator = randomGenerator;
    this.parametrizationType = parametrizationType;
    this.weightInitializationType = weightInitializationType;
    reset();
  }

  public HebbianMultiLayerPerceptron(
      MultiLayerPerceptron.ActivationFunction activationFunction,
      int nOfInput,
      int[] innerNeurons,
      int nOfOutput,
      double[] params,
      double learningRate,
      int weightsUpdateInterval,
      DoubleRange initialWeightRange,
      double maxWeightMagnitude,
      RandomGenerator randomGenerator,
      ParametrizationType parametrizationType,
      WeightInitializationType weightInitializationType
  ) {
    this(
        activationFunction,
        nOfInput,
        innerNeurons,
        nOfOutput,
        learningRate,
        weightsUpdateInterval,
        initialWeightRange,
        maxWeightMagnitude,
        randomGenerator,
        parametrizationType,
        weightInitializationType
    );
    setParams(params);
  }

  public HebbianMultiLayerPerceptron(
      MultiLayerPerceptron.ActivationFunction activationFunction,
      int nOfInput,
      int[] innerNeurons,
      int nOfOutput,
      double learningRate,
      int weightsUpdateInterval,
      DoubleRange initialWeightRange,
      double maxWeightMagnitude,
      RandomGenerator randomGenerator,
      ParametrizationType parametrizationType,
      WeightInitializationType weightInitializationType
  ) {
    this(
        activationFunction,
        MLPUtils.zeroWeights(MLPUtils.countNeurons(nOfInput, innerNeurons, nOfOutput)),
        MLPUtils.zeroWeights(MLPUtils.countNeurons(nOfInput, innerNeurons, nOfOutput)),
        MLPUtils.zeroWeights(MLPUtils.countNeurons(nOfInput, innerNeurons, nOfOutput)),
        MLPUtils.zeroWeights(MLPUtils.countNeurons(nOfInput, innerNeurons, nOfOutput)),
        MLPUtils.zeroWeights(MLPUtils.countNeurons(nOfInput, innerNeurons, nOfOutput)),
        MLPUtils.countNeurons(nOfInput, innerNeurons, nOfOutput),
        learningRate,
        weightsUpdateInterval,
        initialWeightRange,
        maxWeightMagnitude,
        randomGenerator,
        parametrizationType,
        weightInitializationType
    );
  }

  private static int countParams(ParametrizationType parametrizationType, int[] neurons) {
    return switch (parametrizationType) {
      case NETWORK -> 1;
      case LAYER -> (neurons.length - 1);
      case NEURON -> Arrays.stream(neurons).skip(1).sum();
      case SYNAPSE -> MLPUtils.countWeights(neurons);
    };
  }

  private static double[] flat(ParametrizationType parametrizationType, double[][][] params, int[] neurons) {
    return switch (parametrizationType) {
      case NETWORK -> new double[]{params[0][0][0]};
      case LAYER -> Arrays.stream(params)
          .mapToDouble(l -> l[0][0])
          .toArray();
      case NEURON -> Arrays.stream(params)
          .flatMap(l -> Arrays.stream(l).mapToDouble(n -> n[0]).boxed())
          .mapToDouble(v -> v)
          .toArray();
      case SYNAPSE -> MLPUtils.flat(params, neurons);
    };
  }

  private static double[][][] unflat(ParametrizationType parametrizationType, double[] params, int[] neurons) {
    if (params.length != countParams(parametrizationType, neurons)) {
      throw new IllegalArgumentException(
          String.format(
              "Wrong number of params: %d expected, %d found",
              countParams(parametrizationType, neurons),
              params.length
          )
      );
    }
    return switch (parametrizationType) {
      case NETWORK ->
        MLPUtils.unflat(MLPUtils.nCopies(MLPUtils.countWeights(neurons), params[0]), neurons);
      case LAYER -> IntStream.range(1, neurons.length)
          .mapToObj(
              li -> IntStream.range(0, neurons[li])
                  .mapToObj(ni -> MLPUtils.nCopies(neurons[li - 1] + 1, params[li - 1]))
                  .toArray(double[][]::new)
          )
          .toArray(double[][][]::new);
      case NEURON -> {
        double[][][] unflat = MLPUtils.zeroWeights(neurons);
        int c = 0;
        for (double[][] layer : unflat) {
          for (double[] neuron : layer) {
            Arrays.fill(neuron, params[c++]);
          }
        }
        yield unflat;
      }
      case SYNAPSE -> MLPUtils.unflat(params, neurons);
    };
  }

  @Override
  public HebbianMultiLayerPerceptron copyOf() {
    HebbianMultiLayerPerceptron copy = new HebbianMultiLayerPerceptron(
        activationFunction,
        MLPUtils.copy3D(as),
        MLPUtils.copy3D(bs),
        MLPUtils.copy3D(cs),
        MLPUtils.copy3D(ds),
        MLPUtils.copy3D(initialWeights),
        MLPUtils.copy1D(neurons),
        learningRate,
        weightsUpdateInterval,
        initialWeightRange,
        weightRange.max(),
        randomGenerator,
        parametrizationType,
        weightInitializationType
    );
    copy.state = state.copyOf();
    return copy;
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
  public State getState() {
    return state;
  }

  @Override
  public double[] getParams() {
    double[] flatAs = flat(parametrizationType, as, neurons);
    double[] flatBs = flat(parametrizationType, bs, neurons);
    double[] flatCs = flat(parametrizationType, cs, neurons);
    double[] flatDs = flat(parametrizationType, ds, neurons);
    if (weightInitializationType.equals(WeightInitializationType.PARAMS)) {
      double[] flatWeights = MLPUtils.flat(state.weights, neurons);
      return MLPUtils.concat1D(flatAs, flatBs, flatCs, flatDs, flatWeights);
    } else {
      return MLPUtils.concat1D(flatAs, flatBs, flatCs, flatDs);
    }
  }

  @Override
  public void setParams(double[] params) {
    int n = countParams(parametrizationType, neurons);
    MLPUtils.set3D(unflat(parametrizationType, Arrays.copyOfRange(params, 0, n), neurons), as);
    MLPUtils.set3D(unflat(parametrizationType, Arrays.copyOfRange(params, n, 2 * n), neurons), bs);
    MLPUtils.set3D(unflat(parametrizationType, Arrays.copyOfRange(params, 2 * n, 3 * n), neurons), cs);
    MLPUtils.set3D(unflat(parametrizationType, Arrays.copyOfRange(params, 3 * n, 4 * n), neurons), ds);
    if (weightInitializationType.equals(WeightInitializationType.PARAMS)) {
      MLPUtils.set3D(
          unflat(ParametrizationType.SYNAPSE, Arrays.copyOfRange(params, 4 * n, params.length), neurons),
          initialWeights
      );
    }
    reset();
  }

  @Override
  public void reset() {
    stepCounter = 0;
    state = new State(
        switch (weightInitializationType) {
          case PARAMS -> MLPUtils.copy3D(initialWeights);
          case RANDOM -> {
            MLPUtils.set3D(MLPUtils.randomWeights(neurons, initialWeightRange, randomGenerator), initialWeights);
            yield MLPUtils.copy3D(initialWeights);
          }
          case ZEROS -> MLPUtils.zeroWeights(neurons);
        },
        Arrays.stream(neurons).mapToObj(double[]::new).toArray(double[][]::new)
    );
  }

  @Override
  public String toString() {
    return "HebbianMLP-%s-%s"
        .formatted(
            activationFunction.toString().toLowerCase(),
            Arrays.stream(neurons).mapToObj(Integer::toString).collect(Collectors.joining(">"))
        );
  }

  @Override
  public NumericalStatelessSystem stateless() {
    double[][][] frozenWeights = unflat(
        ParametrizationType.SYNAPSE,
        flat(
            ParametrizationType.SYNAPSE,
            getState().weights,
            neurons
        ),
        neurons
    );
    return new MultiLayerPerceptron(activationFunction, frozenWeights, neurons);
  }

  public enum ParametrizationType {
    NETWORK, LAYER, NEURON, SYNAPSE
  }

  public enum WeightInitializationType {
    ZEROS, PARAMS, RANDOM
  }

  @Override
  public double[] step(double[] input) {
    // update weights
    double[][][] newWeights = state.weights;
    if (stepCounter > 0 && stepCounter % weightsUpdateInterval == 0) {
      for (int i = 1; i < neurons.length; i++) {
        for (int j = 0; j < newWeights[i - 1].length; j++) {
          double postActivation = state.activations[i][j];
          for (int k = 0; k < newWeights[i - 1][j].length; k++) {
            double preActivation = (k == 0) ? 1.0 : state.activations[i - 1][k - 1];
            newWeights[i - 1][j][k] = weightRange.clip(
                newWeights[i - 1][j][k] + learningRate * (as[i - 1][j][k] * preActivation + bs[i - 1][j][k] * postActivation + cs[i - 1][j][k] * preActivation * postActivation + ds[i - 1][j][k])
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
        state.activations
    );
    // update state and counter
    stepCounter += 1;
    state = new State(newWeights, newActivations);
    return MLPUtils.copy1D(newActivations[neurons.length - 1]);
  }

  public record State(
      double[][][] weights,
      double[][] activations
  ) implements NumericalParametrized<State>, Copyable<State> {

    @Override
    public State copyOf() {
      return new State(MLPUtils.copy3D(weights), MLPUtils.copy2D(activations));
    }

    @Override
    public void setParams(double[] param) {
      throw new UnsupportedOperationException("Params cannot be set this way");
    }

    @Override
    public double[] getParams() {
      return MLPUtils.flat(weights);
    }

  }
}