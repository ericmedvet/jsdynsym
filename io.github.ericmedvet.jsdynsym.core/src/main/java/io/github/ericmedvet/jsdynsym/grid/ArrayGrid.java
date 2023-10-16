/*-
 * ========================LICENSE_START=================================
 * jsdynsym-core
 * %%
 * Copyright (C) 2023 Eric Medvet
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

package io.github.ericmedvet.jsdynsym.grid;

import java.io.Serializable;
import java.util.Arrays;

public class ArrayGrid<T> extends AbstractGrid<T> implements Serializable {

  private final Object[] ts;

  public ArrayGrid(int w, int h) {
    super(w, h);
    this.ts = new Object[w * h];
  }

  @Override
  public T get(Key key) {
    checkValidity(key);
    //noinspection unchecked
    return (T) ts[(key.y() * w()) + key.x()];
  }

  @Override
  public void set(Key key, T t) {
    checkValidity(key);
    ts[(key.y() * w()) + key.x()] = t;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + Arrays.hashCode(ts);
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    ArrayGrid<?> arrayGrid = (ArrayGrid<?>) o;
    return Arrays.equals(ts, arrayGrid.ts);
  }
}
