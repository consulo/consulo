package consulo.versionControlSystem.impl.internal.diff;

import consulo.diff.chain.DiffRequestChain;
import consulo.diff.chain.DiffRequestProducer;
import consulo.diff.internal.GoToChangePopupBuilder;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.awt.update.UiNotifyConnector;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.util.lang.ref.SimpleReference;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangesBrowser;
import consulo.versionControlSystem.change.ContentRevision;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import consulo.versionControlSystem.impl.internal.ui.awt.InternalChangesBrowser;
import consulo.virtualFileSystem.status.FileStatus;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public abstract class ChangeGoToChangePopupAction<Chain extends DiffRequestChain>
    extends GoToChangePopupBuilder.BaseGoToChangePopupAction<Chain> {
    public ChangeGoToChangePopupAction(Chain chain, Consumer<Integer> onSelected) {
        super(chain, onSelected);
    }

    
    @Override
    protected JBPopup createPopup(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            project = ProjectManager.getInstance().getDefaultProject();
        }

        SimpleReference<JBPopup> popupRef = new SimpleReference<>();
        InternalChangesBrowser cb = new MyChangesBrowser(project, getChanges(), getCurrentSelection(), popupRef);

        JBPopup popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(cb, cb.getPreferredFocusedComponent())
            .setResizable(true)
            .setModalContext(false)
            .setFocusable(true)
            .setRequestFocus(true)
            .setCancelOnWindowDeactivation(true)
            .setCancelOnOtherWindowOpen(true)
            .setMovable(true)
            .setCancelKeyEnabled(true)
            .setCancelOnClickOutside(true)
            .setDimensionServiceKey(project, "Diff.GoToChangePopup", false)
            .createPopup();

        popupRef.set(popup);

        return popup;
    }

    //
    // Abstract
    //

    protected abstract int findSelectedStep(@Nullable Change change);

    
    protected abstract List<Change> getChanges();

    protected abstract @Nullable Change getCurrentSelection();

    //
    // Helpers
    //

    private class MyChangesBrowser extends InternalChangesBrowser implements Runnable {
        
        private final SimpleReference<JBPopup> myPopup;

        public MyChangesBrowser(
            Project project,
            List<Change> changes,
            @Nullable Change currentChange,
            SimpleReference<JBPopup> popup
        ) {
            super(project, null, changes, null, false, false, null, ChangesBrowser.MyUseCase.LOCAL_CHANGES, null);
            setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            setChangesToDisplay(changes);

            UiNotifyConnector.doWhenFirstShown(
                this,
                () -> {
                    if (currentChange != null) {
                        select(Collections.singletonList(currentChange));
                    }
                }
            );

            myPopup = popup;
        }

        @Override
        protected void buildToolBar(DefaultActionGroup toolBarGroup) {
            // remove diff action
        }

        
        @Override
        protected Runnable getDoubleClickHandler() {
            return this;
        }

        @Override
        public void run() {
            Change change = getSelectedChanges().get(0);
            int index = findSelectedStep(change);
            myPopup.get().cancel();
            ProjectIdeFocusManager.getInstance(myProject).doWhenFocusSettlesDown(() -> {
                //noinspection unchecked
                myOnSelected.accept(index);
            });
        }
    }

    public abstract static class Fake<Chain extends DiffRequestChain> extends ChangeGoToChangePopupAction<Chain> {
        
        private final List<Change> myChanges;
        private final int mySelection;

        @SuppressWarnings("AbstractMethodCallInConstructor")
        public Fake(Chain chain, int selection, Consumer<Integer> onSelected) {
            super(chain, onSelected);

            mySelection = selection;

            // we want to show ChangeBrowser-based popup, so have to create some fake changes
            List<? extends DiffRequestProducer> requests = chain.getRequests();

            myChanges = new ArrayList<>(requests.size());
            for (int i = 0; i < requests.size(); i++) {
                FilePath path = getFilePath(i);
                FileStatus status = getFileStatus(i);
                FakeContentRevision revision = new FakeContentRevision(path);
                myChanges.add(new Change(revision, revision, status));
            }
        }

        
        protected abstract FilePath getFilePath(int index);

        
        protected abstract FileStatus getFileStatus(int index);

        @Override
        protected int findSelectedStep(@Nullable Change change) {
            return myChanges.indexOf(change);
        }

        
        @Override
        protected List<Change> getChanges() {
            return myChanges;
        }

        @Override
        protected @Nullable Change getCurrentSelection() {
            if (mySelection < 0 || mySelection >= myChanges.size()) {
                return null;
            }
            return myChanges.get(mySelection);
        }

        private static class FakeContentRevision implements ContentRevision {
            
            private final FilePath myFilePath;

            public FakeContentRevision(FilePath filePath) {
                myFilePath = filePath;
            }

            @Override
            public @Nullable String getContent() throws VcsException {
                return null;
            }

            
            @Override
            public FilePath getFile() {
                return myFilePath;
            }

            
            @Override
            public VcsRevisionNumber getRevisionNumber() {
                return VcsRevisionNumber.NULL;
            }
        }
    }
}
