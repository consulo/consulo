// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.impl.internal.completion;

import consulo.annotation.access.RequiredReadAction;
import consulo.codeEditor.Editor;
import consulo.disposer.Disposable;
import consulo.document.Document;
import consulo.document.DocumentWindow;
import consulo.document.event.DocumentEvent;
import consulo.document.internal.RangeMarkerEx;
import consulo.document.util.TextRange;
import consulo.language.ast.FileASTNode;
import consulo.language.editor.completion.CompletionInitializationContext;
import consulo.language.editor.completion.OffsetMap;
import consulo.language.editor.completion.lookup.InsertionContext;
import consulo.language.editor.completion.lookup.Lookup;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.inject.EditorWindow;
import consulo.language.file.FileViewProvider;
import consulo.language.impl.DebugUtil;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.logging.attachment.Attachment;
import consulo.logging.attachment.AttachmentFactory;
import consulo.logging.attachment.RuntimeExceptionWithAttachments;
import consulo.util.lang.ImmutableCharSequence;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.annotations.Contract;

import java.util.List;

/**
 * @author peter
 */
public class CompletionAssertions {

    @RequiredReadAction
    public static void assertCommitSuccessful(Editor editor, PsiFile psiFile) {
        Document document = editor.getDocument();
        int docLength = document.getTextLength();
        int psiLength = psiFile.getTextLength();
        PsiDocumentManager manager = PsiDocumentManager.getInstance(psiFile.getProject());
        boolean committed = !manager.isUncommited(document);
        if (docLength == psiLength && committed) {
            return;
        }

        FileViewProvider viewProvider = psiFile.getViewProvider();

        String message = "unsuccessful commit:";
        message += "\nmatching=" + (psiFile == manager.getPsiFile(document));
        message += "\ninjectedEditor=" + (editor instanceof EditorWindow);
        message += "\ninjectedFile=" + InjectedLanguageManager.getInstance(psiFile.getProject()).isInjectedFragment(psiFile);
        message += "\ncommitted=" + committed;
        message += "\nfile=" + psiFile.getName();
        message += "\nfile class=" + psiFile.getClass();
        message += "\nfile.valid=" + psiFile.isValid();
        message += "\nfile.physical=" + psiFile.isPhysical();
        message += "\nfile.eventSystemEnabled=" + viewProvider.isEventSystemEnabled();
        message += "\nlanguage=" + psiFile.getLanguage();
        message += "\ndoc.length=" + docLength;
        message += "\npsiFile.length=" + psiLength;
        String fileText = psiFile.getText();
        if (fileText != null) {
            message += "\npsiFile.text.length=" + fileText.length();
        }
        FileASTNode node = psiFile.getNode();
        if (node != null) {
            message += "\nnode.length=" + node.getTextLength();
            String nodeText = node.getText();
            message += "\nnode.text.length=" + nodeText.length();
        }
        VirtualFile virtualFile = viewProvider.getVirtualFile();
        message += "\nvirtualFile=" + virtualFile;
        message += "\nvirtualFile.class=" + virtualFile.getClass();
        message += "\n" + DebugUtil.currentStackTrace();

        throw new RuntimeExceptionWithAttachments("Commit unsuccessful", message,
            AttachmentFactory.get()
                .create(virtualFile.getPath() + "_file.txt", StringUtil.notNullize(fileText)), createAstAttachment(psiFile, psiFile),
            AttachmentFactory.get().create("docText.txt", document.getText()));
    }

    public static void checkEditorValid(Editor editor) {
        if (!isEditorValid(editor)) {
            throw new AssertionError();
        }
    }

    public static boolean isEditorValid(Editor editor) {
        return !(editor instanceof EditorWindow) || ((EditorWindow) editor).isValid();
    }

    private static Attachment createAstAttachment(PsiFile fileCopy, final PsiFile originalFile) {
        return AttachmentFactory.get().create(originalFile.getViewProvider().getVirtualFile().getPath() + " syntactic tree.txt", DebugUtil.psiToString(fileCopy, false, true));
    }

    private static Attachment createFileTextAttachment(PsiFile fileCopy, final PsiFile originalFile) {
        return AttachmentFactory.get().create(originalFile.getViewProvider().getVirtualFile().getPath(), fileCopy.getText());
    }

    public static void assertInjectedOffsets(int hostStartOffset, PsiFile injected, DocumentWindow documentWindow) {
        assert documentWindow != null : "no DocumentWindow for an injected fragment";

        InjectedLanguageManager injectedLanguageManager = InjectedLanguageManager.getInstance(injected.getProject());
        TextRange injectedRange = injected.getTextRange();
        int hostMinOffset = injectedLanguageManager.injectedToHost(injected, injectedRange.getStartOffset(), true);
        int hostMaxOffset = injectedLanguageManager.injectedToHost(injected, injectedRange.getEndOffset(), false);
        assert hostStartOffset >= hostMinOffset : "startOffset before injected";
        assert hostStartOffset <= hostMaxOffset : "startOffset after injected";
    }

