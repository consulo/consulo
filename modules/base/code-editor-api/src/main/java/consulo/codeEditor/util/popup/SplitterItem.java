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
package consulo.codeEditor.util.popup;

import consulo.project.Project;
import consulo.ui.ex.ColoredTextContainer;

import javax.swing.*;

/**
 * @author zajac
 * @since 2012-05-11
 */
public class SplitterItem extends ItemWrapper {

    private final String myText;

    public SplitterItem(String text) {
        myText = text;
    }

    public String getText() {
        return myText;
    }

    @Override
    public void setupRenderer(ColoredTextContainer renderer, Project project, boolean selected) {
    }

    @Override
    public void updateAccessoryView(JComponent label) {
    }

    @Override
    public String speedSearchText() {
        return "";
    }

    @Override
    public String footerText() {
        return null;
    }

    @Override
    protected void doUpdateDetailView(DetailView panel, boolean editorOnly) {
    }

    @Override
    public boolean allowedToRemove() {
        return false;
    }

    @Override
    public void removed(Project project) {
    }
}
