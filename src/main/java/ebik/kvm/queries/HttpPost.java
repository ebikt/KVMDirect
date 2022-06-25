package ebik.kvm.queries;
import ebik.http.*;

public class HttpPost extends Engine {
  private Session session;

  @Override
  public void initialize(Session s) {session = s;}
  
  @Override
  public String execute(Query q) throws Exception {
    // Warning no redirection support (yet)
    String url = q.expandCfg("url");
    String body = q.expandCfg("content");
    log.trace("Requesting "+url+" with content:\n" + body);
    Response r = session.absoluteRequest(url)
      .postData(q.expandCfg("content-type", "text/plain"), body)
      .getResponse();
    if (r.code() < Integer.valueOf(q.expandCfg("minstatus", "200")) ||
	r.code() > Integer.valueOf(q.expandCfg("maxstatus", "299"))) {
      log.warn("Query " + q.cfgPrefix + " " + url + " returned status " + r.code());
      return null;
    }
    return r.toString();
  }

  @Override
  public String getName() {
    return "post";
  }

}
