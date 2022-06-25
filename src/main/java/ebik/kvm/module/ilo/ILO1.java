package ebik.kvm.module.ilo;
import ebik.http.*;

public class ILO1 extends ebik.kvm.module.AppletKVM {
  private String realm = null;

  public boolean homePageMatches(Response resp) throws Exception {
    if (resp.code() != 401) return false;
    String txt = resp.toString();
    if (!txt.contains("\r\n<title>BMC HTTP Server</title>\r\n")) return false;
    if (!txt.contains("lights-out100")) return false;
    String wa = resp.getHeader("WWW-Authenticate");
    log.trace("WWW-Authenticate: [" + String.join("|",wa) + "]");
    if (wa == null) return false;
    String[] parts = wa.split("\"");
    if (parts.length != 6) return false;
    if (!parts[0] .equals( "Digest realm=")) return false;
    if (!parts[2] .equals( ",  qop="))  return false;
    if (!parts[3] .equals( "auth"))     return false;
    if (!parts[4] .equals( ", nonce=")) return false;
    realm = parts[1];
    // Example: Authorization:"Digest username="admin", realm="10.33.224.183", nonce="2981cf27", uri="/index.html", response="a9547dd28f5c840292bf2807a34d7008", qop=auth, nc=0000000b, cnonce="ad2d6844e0a81d73""
    return true;
  }

  public void login() throws Exception {
    session.setAuthCredentials(
      params.get("user"), params.get("password"), realm
    );
  }

  public String getAppletHtml() throws Exception {
    return session.request("kvms.html").getResponse(200).toString();
  }

  public void logout() throws Exception {
    log.trace("Logging out");
    session.request("loggedout.htm").setTimeout(3000).getResponse();
  }
}
