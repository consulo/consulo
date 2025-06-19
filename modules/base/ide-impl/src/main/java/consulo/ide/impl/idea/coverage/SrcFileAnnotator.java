/*
 * Copyright (c) 2000-2006 JetBrains s.r.o. All Rights Reserved.
 */

package consulo.ide.impl.idea.coverage;

import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.LineCoverage;
import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.ProjectData;
import consulo.application.util.LineTokenizer;
import consulo.application.util.diff.Diff;
import consulo.application.util.diff.FilesTooBigForDiffException;
import consulo.codeEditor.DocumentMarkupModel;
import consulo.codeEditor.Editor;
import consulo.codeEditor.markup.HighlighterLayer;
import consulo.codeEditor.markup.HighlighterTargetArea;
import consulo.codeEditor.markup.MarkupModel;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.disposer.Disposable;
import consulo.document.Document;
import consulo.document.event.DocumentAdapter;
import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.document.util.TextRange;
import consulo.execution.coverage.CoverageDataManager;
import consulo.execution.coverage.CoverageEngine;
import consulo.execution.coverage.CoverageSuite;
import consulo.execution.coverage.CoverageSuitesBundle;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.TextEditor;
import consulo.ide.impl.execution.coverage.CoverageLineMarkerRenderer;
import consulo.ide.impl.idea.openapi.module.ModuleUtil;
import consulo.ide.impl.idea.reference.SoftReference;
import consulo.ide.impl.idea.ui.EditorNotificationPanel;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.psi.PsiFile;
import consulo.localHistory.LocalHistory;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.dataholder.Key;
import consulo.util.lang.ref.SimpleReference;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.internal.LoadTextUtil;
import gnu.trove.TIntIntHashMap;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author ven
 */
public class SrcFileAnnotator implements Disposable {
    private static final Logger LOG = Logger.getInstance(SrcFileAnnotator.class);
    public static final Key<List<RangeHighlighter>> COVERAGE_HIGHLIGHTERS = Key.create("COVERAGE_HIGHLIGHTERS");
    private static final Key<DocumentListener> COVERAGE_DOCUMENT_LISTENER = Key.create("COVERAGE_DOCUMENT_LISTENER");
    public static final Key<Map<FileEditor, EditorNotificationPanel>> NOTIFICATION_PANELS = Key.create("NOTIFICATION_PANELS");

    private PsiFile myFile;
    private Editor myEditor;
    private Document myDocument;
    private final Project myProject;

    private SoftReference<TIntIntHashMap> myNewToOldLines;
    private SoftReference<TIntIntHashMap> myOldToNewLines;
    private SoftReference<byte[]> myOldContent;
    private final static Object LOCK = new Object();

