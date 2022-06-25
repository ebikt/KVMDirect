package ebik.kvm.module.ibm;

import ebik.http.*;

public class M3 extends ebik.kvm.module.JnlpKVM {

  protected static String greetPage =
      "<!-- *************************************************************\n" +
      " *****************************************************************\n" +
      " ***                                                            **\n" +
      " ***    (C)Copyright 2007-2008, American Megatrends Inc.        **\n" +
      " ***                                                            **\n" +
      " ***                All Rights Reserved.                        **\n" +
      " ***                                                            **\n" +
      " ***        6145-F, Northbelt Parkway, Norcross,                **\n" +
      " ***                                                            **\n" +
      " ***        Georgia - 30071, USA. Phone-(770)-246-8600.         **\n" +
      " ***                                                            **\n" +
      " *****************************************************************\n" +
      " *****************************************************************\n" +
      " *****************************************************************\n" +
      " *\n" +
      " * Filename: home.html\n" +
      " *\n" +
      " * File description: Implementation for home page\n" +
      " *\n" +
      " * Author: Brandon <brandonburrell@ami.com>\n" +
      " *\t   Manish. T <manisht@ami.com>\n" +
      " *\t   Rufina Mercelene. S <rufinamercelenes@amiindia.co.in>\n" +
      " *\n" +
      " ************************************************************* -->\n";

  public boolean homePageMatches(Response resp) throws Exception {
    String loc = resp.getHeader("Location");
    if (loc != null) {
      return false;
    }
    if (resp.code() != 200) return false;
    return resp.toString().replace("\r\n","\n").startsWith(greetPage);
  }

  protected String sessionKey = null;

  public void login() throws Exception {
    String user = params.get("user");
    String pass = params.get("password");
    String resp = session
      .request("/session/create")
      .postData("application/x-www-form-urlencoded", user + "," + pass)
      .getResponse(200)
      .toString();
    if (!resp.startsWith("ok:"))
      throw new Exception("Login failed, response not matched\n[Response start]"
			  + resp.replace("\\r","[CR]") + "[Response end]");
    sessionKey = resp.substring(3);
  }

  public String getJnlpXML () throws Exception {
    return session
      .request("/kvm/kvm/jnlp?session_id=" + sessionKey)
      .getResponse(200).toString();
  }

  public void logout() throws Exception {
    session.request("/session/deactivate").setTimeout(3000).getResponse();
  }
}
