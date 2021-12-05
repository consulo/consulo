/*
 * Copyright 2013-2019 consulo.io
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
package consulo.sandboxPlugin.ide.remoteServer;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModulePointerManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.options.UnnamedConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.remoteServer.ServerType;
import com.intellij.remoteServer.configuration.deployment.DeploymentConfiguration;
import com.intellij.remoteServer.configuration.deployment.DeploymentConfigurator;
import com.intellij.remoteServer.configuration.deployment.DeploymentSource;
import com.intellij.remoteServer.impl.configuration.deploySource.impl.ModuleDeploymentSourceImpl;
import com.intellij.remoteServer.runtime.ServerConnector;
import com.intellij.remoteServer.runtime.ServerTaskExecutor;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author VISTALL
 * @since 2019-02-25
 */
public class SandServerType extends ServerType<SandServerConfiguration> {
  public SandServerType() {
    super("sand");
  }

  @Nonnull
  @Override
  public String getPresentableName() {
    return "Sand";
  }

  @Nonnull
  @Override
  public Image getIcon() {
    return AllIcons.Actions.Help;
  }

  @Nonnull
  @Override
  public SandServerConfiguration createDefaultConfiguration() {
    return new SandServerConfiguration();
  }

  @Nonnull
  @Override
  public UnnamedConfigurable createConfigurable(@Nonnull SandServerConfiguration configuration) {
    return new UnnamedConfigurable() {
      @RequiredUIAccess
      @Override
      public boolean isModified() {
        return false;
      }

      @RequiredUIAccess
      @Nullable
      @Override
      public Component createUIComponent() {
        return Label.create("Sand stub UI");
      }

      @RequiredUIAccess
      @Override
      public void apply() throws ConfigurationException {

      }

      @RequiredUIAccess
      @Override
      public void reset() {

      }
    };
  }

  @Nonnull
  @Override
  public DeploymentConfigurator<?> createDeploymentConfigurator(Project project) {
    return new DeploymentConfigurator<DeploymentConfiguration>() {
      @Nonnull
      @Override
      public List<DeploymentSource> getAvailableDeploymentSources() {
        Module[] modules = ModuleManager.getInstance(project).getModules();
        ModulePointerManager pointerManager = ModulePointerManager.getInstance(project);
        return Arrays.stream(modules).map(module -> new ModuleDeploymentSourceImpl(pointerManager.create(module))).collect(Collectors.toList());
      }

      @Nonnull
      @Override
      public DeploymentConfiguration createDefaultConfiguration(@Nonnull DeploymentSource source) {
        return new DeploymentConfiguration() {
          @Override
          public PersistentStateComponent<?> getSerializer() {
            return new PersistentStateComponent<Object>() {
              @Nullable
              @Override
              public Object getState() {
                return new Element("state");
              }

              @Override
              public void loadState(Object state) {

              }
            };
          }
        };
      }

      @Nullable
      @Override
      public SettingsEditor<DeploymentConfiguration> createEditor(@Nonnull DeploymentSource source) {
        return null;
      }
    };
  }

  @Nonnull
  @Override
  public ServerConnector<?> createConnector(@Nonnull SandServerConfiguration configuration, @Nonnull ServerTaskExecutor asyncTasksExecutor) {
    return new ServerConnector<DeploymentConfiguration>() {
      @Override
      public void connect(@Nonnull ConnectionCallback<DeploymentConfiguration> callback) {
        callback.errorOccurred("error");
      }
    };
  }
}
