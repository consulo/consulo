/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.versionControlSystem.log.impl.internal.data;

import consulo.application.Application;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.component.ProcessCanceledException;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.Lists;
import consulo.util.lang.Pair;
import consulo.versionControlSystem.log.VcsLogFilterCollection;
import consulo.versionControlSystem.log.graph.PermanentGraph;
import consulo.versionControlSystem.log.impl.internal.VcsLogFilterCollectionImpl;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class VcsLogFiltererImpl implements VcsLogFilterer {
  private static final Logger LOG = Logger.getInstance(VcsLogFiltererImpl.class);

  @Nonnull
  private final SingleTaskController<Request, VisiblePack> myTaskController;
  @Nonnull
  private final VisiblePackBuilder myVisiblePackBuilder;
  @Nonnull
  private final VcsLogDataImpl myLogData;

  @Nonnull
  private VcsLogFilterCollection myFilters;
  @Nonnull
  private PermanentGraph.SortType mySortType;
  @Nonnull
  private CommitCountStage myCommitCount = CommitCountStage.INITIAL;
  @Nonnull
  private List<MoreCommitsRequest> myRequestsToRun = new ArrayList<>();
  @Nonnull
  private List<VisiblePackChangeListener> myVisiblePackChangeListeners = Lists.newLockFreeCopyOnWriteList();
  @Nonnull
  private volatile VisiblePack myVisiblePack = VisiblePack.EMPTY;
  private volatile boolean myIsValid = true;

  public VcsLogFiltererImpl(
    @Nonnull final Project project,
    @Nonnull VcsLogDataImpl logData,
    @Nonnull PermanentGraph.SortType initialSortType
  ) {
    myLogData = logData;
    myVisiblePackBuilder = myLogData.createVisiblePackBuilder();
    myFilters = new VcsLogFilterCollectionImpl(null, null, null, null, null, null, null);
    mySortType = initialSortType;

    myTaskController = new SingleTaskController<>(visiblePack -> {
      myVisiblePack = visiblePack;
      for (VisiblePackChangeListener listener : myVisiblePackChangeListeners) {
        listener.onVisiblePackChange(visiblePack);
      }
    }) {
      @Override
      protected void startNewBackgroundTask() {
        UIUtil.invokeLaterIfNeeded(() -> {
          MyTask task = new MyTask(project, "Applying filters...");
          ProgressManager.getInstance().runProcessWithProgressAsynchronously(task,
                                                                             myLogData.getProgress().createProgressIndicator());
        });
      }
    };
  }

  @Override
  public void addVisiblePackChangeListener(@Nonnull VisiblePackChangeListener listener) {
    myVisiblePackChangeListeners.add(listener);
  }

  @Override
  public void removeVisiblePackChangeListener(@Nonnull VisiblePackChangeListener listener) {
    myVisiblePackChangeListeners.remove(listener);
  }

  @Override
  public void onRefresh() {
    myTaskController.request(new RefreshRequest());
  }

  @Override
  public void setValid(boolean validate) {
    myTaskController.request(new ValidateRequest(validate));
  }

  @Override
  public void onFiltersChange(@Nonnull VcsLogFilterCollection newFilters) {
    myTaskController.request(new FilterRequest(newFilters));
  }

  @Override
  public void onSortTypeChange(@Nonnull PermanentGraph.SortType sortType) {
    myTaskController.request(new SortTypeRequest(sortType));
  }

  @Override
  public void moreCommitsNeeded(@Nonnull Runnable onLoaded) {
    myTaskController.request(new MoreCommitsRequest(onLoaded));
  }

  @Override
  public boolean isValid() {
    return myIsValid;
  }

  private class MyTask extends Task.Backgroundable {

    public MyTask(@Nullable Project project, @Nonnull String title) {
      super(project, title, false);
    }

    @Override
    public void run(@Nonnull ProgressIndicator indicator) {
      VisiblePack visiblePack = null;
      List<Request> requests;
      while (!(requests = myTaskController.popRequests()).isEmpty()) {
        try {
          visiblePack = getVisiblePack(visiblePack, requests);
        }
        catch (ProcessCanceledException reThrown) {
          throw reThrown;
        }
        catch (Throwable t) {
          LOG.error("Error while filtering log", t);
        }
      }

      // visible pack can be null (e.g. when filter is set during initialization) => we just remember filters set by user
      myTaskController.taskCompleted(visiblePack);

      if (visiblePack != null && myIsValid) {
        List<MoreCommitsRequest> requestsToRun = myRequestsToRun;
        myRequestsToRun = new ArrayList<>();

        Application.get().invokeLater(() -> {
          for (MoreCommitsRequest request : requestsToRun) {
            request.onLoaded.run();
          }
        });
      }
    }

    @Nullable
    private VisiblePack getVisiblePack(@Nullable VisiblePack visiblePack, @Nonnull List<Request> requests) {
      ValidateRequest validateRequest = Lists.findLastInstance(requests, ValidateRequest.class);
      FilterRequest filterRequest = Lists.findLastInstance(requests, FilterRequest.class);
      SortTypeRequest sortTypeRequest = Lists.findLastInstance(requests, SortTypeRequest.class);
      List<MoreCommitsRequest> moreCommitsRequests = ContainerUtil.findAll(requests, MoreCommitsRequest.class);

      myRequestsToRun.addAll(moreCommitsRequests);
      if (filterRequest != null) {
        myFilters = filterRequest.filters;
      }
      if (sortTypeRequest != null) {
        mySortType = sortTypeRequest.sortType;
      }

      // On validate requests vs refresh requests.
      // Validate just changes validity (myIsValid field). If myIsValid is already what it needs to be it does nothing.
      // Refresh just tells that new data pack arrived. It does not make this filterer valid (or invalid).
      // So, this two requests bring here two completely different pieces of information.
      // Refresh requests are not explicitly used in this code. Basically what is done is a check that there are some requests apart from
      // instances of ValidateRequest (also we get into this method only when there are some requests in the queue).
      // Refresh request does not carry inside any additional information since current DataPack is just taken from VcsLogDataManager.

      if (!myIsValid) {
        if (validateRequest != null && validateRequest.validate) {
          myIsValid = true;
          return refresh(visiblePack, filterRequest, moreCommitsRequests);
        }
        else { // validateRequest == null || !validateRequest.validate
          // remember filters
          return visiblePack;
        }
      }
      else {
        if (validateRequest != null && !validateRequest.validate) {
          myIsValid = false;
          // invalidate
          VisiblePack frozenVisiblePack = visiblePack == null ? myVisiblePack : visiblePack;
          if (filterRequest != null) {
            frozenVisiblePack = refresh(visiblePack, filterRequest, moreCommitsRequests);
          }
          return new FakeVisiblePackBuilder(myLogData.getHashMap()).build(frozenVisiblePack);
        }

        Request nonValidateRequest = ContainerUtil.find(requests, request -> !(request instanceof ValidateRequest));

        if (nonValidateRequest != null) {
          // only doing something if there are some other requests
          return refresh(visiblePack, filterRequest, moreCommitsRequests);
        }
        else {
          return visiblePack;
        }
      }
    }

    private VisiblePack refresh(@Nullable VisiblePack visiblePack,
                                @Nullable FilterRequest filterRequest,
                                @Nonnull List<MoreCommitsRequest> moreCommitsRequests) {
      DataPack dataPack = myLogData.getDataPack();

      if (dataPack == DataPack.EMPTY) { // when filter is set during initialization, just remember filters
        return visiblePack;
      }

      if (filterRequest != null) {
        // "more commits needed" has no effect if filter changes; it also can't come after filter change request
        myCommitCount = CommitCountStage.INITIAL;
      }
      else if (!moreCommitsRequests.isEmpty()) {
        myCommitCount = myCommitCount.next();
      }

      Pair<VisiblePack, CommitCountStage> pair = myVisiblePackBuilder.build(dataPack, mySortType, myFilters, myCommitCount);
      visiblePack = pair.first;
      myCommitCount = pair.second;
      return visiblePack;
    }
  }

  private interface Request {
  }

  private static final class RefreshRequest implements Request {
  }

  private static final class ValidateRequest implements Request {
    private final boolean validate;

    private ValidateRequest(boolean validate) {
      this.validate = validate;
    }
  }

  private static final class FilterRequest implements Request {
    private final VcsLogFilterCollection filters;

    FilterRequest(VcsLogFilterCollection filters) {
      this.filters = filters;
    }
  }

  private static final class SortTypeRequest implements Request {
    private final PermanentGraph.SortType sortType;

    SortTypeRequest(PermanentGraph.SortType sortType) {
      this.sortType = sortType;
    }
  }

  private static final class MoreCommitsRequest implements Request {
    @Nonnull
    private final Runnable onLoaded;

    MoreCommitsRequest(@Nonnull Runnable onLoaded) {
      this.onLoaded = onLoaded;
    }
  }
}
