/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.ui.tabs;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packageDependencies.DefaultScopesProvider;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.scope.NonProjectFilesScope;
import com.intellij.psi.search.scope.TestsScope;
import com.intellij.psi.search.scope.packageSet.*;
import com.intellij.ui.ColorUtil;
import com.intellij.util.containers.ContainerUtil;
import consulo.psi.search.scope.TestResourcesScope;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * @author spleaner
 * @author Konstantin Bulenkov
 */
// todo[spL]: listen to scope rename
public class FileColorsModel implements Cloneable {
  public static final String FILE_COLOR = "fileColor";

  private final List<FileColorConfiguration> myApplicationLevelConfigurations;
  private final List<FileColorConfiguration> myProjectLevelConfigurations;
  private static final Map<String, String> globalScopes;
  private static Map<String, String> globalScopesColors;
  static {
    globalScopes = new LinkedHashMap<>(4);
    globalScopes.put(NonProjectFilesScope.NAME, "file.colors.enable.non.project");
    globalScopes.put(TestsScope.NAME, "file.colors.enable.tests");
    globalScopes.put(TestResourcesScope.NAME, "file.colors.enable.tests");
    //globalScopes.put(ResourcesScope.NAME, "file.colors.enable.sources");
    //globalScopes.put(SourcesScope.NAME, "file.colors.enable.sources");

    globalScopesColors = new LinkedHashMap<>(4);
  }

  private final Project myProject;

  FileColorsModel(@NotNull final Project project) {
    myProject = project;
    myProjectLevelConfigurations = new ArrayList<>();
    myApplicationLevelConfigurations = new ArrayList<>();

    if (globalScopesColors.size() < globalScopes.size()) {
      final DefaultScopesProvider defaultScopesProvider = DefaultScopesProvider.getInstance(project);
      for (String scopeName : globalScopes.keySet()) {
        final NamedScope scope = defaultScopesProvider.findCustomScope(scopeName);
        assert scope != null : "There is no custom scope with name " + scopeName;
        final Color color = ColorUtil.getColor(scope.getClass());
        assert color != null : scope.getClass().getName() + " is not annotated with @Colored";
        final String colorName = FileColorManagerImpl.getColorName(color);
        globalScopesColors.put(scopeName, colorName == null ? ColorUtil.toHex(color) : colorName);
      }
    }
    initGlobalScopes();
  }

  private FileColorsModel(@NotNull final Project project,
                          @NotNull final List<FileColorConfiguration> regular,
                          @NotNull final List<FileColorConfiguration> shared) {
    myProject = project;
    myProjectLevelConfigurations = new ArrayList<>();
    myApplicationLevelConfigurations = new ArrayList<>();
    myProjectLevelConfigurations.addAll(regular);
    myApplicationLevelConfigurations.addAll(shared);
    initGlobalScopes();
  }

  private void initGlobalScopes() {
    for (String scopeName : globalScopes.keySet()) {
      if (findConfiguration(scopeName, false) == null) {
        final String color = PropertiesComponent.getInstance().getOrInit(globalScopes.get(scopeName), globalScopesColors.get(scopeName));
        if (color.length() != 0) {
          final Color col = ColorUtil.fromHex(color, null);
          final String name = col == null ? null : FileColorManagerImpl.getColorName(col);
          myProjectLevelConfigurations.add(new FileColorConfiguration(scopeName, name == null ? color : name));
        }
      }
    }
  }

  public void save(final Element e, final boolean shared) {
    final List<FileColorConfiguration> configurations = shared ? myApplicationLevelConfigurations : myProjectLevelConfigurations;
    for (final FileColorConfiguration configuration : configurations) {
      final String name = configuration.getScopeName();
      if (globalScopes.containsKey(name)) {
        PropertiesComponent.getInstance().setValue(name, configuration.getColorName());
      }

      configuration.save(e);
    }
  }

  public void load(final Element e, final boolean shared) {
    List<FileColorConfiguration> configurations = shared ? myApplicationLevelConfigurations : myProjectLevelConfigurations;

    configurations.clear();

    final List<Element> list = e.getChildren(FILE_COLOR);

    for (Element child : list) {
      final FileColorConfiguration configuration = FileColorConfiguration.load(child);
      if (configuration != null) {
          final String name = configuration.getScopeName();
          if (globalScopes.get(name) != null) {
            final PropertiesComponent properties = PropertiesComponent.getInstance();
            final String colorName = properties.getValue(name);
            if (colorName != null) {
              configurations.add(new FileColorConfiguration(name, colorName));
            }
            continue;
          }
        configurations.add(configuration);
      }
    }
  }

  @Override
  public FileColorsModel clone() throws CloneNotSupportedException {
    final List<FileColorConfiguration> regular = new ArrayList<>();
    for (final FileColorConfiguration configuration : myProjectLevelConfigurations) {
      regular.add(configuration.clone());
    }

    final ArrayList<FileColorConfiguration> shared = new ArrayList<>();
    for (final FileColorConfiguration sharedConfiguration : myApplicationLevelConfigurations) {
      shared.add(sharedConfiguration.clone());
    }

    return new FileColorsModel(myProject, regular, shared);
  }

  public void add(@NotNull final FileColorConfiguration configuration, boolean shared) {
    final List<FileColorConfiguration> configurations = shared ? myApplicationLevelConfigurations : myProjectLevelConfigurations;
    if (!configurations.contains(configuration)) {
      configurations.add(configuration);
    }
  }

