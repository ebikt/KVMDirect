package ebik.kvm.module.dell;
import ebik.http.*;

public class IDRAC8 extends ebik.kvm.module.JnlpKVM {

  protected static String greetPage =
      "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n" +
      "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
      "<head>\n" +
      "<meta content=\"text/html; charset=UTF-8\" http-equiv=\"Content-Type\" />\n" +
      "<meta http-equiv=\"Content-Script-Type\" content=\"text/javascript\" />\n" +
      "<meta http-equiv=\"Content-Style-Type\" content=\"text/css\" />\n" +
      "<meta http-equiv=\"Cache-control\" content=\"private\">\n" +
      "<title></title>\n" +
      "<script src=\"/js/prototype.js\" type=\"text/javascript\"></script>\n" +
      "<script src=\"/js/Clarity.js\" type=\"text/javascript\"></script>\n" +
      "<script language=\"javascript\" type=\"text/javascript\">\n" +
      "var isSCenabled = 0;\n" +
      "var isSSOenabled = 0;\n" +
      "var isADEnabled = 1;\n" +
      "function getAimIntProp(){\n" +
      "var sessionURI = \"/session?aimGetIntProp=scl_int_enabled,pam_int_ldap_enable_mode\";\n" +
      "var config = new Ajax.Request(sessionURI, { method: 'get', onSuccess: resAimGetIntProp, onFailure: configError});\n" +
      "}\n" +
      "function resAimGetIntProp(e)\n" +
      "{\n" +
      "if (e.responseText == null || e.responseText.length == 0) return;\n" +
      "try {\n" +
      "var tempObj = e.responseText.evalJSON();\n" +
      "var _jsonData = tempObj.aimGetIntProp;\n" +
      "isSCenabled = _jsonData['scl_int_enabled'];\n" +
      "isADEnabled = _jsonData['pam_int_ldap_enable_mode'];\n" +
      "getAimBoolProp();\n" +
      "}catch(e){\n" +
      "}\n" +
      "}\n" +
      "function getAimBoolProp(){\n" +
      "var sessionURI = \"/session?aimGetBoolProp=pam_bool_sso_enabled\";\n" +
      "var config = new Ajax.Request(sessionURI, { method: 'get', onSuccess: resAimGetBoolProp, onFailure: configError});\n" +
      "}\n" +
      "function resAimGetBoolProp(e)\n" +
      "{\n" +
      "if (e.responseText == null || e.responseText.length == 0) return;\n" +
      "try {\n" +
      "var tempObj = e.responseText.evalJSON();\n" +
      "var _jsonData = tempObj.aimGetBoolProp;\n" +
      "isSSOenabled = _jsonData['pam_bool_sso_enabled'];\n" +
      "redirect();\n" +
      "}catch(e){\n" +
      "}\n" +
      "}\n";

  protected static String tokenStart = "<forwardUrl>index.html?";
  protected static String tokenEnd   = "</forwardUrl>";

  public boolean homePageMatches(Response resp) throws Exception {
    String loc = resp.getHeader("Location");
    if (loc != null) {
      if ("application/x-gzip".equals(resp.getHeader("Content-Type")))
	return true;
      return false;
    }
    if (resp.code() != 200) return false;
    String txt = resp.toString()
      .replaceAll("[ \r\t][ \r\t]*", " ")
      .replaceAll(" *\n *","\n")
      .replaceAll("\n\n\n*","\n");
    return txt.startsWith(greetPage);
  }

  protected String user;
  protected String token;

  public void login() throws Exception {
    user = params.get("user");
    String pass = params.get("password");
    Request req = session.request("/data/login");
    req.formData()
      .set("user", user)
      .set("password", pass);
    String loginResp = req.getResponse(200).toString();
    if (!loginResp.contains("<status>ok</status>") || !loginResp.contains("<authResult>0</authResult>"))
      throw new Exception("Login failed, response not matched " + loginResp);
    int start = loginResp.indexOf(tokenStart);
    int end = loginResp.indexOf(tokenEnd);
    if (start < 0 || end < 0)
      throw new Exception("Cannot parse tokens from: " + loginResp);
    log.trace("Login response: "+loginResp);
    start = start + tokenStart.length();
    String resp = loginResp.substring(start, end);
    log.trace("Tokens: "+resp);
    String[] tokens = resp.split(",");
    if (tokens.length != 2 || !tokens[0].contains("=") )
      throw new Exception("Cannot parse tokens from: " + loginResp);
    String[] h = tokens[1].split("=",2);
    session.addHeader(h[0], h[1]);
    token = tokens[0];
  }

  public static void encodeInto(StringBuilder b, String s) {
    java.nio.ByteBuffer bb = org.apache.http.Consts.UTF_8.encode(s);
    while (bb.hasRemaining()) {
      char c = (char) (bb.get() & 0xff);
      if (Character.isLetterOrDigit(c))
	b.append(c);
      else switch (c) {
	case '.': case '_': case '-':
	  b.append(c); break;
	default:
	  b.append('%');
	  b.append(String.format("%02x", (int)c));
      }
    }
  }

  public String getJnlpXML() throws Exception {
    org.json.JSONObject data = session
      .request("/session?aimGetProp=hostname,sysDesc")
      .getResponse(200).json().getJSONObject("aimGetProp");
    StringBuilder buf = new StringBuilder();
    buf.append("/viewer.jnlp(");
    buf.append(session.getPrefix().split("/")[2]);
    buf.append("@0@");
    encodeInto(buf, data.getString("hostname"));
    encodeInto(buf, " ");
    encodeInto(buf, data.getString("sysDesc"));
    encodeInto(buf, " User: ");
    encodeInto(buf, user);
    buf.append("@");
    encodeInto(buf, ""+Math.round(new java.util.Date().getTime()/1000));
    buf.append("@");
    buf.append(token);
    buf.append(")");
    return session.request(buf.toString()).getResponse(200).toString();
  }

  public void logout() throws Exception {
    session.request("/data/logout").setTimeout(3000).getResponse();
  }
}
