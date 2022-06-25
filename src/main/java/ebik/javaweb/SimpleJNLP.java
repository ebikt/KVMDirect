package ebik.javaweb;

import org.jdom2.input.SAXBuilder;

import ebik.config.Registry;

import org.jdom2.Element;

public class SimpleJNLP {
  private static org.apache.commons.logging.Log log =
    org.apache.commons.logging.LogFactory.getLog(SimpleJNLP.class);

  public static class UnsupportedJNLPException extends Exception { // {{{
    private static final long serialVersionUID = 5218204035065814919L;
    UnsupportedJNLPException(String msg) {
      super(msg);
    }
  } // }}}

  private static UnsupportedJNLPException U(String msg) {
    return new UnsupportedJNLPException(msg);
  }

  public static SimpleJNLP fromString(String document) throws java.io.IOException, org.jdom2.JDOMException, UnsupportedJNLPException {
    return fromReader(new java.io.StringReader(document));
  }

  public static SimpleJNLP fromReader(java.io.Reader input) throws org.jdom2.JDOMException, UnsupportedJNLPException, java.io.IOException { // {{{
    SAXBuilder sxb = new SAXBuilder();
    return new SimpleJNLP(sxb.build(input).getRootElement());
  } // }}}

  public static class Resource {
    public String name;
    public String url;
    public String osFilter;
    public String archFilter;
    public String fileName;
    boolean nativeLib;
    Resource(String tagName, String _name, String os, String arch) { // {{{
      switch (tagName) {
	case "nativelib": nativeLib = true; break;
	case "jar":       nativeLib = false; break;
	default: throw new IllegalStateException("Invalid resource tag");
      }
      name = _name;
      osFilter = os;
      archFilter = arch;
    } // }}}
  }

  protected String codeBase = null;
  protected String jarName = null;
  protected String mainClass = null;
  protected String[] args = null;
  protected java.util.Vector<Resource> resources = null;

  private String tag(Element el, Element parent) {
    String ret = el.getName();
    log.trace("Parsing tag <"+ret+"> of <"+parent.getName()+">");
    return ret;
  }

