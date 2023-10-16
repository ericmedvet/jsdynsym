
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

package io.github.ericmedvet.jsdynsym.core.numerical;

import io.github.ericmedvet.jsdynsym.core.composed.AbstractComposed;

import java.util.Arrays;
import java.util.random.RandomGenerator;

public class Noised<S> extends AbstractComposed<NumericalDynamicalSystem<S>>
    implements NumericalDynamicalSystem<S> {

  private final double inputSigma;
  private final double outputSigma;
  private final RandomGenerator randomGenerator;

  public Noised(
      NumericalDynamicalSystem<S> inner,
      double inputSigma,
      double outputSigma,
      RandomGenerator randomGenerator) {
    super(inner);
    this.inputSigma = inputSigma;
    this.outputSigma = outputSigma;
    this.randomGenerator = randomGenerator;
  }

  @Override
  public S getState() {
    return inner().getState();
  }

  @Override
  public void reset() {
    inner().getState();
  }

  @Override
  public double[] step(double t, double[] input) {
    double[] noisedInput = input;
    if (inputSigma > 0) {
      noisedInput =
          Arrays.stream(input).map(v -> v + randomGenerator.nextGaussian(0, inputSigma)).toArray();
    }
    double[] noisedOutput = inner().step(t, noisedInput);
    if (outputSigma > 0) {
      noisedOutput =
          Arrays.stream(noisedOutput)
              .map(v -> v + randomGenerator.nextGaussian(0, outputSigma))
              .toArray();
    }
    return noisedOutput;
  }

  @Override
  public int nOfInputs() {
    return inner().nOfInputs();
  }

  @Override
  public int nOfOutputs() {
    return inner().nOfOutputs();
  }

  @Override
  public String toString() {
    return "noised[in=%.3f;out=%.3f](%s)".formatted(inputSigma, outputSigma, inner());
  }
}
