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
package consulo.execution.test.action;

import consulo.application.AllIcons;
import consulo.application.ui.action.ToggleBooleanProperty;
import consulo.component.util.config.AbstractProperty;
import consulo.execution.localize.ExecutionLocalize;
import consulo.execution.test.TestConsoleProperties;
import consulo.execution.test.TestFrameworkRunningModel;

public class ScrollToTestSourceAction extends ToggleBooleanProperty.Disablable {
  private TestFrameworkRunningModel myModel;

  public ScrollToTestSourceAction(final TestConsoleProperties properties) {
    super(
      ExecutionLocalize.junitAutoScrollToSourceActionName(),
      ExecutionLocalize.junitOpenTextInEditorActionName(),
      AllIcons.General.AutoscrollToSource,
      properties,
      TestConsoleProperties.SCROLL_TO_SOURCE
    );
  }

  protected boolean isEnabled() {
    final AbstractProperty.AbstractPropertyContainer properties = getProperties();
    final TestFrameworkRunningModel model = myModel;
    return isEnabled(properties, model);
  }

  @Override
  protected boolean isVisible() {
    return true;
  }

  private static boolean isEnabled(final AbstractProperty.AbstractPropertyContainer properties, final TestFrameworkRunningModel model) {
    if (!TestConsoleProperties.TRACK_RUNNING_TEST.value(properties)) return true;
    return model != null && !model.isRunning();
  }

  public static boolean isScrollEnabled(final TestFrameworkRunningModel model) {
    final TestConsoleProperties properties = model.getProperties();
    return isEnabled(properties, model) && TestConsoleProperties.SCROLL_TO_SOURCE.value(properties);
  }

  public void setModel(final TestFrameworkRunningModel model) {
    myModel = model;
  }
}
