// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.impl.internal.diff;

import consulo.annotation.component.ServiceImpl;
import consulo.application.util.ListSelection;
import consulo.diff.DiffUserDataKeys;
import consulo.diff.chain.DiffRequestProducer;
import consulo.diff.combined.CombinedBlockId;
import consulo.diff.combined.CombinedBlockProducer;
import consulo.diff.combined.CombinedDiffComponentProcessor;
import consulo.diff.combined.CombinedDiffManager;
import consulo.diff.combined.CombinedPathBlockId;
import consulo.diff.impl.internal.combined.CombinedDiffComponentProcessorImpl;
import consulo.diff.impl.internal.combined.CombinedDiffModel;
import consulo.diff.impl.internal.combined.CombinedDiffViewer;
import consulo.diff.impl.internal.combined.CombinedDiffViewerKeys;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.versionControlSystem.change.PresentableChange;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@ServiceImpl
@Singleton
public class CombinedDiffManagerImpl implements CombinedDiffManager {
    private final Project myProject;

    @Inject
    public CombinedDiffManagerImpl(Project project) {
        myProject = project;
    }

    @RequiredUIAccess
    @Override
    public CombinedDiffComponentProcessor createProcessor(
        @Nullable String diffPlace,
        @Nullable List<AnAction> contextActions,
        @Nullable List<AnAction> goToChangeToolbarActions
    ) {
        CombinedDiffModel model = new CombinedDiffModel(myProject);
        model.getContext().putUserData(DiffUserDataKeys.PLACE, diffPlace);
        model.getContext().putUserData(DiffUserDataKeys.CONTEXT_ACTIONS, contextActions);
        MyGoToChangePopupController controller = new MyGoToChangePopupController(model);
        AnAction goToChangePopupAction = new PresentableGoToChangePopupAction<>(controller);
        return new CombinedDiffComponentProcessorImpl(model, goToChangePopupAction);
    }

    private static final class MyGoToChangePopupController implements MutableDiffRequestProcessorNavigator<PresentableChange> {
        private final CombinedDiffModel myModel;

        private MyGoToChangePopupController(CombinedDiffModel model) {
            myModel = model;
        }

        private @Nullable CombinedDiffViewer getViewer() {
            return myModel.getContext().getUserData(CombinedDiffViewerKeys.COMBINED_DIFF_VIEWER_KEY);
        }

        @Override
        public ListSelection<PresentableChange> getCurrentList() {
            List<PresentableChange> changes = new ArrayList<>();
            for (CombinedBlockProducer request : myModel.getRequests()) {
                DiffRequestProducer producer = request.getProducer();
                if (producer instanceof PresentableChange presentableChange) {
                    changes.add(presentableChange);
                }
            }

            CombinedDiffViewer viewer = getViewer();
            CombinedBlockId currentBlockId = viewer != null ? viewer.getCurrentBlockId() : null;
            int selectedIndex = -1;
            if (currentBlockId instanceof CombinedPathBlockId selected) {
                for (int i = 0; i < changes.size(); i++) {
                    PresentableChange change = changes.get(i);
                    if (Objects.equals(change.getTag(), selected.getTag())
                        && Objects.equals(change.getFileStatus(), selected.getFileStatus())
                        && change.getFilePath().equals(selected.getPath())) {
                        selectedIndex = i;
                        break;
                    }
                }
            }
            return ListSelection.createAt(changes, selectedIndex);
        }

        @Override
        public void selectNext(boolean fromDifferences) {
        }

        @Override
        public void selectPrev(boolean fromDifferences) {
        }

        @RequiredUIAccess
        @Override
        public void select(PresentableChange change) {
            CombinedDiffViewer viewer = getViewer();
            if (viewer != null) {
                viewer.selectDiffBlock(
                    new CombinedPathBlockId(change.getFilePath(), change.getFileStatus(), change.getTag()),
                    true,
                    CombinedDiffViewer.ScrollPolicy.SCROLL_TO_BLOCK
                );
            }
        }

        @Override
        public @Nullable PresentableChange getChangePresentation(PresentableChange change) {
            return change;
        }
    }
}
