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
import io.github.ericmedvet.jnb.datastructure.FormattedNamedFunction;
import io.github.ericmedvet.jnb.datastructure.NamedFunction;
import io.github.ericmedvet.jnb.datastructure.Pair;
import io.github.ericmedvet.jsdynsym.control.Simulation;
import io.github.ericmedvet.jsdynsym.control.SingleAgentTask;
import io.github.ericmedvet.jsdynsym.control.navigation.Arena;
import io.github.ericmedvet.jsdynsym.control.navigation.NavigationEnvironment;
import io.github.ericmedvet.jsdynsym.control.navigation.NavigationEnvironment.State.SymbolicAction;
import io.github.ericmedvet.jsdynsym.control.navigation.State;
import io.github.ericmedvet.jviz.core.geometry.Point;
import io.github.ericmedvet.jviz.core.geometry.Semiline;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

@Discoverable(prefixTemplate = "dynamicalSystem|dynSys|ds.environment|env|e.navigation|nav|n")
public class NavigationFunctions {

  private NavigationFunctions() {
  }

  @Cacheable
  public static <X> FormattedNamedFunction<X, Double> arenaCoverage(
      @Param(value = "name", iS = "arena.coverage[{xBins}x{yBins}]") String name,
      @Param(value = "of", dNPM = "f.identity()") Function<X, Simulation.Outcome<SingleAgentTask.Step<double[], double[], State>>> beforeF,
      @Param(value = "xBins", dI = 10) int xBins,
      @Param(value = "yBins", dI = 10) int yBins,
      @Param(value = "format", dS = "%5.3f") String format
  ) {
    BiFunction<Double, Integer, Integer> quantizer = (v, n) -> Math.min(
        (int) Math.round(v * (double) n),
        n - 1
    );
    Function<Simulation.Outcome<SingleAgentTask.Step<double[], double[], State>>, Double> f = o -> {
      Arena arena = o.snapshots()
          .get(o.snapshots().firstKey())
          .state()
          .configuration()
          .arena();
      long nOfVisitedCells = o.snapshots()
          .values()
          .stream()
          .map(
              s -> new Pair<>(
                  quantizer.apply(s.state().robotPosition().x() / arena.xExtent(), xBins),
                  quantizer.apply(s.state().robotPosition().y() / arena.yExtent(), yBins)
              )
          )
          .distinct()
          .count();
      return (double) nOfVisitedCells / (double) (xBins * yBins);
    };
    return FormattedNamedFunction.from(f, format, name)
        .compose(beforeF);
  }

  @Cacheable
  public static <X> FormattedNamedFunction<X, Double> avgD(
      @Param(value = "name", iS = "avg.dist") String name,
      @Param(value = "of", dNPM = "f.identity()") Function<X, Simulation.Outcome<SingleAgentTask.Step<double[], double[], State>>> beforeF,
      @Param(value = "format", dS = "%5.3f") String format
  ) {
    Function<Simulation.Outcome<SingleAgentTask.Step<double[], double[], State>>, Double> f = o -> o.snapshots()
        .values()
        .stream()
        .mapToDouble(s -> s.state().robotPosition().distanceTo(s.state().targetPosition()))
        .average()
        .orElseThrow();
    return FormattedNamedFunction.from(f, format, name).compose(beforeF);
  }

  @Cacheable
  public static <X> FormattedNamedFunction<X, Double> avgGapToObstacle(
      @Param(value = "name", iS = "avg.gap[d={direction:%.2f}]") String name,
      @Param(value = "of", dNPM = "f.identity()") Function<X, Simulation.Outcome<SingleAgentTask.Step<double[], double[], NavigationEnvironment.State>>> beforeF,
      @Param(value = "direction", dD = -Math.PI / 2d) double direction,
      @Param(value = "format", dS = "%5.3f") String format
  ) {
    Function<Simulation.Outcome<SingleAgentTask.Step<double[], double[], NavigationEnvironment.State>>, Double> f = outcome -> outcome
        .snapshots()
        .values()
        .stream()
        .mapToDouble(s -> {
          Point robotP = s.state().robotPosition();
          double robotA = s.state().robotDirection();
          Semiline sl = new Semiline(robotP, robotA);
          return s.state()
              .configuration()
              .arena()
              .segments()
              .stream()
              .map(sl::intersectionWith)
              .filter(Optional::isPresent)
              .mapToDouble(op -> op.orElseThrow().distanceTo(robotP))
              .min()
              .orElse(Double.POSITIVE_INFINITY);
        })
        .average()
        .orElse(0d);
    return FormattedNamedFunction.from(f, format, name).compose(beforeF);
  }

