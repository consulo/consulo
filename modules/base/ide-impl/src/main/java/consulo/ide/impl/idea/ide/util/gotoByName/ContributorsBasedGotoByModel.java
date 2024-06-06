// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.util.gotoByName;

import consulo.application.dumb.IndexNotReadyException;
import consulo.application.dumb.PossiblyDumbAware;
import consulo.application.impl.internal.progress.ProgressIndicatorBase;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.util.ReadActionProcessor;
import consulo.application.util.concurrent.JobLauncher;
import consulo.application.util.function.Processor;
import consulo.application.util.function.Processors;
import consulo.component.ProcessCanceledException;
import consulo.component.util.PluginExceptionUtil;
import consulo.ide.navigation.ChooseByNameContributor;
import consulo.ide.navigation.ChooseByNameContributorEx;
import consulo.language.editor.ui.navigation.NavigationItemListCellRenderer;
import consulo.language.pom.PomTargetPsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.search.FindSymbolParameters;
import consulo.language.psi.stub.IdFilter;
import consulo.logging.Logger;
import consulo.navigation.NavigationItem;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.util.collection.ArrayUtil;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.virtualFileSystem.VirtualFile;
import gnu.trove.TIntHashSet;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.ConcurrentMap;

/**
 * Contributor-based goto model
 */
public abstract class ContributorsBasedGotoByModel implements ChooseByNameModelEx, PossiblyDumbAware {
  public static final Logger LOG = Logger.getInstance(ContributorsBasedGotoByModel.class);

  protected final Project myProject;
  private final List<? extends ChooseByNameContributor> myContributors;

  protected ContributorsBasedGotoByModel(@Nonnull Project project, @Nonnull ChooseByNameContributor[] contributors) {
    this(project, Arrays.asList(contributors));
  }

  protected ContributorsBasedGotoByModel(@Nonnull Project project, @Nonnull List<? extends ChooseByNameContributor> contributors) {
    myProject = project;
    myContributors = contributors;
    assert !contributors.contains(null);
  }

  @Override
  public boolean isDumbAware() {
    return ContainerUtil.find(getContributorList(), o -> DumbService.isDumbAware(o)) != null;
  }

  @Nonnull
  @Override
  public ListCellRenderer getListCellRenderer() {
    return new NavigationItemListCellRenderer();
  }

  public boolean sameNamesForProjectAndLibraries() {
    return false;
  }

  private final ConcurrentMap<ChooseByNameContributor, TIntHashSet> myContributorToItsSymbolsMap = ContainerUtil.createConcurrentWeakMap();

  @Override
  public void processNames(@Nonnull Processor<? super String> nameProcessor, @Nonnull FindSymbolParameters parameters) {
    long start = System.currentTimeMillis();
    List<? extends ChooseByNameContributor> contributors = filterDumb(getContributorList());
    ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    Processor<ChooseByNameContributor> processor = new ReadActionProcessor<ChooseByNameContributor>() {
      @Override
      public boolean processInReadAction(@Nonnull ChooseByNameContributor contributor) {
        try {
          if (!myProject.isDisposed()) {
            long contributorStarted = System.currentTimeMillis();
            processContributorNames(contributor, parameters, nameProcessor);

            if (LOG.isDebugEnabled()) {
              LOG.debug(contributor + " for " + (System.currentTimeMillis() - contributorStarted));
            }
          }
        }
        catch (ProcessCanceledException | IndexNotReadyException ex) {
          // index corruption detected, ignore
        }
        catch (Exception ex) {
          LOG.error(ex);
        }
        return true;
      }
    };
    if (!JobLauncher.getInstance().invokeConcurrentlyUnderProgress(contributors, indicator, processor)) {
      throw new ProcessCanceledException();
    }
    if (indicator != null) {
      indicator.checkCanceled();
    }
    long finish = System.currentTimeMillis();
    if (LOG.isDebugEnabled()) {
      LOG.debug("processNames(): " + (finish - start) + "ms;");
    }
  }

  public void processContributorNames(@Nonnull ChooseByNameContributor contributor, @Nonnull FindSymbolParameters parameters, @Nonnull Processor<? super String> nameProcessor) {
    TIntHashSet filter = new TIntHashSet(1000);
    if (contributor instanceof ChooseByNameContributorEx) {
      ((ChooseByNameContributorEx)contributor).processNames(s -> {
        if (nameProcessor.process(s)) {
          filter.add(s.hashCode());
        }
        return true;
      }, parameters.getSearchScope(), parameters.getIdFilter());
    }
    else {
      String[] names = contributor.getNames(myProject, parameters.isSearchInLibraries());
      for (String element : names) {
        if (nameProcessor.process(element)) {
          filter.add(element.hashCode());
        }
      }
    }
    myContributorToItsSymbolsMap.put(contributor, filter);
  }

  IdFilter getIdFilter(boolean withLibraries) {
    return IdFilter.getProjectIdFilter(myProject, withLibraries);
  }

