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

public class BiLevelNumericalDynamicalSystem<SH, SL> implements NumericalDynamicalSystem<State<SH, SL>> {

  private final NumericalDynamicalSystem<SH> highInnerNDS;
  private final NumericalDynamicalSystem<SL> lowInnerNDS;
  private final int highPeriod;
  private final int highIndex;
  private final int lowIndex;

  private double[] highOutput;
  private int highStepCount;

  public BiLevelNumericalDynamicalSystem(
      NumericalDynamicalSystem<SH> highInnerNDS,
      NumericalDynamicalSystem<SL> lowInnerNDS,
      int highPeriod,
      int highIndex,
      int lowIndex
  ) {
    this.highInnerNDS = highInnerNDS;
    this.lowInnerNDS = lowInnerNDS;
    this.highPeriod = highPeriod;
    this.highIndex = highIndex;
    this.lowIndex = lowIndex;
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
    return highInnerNDS.nOfInputs() + lowInnerNDS.nOfInputs() - highInnerNDS.nOfOutputs() - (Math.max(
        this.lowIndex - this.highIndex,
        0
    ));
  }

  public int nOfLowInputs() {
    return highInnerNDS.nOfInputs();
  }

  @Override
  public int nOfOutputs() {
    return lowInnerNDS.nOfOutputs();
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
  }

  @Override
  public double[] step(double t, double[] input) {
    if (highStepCount == 0) {
      double[] highInput = Arrays.copyOfRange(input, this.highIndex, this.highIndex + highInnerNDS.nOfInputs());
      highOutput = highInnerNDS.step(t, highInput);
    }
    highStepCount = (highStepCount + 1) % highPeriod;
    double[] lowInput = new double[lowInnerNDS.nOfInputs()];
    // int remainingNOfInputs = nOfInputs() - highInnerNDS.nOfInputs();
    int directNOfInputs = lowInnerNDS.nOfInputs() - highInnerNDS.nOfOutputs();
    System.arraycopy(input, this.lowIndex, lowInput, 0, directNOfInputs);
    System.arraycopy(highOutput, 0, lowInput, directNOfInputs, highOutput.length);
    return lowInnerNDS.step(t, lowInput);
  }
}
