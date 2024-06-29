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
package consulo.ide.impl.idea.vcs.log.ui.frame;

import com.google.common.primitives.Ints;
import consulo.application.util.DateFormatUtil;
import consulo.application.util.registry.Registry;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataProvider;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.impl.idea.vcs.log.data.VcsLogDataImpl;
import consulo.ide.impl.idea.vcs.log.data.VcsLogProgress;
import consulo.ide.impl.idea.vcs.log.data.VisiblePack;
import consulo.ide.impl.idea.vcs.log.graph.DefaultColorGenerator;
import consulo.ide.impl.idea.vcs.log.paint.GraphCellPainter;
import consulo.ide.impl.idea.vcs.log.paint.SimpleGraphCellPainter;
import consulo.ide.impl.idea.vcs.log.ui.VcsLogActionPlaces;
import consulo.ide.impl.idea.vcs.log.ui.VcsLogColorManager;
import consulo.ide.impl.idea.vcs.log.ui.VcsLogColorManagerImpl;
import consulo.ide.impl.idea.vcs.log.ui.VcsLogUiImpl;
import consulo.ide.impl.idea.vcs.log.ui.render.GraphCommitCell;
import consulo.ide.impl.idea.vcs.log.ui.render.GraphCommitCellRenderer;
import consulo.ide.impl.idea.vcs.log.ui.tables.GraphTableModel;
import consulo.logging.Logger;
import consulo.ui.ex.CopyProvider;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.speedSearch.SpeedSearchUtil;
import consulo.ui.ex.awt.table.JBTable;
import consulo.util.dataholder.Key;
import consulo.util.lang.Couple;
import consulo.util.lang.Pair;
import consulo.versionControlSystem.log.VcsCommitStyleFactory;
import consulo.versionControlSystem.log.VcsLogHighlighter;
import consulo.versionControlSystem.log.VcsShortCommitDetails;
import consulo.versionControlSystem.log.graph.RowInfo;
import consulo.versionControlSystem.log.graph.RowType;
import consulo.versionControlSystem.log.graph.VisibleGraph;
import consulo.versionControlSystem.log.graph.action.GraphAnswer;
import consulo.versionControlSystem.log.util.VcsLogUtil;
import consulo.virtualFileSystem.VirtualFile;
import gnu.trove.TIntHashSet;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.basic.BasicTableHeaderUI;
import javax.swing.table.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;

import static consulo.versionControlSystem.log.VcsLogHighlighter.TextStyle.BOLD;
import static consulo.versionControlSystem.log.VcsLogHighlighter.TextStyle.ITALIC;

public class VcsLogGraphTable extends TableWithProgress implements DataProvider, CopyProvider {
  private static final Logger LOG = Logger.getInstance(VcsLogGraphTable.class);

  public static final int ROOT_INDICATOR_WHITE_WIDTH = 5;
  private static final int ROOT_INDICATOR_WIDTH = ROOT_INDICATOR_WHITE_WIDTH + 8;
  private static final int ROOT_NAME_MAX_WIDTH = 200;
  private static final int MAX_DEFAULT_AUTHOR_COLUMN_WIDTH = 200;
  private static final int MAX_ROWS_TO_CALC_WIDTH = 1000;

  @Nonnull
  private final VcsLogUiImpl myUi;
  @Nonnull
  private final VcsLogDataImpl myLogData;
  @Nonnull
  private final MyDummyTableCellEditor myDummyEditor = new MyDummyTableCellEditor();
  @Nonnull
  private final TableCellRenderer myDummyRenderer = new DefaultTableCellRenderer();
  @Nonnull
  private final GraphCommitCellRenderer myGraphCommitCellRenderer;
  @Nonnull
  private final GraphTableController myController;
  @Nonnull
  private final StringCellRenderer myStringCellRenderer;
  private boolean myColumnsSizeInitialized = false;

  @Nullable private Selection mySelection = null;

  @Nonnull
  private final Collection<VcsLogHighlighter> myHighlighters = new ArrayList<>();

