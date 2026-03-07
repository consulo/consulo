// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.list.search;

import org.junit.jupiter.api.Test;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

final class MultiChooserListModelTest {

  @Test
  public void testInitialState() {
    MultiChooserListModel<String> model = new MultiChooserListModel<>();
    assertThat(model.getSize()).isEqualTo(0);
    assertThat(model.getElementAt(0)).isNull();
    assertThat(model.getChosenItems()).isEmpty();
  }

  @Test
  public void testAddItems() {
    MultiChooserListModel<String> model = new MultiChooserListModel<>();
    model.add(List.of("item1", "item2", "item3"));

    assertThat(model.getSize()).isEqualTo(3);
    assertThat(model.getElementAt(0)).isEqualTo("item1");
    assertThat(model.getElementAt(1)).isEqualTo("item2");
    assertThat(model.getElementAt(2)).isEqualTo("item3");
  }

  @Test
  public void testAddDuplicateItems() {
    MultiChooserListModel<String> model = new MultiChooserListModel<>();
    model.add(List.of("item1", "item2"));
    model.add(List.of("item2", "item3"));

    assertThat(model.getSize()).isEqualTo(3);
    assertThat(model.getElementAt(0)).isEqualTo("item1");
    assertThat(model.getElementAt(1)).isEqualTo("item2");
    assertThat(model.getElementAt(2)).isEqualTo("item3");
  }

  @Test
  public void testToggleChosen() {
    MultiChooserListModel<String> model = new MultiChooserListModel<>();
    model.add(List.of("item1", "item2"));

    assertThat(model.isChosen("item1")).isFalse();
    model.toggleChosen(0);
    assertThat(model.isChosen("item1")).isTrue();

    model.toggleChosen(0);
    assertThat(model.isChosen("item1")).isFalse();
  }

  @Test
  public void testSetChosenItems() {
    MultiChooserListModel<String> model = new MultiChooserListModel<>();
    model.add(List.of("item1", "item2", "item3"));

    model.setChosen(List.of("item1", "item3"));
    assertThat(model.isChosen("item1")).isTrue();
    assertThat(model.isChosen("item2")).isFalse();
    assertThat(model.isChosen("item3")).isTrue();

    assertThat(model.getChosenItems()).containsExactly("item1", "item3");
  }

  @Test
  public void testSetChosenClearsPreviousSelection() {
    MultiChooserListModel<String> model = new MultiChooserListModel<>();
    model.add(List.of("item1", "item2", "item3"));

    model.setChosen(List.of("item1", "item2"));
    model.setChosen(List.of("item3"));

    assertThat(model.isChosen("item1")).isFalse();
    assertThat(model.isChosen("item2")).isFalse();
    assertThat(model.isChosen("item3")).isTrue();
  }

  @Test
  public void testSetChosenIgnoresItemsNotInModel() {
    MultiChooserListModel<String> model = new MultiChooserListModel<>();
    model.add(List.of("item1", "item2"));

    model.setChosen(List.of("item1", "nonexistent"));
    assertThat(model.isChosen("item1")).isTrue();
    assertThat(model.isChosen("nonexistent")).isFalse();
    assertThat(model.getChosenItems()).containsExactly("item1");
  }

  @Test
  public void testToggleChosenWithNegativeIndex() {
    MultiChooserListModel<String> model = new MultiChooserListModel<>();
    model.add(List.of("item1"));

    model.toggleChosen(-1);
    assertThat(model.isChosen("item1")).isFalse();
  }

  @Test
  public void testToggleChosenWithOutOfBoundsIndex() {
    MultiChooserListModel<String> model = new MultiChooserListModel<>();
    model.add(List.of("item1"));

    model.toggleChosen(5);
    assertThat(model.isChosen("item1")).isFalse();
  }

  @Test
  public void testGetChosenItemsPreservesOrder() {
    MultiChooserListModel<String> model = new MultiChooserListModel<>();
    model.add(List.of("item1", "item2", "item3", "item4"));

    model.setChosen(List.of("item3", "item1", "item4"));
    assertThat(model.getChosenItems()).containsExactly("item1", "item3", "item4");
  }

