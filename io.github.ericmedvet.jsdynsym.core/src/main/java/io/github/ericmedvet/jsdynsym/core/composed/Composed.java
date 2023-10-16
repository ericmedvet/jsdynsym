
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

package io.github.ericmedvet.jsdynsym.core.composed;

import java.util.Optional;

public interface Composed<C> {
  C inner();

  static <K> Optional<K> deepest(Object o, Class<K> kClass) {
    if (o instanceof Composed<?> c) {
      return c.deepest(kClass);
    }
    if (kClass.isAssignableFrom(o.getClass())) {
      //noinspection unchecked
      return Optional.of((K) o);
    }
    return Optional.empty();
  }

  static <K> Optional<K> shallowest(Object o, Class<K> kClass) {
    if (kClass.isAssignableFrom(o.getClass())) {
      //noinspection unchecked
      return Optional.of((K) o);
    }
    if (o instanceof Composed<?> c) {
      return c.shallowest(kClass);
    }
    return Optional.empty();
  }

  default Object deepest() {
    if (inner() instanceof Composed<?> composed) {
      return composed.deepest();
    }
    return inner();
  }

  default <K> Optional<K> deepest(Class<K> kClass) {
    if (inner() instanceof Composed<?> composed) {
      Optional<K> inside = composed.deepest(kClass);
      if (inside.isPresent()) {
        return inside;
      }
    }
    if (kClass.isAssignableFrom(inner().getClass())) {
      //noinspection unchecked
      return Optional.of((K) inner());
    }
    if (kClass.isAssignableFrom(getClass())) {
      //noinspection unchecked
      return Optional.of((K) this);
    }
    return Optional.empty();
  }

  default <K> Optional<K> shallowest(Class<K> kClass) {
    if (kClass.isAssignableFrom(getClass())) {
      //noinspection unchecked
      return Optional.of((K) this);
    }
    if (kClass.isAssignableFrom(inner().getClass())) {
      //noinspection unchecked
      return Optional.of((K) inner());
    }
    if (inner() instanceof Composed<?> composed) {
      Optional<K> inside = composed.shallowest(kClass);
      if (inside.isPresent()) {
        return inside;
      }
    }
    return Optional.empty();
  }
}
