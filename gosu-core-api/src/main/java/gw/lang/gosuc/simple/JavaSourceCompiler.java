package gw.lang.gosuc.simple;

import gw.lang.javac.SourceJavaFileObject;
import gw.lang.parser.GosuParserFactory;
import manifold.internal.javac.IJavaParser;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class JavaSourceCompiler implements ISourceCompiler<JavaCompilationResult> {
  private final List<String> _sourceFiles;
  private final boolean _isVerbose;
  private final boolean _isNoWarn;

  public JavaSourceCompiler(List<String> sourceFiles, boolean isVerbose, boolean isNoWarn) {
    _sourceFiles = sourceFiles;
    _isVerbose = isVerbose;
    _isNoWarn = isNoWarn;
  }

  @Override
  public JavaCompilationResult compile() {
    var parser = GosuParserFactory.getInterface( IJavaParser.class );
    var errorHandler = new DiagnosticCollector<JavaFileObject>();
    List<JavaFileObject> sourceFiles = _sourceFiles.stream().map( SourceJavaFileObject::new ).collect( Collectors.toList() );
    var files = parser.compile( sourceFiles, makeJavacOptions(_isVerbose, _isNoWarn ), errorHandler );

    return JavaCompilationResult.success(files, errorHandler.getDiagnostics());
  }

  private List<String> makeJavacOptions( boolean isVerbose, boolean isNoWarn )
  {
    ArrayList<String> javacOpts = new ArrayList<>();
    javacOpts.add( "-g" );
    javacOpts.add( "-source" );
    javacOpts.add( "8" );
    javacOpts.add( "-proc:none" );
    javacOpts.add( "-Xlint:unchecked" );
    javacOpts.add( "-parameters" );

    if( isVerbose )
    {
      javacOpts.add( "-verbose" );
    }

    if( isNoWarn )
    {
      javacOpts.add( "-nowarn" );
    }

    return javacOpts;
  }
}
