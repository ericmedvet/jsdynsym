/*
 * Copyright 2023 eric
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

package io.github.ericmedvet.jsdynsym.grid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
public abstract class AbstractGrid<T> implements Grid<T> {

  private final int w;
  private final int h;
  private final List<Key> keys;

  public AbstractGrid(int w, int h) {
    this.w = w;
    this.h = h;
    List<Key> localKeys = new ArrayList<>(w() * h());
    for (int x = 0; x < w(); x++) {
      for (int y = 0; y < h(); y++) {
        localKeys.add(new Key(x, y));
      }
    }
    keys = Collections.unmodifiableList(localKeys);
  }

  protected void checkValidity(Key key) {
    if (!isValid(key)) {
      throw new IllegalArgumentException("Invalid coords (%d,%d) on a %dx%d grid".formatted(
          key.x(),
          key.y(),
          w(),
          h()
      ));
    }
  }

  @Override
  public int h() {
    return h;
  }

  @Override
  public int w() {
    return w;
  }

  @Override
  public List<Key> keys() {
    return keys;
  }

  @Override
  public int hashCode() {
    return Objects.hash(w, h);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    AbstractGrid<?> that = (AbstractGrid<?>) o;
    return w == that.w && h == that.h;
  }

  @Override
  public String toString() {
    return "%s(%dx%d)%s".formatted(getClass().getSimpleName(), w(), h(), entries());
  }

}
