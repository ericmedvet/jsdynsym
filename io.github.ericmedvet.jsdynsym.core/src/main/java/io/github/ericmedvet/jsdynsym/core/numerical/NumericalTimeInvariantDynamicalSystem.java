
package io.github.ericmedvet.jsdynsym.core.numerical;

import io.github.ericmedvet.jsdynsym.core.TimeInvariantDynamicalSystem;

public interface NumericalTimeInvariantDynamicalSystem<S>
    extends NumericalDynamicalSystem<S>, TimeInvariantDynamicalSystem<double[], double[], S> {}
