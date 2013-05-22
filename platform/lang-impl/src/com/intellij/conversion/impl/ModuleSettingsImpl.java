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

package com.intellij.conversion.impl;

import com.intellij.conversion.CannotConvertException;
import com.intellij.conversion.ComponentManagerSettings;
import com.intellij.conversion.ModuleSettings;
import com.intellij.ide.highlighter.ModuleFileType;
import com.intellij.openapi.roots.ContentFolderType;
import com.intellij.openapi.roots.impl.*;
import com.intellij.openapi.roots.impl.libraries.LibraryImpl;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

/**
 * @author nik
 */
public class ModuleSettingsImpl extends ComponentManagerSettingsImpl implements ModuleSettings {
  private final String myModuleName;

  public ModuleSettingsImpl(File moduleFile, ConversionContextImpl context) throws CannotConvertException {
    super(moduleFile, context);
    myModuleName = getModuleName(moduleFile);
  }

  public static String getModuleName(File moduleFile) {
    return StringUtil.trimEnd(moduleFile.getName(), ModuleFileType.DOT_DEFAULT_EXTENSION);
  }

  @Override
  @NotNull
  public String getModuleName() {
    return myModuleName;
  }

  @Override
  @NotNull
  public File getModuleFile() {
    return mySettingsFile.getFile();
  }

  @Override
  @NotNull
  public String expandPath(@NotNull String path) {
    return myContext.expandPath(path, this);
  }

  @NotNull
  @Override
  public String collapsePath(@NotNull String path) {
    return myContext.collapsePath(path, this);
  }

  @Override
  @NotNull
  public Collection<File> getSourceRoots(boolean includeTests) {
    final List<File> result = new ArrayList<File>();
    for (Element contentRoot : getContentRootElements()) {
      for (Element sourceFolder : contentRoot.getChildren(ContentFolderImpl.ELEMENT_NAME)) {
        ContentFolderType contentFolderType = ContentFolderType.valueOf(sourceFolder.getAttributeValue(ContentFolderImpl.TYPE_ATTRIBUTE));
        if (includeTests && (contentFolderType == ContentFolderType.SOURCE || contentFolderType == ContentFolderType.TEST) ||
            !includeTests && contentFolderType == ContentFolderType.SOURCE) {
          result.add(getFile(sourceFolder.getAttributeValue(ContentFolderImpl.URL_ATTRIBUTE)));
        }
      }
    }
    return result;
  }

  private List<Element> getContentRootElements() {
    return JDOMUtil.getChildren(getComponentElement(MODULE_ROOT_MANAGER_COMPONENT), ContentEntryImpl.ELEMENT_NAME);
  }

  @Override
  @NotNull
  public Collection<File> getContentRoots() {
    final List<File> result = new ArrayList<File>();
    for (Element contentRoot : getContentRootElements()) {
      String path = VfsUtil.urlToPath(contentRoot.getAttributeValue(ContentEntryImpl.URL_ATTRIBUTE));
      result.add(new File(FileUtil.toSystemDependentName(expandPath(path))));
    }
    return result;
  }

  @Override
  @Nullable
  public String getProjectOutputUrl() {
    final ComponentManagerSettings rootManagerSettings = myContext.getProjectRootManagerSettings();
    final Element projectRootManager = rootManagerSettings == null ? null : rootManagerSettings.getComponentElement("ProjectRootManager");
    final Element outputElement = projectRootManager == null ? null : projectRootManager.getChild("output");
    return outputElement == null ? null : outputElement.getAttributeValue("url");
  }

  @Override
  public void addExcludedFolder(@NotNull File directory) {
    final ComponentManagerSettings rootManagerSettings = myContext.getProjectRootManagerSettings();
    if (rootManagerSettings != null) {
      final Element projectRootManager = rootManagerSettings.getComponentElement("ProjectRootManager");
      if (projectRootManager != null) {
        final Element outputElement = projectRootManager.getChild("output");
        if (outputElement != null) {
          final String outputUrl = outputElement.getAttributeValue("url");
          if (outputUrl != null) {
            final File outputFile = getFile(outputUrl);
            if (FileUtil.isAncestor(outputFile, directory, false)) {
              return;
            }
          }
        }
      }
    }
    for (Element contentRoot : getContentRootElements()) {
      final File root = getFile(contentRoot.getAttributeValue(ContentEntryImpl.URL_ATTRIBUTE));
      if (FileUtil.isAncestor(root, directory, true)) {
        addExcludedFolder(directory, contentRoot);
      }
    }
  }