  public void add(@NotNull final String scopeName, @NotNull final String colorName, boolean shared) {
    final FileColorConfiguration configuration = new FileColorConfiguration();
    configuration.setScopeName(scopeName);
    configuration.setColorName(colorName);

    add(configuration, shared);
  }

  @Nullable
  private FileColorConfiguration findConfiguration(final String scopeName, final boolean shared) {
    final List<FileColorConfiguration> configurations = shared ? myApplicationLevelConfigurations : myProjectLevelConfigurations;
    for (final FileColorConfiguration configuration : configurations) {
      if (scopeName.equals(configuration.getScopeName())) {
        return configuration;
      }
    }

    return null;
  }

  public boolean isShared(@NotNull final String scopeName) {
    return findConfiguration(scopeName, true) != null;
  }

  @Nullable
  public String getColor(@NotNull PsiFile psiFile) {
    if (!psiFile.isValid()) {
      return null;
    }

    final FileColorConfiguration configuration = findConfiguration(psiFile);
    if (configuration != null && configuration.isValid(psiFile.getProject())) {
      return configuration.getColorName();
    }
    return null;
  }

  @Nullable
  public String getColor(@NotNull VirtualFile file, Project project) {
    if (!file.isValid()) {
      return null;
    }

    final FileColorConfiguration configuration = findConfiguration(file);
    if (configuration != null && configuration.isValid(project)) {
      return configuration.getColorName();
    }
    return null;
  }

  @Nullable
  public String getScopeColor(@NotNull String scopeName, Project project) {
    FileColorConfiguration configuration = null;
    for (FileColorConfiguration each : getConfigurations()) {
      if (scopeName.equals(each.getScopeName())) {
        configuration = each;
        break;
      }
    }
    if (configuration != null && configuration.isValid(project)) {
      return configuration.getColorName();
    }
    return null;
  }

  @Nullable
  private FileColorConfiguration findConfiguration(@NotNull final PsiFile colored) {
    for (final FileColorConfiguration configuration : myProjectLevelConfigurations) {
      final NamedScope scope = NamedScopeManager.getScope(myProject, configuration.getScopeName());
      if (scope != null) {
        final NamedScopesHolder namedScopesHolder = NamedScopeManager.getHolder(myProject, configuration.getScopeName(), null);
        if (scope.getValue() != null && namedScopesHolder != null && scope.getValue().contains(colored, namedScopesHolder)) {
          return configuration;
        }
      }
    }

    for (FileColorConfiguration configuration : myApplicationLevelConfigurations) {
      final NamedScope scope = NamedScopeManager.getScope(myProject, configuration.getScopeName());
      if (scope != null) {
        final NamedScopesHolder namedScopesHolder = NamedScopeManager.getHolder(myProject, configuration.getScopeName(), null);
        if (scope.getValue() != null && namedScopesHolder != null && scope.getValue().contains(colored, namedScopesHolder)) {
          return configuration;
        }
      }
    }

    return null;
  }

  @Nullable
  private FileColorConfiguration findConfiguration(@NotNull final VirtualFile colored) {
    for (final FileColorConfiguration configuration : myProjectLevelConfigurations) {
      final NamedScope scope = NamedScopesHolder.getScope(myProject, configuration.getScopeName());
      if (scope != null) {
        final NamedScopesHolder namedScopesHolder = NamedScopesHolder.getHolder(myProject, configuration.getScopeName(), null);
        final PackageSet packageSet = scope.getValue();
        if (packageSet instanceof PackageSetBase && namedScopesHolder != null && ((PackageSetBase)packageSet).contains(colored, namedScopesHolder)) {
          return configuration;
        }
      }
    }

    for (FileColorConfiguration configuration : myApplicationLevelConfigurations) {
      final NamedScope scope = NamedScopesHolder.getScope(myProject, configuration.getScopeName());
      if (scope != null) {
        final NamedScopesHolder namedScopesHolder = NamedScopesHolder.getHolder(myProject, configuration.getScopeName(), null);
        final PackageSet packageSet = scope.getValue();
        if (packageSet instanceof PackageSetBase && namedScopesHolder != null && ((PackageSetBase)packageSet).contains(colored, namedScopesHolder)) {
          return configuration;
        }
      }
    }

    return null;
  }


  public boolean isShared(FileColorConfiguration configuration) {
    return myApplicationLevelConfigurations.contains(configuration);
  }

  @NotNull
  private List<FileColorConfiguration> getConfigurations() {
    return ContainerUtil.concat(myApplicationLevelConfigurations, myProjectLevelConfigurations);
  }

  public void setConfigurations(final List<FileColorConfiguration> configurations, final boolean shared) {
    if (shared) {
      myApplicationLevelConfigurations.clear();
      myApplicationLevelConfigurations.addAll(configurations);
    }
    else {
      myProjectLevelConfigurations.clear();
      final HashMap<String, String> global = new HashMap<>(globalScopes);
      for (FileColorConfiguration configuration : configurations) {
        myProjectLevelConfigurations.add(configuration);
        final String name = configuration.getScopeName();
        if (global.containsKey(name)) {
          PropertiesComponent.getInstance().setValue(global.get(name), configuration.getColorName());
          global.remove(name);
        }
      }
      for (String name : global.keySet()) {
        PropertiesComponent.getInstance().setValue(global.get(name), "");
      }
    }
  }

  public boolean isColored(String scopeName, boolean shared) {
    return findConfiguration(scopeName, shared) != null;
  }

  public List<FileColorConfiguration> getProjectLevelConfigurations() {
    return myProjectLevelConfigurations;
  }

  public List<FileColorConfiguration> getLocalConfigurations() {
    return myApplicationLevelConfigurations;
  }
}
