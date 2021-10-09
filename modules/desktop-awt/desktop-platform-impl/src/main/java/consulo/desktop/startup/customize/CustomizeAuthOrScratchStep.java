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
package consulo.desktop.startup.customize;

import com.intellij.ide.customize.AbstractCustomizeWizardStep;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.ui.AppIcon;
import com.intellij.ui.SeparatorWithText;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.panels.HorizontalLayout;
import com.intellij.util.ui.UIUtil;
import consulo.container.plugin.PluginId;
import consulo.externalService.impl.HubAuthorizationService;
import consulo.externalStorage.ExternalStoragePluginManager;
import consulo.externalStorage.plugin.StoragePlugin;
import consulo.externalStorage.plugin.StoragePluginState;
import consulo.ide.customize.CustomizeWizardContext;
import consulo.localize.LocalizeValue;
import consulo.ui.Alerts;
import consulo.ui.UIAccess;
import consulo.ui.Window;
import consulo.util.concurrent.AsyncResult;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author VISTALL
 * @since 18/09/2021
 */
public class CustomizeAuthOrScratchStep extends AbstractCustomizeWizardStep {
  private String myAuthorizeEmail;

  public CustomizeAuthOrScratchStep(@Nonnull Runnable nextAction, CustomizeWizardContext context) {
    setLayout(new BorderLayout(10, 10));

    JPanel verticalGroup = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.MIDDLE, 10, 25, true, false));

    JLabel textLabel = new JBLabel("<html><body>I'm new user and want setup <b>Consulo</b> from scratch</html></body>", SwingConstants.CENTER);
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
        Window activeWindow = Window.getActiveWindow();
        if (activeWindow != null) {
          AppIcon.getInstance().requestFocus(activeWindow);
        }

        myAuthorizeEmail = hubAuthorizationService.getEmail();

        fetchExternalStorage(nextAction, context);
      });

      result.doWhenRejected(() -> {
        UIAccess uiAccess = Application.get().getLastUIAccess();

        uiAccess.give(() -> Alerts.okError(LocalizeValue.localizeTODO("Failed to request oauth token")).showAsync());
      });
    });
    hubPanel.add(loginButton, HorizontalLayout.CENTER);
    verticalGroup.add(hubPanel);

    add(verticalGroup, BorderLayout.CENTER);
  }

  private void fetchExternalStorage(Runnable nextAction, CustomizeWizardContext context) {
    Application application = Application.get();
    application.invokeLater(() -> {
      ModalityState currentModalityState = application.getCurrentModalityState();

      application.invokeLater(() -> {
        Task.Modal.queue(null, LocalizeValue.localizeTODO("Fetching external storage..."), indicator -> {
          StoragePlugin[] list = ExternalStoragePluginManager.list();

          Set<PluginId> pluginsForDownload = new TreeSet<>(context.getPluginsForDownload());

          for (StoragePlugin storagePlugin : list) {
            if(storagePlugin.state == StoragePluginState.ENABLED) {
              pluginsForDownload.add(PluginId.getId(storagePlugin.id));
            }
          }

          context.setPluginsForDownload(pluginsForDownload);

          nextAction.run();
        });
      }, currentModalityState);
    }, ModalityState.any());
  }

  @Override
  public void onStepLeave(@Nonnull CustomizeWizardContext customizeWizardContext) {
    customizeWizardContext.setEmail(myAuthorizeEmail);
  }

  @Override
  protected String getTitle() {
    return "Welcome";
  }

  @Override
  protected String getHTMLHeader() {
    return "<html><body><h2>Welcome</h2>&nbsp;</body></html>";
  }

  @Override
  protected String getHTMLFooter() {
    return null;
  }
}
