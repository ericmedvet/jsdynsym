package io.github.ericmedvet.jsdynsym.core.vectorial;

import io.github.ericmedvet.jsdynsym.core.TimeInvariantDynamicalSystem;

/**
 * @author "Eric Medvet" on 2023/02/25 for jsdynsym
 */
public interface VectorialTimeInvariantDynamicalSystem<S> extends VectorialDynamicalSystem<S>,
    TimeInvariantDynamicalSystem<double[], double[], S> {
}
