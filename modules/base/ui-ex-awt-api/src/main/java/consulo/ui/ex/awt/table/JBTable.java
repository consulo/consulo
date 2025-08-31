/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ui.ex.awt.table;

import consulo.application.ApplicationManager;
import consulo.application.ui.wm.ExpirableRunnable;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.disposer.Disposer;
import consulo.ui.ex.ComponentWithExpandableItems;
import consulo.ui.ex.ExpandableItemsHandler;
import consulo.ui.ex.TableCell;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.speedSearch.SpeedSearchSupply;
import consulo.ui.ex.awt.update.UiNotifyConnector;
import consulo.ui.ex.awt.util.GraphicsUtil;
import consulo.ui.ex.awt.util.JBSwingUtilities;
import consulo.ui.ex.awt.util.ScreenUtil;
import consulo.ui.ex.update.Activatable;
import jakarta.annotation.Nonnull;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EventObject;

public class JBTable extends JTable implements ComponentWithEmptyText, ComponentWithExpandableItems<TableCell> {
  public static final int PREFERRED_SCROLLABLE_VIEWPORT_HEIGHT_IN_ROWS = 7;
  public static final int COLUMN_RESIZE_AREA_WIDTH = 3; // same as in BasicTableHeaderUI
  private static final int DEFAULT_MIN_COLUMN_WIDTH = 15; // see TableColumn constructor javadoc

  private final StatusText myEmptyText;
  private final ExpandableItemsHandler<TableCell> myExpandableItemsHandler;

  private boolean myEnableAntialiasing;

  private int myRowHeight = -1;
  private boolean myRowHeightIsExplicitlySet;
  private boolean myRowHeightIsComputing;
  private boolean myUiUpdating = true;

  private Integer myMinRowHeight;

  protected AsyncProcessIcon myBusyIcon;
  private boolean myBusy;

  private int myMaxItemsForSizeCalculation = Integer.MAX_VALUE;

  public JBTable() {
    this(new DefaultTableModel());
  }

  public JBTable(TableModel model) {
    this(model, null);
  }

  public JBTable(TableModel model, TableColumnModel columnModel) {
    super(model, columnModel);

    setSurrendersFocusOnKeystroke(true);

    myEmptyText = new StatusText(this) {
      @Override
      protected boolean isStatusVisible() {
        return isEmpty();
      }
    };

    myExpandableItemsHandler = ExpandableItemsHandlerFactory.install(this);

    setFillsViewportHeight(true);

    addMouseListener(new MyMouseListener());

    final TableModelListener modelListener = new TableModelListener() {
      @Override
      public void tableChanged(@Nonnull TableModelEvent e) {
        onTableChanged(e);
      }
    };

    if (getModel() != null) getModel().addTableModelListener(modelListener);
    addPropertyChangeListener("model", new PropertyChangeListener() {
      @Override
      public void propertyChange(@Nonnull PropertyChangeEvent evt) {
        repaintViewport();

        if (evt.getOldValue() instanceof TableModel) {
          ((TableModel)evt.getOldValue()).removeTableModelListener(modelListener);
        }
        if (evt.getNewValue() instanceof TableModel) {
          ((TableModel)evt.getNewValue()).addTableModelListener(modelListener);
        }
      }
    });

    myUiUpdating = false;

    new MyCellEditorRemover();
  }

  protected void onTableChanged(@Nonnull TableModelEvent e) {
    if (!myRowHeightIsExplicitlySet) {
      myRowHeight = -1;
    }
    if (e.getType() == TableModelEvent.DELETE && isEmpty() || e.getType() == TableModelEvent.INSERT && !isEmpty()) {
      repaintViewport();
    }
  }

  @Override
  public int getRowHeight() {
    if (myRowHeightIsComputing) {
      return super.getRowHeight();
    }

    if (myRowHeight < 0) {
      try {
        myRowHeightIsComputing = true;
        myRowHeight = calculateRowHeight();
      }
      finally {
        myRowHeightIsComputing = false;
      }
    }

    if (myMinRowHeight == null) {
      myMinRowHeight = getFontMetrics(UIManager.getFont("Label.font")).getHeight();
    }

    return Math.max(myRowHeight, myMinRowHeight);
  }

