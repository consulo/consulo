package consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.daemon;

import consulo.application.Application;
import consulo.project.Project;
import consulo.util.collection.MultiValuesMap;
import consulo.application.util.function.ThrowableComputable;
import consulo.ide.impl.idea.util.EventDispatcher;
import consulo.ui.ex.awt.util.MergingUpdateQueue;
import consulo.ui.ex.awt.util.Update;
import consulo.application.AccessRule;
import consulo.disposer.Disposable;
import consulo.logging.Logger;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * @author nik
 */
public class ProjectStructureDaemonAnalyzer implements Disposable {
  public static final Function<Project, ProjectStructureDaemonAnalyzer> FACTORY = ProjectStructureDaemonAnalyzer::new;

  private static final Logger LOG = Logger.getInstance(ProjectStructureDaemonAnalyzer.class);
  private final Map<ProjectStructureElement, ProjectStructureProblemsHolderImpl> myProblemHolders = new HashMap<>();
  private final MultiValuesMap<ProjectStructureElement, ProjectStructureElementUsage> mySourceElement2Usages = new MultiValuesMap<>();
  private final MultiValuesMap<ProjectStructureElement, ProjectStructureElementUsage> myContainingElement2Usages = new MultiValuesMap<>();
  private final Set<ProjectStructureElement> myElementWithNotCalculatedUsages = new HashSet<>();
  private final Set<ProjectStructureElement> myElementsToShowWarningIfUnused = new HashSet<>();
  private final Map<ProjectStructureElement, ProjectStructureProblemDescription> myWarningsAboutUnused = new HashMap<>();
  private final MergingUpdateQueue myAnalyzerQueue;
  private final EventDispatcher<ProjectStructureDaemonAnalyzerListener> myDispatcher = EventDispatcher.create(ProjectStructureDaemonAnalyzerListener.class);
  private final AtomicBoolean myStopped = new AtomicBoolean(false);
  private final ProjectConfigurationProblems myProjectConfigurationProblems;
  private final Project myProject;

  public ProjectStructureDaemonAnalyzer(Project project) {
    myProject = project;
    myProjectConfigurationProblems = new ProjectConfigurationProblems(this, project);
    myAnalyzerQueue = new MergingUpdateQueue("Project Structure Daemon Analyzer", 300, false, null, this, null, false);
  }

  private void doUpdate(ProjectStructureElement element, boolean check, boolean collectUsages) {
    if (myStopped.get()) return;

    if (check) {
      doCheck(element);
    }
    if (collectUsages) {
      doCollectUsages(element);
    }
  }

  private void doCheck(ProjectStructureElement element) {
    ProjectStructureProblemsHolderImpl problemsHolder = new ProjectStructureProblemsHolderImpl();
    AccessRule.read(() -> {
      if (myStopped.get()) return;

      if (LOG.isDebugEnabled()) {
        LOG.debug("checking " + element);
      }
      ProjectStructureValidator.check(myProject, element, problemsHolder);
    });

    invokeLater(() -> {
      if (myStopped.get()) return;

      if (LOG.isDebugEnabled()) {
        LOG.debug("updating problems for " + element);
      }
      ProjectStructureProblemDescription warning = myWarningsAboutUnused.get(element);
      if (warning != null) problemsHolder.registerProblem(warning);
      myProblemHolders.put(element, problemsHolder);
      myDispatcher.getMulticaster().problemsChanged(element);
    });
  }

  private void doCollectUsages(ProjectStructureElement element) {
    ThrowableComputable<List<ProjectStructureElementUsage>,RuntimeException> action = () -> {
      if (myStopped.get()) return null;

      if (LOG.isDebugEnabled()) {
        LOG.debug("collecting usages in " + element);
      }
      return getUsagesInElement(element);
    };
    List<ProjectStructureElementUsage> usages = AccessRule.read(action);

    invokeLater(() -> {
      if (myStopped.get() || usages == null) return;

      if (LOG.isDebugEnabled()) {
        LOG.debug("updating usages for " + element);
      }
      updateUsages(element, usages);
    });
  }

  private static List<ProjectStructureElementUsage> getUsagesInElement(ProjectStructureElement element) {
    return ProjectStructureValidator.getUsagesInElement(element);
  }

  private void updateUsages(ProjectStructureElement element, List<ProjectStructureElementUsage> usages) {
    removeUsagesInElement(element);
    for (ProjectStructureElementUsage usage : usages) {
      addUsage(usage);
    }
    myElementWithNotCalculatedUsages.remove(element);
    reportUnusedElements();
  }

  private static void invokeLater(Runnable runnable) {
    Application.get().getLastUIAccess().give(runnable);
  }

  public void queueUpdate(@Nonnull ProjectStructureElement element) {
    queueUpdate(element, true, true);
  }

