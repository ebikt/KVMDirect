package ebik.kvm.queries;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

public abstract class Engine {
  protected Log log = LogFactory.getLog(this.getClass());
  public void initialize(ebik.http.Session s) {}
  abstract public String execute(Query q) throws Exception;
  abstract public String getName();
}