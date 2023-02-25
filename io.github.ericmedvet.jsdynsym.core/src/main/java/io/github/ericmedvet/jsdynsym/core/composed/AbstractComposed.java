package io.github.ericmedvet.jsdynsym.core.composed;

/**
 * @author "Eric Medvet" on 2023/02/25 for jsdynsym
 */
public abstract class AbstractComposed<C> implements Composed<C> {
  private final C inner;

  public AbstractComposed(C inner) {
    this.inner = inner;
  }

  @Override
  public C inner() {
    return inner;
  }
}
