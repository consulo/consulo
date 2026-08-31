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
package consulo.it.project;

import consulo.application.Application;
import consulo.application.WriteAction;
import consulo.it.AllowLogError;
import consulo.it.HeadlessApplicationExtension;
import consulo.module.ModifiableModuleModel;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.ProjectOpenContext;
import consulo.project.StoreReloadManager;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.jdom.JDOMUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import org.jdom.Element;
import org.jdom.filter.ElementFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Drives external changes of {@code .consulo/modules.xml} against a real opened project and asserts the module
 * list follows them in place, without the project being reloaded as a whole.
 * <p>
 * The applying half lives in {@code ModuleManagerImpl.afterLoad(false)} - {@code loadState} only parses the file
 * into load items, so without the reload calling {@code afterLoad} the new content would be read and then
 * dropped. Both directions are covered, since adding and removing a module take different branches there.
 * <p>
 * The stored XML is always produced by the platform and only edited by deleting an element, so the test never
 * depends on the exact attribute layout.
 *
 * @author VISTALL
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class ModuleReloadTest {
    private static final long TIMEOUT_SECONDS = 30;

    /**
     * See {@code ProjectStateReloadTest} - the refresh makes the platform fire VFS events on the UI thread,
     * where the pointer manager and the indexing listeners break their own threading assertions in a headless
     * application. Unrelated to the reload under test; any other logged error still fails it.
     */
    @AllowLogError({"consulo.virtualFileSystem.internal.BaseVirtualFileManager", "consulo.application.impl.internal.BaseApplication"})
    @Test
    public void modulesFollowExternalChangesOfModulesXml(Application application, ProjectManager projectManager) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-module-reload");

        Project project = projectManager
            .openProjectAsync(directory, application.getLastUIAccess(), new ProjectOpenContext())
            .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // makes the manager register its VFS listener before anything is written
        StoreReloadManager.getInstance(project);

        ModuleManager moduleManager = ModuleManager.getInstance(project);

        createModule(moduleManager, "alpha", directory.resolve("alpha"));
        createModule(moduleManager, "beta", directory.resolve("beta"));
        saveProject(project, application);

        Path modulesFile = directory.resolve(Project.DIRECTORY_STORE_FOLDER).resolve("modules.xml");
        assertThat(modulesFile).exists();

        String bothModules = Files.readString(modulesFile);
        assertThat(moduleNames(moduleManager)).contains("alpha", "beta");

        // let the VFS cache the current content, otherwise the refresh has nothing to diff against
        VirtualFile modulesVirtualFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(modulesFile);
        assertThat(modulesVirtualFile).isNotNull();

        // an external tool or a VCS update drops a module
        Files.writeString(modulesFile, withoutModule(bothModules, "beta"));
        modulesVirtualFile.refresh(false, false);

        waitFor(() -> !moduleNames(moduleManager).contains("beta"));
        assertThat(moduleNames(moduleManager)).contains("alpha").doesNotContain("beta");

        // and brings it back
        Files.writeString(modulesFile, bothModules);
        modulesVirtualFile.refresh(false, false);

        waitFor(() -> moduleNames(moduleManager).contains("beta"));
        assertThat(moduleNames(moduleManager)).contains("alpha", "beta");

        assertThat(project.isDisposed()).isFalse();
    }

    private static void createModule(ModuleManager moduleManager, String name, Path dirPath) throws Exception {
        Files.createDirectories(dirPath);

        WriteAction.run(() -> {
            ModifiableModuleModel model = moduleManager.getModifiableModel();
            model.newModule(name, dirPath.toString());
            model.commit();
        });
    }

    private static void saveProject(Project project, Application application) throws Exception {
        project.saveAsync(application.getLastUIAccess())
            .runAsync(CoroutineScope.of(project.coroutineContext()), null)
            .toFuture()
            .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private static List<String> moduleNames(ModuleManager moduleManager) {
        return Arrays.stream(moduleManager.getModules()).map(Module::getName).toList();
    }

    /**
     * The stored element is wrapped by the storage, so the modules list is looked up as a descendant rather
     * than at a fixed depth.
     */
    private static String withoutModule(String xml, String name) throws Exception {
        Element root = JDOMUtil.loadDocument(xml).getRootElement();

        Iterator<Element> modules = root.getDescendants(new ElementFilter("modules"));
        assertThat(modules.hasNext()).as("no <modules> element in %s", xml).isTrue();

        boolean removed = modules.next()
            .getChildren("module")
            .removeIf(module -> name.equals(module.getAttributeValue("name")));
        assertThat(removed).as("no module named %s in %s", name, xml).isTrue();

        return JDOMUtil.writeElement(root);
    }

    private static void waitFor(BooleanSupplier condition) throws Exception {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS);
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("Modules did not follow the change within " + TIMEOUT_SECONDS + "s");
    }
}
