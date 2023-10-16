
package io.github.ericmedvet.jsdynsym.core.rl;

public interface NumericalTimeInvariantReinforcementLearningAgent<S>
    extends NumericalReinforcementLearningAgent<S>,
        TimeInvariantReinforcementLearningAgent<double[], double[], S> {}
