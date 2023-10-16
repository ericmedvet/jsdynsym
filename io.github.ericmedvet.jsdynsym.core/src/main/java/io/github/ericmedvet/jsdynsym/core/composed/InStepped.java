
package io.github.ericmedvet.jsdynsym.core.composed;

import io.github.ericmedvet.jsdynsym.core.DynamicalSystem;

public class InStepped<I, O, S> extends AbstractComposed<DynamicalSystem<I, O, S>>
    implements DynamicalSystem<I, O, Stepped.State<S>> {
  private final double interval;
  private double lastT;
  private I lastInput;

  public InStepped(DynamicalSystem<I, O, S> inner, double interval) {
    super(inner);
    this.interval = interval;
    lastT = Double.NEGATIVE_INFINITY;
  }

  @Override
  public Stepped.State<S> getState() {
    return new Stepped.State<>(lastT, inner().getState());
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
    return "iStepped[t=%.3f](%s)".formatted(interval, inner());
  }
}
