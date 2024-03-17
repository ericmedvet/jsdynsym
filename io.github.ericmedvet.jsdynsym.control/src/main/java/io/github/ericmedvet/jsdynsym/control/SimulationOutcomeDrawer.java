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

import io.github.ericmedvet.jviz.core.drawer.Drawer;
import io.github.ericmedvet.jviz.core.drawer.ImageBuilder;
import io.github.ericmedvet.jviz.core.drawer.VideoBuilder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface SimulationOutcomeDrawer<S>
    extends ImageBuilder<Simulation.Outcome<S>>, VideoBuilder<Simulation.Outcome<S>> {
  void drawSingle(Graphics2D g, double t, S s);

  default void drawAll(Graphics2D g, SortedMap<Double, S> ss) {}

  @Override
  default BufferedImage build(ImageInfo imageInfo, Simulation.Outcome<S> o) {
    Drawer<SortedMap<Double, S>> lastDrawer = (g, map) -> drawSingle(g, map.lastKey(), map.get(map.lastKey()));
    Drawer<SortedMap<Double, S>> allDrawer = this::drawAll;
    return lastDrawer.andThen(allDrawer).build(imageInfo, o.snapshots());
  }

  @Override
  default Video build(VideoInfo videoInfo, Simulation.Outcome<S> o) throws IOException {
    Drawer<Map.Entry<Double, S>> drawer = (g, e) -> drawSingle(g, e.getKey(), e.getValue());
    Function<Simulation.Outcome<S>, SortedMap<Double, Map.Entry<Double, S>>> splitter =
        lO -> lO.snapshots().entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e, (e1, e2) -> e1, TreeMap::new));
    return VideoBuilder.from(drawer, splitter).build(videoInfo, o);
  }
}
