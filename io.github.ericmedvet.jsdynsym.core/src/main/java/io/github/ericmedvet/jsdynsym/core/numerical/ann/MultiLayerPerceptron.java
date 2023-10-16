
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

package io.github.ericmedvet.jsdynsym.core.numerical.ann;

import io.github.ericmedvet.jsdynsym.core.DoubleRange;
import io.github.ericmedvet.jsdynsym.core.NumericalParametrized;
import io.github.ericmedvet.jsdynsym.core.numerical.MultivariateRealFunction;

import java.util.Arrays;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.Collectors;

public class MultiLayerPerceptron implements MultivariateRealFunction, NumericalParametrized {

  protected final ActivationFunction activationFunction;
  protected final double[][][] weights;
  protected final int[] neurons;

  public MultiLayerPerceptron(
      ActivationFunction activationFunction, double[][][] weights, int[] neurons) {
    this.activationFunction = activationFunction;
    this.weights = weights;
    this.neurons = neurons;
    if (flat(weights, neurons).length != countWeights(neurons)) {
      throw new IllegalArgumentException(
          String.format(
              "Wrong number of weights: %d expected, %d found",
              countWeights(neurons), flat(weights, neurons).length));
    }
  }

  public MultiLayerPerceptron(
      ActivationFunction activationFunction,
      int nOfInput,
      int[] innerNeurons,
      int nOfOutput,
      double[] weights) {
    this(
        activationFunction,
        unflat(weights, countNeurons(nOfInput, innerNeurons, nOfOutput)),
        countNeurons(nOfInput, innerNeurons, nOfOutput));
  }

  public MultiLayerPerceptron(
      ActivationFunction activationFunction, int nOfInput, int[] innerNeurons, int nOfOutput) {
    this(
        activationFunction,
        nOfInput,
        innerNeurons,
        nOfOutput,
        new double[countWeights(countNeurons(nOfInput, innerNeurons, nOfOutput))]);
  }

  public enum ActivationFunction implements DoubleUnaryOperator {
    RELU(x -> (x < 0) ? 0d : x, new DoubleRange(0d, Double.POSITIVE_INFINITY)),
    SIGMOID(x -> 1d / (1d + Math.exp(-x)), DoubleRange.UNIT),
    SIN(Math::sin, DoubleRange.SYMMETRIC_UNIT),
    TANH(Math::tanh, DoubleRange.SYMMETRIC_UNIT),
    SIGN(Math::signum, DoubleRange.SYMMETRIC_UNIT),
    IDENTITY(x -> x, DoubleRange.UNBOUNDED);

    private final DoubleUnaryOperator f;
    private final DoubleRange domain;

    ActivationFunction(DoubleUnaryOperator f, DoubleRange domain) {
      this.f = f;
      this.domain = domain;
    }

    @Override
    public double applyAsDouble(double x) {
      return f.applyAsDouble(x);
    }

    public DoubleRange getDomain() {
      return domain;
    }

    public DoubleUnaryOperator getF() {
      return f;
    }
  }

  public static int[] countNeurons(int nOfInput, int[] innerNeurons, int nOfOutput) {
    final int[] neurons;
    neurons = new int[2 + innerNeurons.length];
    System.arraycopy(innerNeurons, 0, neurons, 1, innerNeurons.length);
    neurons[0] = nOfInput;
    neurons[neurons.length - 1] = nOfOutput;
    return neurons;
  }

  public static int countWeights(int[] neurons) {
    int c = 0;
    for (int i = 1; i < neurons.length; i++) {
      c = c + neurons[i] * (neurons[i - 1] + 1);
    }
    return c;
  }

  public static int countWeights(int nOfInput, int[] innerNeurons, int nOfOutput) {
    return countWeights(countNeurons(nOfInput, innerNeurons, nOfOutput));
  }

  public static double[] flat(double[][][] unflatWeights, int[] neurons) {
    double[] flatWeights = new double[countWeights(neurons)];
    int c = 0;
    for (int i = 1; i < neurons.length; i++) {
      for (int j = 0; j < neurons[i]; j++) {
        for (int k = 0; k < neurons[i - 1] + 1; k++) {
          flatWeights[c] = unflatWeights[i - 1][j][k];
          c = c + 1;
        }
      }
    }
    return flatWeights;
  }

  public static double[][][] unflat(double[] flatWeights, int[] neurons) {
    double[][][] unflatWeights = new double[neurons.length - 1][][];
    int c = 0;
    for (int i = 1; i < neurons.length; i++) {
      unflatWeights[i - 1] = new double[neurons[i]][neurons[i - 1] + 1];
      for (int j = 0; j < neurons[i]; j++) {
        for (int k = 0; k < neurons[i - 1] + 1; k++) {
          unflatWeights[i - 1][j][k] = flatWeights[c];
          c = c + 1;
        }
      }
    }
    return unflatWeights;
  }

  @Override
  public double[] compute(double[] input) {
    if (input.length != neurons[0]) {
      throw new IllegalArgumentException(
          String.format("Expected input length is %d: found %d", neurons[0], input.length));
    }
    double[][] activationValues = new double[neurons.length][];
    activationValues[0] = Arrays.stream(input).map(activationFunction).toArray();
    for (int i = 1; i < neurons.length; i++) {
      activationValues[i] = new double[neurons[i]];
      for (int j = 0; j < neurons[i]; j++) {
        double sum = weights[i - 1][j][0]; // set the bias
        for (int k = 1; k < neurons[i - 1] + 1; k++) {
          sum = sum + activationValues[i - 1][k - 1] * weights[i - 1][j][k];
        }
        activationValues[i][j] = activationFunction.applyAsDouble(sum);
      }
    }
    return activationValues[neurons.length - 1];
  }

  @Override
  public double[] getParams() {
    return flat(weights, neurons);
  }

  @Override
  public void setParams(double[] params) {
    double[][][] newWeights = MultiLayerPerceptron.unflat(params, neurons);
    for (int l = 0; l < newWeights.length; l++) {
      for (int s = 0; s < newWeights[l].length; s++) {
        System.arraycopy(newWeights[l][s], 0, weights[l][s], 0, newWeights[l][s].length);
      }
    }
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
  public String toString() {
    return "MLP-%s-%s"
        .formatted(
            activationFunction.toString().toLowerCase(),
            Arrays.stream(neurons).mapToObj(Integer::toString).collect(Collectors.joining(">")));
  }
}
