package io.github.ericmedvet.jsdynsym.core.numerical;

import io.github.ericmedvet.jsdynsym.core.StatelessSystem;

/**
 * @author "Eric Medvet" on 2023/02/25 for jsdynsym
 */
public interface NumericalStatelessSystem extends NumericalDynamicalSystem<StatelessSystem.State>,
    StatelessSystem<double[], double[]> {
}
