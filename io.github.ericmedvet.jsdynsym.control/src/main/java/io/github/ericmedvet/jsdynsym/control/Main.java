/*-
 * ========================LICENSE_START=================================
 * jsdynsym-control
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
package io.github.ericmedvet.jsdynsym.control;

import io.github.ericmedvet.jnb.datastructure.DoubleRange;
import io.github.ericmedvet.jsdynsym.control.SingleAgentTask.Step;
import io.github.ericmedvet.jsdynsym.control.navigation.Arena.Prepared;
import io.github.ericmedvet.jsdynsym.control.navigation.NavigationDrawer;
import io.github.ericmedvet.jsdynsym.control.navigation.NavigationDrawer.Configuration;
import io.github.ericmedvet.jsdynsym.control.navigation.NavigationEnvironment;
import io.github.ericmedvet.jsdynsym.control.navigation.NavigationEnvironment.State;
import io.github.ericmedvet.jsdynsym.core.DynamicalSystem;
import io.github.ericmedvet.jsdynsym.core.numerical.NumericalStatelessSystem;
import io.github.ericmedvet.jviz.core.util.Misc;
import io.github.ericmedvet.jviz.core.util.VideoUtils;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.SortedMap;
import java.util.random.RandomGenerator;

public class Main {

  public static void main(String[] args) throws IOException {
    RandomGenerator rg = new Random();
    NavigationEnvironment environment = new NavigationEnvironment(
        new DoubleRange(0.5, 0.5),
        new DoubleRange(0.8, 0.8),
        new DoubleRange(0., 0.),
        new DoubleRange(0.6, 0.6),
        new DoubleRange(0.1, 0.1),
        0.05,
        0.01,
        DoubleRange.SYMMETRIC_UNIT,
        5,
        1,
        true,
        Prepared.LARGE_BARRIER.arena(),
        rg);
    NumericalStatelessSystem agent = NumericalStatelessSystem.from(
        7, 2, (t, in) -> new double[] {rg.nextGaussian(0.35, 0.3), rg.nextGaussian(0.5, 0.1)});
    SingleAgentTask<DynamicalSystem<double[], double[], ?>, double[], double[], State> task =
        SingleAgentTask.fromEnvironment(environment, new double[2], new DoubleRange(0, 60), 0.1);
    SortedMap<Double, Step<double[], double[], State>> outcome = task.simulate(agent);
    NavigationDrawer d = new NavigationDrawer(Configuration.DEFAULT);
    Misc.showImage(d.draw(500, 500, outcome));
    VideoUtils.encodeAndSave(d.drawAll(300, 300, outcome), 30, new File("../nav.mp4"));
  }
}
