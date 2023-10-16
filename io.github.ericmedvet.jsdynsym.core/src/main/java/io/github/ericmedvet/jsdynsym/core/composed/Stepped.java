/*-
 * ========================LICENSE_START=================================
 * jsdynsym-core
 * %%
 * Copyright (C) 2023 Eric Medvet
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

package io.github.ericmedvet.jsdynsym.core.composed;

import io.github.ericmedvet.jsdynsym.core.DynamicalSystem;

public class Stepped<I, O, S> extends AbstractComposed<DynamicalSystem<I, O, S>>
    implements DynamicalSystem<I, O, Stepped.State<S>> {
  private final double interval;
  private double lastT;
  private O lastOutput;

  public Stepped(DynamicalSystem<I, O, S> inner, double interval) {
    super(inner);
    this.interval = interval;
    lastT = Double.NEGATIVE_INFINITY;
  }

  public record State<S>(double lastT, S state) {}

  @Override
  public State<S> getState() {
    return new State<>(lastT, inner().getState());
  }

  @Override
  public void reset() {
    lastT = Double.NEGATIVE_INFINITY;
  }

  @Override
  public O step(double t, I input) {
    if (t - lastT > interval) {
      lastOutput = inner().step(t, input);
      lastT = t;
    }
    return lastOutput;
  }

  @Override
  public String toString() {
    return "stepped[t=%.3f](%s)".formatted(interval, inner());
  }
}
