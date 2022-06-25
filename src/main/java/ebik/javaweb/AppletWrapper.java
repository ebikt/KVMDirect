package ebik.javaweb;

import java.awt.event.ComponentAdapter;

@SuppressWarnings({"deprecation","removal"})
public class AppletWrapper {
  //Hide all deprecations here
  
  private java.applet.Applet applet;
  
  public AppletWrapper(Object apl) {
    applet = (java.applet.Applet) apl;
  }
  public void setStub(SimpleStub ss) {
    applet.setStub(ss);
  }
  public java.applet.Applet getApplet() {
    return applet;
  }
  public void addComponentListener(ComponentAdapter ca) {
    applet.addComponentListener(ca);
  }
  public int getHeight() {
    return applet.getHeight();
  }
  public int getWidth() {
    return applet.getWidth();
  }
  public void init() {
    applet.init();
  }
  public void start() {
    applet.start();
  }
  public void setVisible(boolean b) {
    applet.setVisible(b);
  }
}