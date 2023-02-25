package io.github.ericmedvet.jsdynsym.core;

/**
 * @author "Eric Medvet" on 2023/02/25 for jsdynsym
 */
public interface StatelessSystem<I, O> extends DynamicalSystem<I, O, StatelessSystem.State> {

  State EMPTY = new State();

  record State() {}

  @Override
  default State getState() {
    return EMPTY;
  }

  @Override
  default void reset() {
  }
}
