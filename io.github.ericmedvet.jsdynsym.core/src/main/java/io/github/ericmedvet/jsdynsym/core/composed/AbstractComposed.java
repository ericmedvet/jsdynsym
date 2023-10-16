
package io.github.ericmedvet.jsdynsym.core.composed;

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
