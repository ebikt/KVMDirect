package ebik.javaweb;
import java.nio.file.*;
import java.io.IOException;

import ebik.config.Registry;
import ebik.http.Session;

public class PrivateDownloader implements java.io.Closeable {
  protected Path parent_path;
  protected Path path;
  protected boolean deleteOnClose;
  protected org.apache.commons.logging.Log log;
  protected Session session;

  public PrivateDownloader(Path _path, Session _session, boolean _deleteOnClose) {
    log = org.apache.commons.logging.LogFactory.getLog(this.getClass());
    parent_path = _path.toAbsolutePath();
    deleteOnClose = _deleteOnClose;
    session = _session;
  }

  public Session getSession() { return session; }

  public String open(String prefix) throws IOException {
    if (path!=null) return path.toString();
    java.nio.file.attribute.FileAttribute<?> perms = 
      java.nio.file.attribute.PosixFilePermissions.asFileAttribute(
	java.nio.file.attribute.PosixFilePermissions.fromString("rwx------")
      );
    Files.createDirectories(parent_path, perms);
    path = Files.createTempDirectory(parent_path, prefix, perms);
    log.info("Created temorary directory " + path);
    return path.toString();
  }

  public void close() {
    if (path == null)
      return; // Not opened
    if (!deleteOnClose)
      log.info("NOT deleting temorary directory " + path);
    else try {
      Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
	@Override
	public FileVisitResult visitFile(Path file, java.nio.file.attribute.BasicFileAttributes attrs)
	  throws IOException
	{
	  Files.delete(file);
	  return FileVisitResult.CONTINUE;
	}
	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException e)
	  throws IOException
	{
	  if (e == null) {
	    Files.delete(dir);
	    return FileVisitResult.CONTINUE;
	  } else {
	    // directory iteration failed
	    throw e;
	  }
	}
      });
      log.info("Deleting temorary directory " + path);
    } catch (IOException e) {
      log.warn("Deleting temorary directory " + path + " failed:", e);
    }
    path = null;
  }

  public String download(String url, String name) throws IOException {
    log.info("Downloading "+name+" from "+url);
    if (name.contains("/") || name.contains("\\") || name.startsWith("."))
      throw new IOException("Unsupported filename: " + name);
    byte[] data = session.request(url).getBinaryResponse(200).data(); //FIXME with redirects, with progress

    //FIXME hacky detection of pack200
    if (url.endsWith(".pack.gz")) {
      name = name + ".pack.gz";
    }

    Path dest = path.resolve(name);
    Files.write(dest, data, java.nio.file.StandardOpenOption.CREATE_NEW);
    log.info("Downloaded "+name);
    String ret = dest.toString();
    String unpack200 = Registry.get("kvm.unpack200", "unpack200");
    if (ret.endsWith(".pack.gz")) {
      log.info("Launching "+unpack200);
      String d = ret.substring(0, ret.length()-".pack.gz".length());
      //FIXME unpacker binary!
      Process unpack = new ProcessBuilder(
	unpack200,
	ret,
	d
      ).inheritIO().start();
      int rv = -1;
      try {
	rv = unpack.waitFor();
      } catch (Exception e) {
	log.error("Unpack200 exception:", e);
      } finally {
	unpack.destroy();
      }
      if (rv != 0)
	throw new IOException("Unpack200 failed");
      ret = d;
      log.info("Unpacked to "+ret);
    }
    return ret;
  }

  public String writeProperties(Class<?> c, String name, String ... properties) throws IOException {
    java.lang.reflect.Method get;
    try {
      get = c.getDeclaredMethod("getProperty", String.class);
    } catch (NoSuchMethodException e) {
      throw new IOException("Class " + c.getName() + " has no suitable getProperty() method: " + e.toString());
    }
    java.util.Properties wp = new java.util.Properties();
    for (String key: properties) {
      String v;
      try {
	v = (String) get.invoke(c, key);
      } catch (Exception e) {
	throw new IOException("Class " + c.getName() + " has no suitable getProperty() method: " + e.toString());
      }
      wp.setProperty(key, v);
    }
    Path dest = path.resolve(name);
    java.io.FileWriter wr = new java.io.FileWriter(dest.toFile());
    try {
      wp.store(wr, null);
    } finally {
      wr.close();
    }
    return dest.toString();
  }
}
