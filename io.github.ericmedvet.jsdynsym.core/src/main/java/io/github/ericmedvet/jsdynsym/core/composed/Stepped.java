package io.github.ericmedvet.jsdynsym.core.composed;

import io.github.ericmedvet.jsdynsym.core.DynamicalSystem;

/**
 * @author "Eric Medvet" on 2023/02/25 for jsdynsym
 */
public class Stepped<I, O, S> extends AbstractComposed<DynamicalSystem<I, O, S>> implements DynamicalSystem<I, O,
    Stepped.State<S>> {
  private final double interval;
  private double lastT;
  private O lastOutput;

  public Stepped(DynamicalSystem<I, O, S> inner, double interval) {
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
      lastOutput = inner().step(t, input);
      lastT = t;
    }
    return lastOutput;
  }

  @Override
  public String toString() {
    return "stepped[t=%.3f](%s)".formatted(interval, inner());
  }
}
