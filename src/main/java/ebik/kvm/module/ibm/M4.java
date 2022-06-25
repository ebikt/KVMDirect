package ebik.kvm.module.ibm;

import ebik.http.*;

public class M4 extends ebik.kvm.module.JnlpKVM {

  protected static String greetPage =
      "<!--  -->\n" +
      "<html>\n" +
      "<head>\n" +
      "\n" +
      "<title></title>\n" +
      "\n" +
      "<noscript>\n" +
      "<meta http-equiv=\"refresh\" content=\"0;url=/designs/imm/noscript/noscript_en.php\" />\n" +
      "</noscript>\n" +
      "\n" +
      "\n" +
      "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n" +
      "<META HTTP-EQUIV=\"Cache-Control\" CONTENT=\"no-cache\">\n" +
      "<META HTTP-EQUIV=\"Pragma\" CONTENT=\"no-cache\">\n" +
      "<META HTTP-EQUIV=\"Expires\" CONTENT=\"-1\">\n" +
      "\n" +
      "<style type=\"text/css\">\n" +
      "  @import \"/designs/ibmdojo/dojo/resources/dojo.css\";\n" +
      "  @import \"/designs/ibmdojo/dijit/themes/claro/claro.css\";\n" +
      "  @import \"/designs/imm/login.css\";\n" +
      "  @import \"/designs/ibmdojo/ibm/stg/InlineMessage.css\";\n" +
      " input[type=text]::-ms-clear { display: none; }\n" +
      "\n" +
      "</style>\n" +
      "<link rel=\"stylesheet\" type=\"text/css\" href=\"dojoOverrides.css\" />\n";

  public boolean homePageMatches(Response resp) throws Exception {
    String loc = resp.getHeader("Location");
    if (loc != null) {
      if (resp.code() == 301)
	if (loc.contains("/imm/"))
	  return true;
      return false;
    }
    if (resp.code() != 200) return false;
    return resp.toString().replace("\r\n","\n").startsWith(greetPage);
  }

  protected String user = null;

  public void login() throws Exception {
    user = params.get("user");
    String pass = params.get("password");
    Request req = session.request("/data/login");
    req.formData()
      .set("user", user)
      .set("password", pass)
      .set("SessionTimeout", "1200");
    org.json.JSONObject resp = req.getResponse(200).json();
    if (!"ok".equals(resp.get("status")) || !"0".equals(resp.get("authResult")))
      throw new Exception("Login failed, response not matched " + resp);
    if (session.getCookie("_appwebSessionId_") == null)
      throw new Exception("Login failed: cookie not set, response was:" + resp);
  }

  public String getJnlpXML() throws Exception {
    String jnlpDoc = session.request("/designs/imm/viewer("
      + session.getPrefix().split("/")[2]
      + "@443@0@" + Math.round(new java.util.Date().getTime()/1000)
      + "@0@0@1@jnlp@" + user + "@0@0@0@0@1).jnlp"
    ).getResponse(200).toString();
    int xmlStart = jnlpDoc.indexOf('<');
    return jnlpDoc.substring(xmlStart);
  }

  public void logout() throws Exception {
    session.request("/data/logout").setTimeout(3000).getResponse();
  }
}
