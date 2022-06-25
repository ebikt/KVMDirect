package ebik.util;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.*;

public class DevelServiceLoader<S> implements java.util.Iterator<S> {
  private java.util.Vector<String> candidates;
  private String clsPrefix;
  private ClassLoader classLoader;
  private S nextInstance;
  private Class<S> wantedClass;
  
  private org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(this.getClass());
  
  public DevelServiceLoader(Class<S> cl) {
    try {
      String clName = cl.getName();
      int lastDot = clName.lastIndexOf('.');
      if (lastDot > 0) clName = clName.substring(0, lastDot);
      init(cl, pathOf(cl), clName);
    } catch (Exception e) {
      log.trace("Cannot initialize DevelServiceLoader", e);
      nextInstance = null;
    }
  }
  
  public DevelServiceLoader(Class<S> cl, Path path, String prefix) {
    try {
      init(cl, path, prefix);
    } catch (Exception e) {
      log.trace("Cannot initialize DevelServiceLoader", e);
      nextInstance = null;
    }
  }

  private Path pathOf(Class<S> cl) throws URISyntaxException {
    java.net.URL classUrl = cl.getClassLoader().getResource(cl.getName().replace('.','/')+".class");
    if (classUrl.getProtocol() != "file") return null;
    return Paths.get(classUrl.toURI()).getParent();
  }
  
  public void init(Class<S> cl, Path path, String classPrefix) throws Exception {
    if (path == null) {
      nextInstance = null;
      return;
    }
    if (classPrefix == null) clsPrefix = "";
    else if (classPrefix.length()==0) clsPrefix = "";
    else if (classPrefix.endsWith(".")) clsPrefix = classPrefix;
    else clsPrefix = classPrefix + ".";
    wantedClass = cl;
    classLoader = cl.getClassLoader();
    candidates = new java.util.Vector<String>();
    String pathStr = path.resolve("x").toString();
    final String prefix = pathStr.substring(0, pathStr.length()-1);
    final String suffix = ".class";
    log.trace("Trying to load devel services for " + cl.getName() + " in: " + path);
    Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
      public FileVisitResult
        visitFile(Path file, java.nio.file.attribute.BasicFileAttributes attrs)
        throws java.io.IOException
      {
	String p = file.toString();
	if (p.startsWith(prefix)&&p.endsWith(suffix)) {
	  String candidate = 
	    p.substring(prefix.length(),p.length()-suffix.length())
	     .replace(File.separatorChar, '.');
	  if (candidate.length()>0 && candidate.charAt(0)!='.')
	    candidates.add(candidate);
	}
        return FileVisitResult.CONTINUE;
      }
    });
    getNextInstance();
  }
  
  @SuppressWarnings("unchecked")
  private void getNextInstance() {
    while (candidates.size() > 0) {
      String clsName = clsPrefix + candidates.remove(0);
      log.trace("Inspecting " + clsName);
      try {
	Class<?> cl = classLoader.loadClass(clsName);
	if (!wantedClass.isAssignableFrom(cl)) continue;
	java.lang.reflect.Constructor<?> co = cl.getDeclaredConstructor();
	nextInstance = (S) co.newInstance();
	return;
      } catch (Exception e) {
	log.trace("Skipping "+clsName+" because " + e);
      }
    }
    nextInstance = null;
  }
  
  public boolean hasNext() {
    log.trace("has next? " + (nextInstance != null));
    return nextInstance != null;
  }
  
  public S next() {
    S ret = nextInstance;
    log.trace("next? " + (nextInstance != null));
    if (ret == null)
      throw new java.util.NoSuchElementException();
    getNextInstance();
    return ret;
  }
}
