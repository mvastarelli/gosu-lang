package gw.lang.gosuc.simple;

import gw.lang.gosuc.cli.CommandLineOptions;
import gw.lang.javac.SourceJavaFileObject;
import gw.lang.parser.GosuParserFactory;
import manifold.internal.javac.IJavaParser;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class JavaSourceCompiler implements ISourceCompiler<JavaCompilationResult> {
  private CommandLineOptions _options;
  private final List<String> _sourceFiles;

  public JavaSourceCompiler(CommandLineOptions options, List<String> sourceFiles) {
    _options = options;
    _sourceFiles = sourceFiles;
  }

  @Override
  public JavaCompilationResult compile() {
    var isVerbose = _options.isVerbose();
    var isNoWarn = _options.isNoWarn();
    var parser = GosuParserFactory.getInterface( IJavaParser.class );
    var errorHandler = new DiagnosticCollector<JavaFileObject>();
    List<JavaFileObject> sourceFiles = _sourceFiles.stream().map( SourceJavaFileObject::new ).collect( Collectors.toList() );
    var files = parser.compile( sourceFiles, makeJavacOptions(isVerbose, isNoWarn ), errorHandler );

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
