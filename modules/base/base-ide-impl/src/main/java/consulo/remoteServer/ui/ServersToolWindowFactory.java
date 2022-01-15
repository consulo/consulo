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
package consulo.remoteServer.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.remoteServer.impl.runtime.ui.ServersToolWindowContent;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2020-05-30
 */
public class ServersToolWindowFactory implements ToolWindowFactory {
  @RequiredUIAccess
  @Override
  public void createToolWindowContent(@Nonnull Project project, @Nonnull ToolWindow toolWindow) {
    ContentManager contentManager = toolWindow.getContentManager();

    final ServersToolWindowContent serversContent = new ServersToolWindowContent(project);

    Content content = contentManager.getFactory().createContent(serversContent, null, false);
    content.setDisposer(serversContent);
    
    contentManager.addContent(content);
  }

  @Override
  public boolean shouldBeAvailable(@Nonnull Project project) {
    return ServersToolWindowManager.getInstance(project).isAvailable();
  }
}
