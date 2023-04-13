module io.github.ericmedvet.jsdynsym.buildable {
  requires io.github.ericmedvet.jsdynsym.core;
  requires io.github.ericmedvet.jnb.core;
  exports io.github.ericmedvet.jsdynsym.buildable;
  opens io.github.ericmedvet.jsdynsym.buildable.builders to io.github.ericmedvet.jnb.core;
}