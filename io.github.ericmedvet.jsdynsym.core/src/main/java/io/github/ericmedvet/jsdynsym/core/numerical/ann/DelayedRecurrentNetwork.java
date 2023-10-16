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
import io.github.ericmedvet.jsdynsym.core.numerical.NumericalDynamicalSystem;

import java.util.HashMap;
import java.util.Map;

public class DelayedRecurrentNetwork
    implements NumericalDynamicalSystem<DelayedRecurrentNetwork.State>, NumericalParametrized {
  private final MultiLayerPerceptron.ActivationFunction activationFunction;
  private final int nOfInputs;
  private final int nOfOutputs;
  private final int nOfInnerNeurons;
  private final DoubleRange timeRange;
  private final double threshold;
  private final double timeResolution;
  private final Map<Coord, Connection> connections;
  private final double[] biases;
  private final double[] outValues;
  private final double[][] inValues;

  public DelayedRecurrentNetwork(
      MultiLayerPerceptron.ActivationFunction activationFunction,
      int nOfInputs,
      int nOfOutputs,
      int nOfInnerNeurons,
      DoubleRange timeRange,
      double threshold,
      double timeResolution) {
    this.activationFunction = activationFunction;
    this.nOfInputs = nOfInputs;
    this.nOfOutputs = nOfOutputs;
    this.nOfInnerNeurons = nOfInnerNeurons;
    this.timeRange = timeRange;
    this.threshold = threshold;
    this.timeResolution = timeResolution;
    connections = new HashMap<>();
    int nOfNeurons = nOfInputs + nOfOutputs + nOfInnerNeurons;
    biases = new double[nOfNeurons];
    outValues = new double[nOfNeurons];
    inValues = new double[nOfNeurons][];
    reset();
  }

  private record Connection(double weight, double delay, double duration) {}

  private record Coord(int fromId, int toId) {}

  public record State(double[] outValues) {}

  @Override
  public double[] getParams() {
    int nOfNeurons = nOfInputs + nOfOutputs + nOfInnerNeurons;
    double[] params = new double[3 * nOfNeurons * nOfNeurons + nOfNeurons];
    int c = 0;
    for (int i = 0; i < nOfNeurons; i = i + 1) {
      params[c] = biases[i];
      c = c + 1;
    }
    for (int fromI = 0; fromI < nOfNeurons; fromI = fromI + 1) {
      for (int toI = 0; toI < nOfNeurons; toI = toI + 1) {
        Connection connection = connections.get(new Coord(fromI, toI));
        params[c] = connection.weight();
        params[c + 1] = connection.delay();
        params[c + 2] = connection.duration();
        c = c + 3;
      }
    }
    return params;
  }

  @Override
  public void setParams(double[] params) {
    int nOfNeurons = nOfInputs + nOfOutputs + nOfInnerNeurons;
    if (params.length != 3 * nOfNeurons * nOfNeurons + nOfNeurons) {
      throw new IllegalArgumentException(
          "Wrong number of parameters: %d found, %d expected"
              .formatted(params.length, 3 * nOfNeurons * nOfNeurons + nOfNeurons));
    }
    int c = 0;
    for (int i = 0; i < nOfNeurons; i = i + 1) {
      biases[i] = params[c];
      c = c + 1;
    }
    for (int fromI = 0; fromI < nOfNeurons; fromI = fromI + 1) {
      for (int toI = 0; toI < nOfNeurons; toI = toI + 1) {
        connections.put(
            new Coord(fromI, toI), new Connection(params[c], params[c + 1], params[c + 2]));
        c = c + 3;
      }
    }
  }

  @Override
  public State getState() {
    return new State(outValues);
  }

  @Override
  public void reset() {
    int nOfNeurons = nOfInputs + nOfOutputs + nOfInnerNeurons;
    for (int fromI = 0; fromI < nOfNeurons; fromI = fromI + 1) {
      inValues[fromI] = new double[(int) Math.ceil(timeRange.max() / timeResolution)];
      for (int toI = 0; toI < nOfNeurons; toI = toI + 1) {
        connections.put(new Coord(fromI, toI), new Connection(0, 0, 0));
      }
    }
  }

  @Override
  public double[] step(double t, double[] input) {
    // compute current time index
    int currentTI = timeIndex(t);
    // add inputs
    for (int i = 0; i < nOfInputs; i = i + 1) {
      if (Math.abs(input[i]) > threshold) {
        inValues[i][currentTI] = inValues[i][currentTI] + input[i];
      }
    }
    // compute neuron values
    int nOfNeurons = nOfInputs + nOfOutputs + nOfInnerNeurons;
    for (int i = 0; i < nOfNeurons; i = i + 1) {
      outValues[i] = activationFunction.applyAsDouble(biases[i] + inValues[i][currentTI]);
    }
    // generate new pulses
    for (int fromI = 0; fromI < nOfNeurons; fromI = fromI + 1) {
      for (int toI = 0; toI < nOfNeurons; toI = toI + 1) {
        Connection connection = connections.get(new Coord(fromI, toI));
        double pulseValue = outValues[fromI] * connection.weight();
        if (Math.abs(pulseValue) > threshold) {
          double delay =
              timeRange.denormalize(DoubleRange.SYMMETRIC_UNIT.normalize(connection.delay()));
          double duration =
              new DoubleRange(delay, timeRange.max())
                  .denormalize(DoubleRange.SYMMETRIC_UNIT.normalize(connection.duration()));
          for (double futureT = t + delay;
              futureT <= t + delay + duration;
              futureT = futureT + timeResolution) {
            int futureTI = timeIndex(futureT);
            inValues[toI][futureTI] = inValues[toI][futureTI] + pulseValue;
          }
        }
      }
    }
    // clear previous index
    int previousTI = currentTI - 1;
    if (previousTI < 0) {
      previousTI = timeIndex(timeRange.max());
    }
    for (int i = 0; i < nOfInputs; i = i + 1) {
      inValues[i][previousTI] = 0;
    }
    // read outputs
    double[] outputs = new double[nOfOutputs];
    System.arraycopy(outValues, nOfInputs + nOfInnerNeurons, outputs, 0, outputs.length);
    return outputs;
  }

  @Override
  public int nOfInputs() {
    return nOfInputs;
  }

  @Override
  public int nOfOutputs() {
    return nOfOutputs;
  }

  private int timeIndex(double t) {
    return (int) Math.floor((t % timeRange.max()) / timeResolution);
  }

  @Override
  public String toString() {
    return "DRN-%s-%d>(%d)>%d"
        .formatted(
            activationFunction.toString().toLowerCase(), nOfInputs, nOfInnerNeurons, nOfOutputs);
  }
}
