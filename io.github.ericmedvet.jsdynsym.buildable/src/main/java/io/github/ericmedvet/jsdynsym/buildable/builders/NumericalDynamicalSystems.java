package io.github.ericmedvet.jsdynsym.buildable.builders;

import io.github.ericmedvet.jnb.core.Param;
import io.github.ericmedvet.jsdynsym.core.DoubleRange;
import io.github.ericmedvet.jsdynsym.core.StatelessSystem;
import io.github.ericmedvet.jsdynsym.core.composed.InStepped;
import io.github.ericmedvet.jsdynsym.core.composed.OutStepped;
import io.github.ericmedvet.jsdynsym.core.composed.Stepped;
import io.github.ericmedvet.jsdynsym.core.numerical.EnhancedInput;
import io.github.ericmedvet.jsdynsym.core.numerical.Noised;
import io.github.ericmedvet.jsdynsym.core.numerical.NumericalDynamicalSystem;
import io.github.ericmedvet.jsdynsym.core.numerical.Sinusoidal;
import io.github.ericmedvet.jsdynsym.core.numerical.ann.DelayedRecurrentNetwork;
import io.github.ericmedvet.jsdynsym.core.numerical.ann.MultiLayerPerceptron;

import java.util.List;
import java.util.function.BiFunction;
import java.util.random.RandomGenerator;

public class NumericalDynamicalSystems {

  private NumericalDynamicalSystems() {
  }

  public interface Builder<F extends NumericalDynamicalSystem<S>, S> extends BiFunction<Integer, Integer, F> {}

  @SuppressWarnings("unused")
  public static Builder<DelayedRecurrentNetwork, DelayedRecurrentNetwork.State> drn(
      @Param(value = "timeRange", dNPM = "ds.range(min=0;max=1)") DoubleRange timeRange,
      @Param(value = "innerNeuronsRatio", dD = 1d) double innerNeuronsRatio,
      @Param(value = "activationFunction", dS = "tanh") MultiLayerPerceptron.ActivationFunction activationFunction,
      @Param(value = "threshold", dD = 0.1d) double threshold,
      @Param(value = "timeResolution", dD = 0.16666d) double timeResolution
  ) {
    return (nOfInputs, nOfOutputs) -> new DelayedRecurrentNetwork(
        activationFunction,
        nOfInputs,
        nOfOutputs,
        (int) Math.round(innerNeuronsRatio * (nOfInputs + nOfOutputs)),
        timeRange,
        threshold,
        timeResolution
    );
  }

  @SuppressWarnings("unused")
  public static <S> Builder<EnhancedInput<S>, S> enhanced(
      @Param("windowT") double windowT,
      @Param("inner") Builder<? extends NumericalDynamicalSystem<S>, S> inner,
      @Param(value = "types", dSs = {"current", "trend", "avg"}) List<EnhancedInput.Type> types
  ) {
    return (nOfInputs, nOfOutputs) -> new EnhancedInput<>(
        inner.apply(nOfInputs * types.size(), nOfOutputs),
        windowT,
        types
    );
  }

  @SuppressWarnings("unused")
  public static <S> Builder<NumericalDynamicalSystem<Stepped.State<S>>, Stepped.State<S>> inStepped(
      @Param(value = "stepT", dD = 1) double interval,
      @Param("inner") Builder<? extends NumericalDynamicalSystem<S>, S> inner
  ) {
    return (nOfInputs, nOfOutputs) -> NumericalDynamicalSystem.from(
        new InStepped<>(inner.apply(nOfInputs, nOfOutputs), interval),
        nOfInputs,
        nOfOutputs
    );
  }

  @SuppressWarnings("unused")
  public static Builder<MultiLayerPerceptron, StatelessSystem.State> mlp(
      @Param(value = "innerLayerRatio", dD = 0.65) double innerLayerRatio,
      @Param(value = "nOfInnerLayers", dI = 1) int nOfInnerLayers,
      @Param(value = "activationFunction", dS = "tanh") MultiLayerPerceptron.ActivationFunction activationFunction
  ) {
    return (nOfInputs, nOfOutputs) -> {
      int[] innerNeurons = new int[nOfInnerLayers];
      int centerSize = (int) Math.max(2, Math.round(nOfInputs * innerLayerRatio));
      if (nOfInnerLayers > 1) {
        for (int i = 0; i < nOfInnerLayers / 2; i++) {
          innerNeurons[i] = nOfInputs + (centerSize - nOfInputs) / (nOfInnerLayers / 2 + 1) * (i + 1);
        }
        for (int i = nOfInnerLayers / 2; i < nOfInnerLayers; i++) {
          innerNeurons[i] =
              centerSize + (nOfOutputs - centerSize) / (nOfInnerLayers / 2 + 1) * (i - nOfInnerLayers / 2);
        }
      } else if (nOfInnerLayers > 0) {
        innerNeurons[0] = centerSize;
      }
      return new MultiLayerPerceptron(
          activationFunction,
          nOfInputs,
          innerNeurons,
          nOfOutputs
      );
    };
  }

  @SuppressWarnings("unused")
  public static <S> Builder<Noised<S>, S> noised(
      @Param(value = "inputSigma", dD = 0) double inputSigma,
      @Param(value = "outputSigma", dD = 0) double outputSigma,
      @Param(value = "randomGenerator", dNPM = "ds.defaultRG()") RandomGenerator randomGenerator,
      @Param("inner") Builder<? extends NumericalDynamicalSystem<S>, S> inner
  ) {
    return (nOfInputs, nOfOutputs) -> new Noised<>(
        inner.apply(nOfInputs, nOfOutputs),
        inputSigma,
        outputSigma,
        randomGenerator
    );
  }

  @SuppressWarnings("unused")
  public static <S> Builder<NumericalDynamicalSystem<Stepped.State<S>>, Stepped.State<S>> outStepped(
      @Param(value = "stepT", dD = 1) double interval,
      @Param("inner") Builder<? extends NumericalDynamicalSystem<S>, S> inner
  ) {
    return (nOfInputs, nOfOutputs) -> NumericalDynamicalSystem.from(
        new OutStepped<>(inner.apply(nOfInputs, nOfOutputs), interval),
        nOfInputs,
        nOfOutputs
    );
  }

  @SuppressWarnings("unused")
  public static Builder<Sinusoidal, StatelessSystem.State> sin(
      @Param(value = "p", dNPM = "ds.range(min=-1.57;max=1.57)") DoubleRange phaseRange,
      @Param(value = "f", dNPM = "ds.range(min=0;max=1)") DoubleRange frequencyRange,
      @Param(value = "a", dNPM = "ds.range(min=0;max=1)") DoubleRange amplitudeRange,
      @Param(value = "b", dNPM = "ds.range(min=-0.5;max=0.5)") DoubleRange biasRange
  ) {
    return (nOfInputs, nOfOutputs) -> new Sinusoidal(
        nOfInputs,
        nOfOutputs,
        phaseRange,
        frequencyRange,
        amplitudeRange,
        biasRange
    );
  }

  @SuppressWarnings("unused")
  public static <S> Builder<NumericalDynamicalSystem<Stepped.State<S>>, Stepped.State<S>> stepped(
      @Param(value = "stepT", dD = 1) double interval,
      @Param("inner") Builder<? extends NumericalDynamicalSystem<S>, S> inner
  ) {
    return (nOfInputs, nOfOutputs) -> NumericalDynamicalSystem.from(
        new Stepped<>(inner.apply(nOfInputs, nOfOutputs), interval),
        nOfInputs,
        nOfOutputs
    );
  }

}