    private final Alarm myUpdateAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, this);

    public SrcFileAnnotator(PsiFile file, Editor editor) {
        myFile = file;
        myEditor = editor;
        myProject = file.getProject();
        myDocument = myEditor.getDocument();
    }

    public void hideCoverageData() {
        if (myEditor == null) {
            return;
        }
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(myProject);
        List<RangeHighlighter> highlighters = myEditor.getUserData(COVERAGE_HIGHLIGHTERS);
        if (highlighters != null) {
            for (RangeHighlighter highlighter : highlighters) {
                myProject.getApplication().invokeLater(highlighter::dispose);
            }
            myEditor.putUserData(COVERAGE_HIGHLIGHTERS, null);
        }

        Map<FileEditor, EditorNotificationPanel> map = myFile.getCopyableUserData(NOTIFICATION_PANELS);
        if (map != null) {
            VirtualFile vFile = myFile.getVirtualFile();
            LOG.assertTrue(vFile != null);
            boolean freeAll = !fileEditorManager.isFileOpen(vFile);
            myFile.putCopyableUserData(NOTIFICATION_PANELS, null);
            for (FileEditor fileEditor : map.keySet()) {
                if (!freeAll && !isCurrentEditor(fileEditor)) {
                    continue;
                }
                fileEditorManager.removeTopComponent(fileEditor, map.get(fileEditor));
            }
        }

        DocumentListener documentListener = myEditor.getUserData(COVERAGE_DOCUMENT_LISTENER);
        if (documentListener != null) {
            myDocument.removeDocumentListener(documentListener);
            myEditor.putUserData(COVERAGE_DOCUMENT_LISTENER, null);
        }
    }

    private static
    @Nonnull
    String[] getCoveredLines(@Nonnull byte[] oldContent, VirtualFile vFile) {
        String text = LoadTextUtil.getTextByBinaryPresentation(oldContent, vFile, false, false).toString();
        return LineTokenizer.tokenize(text, false);
    }

    private
    @Nonnull
    String[] getUpToDateLines() {
        SimpleReference<String[]> linesRef = new SimpleReference<>();
        Runnable runnable = () -> {
            int lineCount = myDocument.getLineCount();
            String[] lines = new String[lineCount];
            CharSequence chars = myDocument.getCharsSequence();
            for (int i = 0; i < lineCount; i++) {
                lines[i] = chars.subSequence(myDocument.getLineStartOffset(i), myDocument.getLineEndOffset(i)).toString();
            }
            linesRef.set(lines);
        };
        myProject.getApplication().runReadAction(runnable);

        return linesRef.get();
    }

    private static TIntIntHashMap getCoverageVersionToCurrentLineMapping(Diff.Change change, int firstNLines) {
        TIntIntHashMap result = new TIntIntHashMap();
        int prevLineInFirst = 0;
        int prevLineInSecond = 0;
        while (change != null) {

            for (int l = 0; l < change.line0 - prevLineInFirst; l++) {
                result.put(prevLineInFirst + l, prevLineInSecond + l);
            }

            prevLineInFirst = change.line0 + change.deleted;
            prevLineInSecond = change.line1 + change.inserted;

            change = change.link;
        }

        for (int i = prevLineInFirst; i < firstNLines; i++) {
            result.put(i, prevLineInSecond + i - prevLineInFirst);
        }

        return result;
    }

    @Nullable
    private TIntIntHashMap getOldToNewLineMapping(long date) {
        if (myOldToNewLines == null) {
            myOldToNewLines = doGetLineMapping(date, true);
            if (myOldToNewLines == null) {
                return null;
            }
        }
        return myOldToNewLines.get();
    }

    @Nullable
    private TIntIntHashMap getNewToOldLineMapping(long date) {
        if (myNewToOldLines == null) {
            myNewToOldLines = doGetLineMapping(date, false);
            if (myNewToOldLines == null) {
                return null;
            }
        }
        return myNewToOldLines.get();
    }

    @Nullable
    private SoftReference<TIntIntHashMap> doGetLineMapping(long date, boolean oldToNew) {
        VirtualFile f = getVirtualFile();
        byte[] oldContent;
        synchronized (LOCK) {
            if (myOldContent == null) {
                if (myProject.getApplication().isDispatchThread()) {
                    return null;
                }
                byte[] byteContent = LocalHistory.getInstance().getByteContent(f, revisionTimestamp -> revisionTimestamp < date);
                myOldContent = new SoftReference<>(byteContent);
            }
            oldContent = myOldContent.get();
        }

        if (oldContent == null) {
            return null;
        }
        String[] coveredLines = getCoveredLines(oldContent, f);
        String[] currentLines = getUpToDateLines();

        String[] oldLines = oldToNew ? coveredLines : currentLines;
        String[] newLines = oldToNew ? currentLines : coveredLines;

        Diff.Change change;
        try {
            change = Diff.buildChanges(oldLines, newLines);
        }
        catch (FilesTooBigForDiffException e) {
            LOG.info(e);
            return null;
        }
        return new SoftReference<>(getCoverageVersionToCurrentLineMapping(change, oldLines.length));
    }

    public void showCoverageInformation(final CoverageSuitesBundle suite) {
        if (myEditor == null) {
            return;
        }
        final MarkupModel markupModel = DocumentMarkupModel.forDocument(myDocument, myProject, true);
        final List<RangeHighlighter> highlighters = new ArrayList<>();
        final ProjectData data = suite.getCoverageData();
        if (data == null) {
            coverageDataNotFound(suite);
            return;
        }
        final CoverageEngine engine = suite.getCoverageEngine();
        Set<String> qualifiedNames = engine.getQualifiedNames(myFile);

        // let's find old content in local history and build mapping from old lines to new one
        // local history doesn't index libraries, so let's distinguish libraries content with other one
        ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(myProject).getFileIndex();
        VirtualFile file = getVirtualFile();

        long fileTimeStamp = file.getTimeStamp();
        long coverageTimeStamp = suite.getLastCoverageTimeStamp();
        final TIntIntHashMap oldToNewLineMapping;

        //do not show coverage info over cls
        if (engine.isInLibraryClasses(myProject, file)) {
            return;
        }
        // if in libraries content
        if (projectFileIndex.isInLibrarySource(file)) {
            // compare file and coverage timestamps
            if (fileTimeStamp > coverageTimeStamp) {
                showEditorWarningMessage(CodeInsightLocalize.coverageDataOutdated());
                return;
            }
            oldToNewLineMapping = null;
        }
        else {
            // check local history
            oldToNewLineMapping = getOldToNewLineMapping(coverageTimeStamp);
            if (oldToNewLineMapping == null) {

                // if history for file isn't available let's check timestamps
                if (fileTimeStamp > coverageTimeStamp && classesArePresentInCoverageData(data, qualifiedNames)) {
                    showEditorWarningMessage(CodeInsightLocalize.coverageDataOutdated());
                    return;
                }
            }
        }

        if (myEditor.getUserData(COVERAGE_HIGHLIGHTERS) != null) {
            //highlighters already collected - no need to do it twice
            return;
        }

        Module module = myProject.getApplication().runReadAction((Supplier<Module>) () -> ModuleUtil.findModuleForPsiElement(myFile));
        if (module != null) {
            if (engine.recompileProjectAndRerunAction(
                module,
                suite,
                () -> CoverageDataManager.getInstance(myProject).chooseSuitesBundle(suite)
            )) {
                return;
            }
        }

        // now if oldToNewLineMapping is null we should use f(x)=id(x) mapping

        // E.g. all *.class files for java source file with several classes
        Set<File> outputFiles = engine.getCorrespondingOutputFiles(myFile, module, suite);

        final boolean subCoverageActive = CoverageDataManager.getInstance(myProject).isSubCoverageActive();
        final boolean coverageByTestApplicable =
            suite.isCoverageByTestApplicable() && !(subCoverageActive && suite.isCoverageByTestEnabled());
        final SortedMap<Integer, LineData> executableLines = new TreeMap<>();
        final SortedMap<Integer, Object[]> classLines = new TreeMap<>();
        final SortedMap<Integer, String> classNames = new TreeMap<>();
        class HighlightersCollector {
            private void collect(File outputFile, String qualifiedName) {
                ClassData fileData = data.getClassData(qualifiedName);
                if (fileData != null) {
                    Object[] lines = fileData.getLines();
                    if (lines != null) {
                        Object[] postProcessedLines = suite.getCoverageEngine().postProcessExecutableLines(lines, myEditor);
                        for (Object lineDataRaw : postProcessedLines) {
                            if (lineDataRaw instanceof LineData lineData) {
                                int line = lineData.getLineNumber() - 1;
                                int lineNumberInCurrent;
                                if (oldToNewLineMapping != null) {
                                    // use mapping based on local history
                                    if (!oldToNewLineMapping.contains(line)) {
                                        continue;
                                    }
                                    lineNumberInCurrent = oldToNewLineMapping.get(line);
                                }
                                else {
                                    // use id mapping
                                    lineNumberInCurrent = line;
                                }
                                LOG.assertTrue(lineNumberInCurrent < myDocument.getLineCount());
                                executableLines.put(line, lineData);

                                classLines.put(line, postProcessedLines);
                                classNames.put(line, qualifiedName);

                                myProject.getApplication().invokeLater(() -> {
                                    if (myDocument == null || lineNumberInCurrent >= myDocument.getLineCount()) {
                                        return;
                                    }
                                    RangeHighlighter highlighter = createRangeHighlighter(
                                        suite.getLastCoverageTimeStamp(),
                                        markupModel,
                                        coverageByTestApplicable,
                                        executableLines,
                                        qualifiedName,
                                        line,
                                        lineNumberInCurrent,
                                        suite,
                                        postProcessedLines
                                    );
                                    highlighters.add(highlighter);
                                });
                            }
                        }
                    }
                }
                else if (outputFile != null &&
                    !subCoverageActive &&
                    engine.includeUntouchedFileInCoverage(qualifiedName, outputFile, myFile, suite)) {
                    collectNonCoveredFileInfo(outputFile, highlighters, markupModel, executableLines, coverageByTestApplicable);
                }
            }
        }

        HighlightersCollector collector = new HighlightersCollector();
        if (!outputFiles.isEmpty()) {
            for (File outputFile : outputFiles) {
                String qualifiedName = engine.getQualifiedName(outputFile, myFile);
                if (qualifiedName != null) {
                    collector.collect(outputFile, qualifiedName);
                }
            }
        }
        else { //check non-compilable classes which present in ProjectData
            for (String qName : qualifiedNames) {
                collector.collect(null, qName);
            }
        }
        myProject.getApplication().invokeLater(() -> {
            if (myEditor != null && highlighters.size() > 0) {
                myEditor.putUserData(COVERAGE_HIGHLIGHTERS, highlighters);
            }
        });

        DocumentListener documentListener = new DocumentAdapter() {
            @Override
            public void documentChanged(DocumentEvent e) {
                myNewToOldLines = null;
                myOldToNewLines = null;
                List<RangeHighlighter> rangeHighlighters = myEditor.getUserData(COVERAGE_HIGHLIGHTERS);
                if (rangeHighlighters == null) {
                    rangeHighlighters = new ArrayList<>();
                }
                int offset = e.getOffset();
                int lineNumber = myDocument.getLineNumber(offset);
                int lastLineNumber = myDocument.getLineNumber(offset + e.getNewLength());
                TextRange changeRange =
                    new TextRange(myDocument.getLineStartOffset(lineNumber), myDocument.getLineEndOffset(lastLineNumber));
                for (Iterator<RangeHighlighter> it = rangeHighlighters.iterator(); it.hasNext(); ) {
                    RangeHighlighter highlighter = it.next();
                    if (!highlighter.isValid() || TextRange.create(highlighter).intersects(changeRange)) {
                        highlighter.dispose();
                        it.remove();
                    }
                }
                List<RangeHighlighter> highlighters = rangeHighlighters;
                myUpdateAlarm.cancelAllRequests();
                if (!myUpdateAlarm.isDisposed()) {
                    myUpdateAlarm.addRequest(
                        () -> {
                            TIntIntHashMap newToOldLineMapping = getNewToOldLineMapping(suite.getLastCoverageTimeStamp());
                            if (newToOldLineMapping != null) {
                                myProject.getApplication().invokeLater(() -> {
                                    if (myEditor == null) {
                                        return;
                                    }
                                    for (int line = lineNumber; line <= lastLineNumber; line++) {
                                        int oldLineNumber = newToOldLineMapping.get(line);
                                        LineData lineData = executableLines.get(oldLineNumber);
                                        if (lineData != null) {
                                            RangeHighlighter rangeHighlighter =
                                                createRangeHighlighter(
                                                    suite.getLastCoverageTimeStamp(),
                                                    markupModel,
                                                    coverageByTestApplicable,
                                                    executableLines,
                                                    classNames.get(oldLineNumber),
                                                    oldLineNumber,
                                                    line,
                                                    suite,
                                                    classLines.get(oldLineNumber)
                                                );
                                            highlighters.add(rangeHighlighter);
                                        }
                                    }
                                    myEditor.putUserData(COVERAGE_HIGHLIGHTERS, highlighters.size() > 0 ? highlighters : null);
                                });
                            }
                        },
                        100
                    );
                }
            }
        };
        myDocument.addDocumentListener(documentListener);
        myEditor.putUserData(COVERAGE_DOCUMENT_LISTENER, documentListener);
    }

    private static boolean classesArePresentInCoverageData(ProjectData data, Set<String> qualifiedNames) {
        for (String qualifiedName : qualifiedNames) {
            if (data.getClassData(qualifiedName) != null) {
                return true;
            }
        }
        return false;
    }

    private RangeHighlighter createRangeHighlighter(
        long date,
        MarkupModel markupModel,
        boolean coverageByTestApplicable,
        SortedMap<Integer, LineData> executableLines,
        @Nullable String className,
        int line,
        int lineNumberInCurrent,
        @Nonnull CoverageSuitesBundle coverageSuite,
        Object[] lines
    ) {
        EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
        TextAttributes attributes = scheme.getAttributes(CoverageLineMarkerRenderer.getAttributesKey(line, executableLines));
        TextAttributes textAttributes = null;
        if (attributes.getBackgroundColor() != null) {
            textAttributes = attributes;
        }
        int startOffset = myDocument.getLineStartOffset(lineNumberInCurrent);
        int endOffset = myDocument.getLineEndOffset(lineNumberInCurrent);
        RangeHighlighter highlighter = markupModel.addRangeHighlighter(
            startOffset,
            endOffset,
            HighlighterLayer.SELECTION - 1,
            textAttributes,
            HighlighterTargetArea.LINES_IN_RANGE
        );
        Function<Integer, Integer> newToOldConverter = newLine -> {
            if (myEditor == null) {
                return -1;
            }
            TIntIntHashMap oldLineMapping = getNewToOldLineMapping(date);
            return oldLineMapping != null ? oldLineMapping.get(newLine) : newLine;
        };
        Function<Integer, Integer> oldToNewConverter = newLine -> {
            if (myEditor == null) {
                return -1;
            }
            TIntIntHashMap newLineMapping = getOldToNewLineMapping(date);
            return newLineMapping != null ? newLineMapping.get(newLine) : newLine;
        };
        CoverageLineMarkerRenderer markerRenderer = CoverageLineMarkerRenderer.getRenderer(
            line,
            className,
            executableLines,
            coverageByTestApplicable,
            coverageSuite,
            newToOldConverter,
            oldToNewConverter,
            CoverageDataManager.getInstance(myProject).isSubCoverageActive()
        );
        highlighter.setLineMarkerRenderer(markerRenderer);

        LineData lineData = className != null ? (LineData) lines[line + 1] : null;
        if (lineData != null && lineData.getStatus() == LineCoverage.NONE) {
            highlighter.setErrorStripeMarkColor(markerRenderer.getErrorStripeColor(myEditor));
            highlighter.setThinErrorStripeMark(true);
            highlighter.setGreedyToLeft(true);
            highlighter.setGreedyToRight(true);
        }
        return highlighter;
    }

    private void showEditorWarningMessage(@Nonnull LocalizeValue message) {
        myProject.getApplication().invokeLater(() -> {
            if (myEditor == null) {
                return;
            }
            FileEditorManager fileEditorManager = FileEditorManager.getInstance(myProject);
            VirtualFile vFile = myFile.getVirtualFile();
            assert vFile != null;
            Map<FileEditor, EditorNotificationPanel> map = myFile.getCopyableUserData(NOTIFICATION_PANELS);
            if (map == null) {
                map = new HashMap<>();
                myFile.putCopyableUserData(NOTIFICATION_PANELS, map);
            }

            FileEditor[] editors = fileEditorManager.getAllEditors(vFile);
            for (FileEditor editor : editors) {
                if (isCurrentEditor(editor)) {
                    EditorNotificationPanel panel = new EditorNotificationPanel() {
                        {
                            myLabel.setIcon(TargetAWT.to(PlatformIconGroup.generalExclmark()));
                            myLabel.setText(message.get());
                        }
                    };
                    panel.createActionLabel("Close", () -> fileEditorManager.removeTopComponent(editor, panel));
                    map.put(editor, panel);
                    fileEditorManager.addTopComponent(editor, panel);
                    break;
                }
            }
        });
    }

    private boolean isCurrentEditor(FileEditor editor) {
        return editor instanceof TextEditor && ((TextEditor) editor).getEditor() == myEditor;
    }

    private void collectNonCoveredFileInfo(
        File outputFile,
        List<RangeHighlighter> highlighters,
        MarkupModel markupModel,
        SortedMap<Integer, LineData> executableLines,
        boolean coverageByTestApplicable
    ) {
        CoverageSuitesBundle coverageSuite = CoverageDataManager.getInstance(myProject).getCurrentSuitesBundle();
        if (coverageSuite == null) {
            return;
        }
        TIntIntHashMap mapping;
        if (outputFile.lastModified() < getVirtualFile().getTimeStamp()) {
            mapping = getOldToNewLineMapping(outputFile.lastModified());
            if (mapping == null) {
                return;
            }
        }
        else {
            mapping = null;
        }


        List<Integer> uncoveredLines = coverageSuite.getCoverageEngine().collectSrcLinesForUntouchedFile(outputFile, coverageSuite);

        int lineCount = myDocument.getLineCount();
        if (uncoveredLines == null) {
            for (int lineNumber = 0; lineNumber < lineCount; lineNumber++) {
                addHighlighter(
                    outputFile,
                    highlighters,
                    markupModel,
                    executableLines,
                    coverageByTestApplicable,
                    coverageSuite,
                    lineNumber,
                    lineNumber
                );
            }
        }
        else {
            for (int lineNumber : uncoveredLines) {
                if (lineNumber >= lineCount) {
                    continue;
                }

                int updatedLineNumber = mapping != null ? mapping.get(lineNumber) : lineNumber;

                addHighlighter(
                    outputFile,
                    highlighters,
                    markupModel,
                    executableLines,
                    coverageByTestApplicable,
                    coverageSuite,
                    lineNumber,
                    updatedLineNumber
                );
            }
        }
    }

    private void addHighlighter(
        File outputFile,
        List<RangeHighlighter> highlighters,
        MarkupModel markupModel,
        SortedMap<Integer, LineData> executableLines,
        boolean coverageByTestApplicable,
        CoverageSuitesBundle coverageSuite,
        int lineNumber,
        int updatedLineNumber
    ) {
        executableLines.put(updatedLineNumber, null);
        myProject.getApplication().invokeLater(() -> {
            if (myEditor == null) {
                return;
            }
            RangeHighlighter highlighter = createRangeHighlighter(
                outputFile.lastModified(),
                markupModel,
                coverageByTestApplicable,
                executableLines,
                null,
                lineNumber,
                updatedLineNumber,
                coverageSuite,
                null
            );
            highlighters.add(highlighter);
        });
    }

    private VirtualFile getVirtualFile() {
        VirtualFile vFile = myFile.getVirtualFile();
        LOG.assertTrue(vFile != null);
        return vFile;
    }

    private void coverageDataNotFound(CoverageSuitesBundle suite) {
        showEditorWarningMessage(CodeInsightLocalize.coverageDataNotFound());
        for (CoverageSuite coverageSuite : suite.getSuites()) {
            CoverageDataManager.getInstance(myProject).removeCoverageSuite(coverageSuite);
        }
    }

    @Override
    public void dispose() {
        hideCoverageData();
        myEditor = null;
        myDocument = null;
        myFile = null;
    }
}
