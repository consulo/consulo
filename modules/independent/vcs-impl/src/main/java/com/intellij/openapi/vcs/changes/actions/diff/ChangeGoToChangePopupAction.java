package com.intellij.openapi.vcs.changes.actions.diff;

import com.intellij.diff.actions.impl.GoToChangePopupBuilder;
import com.intellij.diff.chains.DiffRequestChain;
import com.intellij.diff.chains.DiffRequestProducer;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.changes.ui.ChangesBrowser;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.util.Consumer;
import com.intellij.util.ui.update.UiNotifyConnector;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class ChangeGoToChangePopupAction<Chain extends DiffRequestChain>
        extends GoToChangePopupBuilder.BaseGoToChangePopupAction<Chain>{
  public ChangeGoToChangePopupAction(@Nonnull Chain chain, @Nonnull Consumer<Integer> onSelected) {
    super(chain, onSelected);
  }

  @Nonnull
  @Override
  protected JBPopup createPopup(@Nonnull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) project = ProjectManager.getInstance().getDefaultProject();

    Ref<JBPopup> popup = new Ref<JBPopup>();
    ChangesBrowser cb = new MyChangesBrowser(project, getChanges(), getCurrentSelection(), popup);

    popup.set(JBPopupFactory.getInstance()
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
                      .createPopup());

    return popup.get();
  }

  //
  // Abstract
  //

  protected abstract int findSelectedStep(@javax.annotation.Nullable Change change);

  @Nonnull
  protected abstract List<Change> getChanges();

  @javax.annotation.Nullable
  protected abstract Change getCurrentSelection();

  //
  // Helpers
  //

  private class MyChangesBrowser extends ChangesBrowser implements Runnable {
    @Nonnull
    private final Ref<JBPopup> myPopup;

    public MyChangesBrowser(@Nonnull Project project,
                            @Nonnull List<Change> changes,
                            @Nullable final Change currentChange,
                            @Nonnull Ref<JBPopup> popup) {
      super(project, null, changes, null, false, false, null, MyUseCase.LOCAL_CHANGES, null);
      setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      setChangesToDisplay(changes);

      UiNotifyConnector.doWhenFirstShown(this, new Runnable() {
        @Override
        public void run() {
          if (currentChange != null) select(Collections.singletonList(currentChange));
        }
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
      IdeFocusManager.getInstance(myProject).doWhenFocusSettlesDown(new Runnable() {
        @Override
        public void run() {
          //noinspection unchecked
          myOnSelected.consume(index);
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

      myChanges = new ArrayList<Change>(requests.size());
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
    protected int findSelectedStep(@javax.annotation.Nullable Change change) {
      return myChanges.indexOf(change);
    }

    @Nonnull
    @Override
    protected List<Change> getChanges() {
      return myChanges;
    }

    @javax.annotation.Nullable
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
