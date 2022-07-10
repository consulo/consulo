package consulo.ide.impl.idea.openapi.vcs.changes.actions.diff;

import consulo.ui.ex.action.AnAction;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.ide.impl.idea.diff.actions.impl.GoToChangePopupBuilder;
import consulo.diff.chain.DiffRequestChain;
import consulo.vcs.change.Change;
import consulo.ide.impl.idea.util.Consumer;
import consulo.ide.impl.idea.util.Function;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;

public class ChangeDiffRequestChain extends UserDataHolderBase implements DiffRequestChain, GoToChangePopupBuilder.Chain {
  @Nonnull
  private final List<ChangeDiffRequestProducer> myRequests;
  private int myIndex;

  public ChangeDiffRequestChain(@Nonnull List<ChangeDiffRequestProducer> requests) {
    myRequests = requests;
  }

  @Nonnull
  @Override
  public List<? extends ChangeDiffRequestProducer> getRequests() {
    return myRequests;
  }

  @Override
  public int getIndex() {
    return myIndex;
  }

  @Override
  public void setIndex(int index) {
    assert index >= 0 && index < myRequests.size();
    myIndex = index;
  }

  @Nonnull
  @Override
  public AnAction createGoToChangeAction(@Nonnull Consumer<Integer> onSelected) {
    return new ChangeGoToChangePopupAction<ChangeDiffRequestChain>(this, onSelected) {
      @Override
      protected int findSelectedStep(@Nullable Change change) {
        if (change == null) return -1;
        for (int i = 0; i < myRequests.size(); i++) {
          Change c = myRequests.get(i).getChange();
          if (c.equals(change)) return i;
        }
        return -1;
      }

      @Nonnull
      @Override
      protected List<Change> getChanges() {
        return ContainerUtil.mapNotNull(myChain.getRequests(), new Function<ChangeDiffRequestProducer, Change>() {
          @Override
          public Change fun(ChangeDiffRequestProducer presentable) {
            return presentable.getChange();
          }
        });
      }

      @javax.annotation.Nullable
      @Override
      protected Change getCurrentSelection() {
        return myChain.getRequests().get(myChain.getIndex()).getChange();
      }
    };
  }
}
