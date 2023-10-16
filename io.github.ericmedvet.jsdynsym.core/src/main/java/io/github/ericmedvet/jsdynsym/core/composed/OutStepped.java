
package io.github.ericmedvet.jsdynsym.core.composed;

import io.github.ericmedvet.jsdynsym.core.DynamicalSystem;

public class OutStepped<I, O, S> extends AbstractComposed<DynamicalSystem<I, O, S>>
    implements DynamicalSystem<I, O, Stepped.State<S>> {
  private final double interval;
  private double lastT;
  private O lastOutput;

  public OutStepped(DynamicalSystem<I, O, S> inner, double interval) {
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
    O output = inner().step(t, input);
    if (t - lastT > interval) {
      lastOutput = output;
      lastT = t;
    }
    return lastOutput;
  }

  @Override
  public String toString() {
    return "oStepped[t=%.3f](%s)".formatted(interval, inner());
  }
}
