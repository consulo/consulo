/*
 * Copyright 2013-2017 consulo.io
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
package consulo.web.wm.impl;

import com.intellij.openapi.util.ActionCallback;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import consulo.wm.impl.ToolWindowBase;
import consulo.wm.impl.ToolWindowManagerBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 25-Sep-17
 */
public class WebToolWindowImpl extends ToolWindowBase {

  public WebToolWindowImpl(ToolWindowManagerBase toolWindowManager, String id, boolean canCloseContent, @Nullable Object component) {
    super(toolWindowManager, id, canCloseContent, component);
  }

  @Override
  public ActionCallback getReady(@NotNull Object requestor) {
    return ActionCallback.DONE;
  }

  @Override
  protected void init(boolean canCloseContent, @Nullable Object component) {
    final ContentFactory contentFactory = ContentFactory.getInstance();
    ContentManager contentManager = myContentManager = contentFactory.createContentManager(new WebToolWindowContentUI(), canCloseContent, myToolWindowManager.getProject());

    if (component != null) {
      final Content content = contentFactory.createUIContent(null, "", false);
      contentManager.addContent(content);
      contentManager.setSelectedContent(content, false);
    }

    // myComponent = contentManager.getComponent();

  }

  @Override
  public void stretchWidth(int value) {

  }

  @Override
  public void stretchHeight(int value) {

  }
}
