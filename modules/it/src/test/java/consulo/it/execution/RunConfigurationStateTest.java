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
import consulo.execution.configuration.LegacyRunConfigurationConverter;
import consulo.execution.configuration.RunConfigurationBase;
import consulo.execution.configuration.fragment.DslJsonConfiguration;
import consulo.execution.configuration.fragment.EnvironmentFragmentState;
import consulo.execution.configuration.fragment.WorkingDirectoryFragmentState;
import consulo.it.HeadlessApplicationExtension;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.sandboxPlugin.ide.coverage.SandCoverageFilterState;
import consulo.sandboxPlugin.ide.run.SandConfiguration;
import consulo.sandboxPlugin.ide.run.SandConfigurationType;
import consulo.util.jdom.JDOMUtil;
import org.jdom.Element;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(HeadlessApplicationExtension.class)
public class RunConfigurationStateTest {
    private static final String LEGACY = """
        <configuration name="App" type="#TestRunConfigurationType" factoryName="#TestRunConfigurationType">
          <option name="MAIN_CLASS_NAME" value="demo.Main" />
          <option name="PROGRAM_PARAMETERS" value="a b" />
          <option name="WORKING_DIRECTORY" value="file:///work/dir" />
          <option name="VERBOSE" value="true" />
          <option name="PASS_PARENT_ENVS" value="false" />
          <envs>
            <env name="A" value="1" />
          </envs>
          <method v="2" />
        </configuration>""";

    private static final String MIGRATED = """
        <configuration format="2">
          <options mainClass="demo.Main" verbose="true" />
          <fragment id="environment" passParentEnvs="false">
            <env name="A" value="1" />
          </fragment>
          <fragment id="programParameters" value="a b" />
          <fragment id="workingDirectory" path="/work/dir" />
        </configuration>""";

    @Test
    public void legacyXmlIsConvertedIntoTheNewState(Application application) throws Exception {
        TestRunConfiguration configuration = create(application);
        configuration.readExternal(load(LEGACY));

        assertThat(configuration.getOptions().getMainClass()).isEqualTo("demo.Main");
        assertThat(configuration.getOptions().isVerbose()).isTrue();
        assertThat(configuration.getProgramParameters()).isEqualTo("a b");
        assertThat(configuration.getWorkingDirectory()).isEqualTo("/work/dir");
        assertThat(configuration.getEnvs()).containsExactly(Map.entry("A", "1"));
        assertThat(configuration.isPassParentEnvs()).isFalse();

        assertThat(write(configuration)).isEqualTo(normalize(MIGRATED));
    }

    @Test
    public void newStateRoundTripsUnchanged(Application application) throws Exception {
        TestRunConfiguration configuration = create(application);
        configuration.readExternal(load(MIGRATED));

        assertThat(write(configuration)).isEqualTo(normalize(MIGRATED));
        assertThat(configuration.getWorkingDirectory()).isEqualTo("/work/dir");
    }

    @Test
    public void defaultsAreNotWritten(Application application) throws Exception {
        TestRunConfiguration configuration = create(application);
        configuration.setPassParentEnvs(true);
        configuration.setProgramParameters("");

        assertThat(write(configuration)).isEqualTo("<configuration format=\"2\" />");
    }

    @Test
    public void unknownFragmentIsKeptVerbatim(Application application) throws Exception {
        String xml = """
            <configuration format="2">
              <fragment id="programParameters" value="x" />
              <fragment id="notLoadedPlugin" answer="42">
                <item value="kept" />
              </fragment>
            </configuration>""";

        TestRunConfiguration configuration = create(application);
        configuration.readExternal(load(xml));

        assertThat(write(configuration)).isEqualTo(normalize(xml));
    }

    @Test
    public void cloneCopiesTheState(Application application) throws Exception {
        TestRunConfiguration configuration = create(application);
        configuration.readExternal(load(LEGACY));

        TestRunConfiguration copy = (TestRunConfiguration) configuration.clone();
        copy.setProgramParameters("changed");
        copy.getOptions().setMainClass("demo.Other");
        copy.getEnvs().put("B", "2");

        assertThat(configuration.getProgramParameters()).isEqualTo("a b");
        assertThat(configuration.getOptions().getMainClass()).isEqualTo("demo.Main");
        assertThat(configuration.getEnvs()).containsOnlyKeys("A");
        assertThat(copy.getProgramParameters()).isEqualTo("changed");
    }

    @Test
    public void legacyEnvShapesAreRead() throws Exception {
        assertThat(new LegacyRunConfigurationConverter(load("""
            <configuration><envs><env name="A" value="1" /></envs></configuration>""")).readEnvs())
            .containsExactly(Map.entry("A", "1"));

        assertThat(new LegacyRunConfigurationConverter(load("""
            <configuration><env name="A" value="1" /><env name="B" /></configuration>""")).readEnvs())
            .containsExactly(Map.entry("A", "1"), Map.entry("B", ""));

        assertThat(new LegacyRunConfigurationConverter(load("""
            <configuration><option name="ENV_VARIABLES" value="A=1;B=x=y" /></configuration>""")).readEnvs())
            .containsExactly(Map.entry("A", "1"), Map.entry("B", "x=y"));
    }

