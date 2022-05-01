// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.completion;

import consulo.ide.impl.idea.openapi.editor.ex.util.EditorUtil;
import consulo.ide.impl.idea.openapi.util.Pair;
import consulo.language.inject.impl.internal.InjectedLanguageUtil;
import consulo.language.editor.internal.PsiUtilBase;
import consulo.ide.impl.idea.reference.SoftReference;
import consulo.application.WriteAction;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.impl.DocumentImpl;
import consulo.language.editor.completion.*;
import consulo.language.file.inject.DocumentWindow;
import consulo.language.impl.psi.PsiFileImpl;
import consulo.language.internal.PsiFileEx;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.util.lang.ref.Ref;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * @author yole
 */
public class CompletionInitializationUtil {
  private static final Logger LOG = Logger.getInstance(CompletionInitializationUtil.class);

  static CompletionInitializationContextImpl createCompletionInitializationContext(@Nonnull Project project,
                                                                                   @Nonnull Editor editor,
                                                                                   @Nonnull Caret caret,
                                                                                   int invocationCount,
                                                                                   CompletionType completionType) {
    return WriteAction.compute(() -> {
      EditorUtil.fillVirtualSpaceUntilCaret(editor);
      PsiDocumentManager.getInstance(project).commitAllDocuments();
      CompletionAssertions.checkEditorValid(editor);

      final PsiFile psiFile = PsiUtilBase.getPsiFileInEditor(caret, project);
      assert psiFile != null : "no PSI file: " + FileDocumentManager.getInstance().getFile(editor.getDocument());
      psiFile.putUserData(PsiFileEx.BATCH_REFERENCE_PROCESSING, Boolean.TRUE);
      CompletionAssertions.assertCommitSuccessful(editor, psiFile);

      return runContributorsBeforeCompletion(editor, psiFile, invocationCount, caret, completionType);
    });
  }

  private static CompletionInitializationContextImpl runContributorsBeforeCompletion(Editor editor, PsiFile psiFile, int invocationCount, @Nonnull Caret caret, CompletionType completionType) {
    final Ref<CompletionContributor> current = Ref.create(null);
    CompletionInitializationContextImpl context = new CompletionInitializationContextImpl(editor, caret, psiFile, completionType, invocationCount) {
      CompletionContributor dummyIdentifierChanger;

      @Override
      public void setDummyIdentifier(@Nonnull String dummyIdentifier) {
        super.setDummyIdentifier(dummyIdentifier);

        if (dummyIdentifierChanger != null) {
          LOG.error("Changing the dummy identifier twice, already changed by " + dummyIdentifierChanger);
        }
        dummyIdentifierChanger = current.get();
      }
    };
    Project project = psiFile.getProject();
    for (final CompletionContributor contributor : CompletionContributor.forLanguageHonorDumbness(context.getPositionLanguage(), project)) {
      current.set(contributor);
      contributor.beforeCompletion(context);
      CompletionAssertions.checkEditorValid(editor);
      assert !PsiDocumentManager.getInstance(project).isUncommited(editor.getDocument()) : "Contributor " + contributor + " left the document uncommitted";
    }
    return context;
  }

  @Nonnull
  static CompletionParameters createCompletionParameters(CompletionInitializationContext initContext, CompletionProcessEx indicator, OffsetsInFile finalOffsets) {
    int offset = finalOffsets.getOffsets().getOffset(CompletionInitializationContext.START_OFFSET);
    PsiFile fileCopy = finalOffsets.getFile();
    PsiFile originalFile = fileCopy.getOriginalFile();
    PsiElement insertedElement = findCompletionPositionLeaf(finalOffsets, offset, originalFile);
    insertedElement.putUserData(CompletionContext.COMPLETION_CONTEXT_KEY, new CompletionContext(fileCopy, finalOffsets.getOffsets()));
    return new CompletionParameters(insertedElement, originalFile, initContext.getCompletionType(), offset, initContext.getInvocationCount(), initContext.getEditor(), indicator);
  }


  static OffsetsInFile insertDummyIdentifier(CompletionInitializationContext initContext, CompletionProgressIndicator indicator) {
    OffsetsInFile topLevelOffsets = indicator.getHostOffsets();
    CompletionAssertions.checkEditorValid(initContext.getEditor());
    if (initContext.getDummyIdentifier().isEmpty()) {
      return topLevelOffsets;
    }

    Editor hostEditor = InjectedLanguageUtil.getTopLevelEditor(initContext.getEditor());
    OffsetMap hostMap = topLevelOffsets.getOffsets();

    PsiFile hostCopy = obtainFileCopy(topLevelOffsets.getFile());
    Document copyDocument = Objects.requireNonNull(hostCopy.getViewProvider().getDocument());

    String dummyIdentifier = initContext.getDummyIdentifier();
    int startOffset = hostMap.getOffset(CompletionInitializationContext.START_OFFSET);
    int endOffset = hostMap.getOffset(CompletionInitializationContext.SELECTION_END_OFFSET);

    indicator.registerChildDisposable(() -> new OffsetTranslatorImpl(hostEditor.getDocument(), initContext.getFile(), copyDocument, startOffset, endOffset, dummyIdentifier));

    copyDocument.setText(hostEditor.getDocument().getText());

    OffsetMap copyOffsets = topLevelOffsets.getOffsets().copyOffsets(copyDocument);
    indicator.registerChildDisposable(() -> copyOffsets);

    copyDocument.replaceString(startOffset, endOffset, dummyIdentifier);
    return new OffsetsInFile(hostCopy, copyOffsets);
  }

