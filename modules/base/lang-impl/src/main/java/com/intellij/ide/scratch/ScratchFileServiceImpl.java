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
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.RoamingType;
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
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.UseScopeEnlarger;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.usages.impl.rules.UsageType;
import com.intellij.usages.impl.rules.UsageTypeProvider;
import com.intellij.util.PathUtil;
import com.intellij.util.containers.ConcurrentFactoryMap;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.IndexableSetContributor;
import com.intellij.util.indexing.LightDirectoryIndex;
import com.intellij.util.messages.MessageBus;
import consulo.annotation.access.RequiredReadAction;
import consulo.container.boot.ContainerPathManager;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.IconDescriptor;
import consulo.ide.IconDescriptorUpdater;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;


@Singleton
@State(name = "ScratchFileService", storages = @Storage(value = "scratches.xml", roamingType = RoamingType.DISABLED))
public class ScratchFileServiceImpl extends ScratchFileService implements PersistentStateComponent<Element>, Disposable {

  private static final RootType NULL_TYPE = new RootType("", null) {
  };

  private final LightDirectoryIndex<RootType> myIndex;
  private final MyLanguages myScratchMapping = new MyLanguages();

  @Inject
  protected ScratchFileServiceImpl(Application application) {
    Disposer.register(this, myScratchMapping);

    myIndex = new LightDirectoryIndex<>(application, NULL_TYPE, index -> {
      LocalFileSystem fileSystem = LocalFileSystem.getInstance();
      for (RootType r : RootType.getAllRootTypes()) {
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
    return FileUtil.toSystemIndependentName(ContainerPathManager.get().getScratchPath());
  }

  @Nonnull
  @Override
  public PerFileMappings<Language> getScratchesMapping() {
    return new PerFileMappings<Language>() {
      @Override
      public void setMapping(@Nullable VirtualFile file, @Nullable Language value) {
        myScratchMapping.setMapping(file, value == null ? null : value.getID());
      }

      @Nullable
      @Override
      public Language getMapping(@Nullable VirtualFile file) {
        return Language.findLanguageByID(myScratchMapping.getMapping(file));
      }
    };
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

  @Override
  public void dispose() {
  }

  private static class MyLanguages extends PerFileMappingsBase<String> {
    @Override
    @Nonnull
    public List<String> getAvailableValues() {
      return ContainerUtil.map(LanguageUtil.getFileLanguages(), Language::getID);
    }

    @Nullable
    @Override
    protected String serialize(String languageID) {
      return languageID;
    }

    @Nullable
    @Override
    protected String handleUnknownMapping(VirtualFile file, String value) {
      return PlainTextLanguage.INSTANCE.getID();
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
      Language result = adjusted != null && adjusted != PlainTextLanguage.INSTANCE ? LanguageSubstitutors.INSTANCE.substituteLanguage(adjusted, file, project) : adjusted;
      return result == Language.ANY ? null : result;
    }
  }

  public static class Highlighter implements SyntaxHighlighterProvider {
    @Override
    @Nullable
    public SyntaxHighlighter create(@Nonnull FileType fileType, @Nullable Project project, @Nullable VirtualFile file) {
      if (project == null || file == null) return null;
      if (!ScratchUtil.isScratch(file)) return null;

      Language language = LanguageUtil.getLanguageForPsi(project, file);
      return language == null ? null : SyntaxHighlighterFactory.getSyntaxHighlighter(language, project, file);
    }
  }

  public static class FilePresentation implements IconDescriptorUpdater, EditorTabTitleProvider {
    private ScratchFileService myScratchFileService;

    @Inject
    public FilePresentation(ScratchFileService scratchFileService) {
      myScratchFileService = scratchFileService;
    }

    @RequiredReadAction
    @Override
    public void updateIcon(@Nonnull IconDescriptor iconDescriptor, @Nonnull PsiElement element, int flags) {
      if (element instanceof PsiFile) {
        VirtualFile virtualFile = ((PsiFile)element).getVirtualFile();
        if (virtualFile == null) {
          return;
        }
        RootType rootType = myScratchFileService.getRootType(virtualFile);
        if (rootType == null) return;
        iconDescriptor.setMainIcon(rootType.substituteIcon(element.getProject(), virtualFile));
      }
    }

    @Nullable
    @Override
    public String getEditorTabTitle(@Nonnull Project project, @Nonnull VirtualFile file) {
      RootType rootType = myScratchFileService.getRootType(file);
      if (rootType == null) return null;
      return rootType.substituteName(project, file);
    }
  }

  public static class AccessExtension implements NonProjectFileWritingAccessExtension {

    @Override
    public boolean isWritable(@Nonnull VirtualFile file) {
      return ScratchUtil.isScratch(file);
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
      for (RootType r : RootType.getAllRootTypes()) {
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

  public static class UseScopeExtension extends UseScopeEnlarger {
    @Nullable
    @Override
    public SearchScope getAdditionalUseScope(@Nonnull PsiElement element) {
      SearchScope useScope = element.getUseScope();
      if (useScope instanceof LocalSearchScope) return null;
      return ScratchesSearchScope.getScratchesScope(element.getProject());
    }
  }

  public static class UsageTypeExtension implements UsageTypeProvider {
    private static final ConcurrentMap<RootType, UsageType> ourUsageTypes = ConcurrentFactoryMap.createMap(key -> new UsageType("Usage in " + key.getDisplayName()));

    @Nullable
    @Override
    public UsageType getUsageType(PsiElement element) {
      VirtualFile file = PsiUtilCore.getVirtualFile(element);
      RootType rootType = ScratchFileService.getInstance().getRootType(file);
      return rootType == null ? null : ourUsageTypes.get(rootType);
    }
  }

  public static class IndexSetContributor extends IndexableSetContributor {

    @Nonnull
    @Override
    public Set<VirtualFile> getAdditionalRootsToIndex() {
      ScratchFileService instance = ScratchFileService.getInstance();
      LocalFileSystem fileSystem = LocalFileSystem.getInstance();
      HashSet<VirtualFile> result = new HashSet<>();
      for (RootType rootType : RootType.getAllRootTypes()) {
        if (rootType.isHidden()) continue;
        ContainerUtil.addIfNotNull(result, fileSystem.findFileByPath(instance.getRootPath(rootType)));
      }
      return result;
    }
  }
}