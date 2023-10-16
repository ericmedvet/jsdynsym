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

package io.github.ericmedvet.jsdynsym.core;

import java.util.random.RandomGenerator;

public interface NumericalParametrized extends Parametrized<double[]> {
  default void randomize(RandomGenerator randomGenerator, DoubleRange range) {
    double[] oldParams = getParams();
    double[] newParams = new double[oldParams.length];
    for (int i = 0; i < newParams.length; i++) {
      newParams[i] = range.denormalize(randomGenerator.nextDouble());
    }
    setParams(newParams);
  }
}
