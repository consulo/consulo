/*
 * Copyright 2013-2020 consulo.io
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
package consulo.ide.eap.plugins;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginManager;
import consulo.ui.UIAccess;
import consulo.util.lang.StringUtil;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * @author VISTALL
 * @since 2020-09-03
 */
public class ExperimentalPluginsNotifier implements StartupActivity.Background {
  private final AtomicBoolean myAlreadyShow = new AtomicBoolean();

  @Override
  public void runActivity(@Nonnull UIAccess uiAccess, @Nonnull Project project) {
    if(myAlreadyShow.compareAndSet(false, true)) {
      List<PluginDescriptor> plugins = PluginManager.getPlugins().stream().filter(PluginDescriptor::isExperimental).collect(Collectors.toList());
      if (plugins.isEmpty()) {
        return;
      }

      String content = StringUtil.join(plugins, (it) -> "<b>" + it.getName() + "</b>", ", ") + " loaded";

      new Notification("Experimental Plugins", "Experimental Plugins", content, NotificationType.WARNING).notify(project);
    }
  }
}
