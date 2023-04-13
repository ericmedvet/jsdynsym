package io.github.ericmedvet.jsdynsym.core.composed;

import java.util.Optional;

/**
 * @author "Eric Medvet" on 2023/02/25 for jsdynsym
 */
public interface Composed<C> {
  C inner();

  default Object mostInner() {
    if (inner() instanceof Composed<?> composed) {
      return composed.mostInner();
    }
    return inner();
  }

  default <K> Optional<K> mostInner(Class<K> kClass) {
    if (kClass.isAssignableFrom(inner().getClass())) {
      //noinspection unchecked
      return Optional.of((K) inner());
    }
    if (inner() instanceof Composed<?> composed) {
      return composed.mostInner(kClass);
    }
    return Optional.empty();
  }
}
