/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vcs.changes.committed;

import consulo.application.ApplicationManager;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.application.util.function.AsynchConsumer;
import consulo.dataContext.DataSink;
import consulo.dataContext.TypeSafeDataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.VcsDataKeys;
import consulo.ide.impl.idea.openapi.vcs.changes.BackgroundFromStartOption;
import consulo.ide.impl.idea.util.BufferedListConsumer;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.util.WaitForProgressToShow;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.FilterComponent;
import consulo.ui.ex.awt.Messages;
import consulo.util.dataholder.Key;
import consulo.versionControlSystem.*;
import consulo.versionControlSystem.change.commited.*;
import consulo.versionControlSystem.impl.internal.change.commited.CommittedChangesCache;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.versionBrowser.ChangeBrowserSettings;
import consulo.versionControlSystem.versionBrowser.CommittedChangeList;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author yole
 * @since 2006-12-05
 */
public class CommittedChangesPanel extends JPanel implements TypeSafeDataProvider, Disposable {
  private static final Logger LOG = Logger.getInstance(CommittedChangesPanel.class);

  private final CommittedChangesTreeBrowser myBrowser;
  private final Project myProject;
  private CommittedChangesProvider myProvider;
  private ChangeBrowserSettings mySettings;
  private final RepositoryLocation myLocation;
  private int myMaxCount = 0;
  private final MyFilterComponent myFilterComponent = new MyFilterComponent();
  private final List<Runnable> myShouldBeCalledOnDispose;
  private volatile boolean myDisposed;
  private volatile boolean myInLoad;
  private Consumer<String> myIfNotCachedReloader;

  public CommittedChangesPanel(Project project,
                               CommittedChangesProvider provider,
                               ChangeBrowserSettings settings,
                               @Nullable RepositoryLocation location,
                               @jakarta.annotation.Nullable ActionGroup extraActions) {
    super(new BorderLayout());
    mySettings = settings;
    myProject = project;
    myProvider = provider;
    myLocation = location;
    myShouldBeCalledOnDispose = new ArrayList<Runnable>();
    myBrowser = new CommittedChangesTreeBrowser(project, new ArrayList<CommittedChangeList>());
    Disposer.register(this, myBrowser);
    add(myBrowser, BorderLayout.CENTER);

    VcsCommittedViewAuxiliary auxiliary = provider.createActions(myBrowser, location);

    JPanel toolbarPanel = new JPanel();
    toolbarPanel.setLayout(new BoxLayout(toolbarPanel, BoxLayout.X_AXIS));

    ActionGroup group = (ActionGroup)ActionManager.getInstance().getAction("CommittedChangesToolbar");

    ActionToolbar toolBar = myBrowser.createGroupFilterToolbar(project, group, extraActions, auxiliary != null ? auxiliary.getToolbarActions() : Collections.<AnAction>emptyList());
    toolbarPanel.add(toolBar.getComponent());
    toolbarPanel.add(Box.createHorizontalGlue());
    toolbarPanel.add(myFilterComponent);
    myFilterComponent.setMinimumSize(myFilterComponent.getPreferredSize());
    myFilterComponent.setMaximumSize(myFilterComponent.getPreferredSize());
    myBrowser.setToolBar(toolbarPanel);

    if (auxiliary != null) {
      myShouldBeCalledOnDispose.add(auxiliary.getCalledOnViewDispose());
      myBrowser.setTableContextMenu(group, (auxiliary.getPopupActions() == null) ? Collections.<AnAction>emptyList() : auxiliary.getPopupActions());
    }
    else {
      myBrowser.setTableContextMenu(group, Collections.<AnAction>emptyList());
    }

    AnAction anAction = ActionManager.getInstance().getAction("CommittedChanges.Refresh");
    anAction.registerCustomShortcutSet(CommonShortcuts.getRerun(), this);
    myBrowser.addFilter(myFilterComponent);
    myIfNotCachedReloader = myLocation == null ? null : (Consumer<String>)s -> refreshChanges(false);
  }

  public RepositoryLocation getRepositoryLocation() {
    return myLocation;
  }

  public void setMaxCount(int maxCount) {
    myMaxCount = maxCount;
  }

