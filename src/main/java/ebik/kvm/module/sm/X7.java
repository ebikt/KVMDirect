package ebik.kvm.module.sm;
import ebik.http.*;

public class X7 extends ebik.kvm.module.AppletKVM {

  protected static String greetPage =
      "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n" +
      "<html>\n" +
      "  <head>\n" +
      "    <meta http-equiv=\"Content-Type\" content=\"text/html; charset=ISO-8859-1\">\n" +
      "    <title>Authentication</title>\n" +
      "\t<link href=\"/style.asp\" type=\"text/css\" rel=\"STYLESHEET\">\n" +
      "    \n" +
      "    <script type=\"text/javascript\">\n" +
      "      <!--\n" +
      "      if (top.frames.length!=0)\n" +
      "      top.location=self.document.location;\n" +
      "      // -->\n" +
      "    </script>\n" +
      "    <link rel=\"SHORTCUT ICON\" href=\"favicon.ico\">\n" +
      "    \n" +
      "  </head>\n" +
      "  <body class=\"auth\">\n" +
      "\t<form action=\"auth.asp\" method=\"POST\">\n" +
      "\t<input type=\"hidden\" name=\"nickname\" value=\"\">\n" +
      "\t\n" +
      "\n" +
      "\t\n" +
      "\n" +
      "      <table align=\"center\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">\n" +
      "\n" +
      "\t<tr><td colspan=\"2\">&nbsp;</td></tr>\n" +
      "\t<tr class=\"authLogo\">\n" +
      "\t  <td colspan=\"2\" align=\"center\">\n" +
      "\t    <img src=\"/logo.gif\" alt=\"LOGO\">\n" +
      "\t  </td>\n" +
      "\t</tr>\n" +
      "\t<tr><td colspan=\"2\" align=\"center\"><br><span class=\"rsp_message\">&nbsp;</span><!-- response_code_begin ERIC_RESPONSE_OK response_code_end response_msg_begin  response_msg_end  --><br></td></tr>\n" +
      "\t<tr>\n" +
      "\t  <td colspan=\"2\" align=\"center\" valign=\"top\">\n" +
      "\t    <table class=\"authInner\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\">\n" +
      "\t    <tr>\n" +
      "\t      <td><img src=\"corner_lt.gif\" width=\"45\" height=\"45\" alt=\"\"></td>\n" +
      "\t      <td class=\"authHeading\" align=\"center\">Authenticate with Login and Password!</td>\n" +
      "\t      <td><img src=\"corner_rt.gif\" width=\"45\" height=\"45\" alt=\"\"></td>\n" +
      "\t    </tr>            \n" +
      "\t    <tr>\n" +
      "\t      <td>&nbsp;</td>\n" +
      "\t      <td>\n" +
      "\t\t<table class=\"authInner\" border=\"0\" cellpadding=\"10\" cellspacing=\"0\">\n" +
      "\t\t  <tr>\n" +
      "\t\t    <td align=\"right\"><div class=\"bold\">Username</div></td>\n" +
      "\t\t    <td align=\"left\"><input type=\"text\" name=\"login\" value=\"\" maxlength=\"32\" size=\"32\"></td>\n" +
      "\t\t  </tr>\n" +
      "\t\t  <tr>\n" +
      "\t\t    <td align=\"right\"><div class=\"bold\">Password</div></td>\n" +
      "\t\t    <td align=\"left\"><input type=\"password\" name=\"password\" value=\"\" maxlength=\"32\" size=\"32\"></td>\n" +
      "\t\t  </tr>\n" +
      "\t\t</table>\n" +
      "\t      </td>\n" +
      "\t      <td>&nbsp</td>\n" +
      "\t    </tr>\n" +
      "\t    <tr>\n" +
      "\t      <td><img src=\"corner_lb.gif\" width=\"45\" height=\"45\" alt=\"\"></td>\n" +
      "\t      <td align=\"center\"><input type=\"image\" src=\"button_login.en.gif\" name=\"action_login\" value=\"Login\" align=\"middle\" style=\"vertical-align:middle\">\n" +
      "    \n" +
      "</td>\n" +
      "\t      <td><img src=\"corner_rb.gif\" width=\"45\" height=\"45\" alt=\"\"></td>\n" +
      "\t    </tr>\n" +
      "\t</table>\n" +
      "      </table>\n" +
      "    </form>\n" +
      "  </body>\n" +
      "</html>\n" ;

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

  private String baseName(String url) {
    int baseStart = 0;
    while (true) {
      int slash = url.indexOf('/',baseStart);
      if (slash < 0) break;
      baseStart = slash + 1;
    }
    return url.substring(baseStart);
  }

  public void login() throws Exception {
    String user = params.get("user");
    String pass = params.get("password");
    Request req = session.request("/auth.asp");
    req.formData()
      .add("nickname", "")
      .add("login",user)
      .add("password",pass)
      .add("action_login.x","0")
      .add("action_login.y","0")
    ;
    Response resp = req.getResponse();
    if (resp.code() != 302) {
      log.trace("Login failed, got " +resp.code()+":\n" + resp.toString());
      throw new Exception("Login failed, redir not matched: " + resp.code());
    }
    String redir = baseName(resp.getHeader("Location"));
    if (!"home.asp".equals(redir))
      throw new Exception("Login failed, redir not matched: " + redir);
  }

  public String getAppletHtml() throws Exception {
    return session.request("/title_app.asp").getResponse(200).toString();
  }

  public void logout() throws Exception {
    session.request("/logout").setTimeout(3000).getResponse();
  }
}
