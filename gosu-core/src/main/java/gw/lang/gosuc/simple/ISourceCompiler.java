package gw.lang.gosuc.simple;

public interface ISourceCompiler<TResult extends CompilationResult> {
  TResult compile();
}