  public void setProvider(CommittedChangesProvider provider) {
    if (myProvider != provider) {
      myProvider = provider;
      mySettings = provider.createDefaultSettings();
    }
  }

  public void refreshChanges(boolean cacheOnly) {
    if (myLocation != null) {
      refreshChangesFromLocation();
    }
    else {
      refreshChangesFromCache(cacheOnly);
    }
  }

  private void refreshChangesFromLocation() {
    myBrowser.reset();

    myInLoad = true;
    myBrowser.setLoading(true);
    ProgressManager.getInstance().run(new Task.Backgroundable(myProject, "Loading changes", true, BackgroundFromStartOption.getInstance()) {

      @Override
      public void run(@Nonnull final ProgressIndicator indicator) {
        try {
          AsynchConsumer<List<CommittedChangeList>> appender = new AsynchConsumer<List<CommittedChangeList>>() {
            @Override
            public void finished() {
            }

            @Override
            public void accept(final List<CommittedChangeList> list) {
              new AbstractCalledLater((Project)myProject, IdeaModalityState.stateForComponent(myBrowser)) {
                @Override
                public void run() {
                  myBrowser.append(list);
                }
              }.callMe();
            }
          };
          final BufferedListConsumer<CommittedChangeList> bufferedListConsumer = new BufferedListConsumer<CommittedChangeList>(30, appender, -1);

          myProvider.loadCommittedChanges(mySettings, myLocation, myMaxCount, new AsynchConsumer<CommittedChangeList>() {
            @Override
            public void finished() {
              bufferedListConsumer.flush();
            }

            @Override
            public void accept(CommittedChangeList committedChangeList) {
              if (myDisposed) {
                indicator.cancel();
              }
              ProgressManager.checkCanceled();
              bufferedListConsumer.consumeOne(committedChangeList);
            }
          });
        }
        catch (final VcsException e) {
          LOG.info(e);
          WaitForProgressToShow.runOrInvokeLaterAboveProgress(new Runnable() {
            @Override
            public void run() {
              Messages.showErrorDialog((Project)myProject, "Error refreshing view: " + StringUtil.join(e.getMessages(), "\n"), "Committed Changes");
            }
          }, null, (Project)myProject);
        }
        finally {
          myInLoad = false;
          myBrowser.setLoading(false);
        }
      }
    });
  }

