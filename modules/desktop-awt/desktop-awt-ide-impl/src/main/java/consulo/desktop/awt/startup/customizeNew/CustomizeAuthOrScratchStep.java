/*
 * Copyright 2013-2021 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.desktop.awt.startup.customizeNew;

import consulo.application.Application;
import consulo.application.progress.Task;
import consulo.container.plugin.PluginId;
import consulo.disposer.Disposable;
import consulo.ide.impl.externalService.impl.HubAuthorizationService;
import consulo.ide.impl.externalStorage.ExternalStoragePluginManager;
import consulo.ide.impl.externalStorage.plugin.StoragePlugin;
import consulo.ide.impl.externalStorage.plugin.StoragePluginState;
import consulo.ide.impl.startup.customize.CustomizeWizardContext;
import consulo.localize.LocalizeValue;
import consulo.ui.Alerts;
import consulo.ui.ModalityState;
import consulo.ui.UIAccess;
import consulo.ui.Window;
import consulo.ui.ex.AppIcon;
import consulo.ui.ex.awt.*;
import consulo.util.concurrent.AsyncResult;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

/**
 * @author VISTALL
 * @since 18/09/2021
 */
public class CustomizeAuthOrScratchStep extends AbstractCustomizeWizardStep {
  @Nonnull
  private final Runnable myNextAction;
  private String myAuthorizeEmail;

  public CustomizeAuthOrScratchStep(@Nonnull Runnable nextAction) {
    myNextAction = nextAction;
  }

  @Nonnull
  @Override
  public JPanel createComponnent(CustomizeWizardContext context, @Nonnull Disposable uiDisposable) {
    JPanel panel = new JPanel(new BorderLayout(10, 10));

    JPanel verticalGroup = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.MIDDLE, 10, 25, true, false));

    JLabel textLabel =
      new JBLabel("<html><body>I'm new user and want setup <b>Consulo</b> from scratch</html></body>", SwingConstants.CENTER);
    textLabel.setFont(UIUtil.getLabelFont(UIUtil.FontSize.BIGGER));

    verticalGroup.add(textLabel);
    verticalGroup.add(new SeparatorWithText("or"));

    JPanel hubPanel = new JPanel(new HorizontalLayout(10, SwingConstants.CENTER));
    JLabel hubLabel = new JLabel("<html><body>Already use Hub, and want setup <b>Consulo</b> from it</html></body>");
    hubLabel.setFont(UIUtil.getLabelFont(UIUtil.FontSize.BIGGER));

    HubAuthorizationService hubAuthorizationService = Application.get().getInstance(HubAuthorizationService.class);

    hubPanel.add(hubLabel, HorizontalLayout.CENTER);
    JButton loginButton = new JButton("Login");
    loginButton.addActionListener(e -> {
      AsyncResult<Void> result = hubAuthorizationService.openLinkSite(true);
      result.doWhenDone(() -> {
        Window activeWindow = consulo.ui.Window.getActiveWindow();
        if (activeWindow != null) {
          AppIcon.getInstance().requestFocus(activeWindow);
        }

        myAuthorizeEmail = hubAuthorizationService.getEmail();

        fetchExternalStorage(myNextAction, context);
      });

      result.doWhenRejected(() -> {
        UIAccess uiAccess = Application.get().getLastUIAccess();

        uiAccess.give(() -> Alerts.okError(LocalizeValue.localizeTODO("Failed to request oauth token")).showAsync());
      });
    });
    hubPanel.add(loginButton, HorizontalLayout.CENTER);
    verticalGroup.add(hubPanel);

    panel.add(verticalGroup, BorderLayout.CENTER);
    return panel;
  }

  private void fetchExternalStorage(Runnable nextAction, CustomizeWizardContext context) {
    Application application = Application.get();
    application.invokeLater(() -> {
      ModalityState currentModalityState = application.getCurrentModalityState();

      application.invokeLater(() -> {
        Task.Modal.queue(null, LocalizeValue.localizeTODO("Fetching external storage..."), indicator -> {
          StoragePlugin[] list = ExternalStoragePluginManager.list();

          Set<PluginId> toDownload = context.getPluginsForDownload();
          toDownload.clear();
          
          for (StoragePlugin storagePlugin : list) {
            if (storagePlugin.state == StoragePluginState.ENABLED) {
              toDownload.add(PluginId.getId(storagePlugin.id));
            }
          }

          nextAction.run();
        });
      }, currentModalityState);
    }, application.getAnyModalityState());
  }

  @Override
  public void onStepLeave(@Nonnull CustomizeWizardContext customizeWizardContext) {
    customizeWizardContext.setEmail(myAuthorizeEmail);
  }

  @Override
  protected String getHTMLHeader() {
    return "<html><body><h2>Hub</h2></body></html>";
  }

  @Override
  public boolean isVisible(@Nonnull CustomizeWizardContext context) {
    return false; // FIXME [VISTALL] not supported for now
  }
}