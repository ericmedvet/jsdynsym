package io.github.ericmedvet.jsdynsym.core.vectorial;

import io.github.ericmedvet.jsdynsym.core.StatelessSystem;

/**
 * @author "Eric Medvet" on 2023/02/25 for jsdynsym
 */
public interface VectorialStatelessSystem extends VectorialDynamicalSystem<StatelessSystem.State>,
    StatelessSystem<double[], double[]> {
}
