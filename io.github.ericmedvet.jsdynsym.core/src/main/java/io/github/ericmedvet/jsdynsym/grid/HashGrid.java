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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class HashGrid<T> extends AbstractGrid<T> implements Serializable {
  private final Map<Grid.Key, T> map;

  public HashGrid(int w, int h) {
    super(w, h);
    this.map = new HashMap<>(w * h);
  }

  @Override
  public T get(Key key) {
    checkValidity(key);
    return map.get(key);
  }

  @Override
  public void set(Key key, T t) {
    checkValidity(key);
    if (t != null) {
      map.put(key, t);
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), map);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    HashGrid<?> hashGrid = (HashGrid<?>) o;
    return map.equals(hashGrid.map);
  }
}
