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
import io.github.ericmedvet.jnb.core.Param;
import io.github.ericmedvet.jnb.datastructure.FormattedNamedFunction;
import io.github.ericmedvet.jsdynsym.control.navigation.State;
import java.util.function.Function;

@Discoverable(prefixTemplate = "dynamicalSystem|dynSys|ds.environment|env|e.navigation|nav|n.reward")
public class NavigationRewards {

  private NavigationRewards() {
  }

  @Cacheable
  public static <X> FormattedNamedFunction<X, Double> reaching(
      @Param(value = "name", iS = "reaching[{targetProximityRadius:%.2f}]") String name,
      @Param(value = "of", dNPM = "f.identity()") Function<X, State> beforeF,
      @Param(value = "targetProximityRadius", dD = 0.05) double targetProximityRadius,
      @Param(value = "targetProximityReward", dD = 1) double targetProximityReward,
      @Param(value = "collisionPenalty", dD = 0.01) double collisionPenalty,
      @Param(value = "distanceWeight", dD = -0.01) double distanceWeight,
      @Param(value = "distanceDecreaseWeight", dD = 0.1) double distanceDecreaseWeight,
      @Param(value = "format", dS = "%5.3f") String format
  ) {
    Function<State, Double> f = s -> {
      double currentDistance = s.robotPosition().distanceTo(s.targetPosition());
      double previousDistance = s.robotPreviousPosition().distanceTo(s.targetPosition());
      double reward = distanceDecreaseWeight * (previousDistance - currentDistance);
      reward = reward + distanceWeight * currentDistance;
      reward = reward + (currentDistance < targetProximityRadius ? targetProximityReward : 0d);
      reward = reward - (s.hasCollided() ? collisionPenalty : 0d);
      return reward;
    };
    return FormattedNamedFunction.from(f, format, name).compose(beforeF);
  }

}