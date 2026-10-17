/*
 * Copyright 2013-2026 consulo.io
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
package consulo.versionControlSystem.distributed.impl.internal.push;

import consulo.versionControlSystem.distributed.push.PushSource;
import consulo.versionControlSystem.distributed.push.PushSupport;
import consulo.versionControlSystem.distributed.push.PushTarget;
import consulo.versionControlSystem.distributed.push.PushTargetPanel;
import consulo.versionControlSystem.distributed.push.VcsError;
import consulo.versionControlSystem.distributed.repository.Repository;
import consulo.versionControlSystem.log.VcsFullCommitDetails;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
final class PushRepoModel<R extends Repository, S extends PushSource, T extends PushTarget> {
    static final int DEFAULT_CHILDREN_PRESENTATION_NUMBER = 20;

    private final R myRepository;
    private final PushSupport<R, S, T> mySupport;
    private final S mySource;
    private final String myRepositoryName;
    private final boolean mySingle;
    private final PushTargetPanel<T> myTargetPanel;

    private @Nullable T myTarget;
    private @Nullable VcsError myTargetError;
    private int myNumberOfShownCommits = DEFAULT_CHILDREN_PRESENTATION_NUMBER;
    private List<? extends VcsFullCommitDetails> myLoadedCommits = List.of();
    private List<VcsError> myErrors = List.of();
    private boolean myChecked;
    private boolean myEnabled;
    private boolean myLoading;
    private @Nullable Future<?> myFuture;

    PushRepoModel(R repository,
                  PushSupport<R, S, T> support,
                  boolean single,
                  String repositoryName,
                  S source,
                  @Nullable T target,
                  PushTargetPanel<T> targetPanel) {
        myRepository = repository;
        mySupport = support;
        mySingle = single;
        myChecked = single;
        myRepositoryName = repositoryName;
        mySource = source;
        myTarget = target;
        myTargetPanel = targetPanel;
        myEnabled = target != null;
    }

    R getRepository() {
        return myRepository;
    }

    PushSupport<R, S, T> getSupport() {
        return mySupport;
    }

    S getSource() {
        return mySource;
    }

    String getRepositoryName() {
        return myRepositoryName;
    }

    boolean isSingle() {
        return mySingle;
    }

    PushTargetPanel<T> getTargetPanel() {
        return myTargetPanel;
    }

    @Nullable T getTarget() {
        return myTarget;
    }

    void setTarget(@Nullable T target) {
        myTarget = target;
    }

    boolean isSelected() {
        return myChecked;
    }

    void setChecked(boolean checked) {
        myChecked = mySingle || checked;
    }

    boolean isEnabled() {
        return myEnabled;
    }

    void setEnabled(boolean enabled) {
        myEnabled = enabled;
    }

    boolean isEditable() {
        return myTargetPanel.isEditable();
    }

    void setError(@Nullable VcsError error) {
        myTargetError = error;
    }

    void clearErrors() {
        myTargetError = null;
    }

    boolean hasError() {
        return myTargetError != null;
    }

    int getNumberOfShownCommits() {
        return myNumberOfShownCommits;
    }

    void increaseShownCommits() {
        myNumberOfShownCommits *= 2;
    }

    List<? extends VcsFullCommitDetails> getLoadedCommits() {
        return myLoadedCommits;
    }

    void setLoadedCommits(List<? extends VcsFullCommitDetails> loadedCommits) {
        myLoadedCommits = loadedCommits;
    }

    void setShownErrors(List<VcsError> errors) {
        myErrors = errors;
    }

    boolean hasCommitInfo() {
        return myTargetError != null || !myLoadedCommits.isEmpty();
    }

    boolean isLoading() {
        return myLoading;
    }

    void startLoading(Future<?> future) {
        myFuture = future;
        myLoading = true;
    }

    void stopLoading() {
        myLoading = false;
    }

    void cancelLoading() {
        Future<?> future = myFuture;
        if (future != null && !future.isDone()) {
            future.cancel(true);
        }
    }

    List<Object> getRows() {
        List<Object> rows = new ArrayList<>();
        if (!myErrors.isEmpty()) {
            for (VcsError error : myErrors) {
                rows.add(new PushErrorRow(this, error));
            }
            return rows;
        }

        List<? extends VcsFullCommitDetails> commits = myLoadedCommits;
        for (int i = 0; i < commits.size(); i++) {
            if (i >= myNumberOfShownCommits) {
                rows.add(new PushMoreCommitsRow(this));
                break;
            }
            rows.add(new PushCommitRow(this, commits.get(i)));
        }
        return rows;
    }

    @Override
    public String toString() {
        T target = myTargetPanel.getValue();
        return myRepositoryName + " " + mySource.getPresentation() + (target != null ? " -> " + target.getPresentation() : "");
    }
}
