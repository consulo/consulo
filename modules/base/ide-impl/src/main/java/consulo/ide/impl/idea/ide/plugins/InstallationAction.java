/*
 * Copyright 2013-2024 consulo.io
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
package consulo.ide.impl.idea.ide.plugins;

import consulo.application.AllIcons;
import consulo.ide.impl.localize.PluginLocalize;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;

/**
* @author UNV
* @since 2024-11-14
*/
enum InstallationAction {
    INSTALL(PluginLocalize.installationActionInstall(), AllIcons.Actions.Install),
    UNINSTALL(PluginLocalize.installationActionUninstall(), AllIcons.Actions.Cancel),
    RESTART(PluginLocalize.installationActionRestart(), AllIcons.Actions.Restart);

    private final LocalizeValue myTitle;
    private final Image myIcon;

    InstallationAction(LocalizeValue title, Image icon) {
        myTitle = title;
        myIcon = icon;
    }

    public LocalizeValue getTitle() {
        return myTitle;
    }

    public Image getIcon() {
        return myIcon;
    }
}
