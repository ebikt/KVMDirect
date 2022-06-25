package com.serverengines.cookies;
//override class from ILO1 Applet that comunicates with browser.
public final class CookieMgr {
  public static final int POOL_SIZE = 5;
  public static final String DOCUMENT = "document";
  public static final String COOKIE = "cookie";
  public static final String EXPIRES = "expires";
  public static final String COOKIE_NAME_PREFIX = "cookie";
  public static final String SEMI_COLON = ";";
  public static final String COMMA = ",";
  public static final char[] ILLEGAL_CHARS = new char[] { ";".charAt(0), "=".charAt(0) };

  private java.util.HashMap<String, String> cookies;
  private org.apache.commons.logging.Log log;

  public CookieMgr() {
    log = org.apache.commons.logging.LogFactory.getLog(this.getClass());
    cookies = new java.util.HashMap<String,String>();
  }

  @SuppressWarnings({"removal"})
  public static CookieMgr getInstance(java.applet.Applet paramApplet) {
    return new CookieMgr();
  }

  public void recycle() {}

  public void getCookies(java.util.Map<String, String> map) {
    log.trace("getCookies()");
    for (String key: cookies.keySet()) {
      log.trace("  " + key + ": " + cookies.get(key));
      map.put(key, cookies.get(key));
    }
  }

  public void writeCookies(java.util.Properties props) {
    log.trace("writeCookies()");
    cookies = new java.util.HashMap<String,String>();
    for (String key: props.stringPropertyNames()) {
      log.trace("  " + key + ": " + props.getProperty(key));
      cookies.put(key, props.getProperty(key));
    }
  }
}
