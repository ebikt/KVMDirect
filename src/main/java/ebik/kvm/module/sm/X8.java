package ebik.kvm.module.sm;

import ebik.http.*;
import ebik.javaweb.SimpleJNLP;

public class X8 extends ebik.kvm.module.KVM {

  protected static String greetPage =
      "<html>\r\n" +
      "<head>\r\n" +
      "<script type=\"text/javascript\" src=\"/lib/xmit.js\"></script>\r\n" +
      "<script language=\"javascript\">\r\n" +
      "window.location.href = \"/page/login.html\";\r\n" +
      "</script>\r\n" +
      "</head>\r\n" +
      "<body style='margin:0;padding:0' bgcolor=\"#E0E0EB\">\r\n" +
      "</body>       \r\n" +
      "</html>\r\n";
  protected static String loginStart =
      "//Dynamic Data Begin\n" +
      " WEBVAR_JSONVAR_WEB_SESSION = \n" +
      " { \n" +
      " WEBVAR_STRUCTNAME_WEB_SESSION : \n" +
      " [ \n" +
      " { 'SESSION_COOKIE' : '";
  protected static String loginEnd =
      "' },  {} ],  \n" +
      " HAPI_STATUS:0 }; \n" +
      "//Dynamic data end\n" +
      "\n";

  public boolean homePageMatches(Response resp) throws Exception {
    String loc = resp.getHeader("Location");
    if (loc != null) {
      if (resp.code() == 302)
	if ("GoAhead-Webs".equals(resp.getHeader("Server")))
	  return true;
      return false;
    }
    if (resp.code() != 200) return false;
    return resp.toString().equals(greetPage);
  }

  public void login() throws Exception {
    String user = params.get("user");
    String pass = params.get("password");
    Request req = session.request("/rpc/WEBSES/create.asp");
    req.formData()
      .add("WEBVAR_USERNAME",user)
      .add("WEBVAR_PASSWORD",pass);
    String resp = req.getResponse(200).toString();
    log.trace("Start matches: " + resp.startsWith(loginStart));
    log.trace("End matches: " + resp.endsWith(loginEnd));
    if (!(resp.startsWith(loginStart) && resp.endsWith(loginEnd)))
      throw new Exception("Login failed, page not matched\n[Response start]" + resp.replace("\\r","[CR]") + "[Response end]");
    String cookie = resp.substring(loginStart.length(), resp.length() - loginEnd.length());
    log.trace("Login cookie:"+cookie);
    session.setCookie("Username", user);
    session.setCookie("SessionCookie", cookie);
  }


  public void run(ebik.kvm.module.Gui gui) throws Exception {
    Request r = session.request("/Java/jviewer.jnlp");
    String jnlpDoc = r.getResponse(200).toString();
    log.trace(jnlpDoc);
    log.trace("About to parse");
    SimpleJNLP jnlp = SimpleJNLP.fromString(jnlpDoc);
    jnlp.debug();
    jnlp.download(downloader);
    int rv = jnlp.run();
    log.debug("Java interpreter returned with code: " + rv);
    gui.shutdown();
  }

  public void logout() throws Exception {
    session.request("/rpc/WEBSES/logout.asp").setTimeout(3000).getResponse();
  }
}
