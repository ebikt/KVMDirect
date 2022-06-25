package ebik.kvm.queries;
import java.util.ServiceLoader;

import ebik.config.Registry;
import ebik.kvm.Gui;

public class Query {
  protected static org.apache.commons.logging.Log log = 
    org.apache.commons.logging.LogFactory.getLog(Query.class);

  final static String queriesPrefix = "kvm.queries.";
  protected String cfgPrefix;
  
  protected static java.util.HashMap<String, Engine> engines =
      new java.util.HashMap<String, Engine>();

  public static class BadConfig extends Exception {
    private static final long serialVersionUID = 287264499603559590L;
    protected String msg;
    public BadConfig(String message) {msg=message;}
    public String getMessage() {return msg;}
  }

  public Query (String cfgName) {
    cfgPrefix = queriesPrefix + cfgName + ".";
  }

  public String cfg(String key, String defaultValue) {
    String ret = Registry.get(cfgPrefix + key);
    if (ret!=null) return ret.trim();
    return defaultValue;
  }
  public String cfg(String key) throws BadConfig  {
    String ret = cfg(key, null);
    if (ret == null) throw new BadConfig("Missing configuration "+cfgPrefix+key);
    return ret;
  }
  
  public String expandCfg(String key, String defaultValue) throws BadConfig {
    String ret = cfg(key, null);
    if (ret==null) return defaultValue;
    ret = ret.trim();
    java.util.Vector<String> retv = new java.util.Vector<String>();
    int pos = 0;
    while (pos >= 0) {
      int start = ret.indexOf("{{", pos);
      if (start < 0) break;
      retv.add(ret.substring(pos, start));
      pos = start;
      int stop = ret.indexOf("}}", start);
      if (stop < 0) break;
      String code = ret.substring(start+2, stop).trim();
      switch (code) {
      	case "NL":   retv.add("\n");   break;
      	case "CR":   retv.add("\r");   break;
      	case "CRNL": retv.add("\r\n"); break;
      	case "HT":   retv.add("\t");   break;
      	case "(":    retv.add("{");    break;
      	case ")":    retv.add("}");    break;
      	default:
      	  String repl = null;
      	  try {
      	    repl = Registry.get(code);
      	  } catch (Exception e) {
      	    throw new BadConfig("Cannot expand " + key + ", bad configuration reference " + code);
      	  }
      	  if (repl == null)
      	    throw new BadConfig("Cannot expand " + key + ", missing configuration " + code);
      	  retv.add(repl);
      }
      pos = stop+2;
    }
    retv.add(ret.substring(pos));
    
    return String.join("",  retv);
  }
  
  public String expandCfg(String key) throws BadConfig  {
    String ret = expandCfg(key, null);
    if (ret == null) throw new BadConfig("Missing configuration "+cfgPrefix+key);
    return ret;
  }
  
  public void badCfg(String key, String message) throws BadConfig {
    throw new BadConfig("Bad configuration "+cfgPrefix+key+": "+message);
  }

  public static void compute(int lowprio, int highprio) throws BadConfig {
    Gui.setStatus("Executing configuration queries.");
    java.util.Properties results = new java.util.Properties();
    Registry.addProperties(results, "queries results");
    Registry.enableSection("results.high", highprio);
    Registry.enableSection("results.low", lowprio);
    for (String queryName: Registry.getNamesOf(queriesPrefix)) {
      Query q = new Query(queryName);
      String qt = q.cfg("type");
      Engine e = engines.get(qt);
      if (e == null) q.badCfg("type", "Unknown query type "+qt);
      Gui.setStatus(log, "Executing query " + queryName + " (type: " + qt + ")");
      String resultName = q.cfg("result").trim();
      String destName;
      if (resultName.charAt(0) == '!') {
	resultName = resultName.substring(1);
	destName="results.high." + resultName;
      } else
	destName="results.low." + resultName;
      try {
	if (!resultName.equals(Registry.nameOf(destName)))
	  throw new Registry.InvalidSectionException(resultName);
      } catch (Registry.InvalidSectionException exc) {
	q.badCfg("result", "Invalid result name " + resultName);
      }
      String result = null;
      try {
	result = e.execute(q);
	if (result == null)
	  log.debug("Not setting " + destName + " as " + q.cfgPrefix + " returned null");
      } catch (Exception exc) {
	log.error("Failed to evaluate query " + q.cfgPrefix + ": " + exc);
	log.trace(exc);
      }
      if (result != null) {
	log.debug("Setting " + destName + " with result of " + q.cfgPrefix );
	log.trace("Value is: " + result);
	results.setProperty(destName, result);
      }
    }
    Gui.setStatus("Querying done.");
  }

  public static void registerEngines(ebik.http.Session session) {
    log.trace("about to load query engines");
    Gui.setStatus("Registering query engines.");
    for (Engine eng: new ebik.util.ChainedIterator<Engine>()
	  .add(ServiceLoader.load(Engine.class).iterator())
	  .add(new ebik.util.DevelServiceLoader<Engine>(Engine.class))
	) {
      log.trace("Initializing engine "+eng.getName());
      try {
	eng.initialize(session);
      } catch(Exception e) {
	log.fatal("Cannot initialize "+eng.getName() +": "+e.toString());
	continue;
      }
      engines.put(eng.getName(), eng);
      log.debug("Registered query engine:"+eng.getName());
    }
    log.trace("query engines registed");
  }
}
