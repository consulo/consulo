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
package consulo.it.execution;

import com.dslplatform.json.DslJson;
import consulo.application.Application;
import consulo.execution.RunManager;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.configuration.PlatformAwareRunProfile;
import consulo.execution.configuration.ProgramParametersAware;
import consulo.execution.impl.internal.configuration.ProjectRunConfigurationManager;
import consulo.it.HeadlessProjectExtension;
import consulo.it.HeadlessProjects;
import consulo.project.Project;
import consulo.util.concurrent.coroutine.CoroutineScope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Shared run configurations stored as {@code .consulo/runConfigurations/<name>.json}: parts whose plugin is not loaded (a fragment,
 * a before-run task, a whole configuration type) are written back as they were read.
 */
@ExtendWith(HeadlessProjectExtension.class)
public class RunConfigurationJsonStorageTest {
    private static final String ECHO = """
        {
          "name": "Echo",
          "type": "#SandExecutableConfigurationType",
          "factoryName": "#SandExecutableConfigurationType",
          "options": {
            "executable": "/bin/printf"
          },
          "fragments": {
            "programParameters": {
              "value": "hi"
            },
            "notLoadedFragment": {
              "paths": [".env", ".env.local"],
              "enabled": true,
              "depth": 3
            }
          },
          "beforeRun": [
            {
              "id": "RunConfigurationTask",
              "state": {
                "enabled": true,
                "configurationType": "#SandConfigurationType",
                "configurationName": "Other"
              }
            },
            {
              "id": "NotLoadedTask",
              "state": {
                "enabled": true,
                "goals": ["clean", "install"],
                "nested": {
                  "a": [1, 2]
                }
              }
            }
          ]
        }
        """;

    private static final String GHOST = """
        {
          "name": "Ghost",
          "type": "NotLoadedConfigurationType",
          "factoryName": "Ghost",
          "options": {
            "x": 1,
            "nested": {
              "a": [1, 2]
            }
          }
        }
        """;

    private final DslJson<Object> myJson = new DslJson<>();

    @Test
    public void unknownFragmentAndTaskAreWrittenBackAsTheyWere(Application application, HeadlessProjects projects) throws Exception {
        Path directory = projects.newDirectory();
        Path echo = writeConfiguration(directory, "Echo.json", ECHO);

        Project project = open(projects, directory);
        RunnerAndConfigurationSettings settings = RunManager.getInstance(project).findConfigurationByName("Echo");
        assertThat(settings).isNotNull();
        assertThat(((ProgramParametersAware) settings.getConfiguration()).getProgramParameters()).isEqualTo("hi");

        ((ProgramParametersAware) settings.getConfiguration()).setProgramParameters("changed");
        save(project, application);

        Map<String, Object> expected = parse(ECHO);
        fragment(expected, "programParameters").put("value", "changed");
        assertThat(parse(Files.readString(echo))).isEqualTo(expected);
        assertThat(echo.resolveSibling("Echo.xml")).doesNotExist();
    }

    @Test
    public void untouchedFilesAreNotRewritten(Application application, HeadlessProjects projects) throws Exception {
        Path directory = projects.newDirectory();
        Path echo = writeConfiguration(directory, "Echo.json", ECHO);
        Path ghost = writeConfiguration(directory, "Ghost.json", GHOST);

        Project project = open(projects, directory);
        save(project, application);

        assertThat(Files.readString(echo)).isEqualTo(ECHO);
        assertThat(Files.readString(ghost)).isEqualTo(GHOST);
    }

    @Test
    public void unknownConfigurationTypeSurvivesSavingOthers(Application application, HeadlessProjects projects) throws Exception {
        Path directory = projects.newDirectory();
        Path echo = writeConfiguration(directory, "Echo.json", ECHO);
        Path ghost = writeConfiguration(directory, "Ghost.json", GHOST);

        Project project = open(projects, directory);
        RunnerAndConfigurationSettings settings = RunManager.getInstance(project).findConfigurationByName("Echo");
        assertThat(settings).isNotNull();
        ((ProgramParametersAware) settings.getConfiguration()).setProgramParameters("changed");
        save(project, application);

        assertThat(parse(Files.readString(echo))).isNotEqualTo(parse(ECHO));
        assertThat(Files.readString(ghost)).isEqualTo(GHOST);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void platformTargetIsAFragment(Application application, HeadlessProjects projects) throws Exception {
        Path directory = projects.newDirectory();
        Path echo = writeConfiguration(directory, "Echo.json", ECHO);

        Project project = open(projects, directory);
        RunnerAndConfigurationSettings settings = RunManager.getInstance(project).findConfigurationByName("Echo");
        assertThat(settings).isNotNull();
        ((PlatformAwareRunProfile) settings.getConfiguration()).setPlatformTargetId("remote-1");
        save(project, application);

        Map<String, Object> expected = parse(ECHO);
        ((Map<String, Object>) expected.get("fragments")).put("platformTarget", Map.of("targetId", "remote-1"));
        assertThat(parse(Files.readString(echo))).isEqualTo(expected);
    }

    private static Path writeConfiguration(Path directory, String fileName, String content) throws Exception {
        Path folder = directory.resolve(Project.DIRECTORY_STORE_FOLDER).resolve("runConfigurations");
        Files.createDirectories(folder);
        Path file = folder.resolve(fileName);
        Files.writeString(file, content);
        return file;
    }

    private static Project open(HeadlessProjects projects, Path directory) throws Exception {
        Project project = projects.open(directory);
        project.getInstance(ProjectRunConfigurationManager.class);
        HeadlessProjects.awaitIdle(project);
        return project;
    }

    private static void save(Project project, Application application) throws Exception {
        project.saveAsync(application.getLastUIAccess())
            .runAsync(CoroutineScope.of(project.coroutineContext()), null)
            .toFuture()
            .get(HeadlessProjects.TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parse(String json) throws Exception {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        return (Map<String, Object>) myJson.deserialize(Map.class, bytes, bytes.length);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> fragment(Map<String, Object> configuration, String id) {
        return (Map<String, Object>) ((Map<String, Object>) configuration.get("fragments")).get(id);
    }
}
