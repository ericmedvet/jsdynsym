package io.github.ericmedvet.jsdynsym.core;

/**
 * @author "Eric Medvet" on 2023/02/25 for jsdynsym
 */
public interface DynamicalSystem<I, O, S> {
  S getState();

  void reset();

  O step(double t, I input);
}
