/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ui;

import consulo.language.editor.ui.awt.EditorComboBox;
import consulo.language.plain.PlainTextFileType;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.RecentsManager;
import consulo.ui.ex.awt.ComponentWithBrowseButton;
import consulo.ui.ex.awt.TextAccessor;
import consulo.util.collection.ArrayUtil;
import jakarta.annotation.Nonnull;

import java.awt.event.ActionListener;
import java.util.List;

/**
 * @author ven
 */
public class EditorComboWithBrowseButton extends ComponentWithBrowseButton<EditorComboBox> implements TextAccessor {
    public EditorComboWithBrowseButton(
        ActionListener browseActionListener,
        String text,
        @Nonnull Project project,
        String recentsKey
    ) {
        super(new EditorComboBox(text, project, PlainTextFileType.INSTANCE), browseActionListener);
        List<String> recentEntries = RecentsManager.getInstance(project).getRecentEntries(recentsKey);
        if (recentEntries != null) {
          setHistory(ArrayUtil.toStringArray(recentEntries));
        }
        if (text != null && text.length() > 0) {
            prependItem(text);
        }
    }

    @Override
    public String getText() {
        return getChildComponent().getText().trim();
    }

    @Override
    @RequiredUIAccess
    public void setText(String text) {
        getChildComponent().setText(text);
    }

    public boolean isEditable() {
        return !getChildComponent().getEditorEx().isViewer();
    }

    public void setHistory(String[] history) {
        getChildComponent().setHistory(history);
    }

    public void prependItem(String item) {
        getChildComponent().prependItem(item);
    }
}