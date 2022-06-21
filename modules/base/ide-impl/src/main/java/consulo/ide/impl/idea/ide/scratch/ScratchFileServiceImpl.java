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
package consulo.ide.impl.idea.ide.scratch;

import consulo.annotation.component.ServiceImpl;
import consulo.application.AccessToken;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.component.messagebus.MessageBus;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.RoamingType;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.container.boot.ContainerPathManager;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.FileDocumentManager;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.event.FileEditorManagerAdapter;
import consulo.fileEditor.event.FileEditorManagerListener;
import consulo.ide.impl.idea.lang.PerFileMappingsBase;
import consulo.ide.impl.idea.openapi.fileEditor.impl.FileEditorManagerImpl;
import consulo.ide.impl.idea.openapi.fileEditor.impl.NonProjectFileWritingAccessExtension;
import consulo.ide.impl.idea.openapi.util.io.FileUtil;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
import consulo.ide.impl.idea.util.PathUtil;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.impl.idea.util.indexing.LightDirectoryIndex;
import consulo.language.Language;
import consulo.language.editor.highlight.SyntaxHighlighter;
import consulo.language.editor.highlight.SyntaxHighlighterFactory;
import consulo.language.editor.highlight.SyntaxHighlighterProvider;
import consulo.language.editor.scratch.RootType;
import consulo.language.editor.scratch.ScratchFileService;
import consulo.language.editor.scratch.ScratchUtil;
import consulo.language.file.FileTypeManager;
import consulo.language.plain.PlainTextLanguage;
import consulo.language.psi.LanguageSubstitutor;
import consulo.language.psi.LanguageSubstitutors;
import consulo.language.util.LanguageUtil;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.event.ProjectManagerListener;
import consulo.ui.UIAccess;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.util.PerFileMappings;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;


@Singleton
@State(name = "ScratchFileService", storages = @Storage(value = "scratches.xml", roamingType = RoamingType.DISABLED))
@ServiceImpl
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
    ProjectManagerListener projectListener = new ProjectManagerListener() {
      @Override
      public void projectOpened(Project project, @Nonnull UIAccess uiAccess) {
        project.getMessageBus().connect(project).subscribe(FileEditorManagerListener.class, editorListener);
        FileEditorManager editorManager = FileEditorManager.getInstance(project);
        for (VirtualFile virtualFile : editorManager.getOpenFiles()) {
          editorListener.fileOpened(editorManager, virtualFile);
        }
      }
    };
    for (Project project : ProjectManager.getInstance().getOpenProjects()) {
      projectListener.projectOpened(project, project.getApplication().getLastUIAccess());
    }
    messageBus.connect().subscribe(ProjectManagerListener.class, projectListener);
  }

  @Nonnull
  protected String getRootPath() {
    return FileUtil.toSystemIndependentName(ContainerPathManager.get().getScratchPath());
  }

  @Nonnull
  @Override
  public PerFileMappings<Language> getScratchesMapping() {
    return new PerFileMappings<>() {
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

}