  private void queueUpdate(@Nonnull ProjectStructureElement element, boolean check, boolean collectUsages) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("start " + (check ? "checking " : "") + (collectUsages ? "collecting usages " : "") + "for " + element);
    }
    if (collectUsages) {
      myElementWithNotCalculatedUsages.add(element);
    }
    if (element.shouldShowWarningIfUnused()) {
      myElementsToShowWarningIfUnused.add(element);
    }
    myAnalyzerQueue.queue(new AnalyzeElementUpdate(element, check, collectUsages));
  }

  public void removeElement(ProjectStructureElement element) {
    myElementWithNotCalculatedUsages.remove(element);
    myElementsToShowWarningIfUnused.remove(element);
    myWarningsAboutUnused.remove(element);
    myProblemHolders.remove(element);
    Collection<ProjectStructureElementUsage> usages = mySourceElement2Usages.removeAll(element);
    if (usages != null) {
      for (ProjectStructureElementUsage usage : usages) {
        myProblemHolders.remove(usage.getContainingElement());
      }
    }
    removeUsagesInElement(element);
    myDispatcher.getMulticaster().problemsChanged(element);
    reportUnusedElements();
  }

  private void reportUnusedElements() {
    if (!myElementWithNotCalculatedUsages.isEmpty()) return;

    for (ProjectStructureElement element : myElementsToShowWarningIfUnused) {
      ProjectStructureProblemDescription warning;
      Collection<ProjectStructureElementUsage> usages = mySourceElement2Usages.get(element);
      if (usages == null || usages.isEmpty()) {
        warning = element.createUnusedElementWarning(myProject);
      }
      else {
        warning = null;
      }

      ProjectStructureProblemDescription old = myWarningsAboutUnused.put(element, warning);
      ProjectStructureProblemsHolderImpl holder = myProblemHolders.get(element);
      if (holder == null) {
        holder = new ProjectStructureProblemsHolderImpl();
        myProblemHolders.put(element, holder);
      }
      if (old != null) {
        holder.removeProblem(old);
      }
      if (warning != null) {
        holder.registerProblem(warning);
      }
      if (old != null || warning != null) {
        myDispatcher.getMulticaster().problemsChanged(element);
      }
    }
  }

  private void removeUsagesInElement(ProjectStructureElement element) {
    Collection<ProjectStructureElementUsage> usages = myContainingElement2Usages.removeAll(element);
    if (usages != null) {
      for (ProjectStructureElementUsage usage : usages) {
        mySourceElement2Usages.remove(usage.getSourceElement(), usage);
      }
    }
  }

  private void addUsage(@Nonnull ProjectStructureElementUsage usage) {
    mySourceElement2Usages.put(usage.getSourceElement(), usage);
    myContainingElement2Usages.put(usage.getContainingElement(), usage);
  }

  public void stop() {
    LOG.debug("analyzer stopped");
    myStopped.set(true);
    myAnalyzerQueue.cancelAllUpdates();
    clearCaches();
    myAnalyzerQueue.deactivate();
  }

  public void clearCaches() {
    LOG.debug("clear caches");
    myProblemHolders.clear();
  }

  public void queueUpdateForAllElementsWithErrors() {
    List<ProjectStructureElement> toUpdate = new ArrayList<>();
    for (Map.Entry<ProjectStructureElement, ProjectStructureProblemsHolderImpl> entry : myProblemHolders.entrySet()) {
      if (entry.getValue().containsProblems()) {
        toUpdate.add(entry.getKey());
      }
    }
    myProblemHolders.clear();
    LOG.debug("Adding to queue updates for " + toUpdate.size() + " problematic elements");
    for (ProjectStructureElement element : toUpdate) {
      queueUpdate(element);
    }
  }

  @Override
  public void dispose() {
    myStopped.set(true);
    myAnalyzerQueue.cancelAllUpdates();
  }

  @Nullable
  public ProjectStructureProblemsHolderImpl getProblemsHolder(ProjectStructureElement element) {
    return myProblemHolders.get(element);
  }

  public Collection<ProjectStructureElementUsage> getUsages(ProjectStructureElement selected) {
    ProjectStructureElement[] elements = myElementWithNotCalculatedUsages.toArray(new ProjectStructureElement[myElementWithNotCalculatedUsages.size()]);
    for (ProjectStructureElement element : elements) {
      updateUsages(element, getUsagesInElement(element));
    }
    Collection<ProjectStructureElementUsage> usages = mySourceElement2Usages.get(selected);
    return usages != null ? usages : Collections.<ProjectStructureElementUsage>emptyList();
  }

  public void addListener(ProjectStructureDaemonAnalyzerListener listener) {
    LOG.debug("listener added " + listener);
    myDispatcher.addListener(listener);
  }

  public void reset() {
    LOG.debug("analyzer started");
    myAnalyzerQueue.activate();
    myAnalyzerQueue.queue(new Update("reset") {
      @Override
      public void run() {
        myStopped.set(false);
      }
    });
  }

  public void clear() {
    myWarningsAboutUnused.clear();
    myElementsToShowWarningIfUnused.clear();
    mySourceElement2Usages.clear();
    myContainingElement2Usages.clear();
    myElementWithNotCalculatedUsages.clear();
    if (myProjectConfigurationProblems != null) {
      myProjectConfigurationProblems.clearProblems();
    }
  }

  private class AnalyzeElementUpdate extends Update {
    private final ProjectStructureElement myElement;
    private final boolean myCheck;
    private final boolean myCollectUsages;
    private final Object[] myEqualityObjects;

    public AnalyzeElementUpdate(ProjectStructureElement element, boolean check, boolean collectUsages) {
      super(element);
      myElement = element;
      myCheck = check;
      myCollectUsages = collectUsages;
      myEqualityObjects = new Object[]{myElement, myCheck, myCollectUsages};
    }

    @Override
    public boolean canEat(Update update) {
      if (!(update instanceof AnalyzeElementUpdate)) return false;
      AnalyzeElementUpdate other = (AnalyzeElementUpdate)update;
      return myElement.equals(other.myElement) && (!other.myCheck || myCheck) && (!other.myCollectUsages || myCollectUsages);
    }

    @Nonnull
    @Override
    public Object[] getEqualityObjects() {
      return myEqualityObjects;
    }

    @Override
    public void run() {
      try {
        doUpdate(myElement, myCheck, myCollectUsages);
      }
      catch (Throwable t) {
        LOG.error(t);
      }
    }
  }
}
