/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.ide.scratch;

import com.intellij.ide.navigationToolbar.AbstractNavBarModelExtension;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageUtil;
import com.intellij.lang.PerFileMappings;
import com.intellij.lang.PerFileMappingsBase;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerAdapter;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.impl.EditorTabTitleProvider;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.fileEditor.impl.NonProjectFileWritingAccessExtension;
import com.intellij.openapi.fileTypes.*;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerAdapter;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.LanguageSubstitutor;
import com.intellij.psi.LanguageSubstitutors;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.*;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.usages.impl.rules.UsageType;
import com.intellij.usages.impl.rules.UsageTypeProvider;
import com.intellij.util.PathUtil;
import com.intellij.util.containers.ConcurrentFactoryMap;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.IndexableSetContributor;
import com.intellij.util.indexing.LightDirectoryIndex;
import com.intellij.util.messages.MessageBus;
import consulo.annotations.RequiredReadAction;
import consulo.ide.IconDescriptor;
import consulo.ide.IconDescriptorUpdater;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentMap;


@Singleton
@State(name = "ScratchFileService", storages = {@Storage("scratches.xml")})
public class ScratchFileServiceImpl extends ScratchFileService implements PersistentStateComponent<Element> {

  private static final RootType NULL_TYPE = new RootType("", null) {
  };

  private final LightDirectoryIndex<RootType> myIndex;
  private final MyLanguages myScratchMapping = new MyLanguages();

  @Inject
  protected ScratchFileServiceImpl(Application application) {
    myIndex = new LightDirectoryIndex<>(application, NULL_TYPE, index -> {
      LocalFileSystem fileSystem = LocalFileSystem.getInstance();
      for (RootType r : RootType.getAllRootIds()) {
        index.putInfo(fileSystem.findFileByPath(getRootPath(r)), r);
      }
    });
    initFileOpenedListener(application.getMessageBus());
  }

  @Nonnull
  @Override
  public String getRootPath(@Nonnull RootType rootId) {
    return getRootPath() + "/" + rootId.getId();
  }

  @Nullable
  @Override
  public RootType getRootType(@Nullable VirtualFile file) {
    if (file == null) return null;
    VirtualFile directory = file.isDirectory() ? file : file.getParent();
    RootType result = myIndex.getInfoForFile(directory);
    return result == NULL_TYPE ? null : result;
  }

