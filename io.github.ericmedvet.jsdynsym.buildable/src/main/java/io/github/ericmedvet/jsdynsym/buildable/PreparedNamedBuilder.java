package io.github.ericmedvet.jsdynsym.buildable;

import io.github.ericmedvet.jnb.core.NamedBuilder;
import io.github.ericmedvet.jsdynsym.buildable.builders.Misc;
import io.github.ericmedvet.jsdynsym.buildable.builders.NumericalDynamicalSystems;

import java.util.List;

public class PreparedNamedBuilder {

  private final static NamedBuilder<Object> NB = NamedBuilder.empty()
      .and(NamedBuilder.fromUtilityClass(Misc.class))
      .and(List.of("numerical", "num"), NamedBuilder.fromUtilityClass(NumericalDynamicalSystems.class));

  private PreparedNamedBuilder() {
  }

  public static NamedBuilder<Object> get() {
    return NB;
  }

}
