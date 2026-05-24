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
import io.github.ericmedvet.jsdynsym.control.SingleRLAgentTask;
import io.github.ericmedvet.jsdynsym.core.numerical.NumericalDynamicalSystem;
import io.github.ericmedvet.jsdynsym.core.rl.NumericalReinforcementLearningAgent;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleBiFunction;

@Discoverable(prefixTemplate = "dynamicalSystem|dynSys|ds.singleRLAgentTask|saRLTask|srlat")
public class SingleRLAgentTasks {

  private SingleRLAgentTasks() {
  }

  @SuppressWarnings("unused")
  @Cacheable
  public static <CS, ES> SingleRLAgentTask<NumericalReinforcementLearningAgent<? extends CS>, double[], double[], CS, ES> fromNumericalEnvironment(
      @Param(value = "name", iS = "{environment.name}[{reward.name}]") String name,
      @Param("environment") Environment<double[], double[], ES, NumericalDynamicalSystem<? extends CS>> environment,
      @Param(value = "stopCondition", dNPM = "predicate.not(condition = predicate.always())") Predicate<ES> stopCondition,
      @Param("resetAgent") boolean resetAgent,
      @Param(value = "initialReward", dD = 0) double initialReward,
      @Param("reward") Function<ES, Double> rewardFunction,
      @Param(value = "", injection = Param.Injection.BUILDER) NamedBuilder<?> nb,
      @Param(value = "", injection = Param.Injection.MAP) ParamMap map
  ) {
    @SuppressWarnings("unchecked") Supplier<Environment<double[], double[], ES, NumericalDynamicalSystem<? extends CS>>> supplier = () -> (Environment<double[], double[], ES, NumericalDynamicalSystem<? extends CS>>) nb
        .build((NamedParamMap) map.value("environment", ParamMap.Type.NAMED_PARAM_MAP));
    ToDoubleBiFunction<ES, double[]> actualRewardFunction = (s, action) -> rewardFunction.apply(s);
    return Naming.named(
        name,
        SingleRLAgentTask.fromNumericalEnvironment(
            supplier,
            stopCondition,
            resetAgent,
            initialReward,
            actualRewardFunction
        )
    );
  }
}