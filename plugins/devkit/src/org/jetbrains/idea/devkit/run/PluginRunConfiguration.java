/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package org.jetbrains.idea.devkit.run;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.JavaSdkType;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.*;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactPointerManager;
import com.intellij.packaging.impl.artifacts.ArtifactUtil;
import org.consulo.sdk.SdkPointerManager;
import org.consulo.sdk.SdkUtil;
import org.consulo.util.pointers.Named;
import org.consulo.util.pointers.NamedPointer;
import org.consulo.util.pointers.NamedPointerManager;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.devkit.DevKitBundle;
import org.jetbrains.idea.devkit.sdk.Sandbox;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

public class PluginRunConfiguration extends RunConfigurationBase implements ModuleRunConfiguration{
  private static final String JAVA_SDK = "java-sdk";
  private static final String CONSULO_SDK = "consulo-sdk";
  private static final String ARTIFACT = "artifact";

  private static final String NAME = "name";

  private NamedPointer<Sdk> myJavaSdkPointer;
  private NamedPointer<Sdk> myConsuloSdkPointer;
  private NamedPointer<Artifact> myArtifactPointer;

  public String VM_PARAMETERS;
  public String PROGRAM_PARAMETERS;

  public PluginRunConfiguration(final Project project, final ConfigurationFactory factory, final String name) {
    super(project, factory, name);
  }

