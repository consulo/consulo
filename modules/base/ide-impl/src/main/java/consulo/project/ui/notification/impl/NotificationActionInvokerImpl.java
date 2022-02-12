/*
 * Copyright 2013-2022 consulo.io
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
package consulo.project.ui.notification.impl;

import com.intellij.openapi.actionSystem.ex.ActionUtil;
import consulo.project.ui.notification.internal.NotificationActionInvoker;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;

/**
 * @author VISTALL
 * @since 12-Feb-22
 */
public class NotificationActionInvokerImpl implements NotificationActionInvoker {
  @Override
  public void invoke(AnAction action, AnActionEvent event) {
    if (ActionUtil.lastUpdateAndCheckDumb(action, event, false)) {
      ActionUtil.performActionDumbAware(action, event);
    }
  }
}
