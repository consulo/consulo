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
package consulo.language.editor.ui.util;

import consulo.ui.RadioGroup;
import consulo.language.editor.ui.localize.LanguageEditorRefactoringUILocalize;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.RadioButton;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.Layout;
import consulo.ui.layout.VerticalLayout;

/**
 * @author dsl
 * @since 2002-06-17
 */
public class DocCommentPanel {
    private final RadioGroup<Integer> myPolicy = RadioGroup.create();

    private final Layout<?> myRootComponent;

    @RequiredUIAccess
    public DocCommentPanel(LocalizeValue titleValue) {
        VerticalLayout layout = VerticalLayout.create();

        addButton(layout, LanguageEditorRefactoringUILocalize.javadocAsIs(), DocCommentPolicy.ASIS);
        addButton(layout, LanguageEditorRefactoringUILocalize.javadocCopy(), DocCommentPolicy.COPY);
        addButton(layout, LanguageEditorRefactoringUILocalize.javadocMove(), DocCommentPolicy.MOVE);

        myPolicy.setValue(DocCommentPolicy.MOVE);

        myRootComponent = LabeledLayout.create(titleValue, layout);
    }

    @RequiredUIAccess
    private void addButton(VerticalLayout layout, LocalizeValue text, int policy) {
        RadioButton button = myPolicy.newButton(text, policy);
        button.setFocusable(false);
        layout.add(button);
    }

    public Component getComponent() {
        return myRootComponent;
    }

    @RequiredUIAccess
    public void setPolicy(int javaDocPolicy) {
        myPolicy.setValue(javaDocPolicy);
    }

    @RequiredUIAccess
    public int getPolicy() {
        return myPolicy.getValueOrError();
    }
}
