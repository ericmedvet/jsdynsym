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

package io.github.ericmedvet.jsdynsym.control.geometry;

import io.github.ericmedvet.jnb.datastructure.DoubleRange;
import java.util.Optional;

public record Segment(Point p1, Point p2) {

  public Point center() {
    return new Point(p1.x() / 2d + p2.x() / 2d, p1.y() / 2d + p2.y() / 2d);
  }

  public double direction() {
    return p2.diff(p1).direction();
  }

  public boolean isPointInBoundingBox(Point point, double precision) {
    return point.x() >= Math.min(this.p1.x(), this.p2.x()) - precision / 2 && point.x() <= Math.max(
        this.p1.x(),
        this.p2.x()
    ) + precision / 2 && point.y() >= Math.min(this.p1.y(), this.p2.y()) - precision / 2 && point.y() <= Math.max(
        this.p1.y(),
        this.p2.y()
    ) + precision / 2;
  }

  public boolean intersect(Segment other) {
    Point v1 = p2().diff(p1());
    Point v2 = other.p2().diff(other.p1());
    if (v1.magnitude() == 0 || v2.magnitude() == 0) {
      return false;
    }
    double cramerDet = v1.y() * v2.x() - v1.x() * v2.y();
    if (cramerDet == 0) {
      if (Math.abs(other.p2().diff(p1()).direction()) != Math.abs(p2().diff(p1()).direction())) {
        return false;
      }
      if (v1.x() > 0 == other.p2().x() > p2().x()) {
        return DoubleRange.UNIT.contains((other.p2().x() - p1().x()) / v1.x());
      }
      return DoubleRange.UNIT.contains((p2().x() - other.p1().x()) / v2.x());
    }
    Point pointDiff = other.p1().diff(p1());
    return DoubleRange.UNIT.contains((pointDiff.y() * v2.x() - pointDiff.x() * v2.y()) / cramerDet) && DoubleRange.UNIT
        .contains(
            (pointDiff.y() * v1.x() - pointDiff
                .x() * v1.y()) / cramerDet
        );
  }

  public Optional<Point> intersection(Segment other, double precision) {
    final double thisDeltaX = this.p1.x() - this.p2.x();
    final double otherDeltaX = other.p1.x() - other.p2.x();
    final double thisDeltaY = this.p1.y() - this.p2.y();
    final double otherDeltaY = other.p1.y() - other.p2.y();
    double denominator = thisDeltaX * otherDeltaY - thisDeltaY * otherDeltaX;
    // if denominator is 0, the lines are parallel or coincident
    if (denominator == 0) {
      return Optional.empty();
    }
    double px = ((this.p1.x() * this.p2.y() - this.p1.y() * this.p2.x()) * otherDeltaX - thisDeltaX * (other.p1
        .x() * other.p2.y() - other.p1.y() * other.p2.x())) / denominator;
    double py = ((this.p1.x() * this.p2.y() - this.p1.y() * this.p2.x()) * otherDeltaY - thisDeltaY * (other.p1
        .x() * other.p2.y() - other.p1.y() * other.p2.x())) / denominator;
    Point intersection = new Point(px, py);
    // check if the intersection point lies on both segments
    if (isPointInBoundingBox(intersection, precision) && other.isPointInBoundingBox(
        intersection,
        precision
    )) {
      return Optional.of(intersection);
    }
    return Optional.empty();
  }

  public double length() {
    return p1.distance(p2);
  }

  @Override
  public String toString() {
    return "s(%s->%s)".formatted(p1, p2);
  }
}