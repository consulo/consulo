package consulo.ide.impl.idea.openapi.vcs.changes.actions.diff;

import consulo.diff.chain.DiffRequestChain;
import consulo.diff.chain.DiffRequestProducer;
import consulo.ide.impl.idea.diff.actions.impl.GoToChangePopupBuilder;
import consulo.ide.impl.idea.openapi.vcs.changes.ui.ChangesBrowser;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.awt.update.UiNotifyConnector;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.util.lang.ref.Ref;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ContentRevision;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import consulo.virtualFileSystem.status.FileStatus;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public abstract class ChangeGoToChangePopupAction<Chain extends DiffRequestChain>
        extends GoToChangePopupBuilder.BaseGoToChangePopupAction<Chain>{
  public ChangeGoToChangePopupAction(@Nonnull Chain chain, @Nonnull Consumer<Integer> onSelected) {
    super(chain, onSelected);
  }

  @Nonnull
  @Override
  protected JBPopup createPopup(@Nonnull AnActionEvent e) {
    Project project = e.getData(Project.KEY);
    if (project == null) project = ProjectManager.getInstance().getDefaultProject();

    Ref<JBPopup> popup = new Ref<>();
    ChangesBrowser cb = new MyChangesBrowser(project, getChanges(), getCurrentSelection(), popup);

    popup.set(
      JBPopupFactory.getInstance()
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
        .createPopup()
    );

    return popup.get();
  }

  //
  // Abstract
  //

  protected abstract int findSelectedStep(@Nullable Change change);

  @Nonnull
  protected abstract List<Change> getChanges();

  @Nullable
  protected abstract Change getCurrentSelection();

  //
  // Helpers
  //

  private class MyChangesBrowser extends ChangesBrowser implements Runnable {
    @Nonnull
    private final Ref<JBPopup> myPopup;

    public MyChangesBrowser(
      @Nonnull Project project,
      @Nonnull List<Change> changes,
      @Nullable final Change currentChange,
      @Nonnull Ref<JBPopup> popup
    ) {
      super(project, null, changes, null, false, false, null, MyUseCase.LOCAL_CHANGES, null);
      setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      setChangesToDisplay(changes);

      UiNotifyConnector.doWhenFirstShown(this, () -> {
        if (currentChange != null) select(Collections.singletonList(currentChange));
      });

      myPopup = popup;
    }

    @Override
    protected void buildToolBar(DefaultActionGroup toolBarGroup) {
      // remove diff action
    }

    @Nonnull
    @Override
    protected Runnable getDoubleClickHandler() {
      return this;
    }

    @Override
    public void run() {
      Change change = getSelectedChanges().get(0);
      final int index = findSelectedStep(change);
      myPopup.get().cancel();
      ProjectIdeFocusManager.getInstance(myProject).doWhenFocusSettlesDown(new Runnable() {
        @Override
        public void run() {
          //noinspection unchecked
          myOnSelected.accept(index);
        }
      });
    }
  }

  public abstract static class Fake<Chain extends DiffRequestChain> extends ChangeGoToChangePopupAction<Chain> {
    @Nonnull
    private final List<Change> myChanges;
    private final int mySelection;

    @SuppressWarnings("AbstractMethodCallInConstructor")
    public Fake(@Nonnull Chain chain, int selection, @Nonnull Consumer<Integer> onSelected) {
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

    @Nonnull
    protected abstract FilePath getFilePath(int index);

    @Nonnull
    protected abstract FileStatus getFileStatus(int index);

    @Override
    protected int findSelectedStep(@Nullable Change change) {
      return myChanges.indexOf(change);
    }

    @Nonnull
    @Override
    protected List<Change> getChanges() {
      return myChanges;
    }

    @Nullable
    @Override
    protected Change getCurrentSelection() {
      if (mySelection < 0 || mySelection >= myChanges.size()) return null;
      return myChanges.get(mySelection);
    }

    private static class FakeContentRevision implements ContentRevision {
      @Nonnull
      private final FilePath myFilePath;

      public FakeContentRevision(@Nonnull FilePath filePath) {
        myFilePath = filePath;
      }

      @Nullable
      @Override
      public String getContent() throws VcsException {
        return null;
      }

      @Nonnull
      @Override
      public FilePath getFile() {
        return myFilePath;
      }

      @Nonnull
      @Override
      public VcsRevisionNumber getRevisionNumber() {
        return VcsRevisionNumber.NULL;
      }
    }
  }
}
