package ebik.kvm.module.sm;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import ebik.http.*;

public class Aten extends ebik.kvm.module.JnlpKVM {
  private Request loginRequest = null;
  private boolean b64encode = false;

  public boolean homePageMatches(Response resp) throws Exception {
    String loc = resp.getHeader("Location");
    if (loc != null) {
      if (resp.code() == 301)
	if (loc.startsWith("https://"))
	  if (resp.toString().length() == 0)
	    return true;
      return false;
    }
    if (resp.code() != 200) return false; //FIXME check
    String firstPage = resp.toString();
    if (!firstPage.contains("<META NAME=\"ATEN International Co Ltd.\" CONTENT=\"(c) ATEN International Co Ltd. 2010\">"))
      return false;
    log.trace("Aten name matched.");
    Element form = Jsoup.parse(firstPage).select("form").first();
    String url = form.attr("action");
    loginRequest = session.request(url);
    b64encode = false;
    for (Element inp: form.select("input")) {
      String name = inp.attr("name");
      String value = inp.attr("value");
      if (name.length()>0)
	loginRequest.formData().set(name, value);
      if ("hidden".equals(inp.attr("type")) && "check".equals(name)) {
	b64encode = true; 
      }
    }
    return (
      (loginRequest.formData().get("name") != null) &&
      (loginRequest.formData().get("pwd") != null)
    );
  }

  public void login() throws Exception {
    String user = params.get("user");
    String password = params.get("password");
    if (b64encode) {
      java.util.Base64.Encoder e = java.util.Base64.getEncoder();
      user = e.encodeToString(user.getBytes());
      password = e.encodeToString(password.getBytes());
    }
    loginRequest.formData().set("name", user);
    loginRequest.formData().set("pwd", password);
    String resp = loginRequest.getResponse(200).toString();
    if (!resp.contains("url_redirect.cgi?url_name=mainmenu")){
      log.error(resp);
      throw new Exception("Login failed.");
    }
  }

  public String getJnlpXML() throws Exception {
    return session
      .request("/cgi/url_redirect.cgi?url_name=ikvm&url_type=jwsk")
      .getResponse(200).toString();
  }

  public void logout() throws Exception {
    session.request("/cgi/logout.cgi").setTimeout(3000).getResponse();
  }
}
