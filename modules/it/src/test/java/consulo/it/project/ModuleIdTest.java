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
import consulo.application.ReadAction;
import consulo.application.WriteAction;
import consulo.it.HeadlessProjectExtension;
import consulo.it.HeadlessProjects;
import consulo.module.ModifiableModuleModel;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.project.Project;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.jdom.JDOMUtil;
import org.jdom.Element;
import org.jdom.filter.ElementFilter;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Module ids are stored in {@code .consulo/modules.xml}, survive a rename and a reopen, and are generated for modules
 * stored without one or with the id of another module.
 *
 * @author VISTALL
 */
@ExtendWith(HeadlessProjectExtension.class)
public class ModuleIdTest {
    private static final long TIMEOUT_SECONDS = 30;

    @Test
    public void idIsStoredAndSurvivesRenameAndReopen(Application application, HeadlessProjects projects) throws Exception {
        Path directory = projects.newDirectory();

        Project first = projects.open(directory);
        ModuleManager firstManager = ModuleManager.getInstance(first);

        UUID requestedId = UUID.nameUUIDFromBytes("maven:org.example:alpha".getBytes(StandardCharsets.UTF_8));
        Module alpha = createModule(firstManager, "alpha", directory.resolve("alpha"), requestedId);
        Module beta = createModule(firstManager, "beta", directory.resolve("beta"), null);

        assertThat(alpha.getId()).isEqualTo(requestedId);
        assertThat(beta.getId()).isNotEqualTo(requestedId);
        UUID betaId = beta.getId();

        WriteAction.run(() -> {
            ModifiableModuleModel model = firstManager.getModifiableModel();
            model.renameModule(beta, "gamma");
            model.commit();
        });

        assertThat(beta.getName()).isEqualTo("gamma");
        assertThat(beta.getId()).isEqualTo(betaId);

        saveProject(first, application);

        assertThat(storedIds(directory)).containsEntry("alpha", requestedId.toString()).containsEntry("gamma", betaId.toString());

        projects.close(first);

        Project second = projects.open(directory);
        ModuleManager secondManager = ModuleManager.getInstance(second);

        assertThat(ReadAction.compute(() -> secondManager.findModuleById(requestedId)))
            .extracting(Module::getName)
            .isEqualTo("alpha");
        assertThat(ReadAction.compute(() -> secondManager.findModuleById(betaId)))
            .extracting(Module::getName)
            .isEqualTo("gamma");
    }

    @Test
    public void missingAndDuplicateIdsAreGeneratedAndStored(Application application, HeadlessProjects projects) throws Exception {
        Path directory = projects.newDirectory();

        Project first = projects.open(directory);
        ModuleManager firstManager = ModuleManager.getInstance(first);

        createModule(firstManager, "alpha", directory.resolve("alpha"), null);
        Module beta = createModule(firstManager, "beta", directory.resolve("beta"), null);
        createModule(firstManager, "gamma", directory.resolve("gamma"), null);
        String betaId = beta.getId().toString();

        saveProject(first, application);
        projects.close(first);

        Path modulesFile = modulesFile(directory);
        editModules(modulesFile, (name, module) -> {
            if (name.equals("alpha")) {
                module.removeAttribute("id");
            }
            else if (name.equals("gamma")) {
                module.setAttribute("id", betaId);
            }
        });
        assertThat(storedIds(directory)).doesNotContainKey("alpha").containsEntry("gamma", betaId);

        Project second = projects.open(directory);
        ModuleManager secondManager = ModuleManager.getInstance(second);

        Map<String, UUID> loadedIds = new HashMap<>();
        for (Module module : secondManager.getModules()) {
            loadedIds.put(module.getName(), module.getId());
        }

        assertThat(loadedIds.get("beta")).hasToString(betaId);
        assertThat(loadedIds.get("gamma")).isNotNull().isNotEqualTo(loadedIds.get("beta"));
        assertThat(loadedIds.get("alpha")).isNotNull();

        saveProject(second, application);

        assertThat(storedIds(directory))
            .containsEntry("alpha", loadedIds.get("alpha").toString())
            .containsEntry("beta", betaId)
            .containsEntry("gamma", loadedIds.get("gamma").toString());
    }

    private static Module createModule(ModuleManager moduleManager, String name, Path dirPath, @Nullable UUID id) throws Exception {
        Files.createDirectories(dirPath);

        return WriteAction.compute(() -> {
            ModifiableModuleModel model = moduleManager.getModifiableModel();
            Module module = model.newModule(name, dirPath.toString(), id);
            model.commit();
            return module;
        });
    }

    private static void saveProject(Project project, Application application) throws Exception {
        project.saveAsync(application.getLastUIAccess())
            .runAsync(CoroutineScope.of(project.coroutineContext()), null)
            .toFuture()
            .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private static Path modulesFile(Path directory) {
        return directory.resolve(Project.DIRECTORY_STORE_FOLDER).resolve("modules.xml");
    }

    private static Map<String, String> storedIds(Path directory) throws Exception {
        Map<String, String> ids = new HashMap<>();
        for (Element module : modules(JDOMUtil.loadDocument(Files.readString(modulesFile(directory))).getRootElement())) {
            String id = module.getAttributeValue("id");
            if (id != null) {
                ids.put(module.getAttributeValue("name"), id);
            }
        }
        return ids;
    }

    private static void editModules(Path modulesFile, BiConsumer<String, Element> editor) throws Exception {
        Element root = JDOMUtil.loadDocument(Files.readString(modulesFile)).getRootElement();
        for (Element module : modules(root)) {
            editor.accept(module.getAttributeValue("name"), module);
        }
        Files.writeString(modulesFile, JDOMUtil.writeElement(root));
    }

    private static List<Element> modules(Element root) {
        Iterator<Element> modules = root.getDescendants(new ElementFilter("modules"));
        assertThat(modules.hasNext()).as("no <modules> element").isTrue();
        return modules.next().getChildren("module");
    }
}
