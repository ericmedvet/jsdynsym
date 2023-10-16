
package io.github.ericmedvet.jsdynsym.core.rl;

public interface EnumeratedTimeInvariantReinforcementLearningAgent<S>
    extends EnumeratedReinforcementLearningAgent<S>,
        TimeInvariantReinforcementLearningAgent<Integer, Integer, S> {}