  @Test
  public void testAddFiresIntervalAddedEvent() {
    MultiChooserListModel<String> model = new MultiChooserListModel<>();
    boolean[] eventFired = {false};
    int[] eventType = {-1};

    model.addListDataListener(new ListDataListener() {
      @Override
      public void intervalAdded(ListDataEvent e) {
        eventFired[0] = true;
        eventType[0] = e.getType();
      }

      @Override
      public void intervalRemoved(ListDataEvent e) {
      }

      @Override
      public void contentsChanged(ListDataEvent e) {
      }
    });

    model.add(List.of("item1", "item2"));
    assertThat(eventFired[0]).isTrue();
    assertThat(eventType[0]).isEqualTo(ListDataEvent.INTERVAL_ADDED);
  }

  @Test
  public void testToggleChosenFiresContentsChangedEvent() {
    MultiChooserListModel<String> model = new MultiChooserListModel<>();
    model.add(List.of("item1"));

    boolean[] eventFired = {false};
    int[] eventType = {-1};

    model.addListDataListener(new ListDataListener() {
      @Override
      public void intervalAdded(ListDataEvent e) {
      }

      @Override
      public void intervalRemoved(ListDataEvent e) {
      }

      @Override
      public void contentsChanged(ListDataEvent e) {
        eventFired[0] = true;
        eventType[0] = e.getType();
      }
    });

    model.toggleChosen(0);
    assertThat(eventFired[0]).isTrue();
    assertThat(eventType[0]).isEqualTo(ListDataEvent.CONTENTS_CHANGED);
  }

  @Test
  public void testSetChosenFiresContentsChangedEvent() {
    MultiChooserListModel<String> model = new MultiChooserListModel<>();
    model.add(List.of("item1", "item2"));

    int[] eventCount = {0};

    model.addListDataListener(new ListDataListener() {
      @Override
      public void intervalAdded(ListDataEvent e) {
      }

      @Override
      public void intervalRemoved(ListDataEvent e) {
      }

      @Override
      public void contentsChanged(ListDataEvent e) {
        eventCount[0]++;
      }
    });

    model.setChosen(List.of("item1", "item2"));
    assertThat(eventCount[0]).isEqualTo(2);
  }

  @Test
  public void testAddEmptyListDoesNotFireEvent() {
    MultiChooserListModel<String> model = new MultiChooserListModel<>();
    model.add(List.of("item1"));

    boolean[] eventFired = {false};

    model.addListDataListener(new ListDataListener() {
      @Override
      public void intervalAdded(ListDataEvent e) {
        eventFired[0] = true;
      }

      @Override
      public void intervalRemoved(ListDataEvent e) {
      }

      @Override
      public void contentsChanged(ListDataEvent e) {
      }
    });

    model.add(List.of());
    assertThat(eventFired[0]).isFalse();
  }

  @Test
  public void testAddOnlyDuplicatesDoesNotFireEvent() {
    MultiChooserListModel<String> model = new MultiChooserListModel<>();
    model.add(List.of("item1", "item2"));

    boolean[] eventFired = {false};

    model.addListDataListener(new ListDataListener() {
      @Override
      public void intervalAdded(ListDataEvent e) {
        eventFired[0] = true;
      }

      @Override
      public void intervalRemoved(ListDataEvent e) {
      }

      @Override
      public void contentsChanged(ListDataEvent e) {
      }
    });

    model.add(List.of("item1", "item2"));
    assertThat(eventFired[0]).isFalse();
  }

  @Test
  public void testRemoveAllExceptChosenRemovesNonChosenItems() {
    MultiChooserListModel<String> model = new MultiChooserListModel<>();
    model.add(List.of("item1", "item2", "item3", "item4"));
    model.setChosen(List.of("item1", "item3"));

    model.removeAllExceptChosen();

    assertThat(model.getSize()).isEqualTo(2);
    assertThat(model.getElementAt(0)).isEqualTo("item1");
    assertThat(model.getElementAt(1)).isEqualTo("item3");
    assertThat(model.getChosenItems()).containsExactly("item1", "item3");
  }

