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

import com.intellij.mock.MockDumbService;
import consulo.testFramework.MockApplicationTestCase;
import org.junit.Assert;

import java.util.List;

public class CompositeFilterTest extends MockApplicationTestCase {

  protected CompositeFilter myCompositeFilter;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    myCompositeFilter = new CompositeFilter(new MockDumbService(null));
  }

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
    return (line, entireLength) -> null;
  }

  private Filter returnResultFilter() {
    return (line, entireLength) -> createFilterResult();
  }

  private Filter returnContinuingResultFilter() {
    return (line, entireLength) -> {
      Filter.Result result = createFilterResult();
      result.setNextAction(Filter.NextAction.CONTINUE_FILTERING);
      return result;
    };
  }

  private Filter.Result createFilterResult() {
    return new Filter.Result(1, 1, null, null);
  }
}
