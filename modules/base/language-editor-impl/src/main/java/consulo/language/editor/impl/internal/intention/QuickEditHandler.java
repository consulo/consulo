/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.language.editor.impl.internal.intention;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.codeEditor.*;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.codeEditor.action.EditorActionManager;
import consulo.codeEditor.event.EditorFactoryEvent;
import consulo.codeEditor.event.EditorFactoryListener;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.*;
import consulo.document.event.DocumentAdapter;
import consulo.document.event.DocumentEvent;
import consulo.document.internal.DocumentEx;
import consulo.document.util.DocumentUtil;
import consulo.document.util.ProperTextRange;
import consulo.document.util.Segment;
import consulo.document.util.TextRange;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.FileEditorWindow;
import consulo.fileEditor.FileEditorWithProviderComposite;
import consulo.language.Language;
import consulo.language.codeStyle.CodeStyleManager;
import consulo.language.codeStyle.PostprocessReformattingAspect;
import consulo.language.editor.action.CopyPastePreProcessor;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.language.editor.impl.intention.QuickEditAction;
import consulo.language.editor.template.TemplateManager;
import consulo.language.file.light.LightVirtualFile;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.inject.impl.internal.InjectedLanguageUtil;
import consulo.language.inject.impl.internal.PlaceImpl;
import consulo.language.psi.*;
import consulo.language.util.IncorrectOperationException;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CommonShortcuts;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.popup.Balloon;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.undoRedo.ProjectUndoManager;
import consulo.undoRedo.UndoManager;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.*;
import consulo.virtualFileSystem.ReadonlyStatusHandler;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * @author Gregory Shrago
 */
public class QuickEditHandler extends DocumentAdapter implements Disposable {
    private final Project myProject;
    private final QuickEditAction myAction;

    private final Editor myEditor;
    private final Document myOrigDocument;

    private final Document myNewDocument;
    private final PsiFile myNewFile;
    private final LightVirtualFile myNewVirtualFile;

    private final long myOrigCreationStamp;
    private FileEditorWindow mySplittedWindow;
    private boolean myCommittingToOriginal;

    private final PsiFile myInjectedFile;
    private final List<Trinity<RangeMarker, RangeMarker, SmartPsiElementPointer>> myMarkers = new LinkedList<>();

    @Nullable
    private final RangeMarker myAltFullRange;
    private static final Key<String> REPLACEMENT_KEY = Key.create("REPLACEMENT_KEY");

