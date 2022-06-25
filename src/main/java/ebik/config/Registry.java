package ebik.config;
import java.util.Vector;
import java.util.Properties;
import java.util.Arrays;
import java.util.HashMap;

public class Registry {
  public static class InvalidSectionException extends IllegalStateException {
    private static final long serialVersionUID = -8425761693849428136L;
    protected String message;
    public InvalidSectionException(String msg) {
      message = msg;
    }
    public String getMessage() { return message; }
  }

  public static class EnabledSections {
    private HashMap<String,Integer> sectionPrio = new HashMap<String,Integer>();

    protected EnabledSections() {}
    public EnabledSections(EnabledSections e) {
      e.copyTo(sectionPrio);
    }

    public void enableSection(String section, int priority) throws InvalidSectionException {
      validateSectionPrefix(section);
      int sep = findSectionSeparator(section);
      if (sep >= 0) throw new InvalidSectionException("Abiguous section '"+section+"' contains real property name start.");
      sectionPrio.put(section, priority);
    }
    public void disableSection(String section) throws InvalidSectionException {
      if (sectionPrio.remove(section)==null)
	throw new InvalidSectionException("'"+section+"' was not enabled");
    }
    public void copyTo(java.util.Map<String, Integer> to) {
      for (String k: sectionPrio.keySet())
	to.put(k, sectionPrio.get(k));
    }
    public Vector<String> asPrefixesVector() {
      // too few entries for clever algorithm
      Vector<String> ret   = new Vector<String>();
      Vector<Integer> prios = new Vector<Integer>();
      for (String k: sectionPrio.keySet()) {
	int prio = sectionPrio.get(k);
	for (int i=0; i<ret.size(); i++) {
	  if (prio > prios.elementAt(i)) {
	    ret.add(i, k+".");
	    prios.add(i, prio);
	    break;
	  }
	}
	ret.add(k+".");
	prios.add(prio);
      }
      return ret;
    }
    public Integer priority(String section) {
      return sectionPrio.get(section);
    }
  }

  private static class NamedProps {
    public String name;
    public Properties props;
    public NamedProps(String _name, Properties _props) {
      name = _name;
      props = _props;
    }
  }
  private static Vector<NamedProps> properties    = new Vector<NamedProps>();
  private static Vector<String> sectionNames      = new Vector<String>();
  private static Vector<String> firstComponents   = new Vector<String>();
  private static EnabledSections globallyEnabled  = new EnabledSections();
  private static boolean initialised = false;
  public static void init(String[] _sectionNames, String[] _firstComponents) {
    if (initialised) throw new IllegalStateException("Already initialised");
    for (String s: _sectionNames)   sectionNames.add(s+".");
    for (String s: _firstComponents) firstComponents.add("."+s+".");
    initialised = true;
  }

  public static void enableSection(String section, int priority) throws InvalidSectionException {
    globallyEnabled.enableSection(section, priority);
  }
  public static void disableSection(String section) throws InvalidSectionException {
    globallyEnabled.disableSection(section);
  }


  protected static void validateSectionPrefix(String section) throws InvalidSectionException {
    if (!initialised) throw new IllegalStateException("Registry not initialised");
    boolean valid = false;
    section = section + ".";
    for (String prefix: sectionNames) {
      if (section.startsWith(prefix)) {
	valid = true; break;
      }
    }
    if (!valid) throw new InvalidSectionException("Invalid section '"+section+"'");
  }

  public static void dump() {
    System.out.println("Dumping:");
    for (NamedProps np: properties) {
      System.out.println(" " + np.name);
      for (String k: np.props.stringPropertyNames()) {
	int i = findSectionSeparator(k);
	System.out.println(" * " + np.name+": "+k.substring(0,i)+
	  ".<"+globallyEnabled.priority(k.substring(0,i))+">."+k.substring(i+1)+
	  "="+np.props.get(k));
      }
    }
  }

  protected static int findSectionSeparator(String fullKey) {
    int separator = fullKey.length();
    for (String first: firstComponents) {
      int i = fullKey.indexOf(first);
      if (i > 0 && i < separator) separator = i;
    }
    if (separator < fullKey.length()) return separator;
    else return -1;
  }

  public static int sectionSeparator(String fullKey) throws InvalidSectionException {
    validateSectionPrefix(fullKey);
    int sep = findSectionSeparator(fullKey);
    if (sep <= 0) throw new InvalidSectionException("Cannot find end of section name in '"+fullKey+"'");
    return sep;
  }

  public static String sectionOf(String fullKey) throws InvalidSectionException {
    return fullKey.substring(0, sectionSeparator(fullKey));
  }

  public static String nameOf(String fullKey) throws InvalidSectionException {
    return fullKey.substring(sectionSeparator(fullKey)+1);
  }

  public static void addProperties(Properties p, String name) {
    properties.add(new NamedProps(name, p));
  }

  public static void addProperties(int index, Properties p, String name) {
    properties.add(index, new NamedProps(name, p));
  }

