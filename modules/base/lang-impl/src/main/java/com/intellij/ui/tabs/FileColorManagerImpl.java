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

package com.intellij.ui.tabs;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.FileColorManager;
import com.intellij.ui.JBColor;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.UIUtil;
import jakarta.inject.Singleton;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jakarta.inject.Inject;

import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * @author spleaner
 * @author Konstantin Bulenkov
 */
@Singleton
@State(name = "FileColors", storages = @Storage(file = StoragePathMacros.WORKSPACE_FILE))
public class FileColorManagerImpl extends FileColorManager implements PersistentStateComponent<Element> {
  public static final String FC_ENABLED = "FileColorsEnabled";
  public static final String FC_TABS_ENABLED = "FileColorsForTabsEnabled";
  public static final String FC_PROJECT_VIEW_ENABLED = "FileColorsForProjectViewEnabled";
  private final Project myProject;
  private final FileColorsModel myModel;
  private FileColorSharedConfigurationManager mySharedConfigurationManager;

  private static final Map<String, Color> ourDefaultColors = ContainerUtil.<String, Color>immutableMapBuilder()
    .put("Blue", JBColor.namedColor("FileColor.Blue", new JBColor(0xeaf6ff, 0x4f556b)))
    .put("Green", JBColor.namedColor("FileColor.Green", new JBColor(0xeffae7, 0x49544a)))
    .put("Orange", JBColor.namedColor("FileColor.Orange", new JBColor(0xf6e9dc, 0x806052)))
    .put("Rose", JBColor.namedColor("FileColor.Rose", new JBColor(0xf2dcda, 0x6e535b)))
    .put("Violet", JBColor.namedColor("FileColor.Violet", new JBColor(0xe6e0f1, 0x534a57)))
    .put("Yellow", JBColor.namedColor("FileColor.Yellow", new JBColor(0xffffe4, 0x4f4b41)))
    .build();

  @Inject
  public FileColorManagerImpl(@Nonnull final Project project) {
    myProject = project;
    myModel = new FileColorsModel(project);
  }

  private void initProjectLevelConfigurations() {
    if (mySharedConfigurationManager == null) {
      mySharedConfigurationManager = ServiceManager.getService(myProject, FileColorSharedConfigurationManager.class);
    }
  }

  @Override
  public boolean isEnabled() {
    return _isEnabled();
  }

  public static boolean _isEnabled() {
    return PropertiesComponent.getInstance().getBoolean(FC_ENABLED, true);
  }

  @Override
  public void setEnabled(boolean enabled) {
    PropertiesComponent.getInstance().setValue(FC_ENABLED, Boolean.toString(enabled));
  }

  public void setEnabledForTabs(boolean enabled) {
    PropertiesComponent.getInstance().setValue(FC_TABS_ENABLED, Boolean.toString(enabled));
  }

  @Override
  public boolean isEnabledForTabs() {
    return _isEnabledForTabs();
  }

  public static boolean _isEnabledForTabs() {
    return PropertiesComponent.getInstance().getBoolean(FC_TABS_ENABLED, true);
  }

  @Override
  public boolean isEnabledForProjectView() {
    return _isEnabledForProjectView();
  }

  public static boolean _isEnabledForProjectView() {
    return PropertiesComponent.getInstance().getBoolean(FC_PROJECT_VIEW_ENABLED, true);
  }

  public static void setEnabledForProjectView(boolean enabled) {
    PropertiesComponent.getInstance().setValue(FC_PROJECT_VIEW_ENABLED, Boolean.toString(enabled));
  }

  public Element getState(final boolean isProjectLevel) {
    return myModel.save(isProjectLevel);
  }

  @Override
  @Nullable
  public Color getColor(@Nonnull final String name) {
    final Color color = ourDefaultColors.get(name);
    return color == null ? ColorUtil.fromHex(name, null) : color;
  }

  @Override
  public Element getState() {
    initProjectLevelConfigurations();
    return getState(false);
  }

  void loadState(Element state, final boolean isProjectLevel) {
    myModel.load(state, isProjectLevel);
  }

  @Override
  public Collection<String> getColorNames() {
    final Set<String> names = ourDefaultColors.keySet();
    final List<String> sorted = new ArrayList<String>(names);
    Collections.sort(sorted);
    return sorted;
  }

  @Override
  public void loadState(Element state) {
    initProjectLevelConfigurations();
    loadState(state, false);
  }

  @Override
  public boolean isColored(@Nonnull final String scopeName, final boolean shared) {
    return myModel.isColored(scopeName, shared);
  }

  @Nullable
  @Override
  public Color getRendererBackground(VirtualFile file) {
    return getRendererBackground(PsiManager.getInstance(myProject).findFile(file));
  }

  @Nullable
  @Override
  public Color getRendererBackground(PsiFile file) {
    if (file == null) return null;

    if (isEnabled()) {
      final Color fileColor = getFileColor(file);
      if (fileColor != null) return fileColor;
    }

    //return FileEditorManager.getInstance(myProject).isFileOpen(vFile) && !UIUtil.isUnderDarcula() ? LightColors.SLIGHTLY_GREEN : null;
    return null;
  }

  @Override
  @Nullable
  public Color getFileColor(@Nonnull final PsiFile file) {
    initProjectLevelConfigurations();

    final String colorName = myModel.getColor(file);
    return colorName == null ? null : getColor(colorName);
  }

  @Override
  @Nullable
  public Color getFileColor(@Nonnull final VirtualFile file) {
    initProjectLevelConfigurations();
    if (!file.isValid()) {
      return null;
    }
    final PsiFile psiFile = PsiManager.getInstance(getProject()).findFile(file);
    if (psiFile != null) {
      return getFileColor(psiFile);
    }
    else {
      final String colorName = myModel.getColor(file, getProject());
      return colorName == null ? null : getColor(colorName);
    }
  }

  @Override
  @Nullable
  public Color getScopeColor(@Nonnull String scopeName) {
    initProjectLevelConfigurations();

    final String colorName = myModel.getScopeColor(scopeName, getProject());
    return colorName == null ? null : getColor(colorName);
  }

  @Override
  public boolean isShared(@Nonnull final String scopeName) {
    return myModel.isProjectLevel(scopeName);
  }

  FileColorsModel getModel() {
    return myModel;
  }

  boolean isShared(FileColorConfiguration configuration) {
    return myModel.isProjectLevel(configuration);
  }

  @Override
  public Project getProject() {
    return myProject;
  }

  public List<FileColorConfiguration> getLocalConfigurations() {
    return myModel.getProjectLevelConfigurations();
  }

  public List<FileColorConfiguration> getSharedConfigurations() {
    return myModel.getLocalConfigurations();
  }

  @Nullable
  public static String getColorName(Color color) {
    for (String name : ourDefaultColors.keySet()) {
      if (color.equals(ourDefaultColors.get(name))) {
        return name;
      }
    }
    return null;
  }

  @Nullable
  private static FileColorConfiguration findConfigurationByName(String name, List<FileColorConfiguration> configurations) {
    for (FileColorConfiguration configuration : configurations) {
      if (name.equals(configuration.getScopeName())) {
        return configuration;
      }
    }
    return null;
  }

  static String getAlias(String text) {
    if (UIUtil.isUnderDarcula()) {
      if (text.equals("Yellow")) return "Brown";
    }
    return text;
  }

}
