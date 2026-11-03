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
package consulo.externalService.impl.internal.plugin.ui.action;

import consulo.application.Application;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.externalService.impl.internal.plugin.PluginInstallUtil;
import consulo.externalService.impl.internal.update.PlatformOrPluginNode;
import consulo.externalService.impl.internal.update.PlatformOrPluginUpdateResult;
import consulo.externalService.impl.internal.update.UnifiedPlatformOrPluginDialog;
import consulo.externalService.internal.PlatformOrPluginUpdateResultType;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.MessageBoxes;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author lloix
 */
public class InstallPluginAction {
    @RequiredUIAccess
    public static void install(
        Application application,
        @Nullable Project project,
        @Nullable Component component,
        PluginDescriptor descriptor,
        List<PluginDescriptor> allPlugins,
        boolean alwaysShowDialog,
        @Nullable Consumer<Collection<PluginDescriptor>> afterCallback
    ) {
        if (!descriptor.isExperimental()) {
            downloadAndInstallPlugins(application, project,component, List.of(descriptor), allPlugins, alwaysShowDialog, afterCallback);
            return;
        }

        MessageBoxes.yesNo()
            .asWarning()
            .title(application.getName())
            .text(LocalizeValue.localizeTODO(
                "Are you sure install experimental plugin? Plugin can make IDE unstable, and may not implement expected features"))
            .showAsync(component)
            .whenCompleteAsync((agreed, error) -> {
                if (Boolean.TRUE.equals(agreed)) {
                    downloadAndInstallPlugins(application, project, component, List.of(descriptor), allPlugins, alwaysShowDialog, afterCallback);
                }
            }, UIAccess.current());
    }

    @RequiredUIAccess
    public static void downloadAndInstallPlugins(
        Application application,
        @Nullable Project project,
        @Nullable Component component,
        List<PluginDescriptor> toInstall,
        List<PluginDescriptor> allPlugins,
        boolean alwaysShowDialog,
        @Nullable Consumer<Collection<PluginDescriptor>> afterCallback
    ) {
        Set<PluginDescriptor> withDependencies = PluginInstallUtil.getPluginsForInstall(toInstall, allPlugins);

        List<PlatformOrPluginNode> nodes = withDependencies.stream()
            .map(descriptor -> new PlatformOrPluginNode(descriptor.getPluginId(), null, descriptor))
            .toList();
        PlatformOrPluginUpdateResult result = new PlatformOrPluginUpdateResult(PlatformOrPluginUpdateResultType.PLUGIN_INSTALL, nodes);

        Predicate<PluginId> greenStrategy = pluginId -> toInstall.stream().noneMatch(it -> it.getPluginId().equals(pluginId));

        UnifiedPlatformOrPluginDialog dialog = new UnifiedPlatformOrPluginDialog(application, project, result, greenStrategy, afterCallback, true);
        if (withDependencies.size() == toInstall.size() && !alwaysShowDialog) {
            dialog.doFastOK(component);
        }
        else {
            dialog.show();
        }
    }
}