  protected int calculateRowHeight() {
    int result = -1;

    for (int row = 0; row < Math.min(getRowCount(), myMaxItemsForSizeCalculation); row++) {
      for (int column = 0; column < Math.min(getColumnCount(), myMaxItemsForSizeCalculation); column++) {
        TableCellRenderer renderer = getCellRenderer(row, column);
        if (renderer != null) {
          Object value = getValueAt(row, column);
          Component component = renderer.getTableCellRendererComponent(this, value, true, true, row, column);
          if (component != null) {
            Dimension size = component.getPreferredSize();
            result = Math.max(size.height, result);
          }
        }
      }
    }

    return result;
  }

  public void setShowColumns(boolean value) {
    JTableHeader tableHeader = getTableHeader();
    tableHeader.setVisible(value);
    tableHeader.setPreferredSize(value ? null : new Dimension());
  }

  @Override
  public void setRowHeight(int rowHeight) {
    if (!myUiUpdating || !UIUtil.isUnderGTKLookAndFeel()) {
      myRowHeight = rowHeight;
      myRowHeightIsExplicitlySet = true;
    }
    // call super to clean rowModel
    super.setRowHeight(rowHeight);
  }

  @Override
  public void updateUI() {
    myUiUpdating = true;
    try {
      super.updateUI();
      myMinRowHeight = null;
    }
    finally {
      myUiUpdating = false;
    }
  }

  private void repaintViewport() {
    if (!isDisplayable() || !isVisible()) return;

    Container p = getParent();
    if (p instanceof JBViewport) {
      p.repaint();
    }
  }

  @Nonnull
  @Override
  protected JTableHeader createDefaultTableHeader() {
    return new JBTableHeader();
  }

  public boolean isEmpty() {
    return getRowCount() == 0;
  }

  @Override
  public void setModel(@Nonnull TableModel model) {
    super.setModel(model);

    if (model instanceof SortableColumnModel) {
      SortableColumnModel sortableModel = (SortableColumnModel)model;
      if (sortableModel.isSortable()) {
        TableRowSorter<TableModel> rowSorter = createRowSorter(model);
        rowSorter.setSortsOnUpdates(isSortOnUpdates());
        setRowSorter(rowSorter);
        RowSorter.SortKey sortKey = sortableModel.getDefaultSortKey();
        if (sortKey != null && sortKey.getColumn() >= 0 && sortKey.getColumn() < model.getColumnCount()) {
          if (sortableModel.getColumnInfos()[sortKey.getColumn()].isSortable()) {
            rowSorter.setSortKeys(Collections.singletonList(sortKey));
          }
        }
      }
      else {
        RowSorter<? extends TableModel> rowSorter = getRowSorter();
        if (rowSorter instanceof DefaultColumnInfoBasedRowSorter) {
          setRowSorter(null);
        }
      }
    }
  }

  protected boolean isSortOnUpdates() {
    return true;
  }

  @Override
  protected void paintComponent(@Nonnull Graphics g) {
    if (myEnableAntialiasing) {
      GraphicsUtil.setupAntialiasing(g);
    }
    super.paintComponent(g);
    myEmptyText.paint(this, g);
  }

  @Override
  protected void paintChildren(Graphics g) {
    if (myEnableAntialiasing) {
      GraphicsUtil.setupAntialiasing(g);
    }
    super.paintChildren(g);
  }

  public void setEnableAntialiasing(boolean flag) {
    myEnableAntialiasing = flag;
  }