  SimpleJNLP(Element root) throws UnsupportedJNLPException { // {{{
    if (!"jnlp".equals(root.getName()))
      throw U("Root element must be jnlp");
    if (!"1.0+".equals(root.getAttributeValue("spec")))
      throw U("Unknown jnlp spec, want 1.0+, got:" + root.getAttributeValue("spec"));
    codeBase = root.getAttributeValue("codebase", "");
    if (codeBase.length() == 0)
      throw U("Empty codebase");

    String packExtension = "";
    boolean useVersion = false;
    java.util.HashSet<String> versions = new java.util.HashSet<String>();
    resources = new java.util.Vector<Resource> ();

    for (Element child: root.getChildren()) switch (tag(child, root)) {
      case "information":
      case "security":
      case "update":
	continue; // ingore these
      case "application-desc": {
	mainClass = child.getAttributeValue("main-class", "");
	java.util.Vector<String> vargs = new java.util.Vector<String>();
	for (Element varg: child.getChildren()) {
	  if (!"argument".equals(tag(varg, child)))
	    throw U("Unknown tag <"+varg.getName()+"> in <application-desc>");
	  if(varg.getChildren().size() > 0)
	    throw U("Tag <"+varg.getName()+"> has nested tags");
	  vargs.add(varg.getText());
	}
	args = vargs.toArray(new String[vargs.size()]);
      } break;
      case "resources": {
	String osFilter = child.getAttributeValue("os", "").trim();
	String archFilter = child.getAttributeValue("arch", "").trim();
	for (Element res: child.getChildren()) switch (tag(res, child)) {
	  case "property": {
	    if (!"true".equals(res.getAttributeValue("value", "")))
	      throw U("Unsuported resources property value "+res.getAttributeValue("name")+"="+res.getAttributeValue("value"));
	    switch (res.getAttributeValue("name", "")) {
	      case "jnlp.packEnabled":
		packExtension = ".pack.gz";
		break;
	      case "jnlp.versionEnabled":
		log.info("JNLP versionEnabled");
		useVersion = true; //FIXME test this!
		break;
	      default:
		throw U("Unsuported resources property "+res.getAttributeValue("name")+"="+res.getAttributeValue("value"));
	    }
	  } break;
	  case "j2se": {
	    for (String v: res.getAttributeValue("version", "").split(" "))
	      if (v.length() > 0)
		versions.add(v);
	  } break;
	  case "jar":
	  case "nativelib": {
	    String name = res.getAttributeValue("href", "");
	    if (name.length() == 0)
	      throw U("<"+res.getName()+"> has empty href");

	    String basename = name.split("\\?",1)[0];
	    int baseStart = 0;
	    while (true) {
	      int slash = basename.indexOf('/',baseStart);
	      if (slash < 0) break;
	      baseStart = slash + 1;
	    }
	    basename = basename.substring(baseStart);

	    Resource resInfo = new Resource(res.getName(), basename, osFilter, archFilter);
	    if (!name.endsWith(".jar"))
	      throw U("Unsuported versioned <"+res.getName()+">, href does not end with .jar:" + name);
	    String version = res.getAttributeValue("version", "");
	    if (useVersion && version.length() > 0) {
	      resInfo.url = name.substring(0, name.length()-4) + "__V" + version + ".jar" + packExtension;
	    } else {
	      resInfo.url = name + packExtension;
	    }
	    resources.add(resInfo);
	    if (!resInfo.nativeLib) {
	      if (osFilter.length() > 0 || archFilter.length() > 0)
		throw U("Unsuported architecture or os specific <jar>");
	      if (jarName != null)
		throw U("Unsuported jnlp with multiple <jar>");
	      jarName = basename;
	    }
	  } break;
	  default:
	    throw U("Unsuported <resources> child <"+child.getName()+">");
	}
      } break;
      default:
	throw U("Unsuported second-level tag <"+child.getName()+">");
    }
    log.trace("Validating");
    if (jarName == null)
      throw U("Unsuported JNLP without <jar> tag");
    if (mainClass == null)
      throw U("Unsuported JNLP without main-class attribute");
    for (Resource r1: resources) for (Resource r2: resources) if (r1 != r2) {
      if (!r1.name.equals(r2.name)) continue;
      if (r1.archFilter.length() > 0 && r2.archFilter.length() > 0 && !r1.archFilter.equals(r2.archFilter)) continue;
      if (r1.osFilter.length() > 0 && r2.osFilter.length() > 0 && !r1.osFilter.equals(r2.osFilter))
	continue;
      throw U("Colliding resources with name "+r1.name);
    }
    log.trace("Parsed");
  } // }}}

  public void blacklistResources(String prefix) {
    for (Resource r: resources)
      if (r.name.startsWith(prefix))
	r.archFilter = "blacklisted."+r.archFilter;
  }

  public void debug() { // {{{
    log.debug("codeBase: " + codeBase);
    log.debug("jarName: " + jarName);
    log.debug("mainClass: " + mainClass);
    log.debug("args: " + java.util.Arrays.toString(args));
    for(Resource ri: resources) {
      log.debug("Resource: " + ri.name);
      log.debug("  url: " + ri.url);
      log.debug("  osFilter: " + ri.osFilter);
      log.debug("  archFilter: " + ri.archFilter);
      log.debug("  nativeLib: " + ri.nativeLib);
    }
  } // }}}

  protected String os = null;
  protected String arch = null;
  protected String sysProps = null;
  protected String secProps = null;
  protected String tmpDir = null;

