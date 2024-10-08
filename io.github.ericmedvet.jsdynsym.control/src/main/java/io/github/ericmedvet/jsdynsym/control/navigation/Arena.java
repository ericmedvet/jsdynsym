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
package io.github.ericmedvet.jsdynsym.control.navigation;

import io.github.ericmedvet.jsdynsym.control.geometry.Point;
import io.github.ericmedvet.jsdynsym.control.geometry.Segment;
import java.util.List;
import java.util.stream.Stream;

public record Arena(double xExtent, double yExtent, List<Segment> obstacles) {
  public enum Prepared {
    EMPTY(new Arena(1, 1, List.of())),
    A_BARRIER(new Arena(1, 1, List.of(new Segment(new Point(0.40, 0.3), new Point(0.60, 0.3))))),
    B_BARRIER(new Arena(1, 1, List.of(new Segment(new Point(0.35, 0.3), new Point(0.65, 0.3))))),
    C_BARRIER(new Arena(1, 1, List.of(new Segment(new Point(0.30, 0.3), new Point(0.70, 0.3))))),
    D_BARRIER(new Arena(1, 1, List.of(new Segment(new Point(0.25, 0.3), new Point(0.75, 0.3))))),
    E_BARRIER(new Arena(1, 1, List.of(new Segment(new Point(0.20, 0.3), new Point(0.80, 0.3))))),
    U_BARRIER(new Arena(
        1,
        1,
        List.of(
            new Segment(new Point(0.3, 0.3), new Point(0.7, 0.3)),
            new Segment(new Point(0.3, 0.3), new Point(0.3, 0.5)),
            new Segment(new Point(0.7, 0.3), new Point(0.7, 0.5))))),
    EASY_MAZE(new Arena(
        1,
        1,
        List.of(
            new Segment(new Point(0, 0.4), new Point(0.7, 0.3)),
            new Segment(new Point(1, 0.7), new Point(0.3, 0.6))))),
    FLAT_MAZE(new Arena(
        1,
        1,
        List.of(
            new Segment(new Point(0, 0.35), new Point(0.7, 0.35)),
            new Segment(new Point(1, 0.65), new Point(0.3, 0.65))))),
    A_MAZE(new Arena(
        1,
        1,
        List.of(
            new Segment(new Point(0, 0.33), new Point(0.45, 0.33)),
            new Segment(new Point(1, 0.66), new Point(0.55, 0.66))))),
    B_MAZE(new Arena(
        1,
        1,
        List.of(
            new Segment(new Point(0, 0.33), new Point(0.52, 0.33)),
            new Segment(new Point(1, 0.66), new Point(0.48, 0.66))))),
    C_MAZE(new Arena(
        1,
        1,
        List.of(
            new Segment(new Point(0, 0.33), new Point(0.59, 0.33)),
            new Segment(new Point(1, 0.66), new Point(0.41, 0.66))))),
    D_MAZE(new Arena(
        1,
        1,
        List.of(
            new Segment(new Point(0, 0.33), new Point(0.66, 0.33)),
            new Segment(new Point(1, 0.66), new Point(0.34, 0.66))))),
    E_MAZE(new Arena(
        1,
        1,
        List.of(
            new Segment(new Point(0, 0.33), new Point(0.73, 0.33)),
            new Segment(new Point(1, 0.66), new Point(0.27, 0.66))))),
    DECEPTIVE_MAZE(new Arena(
        1,
        1,
        List.of(
            new Segment(new Point(0, 0.3), new Point(0.7, 0.4)),
            new Segment(new Point(1, 0.6), new Point(0.3, 0.7)))));

    private final Arena arena;

    Prepared(Arena arena) {
      this.arena = arena;
    }

    public Arena arena() {
      return arena;
    }
  }

  public List<Segment> boundaries() {
    return List.of(
        new Segment(new Point(0, 0), new Point(xExtent, 0)),
        new Segment(new Point(0, 0), new Point(0, yExtent)),
        new Segment(new Point(xExtent, yExtent), new Point(xExtent, 0)),
        new Segment(new Point(xExtent, yExtent), new Point(0, yExtent)));
  }

  public List<Segment> segments() {
    return Stream.concat(boundaries().stream(), obstacles.stream()).toList();
  }
}
