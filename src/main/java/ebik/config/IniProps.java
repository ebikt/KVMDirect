package ebik.config;
import java.util.Properties;
import java.io.*;

/**
 * The {@code IniProps} class extends {@code java.util.Properties} class
 * by introducing ini-style sections. Section name is then prepended
 * before all property names in respective section. Note that sections
 * are meant here only as syntactic sugar to shorten propety names.
 */
public class IniProps extends Properties {
  private static final long serialVersionUID = 397752034991634990L;
  private static int READSIZE   = 1024;
  private static int MAXSECTLEN = 250;
  private static int LOOKBEHIND = 5;
  private boolean debug = "true".equals(Registry.get("kvm.cfg.debug"));

  protected static String pretty(String s) {
    return s
      .replace("\n", "\037[35m␊\037[0m")
      .replace("\r", "\037[35m␍\037[0m")
      .replace("\t", "\037[35m␉\037[0m");
  }
  protected static String pretty(char[] buf, int from) {
    return pretty(new String(buf).substring(from));
  }
  protected static String pretty(char[] buf, int from, int to) {
    return pretty(new String(buf).substring(from, to));
  }

  private class SectionReader extends Reader { // {{{
    public  String  section = null;
    private Reader  source = null;
    private char[]  buffer;
    private int     buflen;
    private int     bufpos;
    private boolean endOfSection = false;
    private boolean eof = false;
    private boolean debug = "true".equals(Registry.get("kvm.cfg.debug"));

    SectionReader(Reader src) { // {{{
      source  = src;
      section = "";
      buffer = new char[READSIZE+LOOKBEHIND];
      buflen = LOOKBEHIND;
      bufpos = LOOKBEHIND;
      for (int i=0; i<LOOKBEHIND; i++) buffer[i]='\n';
    } // }}}

    private void fillBuffer() throws IOException { // {{{
      if (debug) System.out.println("   FillBuffer() \037[31meof:"+eof+"\037[0m bufpos:"+bufpos+" buflen:"+buflen);
      if (debug) System.out.println("     <" + pretty(buffer, bufpos, buflen) + "|");

      if (eof) return;
      if (bufpos > LOOKBEHIND) {
	buflen = buflen - bufpos;
	for (int i=0; i<buflen; i++)
	  buffer[LOOKBEHIND + i] = buffer[bufpos + i];
	buflen += LOOKBEHIND;
	bufpos = LOOKBEHIND;
      }
      if (debug) System.out.println("       copyback eof:"+eof+" bufpos:"+bufpos+" buflen:"+buflen);
      int want = (buflen < LOOKBEHIND) ? READSIZE : READSIZE + LOOKBEHIND - buflen;
      if (want <= 0) {
	if (debug) System.out.println("      WANT == 0!?");
	return;
      }
      int ret = source.read(buffer, buflen, want);
      if (ret < 0)
	eof = true;
      else
	buflen += ret;
      if (debug) System.out.println("       read:"+ret+" eof:"+eof+" bufpos:" + bufpos + " buflen:"+buflen);
      if (debug) System.out.println("     >" + pretty(buffer, bufpos, buflen) + "|");
    } // }}}

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException { // {{{
      if (debug) System.out.println("  SectionReader read(cbuf, "+off+", "+len+") eos:"+endOfSection);
      if (endOfSection) return -1;

      if (debug) System.out.println("    A bufpos:" + bufpos + " buflen:"+buflen + " data:|" + pretty(buffer, bufpos, buflen)+"|");
      if (bufpos > LOOKBEHIND || buflen < READSIZE) fillBuffer();
      if (debug) System.out.println("    B bufpos:" + bufpos + " buflen:"+buflen + " data:|" + pretty(buffer, bufpos, buflen)+"|");
      if (buflen <= bufpos) {
	if (debug) System.out.println("  `-> EOF");
	if (!eof) throw new IllegalStateException("Internal error - short buffer");
	endOfSection = true;
	return -1;
      }
      if (endOfSection) throw new IllegalStateException("Internal error - endOfSection should be false");

      int pos = bufpos;
      int max = (buflen > len + pos) ? pos + len : buflen;
      for (; pos<max; pos++) {
	if (buffer[pos] == '[') {
	  if (buffer[pos-1] == '\r' && buffer[pos-2] == '\n' && buffer[pos-3] !='\\') {
	    endOfSection = true;
	    break;
	  }
	  if (buffer[pos-1] == '\n' && buffer[pos-2] == '\r' && buffer[pos-3] !='\\') {
	    endOfSection = true;
	    break;
	  }
	  if (buffer[pos-1] == '\n' && buffer[pos-2] !='\\') {
	    endOfSection = true;
	    break;
	  }
	}
	cbuf[off++] = buffer[pos];
      }
      if (pos == bufpos) {
	if (debug) System.out.println("  `-> Start section at first character.");
	if (!endOfSection) throw new IllegalStateException("Internal error - endOfSection should be true");
	return -1;
      }
      int ret = pos - bufpos;
      if (debug) System.out.println("  returning "+ret+" characters:\n >>>"
	+ pretty(buffer, bufpos, pos));
      bufpos = pos;
      return ret;
    } // }}}

