package io.github.ericmedvet.jsdynsym.core.composed;

/**
 * @author "Eric Medvet" on 2023/02/25 for jsdynsym
 */
public interface Composed<C> {
  C inner();
  default C mostInner() {
    if (inner() instanceof Composed<?> composed) {
      return (C)composed.mostInner(); //TODO suboptimal
    }
    return inner();
  }
}
