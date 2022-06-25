package ebik.kvm;

import java.util.HashMap;
import ebik.kvm.module.KVM;
import ebik.http.*;
import ebik.config.Registry;
import java.net.URL;

import java.security.Security;
import gist.SSLUtilities;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.ServiceLoader;

import java.util.Vector;
import org.apache.http.conn.ssl.*;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;

import ebik.config.IniProps;

public class KVMDirect {

  private static void loadIniFile(String path) throws IOException {
    IniProps props = new IniProps();
    props.load(new FileInputStream(path));
    Registry.validate(props, "default");
    Registry.addProperties(props, path);
  }

  private static void parseArgs(String[] args) throws IOException { // {{{
    Properties arguments = new Properties ();
    Registry.addProperties(arguments, "arguments");

    for (String arg: args) {
      String[] pair = arg.split("=", 2);
      String key, value;
      if (pair.length == 2) {
	key = pair[0];
	value = pair[1];
      } else {
	key = "host";
	value = arg;
      }
      if ("config".equals(key)) {
	try {
	  loadIniFile(value);
	} catch (IOException e) {
	  throw new IOException("Cannot load config from " + value + ": " + e);
	}
	continue;
      }
      if (!key.contains(".")) key = "arguments.main.kvm." + key;
      try {
	Registry.sectionSeparator("arguments.main."+key);
      } catch (Registry.InvalidSectionException e) {
	throw new IOException("Invalid argument '"+key+"'");
      }
      try {
	Registry.sectionSeparator(key);
      } catch (Registry.InvalidSectionException e) {
	key = "arguments.main."+key;
      }

      if (arguments.getProperty(key) != null) {
	System.err.println("Duplicate parameter " + key);
	System.exit(1);
      } else
	arguments.setProperty(key, value);
    }
  }  // }}}

  private static Log log = null;

  public static class HideInvalidContentLengthStrategy // {{{
    extends org.apache.http.impl.entity.LaxContentLengthStrategy
  {
    //FIXME service
    public long determineLength(org.apache.http.HttpMessage m) throws org.apache.http.HttpException {
      long ret = super.determineLength(m);
      log.trace("super.determineLength: " +ret);
      if (ret < 0) return ret;
      org.apache.http.Header h;
      h = m.getFirstHeader("Server");
      log.trace("determineLength Server: " +h);
      if (h == null) return ret;
      if (!"GoAhead-Webs".equals(h.getValue())) return ret;
      h = m.getFirstHeader("Content-type");
      log.trace("determineLength Content-type: " +h);
      if (h == null) return ret;
      if (!"application/x-java-jnlp-file".equals(h.getValue())) return ret;
      log.debug("Masking invalid Content-Length header ");
      return org.apache.http.entity.ContentLengthStrategy.IDENTITY;
    }
  } // }}}

  private static String[] split(final String s) {
      if (s == null || s.trim().length() == 0) {
	  return null;
      }
      return s.split(" *, *");
  }

