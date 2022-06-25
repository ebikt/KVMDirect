package ebik.kvm.module;

import java.awt.Component;
import javax.swing.JFrame;


public interface Gui {
  public void registerDownloaderForShutdown(ebik.javaweb.PrivateDownloader downloader);
  public void addHiddenComponent(Component c);
  public void addToplevelFrame(JFrame w);
  public void shutdown();
  public void shutdown(int status);
}
