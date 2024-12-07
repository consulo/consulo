// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.folding.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.codeEditor.internal.FoldingUtil;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.folding.CodeFoldingManager;
import consulo.application.ApplicationManager;
import consulo.document.Document;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.FoldRegion;
import consulo.codeEditor.FoldingModelEx;
import consulo.fileEditor.text.CodeFoldingState;
import consulo.language.editor.internal.EditorFoldingInfoImpl;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.disposer.Disposable;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolderEx;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.util.collection.WeakList;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.List;

@Singleton
@ServiceImpl
public class CodeFoldingManagerImpl extends CodeFoldingManager implements Disposable {
  private final Project myProject;

  private final Collection<Document> myDocumentsWithFoldingInfo = new WeakList<>();

  private final Key<DocumentFoldingInfo> myFoldingInfoInDocumentKey = Key.create("FOLDING_INFO_IN_DOCUMENT_KEY");
  private static final Key<Boolean> FOLDING_STATE_KEY = Key.create("FOLDING_STATE_KEY");

  @Inject
  public CodeFoldingManagerImpl(Project project) {
    myProject = project;
  }

  @Override
  public void dispose() {
    for (Document document : myDocumentsWithFoldingInfo) {
      if (document != null) {
        document.putUserData(myFoldingInfoInDocumentKey, null);
      }
    }
  }

  @Override
  public void releaseFoldings(@Nonnull Editor editor) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    final Project project = editor.getProject();
    if (project != null && (!project.equals(myProject) || !project.isOpen())) return;

    Document document = editor.getDocument();
    PsiFile file = PsiDocumentManager.getInstance(myProject).getPsiFile(document);
    if (file == null || !file.getViewProvider().isPhysical() || !file.isValid()) return;

