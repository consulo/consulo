/*
 * Copyright 2013-2026 consulo.io
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
package consulo.it.internal.ui;

import consulo.it.HeadlessAnswers;
import consulo.it.internal.HeadlessUIAccess;
import consulo.ui.Component;
import consulo.ui.MenuBar;
import consulo.ui.UIAccess;
import consulo.ui.Window;
import consulo.ui.ex.dialog.Dialog;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Dummy-but-creatable headless {@link Window}.
 *
 * @author VISTALL
 */
public class HeadlessWindow extends HeadlessComponentBase implements Window {
    private String myTitle;
    private @Nullable Component myContent;

    public HeadlessWindow(String title) {
        myTitle = title;
    }

    @Override
    public void setTitle(String title) {
        myTitle = title;
    }

    @Override
    public @Nullable Window getParent() {
        return null;
    }

    @Override
    public void setContent(Component content) {
        myContent = content;
    }

    public @Nullable Component getContent() {
        return myContent;
    }

    @Override
    public void setMenuBar(@Nullable MenuBar menuBar) {
    }

    @Override
    public void show() {
        Dialog dialog = getUserData(Dialog.KEY);
        if (dialog == null) {
            return;
        }
        HeadlessAnswers answers = HeadlessUIAccess.INSTANCE.getAnswers();
        if (answers != null) {
            UIAccess.current().give(() -> answers.dialogShown(dialog));
        }
        else {
            UIAccess.current().give(() -> HeadlessAnswers.dialogWithoutAnswers(dialog));
        }
    }

    @Override
    public void close() {
    }

    @Override
    public void setCloseHandler(@Nullable Consumer<Window> closeHandler) {
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public void dispose() {
    }
}
