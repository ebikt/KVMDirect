package ebik.kvm.module;

import ebik.javaweb.AppletWrapper;
import java.awt.Component;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import ebik.javaweb.AppletDesc;

public abstract class AppletKVM extends ebik.kvm.module.KVM {
  public abstract String getAppletHtml() throws Exception;

  private JFrame main = null;

  private class WindowMonitor implements java.awt.event.AWTEventListener {
    private Gui gui;
    public WindowMonitor(Gui g) { gui = g; }
    public void eventDispatched(java.awt.AWTEvent event) {
      switch (event.getID()){
	case WindowEvent.WINDOW_OPENED:
	case WindowEvent.WINDOW_CLOSED:
	  WindowEvent we = (WindowEvent)event;
	  log.trace("WindowEVent: " + event.getID());
	  log.trace("  param: " + we.paramString());
	  log.trace("  oldState: " + we.getOldState());
	  log.trace("  newState: " + we.getNewState());
	  java.awt.Window w = we.getWindow();
	  log.trace("  Window: " + w);
	  log.trace("  Owner: " + w.getOwner());
	  log.trace("  is main?: " + (w == main));
	  log.trace("  isActive?: " + w.isActive());
	  log.trace("  isAlwaysOnTop?: " + w.isActive());
	  log.trace("  isAutoRequestFocus?: " + w.isActive());
	  log.trace("  isFocusableWindow?: " + w.isActive());
	  log.trace("  isFocused?: " + w.isActive());
	  log.trace("  isLocationByPlatform?: " + w.isActive());
	  log.trace("  isOpaque?: " + w.isActive());
	  log.trace("  isValidateRoot?: " + w.isActive());
	  log.trace("  isShowing?: " + w.isActive());
	  log.trace("  isShowing?: " + w.isActive());

	  if (event.getID() == WindowEvent.WINDOW_CLOSED && w.getOwner() == null) {
	    gui.shutdown();
	  }
      }
    }
  }

  private void registerMonitor(Component c, Gui g) {
    c.getToolkit().addAWTEventListener(
      new WindowMonitor(g),
      java.awt.AWTEvent.WINDOW_EVENT_MASK
    );
  }

  private void startApplet(Gui gui, AppletDesc ad) throws Exception {
    ebik.kvm.Gui.setStatus(log, "[" + this.getClass().getName() + "] Starting Java applet");

    //FIXME Keyboard does not work?!

    log.trace("origbase: " + ad.baseUrl);
    log.trace("origjar:  " + ad.jarUrl);
    ad.jarUrl = session.absoluteUrls(ad.jarUrl);
    ad.baseUrl = session.getPrefix();
    log.trace("base: " + ad.baseUrl);
    log.trace("jar:  " + ad.jarUrl);

    AppletWrapper apl = ad.loadApplet(new Class<?>[]{com.serverengines.cookies.CookieMgr.class});
    log.trace("Starting applet.");
    int w = Integer.parseInt(ad.width);
    int h = Integer.parseInt(ad.height);
    main = new JFrame();
    main.setSize(w, h);
    main.setTitle(this.getModuleName() + " Console: " + ad.baseUrl);
    main.setVisible(true);
    gui.addToplevelFrame(main);
    JPanel p = new JPanel();
    registerMonitor(p, gui);
    p.setVisible(true);
    p.add((Component)apl.getApplet());
    ComponentAdapter ca = 
      new ComponentAdapter() {
	public void componentResized(ComponentEvent e) {
	  main.setSize(main.getPreferredSize());
	}
      };
    apl.addComponentListener(ca);
    main.getContentPane().add(
      new JScrollPane(p)
    );
    apl.setVisible(true);
    apl.init();
    apl.start();
    log.trace("Applet dimension: " + apl.getWidth() + " x " + apl.getHeight());
    ebik.kvm.Gui.setStatus(log, "[" + this.getClass().getName() + "] Java applet is running");
  }

  public void closeMain() {
    if (main != null) main.dispose();
  }

  public void run(Gui gui) throws Exception {
    startApplet(gui, AppletDesc.fromHtml(getAppletHtml(), session.getPrefix()));
  }


}