  public void clearCaches() {
    CommittedChangesCache cache = CommittedChangesCache.getInstance(myProject);
    cache.clearCaches(new Runnable() {
      @Override
      public void run() {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
          @Override
          public void run() {
            updateFilteredModel(Collections.<CommittedChangeList>emptyList(), true);
          }
        }, IdeaModalityState.nonModal(), myProject.getDisposed());
      }
    });
  }

  @NonNls
  private void refreshChangesFromCache(boolean cacheOnly) {
    CommittedChangesCache cache = CommittedChangesCache.getInstance(myProject);
    cache.hasCachesForAnyRoot(notEmpty -> {
      if (!notEmpty) {
        if (cacheOnly) {
          myBrowser.getEmptyText().setText(VcsLocalize.committedChangesNotLoadedMessage());
          return;
        }
        if (!CacheSettingsDialog.showSettingsDialog(myProject)) return;
      }
      cache.getProjectChangesAsync(
        mySettings,
        myMaxCount,
        cacheOnly,
        committedChangeLists -> updateFilteredModel(committedChangeLists, false),
        vcsExceptions -> AbstractVcsHelper.getInstance(myProject).showErrors(vcsExceptions, "Error refreshing VCS history")
      );
    });
  }

  private static class FilterHelper {
    private final String[] myParts;

    FilterHelper(String filterString) {
      myParts = filterString.split(" ");
      for (int i = 0; i < myParts.length; ++i) {
        myParts[i] = myParts[i].toLowerCase();
      }
    }

    public boolean filter(@Nonnull CommittedChangeList cl) {
      return changeListMatches(cl, myParts);
    }

    private static boolean changeListMatches(@Nonnull CommittedChangeList changeList, String[] filterWords) {
      for (String word : filterWords) {
        String comment = changeList.getComment();
        String committer = changeList.getCommitterName();
        if ((comment != null && comment.toLowerCase().indexOf(word) >= 0) ||
            (committer != null && committer.toLowerCase().indexOf(word) >= 0) ||
            Long.toString(changeList.getNumber()).indexOf(word) >= 0) {
          return true;
        }
      }
      return false;
    }
  }

  private void updateFilteredModel(List<CommittedChangeList> committedChangeLists, boolean reset) {
    if (committedChangeLists == null) {
      return;
    }
    LocalizeValue emptyText = reset ? VcsLocalize.committedChangesNotLoadedMessage() : VcsLocalize.committedChangesEmptyMessage();
    myBrowser.getEmptyText().setText(emptyText);
    myBrowser.setItems(committedChangeLists, CommittedChangesBrowserUseCase.COMMITTED);
  }

  public void setChangesFilter() {
    CommittedChangesFilterDialog filterDialog =
      new CommittedChangesFilterDialog(myProject, myProvider.createFilterUI(true), mySettings);
    filterDialog.show();
    if (filterDialog.isOK()) {
      mySettings = filterDialog.getSettings();
      refreshChanges(false);
    }
  }

  @Override
  public void calcData(Key<?> key, DataSink sink) {
    if (VcsDataKeys.REMOTE_HISTORY_CHANGED_LISTENER == key) {
      sink.put(VcsDataKeys.REMOTE_HISTORY_CHANGED_LISTENER, myIfNotCachedReloader);
    }
    else if (VcsDataKeys.REMOTE_HISTORY_LOCATION.equals(key)) {
      sink.put(VcsDataKeys.REMOTE_HISTORY_LOCATION, myLocation);
    }
    //if (key.equals(VcsDataKeys.CHANGES) || key.equals(VcsDataKeys.CHANGE_LISTS)) {
    myBrowser.calcData(key, sink);
    //}
  }

  @Override
  public void dispose() {
    for (Runnable runnable : myShouldBeCalledOnDispose) {
      runnable.run();
    }
    myDisposed = true;
  }

  private class MyFilterComponent extends FilterComponent implements ChangeListFilteringStrategy {
    private final List<ChangeListener> myList = ContainerUtil.createLockFreeCopyOnWriteList();

    public MyFilterComponent() {
      super("COMMITTED_CHANGES_FILTER_HISTORY", 20);
    }

    @Override
    public CommittedChangesFilterKey getKey() {
      return new CommittedChangesFilterKey("text", CommittedChangesFilterPriority.TEXT);
    }

    @Override
    public void filter() {
      for (ChangeListener changeListener : myList) {
        changeListener.stateChanged(new ChangeEvent(this));
      }
    }

    @Override
    public JComponent getFilterUI() {
      return null;
    }

    @Override
    public void setFilterBase(List<CommittedChangeList> changeLists) {
    }

    @Override
    public void addChangeListener(ChangeListener listener) {
      myList.add(listener);
    }

    @Override
    public void removeChangeListener(ChangeListener listener) {
      myList.remove(listener);
    }

    @Override
    public void resetFilterBase() {
    }

    @Override
    public void appendFilterBase(List<CommittedChangeList> changeLists) {
    }

    @Override
    @Nonnull
    public List<CommittedChangeList> filterChangeLists(List<CommittedChangeList> changeLists) {
      FilterHelper filterHelper = new FilterHelper(myFilterComponent.getFilter());
      List<CommittedChangeList> result = new ArrayList<CommittedChangeList>();
      for (CommittedChangeList list : changeLists) {
        if (filterHelper.filter(list)) {
          result.add(list);
        }
      }
      return result;
    }
  }

  public void passCachedListsToListener(final VcsBranchMappingChangedDetailedNotification notification, final Project project, final VirtualFile root) {
    final LinkedList<CommittedChangeList> resultList = new LinkedList<CommittedChangeList>();
    myBrowser.reportLoadedLists(new CommittedChangeListsListener() {
      @Override
      public void onBeforeStartReport() {
      }

      @Override
      public boolean report(CommittedChangeList list) {
        resultList.add(list);
        return false;
      }

      @Override
      public void onAfterEndReport() {
        if (!resultList.isEmpty()) {
          notification.execute(project, root, resultList);
        }
      }
    });
  }

  public boolean isInLoad() {
    return myInLoad;
  }
}
