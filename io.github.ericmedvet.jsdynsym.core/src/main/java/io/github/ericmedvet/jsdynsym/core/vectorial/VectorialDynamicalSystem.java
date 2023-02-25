package io.github.ericmedvet.jsdynsym.core.vectorial;

import io.github.ericmedvet.jsdynsym.core.DynamicalSystem;

/**
 * @author "Eric Medvet" on 2023/02/25 for jsdynsym
 */
public interface VectorialDynamicalSystem<S> extends DynamicalSystem<double[], double[], S> {
  int nOfInputs();
  int nOfOuputs();
}