  @Override
  @NotNull
  public List<File> getModuleLibraryRoots(String libraryName) {
    final Element library = findModuleLibraryElement(libraryName);
    return library != null ? myContext.getClassRoots(library, this) : Collections.<File>emptyList();
  }

  @Override
  public boolean hasModuleLibrary(String libraryName) {
    return findModuleLibraryElement(libraryName) != null;
  }

  @Nullable
  private Element findModuleLibraryElement(String libraryName) {
    for (Element element : getOrderEntries()) {
      if (ModuleLibraryOrderEntryImpl.ENTRY_TYPE.equals(element.getAttributeValue(OrderEntryFactory.ORDER_ENTRY_TYPE_ATTR))) {
        final Element library = element.getChild(LibraryImpl.ELEMENT);
        if (library != null && libraryName.equals(library.getAttributeValue(LibraryImpl.LIBRARY_NAME_ATTR))) {
          return library;
        }
      }
    }
    return null;
  }

  @Override
  public List<Element> getOrderEntries() {
    final Element component = getComponentElement(MODULE_ROOT_MANAGER_COMPONENT);
    return JDOMUtil.getChildren(component, OrderEntryFactory.ORDER_ENTRY_ELEMENT_NAME);
  }

  @Override
  @NotNull
  public Collection<ModuleSettings> getAllModuleDependencies() {
    Set<ModuleSettings> dependencies = new HashSet<ModuleSettings>();
    collectDependencies(dependencies);
    return dependencies;
  }

  private void collectDependencies(Set<ModuleSettings> dependencies) {
    if (!dependencies.add(this)) {
      return;
    }

    for (Element element : getOrderEntries()) {
      if (ModuleOrderEntryImpl.ENTRY_TYPE.equals(element.getAttributeValue(OrderEntryFactory.ORDER_ENTRY_TYPE_ATTR))) {
        final String moduleName = element.getAttributeValue(ModuleOrderEntryImpl.MODULE_NAME_ATTR);
        if (moduleName != null) {
          final ModuleSettings moduleSettings = myContext.getModuleSettings(moduleName);
          if (moduleSettings != null) {
            ((ModuleSettingsImpl)moduleSettings).collectDependencies(dependencies);
          }
        }
      }
    }
  }

  private void addExcludedFolder(File directory, Element contentRoot) {
    for (Element excludedFolder : contentRoot.getChildren(ContentFolderImpl.ELEMENT_NAME)) {
      ContentFolderType contentFolderType = ContentFolderType.valueOf(excludedFolder.getAttributeValue(ContentFolderImpl.TYPE_ATTRIBUTE));
      if (contentFolderType != ContentFolderType.EXCLUDED) {
        continue;
      }

      final File excludedDir = getFile(excludedFolder.getAttributeValue(ContentFolderImpl.URL_ATTRIBUTE));
      if (FileUtil.isAncestor(excludedDir, directory, false)) {
        return;
      }
    }
    String path = myContext.collapsePath(FileUtil.toSystemIndependentName(directory.getAbsolutePath()), this);
    contentRoot.addContent(
      new Element(ContentFolderImpl.ELEMENT_NAME).setAttribute(ContentFolderImpl.URL_ATTRIBUTE, VfsUtil.pathToUrl(path))
        .setAttribute(ContentFolderImpl.TYPE_ATTRIBUTE, ContentFolderType.EXCLUDED.name()));
  }

  private File getFile(String url) {
    return new File(FileUtil.toSystemDependentName(expandPath(VfsUtil.urlToPath(url))));
  }
}
