package gw.lang.gosuc.simple;

public interface ICompilationOutputWriter<TResult extends CompilationResult> {
  void createOutputFiles(TResult result);
}

