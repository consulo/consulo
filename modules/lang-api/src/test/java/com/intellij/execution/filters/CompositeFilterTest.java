/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.execution.filters;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.DumbModeTask;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ModificationTracker;
import junit.framework.Assert;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import java.util.List;

public class CompositeFilterTest {

  protected CompositeFilter myCompositeFilter;

  @Before
  public void setUp() throws Exception {
    myCompositeFilter = new CompositeFilter(new DumbService() {
      @Override
      public ModificationTracker getModificationTracker() {
        return null;
      }

      @Override
      public boolean isDumb() {
        return false;
      }

      @Override
      public void runWhenSmart(Runnable runnable) {
      }

      @Override
      public void waitForSmartMode() {
      }

      @Override
      public void smartInvokeLater(@NotNull Runnable runnable) {

      }

      @Override
      public void smartInvokeLater(@NotNull Runnable runnable, @NotNull ModalityState modalityState) {

      }

      @Override
      public void queueTask(@NotNull DumbModeTask task) {

      }

      @Override
      public void cancelTask(@NotNull DumbModeTask task) {

      }

      @Override
      public void completeJustSubmittedTasks() {

      }

      @Override
      public JComponent wrapGently(@NotNull JComponent dumbUnawareContent, @NotNull Disposable parentDisposable) {
        return null;
      }

      @Override
      public void showDumbModeNotification(String message) {
      }

      @Override
      public Project getProject() {
        return null;
      }

      @Override
      public void setAlternativeResolveEnabled(boolean enabled) {

      }

      @Override
      public boolean isAlternativeResolveEnabled() {
        return false;
      }
    });
  }

  @Test
  public void testApplyNextFilter() throws Exception {
    Assert.assertNull(applyFilter());

    myCompositeFilter.addFilter(returnNullFilter());
    Assert.assertNull(applyFilter());

    myCompositeFilter.addFilter(returnContinuingResultFilter());
    notNullResultOfSize(applyFilter(), 1);

    myCompositeFilter.addFilter(returnNullFilter());
    myCompositeFilter.addFilter(returnContinuingResultFilter());
    notNullResultOfSize(applyFilter(), 2);

    myCompositeFilter.addFilter(returnNullFilter());
    notNullResultOfSize(applyFilter(), 2);

    myCompositeFilter.addFilter(returnResultFilter());
    notNullResultOfSize(applyFilter(), 3);

    myCompositeFilter.addFilter(returnResultFilter());
    notNullResultOfSize(applyFilter(), 3);

  }

  private Filter.Result applyFilter() {
    return myCompositeFilter.applyFilter("foo\n", 10);
  }

  private void notNullResultOfSize(Filter.Result object, int expected) {
    Assert.assertNotNull(object);
    List<Filter.ResultItem> resultItems = object.getResultItems();
    Assert.assertEquals(expected, resultItems.size());

    for (Filter.ResultItem resultItem : resultItems) {
      Assert.assertNotNull(resultItem);
    }
  }

  private Filter returnNullFilter() {
    return new Filter() {
      @Nullable
      @Override
      public Result applyFilter(String line, int entireLength) {
        return null;
      }
    };
  }

  private Filter returnResultFilter() {
    return new Filter() {
      @Nullable
      @Override
      public Result applyFilter(String line, int entireLength) {
        return createResult();
      }
    };
  }

  private Filter returnContinuingResultFilter() {
    return new Filter() {
      @Nullable
      @Override
      public Result applyFilter(String line, int entireLength) {
        Result result = createResult();
        result.setNextAction(NextAction.CONTINUE_FILTERING);
        return result;
      }
    };
  }

  private Filter.Result createResult() {
    return new Filter.Result(1, 1, null, null);
  }
}
