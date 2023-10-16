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
package io.github.ericmedvet.jsdynsym.buildable;

import io.github.ericmedvet.jnb.core.NamedBuilder;
import io.github.ericmedvet.jsdynsym.buildable.builders.Misc;
import io.github.ericmedvet.jsdynsym.buildable.builders.NumericalDynamicalSystems;

import java.util.List;

public class PreparedNamedBuilder {

  private static final NamedBuilder<Object> NB =
      NamedBuilder.empty()
          .and(
              List.of("dynamicalSystem", "dynSys", "ds"),
              NamedBuilder.empty()
                  .and(NamedBuilder.fromUtilityClass(Misc.class))
                  .and(
                      List.of("numerical", "num"),
                      NamedBuilder.fromUtilityClass(NumericalDynamicalSystems.class)));

  private PreparedNamedBuilder() {}

  public static NamedBuilder<Object> get() {
    return NB;
  }
}
