/*
 * Copyright 2013-2020 consulo.io
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
package consulo.sandboxPlugin.ide.action;

import consulo.ui.ex.action.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import consulo.sandboxPlugin.ui.UITester;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.dialog.DialogService;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2020-05-29
 */
public class ShowUITesterAction extends DumbAwareAction {
  private final DialogService myDialogService;

  public ShowUITesterAction(DialogService dialogService) {
    myDialogService = dialogService;
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    UITester.show(myDialogService);
  }
}
