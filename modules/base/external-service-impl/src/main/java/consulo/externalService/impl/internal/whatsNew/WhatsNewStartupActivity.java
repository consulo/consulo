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
package consulo.externalService.impl.internal.whatsNew;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.configuration.editor.ConfigurationFileEditorManager;
import consulo.externalService.impl.internal.pluginHistory.UpdateHistory;
import consulo.project.Project;
import consulo.project.startup.PostStartupActivity;
import consulo.ui.UIAccess;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author VISTALL
 * @since 21/11/2021
 */
@ExtensionImpl(id = "WhatsNew", order = "after OpenFilesActivity")
public class WhatsNewStartupActivity implements PostStartupActivity, DumbAware {
    private final Provider<UpdateHistory> myUpdateHistoryProvider;
    private final Provider<ConfigurationFileEditorManager> myConfigurationFileEditorManagerProvider;

    private final AtomicBoolean myAlreadyShow = new AtomicBoolean();

    @Inject
    public WhatsNewStartupActivity(Provider<UpdateHistory> updateHistoryProvider,
                                   Provider<ConfigurationFileEditorManager> configurationFileEditorManagerProvider) {
        myUpdateHistoryProvider = updateHistoryProvider;
        myConfigurationFileEditorManagerProvider = configurationFileEditorManagerProvider;
    }

    @Override
    public void runActivity(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
        if (myAlreadyShow.compareAndSet(false, true)) {
            UpdateHistory updateHistory = myUpdateHistoryProvider.get();

            if (updateHistory.isShowChangeLog()) {
                updateHistory.setShowChangeLog(false);

                ConfigurationFileEditorManager manager = myConfigurationFileEditorManagerProvider.get();

                uiAccess.give(() -> manager.open(project, WhatsNewConfigurationFileEditorProvider.class, Map.of()));
            }
        }
    }
}
