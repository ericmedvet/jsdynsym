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
package io.github.ericmedvet.jsdynsym.core.numerical;

import io.github.ericmedvet.jsdynsym.core.numerical.BiLevelNumericalDynamicalSystem.State;
import java.util.Arrays;
import java.util.stream.IntStream;

public class BiLevelNumericalDynamicalSystem<SH, SL> implements NumericalDynamicalSystem<State<SH, SL>> {

  private final NumericalDynamicalSystem<SH> highInnerNDS;
  private final NumericalDynamicalSystem<SL> lowInnerNDS;
  private final int highPeriod;
  private final int[] highIndexes;
  private final int[] lowIndexes;
  private final boolean averageEnabled;

  private int highStepCount;
  private double[] inputsAverage;
  private double[] highOutput;

  public BiLevelNumericalDynamicalSystem(
      NumericalDynamicalSystem<SH> highInnerNDS,
      NumericalDynamicalSystem<SL> lowInnerNDS,
      int highPeriod,
      int[] highIndexes,
      int[] lowIndexes,
      boolean averageEnabled
  ) {
    this.highInnerNDS = highInnerNDS;
    this.lowInnerNDS = lowInnerNDS;
    this.highPeriod = highPeriod;
    this.highIndexes = highIndexes;
    this.lowIndexes = lowIndexes;
    this.averageEnabled = averageEnabled;
    innerReset();
  }

  public record State<SH, SL>(SH highState, SL lowState, double[] highOutput) {
    @Override
    public String toString() {
      return "{%s;%s;%s}".formatted(
          highState,
          lowState,
          Arrays.toString(highOutput)
      );
    }
  }

  @Override
  public int nOfInputs() {
    return Math.max(
        Arrays.stream(highIndexes).max().orElse(0),
        Arrays.stream(lowIndexes).max().orElse(0)
    );
  }

  @Override
  public int nOfOutputs() {
    return lowInnerNDS.nOfOutputs();
  }

  public NumericalDynamicalSystem<SH> getHighInnerNDS() {
    return highInnerNDS;
  }

  public NumericalDynamicalSystem<SL> getLowInnerNDS() {
    return lowInnerNDS;
  }

  @Override
  public State<SH, SL> getState() {
    return new State<>(highInnerNDS.getState(), lowInnerNDS.getState(), highOutput);
  }

  @Override
  public void reset() {
    highInnerNDS.reset();
    lowInnerNDS.reset();
    innerReset();
  }

  private void innerReset() {
    highOutput = new double[highInnerNDS.nOfOutputs()];
    highStepCount = 0;
    inputsAverage = null;
  }

  @Override
  public double[] step(double t, double[] input) {
    if (averageEnabled) {
      double[] highInput;
      if (highStepCount == 0) {
        if (inputsAverage == null) {
          // use first input as input
          highInput = Arrays.stream(highIndexes).mapToDouble(i -> input[i]).toArray();
        } else {
          // get high input from average
          highInput = Arrays.stream(highIndexes).mapToDouble(i -> inputsAverage[i]).toArray();
        }
        // update highOutput
        highOutput = highInnerNDS.step(t, highInput);
        // restart average
        inputsAverage = Arrays.stream(input).map(x -> x / highPeriod).toArray();
      } else {
        inputsAverage = IntStream.range(0, inputsAverage.length)
            .mapToDouble(i -> inputsAverage[i] + input[i] / highPeriod)
            .toArray();
      }
    } else {
      if (highStepCount == 0) {
        double[] highInput = Arrays.stream(highIndexes).mapToDouble(i -> input[i]).toArray();
        highOutput = highInnerNDS.step(t, highInput);
      }
    }
    highStepCount = (highStepCount + 1) % highPeriod;
    double[] lowInput = new double[lowInnerNDS.nOfInputs()];
    System.arraycopy(
        Arrays.stream(lowIndexes).mapToDouble(i -> input[i]).toArray(),
        0,
        lowInput,
        0,
        lowIndexes.length
    );
    System.arraycopy(highOutput, 0, lowInput, lowIndexes.length, highOutput.length);
    return lowInnerNDS.step(t, lowInput);
  }
}
