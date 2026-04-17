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
import io.github.ericmedvet.jnb.datastructure.DoubleRange;
import io.github.ericmedvet.jsdynsym.control.Simulation;
import io.github.ericmedvet.jsdynsym.control.navigation.NavigationArena;
import io.github.ericmedvet.jsdynsym.control.navigation.NavigationEnvironment;
import io.github.ericmedvet.jsdynsym.control.navigation.VariableSensorPositionsNavigation;
import io.github.ericmedvet.jsdynsym.control.synthetic.BooleanUtils;
import io.github.ericmedvet.jsdynsym.control.synthetic.SequentialBooleanFunction;
import io.github.ericmedvet.jsdynsym.control.synthetic.SequentialXor;
import io.github.ericmedvet.jsdynsym.core.bool.BooleanFunction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.random.RandomGenerator;

@Discoverable(prefixTemplate = "dynamicalSystem|dynSys|ds.simulation|sim|s")
public class Simulations {

  private Simulations() {
  }

  @Cacheable
  public static <T, S, O extends Simulation.Outcome<S>> Simulation<T, S, Simulation.Outcome<S>> sequential(
      @Param(value = "name", iS = "seq.sims") String name,
      @Param("relativeV") List<? extends Simulation<T, S, O>> simulations
  ) {
    return Simulation.sequential(simulations);
  }

  @Cacheable
  public static SequentialBooleanFunction<?> sequentialBf(
      @Param(value = "name", iS = "sequentialBf") String name,
      @Param("target") BooleanFunction target,
      @Param("cases") List<String> cases,
      @Param(value = "rewardType", dS = "unlimited") BooleanUtils.ScoreType rewardType,
      @Param("resetAgent") boolean resetAgent,
      @Param("shuffle") boolean shuffle,
      @Param(value = "randomGenerator", dNPM = "m.defaultRG()") RandomGenerator randomGenerator
  ) {
    List<String> casesList = new ArrayList<>(cases);
    if (shuffle) {
      Collections.shuffle(casesList, randomGenerator);
    }
    return new SequentialBooleanFunction<>(
        casesList.stream()
            .map(BooleanUtils::stringToBitString)
            .toList(),
        target,
        rewardType,
        resetAgent
    );
  }

  @Cacheable
  public static SequentialXor<?> sequentialXor(
      @Param(value = "name", iS = "sequentialXor") String name,
      @Param(value = "cases", dSs = {"00", "01", "10", "11"}) List<String> cases,
      @Param(value = "rewardType", dS = "unlimited") BooleanUtils.ScoreType rewardType,
      @Param("resetAgent") boolean resetAgent,
      @Param("shuffle") boolean shuffle,
      @Param(value = "randomGenerator", dNPM = "m.defaultRG()") RandomGenerator randomGenerator
  ) {
    List<String> casesList = new ArrayList<>(cases);
    if (shuffle) {
      Collections.shuffle(casesList, randomGenerator);
    }
    return new SequentialXor<>(
        casesList.stream()
            .map(s -> BooleanUtils.bitStringToDoubleString(BooleanUtils.stringToBitString(s)))
            .toList(),
        rewardType,
        resetAgent
    );
  }

  @Cacheable
  public static VariableSensorPositionsNavigation<?> variableSensorPositionsNavigation(
      @Param(value = "name", iS = "vs[{nOfSensors}]-nav-{arena}") String name,
      @Param(value = "initialRobotDirectionRange", dNPM = "m.range(min=0;max=0)") DoubleRange initialRobotDirectionRange,
      @Param(value = "robotRadius", dD = 0.05) double robotRadius,
      @Param(value = "robotMaxV", dD = 0.01) double robotMaxV,
      @Param(value = "nOfSensors", dI = 5) int nOfSensors,
      @Param(value = "sensorRange", dD = .5) double sensorRange,
      @Param(value = "targetSensing", dS = "limited") NavigationEnvironment.Configuration.TargetSensing targetSensing,
      @Param(value = "arena", dNPM = "ds.arena.prepared()") NavigationArena arena,
      @Param(value = "rescaleInput", dB = true) boolean rescaleInput,
      @Param("relativeV") boolean relativeSpeed,
      @Param(value = "sortAngles", dB = true) boolean sortAngles,
      @Param(value = "randomGenerator", dNPM = "m.defaultRG()") RandomGenerator randomGenerator
  ) {
    return new VariableSensorPositionsNavigation<>(
        new NavigationEnvironment.Configuration(
            initialRobotDirectionRange,
            robotRadius,
            robotMaxV,
            List.of(),
            sensorRange,
            targetSensing,
            arena,
            rescaleInput,
            relativeSpeed,
            randomGenerator
        ),
        nOfSensors,
        sortAngles
    );
  }
}