package io.github.ericmedvet.jsdynsym.core.numerical;

import io.github.ericmedvet.jsdynsym.core.TimeInvariantDynamicalSystem;

/**
 * @author "Eric Medvet" on 2023/02/25 for jsdynsym
 */
public interface NumericalTimeInvariantDynamicalSystem<S> extends NumericalDynamicalSystem<S>,
    TimeInvariantDynamicalSystem<double[], double[], S> {
}
