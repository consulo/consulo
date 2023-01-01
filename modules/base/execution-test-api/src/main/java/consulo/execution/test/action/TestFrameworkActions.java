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

/*
 * User: anna
 * Date: 25-May-2007
 */
package consulo.execution.test.action;

import consulo.component.util.config.AbstractProperty;
import consulo.disposer.Disposer;
import consulo.execution.test.Filter;
import consulo.execution.test.TestConsoleProperties;
import consulo.execution.test.TestFrameworkPropertyListener;
import consulo.execution.test.TestFrameworkRunningModel;

public class TestFrameworkActions {
  public static void installFilterAction(final TestFrameworkRunningModel model) {
    final TestConsoleProperties properties = model.getProperties();
    final TestFrameworkPropertyListener<Boolean> hidePropertyListener = new TestFrameworkPropertyListener<Boolean>() {
      @Override
      public void onChanged(final Boolean value) {
        model.setFilter(getFilter(properties));
      }
    };
    addPropertyListener(TestConsoleProperties.HIDE_PASSED_TESTS, hidePropertyListener, model, true);

    final TestFrameworkPropertyListener<Boolean> ignorePropertyListener = new TestFrameworkPropertyListener<Boolean>() {
      @Override
      public void onChanged(final Boolean value) {
        model.setFilter(getFilter(properties));
      }
    };
    addPropertyListener(TestConsoleProperties.HIDE_IGNORED_TEST, ignorePropertyListener, model, true);
  }

  private static Filter getFilter(TestConsoleProperties properties) {
    final boolean shouldFilterPassed = TestConsoleProperties.HIDE_PASSED_TESTS.value(properties);
    final Filter hidePassedFilter = shouldFilterPassed ? Filter.NOT_PASSED.or(Filter.DEFECT) : Filter.NO_FILTER;

    final boolean shouldFilterIgnored = TestConsoleProperties.HIDE_IGNORED_TEST.value(properties);
    final Filter hideIgnoredFilter = shouldFilterIgnored ? Filter.IGNORED.not() : Filter.NO_FILTER;
    return hidePassedFilter.and(hideIgnoredFilter);
  }

  public static void addPropertyListener(final AbstractProperty<Boolean> property,
                                         final TestFrameworkPropertyListener<Boolean> propertyListener,
                                         final TestFrameworkRunningModel model,
                                         final boolean sendValue) {
    final TestConsoleProperties properties = model.getProperties();
    if (sendValue) {
      properties.addListenerAndSendValue(property, propertyListener);
    }
    else {
      properties.addListener(property, propertyListener);
    }
    Disposer.register(model, () -> properties.removeListener(property, propertyListener));
  }
}