package ebik.kvm.module;

import ebik.config.Registry;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import ebik.http.*;

public abstract class KVM {
  public String stripPrefix = "ebik.kvm.module";
  public String getModuleName() {
    String n = this.getClass().getName();
    if (n.startsWith(stripPrefix+"."))
	return n.substring(stripPrefix.length()+1);
    return n;
  }

  protected Session session = null;
  protected ebik.javaweb.PrivateDownloader downloader;
  protected boolean logged_in = false;
  protected Registry.Getter params = new Registry.Getter("ebik.kvm.module", this.getClass());

  protected Log log = LogFactory.getLog(this.getClass());

  public KVM () {
  }

  public void setSession(Session sess) throws java.io.IOException {
    String dlDir = params.get("kvm.tempDir");
    if (dlDir == null) {
      throw new java.io.IOException("main.tempDir not set!");
    }
    boolean delOnClose = !"true".equals(params.get("kvm.keepFiles"));

    session = sess;
    downloader = new ebik.javaweb.PrivateDownloader(java.nio.file.Paths.get(dlDir), session, delOnClose);
  }

  // First this is called on "unknown" homepage.
  public abstract boolean homePageMatches(Response resp) throws Exception;

  public String getParam(String s) {
    return params.get(s);
  }

  // Then this is called to check if all needed parameters are provided.
  public String checkParams() {
    try {
      if (params.get("kvm.user") == null)
	return "User must be set";
      if (params.get("kvm.password") == null)
	return "Password must be set";
    } catch (Exception e) {
      return e.toString();
    }
    return null;
  }

  public void login() throws Exception {}

  public abstract void run(Gui gui) throws Exception;

  // This is called to start the application.
  // Please call gui.shutdown() on close event on main frame
  public void start(Gui gui) throws Exception {
    params.globalize();
    gui.registerDownloaderForShutdown(downloader);
    //FIXME: Register gui to downloader to display status
    String cp = checkParams();
    if (cp != null) throw new Exception(cp); //FIXME better Exception class
    this.login();
    logged_in = true;
    this.run(gui);
  }

  public void closeMain() {}

  public void logout() throws Exception {}

  // Called from gui.shutdown(), should dispose (main) frames
  // and logout.
  public void shutdown() throws Exception {
    closeMain();
    if (logged_in) {
      try {
	ebik.kvm.Gui.setStatus(log, "[" + this.getClass().getName() + "] logging out");
	this.logout();
      } catch (java.net.SocketException e) {
	log.info("Exception while logging out: " + e.toString());
	log.trace("exception trace:",e);
      }
    }
  }
}
