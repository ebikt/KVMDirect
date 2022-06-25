package ebik.kvm.module.ilo;
import ebik.http.*;

import java.util.HashMap;
import java.util.Vector;
import java.util.Base64;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

public class ILO2 extends ebik.kvm.module.AppletKVM {
  private HashMap<String,String> attrs = null;

  private Element parseScripts(String page) {
    Element ret = null;
    for(Element script: Jsoup.parse(page).select("script")) {
      ret = script;
      for (String line: script.data().split("\n")) {
	String[] kv = line.split("=",2);
	if (kv.length < 2) continue;
	String key = kv[0].trim();
	if (key.startsWith("var ")) key = key.substring(4).trim();
	if (!javax.lang.model.SourceVersion.isIdentifier(key)) continue;
	String value = kv[1].trim();
	if (value.startsWith("\"")) {
	  if (!value.endsWith("\";")) continue;
	  value = value.substring(1,value.length() - 2);
	} else if (value.startsWith("'")) {
	  if (!value.endsWith("';")) continue;
	  value = value.substring(1,value.length() - 2);
	} else {
	  if (!value.endsWith(";")) continue;
	  value = value.substring(0,value.length() - 1).trim();
	  try {
	    Float.valueOf(value);
	  } catch (Exception e) {
	    continue;
	  }
	}
	
	attrs.put(key, value);
      }
    }
    return ret;
  }

  public boolean homePageMatches(Response resp) throws Exception {
    if (resp.code() != 200) return false;
    String txt = resp.toString();
    if (!txt.contains("\r\n<title>\r\nHP Integrated Lights-Out 2\r\n</title>\r\n")) return false;
    attrs = new HashMap<String, String>();
    parseScripts(txt);
    return ":ilo:".equals(attrs.get("platform")) && attrs.get("ipAddress") != null;
  }

  private String appletScript = null;

  public void login() throws Exception {
    //FIXME this is login method is unreliable!?
    //Ilo asks for sync
    String resp="";

    log.trace("Logging in");
    session.setPrefix("https://" + session.getPrefix().split("/")[2]);
    // No LDAP support
    Base64.Encoder b64 = Base64.getEncoder();
    String user = params.get("user");
    String pass = params.get("password");

    for (int i=0; i<5; i++) {
      resp = session.request("login.htm").getResponse(200).toString();
      parseScripts(resp);
      if (attrs.get("sessionkey") == null || attrs.get("sessionkey").length() != 40) {
	if ("NONEAVAILABLE".equals(attrs.get("sessionkey")))
	  log.error("Too much sessions, server cannot issue another session key. Please wait and try again later!.");
	else 
	  System.out.println(resp);
	throw new Exception("Failed to obtain session key.");
      }

      session.setCookie("hp-iLO-Login",
	attrs.get("sessionindex")
	+":"+ b64.encodeToString(user.getBytes())
	+":"+ b64.encodeToString(pass.getBytes())
	+":"+ attrs.get("sessionkey")
      );
      resp = session.request("drc2fram.htm?restart=0").getResponse(200).toString();
      if (resp.contains("<TITLE>Login Delay</TITLE>")) { // More information on some error
	parseScripts(resp);
	log.error("Login failed with readon: " + attrs.get("LoginDelayCause"));
	if (attrs.get("LoginDelayCode").contains("-1")) {
	  String delay = attrs.get("LoginDelayValue");
	  log.error("Asked for waiting for: " + delay + "ms");
	  if ("0".equals(delay)) {
	    log.error("Please check that cookie is sent unquoted");
	    break;
	  } else {
	    Thread.sleep(1 + Integer.parseInt(delay)/1000);
	  }
	} else break;
      } else break;
    }
    appletScript = parseScripts(resp).data();
    if (!user.equals(attrs.get("currentUser"))) throw new Exception("Login failed");
  }


  public void logout() throws Exception {
    log.trace("Logging out");
    session.request("logout.htm").setTimeout(3000).getResponse();
  }

  public static String escAttr(String str) {
    String ret = ebik.javaweb.AppletDesc.escAttr(str);
    if (ret.contains("\""))
      throw new IllegalStateException("escapeAttribute left unescaped quote");
    return ret;
  }

  public String getAppletHtml() throws Exception {
    Vector<String> appletInfo = new Vector<String>();
    String lineStart = "document.writeln(\"";
    String lineEnd = "\");";
    
    for (String line: appletScript.split("\n")) {
      line = line.trim();
      log.trace("Parsing line " + line);
      if (line.startsWith(lineStart) && line.endsWith(lineEnd))
	line = line.substring(lineStart.length(),line.length() - lineEnd.length());
      else
	continue;
      appletInfo.add(line);
    }
    String scriptHtml=String.join("\n",appletInfo);
    for (String key: attrs.keySet()) {
      scriptHtml = scriptHtml.replace("\"+" + key + "+\"", escAttr(attrs.get(key)));
    }
    scriptHtml = scriptHtml.replace("\\\"", "\"");
    if (scriptHtml.contains("\\")) {
      log.debug(scriptHtml);
      throw new Exception("Parsing failed, remaining backslash in output");
    }
    return scriptHtml;
  }

}