  public static void main(String[] args) {
    Gui gui = new Gui();

    Registry.init(
      new String[]{"arguments", "host", "module", "default", "results"},
      new String[]{"kvm", "security", "system", "custom"}
    );
    Registry.enableSection("default",0);
    Registry.enableSection("arguments.main",10020);

    Gui.setStatus("Parsing arguments.");
    try {
      parseArgs(args);
    } catch (IOException e) {
      System.err.println("Cannot parse args: " + e);
    }

    Gui.setStatus("Reading configuration arguments.");
    try {
      loadIniFile("kvm.ini"); //FIXME use jar path instead of working dir
    } catch (Exception e) {
      System.err.println("Failed to load kvm.ini: " + e);
    }
    
    String hostcfg = Registry.get("kvm.host");

    if (hostcfg == null) {
      Gui.fatal("No host specified");
      return;
    }
    Registry.enableSection("host."+hostcfg, 10010);

    Gui.setStatus("Setting system policies.");
    HashMap<String, Registry.Setter> copySpec = new HashMap<String, Registry.Setter>();
    copySpec.put("security", new Registry.Setter() {
      public void set(String k, String v) { Security.setProperty(k, v); }
    });
    copySpec.put("system", new Registry.Setter() {
      public void set(String k, String v) { System.setProperty(k, v); }
    });
    Registry.copyByFirstComponent(copySpec);

    // Depends on Security.* settings!
    if ("UNSAFE-ALL".equals(System.getProperty("https.cipherSuites")))
      System.setProperty("https.cipherSuites", String.join(",",
	SSLUtilities.getSupportedCipherSuites()));

    // Initialise log, as all system settings were copied.
    log = LogFactory.getLog(KVMDirect.class);

    log.debug("Starting KVMDirect " + KVMDirect.class.getPackage().getImplementationVersion());

    for (String key: new String[]{"jdk.tls.disabledAlgorithms","jdk.certpath.disabledAlgorithms"})
      log.debug("Security " + key + " = " + Security.getProperty(key));
    for (String key: new String[]{"https.protocols","https.cipherSuites"})
      log.debug("System " + key + " = " + System.getProperty(key));

    boolean trustCerts = "true".equals(Registry.get("security.hack.trustAllHttpsCertificates"));
    boolean trustHosts = "true".equals(Registry.get("security.hack.trustAllHostnames"));
    log.debug("Hack security.hack.trustAllHttpsCertificates: " + trustCerts);
    log.debug("Hack security.hack.trustAllHostnames: " + trustHosts);

    Gui.setStatus("Starting HTTP Client.");
    Session session;
    try {
      org.apache.http.impl.conn.PoolingHttpClientConnectionManager cm =
	new org.apache.http.impl.conn.PoolingHttpClientConnectionManager(
	  org.apache.http.config.RegistryBuilder
	    .<org.apache.http.conn.socket.ConnectionSocketFactory> create()
	    .register("https", new SSLConnectionSocketFactory(
	      trustCerts ?  org.apache.http.ssl.SSLContexts.custom().loadTrustMaterial(
			       null, TrustAllStrategy.INSTANCE
                            ).build().getSocketFactory()
			 : (javax.net.ssl.SSLSocketFactory) javax.net.ssl.SSLSocketFactory.getDefault(),
	      split(System.getProperty("https.protocols")),
	      split(System.getProperty("https.cipherSuites")),
	      trustHosts ? NoopHostnameVerifier.INSTANCE
                         : SSLConnectionSocketFactory.getDefaultHostnameVerifier()
	    ))
	    .register("http", new org.apache.http.conn.socket.PlainConnectionSocketFactory())
	    .build()
	  ,
	  //SuperTwin2 invalid content-length fix
	  new org.apache.http.impl.conn.ManagedHttpClientConnectionFactory(
	    null, null, new HideInvalidContentLengthStrategy(), null
	  )
	);

      //(getDefaultRegistry(), connFactory, null, null, -1, MILISECONDS)
      org.apache.http.impl.client.HttpClientBuilder builder = 
	org.apache.http.impl.client.HttpClients
	  .custom()
	  .useSystemProperties()
          .setConnectionManager(cm)
	  .disableRedirectHandling();
      Session.basicBuilder = builder;
      session = new Session(builder);
    } catch (Exception e) {
      Gui.fatal(log, "Initialisation of http client failed", e);
      return;
    }

    try {
      ebik.kvm.queries.Query.registerEngines(Session.basic());
      ebik.kvm.queries.Query.compute(10000, 10090);
    } catch (Exception e) {
      Gui.fatal(log, "Failed to evaluate queries", e);
      return;
    }

    // `host` is special: if host.`host`.host exits, then it's value has priority over arguments.
    Registry.enableSection("host."+hostcfg, 10030);
    String host = Registry.get("kvm.host");
    Registry.enableSection("host."+hostcfg, 10010);

    if (! host.contains("://")) {
      host = "http://" + host;
    }
    
    try {
      session.setPrefix(host);
    } catch (Exception e) {
      Gui.fatal(log, "Invalid host url" + host, e);
      return;
    }

    //FIXME Rework of GUI (no console redirection. Use some sort of status!)

    // This is intended for standard library url connections that may be found in downloaded jars.
    if (trustCerts) SSLUtilities.trustAllHttpsCertificates();
    if (trustHosts) SSLUtilities.trustAllHostnames();

    URL.setURLStreamHandlerFactory(new ebik.http.UrlHandlerFactory(session));

    HashMap<String, KVM> services = new HashMap<String, KVM> ();
    log.trace("about to load kvm drivers");
    Gui.setStatus("Loading kvm modules.");
    for (KVM kvm: new ebik.util.ChainedIterator<KVM>()
	  .add(ServiceLoader.load(KVM.class).iterator())
	  .add(new ebik.util.DevelServiceLoader<KVM>(KVM.class))
	) {
      log.trace(kvm.getModuleName());
      try {
	kvm.setSession(session);
      } catch(IOException e) {
	log.fatal("Cannot initialize "+kvm.getModuleName() +": "+e.toString());
	continue;
      }
      services.put(kvm.getModuleName(), kvm);
      log.trace("Found kvm module:"+kvm.getModuleName());
    }
    log.trace("kvm drivers loaded");

    boolean started = false;
    Gui.setStatus("Detecting kvm type.");

    URL redir = null;
    Response mainpage;
    while (true) {
      try {
	if (redir == null) {
	  mainpage = session.request("").getResponse();
	} else {
	  mainpage = session.request(redir).getResponse();
	}
	Vector<String> remove = new Vector<String>();
	for (String key: services.keySet()) {
	  KVM kvm = services.get(key);
	  try {
	    if (!kvm.homePageMatches(mainpage))
	      remove.add(key);
	  } catch (Exception m) {
	    log.error(key + " raised error while trying to match page", m);
	  }
	}
	for (String key: remove)
	  services.remove(key);
	if (services.size() < 1) {
	  log.debug("< ----- PAGE START ----- >\n" +
	    mainpage.toString().replace("\r","␍").replace("\t","␉").replace("\n","␊\n") +
	    "< ----- PAGE END ------ >"
	  );
	  Gui.fatal("No KVM handler matched.");
	  return;
	}
	String redirS =  mainpage.getHeader("Location");
	if (redirS == null) break;
	redir = new URL(redirS);
	session.setPrefix(new URL(redir.getProtocol(), redir.getHost(), redir.getPort(), "").toString());
      } catch (Exception e) {
	Gui.fatal(log, "Internal error while matching homepage", e);
	return;
      }
    }

    if (services.size() > 1) {
      log.error("More than one KVM matched. This is ERROR! Trying random one though.");
    }
    String kvmName = null;
    KVM kvm = null;
    for (String key: services.keySet()) {
      kvm = services.get(key);
      String err = kvm.checkParams();
      if (err != null)
	System.err.println(key + ": " + err);
      else if ("only".equals(Registry.get("kvm.match")))
	System.out.println("Matched: " + key);
      else
	kvmName = key;
    }
    if (kvmName == null) {
      Gui.fatal("Failed to initialize KVM module.");
    }
    try {
      Gui.setStatus(log, "Starting KVM module: " + kvmName);
      gui.setKvm(kvm);
      kvm.start(gui);
      started = true;
    } catch (Exception exc) {
      Gui.fatal(log, "Error starting KVM " + kvmName, exc);
    }

    if (!started) {
      gui.shutdown();
    }
  }
}
