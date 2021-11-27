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
package com.intellij.compiler.impl;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.compiler.ex.CompileContextEx;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.roots.ModuleExtensionWithSdkOrderEntry;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Chunk;
import com.intellij.util.PathsList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.OrderedSet;
import consulo.annotation.DeprecationInfo;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.*;

/**
 * @author Eugene Zhuravlev
 * Date: Sep 29, 2004
 */
public class ModuleChunk extends Chunk<Module> {
  private final CompileContextEx myContext;
  private final Map<Module, List<VirtualFile>> myModuleToFilesMap = new HashMap<>();
  private final Map<VirtualFile, VirtualFile> myTransformedToOriginalMap = new HashMap<>();
  private int mySourcesFilter = ALL_SOURCES;

  public ModuleChunk(CompileContextEx context, Chunk<Module> chunk, Map<Module, List<VirtualFile>> moduleToFilesMap) {
    super(chunk.getNodes());
    myContext = context;
    for (final Module module : chunk.getNodes()) {
      final List<VirtualFile> files = moduleToFilesMap.get(module);
      // Important!!! Collections in the myModuleToFilesMap must be modifiable copies of the corresponding collections
      // from the moduleToFilesMap. This is needed to support SourceTransforming compilers
      myModuleToFilesMap.put(module, files == null ? Collections.<VirtualFile>emptyList() : new ArrayList<>(files));
    }
  }

  public static final int SOURCES = 0x1;
  public static final int TEST_SOURCES = 0x2;
  public static final int ALL_SOURCES = SOURCES | TEST_SOURCES;

  public void setSourcesFilter(int filter) {
    mySourcesFilter = filter;
  }

  public int getSourcesFilter() {
    return mySourcesFilter;
  }

  public void substituteWithTransformedVersion(Module module, int fileIndex, VirtualFile transformedFile) {
    final List<VirtualFile> moduleFiles = getFilesToCompile(module);
    final VirtualFile currentFile = moduleFiles.get(fileIndex);
    moduleFiles.set(fileIndex, transformedFile);
    VirtualFile originalFile = myTransformedToOriginalMap.remove(currentFile);
    if (originalFile == null) {
      originalFile = currentFile;
    }
    myTransformedToOriginalMap.put(transformedFile, originalFile);
  }

  public VirtualFile getOriginalFile(VirtualFile file) {
    final VirtualFile original = myTransformedToOriginalMap.get(file);
    return original != null ? original : file;
  }

  @Nonnull
  public List<VirtualFile> getFilesToCompile(Module forModule) {
    return myModuleToFilesMap.get(forModule);
  }

  @Nonnull
  public List<VirtualFile> getFilesToCompile() {
    if (getModuleCount() == 0) {
      return Collections.emptyList();
    }
    final Set<Module> modules = getNodes();

    final List<VirtualFile> filesToCompile = new ArrayList<>();
    for (final Module module : modules) {
      final List<VirtualFile> moduleCompilableFiles = getFilesToCompile(module);
      if (mySourcesFilter == ALL_SOURCES) {
        filesToCompile.addAll(moduleCompilableFiles);
      }
      else {
        for (final VirtualFile file : moduleCompilableFiles) {
          VirtualFile originalFile = myTransformedToOriginalMap.get(file);
          if (originalFile == null) {
            originalFile = file;
          }
          if (mySourcesFilter == TEST_SOURCES) {
            if (myContext.isInTestSourceContent(originalFile)) {
              filesToCompile.add(file);
            }
          }
          else {
            if (!myContext.isInTestSourceContent(originalFile)) {
              filesToCompile.add(file);
            }
          }
        }
      }
    }
    return filesToCompile;
  }

  @Nonnull
  public VirtualFile[] getSourceRoots() {
    return getSourceRoots(mySourcesFilter);
  }

  @Nonnull
  public VirtualFile[] getSourceRoots(final int sourcesFilter) {
    if (getModuleCount() == 0) {
      return VirtualFile.EMPTY_ARRAY;
    }

    return Application.get().runReadAction((Computable<VirtualFile[]>)() -> filterRoots(getAllSourceRoots(), getNodes().iterator().next().getProject(), sourcesFilter));
  }

  public VirtualFile[] getSourceRoots(final Module module) {
    if (!getNodes().contains(module)) {
      return VirtualFile.EMPTY_ARRAY;
    }
    return Application.get().runReadAction((Computable<VirtualFile[]>)() -> filterRoots(myContext.getSourceRoots(module), module.getProject(), mySourcesFilter));
  }

  private VirtualFile[] filterRoots(VirtualFile[] roots, Project project, final int sourcesFilter) {
    final List<VirtualFile> filteredRoots = new ArrayList<>(roots.length);
    for (final VirtualFile root : roots) {
      if (sourcesFilter != ALL_SOURCES) {
        if (myContext.isInTestSourceContent(root)) {
          if ((sourcesFilter & TEST_SOURCES) == 0) {
            continue;
          }
        }
        else {
          if ((sourcesFilter & SOURCES) == 0) {
            continue;
          }
        }
      }
      if (CompilerManager.getInstance(project).isExcludedFromCompilation(root)) {
        continue;
      }
      filteredRoots.add(root);
    }
    return VfsUtil.toVirtualFileArray(filteredRoots);
  }

  private VirtualFile[] getAllSourceRoots() {
    final Set<Module> modules = getNodes();
    Set<VirtualFile> roots = new HashSet<>();
    for (final Module module : modules) {
      ContainerUtil.addAll(roots, myContext.getSourceRoots(module));
    }
    return VfsUtil.toVirtualFileArray(roots);
  }

