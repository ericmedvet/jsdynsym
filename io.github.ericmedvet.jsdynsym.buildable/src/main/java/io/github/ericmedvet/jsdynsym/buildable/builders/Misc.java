/*-
 * ========================LICENSE_START=================================
 * jsdynsym-buildable
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

package io.github.ericmedvet.jsdynsym.buildable.builders;

import io.github.ericmedvet.jnb.core.Discoverable;
import io.github.ericmedvet.jnb.core.Param;
import io.github.ericmedvet.jnb.datastructure.Grid;
import io.github.ericmedvet.jsdynsym.core.DoubleRange;
import java.util.List;
import java.util.Random;
import java.util.random.RandomGenerator;

@Discoverable(prefixTemplate = "dynamicalSystem|dynSys|ds")
public class Misc {
  private Misc() {}

  @SuppressWarnings("unused")
  public static RandomGenerator defaultRG(@Param(value = "seed", dI = 0) int seed) {
    return seed >= 0 ? new Random(seed) : new Random();
  }

  @SuppressWarnings("unused")
  public static <T> Grid<T> grid(@Param("w") int w, @Param("h") int h, @Param("items") List<T> items) {
    if (items.size() != w * h) {
      throw new IllegalArgumentException(
          "Wrong number of items: %d x %d = %d expected, %d found".formatted(w, h, w * h, items.size()));
    }
    Grid<T> grid = Grid.create(w, h);
    int c = 0;
    for (Grid.Key k : grid.keys()) {
      grid.set(k, items.get(c));
      c = c + 1;
    }
    return grid;
  }

  @SuppressWarnings("unused")
  public static DoubleRange range(@Param("min") double min, @Param("max") double max) {
    return new DoubleRange(min, max);
  }
}
