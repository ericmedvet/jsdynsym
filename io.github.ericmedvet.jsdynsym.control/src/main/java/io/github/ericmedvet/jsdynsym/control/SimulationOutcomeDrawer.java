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

import io.github.ericmedvet.jsdynsym.control.Simulation.Outcome;
import io.github.ericmedvet.jviz.core.drawer.Drawer;
import io.github.ericmedvet.jviz.core.drawer.ImageBuilder;
import io.github.ericmedvet.jviz.core.drawer.Video;
import io.github.ericmedvet.jviz.core.drawer.VideoBuilder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface SimulationOutcomeDrawer<S> extends ImageBuilder<Simulation.Outcome<S>> {
  void drawSingle(Graphics2D g, double t, S s);

  @Override
  default BufferedImage build(ImageInfo imageInfo, Simulation.Outcome<S> o) {
    Drawer<SortedMap<Double, S>> lastDrawer = (g, map) -> drawSingle(g, map.lastKey(), map.get(map.lastKey()));
    Drawer<SortedMap<Double, S>> allDrawer = this::drawAll;
    return lastDrawer.andThen(allDrawer).build(imageInfo, o.snapshots());
  }

  default void drawAll(Graphics2D g, SortedMap<Double, S> ss) {}

  default VideoBuilder<Simulation.Outcome<S>> videoBuilder() {
    return new VideoBuilder<>() {
      @Override
      public Video build(VideoInfo videoInfo, Outcome<S> o) {
        Drawer<Map.Entry<Double, S>> drawer =
            (g, e) -> SimulationOutcomeDrawer.this.drawSingle(g, e.getKey(), e.getValue());
        Function<Outcome<S>, SortedMap<Double, Map.Entry<Double, S>>> splitter =
            lO -> lO.snapshots().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e, (e1, e2) -> e1, TreeMap::new));
        return VideoBuilder.from(drawer, splitter).build(videoInfo, o);
      }

      @Override
      public VideoInfo videoInfo(Outcome<S> o) {
        VideoInfo vi = VideoBuilder.super.videoInfo(o);
        ImageInfo ii = imageInfo(o);
        return new VideoInfo(ii.w(), ii.h(), vi.encoder());
      }
    };
  }
}
