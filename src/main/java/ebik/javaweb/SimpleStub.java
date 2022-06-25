package ebik.javaweb;

import java.net.URL;
import java.util.HashMap;

import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;
import java.applet.*;

import javax.imageio.ImageIO;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

@SuppressWarnings({"removal"})
public class SimpleStub implements AppletStub, AppletContext {
  private URL docBase;
  private URL codBase;
  private HashMap<String, String> params;
  private AppletWrapper target;
  private Log log = LogFactory.getLog(SimpleStub.class);

  public SimpleStub (
    AppletWrapper tgtApplet,
    URL documentBase,
    URL codeBase,
    HashMap<String, String> parameters
  ) {
    docBase = documentBase;
    codBase = codeBase;
    params  = parameters;
    target  = tgtApplet;
  }

  // Stub
  public boolean isActive() {
    return true;
  }

  public URL getDocumentBase() {
    return docBase;
  }

  public URL getCodeBase() {
    return codBase;
  }

  public String getParameter(String paramString) {
    return params.get(paramString);
  }

  public AppletContext getAppletContext() {
    return this;
  }

  public void appletResize(int paramInt1, int paramInt2) {
  }

  // Context
  public AudioClip getAudioClip(URL paramURL) {
    log.trace("getAudioClip: " + paramURL.toString());
    return null;
  }

  public Image getImage(URL paramURL) {
    log.trace("getImage: " + paramURL.toString());
    try {
      Image ret = ImageIO.read(paramURL);
      return ret;
    } catch (Exception e) {
      log.error("Image download error",  e);
      return null;
    }
  }

  public Applet getApplet(String paramString) {
    log.trace("getApplet: " + paramString);
    return null;
  }

  public Enumeration<Applet> getApplets() {
    log.trace("gatApplets");
    Vector<Applet> applets = new Vector<Applet>();
    applets.addElement(target.getApplet());
    return applets.elements();
  }

  public void showDocument(URL paramURL) {
    log.trace("showDocument: " + paramURL.toString());
  }

  public void showDocument(URL paramURL, String paramString) {
    log.trace("showDocument: " + paramURL.toString() + ", " + paramString);
  }

  public void showStatus(String paramString) {
    log.trace("showStatus: " + paramString);
  }

  public void setStream(String paramString, InputStream paramInputStream) throws IOException {
    log.trace("setStream: " + paramString);
  }

  public InputStream getStream(String paramString) {
    log.trace("getStream");
    return null;
  }
 
  public Iterator<String> getStreamKeys() {
    log.trace("getStreamKeys");
    return null;
  }
}
