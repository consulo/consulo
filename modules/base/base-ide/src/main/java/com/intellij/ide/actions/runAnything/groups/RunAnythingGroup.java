// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything.groups;

import com.intellij.ide.actions.runAnything.items.RunAnythingItem;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.codeStyle.NameUtil;
import com.intellij.ui.CollectionListModel;
import com.intellij.util.Function;
import consulo.util.collection.primitive.ints.IntList;
import consulo.util.collection.primitive.ints.IntLists;
import gnu.trove.TIntArrayList;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Represents 'run anything' list group.
 */
public abstract class RunAnythingGroup {
  public static final Function<String, NameUtil.MatcherBuilder> RUN_ANYTHING_MATCHER_BUILDER = pattern -> NameUtil.buildMatcher("*" + pattern);

  /**
   * {@link #myMoreIndex} is a group's 'load more..' index in the main list.
   * -1 means that group has all items loaded and no more 'load more..' placeholder
   */
  volatile int myMoreIndex = -1;

  /**
   * {@link #myTitleIndex} is an index of group title in the main list.
   * -1 means that group has zero elements and thus has no showing title
   */
  private volatile int myTitleIndex = -1;

  /**
   * @return Current group title in the main list.
   */
  @Nonnull
  public abstract String getTitle();

  /**
   * @return Current group maximum number of items to be shown.
   */
  protected int getMaxInitialItems() {
    return 5;
  }

  /**
   * @return Current group maximum number of items to be insert by click on 'load more..'.
   */
  protected int getMaxItemsToInsert() {
    return 5;
  }

  /**
   * Gets current group items to add into the main list.
   *
   * @param dataContext
   * @param model               needed to avoid adding duplicates into the list
   * @param pattern             input search string
   * @param isInsertionMode     if true gets {@link #getMaxItemsToInsert()} group items, else limits to {@link #getMaxInitialItems()}
   * @param cancellationChecker checks 'load more' calculation process to be cancelled
   */
  public abstract SearchResult getItems(@Nonnull DataContext dataContext, @Nonnull CollectionListModel<Object> model, @Nonnull String pattern, boolean isInsertionMode, @Nonnull Runnable cancellationChecker);

  /**
   * Resets current group 'load more..' {@link #myMoreIndex} index.
   */
  public void resetMoreIndex() {
    myMoreIndex = -1;
  }

  /**
   * Shifts {@link #myMoreIndex} for all groups starting from {@code baseIndex} by {@code shift}.
   */
  private static void shiftMoreIndex(Collection<? extends RunAnythingGroup> groups, int baseIndex, int shift) {
    groups.stream().filter(runAnythingGroup -> runAnythingGroup.myMoreIndex >= baseIndex).forEach(runAnythingGroup -> runAnythingGroup.myMoreIndex += shift);
  }

  /**
   * Finds group title by {@code titleIndex}.
   *
   * @return group title if {@code titleIndex} is equals to group {@link #myTitleIndex} and {@code null} if nothing found
   */
  @Nullable
  public static String getTitle(@Nonnull Collection<? extends RunAnythingGroup> groups, int titleIndex) {
    return Optional.ofNullable(findGroup(groups, titleIndex)).map(RunAnythingGroup::getTitle).orElse(null);
  }

  /**
   * Finds group by {@code titleIndex}.
   *
   * @return group if {@code titleIndex} is equals to group {@link #myTitleIndex} and {@code null} if nothing found
   */
  @Nullable
  public static RunAnythingGroup findGroup(@Nonnull Collection<? extends RunAnythingGroup> groups, int titleIndex) {
    return groups.stream().filter(runAnythingGroup -> titleIndex == ((RunAnythingGroup)runAnythingGroup).myTitleIndex).findFirst().orElse(null);
  }

  /**
   * Finds group {@code itemIndex} belongs to.
   */
  @Nullable
  public static RunAnythingGroup findItemGroup(@Nonnull List<? extends RunAnythingGroup> groups, int itemIndex) {
    RunAnythingGroup runAnythingGroup = null;
    for (RunAnythingGroup group : groups) {
      if (group.myTitleIndex == -1) {
        continue;
      }
      if (group.myTitleIndex > itemIndex) {
        break;
      }
      runAnythingGroup = group;
    }

    return runAnythingGroup;
  }

