package ebik.kvm;

import ebik.kvm.module.KVM;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.Component;
import java.awt.Container;

import javax.swing.JLabel;
import javax.swing.JFrame;
import javax.swing.BoxLayout;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

public class Gui extends WindowAdapter implements ebik.kvm.module.Gui {
  protected JFrame main = null;
  protected JLabel status = null;
  protected KVM kvm = null;
  protected java.util.Vector<ebik.javaweb.PrivateDownloader> downloaders =
    new java.util.Vector<ebik.javaweb.PrivateDownloader>();
  private static Log log = null;

  private static Gui gui = null;

  public Gui() {
    gui = this;
    main = new JFrame();
    main.setBounds(0,0,800,100); //FIXME
    main.setTitle("KVMDirect v" + Gui.class.getPackage().getImplementationVersion());
    Container pane = main.getContentPane();
    main.setLayout(new BoxLayout(pane, BoxLayout.X_AXIS));
    status = new JLabel("Starting...");
    pane.add(status);

    main.pack();
    main.setVisible(true);
    addToplevelFrame(main);
  }

  public static void setLog() {
    log = LogFactory.getLog(Gui.class);
  }

  public static void setStatus(Log log, String text) {
    log.info(text);
    setStatus(text);
  }
  public static void setStatus(String text) {
    gui.status.setText(text);
  }

  public static void fatal(Log log, String text, Exception e) {
    log.fatal(text, e);
    //FIXME
    gui.shutdown(1);
  }

  public static void fatal(Log log, String text) {
    log.fatal(text);
    //FIXME
    gui.shutdown(1);
  }

  public static void fatal(String text) {
    System.out.println(text);
    //FIXME
    gui.shutdown(1);
  }

  public void registerDownloaderForShutdown(ebik.javaweb.PrivateDownloader downloader) {
    downloaders.add(downloader);
  }

  public void setKvm(KVM thekvm) {
    kvm = thekvm;
  }

  public void shutdown(int status) {
    if (kvm != null) {
      try {
	kvm.shutdown();
      } catch (Exception e) {
	log.error("Error while shutting down kvm", e);
      }
    }
    for(ebik.javaweb.PrivateDownloader downloader: downloaders)
      downloader.close();
    if (main != null)
      main.dispose();
    System.exit(status);
  }

  public void shutdown() {
    shutdown(0);
  }

  public void addHiddenComponent(Component com) {
    //FIXME
    main.getContentPane().add(com);
  }

  boolean closeArmed = true;
  public void windowClosing(WindowEvent e) {
    if (closeArmed) {
      closeArmed = false;
      shutdown();
    }
  }

  public void addToplevelFrame(JFrame w) {
    w.addWindowListener(this);
    w.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
  }
}
