
package io.github.ericmedvet.jsdynsym.core;

import java.util.function.Function;

public interface TimeInvariantStatelessSystem<I, O>
    extends StatelessSystem<I, O>,
        TimeInvariantDynamicalSystem<I, O, StatelessSystem.State>,
        Function<I, O> {
  @Override
  default O apply(I input) {
    return step(input);
  }
}