  @Nonnull
  @Override
  public String[] getNames(final boolean checkBoxState) {
    final Set<String> allNames = new HashSet<>();

    Collection<String> result = Collections.synchronizedCollection(allNames);
    processNames(Processors.cancelableCollectProcessor(result), FindSymbolParameters.simple(myProject, checkBoxState));
    if (LOG.isDebugEnabled()) {
      LOG.debug("getNames(): (got " + allNames.size() + " elements)");
    }
    return ArrayUtil.toStringArray(allNames);
  }

  private List<? extends ChooseByNameContributor> filterDumb(List<? extends ChooseByNameContributor> contributors) {
    if (!DumbService.getInstance(myProject).isDumb()) return contributors;
    List<ChooseByNameContributor> answer = new ArrayList<>(contributors.size());
    for (ChooseByNameContributor contributor : contributors) {
      if (DumbService.isDumbAware(contributor)) {
        answer.add(contributor);
      }
    }

    return answer;
  }

  @Nonnull
  public Object[] getElementsByName(@Nonnull final String name, @Nonnull final FindSymbolParameters parameters, @Nonnull final ProgressIndicator canceled) {
    long elementByNameStarted = System.currentTimeMillis();
    final List<NavigationItem> items = Collections.synchronizedList(new ArrayList<>());

    Processor<ChooseByNameContributor> processor = contributor -> {
      if (myProject.isDisposed()) {
        return true;
      }
      TIntHashSet filter = myContributorToItsSymbolsMap.get(contributor);
      if (filter != null && !filter.contains(name.hashCode())) return true;
      try {
        boolean searchInLibraries = parameters.isSearchInLibraries();
        long contributorStarted = System.currentTimeMillis();

        if (contributor instanceof ChooseByNameContributorEx) {
          ((ChooseByNameContributorEx)contributor).processElementsWithName(name, item -> {
            canceled.checkCanceled();
            if (acceptItem(item)) items.add(item);
            return true;
          }, parameters);

          if (LOG.isDebugEnabled()) {
            LOG.debug(System.currentTimeMillis() - contributorStarted + "," + contributor + ",");
          }
        }
        else {
          NavigationItem[] itemsByName = contributor.getItemsByName(name, parameters.getLocalPatternName(), myProject, searchInLibraries);
          for (NavigationItem item : itemsByName) {
            canceled.checkCanceled();
            if (item == null) {
              PluginExceptionUtil.logPluginError(LOG, "null item from contributor " + contributor + " for name " + name, null, contributor.getClass());
              continue;
            }
            VirtualFile file = item instanceof PsiElement && !(item instanceof PomTargetPsiElement) ? PsiUtilCore.getVirtualFile((PsiElement)item) : null;
            if (file != null && !parameters.getSearchScope().contains(file)) continue;

            if (acceptItem(item)) {
              items.add(item);
            }
          }

          if (LOG.isDebugEnabled()) {
            LOG.debug(System.currentTimeMillis() - contributorStarted + "," + contributor + "," + itemsByName.length);
          }
        }
      }
      catch (ProcessCanceledException ex) {
        // index corruption detected, ignore
      }
      catch (Exception ex) {
        LOG.error(ex);
      }
      return true;
    };
    if (!JobLauncher.getInstance().invokeConcurrentlyUnderProgress(filterDumb(getContributorList()), canceled, processor)) {
      canceled.cancel();
    }
    canceled.checkCanceled(); // if parallel job execution was canceled because of PCE, rethrow it from here
    if (LOG.isDebugEnabled()) {
      LOG.debug("Retrieving " + name + ":" + items.size() + " for " + (System.currentTimeMillis() - elementByNameStarted));
    }
    return ArrayUtil.toObjectArray(items);
  }

  /**
   * Get elements by name from contributors.
   *
   * @param name          a name
   * @param checkBoxState if true, non-project files are considered as well
   * @param pattern       a pattern to use
   * @return a list of navigation items from contributors for
   * which {@link #acceptItem(NavigationItem) returns true.
   */
  @Nonnull
  @Override
  public Object[] getElementsByName(@Nonnull final String name, final boolean checkBoxState, @Nonnull final String pattern) {
    return getElementsByName(name, FindSymbolParameters.wrap(pattern, myProject, checkBoxState), new ProgressIndicatorBase());
  }

  @Override
  public String getElementName(@Nonnull Object element) {
    if (!(element instanceof NavigationItem)) {
      throw new AssertionError(element + " of " + element.getClass() + " in " + this + " of " + getClass());
    }
    return ((NavigationItem)element).getName();
  }

  @Override
  public String getHelpId() {
    return null;
  }

  protected List<? extends ChooseByNameContributor> getContributorList() {
    return myContributors;
  }

  /**
   * This method allows extending classes to introduce additional filtering criteria to model
   * beyond pattern and project/non-project files. The default implementation just returns true.
   *
   * @param item an item to filter
   * @return true if the item is acceptable according to additional filtering criteria.
   */
  protected boolean acceptItem(NavigationItem item) {
    return true;
  }

  @Override
  public boolean useMiddleMatching() {
    return true;
  }

  public
  @Nonnull
  String removeModelSpecificMarkup(@Nonnull String pattern) {
    return pattern;
  }

  @Nonnull
  public Project getProject() {
    return myProject;
  }
}
