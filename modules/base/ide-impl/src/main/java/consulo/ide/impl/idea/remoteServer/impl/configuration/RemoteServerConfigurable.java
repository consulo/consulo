package consulo.ide.impl.idea.remoteServer.impl.configuration;

import consulo.application.ApplicationManager;
import consulo.configurable.ConfigurationException;
import consulo.configurable.UnnamedConfigurable;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.ui.ex.awt.Messages;
import consulo.ide.impl.idea.openapi.ui.NamedConfigurable;
import consulo.remoteServer.configuration.RemoteServer;
import consulo.remoteServer.configuration.ServerConfiguration;
import consulo.remoteServer.runtime.ConnectionStatus;
import consulo.remoteServer.runtime.ServerConnection;
import consulo.remoteServer.runtime.ServerConnectionManager;
import consulo.application.util.Semaphore;
import consulo.ui.ex.awt.JBUI;
import consulo.disposer.Disposable;
import consulo.configurable.internal.ConfigurableUIMigrationUtil;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import org.jetbrains.annotations.Nls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author nik
 */
public class RemoteServerConfigurable extends NamedConfigurable<RemoteServer<?>> {
  private final UnnamedConfigurable myConfigurable;
  private final RemoteServer<?> myServer;
  private String myServerName;
  private boolean myNew;
  private JPanel myMainPanel;
  private JPanel mySettingsPanel;
  private JButton myTestConnectionButton;

  public <C extends ServerConfiguration> RemoteServerConfigurable(RemoteServer<C> server, Runnable treeUpdater, boolean isNew) {
    super(true, treeUpdater);
    myServer = server;
    myNew = isNew;
    myServerName = myServer.getName();
    C c = server.getConfiguration();
    myConfigurable = server.getType().createConfigurable(c);
  }

  @Override
  public RemoteServer<?> getEditableObject() {
    return myServer;
  }

  @Override
  public String getBannerSlogan() {
    return myServer.getName();
  }

  @RequiredUIAccess
  @Override
  public JComponent createOptionsPanel(Disposable parentDisposable) {
    mySettingsPanel.add(BorderLayout.CENTER, ConfigurableUIMigrationUtil.createComponent(myConfigurable, parentDisposable));
    myTestConnectionButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          myConfigurable.apply();
        }
        catch (ConfigurationException exc) {
          Messages.showErrorDialog(myMainPanel, "Cannot test connection: " + exc.getMessage(), exc.getTitle());
          return;
        }
        testConnection();
      }
    });
    myMainPanel.setBorder(JBUI.Borders.empty(0, 10, 0, 10));
    return myMainPanel;
  }

  private void testConnection() {
    final ServerConnection connection = ServerConnectionManager.getInstance().getOrCreateConnection(myServer);
    final AtomicReference<Runnable> showResultRef = new AtomicReference<Runnable>(null);
    new Task.Modal(null, "Connecting...", true) {
      @Override
      public void run(@Nonnull ProgressIndicator indicator) {
        indicator.setIndeterminate(true);
        final Semaphore semaphore = new Semaphore();
        semaphore.down();
        connection.connect(new Runnable() {
          @Override
          public void run() {
            showResultRef.set(new Runnable() {
              @Override
              public void run() {
                if (connection.getStatus() == ConnectionStatus.CONNECTED) {
                  Messages.showInfoMessage(myMainPanel, "Connection successful", "Test Connection");
                }
                else if (connection.getStatus() == ConnectionStatus.DISCONNECTED) {
                  Messages.showErrorDialog(myMainPanel, "Cannot connect: " + connection.getStatusText(), "Test Connection");
                }
              }
            });
            semaphore.up();
          }
        });
        while (!indicator.isCanceled()) {
          if (semaphore.waitFor(500)) {
            break;
          }
        }
        Runnable showResult = showResultRef.get();
        if (showResult != null) {
          ApplicationManager.getApplication().invokeLater(showResult);
        }
      }
    }.queue();
  }

  @Nls
  @Override
  public String getDisplayName() {
    return myServerName;
  }

  @Override
  public void setDisplayName(String name) {
    myServerName = name;
  }

  @RequiredUIAccess
  @Override
  public boolean isModified() {
    return myNew || myConfigurable.isModified() || !myServerName.equals(myServer.getName());
  }

  @RequiredUIAccess
  @Override
  public void apply() throws ConfigurationException {
    myConfigurable.apply();
    myNew = false;
  }

  @RequiredUIAccess
  @Override
  public void reset() {
    myConfigurable.reset();
  }

  @RequiredUIAccess
  @Override
  public void disposeUIResources() {
    myConfigurable.disposeUIResources();
  }

  @Nullable
  @Override
  public Image getIcon(boolean expanded) {
    return myServer.getType().getIcon();
  }
}
