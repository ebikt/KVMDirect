package ebik.kvm.module.dell;

import ebik.http.*;

public class IDRAC9 extends ebik.kvm.module.JnlpKVM {

  protected static String greetPage =
      "<!DOCTYPE html>\n" +
      "<html ng-app='loginapp' ng-controller=\"loginController\" ng-init=\"onInit(0)\">\n" +
      "<head>\n" +
      "<title ng-bind=\"settings.title\"></title>\n" +
      "<link rel=\"icon\" href=\"images/favicon.ico\">\n" +
      "<meta http-equiv=\"CACHE-CONTROL\" content=\"NO-CACHE\">\n" +
      "<meta charset=\"utf-8\">\n" +
      "<meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge,chrome=1\" />\n" +
      "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1, user-scalable=no\" />\n" +
      "<meta name=\"apple-mobile-web-app-capable\" content=\"yes\" />\n" +
      "<meta name=\"mobile-web-app-capable\" content=\"yes\" />\n" +
      "<link rel=\"stylesheet\" href=\"css/bootstrap/bootstrap.css\">\n" +
      "<link rel=\"stylesheet\" href=\"css/custom.css\">\n" +
      "<link rel=\"stylesheet\" href=\"css/personality.css\">\n" +
      "<link rel=\"stylesheet\" href=\"images/clarityicons/style.css\">\n" +
      "</head>\n" +
      "<body>\n" +
      "<!-- Spinner -->\n" +
      "<!-- <div ng-show=\"pendReqCount > 0\" style=\"left: 48%;top: 48%;position: absolute;z-index: 1;\">\n" +
      "\t<i ng-if=\"!settings.show\" class=\"cui-spinner cui-spinner-color-black\" style=\"height: 60px; width: 60px;\"></i>\n" +
      "\t<i ng-if=\"settings.show\" class=\"cui-spinner cui-spinner-color-white\" style=\"height: 60px; width: 60px;\"></i>\n" +
      "</div> -->\n" +
      "<div ng-show=\"pendReqCount > 0\" style=\"position: fixed;width: 100%;height: 100%;background: rgba(0,0,0,0.5);z-index: 9000;\">\n" +
      "\t<div style=\"left: 48%;top: 48%;position: absolute;z-index: 1;\">\n" +
      "\t    <i class=\"cui-spinner cui-spinner-color-white\" style=\"height: 60px; width: 60px;\"></i>\n" +
      "\t</div>\n" +
      "</div>\n" +
      "<div ng-show=\"settings.show\">\n" +
      "<idrac-start-screen config=\"settings\" on-button-click=\"onBtnAction(action)\" on-text-change=\"onChange(map)\">\n" +
      "</idrac-start-screen>\n" +
      "</div>\n" +
      "<div id=\"activeX\" style=\"width:0;height:0;display:none;\"></div>\n" +
      "<script src='js/angular/angular.min.js' type=\"text/javascript\"></script>\n" +
      "<script src='js/angular/angular-translate.min.js' type=\"text/javascript\"></script>\n" +
      "<script src='js/angular/angular-translate-loader-static-files.min.js' type=\"text/javascript\"></script>   \n" +
      "<script src='js/lib/xml2json.min.js' type=\"text/javascript\"></script>    \n" +
      "<!-- Application scripts -->\n" +
      "<script src=\"js/common/constants.js\" type=\"text/javascript\"></script>\n" +
      "<script src=\"js/common/validator.js\" type=\"text/javascript\"></script>\n" +
      "<script src='js/services/resturi.js' type=\"text/javascript\"></script>\n" +
      "<script src='js/bootstrap/ui-bootstrap-tpls-0.13.3.min.js' type=\"text/javascript\"></script>\n" +
      "<script src='js/loginapp.js' type=\"text/javascript\"></script>\n" +
      "<script src='js/controllers/logincontroller.js' type=\"text/javascript\"></script>\n" +
      "</body>\n" +
      "</html>\n";


  protected static String tokenStart = "<forwardUrl>index.html?";
  protected static String tokenEnd   = "</forwardUrl>";

  public boolean homePageMatches(Response resp) throws Exception {
    String loc = resp.getHeader("Location");
    if (loc != null) {
      if (resp.code() == 302) {
	String srv = resp.getHeader("Server");
	if (srv != null)
	  if (srv.startsWith("Apache"))
	    return true;
      }
      return false;
    }
    if (resp.code() != 200) return false;
    return resp.toString().equals(greetPage);
  }

  public void login() throws Exception {
    String user = params.get("user");
    String pass = params.get("password");
    Response login = session.request("/sysmgmt/2015/bmc/session")
      .setHeader("user", user)
      .setHeader("password", pass)
      .postData("application/x-www-form-urlencoded", "")
      .getResponse();
    if (login.code() != 201)
      throw new Exception("Login failed, expected code 201, got "
	+ login.code() +" response:" + login.toString());
    org.json.JSONObject loginData = login.json();
    if (loginData.getInt("authResult")!=0)
      throw new Exception("Login failed, response not matched: " + login.toString());

    String token = session.getCookie("-http-session-");
    if (token == null || token.trim().length() == 0)
      throw new Exception("Login failed, cookies do not match.");

    String xsrf = login.getHeader("XSRF-TOKEN");
    if (xsrf == null || xsrf.trim().length() == 0)
      throw new Exception("Login failed, response headers do not match.");
    session.setHeader("XSRF-TOKEN", xsrf);
  }


  public String getJnlpXML() throws Exception {
    return session
      .request("/sysmgmt/2015/server/vconsole?type=Java")
      .getResponse(200).toString();
  }

  public void logout() throws Exception {
    //NO-OP?
  }
}
