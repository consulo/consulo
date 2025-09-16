// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.folding.impl;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.application.util.registry.Registry;
import consulo.codeEditor.Editor;
import consulo.codeEditor.FoldingModel;
import consulo.document.Document;
import consulo.document.DocumentWindow;
import consulo.document.RangeMarker;
import consulo.document.impl.DocumentImpl;
import consulo.document.internal.DocumentEx;
import consulo.document.util.TextRange;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.language.Language;
import consulo.language.editor.folding.FoldingBuilder;
import consulo.language.editor.folding.FoldingDescriptor;
import consulo.language.editor.folding.LanguageFolding;
import consulo.language.editor.inject.EditorWindow;
import consulo.language.file.FileViewProvider;
import consulo.language.impl.DebugUtil;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.inject.impl.internal.InjectedLanguageUtil;
import consulo.language.psi.*;
import consulo.language.util.AttachmentFactoryUtil;
import consulo.logging.Logger;
import consulo.logging.attachment.Attachment;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.util.collection.ArrayUtil;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class FoldingUpdate {
    private static final Logger LOG = Logger.getInstance(FoldingUpdate.class);

    private static final Key<CachedValue<Runnable>> CODE_FOLDING_KEY = Key.create("code folding");

    private FoldingUpdate() {
    }

    @Nullable
    static Runnable updateFoldRegions(@Nonnull Editor editor, @Nonnull PsiFile file, boolean applyDefaultState, boolean quick) {
        ApplicationManager.getApplication().assertReadAccessAllowed();

        Project project = file.getProject();
        Document document = editor.getDocument();
        LOG.assertTrue(!PsiDocumentManager.getInstance(project).isUncommited(document));

        CachedValue<Runnable> value = editor.getUserData(CODE_FOLDING_KEY);

        if (value != null && !applyDefaultState) {
            Supplier<Runnable> cached = value.getUpToDateOrNull();
            if (cached != null) {
                return cached.get();
            }
        }
        if (quick || applyDefaultState) {
            return getUpdateResult(file, document, quick, project, editor, applyDefaultState).getValue();
        }

        return CachedValuesManager.getManager(project).getCachedValue(editor, CODE_FOLDING_KEY, () -> {
            PsiFile file1 = PsiDocumentManager.getInstance(project).getPsiFile(document);
            return getUpdateResult(file1, document, false, project, editor, false);
        }, false);
    }

    private static CachedValueProvider.Result<Runnable> getUpdateResult(
        PsiFile file,
        @Nonnull Document document,
        boolean quick,
        Project project,
        Editor editor,
        boolean applyDefaultState
    ) {

        List<RegionInfo> elementsToFold = getFoldingsFor(file, document, quick);
        UpdateFoldRegionsOperation operation = new UpdateFoldRegionsOperation(
            project,
            editor,
            file,
            elementsToFold,
            applyDefaultStateMode(applyDefaultState),
            !applyDefaultState,
            false
        );
        int documentLength = document.getTextLength();
        AtomicBoolean alreadyExecuted = new AtomicBoolean();
        Runnable runnable = () -> {
            if (alreadyExecuted.compareAndSet(false, true)) {
                int curLength = editor.getDocument().getTextLength();
                boolean committed = PsiDocumentManager.getInstance(project).isCommitted(document);
                if (documentLength != curLength || !committed) {
                    LOG.error("Document has changed since fold regions were calculated: " + "lengths " + documentLength + " vs " + curLength + ", " + "document=" + document + ", " + "committed=" + committed);
                }
                editor.getFoldingModel().runBatchFoldingOperationDoNotCollapseCaret(operation);
            }
        };
        Set<Object> dependencies = new HashSet<>();
        dependencies.add(file);
        dependencies.add(editor.getFoldingModel());
        for (RegionInfo info : elementsToFold) {
            dependencies.addAll(info.descriptor.getDependencies());
        }
        return CachedValueProvider.Result.create(runnable, ArrayUtil.toObjectArray(dependencies));
    }

    @Nonnull
    private static UpdateFoldRegionsOperation.ApplyDefaultStateMode applyDefaultStateMode(boolean applyDefaultState) {
        return applyDefaultState ? UpdateFoldRegionsOperation.ApplyDefaultStateMode.EXCEPT_CARET_REGION : UpdateFoldRegionsOperation.ApplyDefaultStateMode.NO;
    }

    private static final Key<Object> LAST_UPDATE_INJECTED_STAMP_KEY = Key.create("LAST_UPDATE_INJECTED_STAMP_KEY");

    @Nullable
    @RequiredReadAction
    public static Runnable updateInjectedFoldRegions(@Nonnull Editor editor, @Nonnull PsiFile file, boolean applyDefaultState) {
        if (file instanceof PsiCompiledElement) {
            return null;
        }
        Application.get().assertReadAccessAllowed();

        Project project = file.getProject();
        Document document = editor.getDocument();
        LOG.assertTrue(!PsiDocumentManager.getInstance(project).isUncommited(document));
        FoldingModel foldingModel = editor.getFoldingModel();

        long timeStamp = document.getModificationStamp();
        if (editor.getUserData(LAST_UPDATE_INJECTED_STAMP_KEY) instanceof Long longLastTimeStamp
            && longLastTimeStamp.longValue() == timeStamp) {
            return null;
        }

        List<DocumentWindow> injectedDocuments =
            InjectedLanguageManager.getInstance(project).getCachedInjectedDocumentsInRange(file, file.getTextRange());
        if (injectedDocuments.isEmpty()) {
            return null;
        }
        List<EditorWindow> injectedEditors = new ArrayList<>();
        List<PsiFile> injectedFiles = new ArrayList<>();
        List<List<RegionInfo>> lists = new ArrayList<>();
        for (DocumentWindow injectedDocument : injectedDocuments) {
            if (!injectedDocument.isValid()) {
                continue;
            }
            InjectedLanguageUtil.enumerate(
                injectedDocument,
                file,
                (injectedFile, places) -> {
                    if (!injectedFile.isValid()) {
                        return;
                    }
                    Editor injectedEditor = InjectedLanguageUtil.getInjectedEditorForInjectedFile(editor, injectedFile);
                    if (!(injectedEditor instanceof EditorWindow editorWindow)) {
                        return;
                    }

                    injectedEditors.add(editorWindow);
                    injectedFiles.add(injectedFile);
                    List<RegionInfo> map = new ArrayList<>();
                    lists.add(map);
                    getFoldingsFor(injectedFile, injectedEditor.getDocument(), map, false);
                }
            );
        }

        return () -> {
            List<Runnable> updateOperations = new ArrayList<>(injectedEditors.size());
            for (int i = 0; i < injectedEditors.size(); i++) {
                EditorWindow injectedEditor = injectedEditors.get(i);
                PsiFile injectedFile = injectedFiles.get(i);
                if (!injectedEditor.getDocument().isValid()) {
                    continue;
                }
                List<RegionInfo> list = lists.get(i);
                updateOperations.add(new UpdateFoldRegionsOperation(
                    project,
                    injectedEditor,
                    injectedFile,
                    list,
                    applyDefaultStateMode(applyDefaultState),
                    !applyDefaultState,
                    true
                ));
            }
            foldingModel.runBatchFoldingOperation(() -> {
                for (Runnable operation : updateOperations) {
                    operation.run();
                }
            });

            editor.putUserData(LAST_UPDATE_INJECTED_STAMP_KEY, timeStamp);
        };
    }

    /**
     * Checks the ability to initialize folding in the Dumb Mode. Due to language injections it may depend on
     * edited file and active injections (not yet implemented).
     *
     * @param editor the editor that holds file view
     * @return true  if folding initialization available in the Dumb Mode
     */
    public static boolean supportsDumbModeFolding(@Nonnull Editor editor) {
        Project project = editor.getProject();
        if (project != null) {
            PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
            if (file != null) {
                return supportsDumbModeFolding(file);
            }
        }
        return true;
    }

    /**
     * Checks the ability to initialize folding in the Dumb Mode for file.
     *
     * @param file the file to test
     * @return true  if folding initialization available in the Dumb Mode
     */
    private static boolean supportsDumbModeFolding(@Nonnull PsiFile file) {
        FileViewProvider viewProvider = file.getViewProvider();
        for (Language language : viewProvider.getLanguages()) {
            FoldingBuilder foldingBuilder = FoldingBuilder.forLanguageComposite(language);
            if (foldingBuilder != null && !DumbService.isDumbAware(foldingBuilder)) {
                return false;
            }
        }
        return true;
    }

    static List<RegionInfo> getFoldingsFor(@Nonnull PsiFile file, @Nonnull Document document, boolean quick) {
        if (file instanceof PsiCompiledFile) {
            file = ((PsiCompiledFile) file).getDecompiledPsiFile();
        }
        List<RegionInfo> foldingMap = new ArrayList<>();
        getFoldingsFor(file, document, foldingMap, quick);
        return foldingMap;
    }

    private static void getFoldingsFor(
        @Nonnull PsiFile file,
        @Nonnull Document document,
        @Nonnull List<? super RegionInfo> elementsToFold,
        boolean quick
    ) {
        FileViewProvider viewProvider = file.getViewProvider();
        TextRange docRange = TextRange.from(0, document.getTextLength());
        Comparator<Language> preferBaseLanguage = Comparator.comparing((Language l) -> l != viewProvider.getBaseLanguage());
        List<Language> languages = ContainerUtil.sorted(viewProvider.getLanguages(), preferBaseLanguage.thenComparing(Language::getID));

        DocumentEx copyDoc = languages.size() > 1 ? new DocumentImpl(document.getImmutableCharSequence()) : null;
        List<RangeMarker> hardRefToRangeMarkers = new ArrayList<>();

        for (Language language : languages) {
            PsiFile psi = viewProvider.getPsi(language);
            FoldingBuilder foldingBuilder = FoldingBuilder.forLanguageComposite(language);
            if (psi != null && foldingBuilder != null) {
                for (FoldingDescriptor descriptor : LanguageFolding.buildFoldingDescriptors(foldingBuilder, psi, document, quick)) {
                    PsiElement psiElement = descriptor.getElement().getPsi();
                    if (psiElement == null) {
                        LOG.error("No PSI for folding descriptor " + descriptor);
                        continue;
                    }
                    TextRange range = descriptor.getRange();
                    if (!docRange.contains(range)) {
                        diagnoseIncorrectRange(psi, document, language, foldingBuilder, descriptor, psiElement);
                        continue;
                    }

                    if (copyDoc != null && !addNonConflictingRegion(copyDoc, range, hardRefToRangeMarkers)) {
                        continue;
                    }

                    RegionInfo regionInfo = new RegionInfo(descriptor, psiElement, foldingBuilder);
                    elementsToFold.add(regionInfo);
                }
            }
        }
    }

    private static boolean addNonConflictingRegion(DocumentEx document, TextRange range, List<? super RangeMarker> hardRefToRangeMarkers) {
        int start = range.getStartOffset();
        int end = range.getEndOffset();
        if (!document.processRangeMarkersOverlappingWith(start, end, rm -> !areConflicting(range, TextRange.create(rm)))) {
            return false;
        }
        RangeMarker marker = document.createRangeMarker(start, end);
        hardRefToRangeMarkers.add(marker); //prevent immediate GC
        return true;
    }

    private static boolean areConflicting(TextRange range1, TextRange range2) {
        if (range1.equals(range2)) {
            return true;
        }
        if (range1.contains(range2) || range2.contains(range1)) {
            return false;
        }

        TextRange intersection = range1.intersection(range2);
        return intersection != null && !intersection.isEmpty();
    }

    private static void diagnoseIncorrectRange(
        @Nonnull PsiFile file,
        @Nonnull Document document,
        Language language,
        FoldingBuilder foldingBuilder,
        FoldingDescriptor descriptor,
        PsiElement psiElement
    ) {
        String message = "Folding descriptor " +
            descriptor +
            " made by " +
            foldingBuilder +
            " for " +
            language +
            " is outside document range" +
            ", PSI element: " +
            psiElement +
            ", PSI element range: " +
            psiElement.getTextRange() +
            "; " +
            DebugUtil.diagnosePsiDocumentInconsistency(psiElement, document);
        LOG.error(
            message,
            ApplicationManager.getApplication().isInternal() ? new Attachment[]{AttachmentFactoryUtil.createAttachment(document),
                consulo.logging.attachment.AttachmentFactory.get().create(
                    "psiTree.txt",
                    DebugUtil.psiToString(file, false, true)
                )} : Attachment.EMPTY_ARRAY
        );
    }

    static void clearFoldingCache(@Nonnull Editor editor) {
        editor.putUserData(CODE_FOLDING_KEY, null);
    }

    static class RegionInfo {
        @Nonnull
        final FoldingDescriptor descriptor;
        final PsiElement element;
        final String signature;
        final boolean collapsedByDefault;

        private RegionInfo(@Nonnull FoldingDescriptor descriptor, @Nonnull PsiElement psiElement, @Nonnull FoldingBuilder foldingBuilder) {
            this.descriptor = descriptor;
            element = psiElement;
            Boolean hardCoded = descriptor.isCollapsedByDefault();
            collapsedByDefault = hardCoded == null ? FoldingPolicy.isCollapsedByDefault(descriptor, foldingBuilder) : hardCoded;
            signature = createSignature(psiElement);
        }

        private static String createSignature(@Nonnull PsiElement element) {
            String signature = FoldingPolicy.getSignature(element);
            if (signature != null && Registry.is("folding.signature.validation")) {
                PsiFile containingFile = element.getContainingFile();
                PsiElement restoredElement = FoldingPolicy.restoreBySignature(containingFile, signature);
                if (!element.equals(restoredElement)) {
                    StringBuilder trace = new StringBuilder();
                    PsiElement restoredAgain = FoldingPolicy.restoreBySignature(containingFile, signature, trace);
                    LOG.error("element: " +
                        element +
                        "(" +
                        element.getText() +
                        "); restoredElement: " +
                        restoredElement +
                        "; signature: '" +
                        signature +
                        "'; file: " +
                        containingFile +
                        "; injected: " +
                        InjectedLanguageManager.getInstance(element.getProject()).isInjectedFragment(containingFile) +
                        "; languages: " +
                        containingFile.getViewProvider().getLanguages() +
                        "; restored again: " +
                        restoredAgain +
                        "; restore produces same results: " +
                        (restoredAgain == restoredElement) +
                        "; trace:\n" +
                        trace);
                }
            }
            return signature;
        }

        @Override
        public String toString() {
            return descriptor + ", collapsedByDefault=" + collapsedByDefault;
        }
    }
}