    public QuickEditHandler(Project project, @Nonnull PsiFile injectedFile, PsiFile origFile, Editor editor, QuickEditAction action) {
        myProject = project;
        myEditor = editor;
        myAction = action;
        myOrigDocument = editor.getDocument();
        PlaceImpl shreds = InjectedLanguageUtil.getShreds(injectedFile);
        FileType fileType = injectedFile.getFileType();
        Language language = injectedFile.getLanguage();
        PsiLanguageInjectionHost.Shred firstShred = ContainerUtil.getFirstItem(shreds);

        PsiFileFactory factory = PsiFileFactory.getInstance(project);
        String text = InjectedLanguageManager.getInstance(project).getUnescapedText(injectedFile);
        String newFileName = StringUtil.notNullize(language.getDisplayName().get(), "Injected") +
            " Fragment " +
            "(" +
            origFile.getName() +
            ":" +
            firstShred.getHost().getTextRange().getStartOffset() +
            ")" +
            "." +
            fileType.getDefaultExtension();

        // preserve \r\n as it is done in MultiHostRegistrarImpl
        myNewFile = factory.createFileFromText(newFileName, language, text, true, false);
        myNewVirtualFile = ObjectUtil.assertNotNull((LightVirtualFile)myNewFile.getVirtualFile());
        myNewVirtualFile.setOriginalFile(origFile.getVirtualFile());

        assert myNewFile != null : "PSI file is null";
        assert myNewFile.getTextLength() == myNewVirtualFile.getContent().length() : "PSI / Virtual file text mismatch";

        myNewVirtualFile.setOriginalFile(origFile.getVirtualFile());
        // suppress possible errors as in injected mode
        myNewFile.putUserData(
            InjectedLanguageUtil.FRANKENSTEIN_INJECTION,
            injectedFile.getUserData(InjectedLanguageUtil.FRANKENSTEIN_INJECTION)
        );
        myNewFile.putUserData(FileContextUtil.INJECTED_IN_ELEMENT, shreds.getHostPointer());
        myNewDocument = PsiDocumentManager.getInstance(project).getDocument(myNewFile);
        assert myNewDocument != null;
        EditorActionManager.getInstance().setReadonlyFragmentModificationHandler(myNewDocument, new MyQuietHandler());
        myOrigCreationStamp = myOrigDocument.getModificationStamp(); // store creation stamp for UNDO tracking
        myOrigDocument.addDocumentListener(this, this);
        myNewDocument.addDocumentListener(this, this);
        EditorFactory editorFactory = ObjectUtil.assertNotNull(EditorFactory.getInstance());
        // not FileEditorManager listener because of RegExp checker and alike
        editorFactory.addEditorFactoryListener(new EditorFactoryListener() {

            int myEditorCount;

            @Override
            public void editorCreated(@Nonnull EditorFactoryEvent event) {
                if (event.getEditor().getDocument() != myNewDocument) {
                    return;
                }
                myEditorCount++;
                EditorActionHandler editorEscape =
                    EditorActionManager.getInstance().getActionHandler(IdeActions.ACTION_EDITOR_ESCAPE);
                if (!myAction.isShowInBalloon()) {
                    new AnAction() {
                        @Override
                        public void update(@Nonnull AnActionEvent e) {
                            Editor editor = e.getData(Editor.KEY);
                            e.getPresentation().setEnabled(editor != null &&
                                LookupManager.getActiveLookup(editor) == null &&
                                TemplateManager.getInstance(myProject).getActiveTemplate(editor) == null &&
                                (editorEscape == null || !editorEscape.isEnabled(editor, e.getDataContext())));
                        }

                        @Override
                        @RequiredUIAccess
                        public void actionPerformed(@Nonnull AnActionEvent e) {
                            closeEditor();
                        }
                    }.registerCustomShortcutSet(CommonShortcuts.ESCAPE, event.getEditor().getContentComponent());
                }
            }

            @Override
            public void editorReleased(@Nonnull EditorFactoryEvent event) {
                if (event.getEditor().getDocument() != myNewDocument) {
                    return;
                }
                if (--myEditorCount > 0) {
                    return;
                }

                if (Boolean.TRUE.equals(myNewVirtualFile.getUserData(FileEditorManager.CLOSING_TO_REOPEN))) {
                    return;
                }

                Disposer.dispose(QuickEditHandler.this);
            }
        }, this);

        if ("JAVA".equals(firstShred.getHost().getLanguage().getID())) {
            PsiLanguageInjectionHost.Shred lastShred = ContainerUtil.getLastItem(shreds);
            myAltFullRange = myOrigDocument.createRangeMarker(
                firstShred.getHostRangeMarker().getStartOffset(),
                lastShred.getHostRangeMarker().getEndOffset()
            );
            myAltFullRange.setGreedyToLeft(true);
            myAltFullRange.setGreedyToRight(true);

            initGuardedBlocks(shreds);
            myInjectedFile = null;
        }
        else {
            initMarkers(shreds);
            myAltFullRange = null;
            myInjectedFile = injectedFile;
        }
    }

    public boolean isValid() {
        boolean valid =
            myNewVirtualFile.isValid() && (myAltFullRange == null && myInjectedFile.isValid() || myAltFullRange != null && myAltFullRange.isValid());
        if (valid) {
            for (Trinity<RangeMarker, RangeMarker, SmartPsiElementPointer> t : myMarkers) {
                if (!t.first.isValid() || !t.second.isValid() || t.third.getElement() == null) {
                    valid = false;
                    break;
                }
            }
        }
        return valid;
    }

