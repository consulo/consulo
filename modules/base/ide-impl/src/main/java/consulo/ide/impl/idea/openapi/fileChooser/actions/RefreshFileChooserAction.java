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
package consulo.ide.impl.idea.openapi.fileChooser.actions;

import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.application.dumb.DumbAware;
import consulo.virtualFileSystem.ManagingFS;
import consulo.virtualFileSystem.RefreshQueue;

/**
 * @author yole
*/
public class RefreshFileChooserAction extends AnAction implements DumbAware {
  public void actionPerformed(final AnActionEvent e) {
    RefreshQueue.getInstance().refresh(true, true, null, IdeaModalityState.current(), ManagingFS.getInstance().getLocalRoots());
  }
}