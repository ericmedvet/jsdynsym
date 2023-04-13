package io.github.ericmedvet.jsdynsym.buildable.builders;

import io.github.ericmedvet.jnb.core.Param;
import io.github.ericmedvet.jsdynsym.core.DoubleRange;

import java.util.Random;
import java.util.random.RandomGenerator;

public class Misc {

  public static RandomGenerator defaultRG(@Param(value = "seed", dI = 0) int seed) {
    return seed >= 0 ? new Random(seed) : new Random();
  }

  public static DoubleRange range(
      @Param("min") double min,
      @Param("max") double max
  ) {
    return new DoubleRange(min, max);
  }

}