  public static int indexOf(String name) {
    for (int i=0; i<properties.size(); i++)
      if (properties.elementAt(i).name.equals(name))
	return i;
    throw new IllegalStateException("No entry \"" + name + "\" found."); //FIXME
  }

  public static void addAfterProperties(String after, Properties p, String name) {
    addProperties(indexOf(after)+1, p, name);
  }

  public static void addBeforeProperties(String before, Properties p, String name) {
    addProperties(indexOf(before), p, name);
  }

  public static void removeProperties(int i) {
    properties.remove(i);
  }

  public static void removeProperties(String name) {
    properties.remove(indexOf(name));
  }

  public static void validate(Properties p, String defaultPrefix) throws InvalidSectionException {
    validateSectionPrefix(defaultPrefix);
    int dpl = defaultPrefix.length();
    defaultPrefix = defaultPrefix + ".";
    for (String s: p.stringPropertyNames()) {
      try {
	sectionSeparator(s);
      } catch (InvalidSectionException e) {
	if (sectionSeparator(defaultPrefix + s) != dpl)
	  throw e;	
	p.setProperty(defaultPrefix + s, (String)p.remove(s));		
      }
    }
  }

  private static class PrioValue {
    public int prio;
    public String value;
    PrioValue(int p, String s) {
      prio = p;
      value = s;
    }
  };

  private static HashMap<String, PrioValue> asMap() throws InvalidSectionException {
    HashMap<String, PrioValue> v = new HashMap<String, PrioValue>();
    for (NamedProps np: properties)
      for (String key: np.props.stringPropertyNames()) {
	Integer prio = globallyEnabled.priority(sectionOf(key));
	if (prio == null) continue;
	PrioValue o = v.get(key);
	if (o == null || o.prio < prio)
	  v.put(nameOf(key), new PrioValue(prio, np.props.getProperty(key)));
      }
    return v;
  }

  public interface Setter {
    public void set(String k, String v);
  }

  public static void copyByFirstComponent(java.util.Map<String,Setter> setterMap) throws InvalidSectionException {
    HashMap<String, PrioValue> m = asMap();
    for (String key: m.keySet()) {
      String[] compos = key.split("\\.", 2);
      if (compos.length != 2) continue;
      Setter s = setterMap.get(compos[0]);
      if (s!=null)
	s.set(compos[1], m.get(key).value);
    }
  }

  public static String get(String key, EnabledSections e) throws InvalidSectionException {
    if (findSectionSeparator("x."+key) != 1)
      throw new InvalidSectionException("Invalid property name '"+key+"', it does not start with allowed prefix.");
    for (String prefix: e.asPrefixesVector()) {
      for (NamedProps np: properties) {
	String value = np.props.getProperty(prefix + key);
	if (value != null) return value;
      }
    }
    return null;
  }

  public static String get(String key) throws InvalidSectionException {
    return get(key, globallyEnabled);
  }

  public static class Getter {
    private EnabledSections enabledSections = new EnabledSections(Registry.globallyEnabled);
    private Vector<String> sections = new Vector<String>();
    public Getter(String clsStripPrefix, Class<?> c) {
      Vector<String> prefixes = new Vector<String>();
      String name = c.getName();
      prefixes.add(name);
      clsStripPrefix += ".";
      if (name.startsWith(clsStripPrefix)) {
	name = name.substring(clsStripPrefix.length());
	if (!name.startsWith(".") && !name.endsWith(".") && !name.contains("..")) {
	  int l = name.length();
	  while (l > 0) {
	    try {
	      String s = "module." + name.substring(0, l);
	      enabledSections.enableSection(s, s.length());
	      sections.add(s);
	    } catch (Exception e) {
	      throw new IllegalStateException(e);
	    }
	    l = name.lastIndexOf('.', l-1);
	  }
	}
      }
    }
    public String get(String key) throws InvalidSectionException {
      if (!key.contains("."))
	key = "kvm."+key;
      return Registry.get(key, enabledSections);
    }
    public void globalize() {
      try {
	for (String s: sections)
	  globallyEnabled.enableSection(s, s.length());
      } catch (Exception e) {
	throw new IllegalStateException(e);
      }
      enabledSections = globallyEnabled;
    }
  }

  public static String get(String key, String defaultValue) {
    String val = get(key);
    if (val == null) return defaultValue;
    return val;
  }

  public static String[] getNamesOf(String prefix) {
    java.util.HashSet<String> ret = new java.util.HashSet<String>();
    if (prefix == null) prefix = "";
    else if (prefix.length()>0 && !prefix.endsWith(".")) prefix = prefix + ".";
    for (String key: asMap().keySet())
      if (key.startsWith(prefix)) {
	key = key.substring(prefix.length());
	int dot = key.indexOf('.');
	if (dot > 0) ret.add(key.substring(0, dot));
	else ret.add(key);
      }
    String[] retArr = ret.toArray(new String[ret.size()]);
    Arrays.sort(retArr);
    return retArr;
  }
}