  public String getCompilationClasspath(SdkType sdkType) {
    final OrderedSet<VirtualFile> cpFiles = getCompilationClasspathFiles(sdkType);
    return convertToStringPath(cpFiles);

  }

  public OrderedSet<VirtualFile> getCompilationClasspathFiles(SdkType sdkType) {
    return getCompilationClasspathFiles(sdkType, true);
  }

  public OrderedSet<VirtualFile> getCompilationClasspathFiles(SdkType sdkType, final boolean exportedOnly) {
    final Set<Module> modules = getNodes();

    OrderedSet<VirtualFile> cpFiles = new OrderedSet<>();
    for (final Module module : modules) {
      Collections.addAll(cpFiles, orderEnumerator(module, exportedOnly, new AfterSdkOrderEntryCondition(sdkType)).getClassesRoots());
    }
    return cpFiles;
  }

  private OrderEnumerator orderEnumerator(Module module, boolean exportedOnly, Condition<OrderEntry> condition) {
    OrderEnumerator enumerator = OrderEnumerator.orderEntries(module).compileOnly().satisfying(condition);
    if ((mySourcesFilter & TEST_SOURCES) == 0) {
      enumerator = enumerator.productionOnly();
    }
    enumerator = enumerator.recursively();
    return exportedOnly ? enumerator.exportedOnly() : enumerator;
  }

  public String getCompilationBootClasspath(SdkType sdkType) {
    return convertToStringPath(getCompilationBootClasspathFiles(sdkType));
  }

  public OrderedSet<VirtualFile> getCompilationBootClasspathFiles(SdkType sdkType) {
    return getCompilationBootClasspathFiles(sdkType, true);
  }

  public OrderedSet<VirtualFile> getCompilationBootClasspathFiles(SdkType sdkType, final boolean exportedOnly) {
    final Set<Module> modules = getNodes();
    final OrderedSet<VirtualFile> cpFiles = new OrderedSet<>();
    final OrderedSet<VirtualFile> jdkFiles = new OrderedSet<>();
    for (final Module module : modules) {
      Collections.addAll(cpFiles, orderEnumerator(module, exportedOnly, new BeforeSdkOrderEntryCondition(sdkType, module)).getClassesRoots());
      Collections.addAll(jdkFiles, OrderEnumerator.orderEntries(module).sdkOnly().getClassesRoots());
    }
    cpFiles.addAll(jdkFiles);
    return cpFiles;
  }

  private static String convertToStringPath(final OrderedSet<VirtualFile> cpFiles) {
    PathsList classpath = new PathsList();
    classpath.addVirtualFiles(cpFiles);
    return classpath.getPathsString();
  }

  public int getModuleCount() {
    return getNodes().size();
  }

  public Module[] getModules() {
    final Set<Module> nodes = getNodes();
    return nodes.toArray(new Module[nodes.size()]);
  }

  public Module getModule() {
    return getNodes().iterator().next();
  }

  @Deprecated
  @DeprecationInfo("use #getSourceRoots(), and process virtual files as you want")
  public String getSourcePath() {
    return getSourcePath(mySourcesFilter);
  }

  @Deprecated
  @DeprecationInfo("use #getSourceRoots(int), and process virtual files as you want")
  public String getSourcePath(final int sourcesFilter) {
    final VirtualFile[] filteredRoots = getSourceRoots(sourcesFilter);
    if (filteredRoots.length == 0) {
      return "";
    }

    final StringBuilder buffer = new StringBuilder();
    Application.get().runReadAction(() -> {
      for (VirtualFile root : filteredRoots) {
        if (buffer.length() > 0) {
          buffer.append(File.pathSeparatorChar);
        }
        buffer.append(root.getPath().replace('/', File.separatorChar));
      }
    });
    return buffer.toString();
  }

  @Nonnull
  public Project getProject() {
    return myContext.getProject();
  }

  private static class BeforeSdkOrderEntryCondition implements Condition<OrderEntry> {
    private boolean mySdkFound;
    private final SdkType mySdkType;
    private final Module myOwnerModule;

    private BeforeSdkOrderEntryCondition(SdkType sdkType, Module ownerModule) {
      mySdkType = sdkType;
      myOwnerModule = ownerModule;
    }

    @Override
    public boolean value(OrderEntry orderEntry) {
      if (orderEntry instanceof ModuleExtensionWithSdkOrderEntry && myOwnerModule.equals(orderEntry.getOwnerModule())) {
        final Sdk sdk = ((ModuleExtensionWithSdkOrderEntry)orderEntry).getSdk();
        if (sdk == null || sdk.getSdkType() != mySdkType) {
          return true;
        }

        mySdkFound = true;
      }
      return !mySdkFound;
    }
  }

  private static class AfterSdkOrderEntryCondition implements Condition<OrderEntry> {
    private final SdkType mySdkType;
    private boolean mySdkFound;

    public AfterSdkOrderEntryCondition(SdkType sdkType) {
      mySdkType = sdkType;
    }

    @Override
    public boolean value(OrderEntry orderEntry) {
      if (orderEntry instanceof ModuleExtensionWithSdkOrderEntry) {
        final Sdk sdk = ((ModuleExtensionWithSdkOrderEntry)orderEntry).getSdk();
        if (sdk == null || sdk.getSdkType() != mySdkType) {
          return true;
        }

        mySdkFound = true;
        return false;
      }
      return mySdkFound;
    }
  }
}