  public void unzipTo(ebik.javaweb.PrivateDownloader downloader, String archive) throws java.io.IOException {
    tmpDir = downloader.open("kvm-jnlp");
    String unzip = Registry.get("kvm.unzip", "unzip");
    Process unpack = new ProcessBuilder(
      unzip,
      "-n",
      archive,
      "-d", tmpDir,
      "-x","META-INF/*"
    ).inheritIO().start();
    int rv = -1;
    try {
      rv = unpack.waitFor();
    } catch (Exception e) {
      log.error("Unzip exception:", e);
    } finally {
      unpack.destroy();
    }
    if (rv != 0)
      throw new java.io.IOException("Unzip failed");
    log.info("Unzipped");
  }

  public void download(ebik.javaweb.PrivateDownloader downloader) throws java.io.IOException {
    String os   = System.getProperty("os.name");
    String arch = System.getProperty("os.arch");
    String jver = System.getProperty("java.version");
    log.debug("os:"+os+" arch:"+arch+" jver:"+jver);
    tmpDir = downloader.open("kvm-jnlp");
    ebik.http.Session session = downloader.getSession(); //FIXME hack
    session.setPrefix(codeBase);
    for (Resource r: resources) {
      boolean osAllow = "".equals(r.osFilter) || os.equals(r.osFilter);
      boolean archAllow = "".equals(r.archFilter) || arch.equals(r.archFilter);
      log.trace("Resource "+r.name+" os:"+r.osFilter+" ("+osAllow+") arch:"+r.archFilter+" ("+archAllow+")");
      if (!(osAllow && archAllow)) continue;
      r.fileName = downloader.download(r.url, r.name);
      if (r.nativeLib) unzipTo(downloader, r.fileName);
    }
    secProps = downloader.writeProperties(
      java.security.Security.class,
      "java.security",
      "jdk.tls.disabledAlgorithms",
      "jdk.certpath.disabledAlgorithms"
    );
  }
  public int run() throws java.io.IOException {
    int rv = -1;
    String javaExe = Registry.get("kvm.jnlp.java");
    if (javaExe == null) try {
      // Java 9 runtime...
      //ProcessHandle processHandle = ProcessHandle.current();
      //String javaExe = processHandle.info().command().get();
      Class<?> PH  = Class.forName("java.lang.ProcessHandle");
      java.lang.reflect.Method PHi = PH.getDeclaredMethod("info");
      Object ocmd = PHi.getReturnType().getDeclaredMethod("command").invoke(
                      PHi.invoke(PH.getDeclaredMethod("current").invoke(null))
		    );
      javaExe = (String) ocmd.getClass().getDeclaredMethod("get").invoke(ocmd);
    } catch (Exception e) {
      log.trace("Java 9+ runtime detection failed", e);
    }
    if (javaExe == null) {
      javaExe = System.getProperties().getProperty("java.home") + 
		java.io.File.separator + "bin" + 
		java.io.File.separator + "java";
      if (System.getProperty("os.name").startsWith("Win"))
	javaExe = javaExe + ".exe";
    }
    java.util.Vector<String> cmdLine = new java.util.Vector<String>();
    cmdLine.add(javaExe);
    cmdLine.add("-Djava.library.path=" + tmpDir);
    cmdLine.add("-Dhttps.protocols=" + System.getProperty("https.protocols"));
    cmdLine.add("-Dhttps.cipherSuites=" + System.getProperty("https.cipherSuites"));
    cmdLine.add("-Djava.security.properties=" + secProps);
    if (mainClass == null || "".equals(mainClass.trim())) {
      cmdLine.add("-jar");
      cmdLine.add(tmpDir + java.io.File.separator + jarName);
    } else {
      cmdLine.add("-cp");
      cmdLine.add(tmpDir + java.io.File.separator + jarName);
      cmdLine.add(mainClass);
    }
    for (String a: args)
      cmdLine.add(a);

    log.info(cmdLine);
    Process java = new ProcessBuilder(
      cmdLine.toArray(new String[cmdLine.size()])
    ).inheritIO().start();
    try {
      rv = java.waitFor();
    } catch (Exception e) {
      log.error("Java execute exception:", e);
    } finally {
      java.destroy();
    }
    return rv;
  }
}
