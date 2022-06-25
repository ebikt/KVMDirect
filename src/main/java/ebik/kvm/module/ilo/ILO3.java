package ebik.kvm.module.ilo;

import ebik.kvm.module.KVM;
import ebik.kvm.module.Gui;
import ebik.http.*;

import ebik.javaweb.AppletWrapper;
import java.awt.Component;
import javax.swing.JFrame;

import ebik.javaweb.AppletDesc;
import org.jdom2.Element;

public class ILO3 extends KVM {
  private JFrame main = null;

  public boolean homePageMatches(Response resp) throws Exception {
    if (resp.code() == 303) {
      return resp.getHeader("Location").startsWith("https://");
    }
    if (resp.code() != 200) return false;
    String txt = resp.toString();
    if (txt.contains("<title>Integrated Lights-Out 3</title>")) {
      log.error("Old firmware iLO3 server, please upgrade iLO3 firmware on server!");
      return false;
    }
    return txt.contains(
      "<meta name=\"copyright\" content=\"© Copyright 2006-2016 Hewlett Packard Enterprise Development LP\" />\n\t<title>iLO 3</title>"
    );
  }

  public void login() throws Exception {
    log.trace("Logging in");
    Request r = session.request("json/login_session");
    r.jsonData()
      .put("method", "login")
      .put("user_login", params.get("user"))
      .put("password", params.get("password"))
    ;
    session.setCookie("sessionKey", r.getResponse().json().getString("session_key"));
    if (session.getCookie("sessionKey") == null) throw new Exception("Login to iLO3 failed, no sessionKey found.");
  }

  public void logout() throws Exception {
    log.trace("Logging out");
    Request r = session.request("json/login_session");
    r.jsonData().put("method", "logout").put("session_key", session.getCookie("sessionKey"));
    r.setTimeout(3000).getResponse();
  }

  private String getAppletJNLP() throws Exception {
    String tm = String.valueOf(System.currentTimeMillis() / 1000L);
    log.trace("Downloading jnlp template");
    String resp = session.request("html/jnlp_template.html?_=" + tm).getResponse().toString();

    //Unwrap jnlp template from CDATA by xml parser
    resp = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + resp;
    Element root = AppletDesc.rootFromXml(resp);
    if (root.getName() != "script")
      throw new Exception("Expected root element of template to be <script>");
    if (! root.getAttribute("id").getValue().equals("jnlpTemplate"))
      throw new Exception("Expected root element id of template to be 'jnlpTemplate'");
    resp = root.getValue().trim();

    //Replace templated strings
    resp = resp.replace("<%= this.baseUrl %>", session.getPrefix().replace("\"", "&quot;").replace("<","&lt;").replace(">","&gt;").replace("&","&amp;"));
    resp = resp.replace("<%= this.sessionKey %>", session.getCookie("sessionKey"));
    resp = resp.replace("<%= this.langId %>", "en");

    return resp;
  }

  private void startApplet(Gui gui, AppletDesc ad) throws Exception {
    AppletWrapper apl = ad.loadApplet();
    log.trace("Starting applet.");
    gui.addHiddenComponent((Component) apl.getApplet());
    apl.init();
    try {
      main = (JFrame) ad.getAppletProperty("dispFrame"); //intgapl specific
      gui.addToplevelFrame(main);
    } catch (Exception close_exc) {
      log.warn("Error when linking close event", close_exc);
    }
    apl.start();
  }

  public void closeMain() {
    if (main != null) main.dispose();
  }

  public void run(Gui gui) throws Exception {
    startApplet(gui, AppletDesc.fromJNLP(getAppletJNLP()));
  }
}
