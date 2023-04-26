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
package consulo.application.impl.internal;

import consulo.application.Application;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginIds;
import consulo.container.plugin.PluginManager;
import consulo.disposer.Disposable;
import consulo.disposer.internal.DiposerRegisterChecker;
import consulo.logging.Logger;

/**
 * @author VISTALL
 * @since 26/04/2023
 */
public class DisposerPluginChecker implements DiposerRegisterChecker {
  private static final Logger LOG = Logger.getInstance(DisposerPluginChecker.class);

  @Override
  public void checkRegister(Disposable parent, Disposable target) {
    if (parent instanceof Application) {
      PluginId pluginId = PluginManager.getPluginId(target.getClass());

      if (PluginIds.isPlatformPlugin(pluginId)) {
        return;
      }

      LOG.error("Trying register disposable " + target + " to Application. Please prefer applicationService inside plugin.");
    }
  }
}
