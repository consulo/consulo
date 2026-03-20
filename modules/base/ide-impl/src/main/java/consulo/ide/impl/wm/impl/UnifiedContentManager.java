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
package consulo.ide.impl.wm.impl;

import consulo.dataContext.DataProvider;
import consulo.dataContext.UiDataProvider;
import consulo.ide.impl.idea.ui.content.impl.DesktopContentManagerImpl;
import consulo.language.editor.PlatformDataKeys;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentUI;
import consulo.ui.layout.DockLayout;
import consulo.util.concurrent.AsyncResult;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 18-Oct-17
 */
public class UnifiedContentManager extends ContentManagerBase {
    private Component myComponent;

    public UnifiedContentManager(ContentUI contentUI, boolean canCloseContents, Project project) {
        super(contentUI, canCloseContents, project);
    }

    @Override
    protected AsyncResult<Void> requestFocusForComponent() {
        return getFocusManager().requestFocus(myComponent, true);
    }

    @Override
    protected boolean isSelectionHoldsFocus() {
        return false; //TODO [VISTALL]
    }

    @Override
    public AsyncResult<Void> requestFocus(@Nullable Content content, boolean forced) {
        return AsyncResult.resolved();  //TODO [VISTALL]
    }

    @RequiredUIAccess
    @Override
    public Component getUIComponent() {
        if (myComponent == null) {
            DockLayout dock = DockLayout.create();
            dock.putUserData(UiDataProvider.KEY, sink -> {
                for (UiDataProvider uiDataProvider : myDataProviders) {
                    uiDataProvider.uiDataSnapshot(sink);
                }

                ContentUI ui = myUI;
                if (ui instanceof UiDataProvider uiDataProvider) {
                    uiDataProvider.uiDataSnapshot(sink);
                }

                sink.set(PlatformDataKeys.CONTENT_MANAGER, UnifiedContentManager.this);

                if (getContentCount() > 1) {
                    sink.set(PlatformDataKeys.NONEMPTY_CONTENT_MANAGER, UnifiedContentManager.this);
                }
            });

            dock.center(myUI.getUIComponent());

            myComponent = dock;

        }
        return myComponent;
    }
}