    EditorFoldingInfoImpl.get(editor).dispose();
  }

  @Override
  public void buildInitialFoldings(@Nonnull final Editor editor) {
    final Project project = editor.getProject();
    if (project == null || !project.equals(myProject) || editor.isDisposed()) return;
    if (!editor.getFoldingModel().isFoldingEnabled()) return;
    if (!FoldingUpdate.supportsDumbModeFolding(editor)) return;

    Document document = editor.getDocument();
    PsiDocumentManager.getInstance(myProject).commitDocument(document);
    CodeFoldingState foldingState = buildInitialFoldings(document);
    if (foldingState != null) {
      foldingState.setToEditor(editor);
    }
  }

  @Nullable
  @Override
  public CodeFoldingState buildInitialFoldings(@Nonnull final Document document) {
    if (myProject.isDisposed()) {
      return null;
    }
    ApplicationManager.getApplication().assertReadAccessAllowed();
    PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(myProject);
    if (psiDocumentManager.isUncommited(document)) {
      // skip building foldings for uncommitted document, CodeFoldingPass invoked by daemon will do it later
      return null;
    }
    //Do not save/restore folding for code fragments
    final PsiFile file = psiDocumentManager.getPsiFile(document);
    if (file == null || !file.isValid() || !file.getViewProvider().isPhysical() && !ApplicationManager.getApplication().isUnitTestMode()) {
      return null;
    }


    final List<FoldingUpdate.RegionInfo> regionInfos = FoldingUpdate.getFoldingsFor(file, document, true);

    return editor -> {
      ApplicationManager.getApplication().assertIsDispatchThread();
      if (myProject.isDisposed() || editor.isDisposed()) return;
      final FoldingModelEx foldingModel = (FoldingModelEx)editor.getFoldingModel();
      if (!foldingModel.isFoldingEnabled()) return;
      if (isFoldingsInitializedInEditor(editor)) return;
      if (DumbService.isDumb(myProject) && !FoldingUpdate.supportsDumbModeFolding(editor)) return;

      foldingModel.runBatchFoldingOperationDoNotCollapseCaret(new UpdateFoldRegionsOperation(myProject, editor, file, regionInfos, UpdateFoldRegionsOperation.ApplyDefaultStateMode.YES, false, false));
      initFolding(editor);
    };
  }

  @Nullable
  @Override
  public Boolean isCollapsedByDefault(@Nonnull FoldRegion region) {
    return region.getUserData(UpdateFoldRegionsOperation.COLLAPSED_BY_DEFAULT);
  }

  @Override
  public void scheduleAsyncFoldingUpdate(@Nonnull Editor editor) {
    FoldingUpdate.clearFoldingCache(editor);
    DaemonCodeAnalyzer.getInstance(myProject).restart();
  }

  private void initFolding(@Nonnull final Editor editor) {
    final Document document = editor.getDocument();
    editor.getFoldingModel().runBatchFoldingOperation(() -> {
      DocumentFoldingInfo documentFoldingInfo = getDocumentFoldingInfo(document);
      Editor[] editors = EditorFactory.getInstance().getEditors(document, myProject);
      for (Editor otherEditor : editors) {
        if (otherEditor == editor || !isFoldingsInitializedInEditor(otherEditor)) continue;
        documentFoldingInfo.loadFromEditor(otherEditor);
        break;
      }
      documentFoldingInfo.setToEditor(editor);
      documentFoldingInfo.clear();

      editor.putUserData(FOLDING_STATE_KEY, Boolean.TRUE);
    });
  }

  @Override
  @Nullable
  public FoldRegion findFoldRegion(@Nonnull Editor editor, int startOffset, int endOffset) {
    return FoldingUtil.findFoldRegion(editor, startOffset, endOffset);
  }

  @Override
  public FoldRegion[] getFoldRegionsAtOffset(@Nonnull Editor editor, int offset) {
    return FoldingUtil.getFoldRegionsAtOffset(editor, offset);
  }

  @Override
  public void updateFoldRegions(@Nonnull Editor editor) {
    updateFoldRegions(editor, false);
  }

  public void updateFoldRegions(Editor editor, boolean quick) {
    if (!editor.getSettings().isAutoCodeFoldingEnabled()) return;
    PsiDocumentManager.getInstance(myProject).commitDocument(editor.getDocument());
    Runnable runnable = updateFoldRegions(editor, false, quick);
    if (runnable != null) {
      runnable.run();
    }
  }

  @Override
  @Nullable
  public Runnable updateFoldRegionsAsync(@Nonnull final Editor editor, final boolean firstTime) {
    if (!editor.getSettings().isAutoCodeFoldingEnabled()) return null;
    final Runnable runnable = updateFoldRegions(editor, firstTime, false);
    return () -> {
      if (runnable != null) {
        runnable.run();
      }
      if (firstTime && !isFoldingsInitializedInEditor(editor)) {
        initFolding(editor);
      }
    };
  }

  @Nullable
  private Runnable updateFoldRegions(@Nonnull Editor editor, boolean applyDefaultState, boolean quick) {
    PsiFile file = PsiDocumentManager.getInstance(myProject).getPsiFile(editor.getDocument());
    return file == null ? null : FoldingUpdate.updateFoldRegions(editor, file, applyDefaultState, quick);
  }

  @Override
  public CodeFoldingState saveFoldingState(@Nonnull Editor editor) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    DocumentFoldingInfo info = getDocumentFoldingInfo(editor.getDocument());
    if (isFoldingsInitializedInEditor(editor)) {
      info.loadFromEditor(editor);
    }
    return info;
  }

  @Override
  public void restoreFoldingState(@Nonnull Editor editor, @Nonnull CodeFoldingState state) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    if (isFoldingsInitializedInEditor(editor)) {
      state.setToEditor(editor);
    }
  }

  @Override
  public void writeFoldingState(@Nonnull CodeFoldingState state, @Nonnull Element element) {
    if (state instanceof DocumentFoldingInfo) {
      ((DocumentFoldingInfo)state).writeExternal(element);
    }
  }

  @Override
  public CodeFoldingState readFoldingState(@Nonnull Element element, @Nonnull Document document) {
    DocumentFoldingInfo info = getDocumentFoldingInfo(document);
    info.readExternal(element);
    return info;
  }

  @Nonnull
  private DocumentFoldingInfo getDocumentFoldingInfo(@Nonnull Document document) {
    DocumentFoldingInfo info = document.getUserData(myFoldingInfoInDocumentKey);
    if (info == null) {
      info = new DocumentFoldingInfo(myProject, document);
      DocumentFoldingInfo written = ((UserDataHolderEx)document).putUserDataIfAbsent(myFoldingInfoInDocumentKey, info);
      if (written == info) {
        myDocumentsWithFoldingInfo.add(document);
      }
      else {
        info = written;
      }
    }
    return info;
  }

  private static boolean isFoldingsInitializedInEditor(@Nonnull Editor editor) {
    return Boolean.TRUE.equals(editor.getUserData(FOLDING_STATE_KEY));
  }
}