  /**
   * Shifts {@link #myTitleIndex} starting from {@code baseIndex} to {@code shift}.
   */
  private static void shiftTitleIndex(@Nonnull Collection<? extends RunAnythingGroup> groups, int baseIndex, int shift) {
    ((Collection<RunAnythingGroup>)groups).stream().filter(runAnythingGroup -> runAnythingGroup.myTitleIndex != -1 && runAnythingGroup.myTitleIndex > baseIndex)
            .forEach(runAnythingGroup -> runAnythingGroup.myTitleIndex += shift);
  }

  /**
   * Clears {@link #myMoreIndex} of all groups.
   */
  public static void clearMoreIndex(@Nonnull Collection<? extends RunAnythingGroup> groups) {
    groups.forEach(runAnythingGroup -> runAnythingGroup.myMoreIndex = -1);
  }

  /**
   * Clears {@link #myTitleIndex} of all groups.
   */
  private static void clearTitleIndex(@Nonnull Collection<? extends RunAnythingGroup> groups) {
    groups.forEach(runAnythingGroup -> ((RunAnythingGroup)runAnythingGroup).myTitleIndex = -1);
  }

  /**
   * Joins {@link #myTitleIndex} and {@link #myMoreIndex} of all groups; using for navigating by 'TAB' between groups.
   */
  public static int[] getAllIndexes(@Nonnull Collection<? extends RunAnythingGroup> groups) {
    IntList list = IntLists.newArrayList();
    for (RunAnythingGroup runAnythingGroup : groups) {
      list.add(runAnythingGroup.myTitleIndex);
    }
    for (RunAnythingGroup runAnythingGroup : groups) {
      list.add(runAnythingGroup.myMoreIndex);
    }

    return list.toArray();
  }

  /**
   * Finds matched by {@link #myMoreIndex} group.
   */
  @Nullable
  public static RunAnythingGroup findGroupByMoreIndex(@Nonnull Collection<? extends RunAnythingGroup> groups, int moreIndex) {
    return groups.stream().filter(runAnythingGroup -> moreIndex == runAnythingGroup.myMoreIndex).findFirst().orElse(null);
  }

  /**
   * Returns {@code true} if {@code index} is a {@link #myMoreIndex} of some group, {@code false} otherwise
   */
  public static boolean isMoreIndex(@Nonnull Collection<? extends RunAnythingGroup> groups, int index) {
    return groups.stream().anyMatch(runAnythingGroup -> runAnythingGroup.myMoreIndex == index);
  }

  /**
   * Shifts {@link #myMoreIndex} and {@link #myTitleIndex} of all groups starting from {@code baseIndex} to {@code shift}.
   */
  public static void shiftIndexes(@Nonnull Collection<? extends RunAnythingGroup> groups, int baseIndex, int shift) {
    shiftTitleIndex(groups, baseIndex, shift);
    shiftMoreIndex(groups, baseIndex, shift);
  }

  /**
   * Clears {@link #myMoreIndex} and {@link #myTitleIndex} of all groups.
   */
  public static void clearIndexes(@Nonnull Collection<? extends RunAnythingGroup> groups) {
    clearTitleIndex(groups);
    clearMoreIndex(groups);
  }

  /**
   * Adds current group matched items into the list.
   *
   * @param dataContext
   * @param model               needed to avoid adding duplicates into the list
   * @param pattern             input search string
   * @param cancellationChecker runnable that should throw a {@code ProcessCancelledException} if 'load more' process was cancelled
   */
  public final synchronized void collectItems(@Nonnull DataContext dataContext, @Nonnull CollectionListModel<Object> model, @Nonnull String pattern, @Nonnull Runnable cancellationChecker) {
    SearchResult result = getItems(dataContext, model, pattern, false, cancellationChecker);

    cancellationChecker.run();
    if (!result.isEmpty()) {
      ApplicationManager.getApplication().invokeLater(() -> {
        cancellationChecker.run();

        myTitleIndex = model.getSize();
        result.forEach(model::add);
        myMoreIndex = result.myNeedMore ? model.getSize() - 1 : -1;
      });
    }
  }

  /**
   * Represents collection of the group items with {@code myNeedMore} flag is set to true when limit is exceeded
   */
  public static class SearchResult extends ArrayList<RunAnythingItem> {
    boolean myNeedMore;

    public boolean isNeedMore() {
      return myNeedMore;
    }

    public void setNeedMore(boolean needMore) {
      myNeedMore = needMore;
    }
  }
}