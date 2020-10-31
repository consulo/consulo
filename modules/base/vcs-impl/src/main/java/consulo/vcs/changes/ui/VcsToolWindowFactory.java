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
package consulo.vcs.changes.ui;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Trinity;
import com.intellij.openapi.vcs.changes.ui.ChangesViewContentI;
import com.intellij.openapi.vcs.changes.ui.ChangesViewContentManager;
import com.intellij.openapi.vcs.impl.VcsInitObject;
import com.intellij.openapi.vcs.impl.VcsStartupActivity;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * @author VISTALL
 * @since 2020-10-30
 */
public class VcsToolWindowFactory implements ToolWindowFactory, DumbAware {
  public static class UpdateVcsStartupActivity implements VcsStartupActivity {
    @Override
    public void runActivity(@Nonnull Project project) {
      ChangesViewContentManager manager = (ChangesViewContentManager)ChangesViewContentManager.getInstance(project);

      manager.update();
    }

    @Override
    public int getOrder() {
      return VcsInitObject.AFTER_COMMON.getOrder();
    }
  }

  @RequiredUIAccess
  @Override
  public void createToolWindowContent(@Nonnull Project project, @Nonnull ToolWindow toolWindow) {
    final ContentManager contentManager = toolWindow.getContentManager();

    ChangesViewContentManager manager = (ChangesViewContentManager)ChangesViewContentManager.getInstance(project);

    manager.loadExtensionTabs();

    Trinity<Image, LocalizeValue, Boolean> state = manager.getAlreadyLoadedState();
    manager.setAlreadyLoadedState(null);
    List<Content> contents = manager.setContentManager(toolWindow, contentManager);

    final List<Content> ordered = ChangesViewContentManager.doPresetOrdering(contents);
    for (Content content : ordered) {
      contentManager.addContent(content);
    }

    if (contentManager.getContentCount() > 0) {
      contentManager.setSelectedContent(contentManager.getContent(0));
    }

    if (state != null) {
      toolWindow.setIcon(state.getFirst());
      toolWindow.setDisplayName(state.getSecond());
      toolWindow.setAvailable(state.getThird(), null);
    }
  }

  @Override
  public boolean shouldBeAvailable(@Nonnull Project project) {
    ChangesViewContentManager manager = (ChangesViewContentManager)project.getInstanceIfCreated(ChangesViewContentI.class);
    if (manager != null) {
      Trinity<Image, LocalizeValue, Boolean> alreadyLoadedState = manager.getAlreadyLoadedState();
      return alreadyLoadedState != null && alreadyLoadedState.getThird();
    }
    return false;
  }
}
