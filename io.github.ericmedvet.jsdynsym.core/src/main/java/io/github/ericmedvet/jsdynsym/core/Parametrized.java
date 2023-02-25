package io.github.ericmedvet.jsdynsym.core;

/**
 * @author "Eric Medvet" on 2023/02/25 for jsdynsym
 */
public interface Parametrized<P> {
  P getParams();

  void setParams(P params);
}