  public VcsLogGraphTable(@Nonnull VcsLogUiImpl ui, @Nonnull VcsLogDataImpl logData, @Nonnull VisiblePack initialDataPack) {
    super(new GraphTableModel(initialDataPack, logData, ui));
    getEmptyText().setText("Changes Log");

    myUi = ui;
    myLogData = logData;
    GraphCellPainter graphCellPainter = new SimpleGraphCellPainter(new DefaultColorGenerator()) {
      @Override
      protected int getRowHeight() {
        return VcsLogGraphTable.this.getRowHeight();
      }
    };
    myGraphCommitCellRenderer =
            new GraphCommitCellRenderer(logData, graphCellPainter, this, ui.isCompactReferencesView(), ui.isShowTagNames());
    myStringCellRenderer = new StringCellRenderer();

    myLogData.getProgress().addProgressIndicatorListener(new MyProgressListener(), ui);

    setDefaultRenderer(VirtualFile.class, new RootCellRenderer(myUi));
    setDefaultRenderer(GraphCommitCell.class, myGraphCommitCellRenderer);
    setDefaultRenderer(String.class, myStringCellRenderer);

    setShowHorizontalLines(false);
    setIntercellSpacing(JBUI.emptySize());
    setTableHeader(new InvisibleResizableHeader());

    myController = new GraphTableController(this, ui, logData, graphCellPainter, myGraphCommitCellRenderer);

    getSelectionModel().addListSelectionListener(new MyListSelectionListener());
    getColumnModel().setColumnSelectionAllowed(false);

    PopupHandler.installPopupHandler(this, VcsLogActionPlaces.POPUP_ACTION_GROUP, VcsLogActionPlaces.VCS_LOG_TABLE_PLACE);
    ScrollingUtil.installActions(this, false);
    new IndexSpeedSearch(myLogData.getProject(), myLogData.getIndex(), this) {
      @Override
      protected boolean isSpeedSearchEnabled() {
        return VcsLogGraphTable.this.isSpeedSearchEnabled() && super.isSpeedSearchEnabled();
      }
    };

    initColumnSize();
    addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        updateCommitColumnWidth();
      }
    });
  }

  protected boolean isSpeedSearchEnabled() {
    return Registry.is("vcs.log.speedsearch");
  }

  public void updateDataPack(@Nonnull VisiblePack visiblePack, boolean permGraphChanged) {
    VcsLogGraphTable.Selection previousSelection = getSelection();
    getModel().setVisiblePack(visiblePack);
    previousSelection.restore(visiblePack.getVisibleGraph(), true, permGraphChanged);
    for (VcsLogHighlighter highlighter : myHighlighters) {
      highlighter.update(visiblePack, permGraphChanged);
    }

    setPaintBusy(false);
    initColumnSize();
  }

  void initColumnSize() {
    if (!myColumnsSizeInitialized && getModel().getRowCount() > 0) {
      myColumnsSizeInitialized = setColumnPreferredSize();
      if (myColumnsSizeInitialized) {
        setAutoCreateColumnsFromModel(false); // otherwise sizes are recalculated after each TableColumn re-initialization
        for (int column = 0; column < getColumnCount(); column++) {
          getColumnModel().getColumn(column).setResizable(column != GraphTableModel.ROOT_COLUMN);
        }
      }
    }
  }

  private boolean setColumnPreferredSize() {
    boolean sizeCalculated = false;
    Font tableFont = UIManager.getFont("Table.font");
    for (int i = 0; i < getColumnCount(); i++) {
      TableColumn column = getColumnModel().getColumn(i);
      if (i == GraphTableModel.ROOT_COLUMN) { // thin stripe, or root name, or nothing
        setRootColumnSize(column);
      }
      else if (i == GraphTableModel.AUTHOR_COLUMN) { // detect author with the longest name
        int maxRowsToCheck = Math.min(MAX_ROWS_TO_CALC_WIDTH, getRowCount());
        int maxWidth = 0;
        int unloaded = 0;
        for (int row = 0; row < maxRowsToCheck; row++) {
          String value = getModel().getValueAt(row, i).toString();
          if (value.isEmpty()) {
            unloaded++;
            continue;
          }
          Font font = tableFont;
          VcsLogHighlighter.TextStyle style = getStyle(row, i, false, false).getTextStyle();
          if (BOLD.equals(style)) {
            font = tableFont.deriveFont(Font.BOLD);
          }
          else if (ITALIC.equals(style)) {
            font = tableFont.deriveFont(Font.ITALIC);
          }
          maxWidth = Math.max(getFontMetrics(font).stringWidth(value + "*"), maxWidth);
        }
        int min = Math.min(maxWidth + myStringCellRenderer.getHorizontalTextPadding(), JBUI.scale(MAX_DEFAULT_AUTHOR_COLUMN_WIDTH));
        column.setPreferredWidth(min);
        if (unloaded * 2 <= maxRowsToCheck) sizeCalculated = true;
      }
      else if (i == GraphTableModel.DATE_COLUMN) { // all dates have nearly equal sizes
        int min = getFontMetrics(tableFont.deriveFont(Font.BOLD)).stringWidth(DateFormatUtil.formatDateTime(new Date())) +
                  myStringCellRenderer.getHorizontalTextPadding();
        column.setPreferredWidth(min);
      }
    }

    updateCommitColumnWidth();

    return sizeCalculated;
  }

  private void updateCommitColumnWidth() {
    int size = getWidth();
    for (int i = 0; i < getColumnCount(); i++) {
      if (i == GraphTableModel.COMMIT_COLUMN) continue;
      TableColumn column = getColumnModel().getColumn(i);
      size -= column.getPreferredWidth();
    }

    TableColumn commitColumn = getColumnModel().getColumn(GraphTableModel.COMMIT_COLUMN);
    commitColumn.setPreferredWidth(size);
  }

  private void setRootColumnSize(@Nonnull TableColumn column) {
    int rootWidth;
    if (!myUi.isMultipleRoots()) {
      rootWidth = 0;
    }
    else if (!myUi.isShowRootNames()) {
      rootWidth = JBUI.scale(ROOT_INDICATOR_WIDTH);
    }
    else {
      rootWidth = Math.min(calculateMaxRootWidth(), JBUI.scale(ROOT_NAME_MAX_WIDTH));
    }

    // NB: all further instructions and their order are important, otherwise the minimum size which is less than 15 won't be applied
    column.setMinWidth(rootWidth);
    column.setMaxWidth(rootWidth);
    column.setPreferredWidth(rootWidth);
  }

  private int calculateMaxRootWidth() {
    int width = 0;
    for (VirtualFile file : myLogData.getRoots()) {
      Font tableFont = UIManager.getFont("Table.font");
      width = Math.max(getFontMetrics(tableFont).stringWidth(file.getName() + "  "), width);
    }
    return width;
  }

  @Override
  public String getToolTipText(@Nonnull MouseEvent event) {
    int row = rowAtPoint(event.getPoint());
    int column = columnAtPoint(event.getPoint());
    if (column < 0 || row < 0) {
      return null;
    }
    if (column == GraphTableModel.ROOT_COLUMN) {
      Object at = getValueAt(row, column);
      if (at instanceof VirtualFile) {
        return "<html><b>" +
               ((VirtualFile)at).getPresentableUrl() +
               "</b><br/>Click to " +
               (myUi.isShowRootNames() ? "collapse" : "expand") +
               "</html>";
      }
    }
    return null;
  }

  public void jumpToRow(int rowIndex) {
    if (rowIndex >= 0 && rowIndex <= getRowCount() - 1) {
      scrollRectToVisible(getCellRect(rowIndex, 0, false));
      setRowSelectionInterval(rowIndex, rowIndex);
      scrollRectToVisible(getCellRect(rowIndex, 0, false));
    }
  }

  @Nullable
  @Override
  public Object getData(@Nonnull @NonNls Key dataId) {
    return KEY == dataId ? this : null;
  }

  @Override
  public void performCopy(@Nonnull DataContext dataContext) {
    StringBuilder sb = new StringBuilder();

    int[] selectedRows = getSelectedRows();
    for (int i = 0; i < Math.min(VcsLogUtil.MAX_SELECTED_COMMITS, selectedRows.length); i++) {
      int row = selectedRows[i];
      sb.append(getModel().getValueAt(row, GraphTableModel.COMMIT_COLUMN).toString());
      sb.append(" ").append(getModel().getValueAt(row, GraphTableModel.AUTHOR_COLUMN).toString());
      sb.append(" ").append(getModel().getValueAt(row, GraphTableModel.DATE_COLUMN).toString());
      if (i != selectedRows.length - 1) sb.append("\n");
    }

    CopyPasteManager.getInstance().setContents(new StringSelection(sb.toString()));
  }

  @Override
  public boolean isCopyEnabled(@Nonnull DataContext dataContext) {
    return getSelectedRowCount() > 0;
  }

  @Override
  public boolean isCopyVisible(@Nonnull DataContext dataContext) {
    return true;
  }

  public void addHighlighter(@Nonnull VcsLogHighlighter highlighter) {
    myHighlighters.add(highlighter);
  }

  public void removeHighlighter(@Nonnull VcsLogHighlighter highlighter) {
    myHighlighters.remove(highlighter);
  }

  public void removeAllHighlighters() {
    myHighlighters.clear();
  }

  @Nonnull
  public SimpleTextAttributes applyHighlighters(
    @Nonnull Component rendererComponent,
    int row,
    int column,
    boolean hasFocus,
    final boolean selected
  ) {
    VcsLogHighlighter.VcsCommitStyle style = getStyle(row, column, hasFocus, selected);

    assert style.getBackground() != null && style.getForeground() != null && style.getTextStyle() != null;

    rendererComponent.setBackground(style.getBackground());
    rendererComponent.setForeground(style.getForeground());

    switch (style.getTextStyle()) {
      case BOLD:
        return SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES;
      case ITALIC:
        return SimpleTextAttributes.REGULAR_ITALIC_ATTRIBUTES;
      default:
    }
    return SimpleTextAttributes.REGULAR_ATTRIBUTES;
  }

  public VcsLogHighlighter.VcsCommitStyle getBaseStyle(int row, int column, boolean hasFocus, boolean selected) {
    Component dummyRendererComponent = myDummyRenderer.getTableCellRendererComponent(this, "", selected, hasFocus, row, column);
    return VcsCommitStyleFactory
      .createStyle(dummyRendererComponent.getForeground(), dummyRendererComponent.getBackground(), VcsLogHighlighter.TextStyle.NORMAL);
  }

  private VcsLogHighlighter.VcsCommitStyle getStyle(int row, int column, boolean hasFocus, boolean selected) {
    VcsLogHighlighter.VcsCommitStyle baseStyle = getBaseStyle(row, column, hasFocus, selected);

    VisibleGraph<Integer> visibleGraph = getVisibleGraph();
    if (row < 0 || row >= visibleGraph.getVisibleCommitCount()) {
      LOG.error("Visible graph has " + visibleGraph.getVisibleCommitCount() + " commits, yet we want row " + row);
      return baseStyle;
    }

    RowInfo<Integer> rowInfo = visibleGraph.getRowInfo(row);

    VcsLogHighlighter.VcsCommitStyle defaultStyle = VcsCommitStyleFactory
            .createStyle(rowInfo.getRowType() == RowType.UNMATCHED ? JBColor.GRAY : baseStyle.getForeground(), baseStyle.getBackground(),
                         VcsLogHighlighter.TextStyle.NORMAL);

    final VcsShortCommitDetails details = myLogData.getMiniDetailsGetter().getCommitDataIfAvailable(rowInfo.getCommit());
    if (details == null) return defaultStyle;

    List<VcsLogHighlighter.VcsCommitStyle> styles =
      ContainerUtil.map(myHighlighters, highlighter -> highlighter.getStyle(details, selected));
    return VcsCommitStyleFactory.combine(ContainerUtil.append(styles, defaultStyle));
  }

  public void viewportSet(JViewport viewport) {
    viewport.addChangeListener(e -> {
      AbstractTableModel model = getModel();
      Couple<Integer> visibleRows = ScrollingUtil.getVisibleRows(this);
      model.fireTableChanged(new TableModelEvent(model, visibleRows.first - 1, visibleRows.second, GraphTableModel.ROOT_COLUMN));
    });
  }

  public void rootColumnUpdated() {
    setRootColumnSize(getColumnModel().getColumn(GraphTableModel.ROOT_COLUMN));
    updateCommitColumnWidth();
  }

  public static JBColor getRootBackgroundColor(@Nonnull VirtualFile root, @Nonnull VcsLogColorManager colorManager) {
    return VcsLogColorManagerImpl.getBackgroundColor(colorManager.getRootColor(root));
  }

  @Override
  public void setCursor(Cursor cursor) {
    super.setCursor(cursor);
    Component layeredPane = UIUtil.findParentByCondition(this, component -> component instanceof LoadingDecorator.CursorAware);
    if (layeredPane != null) {
      layeredPane.setCursor(cursor);
    }
  }

  @Override
  @Nonnull
  public GraphTableModel getModel() {
    return (GraphTableModel)super.getModel();
  }

  @Nonnull
  public Selection getSelection() {
    if (mySelection == null) mySelection = new Selection(this);
    return mySelection;
  }

  public void handleAnswer(@Nullable GraphAnswer<Integer> answer, boolean dataCouldChange) {
    myController.handleGraphAnswer(answer, dataCouldChange, null, null);
  }

  public void showTooltip(int row) {
    myController.showTooltip(row);
  }

  public void setCompactReferencesView(boolean compact) {
    myGraphCommitCellRenderer.setCompactReferencesView(compact);
    repaint();
  }

  public void setShowTagNames(boolean showTagsNames) {
    myGraphCommitCellRenderer.setShowTagsNames(showTagsNames);
    repaint();
  }

  static class Selection {
    @Nonnull
    private final VcsLogGraphTable myTable;
    @Nonnull
    private final TIntHashSet mySelectedCommits;
    @Nullable private final Integer myVisibleSelectedCommit;
    @Nullable private final Integer myDelta;
    private final boolean myIsOnTop;


    public Selection(@Nonnull VcsLogGraphTable table) {
      myTable = table;
      List<Integer> selectedRows = ContainerUtil.sorted(Ints.asList(myTable.getSelectedRows()));
      Couple<Integer> visibleRows = ScrollingUtil.getVisibleRows(myTable);
      myIsOnTop = visibleRows.first - 1 == 0;

      VisibleGraph<Integer> graph = myTable.getVisibleGraph();

      mySelectedCommits = new TIntHashSet();

      Integer visibleSelectedCommit = null;
      Integer delta = null;
      for (int row : selectedRows) {
        if (row < graph.getVisibleCommitCount()) {
          Integer commit = graph.getRowInfo(row).getCommit();
          mySelectedCommits.add(commit);
          if (visibleRows.first - 1 <= row && row <= visibleRows.second && visibleSelectedCommit == null) {
            visibleSelectedCommit = commit;
            delta = myTable.getCellRect(row, 0, false).y - myTable.getVisibleRect().y;
          }
        }
      }
      if (visibleSelectedCommit == null && visibleRows.first - 1 >= 0) {
        visibleSelectedCommit = graph.getRowInfo(visibleRows.first - 1).getCommit();
        delta = myTable.getCellRect(visibleRows.first - 1, 0, false).y - myTable.getVisibleRect().y;
      }

      myVisibleSelectedCommit = visibleSelectedCommit;
      myDelta = delta;
    }

    public void restore(@Nonnull VisibleGraph<Integer> newVisibleGraph, boolean scrollToSelection, boolean permGraphChanged) {
      Pair<TIntHashSet, Integer> toSelectAndScroll = findRowsToSelectAndScroll(myTable.getModel(), newVisibleGraph);
      if (!toSelectAndScroll.first.isEmpty()) {
        myTable.getSelectionModel().setValueIsAdjusting(true);
        toSelectAndScroll.first.forEach(row -> {
          myTable.addRowSelectionInterval(row, row);
          return true;
        });
        myTable.getSelectionModel().setValueIsAdjusting(false);
      }
      if (scrollToSelection) {
        if (myIsOnTop && permGraphChanged) { // scroll on top when some fresh commits arrive
          scrollToRow(0, 0);
        }
        else if (toSelectAndScroll.second != null) {
          assert myDelta != null;
          scrollToRow(toSelectAndScroll.second, myDelta);
        }
      }
      // sometimes commits that were selected are now collapsed
      // currently in this case selection disappears
      // in the future we need to create a method in LinearGraphController that allows to calculate visible commit for our commit
      // or answer from collapse action could return a map that gives us some information about what commits were collapsed and where
    }

    private void scrollToRow(Integer row, Integer delta) {
      Rectangle startRect = myTable.getCellRect(row, 0, true);
      myTable.scrollRectToVisible(
              new Rectangle(startRect.x, Math.max(startRect.y - delta, 0), startRect.width, myTable.getVisibleRect().height));
    }

    @Nonnull
    private Pair<TIntHashSet, Integer> findRowsToSelectAndScroll(@Nonnull GraphTableModel model,
                                                                 @Nonnull VisibleGraph<Integer> visibleGraph) {
      TIntHashSet rowsToSelect = new TIntHashSet();

      if (model.getRowCount() == 0) {
        // this should have been covered by facade.getVisibleCommitCount,
        // but if the table is empty (no commits match the filter), the GraphFacade is not updated, because it can't handle it
        // => it has previous values set.
        return Pair.create(rowsToSelect, null);
      }

      Integer rowToScroll = null;
      for (int row = 0;
           row < visibleGraph.getVisibleCommitCount() && (rowsToSelect.size() < mySelectedCommits.size() || rowToScroll == null);
           row++) { //stop iterating if found all hashes
        int commit = visibleGraph.getRowInfo(row).getCommit();
        if (mySelectedCommits.contains(commit)) {
          rowsToSelect.add(row);
        }
        if (myVisibleSelectedCommit != null && myVisibleSelectedCommit == commit) {
          rowToScroll = row;
        }
      }
      return Pair.create(rowsToSelect, rowToScroll);
    }
  }

  @Nonnull
  public VisibleGraph<Integer> getVisibleGraph() {
    return getModel().getVisiblePack().getVisibleGraph();
  }

  @Override
  public TableCellEditor getCellEditor() {
    // this fixes selection problems by prohibiting selection when user clicks on graph (CellEditor does that)
    // what is fun about this code is that if you set cell editor in constructor with setCellEditor method it would not work
    return myDummyEditor;
  }

  @Override
  public int getRowHeight() {
    return myGraphCommitCellRenderer.getPreferredHeight();
  }

  @Override
  protected void paintFooter(@Nonnull Graphics g, int x, int y, int width, int height) {
    int lastRow = getRowCount() - 1;
    if (lastRow >= 0) {
      g.setColor(getStyle(lastRow, GraphTableModel.COMMIT_COLUMN, hasFocus(), false).getBackground());
      g.fillRect(x, y, width, height);
      if (myUi.isMultipleRoots()) {
        g.setColor(getRootBackgroundColor(getModel().getRoot(lastRow), myUi.getColorManager()));

        int rootWidth = getColumnModel().getColumn(GraphTableModel.ROOT_COLUMN).getWidth();
        if (!myUi.isShowRootNames()) rootWidth -= JBUI.scale(ROOT_INDICATOR_WHITE_WIDTH);

        g.fillRect(x, y, rootWidth, height);
      }
    }
    else {
      g.setColor(getBaseStyle(lastRow, GraphTableModel.COMMIT_COLUMN, hasFocus(), false).getBackground());
      g.fillRect(x, y, width, height);
    }
  }

  boolean isResizingColumns() {
    return getCursor() == Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
  }

  private static class RootCellRenderer extends JBLabel implements TableCellRenderer {
    @Nonnull
    private final VcsLogUiImpl myUi;
    @Nonnull
    private Color myColor = UIUtil.getTableBackground();
    @Nonnull
    private Color myBorderColor = UIUtil.getTableBackground();
    private boolean isNarrow = true;

    RootCellRenderer(@Nonnull VcsLogUiImpl ui) {
      super("", CENTER);
      myUi = ui;
    }

    @Override
    protected void paintComponent(Graphics g) {
      setFont(UIManager.getFont("Table.font"));
      g.setColor(myColor);

      int width = getWidth();

      if (isNarrow) {
        g.fillRect(0, 0, width - JBUI.scale(ROOT_INDICATOR_WHITE_WIDTH), myUi.getTable().getRowHeight());
        g.setColor(myBorderColor);
        g.fillRect(width - JBUI.scale(ROOT_INDICATOR_WHITE_WIDTH), 0, JBUI.scale(ROOT_INDICATOR_WHITE_WIDTH),
                   myUi.getTable().getRowHeight());
      }
      else {
        g.fillRect(0, 0, width, myUi.getTable().getRowHeight());
      }

      super.paintComponent(g);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      String text;
      Color color;

      if (value instanceof VirtualFile) {
        VirtualFile root = (VirtualFile)value;
        int readableRow = ScrollingUtil.getReadableRow(table, Math.round(myUi.getTable().getRowHeight() * 0.5f));
        if (row < readableRow) {
          text = "";
        }
        else if (row == 0 || !value.equals(table.getModel().getValueAt(row - 1, column)) || readableRow == row) {
          text = root.getName();
        }
        else {
          text = "";
        }
        color = getRootBackgroundColor(root, myUi.getColorManager());
      }
      else {
        text = null;
        color = UIUtil.getTableBackground(isSelected);
      }

      myColor = color;
      Color background = ((VcsLogGraphTable)table).getStyle(row, column, hasFocus, isSelected).getBackground();
      assert background != null;
      myBorderColor = background;
      setForeground(UIUtil.getTableForeground(false));

      if (myUi.isShowRootNames()) {
        setText(text);
        isNarrow = false;
      }
      else {
        setText("");
        isNarrow = true;
      }

      return this;
    }

    @Override
    public void setBackground(Color bg) {
      myBorderColor = bg;
    }
  }

  private class StringCellRenderer extends ColoredTableCellRenderer {
    @Override
    protected void customizeCellRenderer(JTable table, Object value, boolean selected, boolean hasFocus, int row, int column) {
      setBorder(null);
      if (value == null) {
        return;
      }
      append(value.toString(), applyHighlighters(this, row, column, hasFocus, selected));
      SpeedSearchUtil.applySpeedSearchHighlighting(table, this, false, selected);
    }

    public int getHorizontalTextPadding() {
      Insets borderInsets = getMyBorder().getBorderInsets(this);
      Insets ipad = getIpad();
      return borderInsets.left + borderInsets.right + ipad.left + ipad.right;
    }
  }

  private class MyDummyTableCellEditor implements TableCellEditor {
    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
      return null;
    }

    @Override
    public Object getCellEditorValue() {
      return null;
    }

    @Override
    public boolean isCellEditable(EventObject anEvent) {
      return false;
    }

    @Override
    public boolean shouldSelectCell(EventObject anEvent) {
      return !(anEvent instanceof MouseEvent mouseEvent && myController.findPrintElement(mouseEvent) != null);
    }

    @Override
    public boolean stopCellEditing() {
      return false;
    }

    @Override
    public void cancelCellEditing() {

    }

    @Override
    public void addCellEditorListener(CellEditorListener l) {

    }

    @Override
    public void removeCellEditorListener(CellEditorListener l) {

    }
  }

  private class InvisibleResizableHeader extends JBTable.JBTableHeader {
    @Nonnull
    private final MyBasicTableHeaderUI myHeaderUI;
    @Nullable private Cursor myCursor = null;

    public InvisibleResizableHeader() {
      myHeaderUI = new MyBasicTableHeaderUI(this);
      // need a header to resize columns, so use header that is not visible
      setDefaultRenderer(new EmptyTableCellRenderer());
      setReorderingAllowed(false);
    }

    @Override
    public void setTable(JTable table) {
      JTable oldTable = getTable();
      if (oldTable != null) {
        oldTable.removeMouseListener(myHeaderUI);
        oldTable.removeMouseMotionListener(myHeaderUI);
      }

      super.setTable(table);

      if (table != null) {
        table.addMouseListener(myHeaderUI);
        table.addMouseMotionListener(myHeaderUI);
      }
    }

    @Override
    public void setCursor(@Nullable Cursor cursor) {
      /* this method and the next one fixes cursor:
         BasicTableHeaderUI.MouseInputHandler behaves like nobody else sets cursor
         so we remember what it set last time and keep it unaffected by other cursor changes in the table
       */
      JTable table = getTable();
      if (table != null) {
        table.setCursor(cursor);
        myCursor = cursor;
      }
      else {
        super.setCursor(cursor);
      }
    }

    @Override
    public Cursor getCursor() {
      if (myCursor == null) {
        JTable table = getTable();
        if (table == null) return super.getCursor();
        return table.getCursor();
      }
      return myCursor;
    }

    @Nonnull
    @Override
    public Rectangle getHeaderRect(int column) {
      // if a header has zero height, mouse pointer can never be inside it, so we pretend it is one pixel high
      Rectangle headerRect = super.getHeaderRect(column);
      return new Rectangle(headerRect.x, headerRect.y, headerRect.width, 1);
    }
  }

  private static class EmptyTableCellRenderer implements TableCellRenderer {
    @Nonnull
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      JPanel panel = new JPanel(new BorderLayout());
      panel.setMaximumSize(new Dimension(0, 0));
      return panel;
    }
  }

  // this class redirects events from the table to BasicTableHeaderUI.MouseInputHandler
  private static class MyBasicTableHeaderUI extends BasicTableHeaderUI implements MouseInputListener {
    public MyBasicTableHeaderUI(@Nonnull JTableHeader tableHeader) {
      header = tableHeader;
      mouseInputListener = createMouseInputListener();
    }

    @Nonnull
    private MouseEvent convertMouseEvent(@Nonnull MouseEvent e) {
      // create a new event, almost exactly the same, but in the header
      return new MouseEvent(
        e.getComponent(),
        e.getID(),
        e.getWhen(),
        e.getModifiers(),
        e.getX(),
        0,
        e.getXOnScreen(),
        header.getY(),
        e.getClickCount(),
        e.isPopupTrigger(),
        e.getButton()
      );
    }

    @Override
    public void mouseClicked(@Nonnull MouseEvent e) {
    }

    @Override
    public void mousePressed(@Nonnull MouseEvent e) {
      if (isOnBorder(e)) return;
      mouseInputListener.mousePressed(convertMouseEvent(e));
    }

    @Override
    public void mouseReleased(@Nonnull MouseEvent e) {
      if (isOnBorder(e)) return;
      mouseInputListener.mouseReleased(convertMouseEvent(e));
    }

    @Override
    public void mouseEntered(@Nonnull MouseEvent e) {
    }

    @Override
    public void mouseExited(@Nonnull MouseEvent e) {
    }

    @Override
    public void mouseDragged(@Nonnull MouseEvent e) {
      if (isOnBorder(e)) return;
      mouseInputListener.mouseDragged(convertMouseEvent(e));
    }

    @Override
    public void mouseMoved(@Nonnull MouseEvent e) {
      if (isOnBorder(e)) return;
      mouseInputListener.mouseMoved(convertMouseEvent(e));
    }

    public boolean isOnBorder(@Nonnull MouseEvent e) {
      return Math.abs(header.getTable().getWidth() - e.getPoint().x) <= JBUI.scale(3);
    }
  }

  private class MyListSelectionListener implements ListSelectionListener {
    @Override
    public void valueChanged(ListSelectionEvent e) {
      mySelection = null;
    }
  }

  private class MyProgressListener implements VcsLogProgress.ProgressListener {
    @Nonnull
    private String myText = "";

    @Override
    public void progressStarted() {
      myText = getEmptyText().getText();
      getEmptyText().setText("Loading History...");
    }

    @Override
    public void progressStopped() {
      getEmptyText().setText(myText);
    }
  }
}
