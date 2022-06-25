package ebik.kvm.queries;

import java.util.regex.*;

public class RegexMatch extends Engine {

  @Override
  public String execute(Query q) throws Exception {
    String document = q.expandCfg("text");
    String regex = q.expandCfg("regex"); 
    log.trace("Selecting " + regex + " in:\n" + document);
    Pattern p = Pattern.compile(regex);
    Matcher m = p.matcher(document);
    if (!m.find()) {
      log.trace("regex did not match");
      return null;
    }
    return m.group(1);
  }

  @Override
  public String getName() {
    return "regex";
  }

}