  @Cacheable
  public static <X> NamedFunction<X, Point> closestRobotP(
      @Param(value = "name", iS = "closest.pos") String name,
      @Param(value = "of", dNPM = "f.identity()") Function<X, Simulation.Outcome<SingleAgentTask.Step<double[], double[], State>>> beforeF,
      @Param(value = "normalized", dB = true) boolean normalized
  ) {
    Function<Simulation.Outcome<SingleAgentTask.Step<double[], double[], State>>, Point> f = o -> {
      Arena arena = o.snapshots()
          .values()
          .iterator()
          .next()
          .state()
          .configuration()
          .arena();
      Point p = o.snapshots()
          .values()
          .stream()
          .min(
              Comparator.comparingDouble(
                  s -> s.state().robotPosition().distanceTo(s.state().targetPosition())
              )
          )
          .map(s -> s.state().robotPosition())
          .orElseThrow();
      if (normalized) {
        return new Point(
            new DoubleRange(0, arena.xExtent()).normalize(p.x()),
            new DoubleRange(0, arena.yExtent()).normalize(p.y())
        );
      }
      return p;
    };
    return NamedFunction.from(f, name).compose(beforeF);
  }

  @Cacheable
  public static <X> FormattedNamedFunction<X, Double> distanceFromTarget(
      @Param(value = "name", iS = "dist") String name,
      @Param(value = "of", dNPM = "f.identity()") Function<X, State> beforeF,
      @Param(value = "format", dS = "%5.3f") String format
  ) {
    Function<State, Double> f = s -> s.robotPosition().distanceTo(s.targetPosition());
    return FormattedNamedFunction.from(f, format, name).compose(beforeF);
  }

  @Cacheable
  public static <X> FormattedNamedFunction<X, Double> finalD(
      @Param(value = "name", iS = "final.dist") String name,
      @Param(value = "of", dNPM = "f.identity()") Function<X, Simulation.Outcome<SingleAgentTask.Step<double[], double[], State>>> beforeF,
      @Param(value = "format", dS = "%5.3f") String format
  ) {
    Function<Simulation.Outcome<SingleAgentTask.Step<double[], double[], State>>, Double> f = o -> o.snapshots()
        .get(o.snapshots().lastKey())
        .state()
        .robotPosition()
        .distanceTo(o.snapshots().get(o.snapshots().lastKey()).state().targetPosition());
    return FormattedNamedFunction.from(f, format, name).compose(beforeF);
  }

