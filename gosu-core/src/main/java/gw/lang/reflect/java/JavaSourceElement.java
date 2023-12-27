package gw.lang.reflect.java;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.SourcePositions;
import gw.lang.reflect.ILocationInfo;
import gw.lang.reflect.LocationInfo;
import gw.lang.reflect.gs.ISourceFileHandle;
import java.net.MalformedURLException;
import java.util.StringTokenizer;

/**
 */
public abstract class JavaSourceElement
{
  private LocationInfo _location;

  public abstract Tree getTree();
  public abstract IJavaClassInfo getEnclosingClass();
  public IJavaClassInfo getDeclaringClass()
  {
    return getEnclosingClass();
  }
  public CompilationUnitTree getCompilationUnitTree()
  {
    if( getEnclosingClass() instanceof JavaSourceElement )
    {
      return ((JavaSourceElement)getEnclosingClass()).getCompilationUnitTree();
    }
    return null;
  }
  public SourcePositions getSourcePositions()
  {
    if( getEnclosingClass() instanceof JavaSourceElement )
    {
      return ((JavaSourceElement)getEnclosingClass()).getSourcePositions();
    }
    return null;
  }
  public int getStartPosition()
  {
    SourcePositions sp = getSourcePositions();
    if( sp != null )
    {
      return (int)sp.getStartPosition( getCompilationUnitTree(), getTree() );
    }
    return -1;
  }
  public int getEndPosition()
  {
    SourcePositions sp = getSourcePositions();
    if( sp != null )
    {
      return (int)sp.getEndPosition( getCompilationUnitTree(), getTree() );
    }
    return -1;
  }

  public ILocationInfo getLocationInfo()
  {
    if( _location != null )
    {
      return _location;
    }

    int startPos = getStartPosition();
    int endPos =  getEndPosition();
    try
    {
      ISourceFileHandle sfh = getDeclaringClass().getSourceFileHandle();
      if( sfh != null )
      {
        _location = new LocationInfo( startPos, endPos - startPos, -1, -1, sfh.getFile().toURI().toURL() );
      }
      return _location;
    }
    catch( MalformedURLException e )
    {
      throw new RuntimeException( e );
    }
  }

  protected IJavaClassInfo findInnerSourceType( IJavaClassInfo topLevelType, String fqnInner )
  {
    String innerSuffix = fqnInner.substring( topLevelType.getName().length() + 1 );
    StringTokenizer tokenizer = new StringTokenizer( innerSuffix, "." );
    IJavaClassInfo innerType = topLevelType;
    outer:
    while( tokenizer.hasMoreTokens() )
    {
      String innerName = tokenizer.nextToken();
      for( IJavaClassInfo innerClass: innerType.getDeclaredClasses() )
      {
        if( innerClass.getSimpleName().equals( innerName ) )
        {
          innerType = innerClass;
          continue outer;
        }
      }
      throw new IllegalStateException( "Should have found inner type" );
    }
    return innerType;
  }
}
