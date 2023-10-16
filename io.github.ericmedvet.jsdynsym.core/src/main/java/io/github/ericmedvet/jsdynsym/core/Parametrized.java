
package io.github.ericmedvet.jsdynsym.core;

public interface Parametrized<P> {
  P getParams();

  void setParams(P params);
}
