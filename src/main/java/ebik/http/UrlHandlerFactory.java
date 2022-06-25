package ebik.http;

import java.net.*;
import java.io.IOException;

public class UrlHandlerFactory implements URLStreamHandlerFactory {
  public static Session defaultSession = null;
  private Session s;

  public UrlHandlerFactory(Session _s) {
    s = _s;
  }
  public UrlHandlerFactory() throws java.net.MalformedURLException {
    if (defaultSession == null)
      defaultSession = Session.basic();
    s = defaultSession;
  }

  public URLStreamHandler createURLStreamHandler(String protocol) {
    if (! ("http".equals(protocol) || "https".equals(protocol)))
      return null;

    return new URLStreamHandler() {

      protected URLConnection openConnection(URL url) throws IOException {
	return new HttpURLConnection(url) {
	  private byte[] data = null;
	  public void connect() throws IOException {
	    Session _s = s;
	    if (_s == null) _s = defaultSession;
	    if (_s == null) _s = Session.basic();
	    //FIXME enable redirects
	    data = s.request(url).getBinaryResponse(200).data();
	  }
	  public java.io.InputStream getInputStream() throws IOException {
	    if (data == null)
	      connect();
	    return new java.io.ByteArrayInputStream(data);
	  }
	  public void disconnect() {};
	  public boolean usingProxy() { return false; };
	};
      }
    };
  }
}
