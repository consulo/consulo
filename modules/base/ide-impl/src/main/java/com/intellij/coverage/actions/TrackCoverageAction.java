/*
 * User: anna
 * Date: 19-Nov-2007
 */
package com.intellij.coverage.actions;

import consulo.execution.coverage.CoverageExecutor;
import consulo.application.AllIcons;
import consulo.application.ApplicationManager;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.action.Location;
import consulo.execution.configuration.ModuleBasedConfiguration;
import consulo.execution.configuration.RunProfile;
import consulo.execution.coverage.CoverageDataManager;
import consulo.execution.coverage.CoverageEnabledConfiguration;
import consulo.execution.coverage.CoverageSuitesBundle;
import consulo.execution.executor.Executor;
import consulo.execution.test.AbstractTestProxy;
import consulo.execution.test.TestConsoleProperties;
import consulo.execution.test.TestFrameworkRunningModel;
import consulo.execution.test.action.ToggleModelAction;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.util.Alarm;

import javax.annotation.Nullable;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import java.util.ArrayList;
import java.util.List;

public class TrackCoverageAction extends ToggleModelAction {
  private final TestConsoleProperties myProperties;
  private TestFrameworkRunningModel myModel;
  private TreeSelectionListener myTreeSelectionListener;

  public TrackCoverageAction(TestConsoleProperties properties) {
    super("Show coverage per test", "Show coverage per test", AllIcons.RunConfigurations.TrackCoverage, properties,
          TestConsoleProperties.TRACK_CODE_COVERAGE);
    myProperties = properties;

  }

  public void setSelected(final AnActionEvent e, final boolean state) {
    super.setSelected(e, state);
    if (!TestConsoleProperties.TRACK_CODE_COVERAGE.value(myProperties)) {
      restoreMergedCoverage();
    } else {
      selectSubCoverage();
    }
  }

  @Override
  public boolean isSelected(AnActionEvent e) {
    return super.isSelected(e) && CoverageDataManager.getInstance(myProperties.getProject()).isSubCoverageActive();
  }

  private void restoreMergedCoverage() {
    final CoverageDataManager coverageDataManager = CoverageDataManager.getInstance(myProperties.getProject());
    if (coverageDataManager.isSubCoverageActive()) {
      final CoverageSuitesBundle currentSuite = coverageDataManager.getCurrentSuitesBundle();
      if (currentSuite != null) {
        coverageDataManager.restoreMergedCoverage(currentSuite);
      }
    }
  }

  public void setModel(final TestFrameworkRunningModel model) {
    if (myModel != null) myModel.getTreeView().removeTreeSelectionListener(myTreeSelectionListener);
    myModel = model;
    if (model != null) {
      myTreeSelectionListener = new MyTreeSelectionListener();
      model.getTreeView().addTreeSelectionListener(myTreeSelectionListener);
      Disposer.register(model, new Disposable() {
        public void dispose() {
          restoreMergedCoverage();
        }
      });
    }
  }

  protected boolean isEnabled() {
    final CoverageSuitesBundle suite = getCurrentCoverageSuite();
    return suite != null && suite.isCoverageByTestApplicable() && suite.isCoverageByTestEnabled();
  }

  @Override
  protected boolean isVisible() {
    final CoverageSuitesBundle suite = getCurrentCoverageSuite();
    return suite != null && suite.isCoverageByTestApplicable();
  }

  @Nullable
  private CoverageSuitesBundle getCurrentCoverageSuite() {
    if (myModel == null) {
      return null;
    }

    final RunProfile runConf = myModel.getProperties().getConfiguration();
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
    final CoverageDataManager coverageDataManager = CoverageDataManager.getInstance(myProperties.getProject());
    final CoverageSuitesBundle currentSuite = coverageDataManager.getCurrentSuitesBundle();
    if (currentSuite != null) {
      final AbstractTestProxy test = myModel.getTreeView().getSelectedTest();
      List<String> testMethods = new ArrayList<String>();
      if (test != null && !test.isInProgress()) {
        final List<? extends AbstractTestProxy> list = test.getAllTests();
        for (AbstractTestProxy proxy : list) {
          final Location location = proxy.getLocation(myProperties.getProject(), myProperties.getScope());
          if (location != null) {
            final PsiElement element = location.getPsiElement();
            final String name = currentSuite.getCoverageEngine().getTestMethodName(element, proxy);
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

    public void valueChanged(final TreeSelectionEvent e) {
      if (myUpdateCoverageAlarm.isDisposed()) return;
      if (!TestConsoleProperties.TRACK_CODE_COVERAGE.value(myModel.getProperties()) || !isEnabled()) return;
      myUpdateCoverageAlarm.cancelAllRequests();
      final Project project = myModel.getProperties().getProject();
      final CoverageDataManager coverageDataManager = CoverageDataManager.getInstance(project);
      final CoverageSuitesBundle currentSuite = coverageDataManager.getCurrentSuitesBundle();
      if (currentSuite != null) {
        if (ApplicationManager.getApplication().isDispatchThread()) {
          myUpdateCoverageAlarm.addRequest(new Runnable() {
            public void run() {
              selectSubCoverage();
            }
          }, 300);
        } else {
          if (coverageDataManager.isSubCoverageActive()) coverageDataManager.restoreMergedCoverage(currentSuite);
        }
      }
    }
  }
}