  private void initFileOpenedListener(MessageBus messageBus) {
    final FileEditorManagerAdapter editorListener = new FileEditorManagerAdapter() {
      @Override
      public void fileOpened(@Nonnull FileEditorManager source, @Nonnull VirtualFile file) {
        if (!isEditable(file)) return;
        RootType rootType = getRootType(file);
        if (rootType == null) return;
        rootType.fileOpened(file, source);
      }

      @Override
      public void fileClosed(@Nonnull FileEditorManager source, @Nonnull VirtualFile file) {
        if (Boolean.TRUE.equals(file.getUserData(FileEditorManagerImpl.CLOSING_TO_REOPEN))) return;
        if (!isEditable(file)) return;

        RootType rootType = getRootType(file);
        if (rootType == null) return;
        rootType.fileClosed(file, source);
      }

      boolean isEditable(@Nonnull VirtualFile file) {
        return FileDocumentManager.getInstance().getDocument(file) != null;
      }
    };
    ProjectManagerAdapter projectListener = new ProjectManagerAdapter() {
      @Override
      public void projectOpened(Project project) {
        project.getMessageBus().connect(project).subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, editorListener);
        FileEditorManager editorManager = FileEditorManager.getInstance(project);
        for (VirtualFile virtualFile : editorManager.getOpenFiles()) {
          editorListener.fileOpened(editorManager, virtualFile);
        }
      }
    };
    for (Project project : ProjectManager.getInstance().getOpenProjects()) {
      projectListener.projectOpened(project);
    }
    messageBus.connect().subscribe(ProjectManager.TOPIC, projectListener);
  }

  @Nonnull
  protected String getRootPath() {
    return FileUtil.toSystemIndependentName(PathManager.getScratchPath());
  }

  @Nonnull
  @Override
  public PerFileMappings<Language> getScratchesMapping() {
    return myScratchMapping;
  }

  @Nullable
  @Override
  public Element getState() {
    return myScratchMapping.getState();
  }

  @Override
  public void loadState(Element state) {
    myScratchMapping.loadState(state);
  }

  private static class MyLanguages extends PerFileMappingsBase<Language> {
    @Override
    protected List<Language> getAvailableValues() {
      return LanguageUtil.getFileLanguages();
    }

    @Nullable
    @Override
    protected String serialize(Language language) {
      return language.getID();
    }

    @Nullable
    @Override
    protected Language handleUnknownMapping(VirtualFile file, String value) {
      return PlainTextLanguage.INSTANCE;
    }
  }

  public static class TypeFactory extends FileTypeFactory {

    @Override
    public void createFileTypes(@Nonnull FileTypeConsumer consumer) {
      consumer.consume(ScratchFileType.INSTANCE);
    }
  }

  public static class Substitutor extends LanguageSubstitutor {
    @Nullable
    @Override
    public Language getLanguage(@Nonnull VirtualFile file, @Nonnull Project project) {
      return substituteLanguage(project, file);
    }

    @Nullable
    public static Language substituteLanguage(@Nonnull Project project, @Nonnull VirtualFile file) {
      RootType rootType = ScratchFileService.getInstance().getRootType(file);
      if (rootType == null) return null;
      Language language = rootType.substituteLanguage(project, file);
      Language adjusted = language != null ? language : getLanguageByFileName(file);
      return adjusted != null && adjusted != ScratchFileType.INSTANCE.getLanguage() ? LanguageSubstitutors.INSTANCE.substituteLanguage(adjusted, file, project) : adjusted;
    }
  }

  public static class Highlighter implements SyntaxHighlighterProvider {
    @Override
    @Nullable
    public SyntaxHighlighter create(@Nonnull FileType fileType, @Nullable Project project, @Nullable VirtualFile file) {
      if (project == null || file == null || !(fileType instanceof ScratchFileType)) return null;

      Language language = LanguageUtil.getLanguageForPsi(project, file);
      return language == null ? null : SyntaxHighlighterFactory.getSyntaxHighlighter(language, project, file);
    }
  }

  public static class FilePresentation implements IconDescriptorUpdater, EditorTabTitleProvider {

    @RequiredReadAction
    @Override
    public void updateIcon(@Nonnull IconDescriptor iconDescriptor, @Nonnull PsiElement element, int flags) {
      if (element instanceof PsiFile) {
        VirtualFile virtualFile = ((PsiFile)element).getVirtualFile();
        if (virtualFile == null) {
          return;
        }
        RootType rootType = ScratchFileService.getInstance().getRootType(virtualFile);
        if (rootType == null) return;
        iconDescriptor.setMainIcon(rootType.substituteIcon(element.getProject(), virtualFile));
      }
    }


    @Nullable
    @Override
    public String getEditorTabTitle(@Nonnull Project project, @Nonnull VirtualFile file) {
      RootType rootType = ScratchFileService.getInstance().getRootType(file);
      if (rootType == null) return null;
      return rootType.substituteName(project, file);
    }
  }

  public static class AccessExtension implements NonProjectFileWritingAccessExtension {

    @Override
    public boolean isWritable(@Nonnull VirtualFile file) {
      return file.getFileType() == ScratchFileType.INSTANCE;
    }
  }

  public static class NavBarExtension extends AbstractNavBarModelExtension {

    @Nullable
    @Override
    public String getPresentableText(Object object) {
      if (!(object instanceof PsiElement)) return null;
      Project project = ((PsiElement)object).getProject();
      VirtualFile virtualFile = PsiUtilCore.getVirtualFile((PsiElement)object);
      if (virtualFile == null || !virtualFile.isValid()) return null;
      RootType rootType = ScratchFileService.getInstance().getRootType(virtualFile);
      if (rootType == null) return null;
      if (virtualFile.isDirectory() && additionalRoots(project).contains(virtualFile)) {
        return rootType.getDisplayName();
      }
      return rootType.substituteName(project, virtualFile);
    }

    @Nonnull
    @Override
    public Collection<VirtualFile> additionalRoots(Project project) {
      Set<VirtualFile> result = ContainerUtil.newLinkedHashSet();
      LocalFileSystem fileSystem = LocalFileSystem.getInstance();
      ScratchFileService app = ScratchFileService.getInstance();
      for (RootType r : RootType.getAllRootIds()) {
        ContainerUtil.addIfNotNull(result, fileSystem.findFileByPath(app.getRootPath(r)));
      }
      return result;
    }
  }

  @Override
  public VirtualFile findFile(@Nonnull RootType rootType, @Nonnull String pathName, @Nonnull Option option) throws IOException {
    ApplicationManager.getApplication().assertReadAccessAllowed();

    String fullPath = getRootPath(rootType) + "/" + pathName;
    if (option != Option.create_new_always) {
      VirtualFile file = LocalFileSystem.getInstance().findFileByPath(fullPath);
      if (file != null && !file.isDirectory()) return file;
      if (option == Option.existing_only) return null;
    }
    String ext = PathUtil.getFileExtension(pathName);
    String fileNameExt = PathUtil.getFileName(pathName);
    String fileName = StringUtil.trimEnd(fileNameExt, ext == null ? "" : "." + ext);
    AccessToken token = ApplicationManager.getApplication().acquireWriteActionLock(getClass());
    try {
      VirtualFile dir = VfsUtil.createDirectories(PathUtil.getParentPath(fullPath));
      if (option == Option.create_new_always) {
        return VfsUtil.createChildSequent(LocalFileSystem.getInstance(), dir, fileName, StringUtil.notNullize(ext));
      }
      else {
        return dir.createChildData(LocalFileSystem.getInstance(), fileNameExt);
      }
    }
    finally {
      token.finish();
    }
  }

  @Nullable
  private static Language getLanguageByFileName(@Nullable VirtualFile file) {
    return file == null ? null : LanguageUtil.getFileTypeLanguage(FileTypeManager.getInstance().getFileTypeByFileName(file.getName()));
  }

  @Nonnull
  public static GlobalSearchScope buildScratchesSearchScope() {
    final ScratchFileService service = ScratchFileService.getInstance();
    return new GlobalSearchScope() {
      @Nonnull
      @Override
      public String getDisplayName() {
        return "Scratches and Consoles";
      }

      @Override
      public boolean contains(@Nonnull VirtualFile file) {
        RootType rootType = file.getFileType() == ScratchFileType.INSTANCE ? service.getRootType(file) : null;
        return rootType != null && !rootType.isHidden();
      }

      @Override
      public boolean isSearchOutsideRootModel() {
        return true;
      }

      @Override
      public int compare(@Nonnull VirtualFile file1, @Nonnull VirtualFile file2) {
        return 0;
      }

      @Override
      public boolean isSearchInModuleContent(@Nonnull Module aModule) {
        return false;
      }

      @Override
      public boolean isSearchInLibraries() {
        return false;
      }

      @Nonnull
      @Override
      public GlobalSearchScope intersectWith(@Nonnull GlobalSearchScope scope) {
        if (scope instanceof ProjectAndLibrariesScope) return this;
        return super.intersectWith(scope);
      }

      @Override
      public String toString() {
        return getDisplayName();
      }
    };
  }

  public static class UseScopeExtension extends UseScopeEnlarger {
    @Nullable
    @Override
    public SearchScope getAdditionalUseScope(@Nonnull PsiElement element) {
      SearchScope useScope = element.getUseScope();
      if (useScope instanceof LocalSearchScope) return null;
      return buildScratchesSearchScope();
    }
  }

  public static class UsageTypeExtension implements UsageTypeProvider {
    private static final ConcurrentMap<RootType, UsageType> ourUsageTypes = ConcurrentFactoryMap.createMap(key -> new UsageType("Usage in " + key.getDisplayName()));

    @Nullable
    @Override
    public UsageType getUsageType(PsiElement element) {
      VirtualFile file = PsiUtilCore.getVirtualFile(element);
      RootType rootType = file != null && file.getFileType() == ScratchFileType.INSTANCE ? ScratchFileService.getInstance().getRootType(file) : null;
      return rootType == null ? null : ourUsageTypes.get(rootType);
    }
  }

  public static class IndexSetContributor extends IndexableSetContributor {

    @Nonnull
    @Override
    public Set<VirtualFile> getAdditionalRootsToIndex() {
      ScratchFileService instance = ScratchFileService.getInstance();
      LocalFileSystem fileSystem = LocalFileSystem.getInstance();
      HashSet<VirtualFile> result = ContainerUtil.newHashSet();
      for (RootType rootType : RootType.getAllRootIds()) {
        if (rootType.isHidden()) continue;
        ContainerUtil.addIfNotNull(result, fileSystem.findFileByPath(instance.getRootPath(rootType)));
      }
      return result;
    }

    @Nonnull
    @Override
    public Set<VirtualFile> getAdditionalProjectRootsToIndex(@Nonnull Project project) {
      return Collections.emptySet();
    }
  }
}