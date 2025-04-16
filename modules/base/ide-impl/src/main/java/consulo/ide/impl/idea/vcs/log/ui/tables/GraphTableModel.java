package consulo.ide.impl.idea.vcs.log.ui.tables;

import consulo.util.lang.function.Condition;
import consulo.util.lang.EmptyRunnable;
import consulo.versionControlSystem.log.*;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ide.impl.idea.util.NotNullFunction;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.application.util.DateFormatUtil;
import consulo.ide.impl.idea.vcs.log.data.*;
import consulo.versionControlSystem.log.util.VcsLogUtil;
import consulo.ide.impl.idea.vcs.log.ui.VcsLogUiImpl;
import consulo.ide.impl.idea.vcs.log.ui.render.GraphCommitCell;
import consulo.versionControlSystem.log.util.VcsUserUtil;
import jakarta.annotation.Nonnull;

import jakarta.annotation.Nullable;
import javax.swing.table.AbstractTableModel;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class GraphTableModel extends AbstractTableModel {
  public static final int ROOT_COLUMN = 0;
  public static final int COMMIT_COLUMN = 1;
  public static final int AUTHOR_COLUMN = 2;
  public static final int DATE_COLUMN = 3;
  private static final int COLUMN_COUNT = DATE_COLUMN + 1;
  private static final String[] COLUMN_NAMES = {"", "Subject", "Author", "Date"};

  private static final int UP_PRELOAD_COUNT = 20;
  private static final int DOWN_PRELOAD_COUNT = 40;

  @Nonnull
  private final VcsLogDataImpl myLogData;
  @Nonnull
  protected final VcsLogUiImpl myUi;

  @Nonnull
  protected VisiblePack myDataPack;

  private boolean myMoreRequested;

  public GraphTableModel(@Nonnull VisiblePack dataPack, @Nonnull VcsLogDataImpl logData, @Nonnull VcsLogUiImpl ui) {
    myLogData = logData;
    myUi = ui;
    myDataPack = dataPack;
  }

  @Override
  public int getRowCount() {
    return myDataPack.getVisibleGraph().getVisibleCommitCount();
  }

  @Nonnull
  public VirtualFile getRoot(int rowIndex) {
    return myDataPack.getRoot(rowIndex);
  }

  @Nonnull
  public Integer getIdAtRow(int row) {
    return myDataPack.getVisibleGraph().getRowInfo(row).getCommit();
  }

  @Nullable
  public CommitId getCommitIdAtRow(int row) {
    return myLogData.getCommitId(getIdAtRow(row));
  }

  public int getRowOfCommit(@Nonnull final Hash hash, @Nonnull VirtualFile root) {
    final int commitIndex = myLogData.getCommitIndex(hash, root);
    return ContainerUtil.indexOf(VcsLogUtil.getVisibleCommits(myDataPack.getVisibleGraph()), (Condition<Integer>)i -> i == commitIndex);
  }

  public int getRowOfCommitByPartOfHash(@Nonnull String partialHash) {
    final CommitIdByStringCondition hashByString = new CommitIdByStringCondition(partialHash);
    CommitId commitId = myLogData.getHashMap().findCommitId(
            commitId1 -> hashByString.test(commitId1) && getRowOfCommit(commitId1.getHash(), commitId1.getRoot()) != -1);
    return commitId != null ? getRowOfCommit(commitId.getHash(), commitId.getRoot()) : -1;
  }

  @Override
  public final int getColumnCount() {
    return COLUMN_COUNT;
  }

  /**
   * Requests the proper data provider to load more data from the log & recreate the model.
   *
   * @param onLoaded will be called upon task completion on the EDT.
   */
  public void requestToLoadMore(@Nonnull Runnable onLoaded) {
    myMoreRequested = true;
    myUi.getFilterer().moreCommitsNeeded(onLoaded);
    myUi.getTable().setPaintBusy(true);
  }

  @Nonnull
  @Override
  public final Object getValueAt(int rowIndex, int columnIndex) {
    if (rowIndex >= getRowCount() - 1 && canRequestMore()) {
      requestToLoadMore(EmptyRunnable.INSTANCE);
    }

    VcsShortCommitDetails data = getShortDetails(rowIndex);
    switch (columnIndex) {
      case ROOT_COLUMN:
        return getRoot(rowIndex);
      case COMMIT_COLUMN:
        return new GraphCommitCell(data.getSubject(), getRefsAtRow(rowIndex),
                                   myDataPack.getVisibleGraph().getRowInfo(rowIndex).getPrintElements());
      case AUTHOR_COLUMN:
        String authorString = VcsUserUtil.getShortPresentation(data.getAuthor());
        return authorString + (VcsUserUtil.isSamePerson(data.getAuthor(), data.getCommitter()) ? "" : "*");
      case DATE_COLUMN:
        if (data.getAuthorTime() < 0) {
          return "";
        }
        else {
          return DateFormatUtil.formatDateTime(data.getAuthorTime());
        }
      default:
        throw new IllegalArgumentException("columnIndex is " + columnIndex + " > " + (COLUMN_COUNT - 1));
    }
  }

  /**
   * Returns true if not all data has been loaded, i.e. there is sense to {@link #requestToLoadMore(Runnable) request more data}.
   */
  public boolean canRequestMore() {
    return !myMoreRequested && myDataPack.canRequestMore();
  }

  @Override
  public Class<?> getColumnClass(int column) {
    switch (column) {
      case ROOT_COLUMN:
        return VirtualFile.class;
      case COMMIT_COLUMN:
        return GraphCommitCell.class;
      case AUTHOR_COLUMN:
        return String.class;
      case DATE_COLUMN:
        return String.class;
      default:
        throw new IllegalArgumentException("columnIndex is " + column + " > " + (COLUMN_COUNT - 1));
    }
  }

  @Override
  public String getColumnName(int column) {
    return COLUMN_NAMES[column];
  }

  public void setVisiblePack(@Nonnull VisiblePack visiblePack) {
    myDataPack = visiblePack;
    myMoreRequested = false;
    fireTableDataChanged();
  }

  @Nonnull
  public VisiblePack getVisiblePack() {
    return myDataPack;
  }

  @Nonnull
  public VcsFullCommitDetails getFullDetails(int row) {
    return getDetails(row, myLogData.getCommitDetailsGetter());
  }

  @Nonnull
  public VcsShortCommitDetails getShortDetails(int row) {
    return getDetails(row, myLogData.getMiniDetailsGetter());
  }

  @Nonnull
  private <T extends VcsShortCommitDetails> T getDetails(int row, @Nonnull DataGetter<T> dataGetter) {
    Iterable<Integer> iterable = createRowsIterable(row, UP_PRELOAD_COUNT, DOWN_PRELOAD_COUNT, getRowCount());
    return dataGetter.getCommitData(getIdAtRow(row), iterable);
  }

  @Nonnull
  public Collection<VcsRef> getRefsAtRow(int row) {
    return ((RefsModel)myDataPack.getRefs()).refsToCommit(getIdAtRow(row));
  }

  @Nonnull
  public List<VcsRef> getBranchesAtRow(int row) {
    return getRefsAtRow(row).stream().filter(ref -> ref.getType().isBranch()).collect(Collectors.toList());
  }

  @Nonnull
  private Iterable<Integer> createRowsIterable(final int row, final int above, final int below, final int maxRows) {
    return () -> new Iterator<Integer>() {
      private int myRowIndex = Math.max(0, row - above);

      @Override
      public boolean hasNext() {
        return myRowIndex < row + below && myRowIndex < maxRows;
      }

      @Override
      public Integer next() {
        int nextRow = myRowIndex;
        myRowIndex++;
        return getIdAtRow(nextRow);
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException("Removing elements is not supported.");
      }
    };
  }

  @Nonnull
  public List<Integer> convertToCommitIds(@Nonnull List<Integer> rows) {
    return ContainerUtil.map(rows, (NotNullFunction<Integer, Integer>)this::getIdAtRow);
  }
}