  @Test
  public void testRemoveAllExceptChosenWithNoChosenItemsClearsList() {
    MultiChooserListModel<String> model = new MultiChooserListModel<>();
    model.add(List.of("item1", "item2", "item3"));

    model.removeAllExceptChosen();

    assertThat(model.getSize()).isEqualTo(0);
    assertThat(model.getChosenItems()).isEmpty();
  }

  @Test
  public void testRemoveAllExceptChosenWithAllChosenItemsKeepsAll() {
    MultiChooserListModel<String> model = new MultiChooserListModel<>();
    model.add(List.of("item1", "item2", "item3"));
    model.setChosen(List.of("item1", "item2", "item3"));

    model.removeAllExceptChosen();

    assertThat(model.getSize()).isEqualTo(3);
    assertThat(model.getElementAt(0)).isEqualTo("item1");
    assertThat(model.getElementAt(1)).isEqualTo("item2");
    assertThat(model.getElementAt(2)).isEqualTo("item3");
  }

  @Test
  public void testRemoveAllExceptChosenFiresIntervalRemovedEvent() {
    MultiChooserListModel<String> model = new MultiChooserListModel<>();
    model.add(List.of("item1", "item2", "item3"));
    model.setChosen(List.of("item1"));

    boolean[] intervalRemovedFired = {false};

    model.addListDataListener(new ListDataListener() {
      @Override
      public void intervalAdded(ListDataEvent e) {
      }

      @Override
      public void intervalRemoved(ListDataEvent e) {
        intervalRemovedFired[0] = true;
      }

      @Override
      public void contentsChanged(ListDataEvent e) {
      }
    });

    model.removeAllExceptChosen();
    assertThat(intervalRemovedFired[0]).isTrue();
  }

  @Test
  public void testRemoveAllExceptChosenOnEmptyListDoesNothing() {
    MultiChooserListModel<String> model = new MultiChooserListModel<>();

    boolean[] eventFired = {false};

    model.addListDataListener(new ListDataListener() {
      @Override
      public void intervalAdded(ListDataEvent e) {
        eventFired[0] = true;
      }

      @Override
      public void intervalRemoved(ListDataEvent e) {
        eventFired[0] = true;
      }

      @Override
      public void contentsChanged(ListDataEvent e) {
        eventFired[0] = true;
      }
    });

    model.removeAllExceptChosen();
    assertThat(eventFired[0]).isFalse();
    assertThat(model.getSize()).isEqualTo(0);
  }

  @Test
  public void testRetainChosenAndUpdateReplacesItemsWithNewList() {
    MultiChooserListModel<String> model = new MultiChooserListModel<>();
    model.add(List.of("item1", "item2", "item3"));

    model.retainChosenAndUpdate(List.of("item4", "item5"));

    assertThat(model.getSize()).isEqualTo(2);
    assertThat(model.getElementAt(0)).isEqualTo("item4");
    assertThat(model.getElementAt(1)).isEqualTo("item5");
  }

  @Test
  public void testRetainChosenAndUpdateKeepsChosenItems() {
    MultiChooserListModel<String> model = new MultiChooserListModel<>();
    model.add(List.of("item1", "item2", "item3"));
    model.setChosen(List.of("item1", "item3"));

    model.retainChosenAndUpdate(List.of("item4", "item5"));

    assertThat(model.getSize()).isEqualTo(4);
    assertThat(model.getElementAt(0)).isEqualTo("item1");
    assertThat(model.getElementAt(1)).isEqualTo("item3");
    assertThat(model.getElementAt(2)).isEqualTo("item4");
    assertThat(model.getElementAt(3)).isEqualTo("item5");
    assertThat(model.isChosen("item1")).isTrue();
    assertThat(model.isChosen("item3")).isTrue();
  }

