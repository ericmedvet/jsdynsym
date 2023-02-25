package io.github.ericmedvet.jsdynsym.core;

import io.github.ericmedvet.jsdynsym.core.composed.Rated;

/**
 * @author "Eric Medvet" on 2023/02/25 for jsdynsym
 */
public class Main {
  public static void main(String[] args) {
    TimeInvariantStatelessSystem<Double, Double> f = x -> x * x;
    System.out.println(f.apply(3d));

    Rated<Double, Double, ?> rF = new Rated<>(f, 1d);
    Rated<Double, Double, ?> rrF = new Rated<>(rF, 1.5d);
    for (double t = 0; t < 4d; t = t + 0.25) {
      System.out.printf("t=%5.2f f(t,sin(t))=%5.2f rF(t,sin(t))=%5.2f rrF(t,sin(t))=%5.2f\n",
          t,
          f.step(t, Math.sin(t)),
          rF.step(t, Math.sin(t)),
          rrF.step(t, Math.sin(t))
      );
    }

    System.out.println(rF.mostInner());
    System.out.println(rrF.mostInner());
  }
}
