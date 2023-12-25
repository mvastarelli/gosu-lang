package gw.lang.gosuc.simple;

import gw.lang.gosuc.cli.CommandLineOptions;
import gw.util.PathUtil;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class DefaultSourceCollector implements ISourceCollector {
  private static final String[] SOURCE_EXTENSIONS = { ".gs", ".gsx", ".gst", ".java" };
  private static final Map<SourceType, Predicate<String>> _sourceTypePredicates = Map.of(
      SourceType.GOSU, s -> s.matches("^.+\\.gs[xt]?$"),
      SourceType.JAVA, s -> s.endsWith(".java")
  );

  private final Collection<String> _sources;

  public DefaultSourceCollector(CommandLineOptions options) {
    var sourceFiles = options.getSourceFiles();
    var configuredSourcePath = options.getSourcepath();

    if (!sourceFiles.isEmpty()) {
      _sources = sourceFiles;
    } else if (configuredSourcePath.isEmpty()) {
      _sources = Collections.emptyList();
    } else {
      sourceFiles = new ArrayList<>();
      for (StringTokenizer tok = new StringTokenizer(File.pathSeparator); tok.hasMoreTokens(); ) {
        String path = tok.nextToken();
        Path sourcePath = PathUtil.create(path);
        addToSources(sourcePath, sourceFiles);
      }

      _sources = sourceFiles;
    }
  }

  @Override
  public Stream<String> getByExtension(SourceType sourceType) {
    var predicate = _sourceTypePredicates.get(sourceType);
    return _sources.stream().filter(predicate);
  }

  private void addToSources( Path sourcePath, List<String> sourceFiles )
  {
    if( !PathUtil.exists( sourcePath ) )
    {
      return;
    }

    if( Files.isDirectory( sourcePath ) )
    {
      for( Path child : PathUtil.listFiles( sourcePath ) )
      {
        addToSources( child, sourceFiles );
      }
    }
    else
    {
      String absolutePathName = PathUtil.getAbsolutePathName( sourcePath );

      if( isSourceFile( absolutePathName ) )
      {
        sourceFiles.add( absolutePathName );
      }
    }
  }

  private boolean isSourceFile( String absolutePathName )
  {
    return Arrays.stream( SOURCE_EXTENSIONS ).anyMatch( e -> absolutePathName.toLowerCase().endsWith( e ) );
  }
}
