/*-
 * ========================LICENSE_START=================================
 * jsdynsym-control
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
package io.github.ericmedvet.jsdynsym.control.navigation;

import io.github.ericmedvet.jnb.datastructure.Grid;
import io.github.ericmedvet.jnb.datastructure.Grid.Entry;
import io.github.ericmedvet.jnb.datastructure.Grid.Key;
import io.github.ericmedvet.jnb.datastructure.TriFunction;
import io.github.ericmedvet.jviz.core.geometry.Point;
import io.github.ericmedvet.jviz.core.geometry.Segment;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public interface Arena {

  double xExtent();

  double yExtent();

  List<Segment> obstacles();

  static Arena of(double xExtent, double yExtent, List<Segment> obstacles) {
    record HardArena(double xExtent, double yExtent, List<Segment> obstacles) implements Arena {

    }
    return new HardArena(xExtent, yExtent, obstacles);
  }

  default Arena yMirrored() {
    return of(
        xExtent(),
        yExtent(),
        obstacles().stream()
            .map(
                segment -> new Segment(
                    new Point(xExtent() - segment.p1().x(), segment.p1().y()),
                    new Point(xExtent() - segment.p2().x(), segment.p2().y())
                )
            )
            .toList()
    );
  }

  default Arena xMirrored() {
    return of(
        xExtent(),
        yExtent(),
        obstacles().stream()
            .map(
                segment -> new Segment(
                    new Point(segment.p1().x(), yExtent() - segment.p1().y()),
                    new Point(segment.p2().x(), yExtent() - segment.p2().y())
                )
            )
            .toList()
    );
  }

  enum Prepared {
    EMPTY(of(1, 1, List.of())), A_BARRIER(
        of(1, 1, List.of(new Segment(new Point(0.40, 0.3), new Point(0.60, 0.3))))
    ), B_BARRIER(
        of(1, 1, List.of(new Segment(new Point(0.35, 0.3), new Point(0.65, 0.3))))
    ), C_BARRIER(
        of(1, 1, List.of(new Segment(new Point(0.30, 0.3), new Point(0.70, 0.3))))
    ), D_BARRIER(
        of(1, 1, List.of(new Segment(new Point(0.25, 0.3), new Point(0.75, 0.3))))
    ), E_BARRIER(
        of(1, 1, List.of(new Segment(new Point(0.20, 0.3), new Point(0.80, 0.3))))
    ), U_BARRIER(
        of(
            1,
            1,
            List.of(
                new Segment(new Point(0.3, 0.3), new Point(0.7, 0.3)),
                new Segment(new Point(0.3, 0.3), new Point(0.3, 0.5)),
                new Segment(new Point(0.7, 0.3), new Point(0.7, 0.5))
            )
        )
    ), EASY_MAZE(
        of(
            1,
            1,
            List.of(
                new Segment(new Point(0, 0.4), new Point(0.7, 0.3)),
                new Segment(new Point(1, 0.7), new Point(0.3, 0.6))
            )
        )
    ), FLAT_MAZE(
        of(
            1,
            1,
            List.of(
                new Segment(new Point(0, 0.35), new Point(0.7, 0.35)),
                new Segment(new Point(1, 0.65), new Point(0.3, 0.65))
            )
        )
    ), A_MAZE(
        of(
            1,
            1,
            List.of(
                new Segment(new Point(0, 0.33), new Point(0.45, 0.33)),
                new Segment(new Point(1, 0.66), new Point(0.55, 0.66))
            )
        )
    ), B_MAZE(
        of(
            1,
            1,
            List.of(
                new Segment(new Point(0, 0.33), new Point(0.52, 0.33)),
                new Segment(new Point(1, 0.66), new Point(0.48, 0.66))
            )
        )
    ), C_MAZE(
        of(
            1,
            1,
            List.of(
                new Segment(new Point(0, 0.33), new Point(0.59, 0.33)),
                new Segment(new Point(1, 0.66), new Point(0.41, 0.66))
            )
        )
    ), D_MAZE(
        of(
            1,
            1,
            List.of(
                new Segment(new Point(0, 0.33), new Point(0.66, 0.33)),
                new Segment(new Point(1, 0.66), new Point(0.34, 0.66))
            )
        )
    ), E_MAZE(
        of(
            1,
            1,
            List.of(
                new Segment(new Point(0, 0.33), new Point(0.73, 0.33)),
                new Segment(new Point(1, 0.66), new Point(0.27, 0.66))
            )
        )
    ), DECEPTIVE_MAZE(
        of(
            1,
            1,
            List.of(
                new Segment(new Point(0, 0.3), new Point(0.7, 0.4)),
                new Segment(new Point(1, 0.6), new Point(0.3, 0.7))
            )
        )
    ), STANDARD(
        of(
            //suitable starting point is 0.15,0.15; suitable target is 0.15,0.9
            1,
            1,
            List.of(
                new Segment(new Point(.35, 0), new Point(0.55, 0.2)),
                new Segment(new Point(0.25, 0.25), new Point(0.25, 0.8)),
                new Segment(new Point(0.25, 0.5), new Point(0.75, 0.25)),
                new Segment(new Point(0.45, 0.55), new Point(1, 0.3)),
                new Segment(new Point(0, 0.825), new Point(0.25, 0.8)),
                new Segment(new Point(0.25, 0.8), new Point(0.7, 0.9)),
                new Segment(new Point(0, 0.65), new Point(0.15, 0.45))
            )
        ).xMirrored()
    ), U_SHAPED(
        of(
            //suitable starting point is 0.15,0.15; suitable target is 0.85,0.15
            1,
            1,
            List.of(
                new Segment(new Point(0.35, 0), new Point(0.35, 0.7)),
                new Segment(new Point(0.35, 0.7), new Point(0.65, 0.7)),
                new Segment(new Point(0.65, 0), new Point(0.65, 0.7))
            )
        ).xMirrored()
    ), SNAKE(
        of(
            //suitable starting point is 0.1,0.1; suitable target is 0.9,0.1
            1,
            1,
            List.of(
                new Segment(new Point(0.2, 0), new Point(0.2, 0.8)),
                new Segment(new Point(0.2, 0.8), new Point(0.8, 0.8)),
                new Segment(new Point(0.4, 0.6), new Point(1, 0.6)),
                new Segment(new Point(0.2, 0.4), new Point(0.8, 0.4)),
                new Segment(new Point(0.4, 0.2), new Point(1, 0.2))
            )
        ).xMirrored()
    ), Y_MAZE(
        //suitable starting point is 0.5,0.1; suitable target is 0.5,0.7
        of(
            1,
            1,
            List.of(
                new Segment(new Point(0.5, 0.2), new Point(0.5, 0.6)),
                new Segment(new Point(0.5, 0.6), new Point(0.3, 0.8)),
                new Segment(new Point(0.5, 0.6), new Point(0.7, 0.8))
            )
        ).xMirrored()
    );

    private final Arena arena;

    Prepared(Arena arena) {
      this.arena = arena;
    }

    public Arena arena() {
      return arena;
    }
  }

  default List<Segment> boundaries() {
    return List.of(
        new Segment(new Point(0, 0), new Point(xExtent(), 0)),
        new Segment(new Point(0, 0), new Point(0, yExtent())),
        new Segment(new Point(xExtent(), yExtent()), new Point(xExtent(), 0)),
        new Segment(new Point(xExtent(), yExtent()), new Point(0, yExtent()))
    );
  }

  default List<Segment> segments() {
    return Stream.concat(boundaries().stream(), obstacles().stream()).toList();
  }

  static Arena fromGrid(Grid<Boolean> grid, double sideLength, boolean diagonal) {
    Set<List<Key>> lines = new HashSet<>();
    Predicate<Key> kp = k -> grid.isValid(k) && grid.get(k);
    grid.entries().stream().filter(Entry::value).forEach(e -> {
      Set<List<Key>> localLines = new HashSet<>();
      localLines.add(Stream.iterate(e.key(), kp, k -> k.translated(1, 0)).toList());
      localLines.add(Stream.iterate(e.key(), kp, k -> k.translated(0, 1)).toList());
      if (diagonal) {
        localLines.add(Stream.iterate(e.key(), kp, k -> k.translated(1, 1)).toList());
        localLines.add(Stream.iterate(e.key(), kp, k -> k.translated(-1, 1)).toList());
      }
      localLines.forEach(l -> {
        if (lines.stream().noneMatch(ol -> new HashSet<>(ol).containsAll(l))) {
          List<List<Key>> toRemoveLines = lines.stream().filter(l::containsAll).toList();
          toRemoveLines.forEach(lines::remove);
          lines.add(l);
        }
      });
    });
    TriFunction<Key, Integer, Integer, Point> k2p = (k, ox, oy) -> new Point(
        (k.x() + 0.5d + ox / 2d) * sideLength,
        (k.y() + 0.5d + oy / 2d) * sideLength
    );
    Predicate<Key> isBoundary = k -> k.x() == 0 || k.x() == (grid.w() - 1) || k.y() == 0 || k.y() == (grid.h() - 1);
    Function<List<Key>, Segment> l2s = l -> {
      //one point
      if (l.size() == 1) {
        return new Segment(
            k2p.apply(l.getFirst(), -1, 0),
            k2p.apply(l.getFirst(), 1, 0)
        );
      }
      //horizontal
      if (l.getFirst().y() == l.getLast().y()) {
        return new Segment(
            k2p.apply(l.getFirst(), isBoundary.test(l.getFirst()) ? -1 : 0, 0),
            k2p.apply(l.getLast(), isBoundary.test(l.getLast()) ? 1 : 0, 0)
        );
      }
      //vertical
      if (l.getFirst().x() == l.getLast().x()) {
        return new Segment(
            k2p.apply(l.getFirst(), 0, isBoundary.test(l.getFirst()) ? -1 : 0),
            k2p.apply(l.getLast(), 0, isBoundary.test(l.getLast()) ? 1 : 0)
        );
      }
      //nw->se
      if (l.getFirst().x() < l.getLast().x()) {
        return new Segment(
            k2p.apply(
                l.getFirst(),
                isBoundary.test(l.getFirst()) ? -1 : 0,
                isBoundary.test(l.getFirst()) ? -1 : 0
            ),
            k2p.apply(
                l.getLast(),
                isBoundary.test(l.getLast()) ? 1 : 0,
                isBoundary.test(l.getLast()) ? 1 : 0
            )
        );
      }
      //ne->sw
      return new Segment(
          k2p.apply(
              l.getFirst(),
              isBoundary.test(l.getFirst()) ? 1 : 0,
              isBoundary.test(l.getFirst()) ? -1 : 0
          ),
          k2p.apply(
              l.getLast(),
              isBoundary.test(l.getLast()) ? -1 : 0,
              isBoundary.test(l.getLast()) ? 1 : 0
          )
      );
    };
    return of(
        grid.w() * sideLength,
        grid.h() * sideLength,
        lines.stream().map(l2s).toList()
    );
  }

}