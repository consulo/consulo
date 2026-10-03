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

import consulo.application.ReadAction;
import consulo.application.WriteAction;
import consulo.application.dumb.IndexNotReadyException;
import consulo.endpoint.EndpointFilter;
import consulo.endpoint.EndpointProvider;
import consulo.endpoint.ModuleEndpointFilter;
import consulo.endpoint.SearchScopeEndpointFilter;
import consulo.it.HeadlessModules;
import consulo.it.HeadlessProjectExtension;
import consulo.it.HeadlessProjects;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.project.Project;
import consulo.sandboxPlugin.ide.endpoint.SandEndpointProvider;
import consulo.sandboxPlugin.ide.module.extension.SandMutableModuleExtension;
import consulo.sandboxPlugin.lang.psi.SandClass;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static consulo.it.index.ScanningTestSupport.waitFor;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(HeadlessProjectExtension.class)
public class EndpointModuleExtensionFilterTest {
    @Test
    public void endpointsComeOnlyFromModulesWithSandExtension(HeadlessProjects projects) throws Exception {
        Path directory = projects.newDirectory();
        Path server = directory.resolve("server");
        Path plain = directory.resolve("plain");
        Files.createDirectories(server);
        Files.createDirectories(plain);
        Files.writeString(server.resolve("ServerApi.sand"), "class ServerApi { \"GET /server\" }");
        Files.writeString(plain.resolve("PlainApi.sand"), "class PlainApi { \"GET /plain\" }");

        Project project = projects.open(directory);
        Module serverModule = HeadlessModules.createModule(project, "server", findFile(server));
        Module plainModule = HeadlessModules.createModule(project, "plain", findFile(plain));
        setSandEnabled(serverModule);
        HeadlessProjects.awaitIdle(project);

        EndpointProvider<?, ?> provider = project.getExtensionPoint(EndpointProvider.class)
            .findFirstSafe(it -> it instanceof SandEndpointProvider);
        assertThat(provider).as("sand endpoint provider").isNotNull();

        ModuleEndpointFilter serverFilter = new ModuleEndpointFilter(serverModule, false, false);
        ModuleEndpointFilter plainFilter = new ModuleEndpointFilter(plainModule, false, false);
        ModuleEndpointFilter plainWithLibrariesFilter = new ModuleEndpointFilter(plainModule, true, true);
        SearchScopeEndpointFilter projectFilter = new SearchScopeEndpointFilter() {
            @Override
            public GlobalSearchScope getContentSearchScope() {
                return GlobalSearchScope.projectScope(project);
            }

            @Override
            public GlobalSearchScope getTransitiveSearchScope() {
                return GlobalSearchScope.projectScope(project);
            }
        };

        waitFor(
            "the sand module lists its endpoints",
            () -> List.of("ServerApi").equals(groupNames(provider, serverFilter)),
            () -> "server=" + groupNames(provider, serverFilter)
        );

        assertThat(groupNames(provider, plainFilter))
            .as("a module without the sand extension lists no endpoints")
            .isEmpty();
        assertThat(groupNames(provider, plainWithLibrariesFilter))
            .as("a module without the sand extension lists no endpoints, even with libraries and tests")
            .isEmpty();
        assertThat(groupNames(provider, projectFilter))
            .as("a project-wide search skips files of modules without the sand extension")
            .containsExactly("ServerApi");
    }

    private static VirtualFile findFile(Path path) {
        VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path);
        assertThat(file).as(path.toString()).isNotNull();
        return file;
    }

    private static void setSandEnabled(Module module) {
        WriteAction.run(() -> {
            ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
            SandMutableModuleExtension extension = rootModel.getExtensionWithoutCheck(SandMutableModuleExtension.class);
            assertThat(extension).isNotNull();
            extension.setEnabled(true);
            rootModel.commit();
        });
    }

    @SuppressWarnings("unchecked")
    private static List<String> groupNames(EndpointProvider<?, ?> provider, EndpointFilter filter) {
        return ReadAction.compute(() -> {
            try {
                List<String> names = new ArrayList<>();
                for (Object group : ((EndpointProvider<Object, ?>) provider).getEndpointGroups(filter)) {
                    names.add(((SandClass) group).getName());
                }
                return names;
            }
            catch (IndexNotReadyException e) {
                return List.of();
            }
        });
    }
}
