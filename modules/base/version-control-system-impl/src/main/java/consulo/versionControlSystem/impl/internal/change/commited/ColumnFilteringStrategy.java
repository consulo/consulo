/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal.change.commited;

import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.ui.ex.awt.JBList;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.Lists;
import consulo.versionControlSystem.ChangeListColumn;
import consulo.versionControlSystem.CommittedChangesProvider;
import consulo.versionControlSystem.change.commited.ChangeListFilteringStrategy;
import consulo.versionControlSystem.change.commited.CommittedChangesFilterKey;
import consulo.versionControlSystem.change.commited.CommittedChangesFilterPriority;
import consulo.versionControlSystem.change.commited.ReceivedChangeList;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.versionBrowser.CommittedChangeList;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.util.*;
import java.util.function.Function;

/**
 * @author yole
 */
public class ColumnFilteringStrategy implements ChangeListFilteringStrategy {
  private final JScrollPane myScrollPane;
  private final JList myValueList;
  private final List<ChangeListener> myListeners = Lists.newLockFreeCopyOnWriteList();
  private final ChangeListColumn myColumn;
  private final Class<? extends CommittedChangesProvider> myProviderClass;
  private final MyListModel myModel;
  private final CommittedChangeListToStringConvertor ourConvertorInstance = new CommittedChangeListToStringConvertor();

  private Object[] myPrefferedSelection;

  public ColumnFilteringStrategy(ChangeListColumn column,
                                 Class<? extends CommittedChangesProvider> providerClass) {
    myModel = new MyListModel();
    myValueList = new JBList();
    myScrollPane = ScrollPaneFactory.createScrollPane(myValueList);
    myValueList.setModel(myModel);
    myValueList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        for (ChangeListener listener : myListeners) {
          listener.stateChanged(new ChangeEvent(this));
        }
      }
    });
    myValueList.setCellRenderer(new ColoredListCellRenderer() {
      @Override
      protected void customizeCellRenderer(JList list, Object value, int index, boolean selected, boolean hasFocus) {
        if (index == 0) {
          append(value.toString(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
        }
        else if (value.toString().length() == 0) {
          append(VcsLocalize.committedChangesFilterNone().get(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
        }
        else {
          append(value.toString(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        }
      }
    });
    myColumn = column;
    myProviderClass = providerClass;
  }

  @Override
  public CommittedChangesFilterKey getKey() {
    return new CommittedChangesFilterKey(toString(), CommittedChangesFilterPriority.USER);
  }

  @Override
  public String toString() {
    return myColumn.getTitle();
  }

  @Override
  @jakarta.annotation.Nullable
  public JComponent getFilterUI() {
    return myScrollPane;
  }

  @Override
  public void setFilterBase(List<CommittedChangeList> changeLists) {
    myPrefferedSelection = null;
    appendFilterBase(changeLists);
  }

  @Override
  public void addChangeListener(ChangeListener listener) {
    myListeners.add(listener);
  }

  @Override
  public void removeChangeListener(ChangeListener listener) {
    myListeners.remove(listener);
  }

  @Override
  public void resetFilterBase() {
    myPrefferedSelection = myValueList.getSelectedValues();
    myValueList.clearSelection();
    myModel.clear();
    myValueList.revalidate();
    myValueList.repaint();
  }

  @Override
  public void appendFilterBase(List<CommittedChangeList> changeLists) {
    Object[] oldSelection = myModel.isEmpty() ? myPrefferedSelection : myValueList.getSelectedValues();

    myModel.addNext(changeLists, ourConvertorInstance);
    if (oldSelection != null) {
      for (Object o : oldSelection) {
        myValueList.setSelectedValue(o, false);
      }
    }
    myValueList.revalidate();
    myValueList.repaint();
  }

  private class CommittedChangeListToStringConvertor implements Function<CommittedChangeList, String> {
    @Override
    public String apply(CommittedChangeList o) {
      if (myProviderClass == null || myProviderClass.isInstance(o.getVcs().getCommittedChangesProvider())) {
        return myColumn.getValue(ReceivedChangeList.unwrap(o)).toString();
      }
      return null;
    }
  }

  @Override
  @Nonnull
  public List<CommittedChangeList> filterChangeLists(List<CommittedChangeList> changeLists) {
    Object[] selection = myValueList.getSelectedValues();
    if (myValueList.getSelectedIndex() == 0 || selection.length == 0) {
      return changeLists;
    }
    List<CommittedChangeList> result = new ArrayList<CommittedChangeList>();
    for (CommittedChangeList changeList : changeLists) {
      if (myProviderClass == null || myProviderClass.isInstance(changeList.getVcs().getCommittedChangesProvider())) {
        for (Object value : selection) {
          //noinspection unchecked
          if (value.toString().equals(myColumn.getValue(ReceivedChangeList.unwrap(changeList)).toString())) {
            result.add(changeList);
            break;
          }
        }
      }
    }
    return result;
  }

  private static class MyListModel extends AbstractListModel {
    private volatile String[] myValues;

    private MyListModel() {
      myValues = ArrayUtil.EMPTY_STRING_ARRAY;
    }

    public <T> void addNext(Collection<T> values, Function<T, String> convertor) {
      TreeSet<String> set = new TreeSet<String>(Arrays.asList(myValues));
      for (T value : values) {
        String converted = convertor.apply(value);
        if (converted != null) {
          // also works as filter
          set.add(converted);
        }
      }
      myValues = ArrayUtil.toStringArray(set);
      fireContentsChanged(this, 0, myValues.length);
    }

    @Override
    public int getSize() {
      return myValues.length + 1;
    }

    public boolean isEmpty() {
      return myValues.length == 0;
    }

    @Override
    public Object getElementAt(int index) {
      if (index == 0) {
        return VcsLocalize.committedChangesFilterAll().get();
      }
      return myValues[index - 1];
    }

    public void clear() {
      myValues = ArrayUtil.EMPTY_STRING_ARRAY;
    }
  }
}
