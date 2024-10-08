/*-
 * ========================LICENSE_START=================================
 * jsdynsym-core
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

package io.github.ericmedvet.jsdynsym.core;

@FunctionalInterface
public interface StatelessSystem<I, O> extends DynamicalSystem<I, O, StatelessSystem.State> {

  record State() {
    public static final State EMPTY = new State();
  }

  @Override
  default State getState() {
    return State.EMPTY;
  }

  @Override
  default void reset() {}

  default <P> StatelessSystem<I, P> andThen(StatelessSystem<O, P> other) {
    StatelessSystem<I, O> thisSystem = this;
    return (t, input) -> other.step(t, thisSystem.step(t, input));
  }
}
