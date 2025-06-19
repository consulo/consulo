package consulo.ide.impl.idea.coverage;

import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.LineCoverage;
import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.ProjectData;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.application.util.function.Computable;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.event.EditorFactoryEvent;
import consulo.codeEditor.event.EditorFactoryListener;
import consulo.colorScheme.event.EditorColorsListener;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.container.boot.ContainerPathManager;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.execution.configuration.RunConfigurationBase;
import consulo.execution.configuration.RunnerSettings;
import consulo.execution.coverage.*;
import consulo.execution.coverage.localize.ExecutionCoverageLocalize;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.TextEditor;
import consulo.ide.impl.idea.coverage.view.CoverageViewSuiteListener;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.localize.CommonLocalize;
import consulo.process.ProcessHandler;
import consulo.process.event.ProcessAdapter;
import consulo.process.event.ProcessEvent;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.event.ProjectManagerAdapter;
import consulo.project.ui.view.ProjectView;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.Alarm;
import consulo.util.collection.ArrayUtil;
import consulo.util.io.FileUtil;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.JDOMExternalizable;
import consulo.util.xml.serializer.WriteExternalException;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;

/**
 * @author ven
 */
@Singleton
@State(name = "CoverageDataManager", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
@ServiceImpl
public class CoverageDataManagerImpl extends CoverageDataManager implements JDOMExternalizable {
    private static final LocalizeValue REPLACE_ACTIVE_SUITES = ExecutionCoverageLocalize.coverageReplaceActiveSuites();
    private static final LocalizeValue ADD_TO_ACTIVE_SUITES = ExecutionCoverageLocalize.coverageAddToActiveSuites();
    private static final LocalizeValue DO_NOT_APPLY_COLLECTED_COVERAGE = ExecutionCoverageLocalize.coverageDoNotApplyCollectedCoverage();

    private final List<CoverageSuiteListener> myListeners = ContainerUtil.createLockFreeCopyOnWriteList();
    private static final Logger LOG = Logger.getInstance(CoverageDataManagerImpl.class);
    private static final String SUITE = "SUITE";

    private final Project myProject;
    private final Set<CoverageSuite> myCoverageSuites = new HashSet<>();
    private boolean myIsProjectClosing = false;

    private final Object myLock = new Object();
    private boolean mySubCoverageIsActive;

    @Override
    public CoverageSuitesBundle getCurrentSuitesBundle() {
        return myCurrentSuitesBundle;
    }

    private CoverageSuitesBundle myCurrentSuitesBundle;

    private final Object ANNOTATORS_LOCK = new Object();
    private final Map<Editor, SrcFileAnnotator> myAnnotators = new HashMap<>();

    @Inject
    public CoverageDataManagerImpl(Project project) {
        myProject = project;
        if (project.isDefault()) {
            return;
        }

        project.getMessageBus().connect().subscribe(EditorColorsListener.class, scheme -> chooseSuitesBundle(myCurrentSuitesBundle));

        EditorFactory.getInstance().addEditorFactoryListener(new CoverageEditorFactoryListener(), myProject);
        ProjectManagerAdapter projectManagerListener = new ProjectManagerAdapter() {
            @Override
            public void projectClosing(@Nonnull Project project) {
                synchronized (myLock) {
                    myIsProjectClosing = true;
                }
            }
        };
        ProjectManager.getInstance().addProjectManagerListener(myProject, projectManagerListener);

        addSuiteListener(new CoverageViewSuiteListener(this, myProject), myProject);
    }

    @Override
    public void readExternal(Element element) throws InvalidDataException {
        //noinspection unchecked
        for (Element suiteElement : element.getChildren(SUITE)) {
            CoverageRunner coverageRunner = BaseCoverageSuite.readRunnerAttribute(suiteElement);
            // skip unknown runners
            if (coverageRunner == null) {
                // collect gc
                CoverageFileProvider fileProvider = BaseCoverageSuite.readDataFileProviderAttribute(suiteElement);
                if (fileProvider.isValid()) {
                    //deleteCachedCoverage(fileProvider.getCoverageDataFilePath());
                }
                continue;
            }

            CoverageSuite suite = myProject.getApplication().getExtensionPoint(CoverageEngine.class).computeSafeIfAny(
                engine -> coverageRunner.acceptsCoverageEngine(engine) ? engine.createEmptyCoverageSuite(coverageRunner) : null
            );
            if (suite != null) {
                try {
                    suite.readExternal(suiteElement);
                    myCoverageSuites.add(suite);
                }
                catch (NumberFormatException e) {
                    //try next suite
                }
            }
        }
    }

    @Override
    public void writeExternal(Element element) throws WriteExternalException {
        for (CoverageSuite coverageSuite : myCoverageSuites) {
            Element suiteElement = new Element(SUITE);
            element.addContent(suiteElement);
            coverageSuite.writeExternal(suiteElement);
        }
    }

    @Override
    @RequiredUIAccess
    public CoverageSuite addCoverageSuite(
        String name,
        CoverageFileProvider fileProvider,
        String[] filters,
        long lastCoverageTimeStamp,
        @Nullable String suiteToMergeWith,
        CoverageRunner coverageRunner,
        boolean collectLineInfo,
        boolean tracingEnabled
    ) {
        CoverageSuite suite = createCoverageSuite(
            coverageRunner,
            name,
            fileProvider,
            filters,
            lastCoverageTimeStamp,
            suiteToMergeWith,
            collectLineInfo,
            tracingEnabled
        );
        if (suiteToMergeWith == null || !name.equals(suiteToMergeWith)) {
            removeCoverageSuite(suite);
        }
        myCoverageSuites.remove(suite); // remove previous instance
        myCoverageSuites.add(suite); // add new instance
        return suite;
    }

    @Override
    public CoverageSuite addExternalCoverageSuite(
        String selectedFileName,
        long timeStamp,
        CoverageRunner coverageRunner,
        CoverageFileProvider fileProvider
    ) {
        CoverageSuite suite = createCoverageSuite(
            coverageRunner,
            selectedFileName,
            fileProvider,
            ArrayUtil.EMPTY_STRING_ARRAY,
            timeStamp,
            null,
            false,
            false
        );
        myCoverageSuites.add(suite);
        return suite;
    }

    @Override
    @RequiredUIAccess
    public CoverageSuite addCoverageSuite(CoverageEnabledConfiguration config) {
        String name = config.getName() + " Coverage Results";
        String covFilePath = config.getCoverageFilePath();
        assert covFilePath != null; // Shouldn't be null here!

        CoverageRunner coverageRunner = config.getCoverageRunner();
        LOG.assertTrue(coverageRunner != null, "Coverage runner id = " + config.getRunnerId());

        DefaultCoverageFileProvider fileProvider = new DefaultCoverageFileProvider(new File(covFilePath));
        CoverageSuite suite = createCoverageSuite(config, name, coverageRunner, fileProvider);

        // remove previous instance
        removeCoverageSuite(suite);

        // add new instance
        myCoverageSuites.add(suite);
        return suite;
    }

    @Override
    @RequiredUIAccess
    public void removeCoverageSuite(CoverageSuite suite) {
        String fileName = suite.getCoverageDataFileName();

        boolean deleteTraces = suite.isTracingEnabled();
        if (!FileUtil.isAncestor(ContainerPathManager.get().getSystemPath(), fileName, false)) {
            String message = ExecutionCoverageLocalize.dialogMessageWouldYouLikeToDeleteFileOnDisk(fileName).get();
            if (deleteTraces) {
                message += " And traces directory \'" + FileUtil.getNameWithoutExtension(new File(fileName)) + "\' as well.";
            }
            if (Messages.showYesNoDialog(
                myProject,
                message,
                CommonLocalize.titleWarning().get(),
                UIUtil.getWarningIcon()
            ) == Messages.YES) {
                deleteCachedCoverage(fileName, deleteTraces);
            }
        }
        else {
            deleteCachedCoverage(fileName, deleteTraces);
        }

        myCoverageSuites.remove(suite);
        if (myCurrentSuitesBundle != null && myCurrentSuitesBundle.contains(suite)) {
            CoverageSuite[] suites = myCurrentSuitesBundle.getSuites();
            suites = ArrayUtil.remove(suites, suite);
            chooseSuitesBundle(suites.length > 0 ? new CoverageSuitesBundle(suites) : null);
        }
    }

    private void deleteCachedCoverage(String coverageDataFileName, boolean deleteTraces) {
        FileUtil.delete(new File(coverageDataFileName));
        if (deleteTraces) {
            FileUtil.delete(getTracesDirectory(coverageDataFileName));
        }
    }

    @Override
    public CoverageSuite[] getSuites() {
        return myCoverageSuites.toArray(new CoverageSuite[myCoverageSuites.size()]);
    }

    @Override
    public void chooseSuitesBundle(CoverageSuitesBundle suite) {
        if (myCurrentSuitesBundle == suite && suite == null) {
            return;
        }

        LOG.assertTrue(!myProject.isDefault());

        fireBeforeSuiteChosen();

        mySubCoverageIsActive = false;
        if (myCurrentSuitesBundle != null) {
            myCurrentSuitesBundle.getCoverageEngine().getCoverageAnnotator(myProject).onSuiteChosen(suite);
        }

        myCurrentSuitesBundle = suite;
        disposeAnnotators();

        if (suite == null) {
            triggerPresentationUpdate();
            return;
        }

        for (CoverageSuite coverageSuite : myCurrentSuitesBundle.getSuites()) {
            boolean suiteFileExists = coverageSuite.getCoverageDataFileProvider().ensureFileExists();
            if (!suiteFileExists) {
                chooseSuitesBundle(null);
                return;
            }
        }

        renewCoverageData(suite);

        fireAfterSuiteChosen();
    }

    @Override
    public void coverageGathered(@Nonnull CoverageSuite suite) {
        myProject.getApplication().invokeLater(() -> {
            if (myProject.isDisposed()) {
                return;
            }
            if (myCurrentSuitesBundle != null) {
                LocalizeValue message = CodeInsightLocalize.displayCoveragePrompt(suite.getPresentableName());

                CoverageOptionsProvider coverageOptionsProvider = CoverageOptionsProvider.getInstance(myProject);
                DialogWrapper.DoNotAskOption doNotAskOption = new DialogWrapper.DoNotAskOption() {
                    @Override
                    public boolean isToBeShown() {
                        return coverageOptionsProvider.getOptionToReplace() == 3;
                    }

                    @Override
                    public void setToBeShown(boolean value, int exitCode) {
                        coverageOptionsProvider.setOptionsToReplace(value ? 3 : exitCode);
                    }

                    @Override
                    public boolean canBeHidden() {
                        return true;
                    }

                    @Override
                    public boolean shouldSaveOptionsOnCancel() {
                        return true;
                    }

                    @Nonnull
                    @Override
                    public LocalizeValue getDoNotShowMessage() {
                        return CommonLocalize.dialogOptionsDoNotShow();
                    }
                };
                String[] options = myCurrentSuitesBundle.getCoverageEngine() == suite.getCoverageEngine()
                    ? new String[]{REPLACE_ACTIVE_SUITES.get(), ADD_TO_ACTIVE_SUITES.get(), DO_NOT_APPLY_COLLECTED_COVERAGE.get()}
                    : new String[]{REPLACE_ACTIVE_SUITES.get(), DO_NOT_APPLY_COLLECTED_COVERAGE.get()};
                int answer = doNotAskOption.isToBeShown()
                    ? Messages.showDialog(
                        message.get(),
                        CodeInsightLocalize.codeCoverage().get(),
                        options,
                        1,
                        UIUtil.getQuestionIcon(),
                        doNotAskOption
                    )
                    : coverageOptionsProvider.getOptionToReplace();
                if (answer == DialogWrapper.OK_EXIT_CODE) {
                    chooseSuitesBundle(new CoverageSuitesBundle(suite));
                }
                else if (answer == 1) {
                    chooseSuitesBundle(new CoverageSuitesBundle(ArrayUtil.append(myCurrentSuitesBundle.getSuites(), suite)));
                }
            }
            else {
                chooseSuitesBundle(new CoverageSuitesBundle(suite));
            }
        });
    }

    @Override
    public void triggerPresentationUpdate() {
        renewInformationInEditors();
        UIUtil.invokeLaterIfNeeded(() -> {
            if (myProject.isDisposed()) {
                return;
            }
            ProjectView.getInstance(myProject).refresh();
            CoverageViewManager.getInstance(myProject).setReady(true);
        });
    }

    @Override
    public void attachToProcess(
        @Nonnull ProcessHandler handler,
        @Nonnull RunConfigurationBase configuration,
        RunnerSettings runnerSettings
    ) {
        handler.addProcessListener(new ProcessAdapter() {
            @Override
            public void processTerminated(ProcessEvent event) {
                processGatheredCoverage(configuration, runnerSettings);
            }
        });
    }

    @Override
    public void processGatheredCoverage(@Nonnull RunConfigurationBase configuration, RunnerSettings runnerSettings) {
        if (runnerSettings instanceof CoverageRunnerData) {
            processGatheredCoverage(configuration);
        }
    }

    public static void processGatheredCoverage(RunConfigurationBase configuration) {
        Project project = configuration.getProject();
        if (project.isDisposed()) {
            return;
        }
        CoverageDataManager coverageDataManager = CoverageDataManager.getInstance(project);
        CoverageEnabledConfiguration coverageEnabledConfiguration = CoverageEnabledConfiguration.getOrCreate(configuration);
        //noinspection ConstantConditions
        CoverageSuite coverageSuite = coverageEnabledConfiguration.getCurrentCoverageSuite();
        if (coverageSuite != null) {
            ((BaseCoverageSuite)coverageSuite).setConfiguration(configuration);
            coverageDataManager.coverageGathered(coverageSuite);
        }
    }

    protected void renewCoverageData(@Nonnull CoverageSuitesBundle suite) {
        if (myCurrentSuitesBundle != null) {
            myCurrentSuitesBundle.getCoverageEngine().getCoverageAnnotator(myProject).renewCoverageData(suite, this);
        }
    }

    private void renewInformationInEditors() {
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(myProject);
        VirtualFile[] openFiles = fileEditorManager.getOpenFiles();
        for (VirtualFile openFile : openFiles) {
            FileEditor[] allEditors = fileEditorManager.getAllEditors(openFile);
            applyInformationToEditor(allEditors, openFile);
        }
    }

    private void applyInformationToEditor(FileEditor[] editors, VirtualFile file) {
        PsiFile psiFile = doInReadActionIfProjectOpen(() -> PsiManager.getInstance(myProject).findFile(file));
        if (psiFile != null && myCurrentSuitesBundle != null && psiFile.isPhysical()) {
            CoverageEngine engine = myCurrentSuitesBundle.getCoverageEngine();
            if (!engine.coverageEditorHighlightingApplicableTo(psiFile)) {
                return;
            }

            for (FileEditor fileEditor : editors) {
                if (fileEditor instanceof TextEditor textEditor) {
                    Editor editor = textEditor.getEditor();
                    SrcFileAnnotator annotator;
                    synchronized (ANNOTATORS_LOCK) {
                        annotator = myAnnotators.remove(editor);
                    }
                    if (annotator != null) {
                        Disposer.dispose(annotator);
                    }
                    break;
                }
            }

            for (FileEditor fileEditor : editors) {
                if (fileEditor instanceof TextEditor textEditor) {
                    Editor editor = textEditor.getEditor();
                    SrcFileAnnotator annotator = getAnnotator(editor);
                    if (annotator == null) {
                        annotator = new SrcFileAnnotator(psiFile, editor);
                        synchronized (ANNOTATORS_LOCK) {
                            myAnnotators.put(editor, annotator);
                        }
                    }

                    if (myCurrentSuitesBundle != null && engine.acceptedByFilters(psiFile, myCurrentSuitesBundle)) {
                        annotator.showCoverageInformation(myCurrentSuitesBundle);
                    }
                }
            }
        }
    }

    @Override
    public <T> T doInReadActionIfProjectOpen(@RequiredReadAction Supplier<T> computation) {
        synchronized (myLock) {
            if (myIsProjectClosing) {
                return null;
            }
        }
        return myProject.getApplication().runReadAction(computation);
    }

    @Override
    public void selectSubCoverage(@Nonnull CoverageSuitesBundle suite, List<String> testNames) {
        suite.restoreCoverageData();
        ProjectData data = suite.getCoverageData();
        if (data == null) {
            return;
        }
        mySubCoverageIsActive = true;
        Map<String, Set<Integer>> executionTrace = new HashMap<>();
        for (CoverageSuite coverageSuite : suite.getSuites()) {
            String fileName = coverageSuite.getCoverageDataFileName();
            File tracesDir = getTracesDirectory(fileName);
            for (String testName : testNames) {
                File file = new File(tracesDir, FileUtil.sanitizeFileName(testName) + ".tr");
                if (file.exists()) {
                    DataInputStream in = null;
                    try {
                        in = new DataInputStream(new FileInputStream(file));
                        int traceSize = in.readInt();
                        for (int i = 0; i < traceSize; i++) {
                            String className = in.readUTF();
                            int linesSize = in.readInt();
                            Set<Integer> lines = executionTrace.get(className);
                            if (lines == null) {
                                lines = new HashSet<>();
                                executionTrace.put(className, lines);
                            }
                            for (int l = 0; l < linesSize; l++) {
                                lines.add(in.readInt());
                            }
                        }
                    }
                    catch (Exception e) {
                        LOG.error(e);
                    }
                    finally {
                        try {
                            in.close();
                        }
                        catch (IOException e) {
                            LOG.error(e);
                        }
                    }
                }
            }
        }
        ProjectData projectData = new ProjectData();
        for (String className : executionTrace.keySet()) {
            ClassData loadedClassData = projectData.getClassData(className);
            if (loadedClassData == null) {
                loadedClassData = projectData.getOrCreateClassData(className);
            }
            Set<Integer> lineNumbers = executionTrace.get(className);
            ClassData oldData = data.getClassData(className);
            LOG.assertTrue(oldData != null, "missed className: \"" + className + "\"");
            Object[] oldLines = oldData.getLines();
            LOG.assertTrue(oldLines != null);
            int maxNumber = oldLines.length;
            for (Integer lineNumber : lineNumbers) {
                if (lineNumber >= maxNumber) {
                    maxNumber = lineNumber + 1;
                }
            }
            LineData[] lines = new LineData[maxNumber];
            for (Integer line : lineNumbers) {
                int lineIdx = line - 1;
                String methodSig = null;
                if (lineIdx < oldData.getLines().length) {
                    LineData oldLineData = oldData.getLineData(lineIdx);
                    if (oldLineData != null) {
                        methodSig = oldLineData.getMethodSignature();
                    }
                }
                LineData lineData = new LineData(lineIdx, methodSig);
                if (methodSig != null) {
                    loadedClassData.registerMethodSignature(lineData);
                }
                lineData.setStatus(LineCoverage.FULL);
                lines[lineIdx] = lineData;
            }
            loadedClassData.setLines(lines);
        }
        suite.setCoverageData(projectData);
        renewCoverageData(suite);
    }

    private File getTracesDirectory(String fileName) {
        return new File(new File(fileName).getParentFile(), FileUtil.getNameWithoutExtension(new File(fileName)));
    }

    @Override
    public void restoreMergedCoverage(@Nonnull CoverageSuitesBundle suite) {
        mySubCoverageIsActive = false;
        suite.restoreCoverageData();
        renewCoverageData(suite);
    }

    @Override
    public void addSuiteListener(CoverageSuiteListener listener, Disposable parentDisposable) {
        myListeners.add(listener);
        Disposer.register(parentDisposable, () -> myListeners.remove(listener));
    }

    public void fireBeforeSuiteChosen() {
        for (CoverageSuiteListener listener : myListeners) {
            listener.beforeSuiteChosen();
        }
    }

    public void fireAfterSuiteChosen() {
        for (CoverageSuiteListener listener : myListeners) {
            listener.afterSuiteChosen();
        }
    }

    @Override
    public boolean isSubCoverageActive() {
        return mySubCoverageIsActive;
    }

    @Nullable
    public SrcFileAnnotator getAnnotator(Editor editor) {
        synchronized (ANNOTATORS_LOCK) {
            return myAnnotators.get(editor);
        }
    }

    public void disposeAnnotators() {
        synchronized (ANNOTATORS_LOCK) {
            for (SrcFileAnnotator annotator : myAnnotators.values()) {
                if (annotator != null) {
                    Disposer.dispose(annotator);
                }
            }
            myAnnotators.clear();
        }
    }

    @Nonnull
    private CoverageSuite createCoverageSuite(
        CoverageEnabledConfiguration config,
        String name,
        CoverageRunner coverageRunner,
        DefaultCoverageFileProvider fileProvider
    ) {
        CoverageSuite suite = myProject.getApplication().getExtensionPoint(CoverageEngine.class).computeSafeIfAny(
            engine -> coverageRunner.acceptsCoverageEngine(engine) && engine.isApplicableTo(config.getConfiguration())
                ? engine.createCoverageSuite(coverageRunner, name, fileProvider, config)
                : null
        );
        LOG.assertTrue(suite != null, "Cannot create coverage suite for runner: " + coverageRunner.getPresentableName());
        return suite;
    }

    @Nonnull
    private CoverageSuite createCoverageSuite(
        CoverageRunner coverageRunner,
        String name,
        CoverageFileProvider fileProvider,
        String[] filters,
        long lastCoverageTimeStamp,
        String suiteToMergeWith,
        boolean collectLineInfo,
        boolean tracingEnabled
    ) {
        CoverageSuite suite = myProject.getApplication().getExtensionPoint(CoverageEngine.class).computeSafeIfAny(
            engine -> coverageRunner.acceptsCoverageEngine(engine)
                ? engine.createCoverageSuite(
                    coverageRunner,
                    name,
                    fileProvider,
                    filters,
                    lastCoverageTimeStamp,
                    suiteToMergeWith,
                    collectLineInfo,
                    tracingEnabled,
                    false,
                    myProject
                )
                : null
        );
        LOG.assertTrue(suite != null, "Cannot create coverage suite for runner: " + coverageRunner.getPresentableName());
        return suite;
    }

    private class CoverageEditorFactoryListener implements EditorFactoryListener {
        private final Alarm myAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, myProject);
        private final Map<Editor, Runnable> myCurrentEditors = new HashMap<>();

        @Override
        public void editorCreated(@Nonnull EditorFactoryEvent event) {
            synchronized (myLock) {
                if (myIsProjectClosing) {
                    return;
                }
            }

            Editor editor = event.getEditor();
            if (editor.getProject() != myProject) {
                return;
            }
            PsiFile psiFile = myProject.getApplication().runReadAction(new Supplier<PsiFile>() {
                @Nullable
                @Override
                public PsiFile get() {
                    if (myProject.isDisposed()) {
                        return null;
                    }
                    PsiDocumentManager documentManager = PsiDocumentManager.getInstance(myProject);
                    Document document = editor.getDocument();
                    return documentManager.getPsiFile(document);
                }
            });

            if (psiFile != null && myCurrentSuitesBundle != null && psiFile.isPhysical()) {
                CoverageEngine engine = myCurrentSuitesBundle.getCoverageEngine();
                if (!engine.coverageEditorHighlightingApplicableTo(psiFile)) {
                    return;
                }

                SrcFileAnnotator annotator = getAnnotator(editor);
                if (annotator == null) {
                    annotator = new SrcFileAnnotator(psiFile, editor);
                }

                SrcFileAnnotator finalAnnotator = annotator;

                synchronized (ANNOTATORS_LOCK) {
                    myAnnotators.put(editor, finalAnnotator);
                }

                Runnable request = () -> {
                    if (myProject.isDisposed()) {
                        return;
                    }
                    if (myCurrentSuitesBundle != null) {
                        if (engine.acceptedByFilters(psiFile, myCurrentSuitesBundle)) {
                            finalAnnotator.showCoverageInformation(myCurrentSuitesBundle);
                        }
                    }
                };
                myCurrentEditors.put(editor, request);
                myAlarm.addRequest(request, 100);
            }
        }

        @Override
        public void editorReleased(@Nonnull EditorFactoryEvent event) {
            Editor editor = event.getEditor();
            if (editor.getProject() != myProject) {
                return;
            }
            try {
                SrcFileAnnotator fileAnnotator;
                synchronized (ANNOTATORS_LOCK) {
                    fileAnnotator = myAnnotators.remove(editor);
                }
                if (fileAnnotator != null) {
                    Disposer.dispose(fileAnnotator);
                }
            }
            finally {
                Runnable request = myCurrentEditors.remove(editor);
                if (request != null) {
                    myAlarm.cancelRequest(request);
                }
            }
        }
    }
}
