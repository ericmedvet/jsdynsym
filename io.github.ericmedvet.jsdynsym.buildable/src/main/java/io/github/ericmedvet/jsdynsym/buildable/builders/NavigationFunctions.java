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
import io.github.ericmedvet.jnb.datastructure.FormattedNamedFunction;
import io.github.ericmedvet.jnb.datastructure.NamedFunction;
import io.github.ericmedvet.jsdynsym.control.Simulation;
import io.github.ericmedvet.jsdynsym.control.SingleAgentTask;
import io.github.ericmedvet.jsdynsym.control.geometry.Point;
import io.github.ericmedvet.jsdynsym.control.navigation.Arena;
import io.github.ericmedvet.jsdynsym.control.navigation.NavigationEnvironment;
import io.github.ericmedvet.jsdynsym.control.navigation.PointNavigationEnvironment;
import java.util.Comparator;
import java.util.function.Function;

@Discoverable(prefixTemplate = "dynamicalSystem|dynSys|ds.environment|env|e.navigation|nav|n")
public class NavigationFunctions {

  private NavigationFunctions() {}

  @SuppressWarnings("unused")
  public static <X> FormattedNamedFunction<X, Double> avgD(
      @Param(value = "of", dNPM = "f.identity()")
          Function<
                  X,
                  Simulation.Outcome<
                      SingleAgentTask.Step<double[], double[], NavigationEnvironment.State>>>
              beforeF,
      @Param(value = "format", dS = "%5.3f") String format) {
    Function<Simulation.Outcome<SingleAgentTask.Step<double[], double[], NavigationEnvironment.State>>, Double> f =
        o -> o.snapshots().values().stream()
            .mapToDouble(s ->
                s.state().robotPosition().distance(s.state().targetPosition()))
            .average()
            .orElseThrow();
    return FormattedNamedFunction.from(f, format, "avg.dist").compose(beforeF);
  }

  @SuppressWarnings("unused")
  public static <X> NamedFunction<X, Point> closestRobotP(
      @Param(value = "of", dNPM = "f.identity()")
          Function<
                  X,
                  Simulation.Outcome<
                      SingleAgentTask.Step<double[], double[], NavigationEnvironment.State>>>
              beforeF,
      @Param(value = "normalized", dB = true) boolean normalized) {
    Function<Simulation.Outcome<SingleAgentTask.Step<double[], double[], NavigationEnvironment.State>>, Point> f =
        o -> {
          Arena arena = o.snapshots()
              .values()
              .iterator()
              .next()
              .state()
              .configuration()
              .arena();
          Point p = o.snapshots().values().stream()
              .min(Comparator.comparingDouble(s ->
                  s.state().robotPosition().distance(s.state().targetPosition())))
              .map(s -> s.state().robotPosition())
              .orElseThrow();
          if (normalized) {
            return new Point(
                new DoubleRange(0, arena.xExtent()).normalize(p.x()),
                new DoubleRange(0, arena.yExtent()).normalize(p.y()));
          }
          return p;
        };
    return NamedFunction.from(f, "closest.pos").compose(beforeF);
  }

  @SuppressWarnings("unused")
  public static <X> FormattedNamedFunction<X, Double> finalD(
      @Param(value = "of", dNPM = "f.identity()")
          Function<
                  X,
                  Simulation.Outcome<
                      SingleAgentTask.Step<double[], double[], NavigationEnvironment.State>>>
              beforeF,
      @Param(value = "format", dS = "%5.3f") String format) {
    Function<Simulation.Outcome<SingleAgentTask.Step<double[], double[], NavigationEnvironment.State>>, Double> f =
        o -> o.snapshots()
            .get(o.snapshots().lastKey())
            .state()
            .robotPosition()
            .distance(o.snapshots()
                .get(o.snapshots().lastKey())
                .state()
                .targetPosition());
    return FormattedNamedFunction.from(f, format, "final.dist").compose(beforeF);
  }

  // TODO REFACTORING
  @SuppressWarnings("unused")
  public static <X> FormattedNamedFunction<X, Double> pointFinalD(
      @Param(value = "of", dNPM = "f.identity()")
          Function<
                  X,
                  Simulation.Outcome<
                      SingleAgentTask.Step<double[], double[], PointNavigationEnvironment.State>>>
              beforeF,
      @Param(value = "format", dS = "%5.3f") String format) {
    Function<Simulation.Outcome<SingleAgentTask.Step<double[], double[], PointNavigationEnvironment.State>>, Double>
        f = o -> o.snapshots()
        .get(o.snapshots().lastKey())
        .state()
        .robotPosition()
        .distance(o.snapshots().get(o.snapshots().lastKey()).state().targetPosition());
    return FormattedNamedFunction.from(f, format, "final.dist").compose(beforeF);
  }

  @SuppressWarnings("unused")
  public static <X> NamedFunction<X, Point> finalRobotP(
      @Param(value = "of", dNPM = "f.identity()")
          Function<
                  X,
                  Simulation.Outcome<
                      SingleAgentTask.Step<double[], double[], NavigationEnvironment.State>>>
              beforeF,
      @Param(value = "normalized", dB = true) boolean normalized) {
    Function<Simulation.Outcome<SingleAgentTask.Step<double[], double[], NavigationEnvironment.State>>, Point> f =
        o -> {
          Arena arena = o.snapshots()
              .values()
              .iterator()
              .next()
              .state()
              .configuration()
              .arena();
          Point p = o.snapshots().get(o.snapshots().lastKey()).state().robotPosition();
          if (normalized) {
            return new Point(
                new DoubleRange(0, arena.xExtent()).normalize(p.x()),
                new DoubleRange(0, arena.yExtent()).normalize(p.y()));
          }
          return p;
        };
    return NamedFunction.from(f, "final.pos").compose(beforeF);
  }

  @SuppressWarnings("unused")
  public static <X> FormattedNamedFunction<X, Double> minD(
      @Param(value = "of", dNPM = "f.identity()")
          Function<
                  X,
                  Simulation.Outcome<
                      SingleAgentTask.Step<double[], double[], NavigationEnvironment.State>>>
              beforeF,
      @Param(value = "format", dS = "%5.3f") String format) {
    Function<Simulation.Outcome<SingleAgentTask.Step<double[], double[], NavigationEnvironment.State>>, Double> f =
        o -> o.snapshots().values().stream()
            .mapToDouble(s ->
                s.state().robotPosition().distance(s.state().targetPosition()))
            .min()
            .orElseThrow();
    return FormattedNamedFunction.from(f, format, "min.dist").compose(beforeF);
  }

  @SuppressWarnings("unused")
  public static <X> FormattedNamedFunction<X, Double> x(
      @Param(value = "of", dNPM = "f.identity()") Function<X, Point> beforeF,
      @Param(value = "format", dS = "%5.3f") String format) {
    Function<Point, Double> f = Point::x;
    return FormattedNamedFunction.from(f, format, "x").compose(beforeF);
  }

  @SuppressWarnings("unused")
  public static <X> FormattedNamedFunction<X, Double> y(
      @Param(value = "of", dNPM = "f.identity()") Function<X, Point> beforeF,
      @Param(value = "format", dS = "%5.3f") String format) {
    Function<Point, Double> f = Point::y;
    return FormattedNamedFunction.from(f, format, "y").compose(beforeF);
  }
}
