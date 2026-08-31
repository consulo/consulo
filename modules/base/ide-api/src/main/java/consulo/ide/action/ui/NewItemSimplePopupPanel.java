// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.action.ui;

import consulo.disposer.Disposable;
import consulo.ide.localize.IdeLocalize;
import consulo.ui.Component;
import consulo.ui.HasValidator;
import consulo.ui.PseudoComponent;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.event.details.KeyCode;
import consulo.ui.event.details.KeyboardInputDetails;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.WrappedLayout;
import consulo.util.lang.StringUtil;

import java.awt.event.InputEvent;
import java.util.function.Consumer;

public class NewItemSimplePopupPanel implements PseudoComponent, Disposable {
    protected final TextBox myTextField;
    protected final DockLayout myRootLayout;

    protected Consumer<? super InputEvent> myApplyAction;

    @RequiredUIAccess
    public NewItemSimplePopupPanel() {
        myTextField = createTextField();

        WrappedLayout textFieldLayout = WrappedLayout.create(myTextField);
        textFieldLayout.addBorder(BorderPosition.TOP, BorderStyle.LINE);
        textFieldLayout.addBorder(BorderPosition.BOTTOM, BorderStyle.LINE);

        myRootLayout = DockLayout.create();
        myRootLayout.top(textFieldLayout);
    }

    @Override
    @RequiredUIAccess
    public Component getComponent() {
        return myRootLayout;
    }

    public void addValidator(HasValidator.Validator<String> validator) {
        myTextField.addValidator(value -> validator.validateValue(StringUtil.notNullize(value).trim()));
    }

    public void setApplyAction(Consumer<? super InputEvent> applyAction) {
        myApplyAction = applyAction;
    }

    @Override
    public void dispose() {
    }

    public TextBox getTextField() {
        return myTextField;
    }

    protected TextBox createTextField() {
        TextBox res = TextBox.create();

        res.setVisibleLength(30);

        res.addBorders(BorderStyle.EMPTY, null, 6);

        res.setPlaceholder(IdeLocalize.actionCreateNewClassNameField());
        res.addKeyPressedListener(e -> {
            KeyboardInputDetails details = e.getInputDetails();
            KeyCode keyCode = details.getKeyCode();

            if (KeyCode.ENTER.equals(keyCode)) {
                if (!myTextField.validate()) {
                    return;
                }

                if (myApplyAction != null) {
                    myApplyAction.accept(null); // todo null ??
                }
            }
        });
        return res;
    }
}
