package io.github.ericmedvet.jsdynsym.control.navigation;

import io.github.ericmedvet.jsdynsym.control.geometry.Point;

public interface State {
    Configuration configuration();
    Point targetPosition();

    Point robotPosition();

    int nOfCollisions();
}
