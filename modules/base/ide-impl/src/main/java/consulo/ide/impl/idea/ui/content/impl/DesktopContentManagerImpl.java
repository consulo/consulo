/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ui.content.impl;

import consulo.dataContext.DataSink;
import consulo.dataContext.UiDataProvider;
import consulo.ide.impl.wm.impl.ContentManagerBase;
import consulo.language.editor.PlatformDataKeys;
import consulo.project.Project;
import consulo.ui.ex.awt.NonOpaquePanel;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentUI;
import consulo.util.concurrent.AsyncResult;
import consulo.util.lang.Comparing;

import javax.swing.*;
import java.awt.*;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
@SuppressWarnings("deprecation")
public class DesktopContentManagerImpl extends ContentManagerBase {
    protected JComponent myComponent;

    public DesktopContentManagerImpl(ContentUI contentUI, boolean canCloseContents, Project project) {
        super(contentUI, canCloseContents, project);
    }

    @Override
    protected void updateUI() {
        myUI.getComponent().updateUI();
    }

    @Override
    protected AsyncResult<Void> requestFocusForComponent() {
        return getFocusManager().requestFocus(myComponent, true);
    }

    @Override
    protected boolean isSelectionHoldsFocus() {
        boolean focused = false;
        Content[] selection = getSelectedContents();
        for (Content each : selection) {
            if (UIUtil.isFocusAncestor(each.getComponent())) {
                focused = true;
                break;
            }
        }
        return focused;
    }

    @Override
    public Content getContent(JComponent component) {
        Content[] contents = getContents();
        for (Content content : contents) {
            if (Comparing.equal(component, content.getComponent())) {
                return content;
            }
        }
        return null;
    }

    @Override
    public JComponent getComponent() {
        if (myComponent == null) {
            myComponent = new MyNonOpaquePanel();

            NonOpaquePanel contentComponent = new NonOpaquePanel();
            contentComponent.setContent(myUI.getComponent());
            contentComponent.setFocusCycleRoot(true);

            myComponent.add(contentComponent, BorderLayout.CENTER);
        }
        return myComponent;
    }

    @Override
    public AsyncResult<Void> requestFocus(Content content, boolean forced) {
        Content toSelect = content == null ? getSelectedContent() : content;
        if (toSelect == null) {
            return AsyncResult.rejected();
        }
        assert myContents.contains(toSelect);
        JComponent preferredFocusableComponent = toSelect.getPreferredFocusableComponent();
        return preferredFocusableComponent != null ? getFocusManager().requestFocusInProject(preferredFocusableComponent, myProject) : AsyncResult.rejected();
    }

    private class MyNonOpaquePanel extends NonOpaquePanel implements UiDataProvider {
        public MyNonOpaquePanel() {
            super(new BorderLayout());
        }

        @Override
        public void uiDataSnapshot(DataSink sink) {
            for (UiDataProvider uiDataProvider : myDataProviders) {
                uiDataProvider.uiDataSnapshot(sink);
            }

            ContentUI ui = myUI;
            if (ui instanceof UiDataProvider uiDataProvider) {
                uiDataProvider.uiDataSnapshot(sink);
            }

            sink.set(PlatformDataKeys.CONTENT_MANAGER, DesktopContentManagerImpl.this);

            if (getContentCount() > 1) {
                sink.set(PlatformDataKeys.NONEMPTY_CONTENT_MANAGER, DesktopContentManagerImpl.this);
            }
        }
    }
}
