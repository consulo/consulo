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
package consulo.wm.impl;

import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentUI;
import consulo.ui.Component;
import consulo.ui.layout.DockLayout;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;


/**
 * @author VISTALL
 * @since 18-Oct-17
 */
public class UnifiedContentManager extends ContentManagerBase {
  private Component myComponent;

  public UnifiedContentManager(@Nonnull ContentUI contentUI, boolean canCloseContents, @Nonnull Project project) {
    super(contentUI, canCloseContents, project);
  }

  @Nonnull
  @Override
  protected AsyncResult<Void> requestFocusForComponent() {
    return getFocusManager().requestFocus(myComponent, true);
  }

  @Override
  protected boolean isSelectionHoldsFocus() {
    return false; //TODO [VISTALL]
  }

  @Nonnull
  @Override
  public AsyncResult<Void> requestFocus(@Nullable Content content, boolean forced) {
    return AsyncResult.resolved();  //TODO [VISTALL]
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public Component getUIComponent() {
    if (myComponent == null) {
      DockLayout dock = DockLayout.create();
      dock.addUserDataProvider(dataId -> {
        if (PlatformDataKeys.CONTENT_MANAGER == dataId || PlatformDataKeys.NONEMPTY_CONTENT_MANAGER == dataId && getContentCount() > 1) {
          return this;
        }

        for (DataProvider dataProvider : myDataProviders) {
          Object data = dataProvider.getData(dataId);
          if (data != null) {
            return data;
          }
        }

        if (myUI instanceof DataProvider) {
          return ((DataProvider)myUI).getData(dataId);
        }

        return null;
      });
      dock.center(myUI.getUIComponent());

      myComponent = dock;

    }
    return myComponent;
  }
}
