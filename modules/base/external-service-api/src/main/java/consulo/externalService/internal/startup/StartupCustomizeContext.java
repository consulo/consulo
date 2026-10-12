/*
 * Copyright 2013-2026 consulo.io
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
package consulo.externalService.internal.startup;

import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.localize.LocalizeValue;
import consulo.util.dataholder.UserDataHolder;

import java.util.List;
import java.util.Set;

/**
 * What the steps of one run of the startup wizard share.
 *
 * @author VISTALL
 * @since 2026-10-11
 */
public interface StartupCustomizeContext extends UserDataHolder {
    /**
     * Whether the wizard runs because the application starts for the first time, rather than on request.
     */
    boolean isFirstShow();

    /**
     * The plugins of the repository, read once before the first step is shown - empty when the repository could not
     * be reached.
     */
    List<PluginDescriptor> getRepositoryPlugins();

    /**
     * The plugins chosen for installation. The steps choosing plugins all write here, so a choice made on one step shows
     * on the others.
     */
    Set<PluginId> getPluginsToInstall();

    /**
     * Adds work to run once the user finishes the wizard, in the order added.
     */
    void addFinishTask(StartupCustomizeFinishTask task);

    /**
     * Reports what the finish work could not do - the wizard shows all of it to the user once the work is over.
     */
    void addProblem(LocalizeValue problem);
}
