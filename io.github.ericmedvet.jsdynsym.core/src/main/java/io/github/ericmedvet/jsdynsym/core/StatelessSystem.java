
package io.github.ericmedvet.jsdynsym.core;

@FunctionalInterface
public interface StatelessSystem<I, O> extends DynamicalSystem<I, O, StatelessSystem.State> {

  record State() {
    public static State EMPTY = new State();
  }

  @Override
  default State getState() {
    return State.EMPTY;
  }

  @Override
  default void reset() {}
}