  public static DefaultCellEditor createBooleanEditor() {
    return new DefaultCellEditor(new JCheckBox()) {
      {
        ((JCheckBox)getComponent()).setHorizontalAlignment(SwingConstants.CENTER);
      }

      @Override
      public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        Component component = super.getTableCellEditorComponent(table, value, isSelected, row, column);
        component.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
        return component;
      }
    };
  }

  public void resetDefaultFocusTraversalKeys() {
    KeyboardFocusManager m = KeyboardFocusManager.getCurrentKeyboardFocusManager();
    for (Integer each : Arrays.asList(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
                                      KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
                                      KeyboardFocusManager.UP_CYCLE_TRAVERSAL_KEYS,
                                      KeyboardFocusManager.DOWN_CYCLE_TRAVERSAL_KEYS)) {
      setFocusTraversalKeys(each, m.getDefaultFocusTraversalKeys(each));
    }
  }

  @Nonnull
  @Override
  public StatusText getEmptyText() {
    return myEmptyText;
  }

  @Override
  @Nonnull
  public ExpandableItemsHandler<TableCell> getExpandableItemsHandler() {
    return myExpandableItemsHandler;
  }

  @Override
  public void setExpandableItemsEnabled(boolean enabled) {
    myExpandableItemsHandler.setEnabled(enabled);
  }

  @Override
  public void removeNotify() {
    super.removeNotify();
    if (ScreenUtil.isStandardAddRemoveNotify(this)) {
      if (myBusyIcon != null) {
        remove(myBusyIcon);
        Disposer.dispose(myBusyIcon);
        myBusyIcon = null;
      }
    }
  }

  @Override
  public int getScrollableUnitIncrement(@Nonnull Rectangle visibleRect, int orientation, int direction) {
    if (orientation == SwingConstants.VERTICAL) {
      return super.getScrollableUnitIncrement(visibleRect, orientation, direction);
    }
    else { // if orientation == SwingConstants.HORIZONTAL
      // use smooth editor-like scrolling
      return SwingUtilities.computeStringWidth(getFontMetrics(getFont()), " ");
    }
  }

  @Override
  public void doLayout() {
    super.doLayout();
    if (myBusyIcon != null) {
      myBusyIcon.updateLocation(this);
    }
  }

  @Override
  protected Graphics getComponentGraphics(Graphics graphics) {
    return JBSwingUtilities.runGlobalCGTransform(this, super.getComponentGraphics(graphics));
  }

  @Override
  public void paint(@Nonnull Graphics g) {
    if (!isEnabled()) {
      g = new Grayer((Graphics2D)g, getBackground());
    }
    super.paint(g);
    if (myBusyIcon != null) {
      myBusyIcon.updateLocation(this);
    }
  }

  public void setPaintBusy(boolean paintBusy) {
    if (myBusy == paintBusy) return;

    myBusy = paintBusy;
    updateBusy();
  }

  private void updateBusy() {
    if (myBusy) {
      if (myBusyIcon == null) {
        myBusyIcon = createBusyIcon();
        myBusyIcon.setOpaque(false);
        myBusyIcon.setPaintPassiveIcon(false);
        add(myBusyIcon);
      }
    }

    if (myBusyIcon != null) {
      if (myBusy) {
        myBusyIcon.resume();
      }
      else {
        myBusyIcon.suspend();
        //noinspection SSBasedInspection
        SwingUtilities.invokeLater(() -> {
          if (myBusyIcon != null) {
            repaint();
          }
        });
      }
      if (myBusyIcon != null) {
        myBusyIcon.updateLocation(this);
      }
    }
  }

  @Nonnull
  protected AsyncProcessIcon createBusyIcon() {
    return new AsyncProcessIcon(toString());
  }

  public boolean isStriped() {
    return false;
  }

  @Deprecated
  public void setStriped(boolean striped) {
  }

  @Override
  public boolean editCellAt(int row, int column, EventObject e) {
    if (cellEditor != null && !cellEditor.stopCellEditing()) {
      return false;
    }

    if (row < 0 || row >= getRowCount() || column < 0 || column >= getColumnCount()) {
      return false;
    }

    if (!isCellEditable(row, column)) {
      return false;
    }

    if (e instanceof KeyEvent) {
      // do not start editing in autoStartsEdit mode on Ctrl-Z and other non-typed events
      if (!UIUtil.isReallyTypedEvent((KeyEvent)e) || ((KeyEvent)e).getKeyChar() == KeyEvent.CHAR_UNDEFINED) return false;

      SpeedSearchSupply supply = SpeedSearchSupply.getSupply(this);
      if (supply != null && supply.isPopupActive()) {
        return false;
      }
    }

    TableCellEditor editor = getCellEditor(row, column);
    if (editor != null && editor.isCellEditable(e)) {
      editorComp = prepareEditor(editor, row, column);
      //((JComponent)editorComp).setBorder(null);
      if (editorComp == null) {
        removeEditor();
        return false;
      }
      editorComp.setBounds(getCellRect(row, column, false));
      add(editorComp);
      editorComp.validate();

      if (surrendersFocusOnKeyStroke() && !(editorComp instanceof AbstractButton)) {
        // this replaces focus request in JTable.processKeyBinding
        IdeFocusManager.findInstanceByComponent(this).requestFocus(editorComp, true);
      }

      setCellEditor(editor);
      setEditingRow(row);
      setEditingColumn(column);
      editor.addCellEditorListener(this);

      return true;
    }
    return false;
  }

  /**
   * Always returns false.
   * If you're interested in value of JTable.surrendersFocusOnKeystroke property, call JBTable.surrendersFocusOnKeyStroke()
   * @return false
   * @see #surrendersFocusOnKeyStroke
   */
  @Override
  public boolean getSurrendersFocusOnKeystroke() {
    return false; // prevents JTable.processKeyBinding from requesting editor component to be focused
  }

  public boolean surrendersFocusOnKeyStroke() {
    return super.getSurrendersFocusOnKeystroke();
  }

  @Nonnull
  @Override
  public Component prepareRenderer(@Nonnull TableCellRenderer renderer, int row, int column) {
    Component result = super.prepareRenderer(renderer, row, column);

    if (myExpandableItemsHandler.getExpandedItems().contains(new TableCell(row, column))) {
      result = ExpandedItemRendererComponentWrapper.wrap(result);
    }
    return result;
  }

  private final class MyCellEditorRemover implements Activatable, PropertyChangeListener {
    private boolean myIsActive = false;

    public MyCellEditorRemover() {
      addPropertyChangeListener("tableCellEditor", this);
      new UiNotifyConnector(JBTable.this, this);
    }

    public void activate() {
      if (!myIsActive) {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("permanentFocusOwner", this);
      }
      myIsActive = true;
    }

    public void deactivate() {
      if (myIsActive) {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removePropertyChangeListener("permanentFocusOwner", this);
      }
      myIsActive = false;
    }

    @Override
    public void hideNotify() {
      removeCellEditor();
    }

    @Override
    public void propertyChange(@Nonnull PropertyChangeEvent e) {
      if ("tableCellEditor".equals(e.getPropertyName())) {
        tableCellEditorChanged(e.getOldValue(), e.getNewValue());
      }
      else if ("permanentFocusOwner".equals(e.getPropertyName())) {
        permanentFocusOwnerChanged();
      }
    }

    private void tableCellEditorChanged(Object from, Object to) {
      boolean editingStarted = from == null && to != null;
      boolean editingStopped = from != null && to == null;

      if (editingStarted) {
        activate();
      }
      else if (editingStopped) {
        deactivate();
      }
    }

    private void permanentFocusOwnerChanged() {
      if (!isEditing()) {
        return;
      }

      final IdeFocusManager focusManager = IdeFocusManager.findInstanceByComponent(JBTable.this);
      focusManager.doWhenFocusSettlesDown(new ExpirableRunnable() {
        @Override
        public boolean isExpired() {
          return !isEditing();
        }

        @Override
        public void run() {
          Component c = focusManager.getFocusOwner();
          if (UIUtil.isMeaninglessFocusOwner(c)) {
            // this allows using popup menus and menu bar without stopping cell editing
            return;
          }
          while (c != null) {
            if (c instanceof JPopupMenu) {
              c = ((JPopupMenu)c).getInvoker();
            }
            if (c == JBTable.this) {
              // focus remains inside the table
              return;
            }
            else if (c instanceof Window) {
              if (c == SwingUtilities.getWindowAncestor(JBTable.this)) {
                removeCellEditor();
              }
              break;
            }
            c = c.getParent();
          }
        }
      });
    }

    private void removeCellEditor() {
      TableCellEditor cellEditor = getCellEditor();
      if (cellEditor != null && !cellEditor.stopCellEditing()) {
        cellEditor.cancelCellEditing();
      }
    }
  }

  private final class MyMouseListener extends MouseAdapter {
    @Override
    public void mousePressed(@Nonnull MouseEvent e) {
      if (JBSwingUtilities.isRightMouseButton(e)) {
        int[] selectedRows = getSelectedRows();
        if (selectedRows.length < 2) {
          int row = rowAtPoint(e.getPoint());
          if (row != -1) {
            getSelectionModel().setSelectionInterval(row, row);
          }
        }
      }
    }
  }

  @SuppressWarnings({"MethodMayBeStatic", "unchecked"})
  protected TableRowSorter<TableModel> createRowSorter(TableModel model) {
    return new DefaultColumnInfoBasedRowSorter(model);
  }

  protected static class DefaultColumnInfoBasedRowSorter extends TableRowSorter<TableModel> {
    public DefaultColumnInfoBasedRowSorter(TableModel model) {
      super(model);
      setModelWrapper(new TableRowSorterModelWrapper(model));
      setMaxSortKeys(1);
    }

    @Override
    public Comparator<?> getComparator(int column) {
      TableModel model = getModel();
      if (model instanceof SortableColumnModel) {
        ColumnInfo[] columnInfos = ((SortableColumnModel)model).getColumnInfos();
        if (column >= 0 && column < columnInfos.length) {
          Comparator comparator = columnInfos[column].getComparator();
          if (comparator != null) return comparator;
        }
      }

      return super.getComparator(column);
    }

    @Override
    protected boolean useToString(int column) {
      return false;
    }

    @Override
    public boolean isSortable(int column) {
      TableModel model = getModel();
      if (model instanceof SortableColumnModel) {
        ColumnInfo[] columnInfos = ((SortableColumnModel)model).getColumnInfos();
        if (column >= 0 && column < columnInfos.length) {
          return columnInfos[column].isSortable() && columnInfos[column].getComparator() != null;
        }
      }

      return false;
    }

    private class TableRowSorterModelWrapper extends ModelWrapper<TableModel, Integer> {
      private final TableModel myModel;

      private TableRowSorterModelWrapper(@Nonnull TableModel model) {
        myModel = model;
      }

      @Override
      public TableModel getModel() {
        return myModel;
      }

      @Override
      public int getColumnCount() {
        return myModel.getColumnCount();
      }

      @Override
      public int getRowCount() {
        return myModel.getRowCount();
      }

      @Override
      public Object getValueAt(int row, int column) {
        if (myModel instanceof SortableColumnModel) {
          return ((SortableColumnModel)myModel).getRowValue(row);
        }

        return myModel.getValueAt(row, column);
      }

      @Nonnull
      @Override
      public String getStringValueAt(int row, int column) {
        TableStringConverter converter = getStringConverter();
        if (converter != null) {
          // Use the converter
          String value = converter.toString(
                  myModel, row, column);
          if (value != null) {
            return value;
          }
          return "";
        }

        // No converter, use getValueAt followed by toString
        Object o = getValueAt(row, column);
        if (o == null) {
          return "";
        }
        String string = o.toString();
        if (string == null) {
          return "";
        }
        return string;
      }

      @Override
      public Integer getIdentifier(int index) {
        return index;
      }
    }
  }

  protected class JBTableHeader extends JTableHeader {
    public JBTableHeader() {
      super(JBTable.this.columnModel);
      JBTable.this.addPropertyChangeListener(new PropertyChangeListener() {
        @Override
        public void propertyChange(@Nonnull PropertyChangeEvent evt) {
          if ("enabled".equals(evt.getPropertyName())) {
            JBTableHeader.this.repaint();
          }
        }
      });
    }

    @Override
    public void paint(@Nonnull Graphics g) {
      if (myEnableAntialiasing) {
        GraphicsUtil.setupAntialiasing(g);
      }
      if (!JBTable.this.isEnabled()) {
        g = new Grayer((Graphics2D)g, getBackground());
      }
      super.paint(g);
    }

    @Override
    public String getToolTipText(@Nonnull MouseEvent event) {
      TableModel model = getModel();
      if (model instanceof SortableColumnModel) {
        int i = columnAtPoint(event.getPoint());
        int infoIndex = i >= 0 ? convertColumnIndexToModel(i) : -1;
        ColumnInfo[] columnInfos = ((SortableColumnModel)model).getColumnInfos();
        String tooltipText = infoIndex >= 0 && infoIndex < columnInfos.length ? columnInfos[infoIndex].getTooltipText() : null;
        if (tooltipText != null) {
          return tooltipText;
        }
      }
      return super.getToolTipText(event);
    }

    @Override
    protected void processMouseEvent(MouseEvent e) {
      if (e.getID() == MouseEvent.MOUSE_CLICKED && e.getButton() == MouseEvent.BUTTON1) {
        int columnToPack = getColumnToPack(e.getPoint());
        if (columnToPack != -1 && canResize(columnToPack)) {
          if (e.getClickCount() % 2 == 0) {
            packColumn(columnToPack);
          }
          return; // prevents click events in column resize area
        }
      }
      super.processMouseEvent(e);
    }

    protected void packColumn(int columnToPack) {
      TableColumn column = getColumnModel().getColumn(columnToPack);
      int currentWidth = column.getWidth();
      int expandedWidth = getExpandedColumnWidth(columnToPack);
      int newWidth = getColumnModel().getColumnMargin() +
                     (currentWidth >= expandedWidth ? getPreferredHeaderWidth(columnToPack) : expandedWidth);

      setResizingColumn(column);
      column.setWidth(newWidth);
      Dimension tableSize = JBTable.this.getSize();
      tableSize.width += newWidth - column.getWidth();
      JBTable.this.setSize(tableSize);
      // let the table update it's layout with resizing column set
      ApplicationManager.getApplication().invokeLater(() -> setResizingColumn(null));
    }

    private int getColumnToPack(Point p) {
      int viewColumnIdx = JBTable.this.columnAtPoint(p);
      if (viewColumnIdx == -1) return -1;

      Rectangle headerRect = getHeaderRect(viewColumnIdx);

      boolean atLeftBound = p.x - headerRect.x < COLUMN_RESIZE_AREA_WIDTH;
      if (atLeftBound) {
        return viewColumnIdx == 0 ? viewColumnIdx : viewColumnIdx - 1;
      }

      boolean atRightBound = headerRect.x + headerRect.width - p.x < COLUMN_RESIZE_AREA_WIDTH;
      return atRightBound ? viewColumnIdx : -1;
    }

    private boolean canResize(int columnIdx) {
      TableColumnModel columnModel = getColumnModel();
      return resizingAllowed && columnModel.getColumn(columnIdx).getResizable();
    }
  }

  public int getExpandedColumnWidth(int columnToExpand) {
    int expandedWidth = getPreferredHeaderWidth(columnToExpand);
    for (int row = 0; row < getRowCount(); row++) {
      TableCellRenderer cellRenderer = getCellRenderer(row, columnToExpand);
      if (cellRenderer != null) {
        Component c = prepareRenderer(cellRenderer, row, columnToExpand);
        expandedWidth = Math.max(expandedWidth, c.getPreferredSize().width);
      }
    }
    return expandedWidth;
  }

  private int getPreferredHeaderWidth(int columnIdx) {
    TableColumn column = getColumnModel().getColumn(columnIdx);
    TableCellRenderer renderer = column.getHeaderRenderer();
    if (renderer == null) {
      JTableHeader header = getTableHeader();
      if (header == null) {
        return DEFAULT_MIN_COLUMN_WIDTH;
      }
      renderer = header.getDefaultRenderer();
    }
    Object headerValue = column.getHeaderValue();
    Component headerCellRenderer = renderer.getTableCellRendererComponent(this, headerValue, false, false, -1, columnIdx);
    return headerCellRenderer.getPreferredSize().width;
  }

  /**
   * JTable gets table data from model lazily - only for a table part to be shown.
   * JBTable loads <i>all</i> the data on initialization to calculate cell size.
   * This methods provides possibility to calculate size without loading all the table data.
   *
   * @param maxItemsForSizeCalculation maximum number ot items in table to be loaded for size calculation
   */
  public void setMaxItemsForSizeCalculation(int maxItemsForSizeCalculation) {
    myMaxItemsForSizeCalculation = maxItemsForSizeCalculation;
  }

  @Override
  public AccessibleContext getAccessibleContext() {
    if (accessibleContext == null) {
      accessibleContext = new AccessibleJBTable();
    }
    return accessibleContext;
  }

  /**
   * Specialization of {@link AccessibleJTable} to ensure instances of
   * {@link AccessibleJBTableCell}, as opposed to {@link AccessibleJTableCell},
   * are created in all code paths.
   */
  protected class AccessibleJBTable extends AccessibleJTable {
    @Override
    public Accessible getAccessibleChild(int i) {
      if (i < 0 || i >= getAccessibleChildrenCount()) {
        return null;
      } else {
        int column = getAccessibleColumnAtIndex(i);
        int row = getAccessibleRowAtIndex(i);
        return new AccessibleJBTableCell(JBTable.this, row, column, getAccessibleIndexAt(row, column));
      }
    }

    @Override
    public Accessible getAccessibleAt(Point p) {
      int column = columnAtPoint(p);
      int row = rowAtPoint(p);

      if ((column != -1) && (row != -1)) {
        return getAccessibleChild(getAccessibleIndexAt(row, column));
      }
      return null;
    }

    /**
     * Specialization of {@link AccessibleJTableCell} to ensure the underlying cell renderer
     * is obtained by calling the virtual method {@link JTable#getCellRenderer(int, int)}.
     *
     * <p>
     * NOTE: The reason we need this class is that even though the documentation of the
     * {@link JTable#getCellRenderer(int, int)} method mentions that
     * </p>
     *
     * <pre>
     * Throughout the table package, the internal implementations always
     * use this method to provide renderers so that this default behavior
     * can be safely overridden by a subclass.
     * </pre>
     *
     * <p>
     * the {@link AccessibleJTableCell#getCurrentComponent()} and
     * {@link AccessibleJTableCell#getCurrentAccessibleContext()} methods do not
     * respect that contract, instead using a <strong>copy</strong> of the default
     * implementation of {@link JTable#getCellRenderer(int, int)}.
     * </p>
     *
     * <p>
     * There are a few derived classes of {@link JBTable}, e.g.
     * {@link consulo.ide.impl.idea.ui.dualView.TreeTableView} that depend on the ability to
     * override {@link JTable#getCellRenderer(int, int)} method to behave correctly,
     * so we need to ensure we go through the same code path to ensure correct
     * accessibility behavior.
     * </p>
     */
    protected class AccessibleJBTableCell extends AccessibleJTableCell {
      private final int myRow;
      private final int myColumn;

      public AccessibleJBTableCell(JTable table, int row, int columns, int index) {
        super(table, row, columns, index);
        this.myRow = row;
        this.myColumn = columns;
      }

      @Override
      protected Component getCurrentComponent() {
        return JBTable.this
                .getCellRenderer(myRow, myColumn)
                .getTableCellRendererComponent(JBTable.this, getValueAt(myRow, myColumn), false, false, myRow, myColumn);
      }

      @Override
      protected AccessibleContext getCurrentAccessibleContext() {
        Component c = getCurrentComponent();
        if (c instanceof Accessible)
          return c.getAccessibleContext();
        // Note: don't call "super" as 1) we know for sure the cell is not accessible
        // and 2) the super implementation is incorrect anyways
        return null;
      }
    }
  }
}
