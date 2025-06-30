package consulo.execution.coverage.impl.internal.action;

import consulo.application.Application;
import consulo.disposer.Disposer;
import consulo.execution.action.Location;
import consulo.execution.configuration.ModuleBasedConfiguration;
import consulo.execution.configuration.RunProfile;
import consulo.execution.coverage.CoverageDataManager;
import consulo.execution.coverage.CoverageEnabledConfiguration;
import consulo.execution.coverage.CoverageExecutor;
import consulo.execution.coverage.CoverageSuitesBundle;
import consulo.execution.coverage.localize.ExecutionCoverageLocalize;
import consulo.execution.executor.Executor;
import consulo.execution.test.AbstractTestProxy;
import consulo.execution.test.TestConsoleProperties;
import consulo.execution.test.TestFrameworkRunningModel;
import consulo.execution.test.action.ToggleModelAction;
import consulo.language.psi.PsiElement;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.util.Alarm;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * @author anna
 * @since 2007-11-19
 */
public class TrackCoverageAction extends ToggleModelAction {
    private final TestConsoleProperties myProperties;
    private TestFrameworkRunningModel myModel;
    private TreeSelectionListener myTreeSelectionListener;

    public TrackCoverageAction(TestConsoleProperties properties) {
        super(
            ExecutionCoverageLocalize.showCoveragePerTestActionText(),
            ExecutionCoverageLocalize.showCoveragePerTestActionDescription(),
            PlatformIconGroup.runconfigurationsTrackcoverage(),
            properties,
            TestConsoleProperties.TRACK_CODE_COVERAGE
        );
        myProperties = properties;
    }

    @Override
    public void setSelected(@Nonnull AnActionEvent e, boolean state) {
        super.setSelected(e, state);
        if (!TestConsoleProperties.TRACK_CODE_COVERAGE.value(myProperties)) {
            restoreMergedCoverage();
        }
        else {
            selectSubCoverage();
        }
    }

    @Override
    public boolean isSelected(@Nonnull AnActionEvent e) {
        return super.isSelected(e) && CoverageDataManager.getInstance(myProperties.getProject()).isSubCoverageActive();
    }

    private void restoreMergedCoverage() {
        CoverageDataManager coverageDataManager = CoverageDataManager.getInstance(myProperties.getProject());
        if (coverageDataManager.isSubCoverageActive()) {
            CoverageSuitesBundle currentSuite = coverageDataManager.getCurrentSuitesBundle();
            if (currentSuite != null) {
                coverageDataManager.restoreMergedCoverage(currentSuite);
            }
        }
    }

    @Override
    public void setModel(TestFrameworkRunningModel model) {
        if (myModel != null) {
            myModel.getTreeView().removeTreeSelectionListener(myTreeSelectionListener);
        }
        myModel = model;
        if (model != null) {
            myTreeSelectionListener = new MyTreeSelectionListener();
            model.getTreeView().addTreeSelectionListener(myTreeSelectionListener);
            Disposer.register(model, this::restoreMergedCoverage);
        }
    }

    @Override
    protected boolean isEnabled() {
        CoverageSuitesBundle suite = getCurrentCoverageSuite();
        return suite != null && suite.isCoverageByTestApplicable() && suite.isCoverageByTestEnabled();
    }

    @Override
    protected boolean isVisible() {
        CoverageSuitesBundle suite = getCurrentCoverageSuite();
        return suite != null && suite.isCoverageByTestApplicable();
    }

    @Nullable
    private CoverageSuitesBundle getCurrentCoverageSuite() {
        if (myModel == null) {
            return null;
        }

        RunProfile runConf = myModel.getProperties().getConfiguration();
        if (runConf instanceof ModuleBasedConfiguration) {
            // if coverage supported for run configuration
            if (CoverageEnabledConfiguration.isApplicableTo((ModuleBasedConfiguration) runConf)) {
                // Get coverage settings
                Executor executor = myProperties.getExecutor();
                if (executor != null && executor.getId().equals(CoverageExecutor.EXECUTOR_ID)) {
                    return CoverageDataManager.getInstance(myProperties.getProject()).getCurrentSuitesBundle();
                }
            }
        }
        return null;
    }

    private void selectSubCoverage() {
        CoverageDataManager coverageDataManager = CoverageDataManager.getInstance(myProperties.getProject());
        CoverageSuitesBundle currentSuite = coverageDataManager.getCurrentSuitesBundle();
        if (currentSuite != null) {
            AbstractTestProxy test = myModel.getTreeView().getSelectedTest();
            List<String> testMethods = new ArrayList<>();
            if (test != null && !test.isInProgress()) {
                List<? extends AbstractTestProxy> list = test.getAllTests();
                for (AbstractTestProxy proxy : list) {
                    Location location = proxy.getLocation(myProperties.getProject(), myProperties.getScope());
                    if (location != null) {
                        PsiElement element = location.getPsiElement();
                        String name = currentSuite.getCoverageEngine().getTestMethodName(element, proxy);
                        if (name != null) {
                            testMethods.add(name);
                        }
                    }
                }
            }
            coverageDataManager.selectSubCoverage(currentSuite, testMethods);
        }
    }

    private class MyTreeSelectionListener implements TreeSelectionListener {
        private final Alarm myUpdateCoverageAlarm;

        public MyTreeSelectionListener() {
            myUpdateCoverageAlarm = new Alarm(myModel);
        }

        @Override
        public void valueChanged(TreeSelectionEvent e) {
            if (myUpdateCoverageAlarm.isDisposed()) {
                return;
            }
            if (!TestConsoleProperties.TRACK_CODE_COVERAGE.value(myModel.getProperties()) || !isEnabled()) {
                return;
            }
            myUpdateCoverageAlarm.cancelAllRequests();
            Project project = myModel.getProperties().getProject();
            CoverageDataManager coverageDataManager = CoverageDataManager.getInstance(project);
            CoverageSuitesBundle currentSuite = coverageDataManager.getCurrentSuitesBundle();
            if (currentSuite != null) {
                if (Application.get().isDispatchThread()) {
                    myUpdateCoverageAlarm.addRequest(TrackCoverageAction.this::selectSubCoverage, 300);
                }
                else if (coverageDataManager.isSubCoverageActive()) {
                    coverageDataManager.restoreMergedCoverage(currentSuite);
                }
            }
        }
    }
}