  static OffsetsInFile toInjectedIfAny(PsiFile originalFile, OffsetsInFile hostCopyOffsets) {
    CompletionAssertions.assertHostInfo(hostCopyOffsets.getFile(), hostCopyOffsets.getOffsets());

    int hostStartOffset = hostCopyOffsets.getOffsets().getOffset(CompletionInitializationContext.START_OFFSET);
    OffsetsInFile translatedOffsets = hostCopyOffsets.toInjectedIfAny(hostStartOffset);
    if (translatedOffsets != hostCopyOffsets) {
      PsiFile injected = translatedOffsets.getFile();
      if (originalFile != injected && injected instanceof PsiFileImpl && InjectedLanguageManager.getInstance(originalFile.getProject()).isInjectedFragment(originalFile)) {
        ((PsiFileImpl)injected).setOriginalFile(originalFile);
      }
      DocumentWindow documentWindow = InjectedLanguageUtil.getDocumentWindow(injected);
      CompletionAssertions.assertInjectedOffsets(hostStartOffset, injected, documentWindow);

      if (injected.getTextRange().contains(translatedOffsets.getOffsets().getOffset(CompletionInitializationContext.START_OFFSET))) {
        return translatedOffsets;
      }
    }

    return hostCopyOffsets;
  }

  @Nonnull
  private static PsiElement findCompletionPositionLeaf(OffsetsInFile offsets, int offset, PsiFile originalFile) {
    PsiElement insertedElement = offsets.getFile().findElementAt(offset);
    if (insertedElement == null && offsets.getFile().getTextLength() == offset) {
      insertedElement = PsiTreeUtil.getDeepestLast(offsets.getFile());
    }
    CompletionAssertions.assertCompletionPositionPsiConsistent(offsets, offset, originalFile, insertedElement);
    return insertedElement;
  }

  private static PsiFile obtainFileCopy(PsiFile file) {
    final VirtualFile virtualFile = file.getVirtualFile();
    boolean mayCacheCopy = file.isPhysical() &&
                           // we don't want to cache code fragment copies even if they appear to be physical
                           virtualFile != null && virtualFile.isInLocalFileSystem();
    if (mayCacheCopy) {
      final Pair<PsiFile, Document> cached = SoftReference.dereference(file.getUserData(FILE_COPY_KEY));
      if (cached != null && isCopyUpToDate(cached.second, cached.first, file)) {
        PsiFile copy = cached.first;
        CompletionAssertions.assertCorrectOriginalFile("Cached", file, copy);
        return copy;
      }
    }

    final PsiFile copy = (PsiFile)file.copy();
    if (copy.isPhysical() || copy.getViewProvider().isEventSystemEnabled()) {
      LOG.error("File copy should be non-physical and non-event-system-enabled! Language=" + file.getLanguage() + "; file=" + file + " of " + file.getClass());
    }
    CompletionAssertions.assertCorrectOriginalFile("New", file, copy);

    if (mayCacheCopy) {
      final Document document = copy.getViewProvider().getDocument();
      assert document != null;
      syncAcceptSlashR(file.getViewProvider().getDocument(), document);
      file.putUserData(FILE_COPY_KEY, new SoftReference<>(Pair.create(copy, document)));
    }
    return copy;
  }

  private static final Key<SoftReference<Pair<PsiFile, Document>>> FILE_COPY_KEY = Key.create("CompletionFileCopy");

  private static boolean isCopyUpToDate(Document document, @Nonnull PsiFile copyFile, @Nonnull PsiFile originalFile) {
    if (!copyFile.getClass().equals(originalFile.getClass()) || !copyFile.isValid() || !copyFile.getName().equals(originalFile.getName())) {
      return false;
    }
    // the psi file cache might have been cleared by some external activity,
    // in which case PSI-document sync may stop working
    PsiFile current = PsiDocumentManager.getInstance(copyFile.getProject()).getPsiFile(document);
    return current != null && current.getViewProvider().getPsi(copyFile.getLanguage()) == copyFile;
  }

  private static void syncAcceptSlashR(Document originalDocument, Document documentCopy) {
    if (!(originalDocument instanceof DocumentImpl) || !(documentCopy instanceof DocumentImpl)) {
      return;
    }

    ((DocumentImpl)documentCopy).setAcceptSlashR(((DocumentImpl)originalDocument).acceptsSlashR());
  }

}
