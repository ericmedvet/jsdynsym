
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

import java.util.Collection;
import java.util.EnumSet;
import java.util.SortedMap;
import java.util.TreeMap;

public class AggregatedInput<S> extends AbstractComposed<NumericalDynamicalSystem<S>>
    implements NumericalDynamicalSystem<AggregatedInput.State<S>> {

  public record State<S>(SortedMap<Double, double[]> inputHistory, S innerState) {}

  private final double windowT;
  private final EnumSet<Type> types;
  private final SortedMap<Double, double[]> history;

  public AggregatedInput(
      NumericalDynamicalSystem<S> innerVDS, double windowT, Collection<Type> types) {
    super(innerVDS);
    if (innerVDS.nOfInputs() % types.size() != 0) {
      throw new IllegalArgumentException(
          "Cannot build VDS with %d aggregate types (%s), because inner VDS input size is wrong (%d)"
              .formatted(types.size(), types, innerVDS.nOfInputs()));
    }
    this.windowT = windowT;
    this.types = EnumSet.copyOf(types);
    this.history = new TreeMap<>();
  }

  public enum Type {
    CURRENT,
    TREND,
    AVG
  }

  @Override
  public State<S> getState() {
    return new State<>(history, inner().getState());
  }

  @Override
  public void reset() {
    inner().reset();
    history.clear();
  }

  @Override
  public double[] step(double t, double[] input) {
    // add new sample to memory
    history.put(t, input);
    // update memory
    history.keySet().stream()
        .filter(mt -> mt < t - windowT)
        .toList()
        .forEach(history.keySet()::remove);
    // build inner input
    double[] iInput = new double[inner().nOfInputs()];
    double[] firstInput = history.get(history.firstKey());
    double firstT = history.firstKey();
    int c = 0;
    for (Type type : types) {
      if (type.equals(Type.CURRENT)) {
        System.arraycopy(input, 0, iInput, c, input.length);
        c = c + input.length;
      } else if (type.equals(Type.TREND)) {
        double[] lInput = new double[input.length];
        double dT = t - firstT;
        for (int i = 0; i < input.length; i = i + 1) {
          lInput[i] = (dT == 0) ? 0 : ((input[i] - firstInput[i]) / dT);
        }
        System.arraycopy(lInput, 0, iInput, c, input.length);
        c = c + input.length;
      } else if (type.equals(Type.AVG)) {
        double[] lInput = new double[input.length];
        for (int i = 0; i < input.length; i = i + 1) {
          lInput[i] = (input[i] + firstInput[i]) / 2d;
        }
        System.arraycopy(lInput, 0, iInput, c, input.length);
        c = c + input.length;
      }
    }
    return inner().step(t, iInput);
  }

  @Override
  public int nOfInputs() {
    return inner().nOfInputs() / types.size();
  }

  @Override
  public int nOfOutputs() {
    return inner().nOfOutputs();
  }

  @Override
  public String toString() {
    return "InputDiffTRF{" + "windowT=" + windowT + ", types=" + types + '}';
  }
}