    public void navigate(int injectedOffset) {
        if (myAction.isShowInBalloon()) {
            JComponent component = myAction.createBalloonComponent(myNewFile);
            if (component != null) {
                Balloon balloon = JBPopupFactory.getInstance()
                    .createBalloonBuilder(component)
                    .setShadow(true)
                    .setAnimationCycle(0)
                    .setHideOnClickOutside(true)
                    .setHideOnKeyOutside(true)
                    .setHideOnAction(false)
                    .setFillColor(UIUtil.getControlColor())
                    .createBalloon();
                new AnAction() {
                    @Override
                    @RequiredUIAccess
                    public void actionPerformed(@Nonnull AnActionEvent e) {
                        balloon.hide();
                    }
                }.registerCustomShortcutSet(CommonShortcuts.ESCAPE, component);
                Disposer.register(myNewFile.getProject(), balloon);
                Balloon.Position position = QuickEditAction.getBalloonPosition(myEditor);
                RelativePoint point = EditorPopupHelper.getInstance().guessBestPopupLocation(myEditor);
                if (position == Balloon.Position.above) {
                    Point p = point.getPoint();
                    point = new RelativePoint(point.getComponent(), new Point(p.x, p.y - myEditor.getLineHeight()));
                }
                balloon.show(point, position);
            }
        }
        else {
            FileEditorManager fileEditorManager = FileEditorManager.getInstance(myProject);
            FileEditor[] editors = fileEditorManager.getEditors(myNewVirtualFile);
            if (editors.length == 0) {
                FileEditorWindow curWindow = fileEditorManager.getCurrentWindow();
                mySplittedWindow = curWindow.split(SwingConstants.HORIZONTAL, false, myNewVirtualFile, true);
            }
            Editor editor = fileEditorManager.openTextEditor(OpenFileDescriptorFactory.getInstance(myProject)
                .builder(myNewVirtualFile)
                .offset(injectedOffset)
                .build(), true);
            // fold missing values
            if (editor != null) {
                editor.putUserData(QuickEditAction.QUICK_EDIT_HANDLER, this);
                FoldingModel foldingModel = editor.getFoldingModel();
                foldingModel.runBatchFoldingOperation(() -> {
                    for (RangeMarker o : ContainerUtil.reverse(((DocumentEx)myNewDocument).getGuardedBlocks())) {
                        String replacement = o.getUserData(REPLACEMENT_KEY);
                        if (StringUtil.isEmpty(replacement)) {
                            continue;
                        }
                        FoldRegion region = foldingModel.addFoldRegion(o.getStartOffset(), o.getEndOffset(), replacement);
                        if (region != null) {
                            region.setExpanded(false);
                        }
                    }
                });
            }
            SwingUtilities.invokeLater(() -> myEditor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE));
        }
    }

    @Override
    public void documentChanged(DocumentEvent e) {
        UndoManager undoManager = ProjectUndoManager.getInstance(myProject);
        boolean undoOrRedo = undoManager.isUndoInProgress() || undoManager.isRedoInProgress();
        if (undoOrRedo) {
            // allow undo/redo up until 'creation stamp' back in time
            // and check it after action is completed
            if (e.getDocument() == myOrigDocument) {
                //noinspection SSBasedInspection
                SwingUtilities.invokeLater(() -> {
                    if (myOrigCreationStamp > myOrigDocument.getModificationStamp()) {
                        closeEditor();
                    }
                });
            }
        }
        else if (e.getDocument() == myNewDocument) {
            commitToOriginal(e);
            if (!isValid()) {
                Application.get().invokeLater(this::closeEditor, myProject.getDisposed());
            }
        }
        else if (e.getDocument() == myOrigDocument) {
            if (myCommittingToOriginal || myAltFullRange != null && myAltFullRange.isValid()) {
                return;
            }
            Application.get().invokeLater(this::closeEditor, myProject.getDisposed());
        }
    }

    private void closeEditor() {
        boolean unsplit = false;
        if (mySplittedWindow != null && !mySplittedWindow.isDisposed()) {
            FileEditorWithProviderComposite[] editors = mySplittedWindow.getEditors();
            if (editors.length == 1 && Comparing.equal(editors[0].getFile(), myNewVirtualFile)) {
                unsplit = true;
            }
        }
        FileEditorManager.getInstance(myProject).closeFile(myNewVirtualFile);
        if (unsplit) {
            for (FileEditorWindow editorWindow : mySplittedWindow.findSiblings()) {
                editorWindow.unsplit(true);
            }
        }
    }

    @RequiredReadAction
    public void initMarkers(PlaceImpl shreds) {
        SmartPointerManager smartPointerManager = SmartPointerManager.getInstance(myProject);
        int curOffset = -1;
        for (PsiLanguageInjectionHost.Shred shred : shreds) {
            RangeMarker rangeMarker = myNewDocument.createRangeMarker(
                shred.getRange().getStartOffset() + shred.getPrefix().length(),
                shred.getRange().getEndOffset() - shred.getSuffix().length()
            );
            TextRange rangeInsideHost = shred.getRangeInsideHost();
            PsiLanguageInjectionHost host = shred.getHost();
            RangeMarker origMarker = myOrigDocument.createRangeMarker(rangeInsideHost.shiftRight(host.getTextRange().getStartOffset()));
            SmartPsiElementPointer<PsiLanguageInjectionHost> elementPointer = smartPointerManager.createSmartPsiElementPointer(host);
            Trinity<RangeMarker, RangeMarker, SmartPsiElementPointer> markers =
                Trinity.<RangeMarker, RangeMarker, SmartPsiElementPointer>create(origMarker, rangeMarker, elementPointer);
            myMarkers.add(markers);

            origMarker.setGreedyToRight(true);
            rangeMarker.setGreedyToRight(true);
            if (origMarker.getStartOffset() > curOffset) {
                origMarker.setGreedyToLeft(true);
                rangeMarker.setGreedyToLeft(true);
            }
            curOffset = origMarker.getEndOffset();
        }
        initGuardedBlocks(shreds);
    }

    private void initGuardedBlocks(PlaceImpl shreds) {
        int origOffset = -1;
        int curOffset = 0;
        for (PsiLanguageInjectionHost.Shred shred : shreds) {
            Segment hostRangeMarker = shred.getHostRangeMarker();
            int start = shred.getRange().getStartOffset() + shred.getPrefix().length();
            int end = shred.getRange().getEndOffset() - shred.getSuffix().length();
            if (curOffset < start) {
                RangeMarker guard = myNewDocument.createGuardedBlock(curOffset, start);
                if (curOffset == 0 && shred == shreds.get(0)) {
                    guard.setGreedyToLeft(true);
                }
                String padding = origOffset < 0 ? "" : myOrigDocument.getText().substring(origOffset, hostRangeMarker.getStartOffset());
                guard.putUserData(REPLACEMENT_KEY, fixQuotes(padding));
            }
            curOffset = end;
            origOffset = hostRangeMarker.getEndOffset();
        }
        if (curOffset < myNewDocument.getTextLength()) {
            RangeMarker guard = myNewDocument.createGuardedBlock(curOffset, myNewDocument.getTextLength());
            guard.setGreedyToRight(true);
            guard.putUserData(REPLACEMENT_KEY, "");
        }
    }

    private void commitToOriginal(DocumentEvent e) {
        VirtualFile origVirtualFile = PsiUtilCore.getVirtualFile(myNewFile.getContext());
        myCommittingToOriginal = true;
        try {
            if (origVirtualFile == null
                || !ReadonlyStatusHandler.getInstance(myProject).ensureFilesWritable(origVirtualFile).hasReadonlyFiles()) {
                PostprocessReformattingAspect.getInstance(myProject).disablePostprocessFormattingInside(() -> {
                    if (myAltFullRange != null) {
                        altCommitToOriginal(e);
                        return;
                    }
                    commitToOriginalInner();
                });
                PsiDocumentManager.getInstance(myProject).doPostponedOperationsAndUnblockDocument(myOrigDocument);
            }
        }
        finally {
            myCommittingToOriginal = false;
        }
    }

    @RequiredReadAction
    private void commitToOriginalInner() {
        String text = myNewDocument.getText();
        Map<PsiLanguageInjectionHost, Set<Trinity<RangeMarker, RangeMarker, SmartPsiElementPointer>>> map = ContainerUtil.classify(
            myMarkers.iterator(),
            o -> {
                PsiElement element = o.third.getElement();
                return (PsiLanguageInjectionHost)element;
            }
        );
        PsiDocumentManager documentManager = PsiDocumentManager.getInstance(myProject);
        documentManager.commitDocument(myOrigDocument); // commit here and after each manipulator update
        int localInsideFileCursor = 0;
        for (PsiLanguageInjectionHost host : map.keySet()) {
            if (host == null) {
                continue;
            }
            String hostText = host.getText();
            ProperTextRange insideHost = null;
            StringBuilder sb = new StringBuilder();
            for (Trinity<RangeMarker, RangeMarker, SmartPsiElementPointer> entry : map.get(host)) {
                RangeMarker origMarker = entry.first; // check for validity?
                int hostOffset = host.getTextRange().getStartOffset();
                ProperTextRange localInsideHost =
                    new ProperTextRange(origMarker.getStartOffset() - hostOffset, origMarker.getEndOffset() - hostOffset);
                RangeMarker rangeMarker = entry.second;
                ProperTextRange localInsideFile =
                    new ProperTextRange(Math.max(localInsideFileCursor, rangeMarker.getStartOffset()), rangeMarker.getEndOffset());
                if (insideHost != null) {
                    //append unchanged inter-markers fragment
                    sb.append(hostText.substring(insideHost.getEndOffset(), localInsideHost.getStartOffset()));
                }
                sb.append(localInsideFile.getEndOffset() <= text.length() && !localInsideFile.isEmpty() ? localInsideFile.substring(text) : "");
                localInsideFileCursor = localInsideFile.getEndOffset();
                insideHost = insideHost == null ? localInsideHost : insideHost.union(localInsideHost);
            }
            assert insideHost != null;
            ElementManipulators.getManipulator(host).handleContentChange(host, insideHost, sb.toString());
            documentManager.commitDocument(myOrigDocument);
        }
    }

    private void altCommitToOriginal(@Nonnull DocumentEvent e) {
        PsiFile origPsiFile = PsiDocumentManager.getInstance(myProject).getPsiFile(myOrigDocument);
        String newText = myNewDocument.getText();
        // prepare guarded blocks
        Map<String, String> replacementMap = new LinkedHashMap<>();
        int count = 0;
        for (RangeMarker o : ContainerUtil.reverse(((DocumentEx)myNewDocument).getGuardedBlocks())) {
            String replacement = o.getUserData(REPLACEMENT_KEY);
            String tempText = "REPLACE" + (count++) + Long.toHexString(StringHash.calc(replacement));
            newText = newText.substring(0, o.getStartOffset()) + tempText + newText.substring(o.getEndOffset());
            replacementMap.put(tempText, replacement);
        }
        // run preformat processors
        int hostStartOffset = myAltFullRange.getStartOffset();
        myEditor.getCaretModel().moveToOffset(hostStartOffset);
        for (CopyPastePreProcessor preProcessor : CopyPastePreProcessor.EP_NAME.getExtensionList()) {
            newText = preProcessor.preprocessOnPaste(myProject, origPsiFile, myEditor, newText, null);
        }
        myOrigDocument.replaceString(hostStartOffset, myAltFullRange.getEndOffset(), newText);
        // replace temp strings for guarded blocks
        for (String tempText : replacementMap.keySet()) {
            int idx = CharArrayUtil.indexOf(myOrigDocument.getCharsSequence(), tempText, hostStartOffset, myAltFullRange.getEndOffset());
            myOrigDocument.replaceString(idx, idx + tempText.length(), replacementMap.get(tempText));
        }
        // JAVA: fix occasional char literal concatenation
        fixDocumentQuotes(myOrigDocument, hostStartOffset - 1);
        fixDocumentQuotes(myOrigDocument, myAltFullRange.getEndOffset());

        // reformat
        PsiDocumentManager.getInstance(myProject).commitDocument(myOrigDocument);
        Runnable task = () -> {
            try {
                CodeStyleManager.getInstance(myProject)
                    .reformatRange(origPsiFile, hostStartOffset, myAltFullRange.getEndOffset(), true);
            }
            catch (IncorrectOperationException e1) {
                //LOG.error(e);
            }
        };
        DocumentUtil.executeInBulk(myOrigDocument, true, task);

        PsiElement newInjected = InjectedLanguageManager.getInstance(myProject).findInjectedElementAt(origPsiFile, hostStartOffset);
        DocumentWindow documentWindow = newInjected == null ? null : InjectedLanguageUtil.getDocumentWindow(newInjected);
        if (documentWindow != null) {
            myEditor.getCaretModel().moveToOffset(documentWindow.injectedToHost(e.getOffset()));
            myEditor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
        }
    }

    private static String fixQuotes(String padding) {
        if (padding.isEmpty()) {
            return padding;
        }
        if (padding.startsWith("'")) {
            padding = '\"' + padding.substring(1);
        }
        if (padding.endsWith("'")) {
            padding = padding.substring(0, padding.length() - 1) + "\"";
        }
        return padding;
    }

    private static void fixDocumentQuotes(Document doc, int offset) {
        if (doc.getCharsSequence().charAt(offset) == '\'') {
            doc.replaceString(offset, offset + 1, "\"");
        }
    }

    @Override
    public void dispose() {
        // noop
    }

    @TestOnly
    public PsiFile getNewFile() {
        return myNewFile;
    }

    public boolean changesRange(TextRange range) {
        if (myAltFullRange != null) {
            return range.intersects(myAltFullRange.getStartOffset(), myAltFullRange.getEndOffset());
        }
        else if (!myMarkers.isEmpty()) {
            TextRange hostRange =
                TextRange.create(myMarkers.get(0).first.getStartOffset(), myMarkers.get(myMarkers.size() - 1).first.getEndOffset());
            return range.intersects(hostRange);
        }
        return false;
    }

    private static class MyQuietHandler implements ReadonlyFragmentModificationHandler {
        @Override
        public void handle(ReadOnlyFragmentModificationException e) {
            //nothing
        }
    }
}