    @Test
    public void workingDirectoryUrlBecomesPath() {
        assertThat(new WorkingDirectoryFragmentState("file://$PROJECT_DIR$/src").getPath()).isEqualTo("$PROJECT_DIR$/src");
        assertThat(new WorkingDirectoryFragmentState("C:\\work\\dir").getPath()).isEqualTo("C:/work/dir");
        assertThat(new WorkingDirectoryFragmentState("  ").getPath()).isNull();
    }

    @Test
    public void fragmentStateRoundTripsThroughJson() throws Exception {
        DslJson<Object> json = new DslJson<>(new DslJson.Settings<Object>().with(new DslJsonConfiguration()));

        EnvironmentFragmentState state = new EnvironmentFragmentState(Map.of("A", "1"), false);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        json.serialize(state, out);
        assertThat(out.toString(StandardCharsets.UTF_8)).contains("\"envs\":{\"A\":\"1\"}", "\"passParentEnvs\":false");

        byte[] bytes = out.toByteArray();
        EnvironmentFragmentState read = json.deserialize(EnvironmentFragmentState.class, bytes, bytes.length);
        assertThat(read.getEnvs()).containsExactly(Map.entry("A", "1"));
        assertThat(read.isPassParentEnvs()).isFalse();
    }

    @Test
    public void unknownExtensionSettingsAreKept(Application application) throws Exception {
        TestRunConfiguration configuration = create(application);
        configuration.readExternal(load("""
            <configuration name="App" type="#TestRunConfigurationType" factoryName="#TestRunConfigurationType">
              <option name="MAIN_CLASS_NAME" value="demo.Main" />
              <extension name="notLoadedExtension" answer="42">
                <pattern value="demo.*" />
              </extension>
            </configuration>"""));

        String expected = normalize("""
            <configuration format="2">
              <options mainClass="demo.Main" />
              <legacyExtensions>
                <extension name="notLoadedExtension" answer="42">
                  <pattern value="demo.*" />
                </extension>
              </legacyExtensions>
            </configuration>""");
        assertThat(write(configuration)).isEqualTo(expected);

        TestRunConfiguration reloaded = create(application);
        reloaded.readExternal(load(expected));
        assertThat(write(reloaded)).isEqualTo(expected);
    }

    @Test
    public void extensionSettingsAreConvertedByTheirFragment(Application application) throws Exception {
        SandConfiguration configuration = createSand(application);
        configuration.readExternal(load("""
            <configuration name="App" type="#SandConfigurationType" factoryName="#SandConfigurationType">
              <extension name="sandCoverage" include="main*" />
              <extension name="notLoadedExtension" answer="42" />
            </configuration>"""));

        assertThat(configuration.getFragmentState(SandCoverageFilterState.class).getIncludePattern()).isEqualTo("main*");
        assertThat(write(configuration))
            .contains("<fragment id=\"coverage.sand.include\" includePattern=\"main*\" />")
            .contains("<extension name=\"notLoadedExtension\" answer=\"42\" />")
            .doesNotContain("sandCoverage");
    }

    @Test
    public void keptExtensionSettingsAreConvertedOnceTheirFragmentIsThere(Application application) throws Exception {
        SandConfiguration configuration = createSand(application);
        configuration.readExternal(load("""
            <configuration format="2">
              <legacyExtensions>
                <extension name="sandCoverage" include="late*" />
              </legacyExtensions>
            </configuration>"""));

        assertThat(configuration.getFragmentState(SandCoverageFilterState.class).getIncludePattern()).isEqualTo("late*");
        assertThat(write(configuration))
            .contains("<fragment id=\"coverage.sand.include\" includePattern=\"late*\" />")
            .doesNotContain("legacyExtensions");
    }

    private static SandConfiguration createSand(Application application) {
        Project project = application.getInstance(ProjectManager.class).getDefaultProject();
        return new SandConfiguration(project, new SandConfigurationType(), "App");
    }

    private static String write(RunConfigurationBase configuration) throws Exception {
        Element element = new Element("configuration");
        configuration.writeExternal(element);
        return JDOMUtil.writeElement(element, "\n");
    }

    private static TestRunConfiguration create(Application application) {
        Project project = application.getInstance(ProjectManager.class).getDefaultProject();
        return new TestRunConfiguration(project, new TestRunConfigurationType(), "App");
    }

    private static String normalize(String xml) throws Exception {
        return JDOMUtil.writeElement(load(xml), "\n");
    }

    private static Element load(String xml) throws Exception {
        return JDOMUtil.loadDocument(xml).getRootElement();
    }
}
