/*-
 * ========================LICENSE_START=================================
 * jsdynsym-core
 * %%
 * Copyright (C) 2023 - 2026 Eric Medvet
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
/*
 * Copyright 2026 eric
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.ericmedvet.jsdynsym.core.numerical.ann;

import io.github.ericmedvet.jnb.datastructure.DoubleRange;
import java.util.Arrays;
import java.util.random.RandomGenerator;

public class MLPUtils {

  private MLPUtils() {
  }

  public static double[][] computeActivations(
      double[] input,
      double[][][] weights,
      MultiLayerPerceptron.ActivationFunction activationFunction,
      double[][] activations
  ) {
    if (input.length != activations[0].length) {
      throw new IllegalArgumentException(
          String.format(
              "Expected input length is %d: found %d",
              activations[0].length,
              input.length
          )
      );
    }
    System.arraycopy(input, 0, activations[0], 0, input.length);
    for (int i = 1; i < activations.length; i++) {
      for (int j = 0; j < activations[i].length; j++) {
        double sum = weights[i - 1][j][0];
        for (int k = 1; k < activations[i - 1].length + 1; k++) {
          sum = sum + activations[i - 1][k - 1] * weights[i - 1][j][k];
        }
        activations[i][j] = activationFunction.applyAsDouble(sum);
      }
    }
    return activations;
  }

  public static double[] concat1D(double[]... arrays) {
    int totalLength = 0;
    for (double[] array : arrays) {
      totalLength += array.length;
    }
    double[] concatenated = new double[totalLength];
    int offset = 0;
    for (double[] array : arrays) {
      System.arraycopy(array, 0, concatenated, offset, array.length);
      offset += array.length;
    }
    return concatenated;
  }

  public static double[] copy1D(double[] src) {
    return Arrays.copyOf(src, src.length);
  }

  public static int[] copy1D(int[] src) {
    return Arrays.copyOf(src, src.length);
  }

  public static double[][] copy2D(double[][] src) {
    double[][] copy = new double[src.length][];
    for (int i = 0; i < src.length; i++) {
      copy[i] = copy1D(src[i]);
    }
    return copy;
  }

  public static double[][][] copy3D(double[][][] src) {
    double[][][] copy = new double[src.length][][];
    for (int i = 0; i < src.length; i++) {
      copy[i] = new double[src[i].length][];
      for (int j = 0; j < src[i].length; j++) {
        copy[i][j] = copy1D(src[i][j]);
      }
    }
    return copy;
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

  public static double[] flat(double[][][] unflatWeights) {
    int[] neurons = new int[unflatWeights.length + 1];
    neurons[0] = unflatWeights[0][0].length - 1;
    for (int i = 1; i < neurons.length; i++) {
      neurons[i] = unflatWeights[i - 1].length;
    }
    return flat(unflatWeights, neurons);
  }

  public static double[] nCopies(int n, double value) {
    double[] values = new double[n];
    Arrays.fill(values, value);
    return values;
  }

  public static double[][][] randomWeights(
      int[] neurons,
      DoubleRange initialWeightRange,
      RandomGenerator randomGenerator
  ) {
    double[][][] randomWeights = zeroWeights(neurons);
    for (int i = 1; i < neurons.length; i++) {
      for (int j = 0; j < neurons[i]; j++) {
        for (int k = 0; k < neurons[i - 1] + 1; k++) {
          randomWeights[i - 1][j][k] = initialWeightRange.denormalize(randomGenerator.nextDouble());
        }
      }
    }
    return randomWeights;
  }

  public static void set3D(double[][][] src, double[][][] dst) {
    for (int l = 0; l < src.length; l++) {
      for (int s = 0; s < src[l].length; s++) {
        System.arraycopy(src[l][s], 0, dst[l][s], 0, src[l][s].length);
      }
    }
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

  public static double[][][] zeroWeights(int[] neurons) {
    return unflat(new double[countWeights(neurons)], neurons);
  }

}