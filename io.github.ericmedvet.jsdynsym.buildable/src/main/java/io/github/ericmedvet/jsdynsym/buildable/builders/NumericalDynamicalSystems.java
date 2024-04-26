/*-
 * ========================LICENSE_START=================================
 * jsdynsym-buildable
 * %%
 * Copyright (C) 2023 - 2024 Eric Medvet
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

package io.github.ericmedvet.jsdynsym.buildable.builders;

import io.github.ericmedvet.jnb.core.Discoverable;
import io.github.ericmedvet.jnb.core.Param;
import io.github.ericmedvet.jnb.datastructure.DoubleRange;
import io.github.ericmedvet.jsdynsym.core.StatelessSystem;
import io.github.ericmedvet.jsdynsym.core.composed.InStepped;
import io.github.ericmedvet.jsdynsym.core.composed.OutStepped;
import io.github.ericmedvet.jsdynsym.core.composed.Stepped;
import io.github.ericmedvet.jsdynsym.core.numerical.*;
import io.github.ericmedvet.jsdynsym.core.numerical.ann.DelayedRecurrentNetwork;
import io.github.ericmedvet.jsdynsym.core.numerical.ann.MultiLayerPerceptron;
import java.util.List;
import java.util.function.BiFunction;
import java.util.random.RandomGenerator;

@Discoverable(prefixTemplate = "dynamicalSystem|dynSys|ds.num")
public class NumericalDynamicalSystems {

  private NumericalDynamicalSystems() {}

  public interface Builder<F extends NumericalDynamicalSystem<S>, S>
      extends BiFunction<List<String>, List<String>, F> {
    default F apply(int nOfInputs, int nOfOutputs) {
      return apply(
          MultivariateRealFunction.varNames("x", nOfInputs),
          MultivariateRealFunction.varNames("y", nOfOutputs));
    }

    static NumericalDynamicalSystems.Builder<NumericalDynamicalSystem<StatelessSystem.State>, StatelessSystem.State>
        empty() {
      return (xVarNames, yVarNames) -> NumericalStatelessSystem.from(
          xVarNames.size(), yVarNames.size(), (t, x) -> new double[yVarNames.size()]);
    }
  }

  @SuppressWarnings("unused")
  public static Builder<DelayedRecurrentNetwork, DelayedRecurrentNetwork.State> drn(
      @Param(value = "timeRange", dNPM = "m.range(min=0;max=1)") DoubleRange timeRange,
      @Param(value = "innerNeuronsRatio", dD = 1d) double innerNeuronsRatio,
      @Param(value = "activationFunction", dS = "tanh")
          MultiLayerPerceptron.ActivationFunction activationFunction,
      @Param(value = "threshold", dD = 0.1d) double threshold,
      @Param(value = "timeResolution", dD = 0.16666d) double timeResolution) {
    return (xVarNames, yVarNames) -> new DelayedRecurrentNetwork(
        activationFunction,
        xVarNames.size(),
        yVarNames.size(),
        (int) Math.round(innerNeuronsRatio * (xVarNames.size() + yVarNames.size())),
        timeRange,
        threshold,
        timeResolution);
  }

  @SuppressWarnings("unused")
  public static <S> Builder<EnhancedInput<S>, S> enhanced(
      @Param("windowT") double windowT,
      @Param("inner") Builder<? extends NumericalDynamicalSystem<S>, S> inner,
      @Param(
              value = "types",
              dSs = {"current", "trend", "avg"})
          List<EnhancedInput.Type> types) {
    return (xVarNames, yVarNames) -> new EnhancedInput<>(
        inner.apply(
            xVarNames.stream()
                .map(n -> types.stream()
                    .map(t -> n + "_" + t.toString().toLowerCase())
                    .toList())
                .flatMap(List::stream)
                .toList(),
            yVarNames),
        windowT,
        types);
  }

  @SuppressWarnings("unused")
  public static <S> Builder<NumericalDynamicalSystem<Stepped.State<S>>, Stepped.State<S>> inStepped(
      @Param(value = "stepT", dD = 1) double interval,
      @Param("inner") Builder<? extends NumericalDynamicalSystem<S>, S> inner) {
    return (xVarNames, yVarNames) -> NumericalDynamicalSystem.from(
        new InStepped<>(inner.apply(xVarNames, yVarNames), interval), xVarNames.size(), yVarNames.size());
  }

  @SuppressWarnings("unused")
  public static Builder<MultiLayerPerceptron, StatelessSystem.State> mlp(
      @Param(value = "innerLayerRatio", dD = 0.65) double innerLayerRatio,
      @Param(value = "nOfInnerLayers", dI = 1) int nOfInnerLayers,
      @Param(value = "activationFunction", dS = "tanh")
          MultiLayerPerceptron.ActivationFunction activationFunction) {
    return (xVarNames, yVarNames) -> {
      int[] innerNeurons = new int[nOfInnerLayers];
      int centerSize = (int) Math.max(2, Math.round(xVarNames.size() * innerLayerRatio));
      if (nOfInnerLayers > 1) {
        for (int i = 0; i < nOfInnerLayers / 2; i++) {
          innerNeurons[i] =
              xVarNames.size() + (centerSize - xVarNames.size()) / (nOfInnerLayers / 2 + 1) * (i + 1);
        }
        for (int i = nOfInnerLayers / 2; i < nOfInnerLayers; i++) {
          innerNeurons[i] = centerSize
              + (yVarNames.size() - centerSize) / (nOfInnerLayers / 2 + 1) * (i - nOfInnerLayers / 2);
        }
      } else if (nOfInnerLayers > 0) {
        innerNeurons[0] = centerSize;
      }
      return new MultiLayerPerceptron(activationFunction, xVarNames.size(), innerNeurons, yVarNames.size());
    };
  }

  @SuppressWarnings("unused")
  public static <S> Builder<Noised<S>, S> noised(
      @Param(value = "inputSigma", dD = 0) double inputSigma,
      @Param(value = "outputSigma", dD = 0) double outputSigma,
      @Param(value = "randomGenerator", dNPM = "m.defaultRG()") RandomGenerator randomGenerator,
      @Param("inner") Builder<? extends NumericalDynamicalSystem<S>, S> inner) {
    return (xVarNames, yVarNames) ->
        new Noised<>(inner.apply(xVarNames, yVarNames), inputSigma, outputSigma, randomGenerator);
  }

  @SuppressWarnings("unused")
  public static <S> Builder<NumericalDynamicalSystem<Stepped.State<S>>, Stepped.State<S>> outStepped(
      @Param(value = "stepT", dD = 1) double interval,
      @Param("inner") Builder<? extends NumericalDynamicalSystem<S>, S> inner) {
    return (xVarNames, yVarNames) -> NumericalDynamicalSystem.from(
        new OutStepped<>(inner.apply(xVarNames, yVarNames), interval), xVarNames.size(), yVarNames.size());
  }

  @SuppressWarnings("unused")
  public static Builder<Sinusoidal, StatelessSystem.State> sin(
      @Param(value = "p", dNPM = "m.range(min=-1.57;max=1.57)") DoubleRange phaseRange,
      @Param(value = "f", dNPM = "m.range(min=0;max=1)") DoubleRange frequencyRange,
      @Param(value = "a", dNPM = "m.range(min=0;max=1)") DoubleRange amplitudeRange,
      @Param(value = "b", dNPM = "m.range(min=-0.5;max=0.5)") DoubleRange biasRange) {
    return (xVarNames, yVarNames) -> new Sinusoidal(
        xVarNames.size(), yVarNames.size(), phaseRange, frequencyRange, amplitudeRange, biasRange);
  }

  @SuppressWarnings("unused")
  public static <S> Builder<NumericalDynamicalSystem<Stepped.State<S>>, Stepped.State<S>> stepped(
      @Param(value = "stepT", dD = 1) double interval,
      @Param("inner") Builder<? extends NumericalDynamicalSystem<S>, S> inner) {
    return (xVarNames, yVarNames) -> NumericalDynamicalSystem.from(
        new Stepped<>(inner.apply(xVarNames, yVarNames), interval), xVarNames.size(), yVarNames.size());
  }
}
