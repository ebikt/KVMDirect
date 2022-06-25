package ebik.http;

import java.util.Vector;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.IOException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.StatusLine;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.cookie.Cookie;

import ebik.kvm.Gui; //FIXME

public class Session {
  protected OrderedDict headers;
  private String prefix;
  private String hostPrefix;
  private String protocol;
  private CloseableHttpClient implClient;
  private Log log = LogFactory.getLog(Session.class);
  private org.apache.http.client.CredentialsProvider   creds = null;
  private org.apache.http.client.CookieStore           cook = null;

  public static org.apache.http.impl.client.HttpClientBuilder basicBuilder = null;

  public static Session basic() throws MalformedURLException {
    if (basicBuilder == null)
      basicBuilder = org.apache.http.impl.client.HttpClients.custom().useSystemProperties();
    return new Session(basicBuilder);
  }

  public Session (HttpClientBuilder b) throws MalformedURLException {
    headers = new OrderedDict();
    creds = new org.apache.http.impl.client.BasicCredentialsProvider();
    cook  = new org.apache.http.impl.client.BasicCookieStore();
    implClient = b
      .setDefaultCredentialsProvider(creds)
      .setDefaultCookieStore(cook)
      //DO NOT .disableContentCompression(), iDRAC8 needs this
      .build();
  }

  public String getPrefix() {
    return prefix;
  }

  public void setPrefix(String newPrefix) throws MalformedURLException {
    if (newPrefix == null) {
      prefix = null;
      hostPrefix = null;
      protocol = null;
    }
    newPrefix = absoluteUrl(newPrefix);
    if (!(newPrefix.startsWith("http://") || newPrefix.startsWith("https://")))
      throw new MalformedURLException("Unsupported protocol: "+newPrefix);
    String[] split = newPrefix.split("/");
    if (split[2].length() == 0)
      throw new MalformedURLException("Empty host: "+newPrefix);
    protocol = split[0];
    hostPrefix = split[0] + "//" + split[2];
    if (! newPrefix.endsWith("/")) {
      prefix = newPrefix + "/";
    } else {
      prefix = newPrefix;
    }
  }

  public String absoluteUrl(String url) throws MalformedURLException {
    int pos = url.indexOf("://");
    if (pos > 0) {
      boolean isProto = true;
      for (int i=0; i<pos; i++) {
	int c = url.charAt(i);
	if (Character.isLetterOrDigit(c)) continue;
	switch (c) {
	  case '_':
	  case '-':
	  case '+':
	  case '=':
	    continue;
	}
	isProto = false;
	break;
      }
      if (isProto) return url;
    }
    if (prefix == null) 
      throw new MalformedURLException("First url must be absolute url including protocol, got:" + url);
    if (url.startsWith("//")) {
      if (url.length()<3 || url.charAt(2) == '/')
	throw new MalformedURLException("Empty host: "+url);
      return protocol + url;
    }
    if (url.startsWith("/"))
      return hostPrefix + url;
    return prefix + url;
  }
  public String[] absoluteUrls(String[] urls) throws MalformedURLException {
    String ret[] = new String[urls.length];
    for (int i=0; i<urls.length; i++)
      ret[i] = absoluteUrl(urls[i]);
    return ret;
  }

  public Session addHeader(String key, String value) { headers.add(key, value); return this; }
  public Session setHeader(String key, String value) { headers.set(key, value); return this; }
  public Session removeHeader(String key) { headers.remove(key); return this; }
  public String getHeader(String key) { return headers.get(key); }
  public Vector<String> getAllHeader(String key) { return headers.getAll(key); }

  protected void traceCookie(String msg, Cookie c) {
    log.trace(msg + " cookie: " + c.getName() + "=" + c.getValue());
    log.trace("  +- domain: " + c.getDomain());
    log.trace("  +- path: " + c.getPath());
    log.trace("  +- comment: " + c.getComment());
    log.trace("  +- commentUrl: " + c.getCommentURL());
    log.trace("  +- port: " + java.util.Arrays.toString(c.getPorts()));
    log.trace("  +- version: " + c.getVersion());
    log.trace("  +- expired?: " + c.isExpired(new java.util.Date()));
    log.trace("  +- persistent?: " + c.isPersistent());
    log.trace("  +- secure?: " + c.isSecure());
  }

  public Cookie newCookie(String key, String value) {
    org.apache.http.impl.cookie.BasicClientCookie c =
      new org.apache.http.impl.cookie.BasicClientCookie(key, value);
    c.setDomain(prefix.split("/")[2]);
    c.setPath("/");
    //traceCookie("New cookie ", c);
    return c;
  }

