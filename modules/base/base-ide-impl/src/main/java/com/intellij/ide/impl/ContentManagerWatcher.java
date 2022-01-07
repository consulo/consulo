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
package com.intellij.ide.impl;

import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.content.ContentManagerEvent;
import com.intellij.ui.content.ContentManagerListener;
import consulo.annotation.DeprecationInfo;
import consulo.ui.annotation.RequiredUIAccess;
import kava.beans.PropertyChangeListener;

import javax.annotation.Nonnull;

public class ContentManagerWatcher {
  @RequiredUIAccess
  public static void watchContentManager(@Nonnull ToolWindow toolWindow, @Nonnull ContentManager contentManager) {
    toolWindow.setAvailable(contentManager.getContentCount() > 0);

    contentManager.addContentManagerListener(new ContentManagerListener() {
      @RequiredUIAccess
      @Override
      public void contentAdded(@Nonnull ContentManagerEvent e) {
        toolWindow.setAvailable(true);
      }

      @RequiredUIAccess
      @Override
      public void contentRemoved(@Nonnull ContentManagerEvent e) {
        toolWindow.setAvailable(contentManager.getContentCount() > 0);
      }
    });
  }

  @Deprecated
  @DeprecationInfo("Use #watchContentManager ")
  public ContentManagerWatcher(ToolWindow toolWindow, ContentManager contentManager) {
    watchContentManager(toolWindow, contentManager);
  }
}