    boolean nextSection() throws IOException { // {{{
      if (debug) System.out.println("  NextSection() bufpos:"+bufpos+" buflen:"+buflen);
      if (!endOfSection)
	throw new IllegalStateException("Internal error - nextSection() called while section not ended");
      while (!eof && (buflen < MAXSECTLEN + 2 || bufpos > LOOKBEHIND))
	fillBuffer();
      if (debug)
	System.out.println("    IniProps eof:" + eof + " buffer: |"
	  + pretty(buffer, bufpos) + "|");
      if (bufpos >= buflen) return false;
      if (debug) System.out.println("    Buffer["+bufpos+"]: |"
	  + pretty(buffer, bufpos) + "|");
      if (buffer[bufpos] != '[')
	throw new IllegalStateException("Internal error - nextSection() and next character is not '['");
      for (int i=bufpos+1;i<buflen;i++) {
	if (buffer[i] == '\n' || buffer[i] == '\r' || buffer[i] == '[')
	  throw new IOException("Illegal character in section title"); //FIXME where
	if (buffer[i] == ']') {
	  buffer[i] = '.';
	  section = new String(buffer, bufpos+1, i-bufpos);
	  if (debug) System.out.println("  `-> section:" + section);
	  bufpos = i+1;
	  endOfSection = false;
	  return true;
	}
      }
      throw new IOException("Section title too long"); //FIXME where
    } // }}}

    @Override
    public void close() throws IOException { // {{{
      source.close();
      eof=true;
      endOfSection=true;
    } // }}}
  } // }}}

  @Override
  public synchronized void load(Reader reader) throws IOException { // {{{
    SectionReader sr = new SectionReader(reader);
    while (true) {
      Properties props = new Properties();
      props.load(sr);
      for (String key: props.stringPropertyNames()) {
	setProperty(sr.section + key, props.getProperty(key));
      }
      if (!sr.nextSection()) break;
    }
  } // }}}

  // java.util.properties uses Latin1 encoding if provided InputStream anyways
  private class Latin1Reader extends Reader { // {{{
    private InputStream stream;
    private byte[] buffer;
    private int bufpos = 0;
    private int buflen = 0;

    Latin1Reader(InputStream s) { // {{{
      stream = s;
      buffer = new byte[8192];
      buflen = 0;
      bufpos = 0;
    } // }}}

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException { // {{{
      if (debug) System.out.println("    Latin1Reader read(cbuf ," + off + ", " + len +")");
      if (bufpos >= buflen) {
	bufpos = 0;
	buflen = stream.read(buffer, 0, 8192);
	if (debug) System.out.println("     Latin1Reader stream.read(8192) = " + buflen);
	if (buflen <= 0)
	  //Handles eof
	  return buflen;
      }
      int want = len;
      if (want > cbuf.length - off) want = cbuf.length - off;
      if (want > buflen - bufpos) want = buflen - bufpos;
      for (int i=0; i<want; i++) {
	// (char)(byte & 0xFF) is equivalent to calling a ISO8859-1 decoder.
	cbuf[off+i] = (char)(buffer[bufpos+i] & 0xFF);
      }
      if (debug) System.out.println("     Latin1Reader return = " + want);
      bufpos += want;
      return want;
    } // }}}

    @Override
    public void close() throws IOException{ // {{{
      stream.close();
    } // }}}
  } // }}}

  @Override
  public synchronized void load(InputStream stream) throws IOException { // {{{
    load(new Latin1Reader(stream));
  } // }}}

  /**
   * Read property {@code section + "." + key}, return {@code null} if it does not exist.
   */
  public String getSectionProperty(String section, String key) {
    return getProperty(section + "." + key);
  }

  /**
   * Read property {@code section + "." + key}, return {@code defaultValie} if it does not exist.
   */
  public String getSectionProperty(String section, String key, String defaultValue) {
    return getProperty(section + "." + key, defaultValue);
  }

  /**
   * Set of properties starting with {@code section + "."} with {@ section + "."} removed from
   * their names.
   */
  public java.util.Set<String> sectionPropertyNames(String section) {
    String prefix = section + ".";
    java.util.HashSet<String> s = new java.util.HashSet<String>();
    for (String key: stringPropertyNames()) {
      if (key.startsWith(prefix)) {
	s.add(key.substring(prefix.length()));
      }
    }
    return java.util.Collections.unmodifiableSet(s);
  }
}
