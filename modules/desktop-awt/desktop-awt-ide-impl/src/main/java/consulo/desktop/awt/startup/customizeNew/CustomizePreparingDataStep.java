/*
 * Copyright 2013-2023 consulo.io
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
import consulo.application.eap.EarlyAccessProgramManager;
import consulo.container.plugin.PluginDescriptor;
import consulo.disposer.Disposable;
import consulo.externalService.update.UpdateChannel;
import consulo.externalService.update.UpdateSettings;
import consulo.ide.impl.idea.ide.plugins.RepositoryHelper;
import consulo.ide.impl.startup.customize.CustomizeWizardContext;
import consulo.ide.impl.startup.customize.FirstTemplateLoader;
import consulo.logging.Logger;
import consulo.ui.UIAccess;
import consulo.ui.ex.awt.LoadingDecorator;
import consulo.ui.ex.awt.update.UiNotifyConnector;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 09/07/2023
 */
public class CustomizePreparingDataStep extends AbstractCustomizeWizardStep {
  private static final Logger LOG = Logger.getInstance(CustomizePreparingDataStep.class);

  private final Runnable myNextAction;

  public CustomizePreparingDataStep(Runnable nextAction) {
    myNextAction = nextAction;
  }

  @Override
  protected String getHTMLHeader() {
    return null;
  }

  @Nonnull
  @Override
  public JPanel createComponnent(CustomizeWizardContext context, @Nonnull Disposable uiDisposable) {
    UIAccess uiAccess = UIAccess.current();

    JPanel panel = new JPanel(new BorderLayout());
    LoadingDecorator decorator = new LoadingDecorator(new JPanel(), uiDisposable, 200);
    UiNotifyConnector.doWhenFirstShown(panel, () -> {
      decorator.setLoadingText("Connecting To Repository...");
      decorator.startLoading(false);

      startLoadData(decorator, uiAccess, context);
    });
    panel.add(decorator.getComponent(), BorderLayout.CENTER);
    return panel;
  }

  private void startLoadData(LoadingDecorator decorator, UIAccess uiAccess, CustomizeWizardContext context) {
    Application.get().executeOnPooledThread(() -> {
      CompletableFuture<UpdateChannel> channelFuture;
      UpdateChannel channel = context.getUpdateChannel();
      if (channel != null) {
        channelFuture = CompletableFuture.completedFuture(channel);
      }
      else {
        channelFuture = FirstTemplateLoader.requestPluginChannel();
      }

      channelFuture.whenComplete((updateChannel, t) -> {
        if (updateChannel == null) {
          updateChannel = UpdateChannel.release;
        }

        UpdateSettings.getInstance().setChannel(updateChannel);
        try {
          List<PluginDescriptor> plugins =
            RepositoryHelper.loadOnlyPluginsFromRepository(null, updateChannel, EarlyAccessProgramManager.getInstance());

          for (PluginDescriptor plugin : plugins) {
            context.addPluginDescriptor(plugin);
          }
        }
        catch (Throwable e) {
          LOG.warn(e);
        }

        FirstTemplateLoader.loadPredefinedTemplateSets().whenComplete((result, t2) -> {
          decorator.stopLoading();

          if (result != null) {
            context.getPredefinedTemplateSets().putAll(result);
          }
          
          uiAccess.give(myNextAction);
        });
      });
    });
  }

  @Override
  public boolean isVisible(@Nonnull CustomizeWizardContext context) {
    return context.getPredefinedTemplateSets().isEmpty();
  }
}
