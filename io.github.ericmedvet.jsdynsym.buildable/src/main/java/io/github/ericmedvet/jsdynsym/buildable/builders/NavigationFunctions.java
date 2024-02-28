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
/*
 * Copyright 2024 eric
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

package io.github.ericmedvet.jsdynsym.buildable.builders;

import io.github.ericmedvet.jnb.core.Discoverable;
import io.github.ericmedvet.jsdynsym.control.SingleAgentTask;
import io.github.ericmedvet.jsdynsym.control.navigation.NavigationEnvironment;
import java.util.SortedMap;
import java.util.function.Function;

@Discoverable(prefixTemplate = "dynamicalSystem|dynSys|ds.environment|env|e.navigation|nav|n")
public class NavigationFunctions {
  private NavigationFunctions() {}

  @SuppressWarnings("unused")
  public static Function<
          SortedMap<Double, SingleAgentTask.Step<double[], double[], NavigationEnvironment.State>>, Double>
      finalD() {
    return map -> map.get(map.lastKey())
        .state()
        .robotPosition()
        .distance(map.get(map.lastKey()).state().targetPosition());
  }

  @SuppressWarnings("unused")
  public static Function<
          SortedMap<Double, SingleAgentTask.Step<double[], double[], NavigationEnvironment.State>>, Double>
      minD() {
    return map -> map.values().stream()
        .mapToDouble(s -> s.state().robotPosition().distance(s.state().targetPosition()))
        .min()
        .orElseThrow();
  }

  @SuppressWarnings("unused")
  public static Function<
          SortedMap<Double, SingleAgentTask.Step<double[], double[], NavigationEnvironment.State>>, Double>
      avgD() {
    return map -> map.values().stream()
        .mapToDouble(s -> s.state().robotPosition().distance(s.state().targetPosition()))
        .average()
        .orElseThrow();
  }
}
