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
package consulo.it.endpoint;

import consulo.application.WriteAction;
import consulo.endpoint.EndpointProvider;
import consulo.endpoint.EndpointViewOpener;
import consulo.it.HeadlessModules;
import consulo.it.HeadlessProjectExtension;
import consulo.it.HeadlessProjects;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowFactory;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.sandboxPlugin.ide.module.extension.SandMutableModuleExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Path;

import static consulo.it.index.ScanningTestSupport.waitFor;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(HeadlessProjectExtension.class)
public class EndpointToolWindowAvailabilityTest {
    @Test
    public void toolWindowFollowsSandModuleExtension(HeadlessProjects projects) throws Exception {
        Path directory = projects.newDirectory();
        Project project = projects.open(directory);
        Module module = HeadlessModules.createModule(project, "main", directory);
        HeadlessProjects.awaitIdle(project);

        ToolWindowFactory factory = findFactory(project);

        assertThat(isRegistered(project))
            .as("no sand module extension: the Endpoints tool window is not registered")
            .isFalse();
        assertThat(factory.validate(project)).isFalse();
        assertThat(EndpointProvider.getAvailableProviders(project)).isEmpty();

        setSandEnabled(module, true);

        waitFor(
            "enabling the sand module extension must register the Endpoints tool window",
            () -> isRegistered(project),
            () -> describe(project, factory)
        );
        assertThat(factory.validate(project)).isTrue();
        assertThat(EndpointProvider.getAvailableProviders(project)).isNotEmpty();

        setSandEnabled(module, false);

        waitFor(
            "disabling the sand module extension must unregister the Endpoints tool window",
            () -> !isRegistered(project),
            () -> describe(project, factory)
        );
        assertThat(factory.validate(project)).isFalse();
        assertThat(EndpointProvider.getAvailableProviders(project)).isEmpty();
    }

    private static ToolWindowFactory findFactory(Project project) {
        ToolWindowFactory factory = project.getApplication()
            .getExtensionPoint(ToolWindowFactory.class)
            .findFirstSafe(it -> EndpointViewOpener.ENDPOINTS_TOOLWINDOW_ID.equals(it.getId()));
        assertThat(factory).as("Endpoints tool window factory").isNotNull();
        return factory;
    }

    private static boolean isRegistered(Project project) {
        return project.getUIAccess().giveAndWaitIfNeed(
            () -> ToolWindowManager.getInstance(project).getToolWindow(EndpointViewOpener.ENDPOINTS_TOOLWINDOW_ID) != null
        );
    }

    private static void setSandEnabled(Module module, boolean enabled) {
        WriteAction.run(() -> {
            ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
            SandMutableModuleExtension extension = rootModel.getExtensionWithoutCheck(SandMutableModuleExtension.class);
            assertThat(extension).isNotNull();
            extension.setEnabled(enabled);
            rootModel.commit();
        });
    }

    private static String describe(Project project, ToolWindowFactory factory) {
        return "registered=" + isRegistered(project)
            + " validate=" + factory.validate(project)
            + " available=" + EndpointProvider.getAvailableProviders(project).size();
    }
}
