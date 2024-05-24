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
import io.github.ericmedvet.jnb.core.Param;
import io.github.ericmedvet.jnb.datastructure.DoubleRange;
import io.github.ericmedvet.jsdynsym.control.Environment;
import io.github.ericmedvet.jsdynsym.control.SingleAgentTask;
import io.github.ericmedvet.jsdynsym.core.DynamicalSystem;

import java.util.function.Predicate;

@Discoverable(prefixTemplate = "dynamicalSystem|dynSys|ds.singleAgentTask|saTask|sat")
public class SingleAgentTasks {

  private SingleAgentTasks() {}

  @SuppressWarnings("unused")
  public static <C extends DynamicalSystem<O, A, ?>, O, A, S> SingleAgentTask<C, O, A, S> fromEnvironment(
      @Param(value = "name", iS = "{environment.name}") String name,
      @Param("environment") Environment<O, A, S> environment,
      @Param("stopCondition") Predicate<O> stopCondition,
      @Param("tRange") DoubleRange tRange,
      @Param("dT") double dT) {
    return SingleAgentTask.fromEnvironment(environment, stopCondition, tRange, dT);
  }
}