  public Session addCookie(String key, String value) {
    log.trace("Adding cookie " + key + "=" + value);
    cook.addCookie(newCookie(key, value));
    return this;
  }

  public Session setCookie(String key, String value) {
    log.trace("Setting cookie " + key + "=" + value);
    Cookie nc = null;
    if (value != null) nc = newCookie(key, value);
    java.util.Vector<Cookie> vc = new java.util.Vector<Cookie>();
    for (Cookie c: cook.getCookies()) {
      //traceCookie("Previous cookie ", c);
      if (key.equals(c.getName())) {
	if (nc != null) {
	  vc.add(nc);
	  nc = null;
	}
      } else vc.add(c);
    }
    if (nc != null)
	vc.add(nc);
    cook.clear();
    for (Cookie c: vc)
      cook.addCookie(c);
    return this;
  }

  public Session removeCookie(String key) {
    return setCookie(key, null);
  }

  public String getCookie(String key) {
    String value = null;
    for (org.apache.http.cookie.Cookie c: cook.getCookies())
      if (c.getName().equals(key)) {
	value = c.getValue();
	break;
      }
    log.trace("Extracting cookie " + key + "=" + value);
    return value;
  }

  public void setAuthCredentials(String u, String p, String realm) {
    creds.setCredentials(
      new org.apache.http.auth.AuthScope(null, -1, realm),
      new org.apache.http.auth.UsernamePasswordCredentials(u, p)
    );
  }

  public static boolean validCookieName(String s) {
    //FIXME this is not correct, but it is minimum needed for not-quoting ILO2 cookies
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (Character.isLetterOrDigit(c)) continue;
      if (c == '_') continue;
      if (c == '-') continue;
      if (c == ':') continue;
      if (c == '+') continue;
      if (c == '=') continue;
      if (c == '/') continue;
      return false;
    }
    return true;
  }

  public Request absoluteRequest(String url) throws IOException {
    return new Request(url, headers, this);
  }

  public Request request(URL url) throws IOException {
    return absoluteRequest(url.toString());
  }

  public Request request(String path) throws IOException {
    return absoluteRequest(absoluteUrl(path));
  }

  // FIXME separate implementation
  public Response execute(Request req, boolean binary) throws IOException {
    log.trace("execute");
    Gui.setStatus("Downloading " + req.getUrl() + "...");
    HttpRequestBase hr = null;
    switch (req.getMethod()) {
      case "POST":
	log.trace("new post");
	HttpPost post = new HttpPost(req.getUrl());
	log.trace("created");
	byte[] data = req.getData();
	if (data != null)
	  post.setEntity(new ByteArrayEntity(req.getData()));
	else {
	  post.setEntity(new org.apache.http.client.entity.UrlEncodedFormEntity(req.getFormData()));
	}
	log.trace("data set");
	hr = post;
	break;
      case "GET":
	hr = new HttpGet(req.getUrl());
	break;
      default:
	throw new MalformedURLException("Invalid method");
    }
    log.trace("setting headers");
    for (OrderedDict.Pair h: req.getHeaders()) {
      hr.addHeader(h.k, h.v);
    }

    final HttpRequestBase hrf = hr;
    int tmout = req.getTimeout();
    if (tmout > 0) {
      new java.util.Timer(true).schedule(
	new java.util.TimerTask() {
	  @Override
	  public void run() { hrf.abort(); }
	}, tmout);
    }
    CloseableHttpResponse resp = implClient.execute(hrf);

    StatusLine s = resp.getStatusLine();
    OrderedDict h = new OrderedDict(resp.getAllHeaders());
    HttpEntity e = resp.getEntity();
    byte[] datab = null;
    String datas = null;
    if (binary) {
      datab = EntityUtils.toByteArray(e);
    } else {
      datas = EntityUtils.toString(e, "utf-8");
      switch (s.getStatusCode()) {
	case 200: //FIXME allow also other codes?
	  // some SM-ATEN kvm needs Referer to be set
	  headers.set("Referer", req.getUrl());
      }
    }
    EntityUtils.consume(e);
    Gui.setStatus("Downloaded (" + req.getUrl() + ") " + s.getStatusCode());
    return new Response(
      s.getStatusCode(),
      s.getReasonPhrase(),
      req.getUrl(),
      h,
      datas,
      datab,
      req);
  }
}