  @Cacheable
  public static <X> NamedFunction<X, Point> finalRobotP(
      @Param(value = "name", iS = "final.robot.pos") String name,
      @Param(value = "of", dNPM = "f.identity()") Function<X, Simulation.Outcome<SingleAgentTask.Step<double[], double[], State>>> beforeF,
      @Param(value = "normalized", dB = true) boolean normalized
  ) {
    Function<Simulation.Outcome<SingleAgentTask.Step<double[], double[], State>>, Point> f = o -> {
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
            new DoubleRange(0, arena.yExtent()).normalize(p.y())
        );
      }
      return p;
    };
    return NamedFunction.from(f, name).compose(beforeF);
  }

  @Cacheable
  public static <X> NamedFunction<X, Point> finalTargetP(
      @Param(value = "name", iS = "final.target.pos") String name,
      @Param(value = "of", dNPM = "f.identity()") Function<X, Simulation.Outcome<SingleAgentTask.Step<double[], double[], State>>> beforeF,
      @Param(value = "normalized", dB = true) boolean normalized
  ) {
    Function<Simulation.Outcome<SingleAgentTask.Step<double[], double[], State>>, Point> f = o -> {
      Arena arena = o.snapshots()
          .values()
          .iterator()
          .next()
          .state()
          .configuration()
          .arena();
      Point p = o.snapshots().get(o.snapshots().lastKey()).state().targetPosition();
      if (normalized) {
        return new Point(
            new DoubleRange(0, arena.xExtent()).normalize(p.x()),
            new DoubleRange(0, arena.yExtent()).normalize(p.y())
        );
      }
      return p;
    };
    return NamedFunction.from(f, name).compose(beforeF);
  }

  @Cacheable
  public static <X> FormattedNamedFunction<X, Double> finalTime(
      @Param(value = "name", iS = "final.time") String name,
      @Param(value = "of", dNPM = "f.identity()") Function<X, Simulation.Outcome<SingleAgentTask.Step<double[], double[], State>>> beforeF,
      @Param(value = "format", dS = "%5.3f") String format
  ) {
    Function<Simulation.Outcome<SingleAgentTask.Step<double[], double[], State>>, Double> f = o -> o.snapshots()
        .lastKey();
    return FormattedNamedFunction.from(f, format, name).compose(beforeF);
  }


  @Cacheable
  public static <X> FormattedNamedFunction<X, Double> finalTimePlusD(
      @Param(value = "name", iS = "final.td") String name,
      @Param(value = "of", dNPM = "f.identity()") Function<X, Simulation.Outcome<SingleAgentTask.Step<double[], double[], State>>> beforeF,
      @Param(value = "epsilon", dD = .01) double epsilon,
      @Param(value = "format", dS = "%5.3f") String format
  ) {
    Function<Simulation.Outcome<SingleAgentTask.Step<double[], double[], State>>, Double> f = o -> {
      SortedMap<Double, SingleAgentTask.Step<double[], double[], State>> snapshots = o.snapshots();
      double stopTime = snapshots.lastKey();
      double lastDistance = snapshots
          .get(stopTime)
          .state()
          .robotPosition()
          .distanceTo(snapshots.get(stopTime).state().targetPosition());
      return stopTime + (lastDistance < epsilon ? 0d : lastDistance);
    };
    return FormattedNamedFunction.from(f, format, name).compose(beforeF);
  }

  @Cacheable
  public static <X> FormattedNamedFunction<X, Double> minD(
      @Param(value = "name", iS = "min.dist") String name,
      @Param(value = "of", dNPM = "f.identity()") Function<X, Simulation.Outcome<SingleAgentTask.Step<double[], double[], State>>> beforeF,
      @Param(value = "format", dS = "%5.3f") String format
  ) {
    Function<Simulation.Outcome<SingleAgentTask.Step<double[], double[], State>>, Double> f = o -> o.snapshots()
        .values()
        .stream()
        .mapToDouble(s -> s.state().robotPosition().distanceTo(s.state().targetPosition()))
        .min()
        .orElseThrow();
    return FormattedNamedFunction.from(f, format, name).compose(beforeF);
  }

  @Cacheable
  public static <X> FormattedNamedFunction<X, String> symbolicTrajectory(
      @Param(value = "name", iS = "symbolic.trajectory[{collapse}]") String name,
      @Param(value = "of", dNPM = "f.identity()") Function<X, Simulation.Outcome<SingleAgentTask.Step<double[], double[], NavigationEnvironment.State>>> beforeF,
      @Param(value = "movTRate", dD = 0.1) double movTRate,
      @Param(value = "turnT", dD = 0.25) double turnT,
      @Param(value = "forwardSymbol", dS = "↑") String forwardSymbol,
      @Param(value = "backwardSymbol", dS = "↓") String backwardSymbol,
      @Param(value = "forwardLeftSymbol", dS = "↖") String forwardLeftSymbol,
      @Param(value = "forwardRightSymbol", dS = "↗") String forwardRightSymbol,
      @Param(value = "backwardLeftSymbol", dS = "↙") String backwardLeftSymbol,
      @Param(value = "backwardRightSymbol", dS = "↘") String backwardRightSymbol,
      @Param(value = "rotateLeftSymbol", dS = "↶") String rotateLeftSymbol,
      @Param(value = "rotateRightSymbol", dS = "↷") String rotateRightSymbol,
      @Param(value = "stopSymbol", dS = "o") String stopSymbol,
      @Param("collapse") boolean collapse,
      @Param(value = "format", dS = "%s") String format
  ) {
    Map<SymbolicAction, String> symbols = new EnumMap<>(SymbolicAction.class);
    symbols.put(SymbolicAction.FORWARD, forwardSymbol);
    symbols.put(SymbolicAction.FORWARD_LEFT, forwardLeftSymbol);
    symbols.put(SymbolicAction.FORWARD_RIGHT, forwardRightSymbol);
    symbols.put(SymbolicAction.BACKWARD, backwardSymbol);
    symbols.put(SymbolicAction.BACKWARD_LEFT, backwardLeftSymbol);
    symbols.put(SymbolicAction.BACKWARD_RIGHT, backwardRightSymbol);
    symbols.put(SymbolicAction.STOP, stopSymbol);
    symbols.put(SymbolicAction.ROTATE_LEFT, rotateLeftSymbol);
    symbols.put(SymbolicAction.ROTATE_RIGHT, rotateRightSymbol);
    Function<Simulation.Outcome<SingleAgentTask.Step<double[], double[], NavigationEnvironment.State>>, String> f = o -> o
        .snapshots()
        .values()
        .stream()
        .map(s -> symbols.getOrDefault(s.state().symbolicAction(movTRate, turnT), stopSymbol))
        .collect(Collectors.joining());
    Function<String, String> collapseF = s -> {
      StringBuilder sb = new StringBuilder();
      sb.append(s.charAt(0));
      for (int i = 1; i < s.length(); i = i + 1) {
        char c = s.charAt(i);
        if (c != sb.charAt(sb.length() - 1)) {
          sb.append(c);
        }
      }
      return sb.toString();
    };
    if (collapse) {
      f = f.andThen(collapseF);
    }
    return FormattedNamedFunction.from(f, format, name).compose(beforeF);
  }

  @Cacheable
  public static <X> FormattedNamedFunction<X, Double> x(
      @Param(value = "name", iS = "x") String name,
      @Param(value = "of", dNPM = "f.identity()") Function<X, Point> beforeF,
      @Param(value = "format", dS = "%5.3f") String format
  ) {
    Function<Point, Double> f = Point::x;
    return FormattedNamedFunction.from(f, format, name).compose(beforeF);
  }

  @Cacheable
  public static <X> FormattedNamedFunction<X, Double> y(
      @Param(value = "name", iS = "y") String name,
      @Param(value = "of", dNPM = "f.identity()") Function<X, Point> beforeF,
      @Param(value = "format", dS = "%5.3f") String format
  ) {
    Function<Point, Double> f = Point::y;
    return FormattedNamedFunction.from(f, format, name).compose(beforeF);
  }
}