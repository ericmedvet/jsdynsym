package io.github.ericmedvet.jsdynsym.core;

/**
 * @author "Eric Medvet" on 2023/02/25 for jsdynsym
 */
public class Main {
  public static void main(String[] args) {
    TimeInvariantStatelessSystem<Double, Double> f = x -> x * x;
    System.out.println(f.apply(3d));
  }
}