    public static void assertHostInfo(PsiFile hostCopy, OffsetMap hostMap) {
        PsiUtilCore.ensureValid(hostCopy);
        if (hostMap.getOffset(CompletionInitializationContext.START_OFFSET) > hostCopy.getTextLength()) {
            throw new AssertionError("startOffset outside the host file: " + hostMap.getOffset(CompletionInitializationContext.START_OFFSET) + "; " + hostCopy);
        }
    }

    @Contract("_,_,_,null->fail")
    public static void assertCompletionPositionPsiConsistent(OffsetsInFile offsets, int offset, PsiFile originalFile, PsiElement insertedElement) {
        PsiFile fileCopy = offsets.getFile();
        if (insertedElement == null) {
            throw new RuntimeExceptionWithAttachments("No element at insertion offset", "offset=" + offset, createFileTextAttachment(fileCopy, originalFile), createAstAttachment(fileCopy, originalFile));
        }

        final TextRange range = insertedElement.getTextRange();
        CharSequence fileCopyText = fileCopy.getViewProvider().getContents();
        if ((range.getEndOffset() > fileCopyText.length()) || !isEquals(fileCopyText.subSequence(range.getStartOffset(), range.getEndOffset()), insertedElement.getNode().getChars())) {
            throw new RuntimeExceptionWithAttachments("Inconsistent completion tree", "range=" + range, createFileTextAttachment(fileCopy, originalFile), createAstAttachment(fileCopy, originalFile),
                AttachmentFactory.get().create("Element at caret.txt", insertedElement.getText()));
        }
    }

    private static boolean isEquals(CharSequence left, CharSequence right) {
        if (left == right) {
            return true;
        }
        if (left instanceof ImmutableCharSequence && right instanceof ImmutableCharSequence) {
            return left.equals(right);
        }
        return left.toString().equals(right.toString());
    }

    public static void assertCorrectOriginalFile(String prefix, PsiFile file, PsiFile copy) {
        if (copy.getOriginalFile() != file) {
            throw new AssertionError(prefix + " copied file doesn't have correct original: noOriginal=" + (copy.getOriginalFile() == copy) + "\n file " + fileInfo(file) + "\n copy " + fileInfo(copy));
        }
    }

    private static String fileInfo(PsiFile file) {
        return file + " of " + file.getClass() + " in " + file.getViewProvider() + ", languages=" + file.getViewProvider().getLanguages() + ", physical=" + file.isPhysical();
    }

    public static class WatchingInsertionContext extends InsertionContext implements Disposable {
        private RangeMarkerEx tailWatcher;
        public Throwable invalidateTrace;
        DocumentEvent killer;
        private RangeMarkerSpy spy;

        public WatchingInsertionContext(OffsetMap offsetMap, PsiFile file, char completionChar, List<LookupElement> items, Editor editor) {
            super(offsetMap, completionChar, items.toArray(LookupElement.EMPTY_ARRAY), file, editor, Lookup.shouldAddCompletionChar(completionChar));
        }

        @Override
        public void setTailOffset(int offset) {
            super.setTailOffset(offset);
            watchTail(offset);
        }

        private void watchTail(int offset) {
            stopWatching();
            tailWatcher = (RangeMarkerEx) getDocument().createRangeMarker(offset, offset);
            if (!tailWatcher.isValid()) {
                throw new AssertionError(getDocument() + "; offset=" + offset);
            }
            tailWatcher.setGreedyToRight(true);
            spy = new RangeMarkerSpy(this, tailWatcher);
            getDocument().addDocumentListener(spy);
        }

        public void stopWatching() {
            if (tailWatcher != null) {
                if (spy != null) {
                    getDocument().removeDocumentListener(spy);
                    spy = null;
                }
                tailWatcher.dispose();
            }
        }

        @Override
        public int getTailOffset() {
            if (!getOffsetMap().containsOffset(TAIL_OFFSET) && invalidateTrace != null) {
                throw new RuntimeExceptionWithAttachments("Tail offset invalid", AttachmentFactory.get().create("invalidated", invalidateTrace));
            }

            int offset = super.getTailOffset();
            if (tailWatcher.getStartOffset() != tailWatcher.getEndOffset() && offset > 0) {
                watchTail(offset);
            }

            return offset;
        }

        @Override
        public void dispose() {
            stopWatching(); // used by Fleet as ad-hoc memory leak fix
        }
    }
}
