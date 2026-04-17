/*-
 * ========================LICENSE_START=================================
 * jsdynsym-buildable
 * %%
 * Copyright (C) 2023 - 2025 Eric Medvet
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

import io.github.ericmedvet.jnb.core.Cacheable;
import io.github.ericmedvet.jnb.core.Discoverable;
import io.github.ericmedvet.jnb.core.NamedBuilder;
import io.github.ericmedvet.jnb.core.NamedParamMap;
import io.github.ericmedvet.jnb.core.Param;
import io.github.ericmedvet.jnb.core.ParamMap;
import io.github.ericmedvet.jsdynsym.buildable.util.Naming;
import io.github.ericmedvet.jsdynsym.control.Environment;
import io.github.ericmedvet.jsdynsym.control.SingleAgentTask;
import io.github.ericmedvet.jsdynsym.core.DynamicalSystem;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Discoverable(prefixTemplate = "dynamicalSystem|dynSys|ds.singleAgentTask|saTask|sat")
public class SingleAgentTasks {

  private SingleAgentTasks() {
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <C extends DynamicalSystem<O, A, CS>, O, A, CS, ES> SingleAgentTask<C, O, A, CS, ES> fromEnvironment(
      @Param(value = "name", iS = "{environment.name}") String name,
      @Param("environment") Environment<O, A, ES, C> environment,
      @Param(value = "stopCondition", dNPM = "predicate.not(condition = predicate.always())") Predicate<ES> stopCondition,
      @Param(value = "resetAgent", dB = true) boolean resetAgent,
      @Param(value = "", injection = Param.Injection.BUILDER) NamedBuilder<?> nb,
      @Param(value = "", injection = Param.Injection.MAP) ParamMap map
  ) {
    @SuppressWarnings("unchecked") Supplier<Environment<O, A, ES, C>> supplier = () -> (Environment<O, A, ES, C>) nb
        .build((NamedParamMap) map.value("environment", ParamMap.Type.NAMED_PARAM_MAP));
    return Naming.named(name, SingleAgentTask.fromEnvironment(supplier, stopCondition, resetAgent));
  }

  @Cacheable
  public static <C extends DynamicalSystem<O, A, ? extends CS>, O, A, CS, TS> SingleAgentTask<C, O, A, CS, TS> sequential(
      @Param(value = "name", iS = "seq.tasks") String name,
      @Param("tasks") List<? extends SingleAgentTask<C, O, A, CS, TS>> tasks,
      @Param("resetFirst") boolean resetFirst,
      @Param("resetAll") boolean resetAll
  ) {
    return SingleAgentTask.sequential(tasks, resetFirst, resetAll);
  }
}