  @Test
  public void testRetainChosenAndUpdateAvoidsDuplicatesBetweenChosenAndNewItems() {
    MultiChooserListModel<String> model = new MultiChooserListModel<>();
    model.add(List.of("item1", "item2", "item3"));
    model.setChosen(List.of("item1", "item2"));

    model.retainChosenAndUpdate(List.of("item2", "item3", "item4"));

    assertThat(model.getSize()).isEqualTo(4);
    assertThat(model.getElementAt(0)).isEqualTo("item1");
    assertThat(model.getElementAt(1)).isEqualTo("item2");
    assertThat(model.getElementAt(2)).isEqualTo("item3");
    assertThat(model.getElementAt(3)).isEqualTo("item4");
  }

  @Test
  public void testRetainChosenAndUpdateWithEmptyNewListKeepsChosenItems() {
    MultiChooserListModel<String> model = new MultiChooserListModel<>();
    model.add(List.of("item1", "item2", "item3"));
    model.setChosen(List.of("item1", "item3"));

    model.retainChosenAndUpdate(List.of());

    assertThat(model.getSize()).isEqualTo(2);
    assertThat(model.getElementAt(0)).isEqualTo("item1");
    assertThat(model.getElementAt(1)).isEqualTo("item3");
    assertThat(model.getChosenItems()).containsExactly("item1", "item3");
  }

  @Test
  public void testRetainChosenAndUpdateWithNoChosenItemsReplacesAll() {
    MultiChooserListModel<String> model = new MultiChooserListModel<>();
    model.add(List.of("item1", "item2", "item3"));

    model.retainChosenAndUpdate(List.of("item4", "item5"));

    assertThat(model.getSize()).isEqualTo(2);
    assertThat(model.getElementAt(0)).isEqualTo("item4");
    assertThat(model.getElementAt(1)).isEqualTo("item5");
  }

  @Test
  public void testRetainChosenAndUpdateFiresAppropriateEventsWhenGrowing() {
    MultiChooserListModel<String> model = new MultiChooserListModel<>();
    model.add(List.of("item1", "item2"));

    boolean[] contentsChangedFired = {false};
    boolean[] intervalAddedFired = {false};

    model.addListDataListener(new ListDataListener() {
      @Override
      public void intervalAdded(ListDataEvent e) {
        intervalAddedFired[0] = true;
      }

      @Override
      public void intervalRemoved(ListDataEvent e) {
      }

      @Override
      public void contentsChanged(ListDataEvent e) {
        contentsChangedFired[0] = true;
      }
    });

    model.retainChosenAndUpdate(List.of("item3", "item4", "item5"));

    assertThat(contentsChangedFired[0]).isTrue();
    assertThat(intervalAddedFired[0]).isTrue();
  }

  @Test
  public void testRetainChosenAndUpdateFiresAppropriateEventsWhenShrinking() {
    MultiChooserListModel<String> model = new MultiChooserListModel<>();
    model.add(List.of("item1", "item2", "item3", "item4"));

    boolean[] contentsChangedFired = {false};
    boolean[] intervalRemovedFired = {false};

    model.addListDataListener(new ListDataListener() {
      @Override
      public void intervalAdded(ListDataEvent e) {
      }

      @Override
      public void intervalRemoved(ListDataEvent e) {
        intervalRemovedFired[0] = true;
      }

      @Override
      public void contentsChanged(ListDataEvent e) {
        contentsChangedFired[0] = true;
      }
    });

    model.retainChosenAndUpdate(List.of("itemA"));

    assertThat(contentsChangedFired[0]).isTrue();
    assertThat(intervalRemovedFired[0]).isTrue();
  }

  @Test
  public void testRetainChosenAndUpdatePreservesChosenState() {
    MultiChooserListModel<String> model = new MultiChooserListModel<>();
    model.add(List.of("item1", "item2", "item3"));
    model.setChosen(List.of("item1", "item2"));

    model.retainChosenAndUpdate(List.of("item1", "item4"));

    assertThat(model.isChosen("item1")).isTrue();
    assertThat(model.isChosen("item2")).isTrue();
    assertThat(model.isChosen("item4")).isFalse();
  }
}
