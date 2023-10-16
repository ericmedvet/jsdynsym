
package io.github.ericmedvet.jsdynsym.core.numerical;

import io.github.ericmedvet.jsdynsym.core.StatelessSystem;
import io.github.ericmedvet.jsdynsym.core.TimeInvariantStatelessSystem;

public interface NumericalTimeInvariantStatelessSystem
    extends NumericalDynamicalSystem<StatelessSystem.State>,
        TimeInvariantStatelessSystem<double[], double[]> {}