  @Override
  public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
    return new PluginRunConfigurationEditor(getProject());
  }

  @Override
  public JDOMExternalizable createRunnerSettings(ConfigurationInfoProvider provider) {
    return null;
  }

  @Override
  public SettingsEditor<JDOMExternalizable> getRunnerSettingsEditor(ProgramRunner runner) {
    return null;
  }

  @Override
  public RunProfileState getState(@NotNull final Executor executor, @NotNull final ExecutionEnvironment env) throws ExecutionException {
    final Sdk javaSdk = myJavaSdkPointer == null ? null : myJavaSdkPointer.get();
    if (javaSdk == null) {
      throw new ExecutionException(DevKitBundle.message("run.configuration.no.java.sdk"));
    }

    final Sdk consuloSdk = myConsuloSdkPointer == null ? null : myConsuloSdkPointer.get();
    if (consuloSdk == null) {
      throw new ExecutionException(DevKitBundle.message("run.configuration.no.consulo.sdk"));
    }

    final Artifact artifact = myArtifactPointer == null ? null : myArtifactPointer.get();
    if(artifact == null) {
      throw new ExecutionException(DevKitBundle.message("run.configuration.no.plugin.artifact"));
    }

    String sandboxHome = ((Sandbox)consuloSdk.getSdkAdditionalData()).getSandboxHome();

    if (sandboxHome == null) {
      throw new ExecutionException(DevKitBundle.message("sandbox.no.configured"));
    }

    try {
      sandboxHome = new File(sandboxHome).getCanonicalPath();
    }
    catch (IOException e) {
      throw new ExecutionException(DevKitBundle.message("sandbox.no.configured"));
    }
    final String canonicalSandbox = sandboxHome;


    final JavaCommandLineState state = new JavaCommandLineState(env) {
      @Override
      protected JavaParameters createJavaParameters() throws ExecutionException {

        final JavaParameters params = new JavaParameters();

        ParametersList vm = params.getVMParametersList();

        fillParameterList(vm, VM_PARAMETERS);
        fillParameterList(params.getProgramParametersList(), PROGRAM_PARAMETERS);

        @NonNls String libPath = consuloSdk.getHomePath() + File.separator + "lib";
        vm.add("-Xbootclasspath/a:" + libPath + File.separator + "boot.jar");

        vm.defineProperty(PathManager.PROPERTY_CONFIG_PATH, canonicalSandbox + File.separator + "config");
        vm.defineProperty(PathManager.PROPERTY_SYSTEM_PATH, canonicalSandbox + File.separator + "system");
        vm.defineProperty(PathManager.PROPERTY_PLUGINS_PATH, artifact.getOutputPath());

        if (SystemInfo.isMac) {
          vm.defineProperty("idea.smooth.progress", "false");
          vm.defineProperty("apple.laf.useScreenMenuBar", "true");
        }

        if (SystemInfo.isXWindow) {
          if (VM_PARAMETERS == null || !VM_PARAMETERS.contains("-Dsun.awt.disablegrab")) {
            vm.defineProperty("sun.awt.disablegrab", "true"); // See http://devnet.jetbrains.net/docs/DOC-1142
          }

        }
        params.setWorkingDirectory(consuloSdk.getHomePath() + File.separator + "bin" + File.separator);

        params.setJdk(javaSdk);

        params.getClassPath().addFirst(libPath + File.separator + "log4j.jar");
        params.getClassPath().addFirst(libPath + File.separator + "jdom.jar");
        params.getClassPath().addFirst(libPath + File.separator + "trove4j.jar");
        params.getClassPath().addFirst(libPath + File.separator + "util.jar");
        params.getClassPath().addFirst(libPath + File.separator + "extensions.jar");
        params.getClassPath().addFirst(libPath + File.separator + "bootstrap.jar");
        params.getClassPath().addFirst(libPath + File.separator + "idea.jar");
        params.getClassPath().addFirst(((JavaSdkType)javaSdk.getSdkType()).getToolsPath(javaSdk));

        params.setMainClass("com.intellij.idea.Main");

        return params;
      }
    };

    state.setConsoleBuilder(TextConsoleBuilderFactory.getInstance().createBuilder(getProject()));
    return state;
  }

  private static void fillParameterList(ParametersList list, @Nullable String value) {
    if (value == null) return;

    for (String parameter : value.split(" ")) {
      if (parameter != null && parameter.length() > 0) {
        list.add(parameter);
      }
    }
  }

  @Override
  public void checkConfiguration() throws RuntimeConfigurationException {
  }

  @Override
  public void readExternal(Element element) throws InvalidDataException {
    DefaultJDOMExternalizer.readExternal(this, element);

    myJavaSdkPointer = readPointer(JAVA_SDK, element, new NotNullFactory<NamedPointerManager<Sdk>>() {
      @NotNull
      @Override
      public NamedPointerManager<Sdk> create() {
        return ServiceManager.getService(SdkPointerManager.class);
      }
    });

    myConsuloSdkPointer = readPointer(CONSULO_SDK, element, new NotNullFactory<NamedPointerManager<Sdk>>() {
      @NotNull
      @Override
      public NamedPointerManager<Sdk> create() {
        return ServiceManager.getService(SdkPointerManager.class);
      }
    });

    myArtifactPointer = readPointer(ARTIFACT, element, new NotNullFactory<NamedPointerManager<Artifact>>() {
      @NotNull
      @Override
      public NamedPointerManager<Artifact> create() {
        return ArtifactPointerManager.getInstance(getProject());
      }
    });

    super.readExternal(element);
  }

  @Override
  public void writeExternal(Element element) throws WriteExternalException {
    DefaultJDOMExternalizer.writeExternal(this, element);

    writePointer(JAVA_SDK, element, myJavaSdkPointer);
    writePointer(CONSULO_SDK, element, myConsuloSdkPointer);
    writePointer(ARTIFACT, element, myArtifactPointer);

    super.writeExternal(element);
  }

  @Nullable
  private static <T extends Named> NamedPointer<T> readPointer(String childName, Element parent, NotNullFactory<NamedPointerManager<T>> fun) {
    final NamedPointerManager<T> namedPointerManager = fun.create();

    Element child = parent.getChild(childName);
    if(child != null) {
      final String attributeValue = child.getAttributeValue(NAME);
      if(attributeValue != null) {
        return namedPointerManager.create(attributeValue);
      }
    }
    return null;
  }

  private static void writePointer(String childName, Element parent, NamedPointer<?> pointer) {
    if(pointer == null) {
      return;
    }
    Element element = new Element(childName);
    element.setAttribute(NAME, pointer.getName());

    parent.addContent(element);
  }

  @Nullable
  public String getArtifactName() {
    return myArtifactPointer == null ? null : myArtifactPointer.getName();
  }

  public void setArtifactName(@Nullable String name) {
    myArtifactPointer = name == null ? null : ArtifactPointerManager.getInstance(getProject()).create(name);
  }

  @Nullable
  public String getJavaSdkName() {
    return myJavaSdkPointer == null ? null : myJavaSdkPointer.getName();
  }

  public void setJavaSdkName(@Nullable String name) {
    myJavaSdkPointer = name == null ? null :SdkUtil.createPointer(name);
  }

  public void setConsuloSdkName(@Nullable String name) {
    myConsuloSdkPointer = name == null ? null : SdkUtil.createPointer(name);
  }

  @Nullable
  public String getConsuloSdkName() {
    return myConsuloSdkPointer == null ? null : myConsuloSdkPointer.getName();
  }

  @NotNull
  @Override
  public Module[] getModules() {
    Artifact artifact = myArtifactPointer == null ? null : myArtifactPointer.get();
    if(artifact == null) {
      return Module.EMPTY_ARRAY;
    }
    final Set<Module> modules =
      ArtifactUtil.getModulesIncludedInArtifacts(Collections.singletonList(artifact), getProject());

    return modules.isEmpty() ? Module.EMPTY_ARRAY : modules.toArray(new Module[modules.size()]);
  }
}
