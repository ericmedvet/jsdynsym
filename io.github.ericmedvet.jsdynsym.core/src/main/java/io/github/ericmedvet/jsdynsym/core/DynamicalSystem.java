
package io.github.ericmedvet.jsdynsym.core;

public interface DynamicalSystem<I, O, S> {
  S getState();

  void reset();

  O step(double t, I input);
}
