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

import io.github.ericmedvet.jnb.datastructure.DoubleRange;
import io.github.ericmedvet.jnb.datastructure.NumericalParametrized;
import io.github.ericmedvet.jsdynsym.core.numerical.MultivariateRealFunction;
import java.util.Arrays;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.Collectors;

public class MultiLayerPerceptron implements MultivariateRealFunction, NumericalParametrized<MultiLayerPerceptron> {

  private final ActivationFunction activationFunction;
  private final double[][][] weights;
  private final int[] neurons;

  public MultiLayerPerceptron(
      ActivationFunction activationFunction,
      double[][][] weights,
      int[] neurons
  ) {
    this.activationFunction = activationFunction;
    this.weights = weights;
    this.neurons = neurons;
    if (MLPUtils.flat(weights, neurons).length != MLPUtils.countWeights(neurons)) {
      throw new IllegalArgumentException(
          String.format(
              "Wrong number of weights: %d expected, %d found",
              MLPUtils.countWeights(neurons),
              MLPUtils.flat(weights, neurons).length
          )
      );
    }
  }

  public MultiLayerPerceptron(
      ActivationFunction activationFunction,
      int nOfInput,
      int[] innerNeurons,
      int nOfOutput,
      double[] weights
  ) {
    this(
        activationFunction,
        MLPUtils.unflat(weights, MLPUtils.countNeurons(nOfInput, innerNeurons, nOfOutput)),
        MLPUtils.countNeurons(nOfInput, innerNeurons, nOfOutput)
    );
  }

  public MultiLayerPerceptron(
      ActivationFunction activationFunction,
      int nOfInput,
      int[] innerNeurons,
      int nOfOutput
  ) {
    this(
        activationFunction,
        nOfInput,
        innerNeurons,
        nOfOutput,
        new double[MLPUtils.countWeights(MLPUtils.countNeurons(nOfInput, innerNeurons, nOfOutput))]
    );
  }

  @Override
  public double[] compute(double[] input) {
    double[][] activationValues = new double[neurons.length][];
    for (int i = 0; i < neurons.length; i++) {
      activationValues[i] = new double[neurons[i]];
    }
    return MLPUtils.computeActivations(input, weights, activationFunction, activationValues)[neurons.length - 1];
  }

  public enum ActivationFunction implements DoubleUnaryOperator {
    RELU(x -> (x < 0) ? 0d : x, new DoubleRange(0d, Double.POSITIVE_INFINITY)), SIGMOID(
        x -> 1d / (1d + Math.exp(-x)),
        DoubleRange.UNIT
    ), SIN(Math::sin, DoubleRange.SYMMETRIC_UNIT), TANH(
        Math::tanh,
        DoubleRange.SYMMETRIC_UNIT
    ), SIGN(
        Math::signum,
        DoubleRange.SYMMETRIC_UNIT
    ), IDENTITY(x -> x, DoubleRange.UNBOUNDED);

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

  @Override
  public double[] getParams() {
    return MLPUtils.flat(weights, neurons);
  }

  @Override
  public void setParams(double[] params) {
    double[][][] newWeights = MLPUtils.unflat(params, neurons);
    for (int l = 0; l < newWeights.length; l++) {
      for (int s = 0; s < newWeights[l].length; s++) {
        System.arraycopy(newWeights[l][s], 0, weights[l][s], 0, newWeights[l][s].length);
      }
    }
  }

  public int sizeOfLayer(
      int indexOfLayer
  ) {
    return neurons[indexOfLayer];
  }

  @Override
  public int nOfInputs() {
    return sizeOfLayer(0);
  }

  public int nOfLayers() {
    return neurons.length;
  }

  @Override
  public int nOfOutputs() {
    return sizeOfLayer(neurons.length - 1);
  }

  @Override
  public String toString() {
    return "MLP-%s-%s"
        .formatted(
            activationFunction.toString().toLowerCase(),
            Arrays.stream(neurons).mapToObj(Integer::toString).collect(Collectors.joining(">"))
        );
  }
}