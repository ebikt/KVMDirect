package ebik.javaweb;

import java.util.HashMap;
import org.jdom2.input.SAXBuilder;
import java.io.StringReader;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.net.URL;

public class AppletDesc {
  public String className;
  public String name;
  public String baseUrl;
  public String width;
  public String height;
  public String[] jarUrl;
  public HashMap<String, String> params;
  private static Log log = null;

  public AppletDesc() {
    params = new HashMap<String, String> ();
    className = null;
    name    = null;
    baseUrl = null;
    width   = null;
    height  = null;
    jarUrl  = null;
  }

  public static org.jdom2.Element rootFromXml(String xmldoc) throws org.jdom2.JDOMException, java.io.IOException {
    SAXBuilder sxb = new SAXBuilder();
    org.jdom2.Document doc   = sxb.build(new StringReader(xmldoc));
    return doc.getRootElement();
  }

  public static String[] commaSplit(String s) {
    return s
      .replace("\n", " ")
      .replace("\r", " ")
      .replace("\t", " ")
      .split(" *, *");
  }

  public static AppletDesc fromJNLP(String jnlpdoc) throws Exception {
    if (log == null) log = LogFactory.getLog(AppletDesc.class);
    org.jdom2.Element root = rootFromXml(jnlpdoc);
    AppletDesc ret = new AppletDesc();
    for (org.jdom2.Element res: root.getChildren("resources")) {
      log.trace("resources os:" + res.getAttribute("os") + " arch:" + res.getAttribute("arch"));
      for (org.jdom2.Element jar: res.getChildren("jar")) {
	if (ret.jarUrl != null)
	  throw new Exception("multiple jars not supported");
	log.trace("jar href:" + jar.getAttribute("href") + " version:" + jar.getAttribute("version") + " main:" + jar.getAttribute("main"));
	ret.jarUrl = commaSplit(jar.getAttribute("href").getValue());
      }
      if (res.getChildren("nativelib").size() > 0)
	throw new Exception("nativelib not supported");
    }
    if (ret.jarUrl == null)
      throw new Exception("Cannot find jarUrl");
    int cnt = 0;

    for (org.jdom2.Element app: root.getChildren("applet-desc")) {
      if (cnt>0) throw new Exception("Too many applet-desc elements");
      cnt += 1;
      ret.className = app.getAttribute("main-class").getValue();
      ret.name      = app.getAttribute("name").getValue();
      ret.baseUrl   = app.getAttribute("documentbase").getValue();
      ret.width     = app.getAttribute("width").getValue();
      ret.height    = app.getAttribute("height").getValue();
      for (org.jdom2.Element par: app.getChildren("param")) {
	ret.params.put(par.getAttribute("name").getValue(), par.getAttribute("value").getValue());
      }
    }
    if (cnt < 1) throw new Exception("No applet-desc element");
    log.trace("Class: " + ret.className);
    log.trace("Name:  " + ret.name);
    log.trace("Base:  " + ret.baseUrl);
    log.trace("Width: " + ret.width);
    log.trace("Height:" + ret.height);
    log.trace("Jar:   " + java.util.Arrays.toString(ret.jarUrl));
    log.trace(ret.params);
    return ret;
  }

  public static String mustAttr(org.jsoup.nodes.Element e, String a) throws Exception {
    String ret = e.attributes().get(a);
    if (ret == null || ret.equals(""))
      throw new Exception("Failed to parse attribute " + a + " of " + e.outerHtml());
    return ret;
  }
  public static String mayAttr(org.jsoup.nodes.Element e, String a) {
    String ret = e.attributes().get(a);
    if (ret == null) return "";
    return ret;
  }

  private static org.jdom2.output.EscapeStrategy
    escStrategy = org.jdom2.output.Format.getRawFormat().getEscapeStrategy();
  public static String escAttr(String attr) {
    return org.jdom2.output.Format.escapeAttribute(escStrategy, attr);
  }

  public static AppletDesc fromHtml(String html, String baseUrl) throws Exception {
    if (log == null) log = LogFactory.getLog(AppletDesc.class);
    AppletDesc ret = new AppletDesc();
    org.jsoup.nodes.Element appEl = org.jsoup.Jsoup.parse(html).select("applet").first();
    ret.className = mustAttr(appEl, "code");
    if (ret.className.endsWith(".class"))
      ret.className = ret.className.substring(0,ret.className.length() - ".class".length());
    ret.name    = mayAttr(appEl, "name");
    ret.baseUrl = baseUrl;
    ret.baseUrl = mayAttr(appEl, "documentbase");
    ret.width   = mayAttr(appEl, "width");
    ret.height  = mayAttr(appEl, "height");
    ret.jarUrl  = commaSplit(mustAttr(appEl, "archive"));

    for (org.jsoup.nodes.Element param: appEl.select("param")) {
      String n = mustAttr(param, "name");
      String v = mayAttr(param, "value");
      log.trace("Parsed param: " + param.outerHtml() + " as "+n+"="+v);
      ret.params.put(n, v);
    }
    log.trace("Class: " + ret.className);
    log.trace("Name:  " + ret.name);
    log.trace("Base:  " + ret.baseUrl);
    log.trace("Width: " + ret.width);
    log.trace("Height:" + ret.height);
    log.trace("Jar:   " + java.util.Arrays.toString(ret.jarUrl));
    log.trace(ret.params);
    return ret;
  }

  public int getWidth()  { return Integer.parseInt(width);  }
  public int getHeight() { return Integer.parseInt(height); }

  private Class<?> mainClass = null;
  private Object mainInst = null;

  protected class MyLoader extends java.net.URLClassLoader {
    private HashMap<String, Class<?>> preloaded;

    public MyLoader(URL[] u, Class<?>[] preload) {
      //preload ILO100 overlay class
      super(u, (new Object()).getClass().getClassLoader());
      preloaded = new HashMap<String, Class<?>>();
      for (Class<?> cls: preload) {
	log.trace("Preloading class " + cls.getName());
	preloaded.put(cls.getName(), cls);
      }
    }
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      Class<?> c = preloaded.get(name);
      if (c == null)
	c = super.loadClass(name, resolve);
      else
	log.trace("Using preloaded class " + name);
      return c;
    }
  }

  public AppletWrapper loadApplet() throws Exception {
    return loadApplet(new Class<?>[]{});
  }
  
  public AppletWrapper loadApplet(Class<?>[] preload) throws Exception {
    log.info("loading applet");
    URL[] urls = new URL[jarUrl.length];
    for (int i=0; i<jarUrl.length; i++)
      urls[i] = new URL(jarUrl[i]);
    java.net.URLClassLoader ucl = new MyLoader(
      urls,
      preload
    );
    for (URL u: ucl.getURLs()) {
      log.trace("Loading classes from: " + u);
    }
    mainClass = Class.forName(className, true, ucl);
    mainInst  = mainClass.getDeclaredConstructor().newInstance();
    AppletWrapper apl = new AppletWrapper(mainInst);
    SimpleStub ss = new SimpleStub(
      apl,
      new URL(baseUrl),
      urls[0],
      params
    );
    apl.setStub(ss);
    return apl;
  }

  public Object getAppletProperty(String name) throws java.lang.NoSuchFieldException, java.lang.IllegalAccessException {
    return mainClass.getDeclaredField(name).get(mainInst);
  }
}
