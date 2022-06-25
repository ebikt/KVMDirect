package ebik.http;

import java.util.Vector;
import java.util.Iterator;
import org.apache.http.NameValuePair;


public class OrderedDict implements Iterable<OrderedDict.Pair> {
  public class Pair implements org.apache.http.NameValuePair {
    public String k;
    public String v;
    public Pair(String _k, String _v) {
      k = _k;
      v = _v;
    }

    public String getName()  { return k; }
    public String getValue() { return v; }
  }
  private Vector<Pair> content = new Vector<Pair>();

  public OrderedDict() {}
  public OrderedDict(OrderedDict copy) {
    for (Pair p: copy)
      add(p);
  }
  public OrderedDict(NameValuePair[] copy) {
    for (NameValuePair h: copy)
      add(h);
  }

  public OrderedDict remove(String key) {
    Vector<Pair> v = new Vector<Pair>();
    for (Pair p: content)
      if (!p.k.equals(key))
	v.add(p);
    content = v;
    return this;
  }

  public OrderedDict add(String key, String value) {
    content.add(new Pair(key, value));
    return this;
  }

  public OrderedDict add(Pair p) {
    content.add(new Pair(p.k, p.v));
    return this;
  }

  public OrderedDict add(NameValuePair p) {
    content.add(new Pair(p.getName(), p.getValue()));
    return this;
  }

  public OrderedDict set(String key, String value) {
    for (Pair p: content) {
      if (p.k.equals(key)) {
	p.v = value;
	return this;
      }
    }
    return add(key, value);
  }

  public String get(String key) {
    for (Pair p: content)
      if (p.k.equals(key))
	return p.v;
    return null;
  }

  public Vector<String> getAll(String key) {
    Vector<String> ret = new Vector<String>();
    for (Pair p: content)
      if (p.k.equals(key))
	ret.add(p.v);
    return ret;
  }

  public Iterator<Pair> iterator() {
    return content.iterator();
  }
}
