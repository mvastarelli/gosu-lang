package gw.lang.gosuc.simple;

public abstract class CompilerContext {
  protected final ICompilerDriver _driver;

  protected CompilerContext(ICompilerDriver driver) {
    _driver = driver;
  }
}
