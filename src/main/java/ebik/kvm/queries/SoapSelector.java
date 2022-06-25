package ebik.kvm.queries;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

public class SoapSelector extends Engine {

  @Override
  public String execute(Query q) throws Exception {
    String document = q.expandCfg("html");
    String selector = q.expandCfg("query");
    log.trace("Selecting " + selector + " in: " + document);
    Element el = Jsoup.parse(document).select(selector).first();
    if (el == null) return null;
    return el.html().trim();
  }

  @Override
  public String getName() {
    return "soap";
  }
}
