package consulo.execution.coverage;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.AllIcons;
import consulo.application.ApplicationManager;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.disposer.Disposer;
import consulo.execution.configuration.RunConfigurationBase;
import consulo.execution.coverage.view.CoverageView;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.toolWindow.ContentManagerWatcher;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * User: anna
 * Date: 1/2/12
 */
@Singleton
@State(name = "CoverageViewManager", storages = {@Storage(file = StoragePathMacros.WORKSPACE_FILE)})
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class CoverageViewManager implements PersistentStateComponent<CoverageViewManager.StateBean> {
  private static final Logger LOG = Logger.getInstance(CoverageViewManager.class);
  public static final String TOOLWINDOW_ID = "Coverage";
  private Project myProject;
  private final CoverageDataManager myDataManager;
  private ContentManager myContentManager;
  private StateBean myStateBean = new StateBean();
  private Map<String, CoverageView> myViews = new HashMap<String, CoverageView>();
  private boolean myReady;

  @Inject
  public CoverageViewManager(Project project, ToolWindowManager toolWindowManager, CoverageDataManager dataManager) {
    myProject = project;
    myDataManager = dataManager;

    ToolWindow toolWindow = toolWindowManager.registerToolWindow(TOOLWINDOW_ID, true, ToolWindowAnchor.RIGHT, myProject);
    toolWindow.setIcon(AllIcons.Toolwindows.ToolWindowCoverage);
    toolWindow.setSplitMode(true, null);
    myContentManager = toolWindow.getContentManager();
    ContentManagerWatcher.watchContentManager(toolWindow, myContentManager);
  }

  public StateBean getState() {
    return myStateBean;
  }

  public void loadState(StateBean state) {
    myStateBean = state;
  }

  public CoverageView getToolwindow(CoverageSuitesBundle suitesBundle) {
    return myViews.get(getDisplayName(suitesBundle));
  }

  public void activateToolwindow(CoverageView view, boolean requestFocus) {
    ToolWindow toolWindow = ToolWindowManager.getInstance(myProject).getToolWindow(TOOLWINDOW_ID);
    if (requestFocus) {
      myContentManager.setSelectedContent(myContentManager.getContent(view));
      LOG.assertTrue(toolWindow != null);
      toolWindow.activate(null, false);
    }
  }

  public static CoverageViewManager getInstance(@Nonnull Project project) {
    return project.getInstance(CoverageViewManager.class);
  }

  public void createToolWindow(String displayName, boolean defaultFileProvider) {
    closeView(displayName);

    final CoverageView coverageView = new CoverageView(myProject, myDataManager, myStateBean);
    myViews.put(displayName, coverageView);
    Content content = myContentManager.getFactory().createContent(coverageView, displayName, true);
    myContentManager.addContent(content);
    myContentManager.setSelectedContent(content);

    if (CoverageOptionsProvider.getInstance(myProject).activateViewOnRun() && defaultFileProvider) {
      activateToolwindow(coverageView, true);
    }
  }

  public void closeView(String displayName) {
    final CoverageView oldView = myViews.get(displayName);
    if (oldView != null) {
      final Content content = myContentManager.getContent(oldView);
      final Runnable runnable = new Runnable() {
        public void run() {
          if (content != null) {
            myContentManager.removeContent(content, true);
          }
          Disposer.dispose(oldView);
        }
      };
      ApplicationManager.getApplication().invokeLater(runnable);
    }
    setReady(false);
  }

  public boolean isReady() {
    return myReady;
  }

  public void setReady(boolean ready) {
    myReady = ready;
  }

  public static String getDisplayName(CoverageSuitesBundle suitesBundle) {
    final RunConfigurationBase configuration = suitesBundle.getRunConfiguration();
    return configuration != null ? configuration.getName() : suitesBundle.getPresentableName();
  }

  public static class StateBean {
    public boolean myFlattenPackages = false;
    public boolean myAutoScrollToSource = false;
    public boolean myAutoScrollFromSource = false;
  }
}
