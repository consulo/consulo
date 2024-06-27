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

package consulo.ide.impl.idea.history.integration.ui.actions;

import consulo.application.dumb.DumbAware;
import consulo.ide.impl.idea.history.core.LocalHistoryFacade;
import consulo.ide.impl.idea.history.integration.IdeaGateway;
import consulo.ide.impl.idea.history.integration.LocalHistoryImpl;
import consulo.language.editor.CommonDataKeys;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.util.collection.Streams;
import consulo.versionControlSystem.VcsDataKeys;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import static consulo.util.lang.ObjectUtil.notNull;

public abstract class LocalHistoryAction extends AnAction implements DumbAware {
  @Override
  public void update(@Nonnull AnActionEvent e) {
    Presentation p = e.getPresentation();

    if (e.getData(CommonDataKeys.PROJECT) == null) {
      p.setEnabledAndVisible(false);
    }
    else {
      p.setVisible(true);
      p.setText(getText(e), true);

      LocalHistoryFacade vcs = getVcs();
      IdeaGateway gateway = getGateway();
      p.setEnabled(vcs != null && gateway != null && isEnabled(vcs, gateway, e));
    }
  }

  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    actionPerformed(e.getRequiredData(CommonDataKeys.PROJECT), notNull(getGateway()), e);
  }

  protected String getText(@Nonnull AnActionEvent e) {
    return e.getPresentation().getTextWithMnemonic();
  }

  protected boolean isEnabled(@Nonnull LocalHistoryFacade vcs, @Nonnull IdeaGateway gw, @Nonnull AnActionEvent e) {
    return isEnabled(vcs, gw, getFile(e), e);
  }

  protected void actionPerformed(@Nonnull Project p, @Nonnull IdeaGateway gw, @Nonnull AnActionEvent e) {
    actionPerformed(p, gw, notNull(getFile(e)), e);
  }

  protected boolean isEnabled(@Nonnull LocalHistoryFacade vcs,
                              @Nonnull IdeaGateway gw,
                              @jakarta.annotation.Nullable VirtualFile f,
                              @Nonnull AnActionEvent e) {
    return true;
  }

  protected void actionPerformed(@Nonnull Project p, @Nonnull IdeaGateway gw, @Nonnull VirtualFile f, @Nonnull AnActionEvent e) {
  }

  @Nullable
  protected LocalHistoryFacade getVcs() {
    return LocalHistoryImpl.getInstanceImpl().getFacade();
  }

  @Nullable
  protected IdeaGateway getGateway() {
    return LocalHistoryImpl.getInstanceImpl().getGateway();
  }

  @Nullable
  protected VirtualFile getFile(@Nonnull AnActionEvent e) {
    return Streams.getIfSingle(e.getData(VcsDataKeys.VIRTUAL_FILE_STREAM));
  }
}
