package io.github.ericmedvet.jsdynsym.control.navigation;

import io.github.ericmedvet.jnb.datastructure.DoubleRange;

import java.util.random.RandomGenerator;

public interface Configuration {
    DoubleRange initialRobotXRange();
    DoubleRange initialRobotYRange();
    DoubleRange targetXRange();
    DoubleRange targetYRange();
    double robotMaxV();
    Arena arena();
    RandomGenerator randomGenerator();
}
