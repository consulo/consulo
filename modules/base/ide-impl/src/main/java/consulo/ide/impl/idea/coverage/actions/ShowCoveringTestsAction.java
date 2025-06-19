/*
 * User: anna
 * Date: 29-May-2008
 */
package consulo.ide.impl.idea.coverage.actions;

import com.intellij.rt.coverage.data.LineCoverage;
import com.intellij.rt.coverage.data.LineData;
import consulo.application.progress.ProgressManager;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.execution.coverage.CoverageDataManager;
import consulo.execution.coverage.CoverageSuite;
import consulo.execution.coverage.CoverageSuitesBundle;
import consulo.execution.coverage.localize.ExecutionCoverageLocalize;
import consulo.ide.impl.idea.codeInsight.hint.ImplementationViewComponent;
import consulo.ide.impl.idea.openapi.ui.PanelWithText;
import consulo.ide.impl.idea.ui.popup.NotLookupOrSearchCondition;
import consulo.language.editor.hint.HintManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.popup.ComponentPopupBuilder;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.util.collection.ArrayUtil;
import consulo.util.io.FileUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class ShowCoveringTestsAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(ShowCoveringTestsAction.class);

    private final String myClassFQName;
    private final LineData myLineData;

    public ShowCoveringTestsAction(String classFQName, LineData lineData) {
        super(
            ExecutionCoverageLocalize.actionShowTestsCoveringLineText(),
            ExecutionCoverageLocalize.actionShowTestsCoveringLineDescription(),
            PlatformIconGroup.modulesTestroot()
        );
        myClassFQName = classFQName;
        myLineData = lineData;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        DataContext context = e.getDataContext();
        Project project = context.getData(Project.KEY);
        LOG.assertTrue(project != null);
        Editor editor = context.getData(Editor.KEY);
        LOG.assertTrue(editor != null);

        CoverageSuitesBundle currentSuite = CoverageDataManager.getInstance(project).getCurrentSuitesBundle();
        LOG.assertTrue(currentSuite != null);

        File[] traceFiles = getTraceFiles(project);

        Set<String> tests = new HashSet<>();
        Runnable runnable = () -> {
            for (File traceFile : traceFiles) {
                DataInputStream in = null;
                try {
                    in = new DataInputStream(new FileInputStream(traceFile));
                    extractTests(traceFile, in, tests);
                }
                catch (Exception ex) {
                    LOG.error(traceFile.getName(), ex);
                }
                finally {
                    try {
                        in.close();
                    }
                    catch (IOException ex) {
                        LOG.error(ex);
                    }
                }
            }
        };

        if (ProgressManager.getInstance().runProcessWithProgressSynchronously(
            runnable,
            "Extract information about tests",
            false,
            project
        )) { //todo cache them? show nothing found message
            String[] testNames = ArrayUtil.toStringArray(tests);
            Arrays.sort(testNames);
            if (testNames.length == 0) {
                HintManager.getInstance().showErrorHint(editor, ExecutionCoverageLocalize.hintTextFailedToLoadCoveredTests());
                return;
            }
            List<PsiElement> elements = currentSuite.getCoverageEngine().findTestsByNames(testNames, project);
            ImplementationViewComponent component;
            LocalizeValue title = ExecutionCoverageLocalize.popupTitleTestsCoveringLine(myClassFQName, myLineData.getLineNumber());
            ComponentPopupBuilder popupBuilder;
            if (!elements.isEmpty()) {
                component = new ImplementationViewComponent(PsiUtilCore.toPsiElementArray(elements), 0);
                popupBuilder = JBPopupFactory.getInstance()
                    .createComponentPopupBuilder(component, component.getPreferredFocusableComponent())
                    .setDimensionServiceKey(project, "ShowTestsPopup", false)
                    .setCouldPin(popup -> {
                        component.showInUsageView();
                        popup.cancel();
                        return false;
                    });
            }
            else {
                component = null;
                JPanel panel = new PanelWithText(
                    ExecutionCoverageLocalize.followingTestCouldNotBeFound1(
                        testNames.length,
                        StringUtil.join(testNames, ",").replace("_", ".")
                    ).get()
                );
                popupBuilder = JBPopupFactory.getInstance().createComponentPopupBuilder(panel, null);
            }
            JBPopup popup = popupBuilder.setRequestFocusCondition(project, NotLookupOrSearchCondition.INSTANCE)
                .setProject(project)
                .setResizable(true)
                .setMovable(true)
                .setTitle(title)
                .createPopup();
            editor.showPopupInBestPositionFor(popup);

            if (component != null) {
                component.setHint(popup, title);
            }
        }
    }

    private void extractTests(File traceFile, DataInputStream in, Set<String> tests) throws IOException {
        long traceSize = in.readInt();
        for (int i = 0; i < traceSize; i++) {
            String className = in.readUTF();
            int linesSize = in.readInt();
            for (int l = 0; l < linesSize; l++) {
                int line = in.readInt();
                if (Comparing.strEqual(className, myClassFQName)) {
                    if (myLineData.getLineNumber() == line) {
                        tests.add(FileUtil.getNameWithoutExtension(traceFile));
                        return;
                    }
                }
            }
        }
    }

    @Override
    @RequiredUIAccess
    public void update(AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        presentation.setEnabled(false);
        if (myLineData != null && myLineData.getStatus() != LineCoverage.NONE) {
            Project project = e.getDataContext().getData(Project.KEY);
            if (project != null) {
                File[] files = getTraceFiles(project);
                if (files != null && files.length > 0) {
                    presentation.setEnabled(CoverageDataManager.getInstance(project).getCurrentSuitesBundle().isCoverageByTestEnabled());
                }
            }
        }
    }

    @Nullable
    private static File[] getTraceFiles(Project project) {
        CoverageSuitesBundle currentSuite = CoverageDataManager.getInstance(project).getCurrentSuitesBundle();
        if (currentSuite == null) {
            return null;
        }
        List<File> files = new ArrayList<>();
        for (CoverageSuite coverageSuite : currentSuite.getSuites()) {
            String filePath = coverageSuite.getCoverageDataFileName();
            String dirName = FileUtil.getNameWithoutExtension(new File(filePath).getName());

            File parentDir = new File(filePath).getParentFile();
            File tracesDir = new File(parentDir, dirName);
            File[] suiteFiles = tracesDir.listFiles();
            if (suiteFiles != null) {
                Collections.addAll(files, suiteFiles);
            }
        }

        return files.isEmpty() ? null : files.toArray(new File[files.size()]);
    }
}
