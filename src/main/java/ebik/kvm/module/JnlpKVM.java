package ebik.kvm.module;

import ebik.javaweb.SimpleJNLP;
import ebik.kvm.Gui;

public abstract class JnlpKVM extends ebik.kvm.module.KVM {
  public abstract String getJnlpXML() throws Exception;

  public void run(ebik.kvm.module.Gui gui) throws Exception {
    String jnlpDoc = getJnlpXML();
    Gui.setStatus(log, "[" + this.getClass().getName() + "] Parsing JNLP Document");

    log.trace(jnlpDoc);
    log.trace("About to parse");
    SimpleJNLP jnlp = SimpleJNLP.fromString(jnlpDoc);
    String bls = params.get("kvm.jnlp.blacklist");
    if (bls!=null)
      for (String bl: bls.split(" *, *"))
	if (bl.trim().length() > 0)
	  jnlp.blacklistResources(bl.trim());
    jnlp.debug();
    String os     = System.getProperty("os.name");
    String arch   = System.getProperty("os.arch");
    for (String param: new String[]{
	"kvm.jnlp.unzip."+os+"."+arch,
	"kvm.jnlp.unzip."+os,
	"kvm.jnlp.unzip"
    }) {
      String archives = params.get(param);
      log.trace(param + ": " + archives);
      if (archives!=null)
	for (String ar: archives.split(" *, *"))
	  if (ar.trim().length() > 0) {
	    Gui.setStatus(log, "[" + this.getClass().getName() + "] Unzipping configuration provided archove " +ar);
	    jnlp.unzipTo(downloader, ar.trim());
	  }
    }
    Gui.setStatus(log, "[" + this.getClass().getName() + "] Downloading jars." );
    jnlp.download(downloader);
    Gui.setStatus(log, "[" + this.getClass().getName() + "] Launched java intepreter." ); // FIXME: in fact not yet.
    int rv = jnlp.run();
    log.debug("Java interpreter returned with code: " + rv);
    gui.shutdown();
  }
}
