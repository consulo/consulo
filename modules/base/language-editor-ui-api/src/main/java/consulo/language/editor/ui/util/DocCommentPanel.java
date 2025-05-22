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

/*
 * Created by IntelliJ IDEA.
 * User: dsl
 * Date: 17.06.2002
 * Time: 20:38:33
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package consulo.language.editor.ui.util;

import consulo.language.editor.ui.localize.LanguageEditorRefactoringUILocalize;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.RadioButton;
import consulo.ui.ValueGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.Layout;
import consulo.ui.layout.VerticalLayout;
import jakarta.annotation.Nonnull;

public class DocCommentPanel {
    private RadioButton myRbJavaDocAsIs = null;
    private RadioButton myRbJavaDocMove = null;
    private RadioButton myRbJavaDocCopy = null;

    private final Layout<?> myRootComponent;

    @RequiredUIAccess
    public DocCommentPanel(@Nonnull LocalizeValue titleValue) {
        VerticalLayout layout = VerticalLayout.create();

        myRbJavaDocAsIs = RadioButton.create(LanguageEditorRefactoringUILocalize.javadocAsIs());
        layout.add(myRbJavaDocAsIs);
        myRbJavaDocAsIs.setFocusable(false);

        myRbJavaDocCopy = RadioButton.create(LanguageEditorRefactoringUILocalize.javadocCopy());
        myRbJavaDocCopy.setFocusable(false);
        layout.add(myRbJavaDocCopy);

        myRbJavaDocMove = RadioButton.create(LanguageEditorRefactoringUILocalize.javadocMove());
        myRbJavaDocMove.setFocusable(false);
        layout.add(myRbJavaDocMove);

        ValueGroup<Boolean> bg = ValueGroup.createBool();
        bg.add(myRbJavaDocAsIs);
        bg.add(myRbJavaDocCopy);
        bg.add(myRbJavaDocMove);

        myRbJavaDocMove.setValue(true);

        myRootComponent = LabeledLayout.create(titleValue, layout);
    }

    @Nonnull
    public Component getComponent() {
        return myRootComponent;
    }

    @RequiredUIAccess
    public void setPolicy(final int javaDocPolicy) {
        if (javaDocPolicy == DocCommentPolicy.COPY) {
            myRbJavaDocCopy.setValue(true);
        }
        else if (javaDocPolicy == DocCommentPolicy.MOVE) {
            myRbJavaDocMove.setValue(true);
        }
        else {
            myRbJavaDocAsIs.setValue(true);
        }
    }

    @RequiredUIAccess
    public int getPolicy() {
        if (myRbJavaDocCopy != null && myRbJavaDocCopy.getValueOrError()) {
            return DocCommentPolicy.COPY;
        }
        if (myRbJavaDocMove != null && myRbJavaDocMove.getValueOrError()) {
            return DocCommentPolicy.MOVE;
        }

        return DocCommentPolicy.ASIS;
    }
}
