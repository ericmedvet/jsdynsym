package io.github.ericmedvet.jsdynsym.core.composed;

import io.github.ericmedvet.jsdynsym.core.DynamicalSystem;

/**
 * @author "Eric Medvet" on 2023/02/25 for jsdynsym
 */
public class InStepped<I, O, S> extends AbstractComposed<DynamicalSystem<I, O, S>> implements DynamicalSystem<I, O,
    InStepped.State<S>> {
  private final double interval;
  private double lastT;
  private I lastInput;

  public InStepped(DynamicalSystem<I, O, S> inner, double interval) {
    super(inner);
    this.interval = interval;
    lastT = Double.NEGATIVE_INFINITY;
  }

  public record State<S>(double lastT, S state) {}

  @Override
  public State<S> getState() {
    return new State<>(lastT, inner().getState());
  }

  @Override
  public void reset() {
    lastT = Double.NEGATIVE_INFINITY;
  }

  @Override
  public O step(double t, I input) {
    if (t - lastT > interval) {
      lastInput = input;
      lastT = t;
    }
    return inner().step(t, lastInput);
  }

  @Override
  public String toString() {
    return "iStepped(%s @ t=%.3f)".formatted(inner(), interval);
  }
}
