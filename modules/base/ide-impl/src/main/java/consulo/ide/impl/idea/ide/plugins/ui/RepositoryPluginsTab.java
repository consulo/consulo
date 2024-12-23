/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.plugins.ui;

import consulo.application.Application;
import consulo.application.eap.EarlyAccessProgramManager;
import consulo.configurable.ConfigurableSession;
import consulo.container.plugin.PluginDescriptor;
import consulo.externalService.update.UpdateSettings;
import consulo.ide.impl.idea.ide.plugins.RepositoryHelper;
import consulo.ide.localize.IdeLocalize;
import consulo.platform.base.localize.CommonLocalize;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.SimpleReference;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * User: anna
 */
public class RepositoryPluginsTab extends PluginTab {
    public RepositoryPluginsTab(PluginsPanel pluginsPanel) {
        super(pluginsPanel);
        init();
    }

    @RequiredUIAccess
    protected CompletableFuture<?> loadAvailablePlugins() {
        EarlyAccessProgramManager earlyAccessProgramManager =
            ConfigurableSession.get().getOrCopy(Application.get(), EarlyAccessProgramManager.class);

        return loadPluginsFromHostInBackground(earlyAccessProgramManager);
    }

    @RequiredUIAccess
    public void reload() {
        UIAccess uiAccess = UIAccess.current();

        loadAvailablePlugins().whenCompleteAsync((o, throwable) -> {
            myPluginList.reset();
        }, uiAccess);
    }

    /**
     * Start a new thread which downloads new list of plugins from the site in
     * the background and updates a list of plugins in the table.
     */
    protected CompletableFuture<?> loadPluginsFromHostInBackground(EarlyAccessProgramManager earlyAccessProgramManager) {
        setDownloadStatus(true);

        CompletableFuture<?> future = new CompletableFuture<>();

        Application.get().executeOnPooledThread(() -> {
            SimpleReference<List<PluginDescriptor>> ref = SimpleReference.create();
            List<String> errorMessages = new ArrayList<>();

            try {
                ref.set(RepositoryHelper.loadOnlyPluginsFromRepository(
                    null,
                    UpdateSettings.getInstance().getChannel(),
                    earlyAccessProgramManager
                ));
            }
            catch (Throwable e) {
                LOG.info(e);
                errorMessages.add(e.getMessage());
                future.completeExceptionally(e);
            }

            UIUtil.invokeLaterIfNeeded(() -> {
                setDownloadStatus(false);
                List<PluginDescriptor> list = ref.get();

                if (list != null) {
                    modifyPluginsList(list);
                    future.complete(null);
                }

                if (!errorMessages.isEmpty()) {
                    if (Messages.showOkCancelDialog(
                        IdeLocalize.errorListOfPluginsWasNotLoaded(StringUtil.join(errorMessages, ", ")).get(),
                        IdeLocalize.titlePlugins().get(),
                        CommonLocalize.buttonRetry().get(),
                        CommonLocalize.buttonCancel().get(),
                        Messages.getErrorIcon()
                    ) == Messages.OK) {
                        loadPluginsFromHostInBackground(earlyAccessProgramManager);
                    }
                }
            });
        });

        return future;
    }

    @Override
    public List<PluginSorter> getSorters() {
        return List.of(PluginSorter.NAME, PluginSorter.RATING, PluginSorter.DOWNLOADS, PluginSorter.LAST_UPDATED);
    }

    @RequiredUIAccess
    @Override
    protected void onShow() {
        super.onShow();
        loadAvailablePlugins();
    }
}
