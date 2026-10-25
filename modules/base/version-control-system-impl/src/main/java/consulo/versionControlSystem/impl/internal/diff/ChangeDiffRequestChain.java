package consulo.versionControlSystem.impl.internal.diff;

import consulo.diff.chain.DiffRequestChain;
import consulo.diff.combined.CombinedBlockProducer;
import consulo.diff.combined.CombinedDiffRequestChain;
import consulo.diff.combined.CombinedPathBlockId;
import consulo.diff.internal.GoToChangePopupBuilder;
import consulo.ui.ex.action.AnAction;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.diff.ChangeDiffRequestProducer;
import consulo.versionControlSystem.internal.ShowDiffContext;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ChangeDiffRequestChain extends UserDataHolderBase implements CombinedDiffRequestChain, GoToChangePopupBuilder.Chain {
    private final List<ChangeDiffRequestProducer> myRequests;
    private final ShowDiffContext myContext;
    private int myIndex;

    public ChangeDiffRequestChain(List<ChangeDiffRequestProducer> requests, ShowDiffContext context) {
        myRequests = requests;
        myContext = context;
    }

    @Override
    public List<? extends ChangeDiffRequestProducer> getRequests() {
        return myRequests;
    }

    @Override
    public int getIndex() {
        return myIndex;
    }

    @Override
    public List<CombinedBlockProducer> getCombinedBlocks() {
        List<CombinedBlockProducer> blocks = new ArrayList<>(myRequests.size());
        for (ChangeDiffRequestProducer producer : myRequests) {
            blocks.add(new CombinedBlockProducer(new CombinedPathBlockId(producer.getFilePath(), producer.getFileStatus()), producer));
        }
        return blocks;
    }

    @Override
    public void setIndex(int index) {
        if (index < 0 || myRequests.size() <= index) {
            throw new IndexOutOfBoundsException(index);
        }
        myIndex = index;
        myContext.selectCurrentChange(myRequests.get(index).getChange());
    }

    @Override
    public AnAction createGoToChangeAction(Consumer<Integer> onSelected) {
        return new ChangeGoToChangePopupAction<>(this, onSelected) {
            @Override
            protected int findSelectedStep(@Nullable Change change) {
                if (change == null) {
                    return -1;
                }
                for (int i = 0; i < myRequests.size(); i++) {
                    Change c = myRequests.get(i).getChange();
                    if (c.equals(change)) {
                        return i;
                    }
                }
                return -1;
            }

            @Override
            protected List<Change> getChanges() {
                return ContainerUtil.mapNotNull(myChain.getRequests(), ChangeDiffRequestProducer::getChange);
            }

            @Override
            protected Change getCurrentSelection() {
                return myChain.getRequests().get(myChain.getIndex()).getChange();
            }
        };
    }
}
