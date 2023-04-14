package io.github.ericmedvet.jsdynsym.core.numerical;

import io.github.ericmedvet.jsdynsym.core.DynamicalSystem;

/**
 * @author "Eric Medvet" on 2023/02/25 for jsdynsym
 */
public interface NumericalDynamicalSystem<S> extends DynamicalSystem<double[], double[], S> {
  int nOfInputs();
  int nOfOutputs();
}
