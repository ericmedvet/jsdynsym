package io.github.ericmedvet.jsdynsym.core;

/**
 * @author "Eric Medvet" on 2023/02/25 for jsdynsym
 */
public interface TimeInvariantDynamicalSystem<I, O, S> extends DynamicalSystem<I, O, S> {
  O step(I input);

  @Override
  default O step(double t, I input) {
    return step(input);
  }
}
