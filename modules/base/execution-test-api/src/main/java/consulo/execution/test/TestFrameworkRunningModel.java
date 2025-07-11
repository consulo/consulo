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
package consulo.execution.test;

import consulo.disposer.Disposable;
import consulo.execution.test.ui.AbstractTestTreeBuilder;
import consulo.execution.test.ui.TestTreeView;

/**
 * @author anna
 * @since 2007-05-25
 */
public interface TestFrameworkRunningModel extends Disposable {
  TestConsoleProperties getProperties();

  void setFilter(Filter filter);

  boolean isRunning();

  TestTreeView getTreeView();

  AbstractTestTreeBuilder getTreeBuilder();

  boolean hasTestSuites();

  AbstractTestProxy getRoot();

  void selectAndNotify(AbstractTestProxy testProxy);
}