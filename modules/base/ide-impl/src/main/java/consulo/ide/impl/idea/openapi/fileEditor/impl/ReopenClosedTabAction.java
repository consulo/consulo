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
package consulo.ide.impl.idea.openapi.fileEditor.impl;

import consulo.annotation.component.ActionImpl;
import consulo.dataContext.DataManager;
import consulo.fileEditor.FileEditorWindow;
import consulo.fileEditor.FileEditorsSplitters;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.UIExAWTDataKey;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;

/**
 * @author Konstantin Bulenkov
 */
@ActionImpl(id = "ReopenClosedTab")
public class ReopenClosedTabAction extends AnAction {
    public ReopenClosedTabAction() {
        super(ActionLocalize.actionReopenclosedtabText());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        FileEditorWindow window = getEditorWindow(e);
        if (window != null) {
            window.restoreClosedTab();
        }
    }

    @Nullable
    private static FileEditorWindow getEditorWindow(AnActionEvent e) {
        Component component = e.getData(UIExAWTDataKey.CONTEXT_COMPONENT);
        if (component != null) {
            FileEditorsSplitters splitters = DataManager.getInstance().getDataContext(component).getData(FileEditorsSplitters.KEY);
            if (splitters != null) {
                return splitters.getCurrentWindow();
            }
        }
        return null;
    }

    @Override
    @RequiredUIAccess
    public void update(@Nonnull AnActionEvent e) {
        FileEditorWindow window = getEditorWindow(e);
        e.getPresentation().setEnabled(window != null && window.hasClosedTabs());
    }
}
