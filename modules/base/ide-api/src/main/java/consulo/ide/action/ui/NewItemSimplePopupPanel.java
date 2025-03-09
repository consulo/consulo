// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.action.ui;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.IdeBundle;
import consulo.ide.localize.IdeLocalize;
import consulo.ui.TextBoxWithExtensions;
import consulo.ui.ValidableComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.event.details.KeyCode;
import consulo.ui.event.details.KeyboardInputDetails;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.awt.JBPanel;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.layout.WrappedLayout;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

import java.awt.*;
import java.awt.event.InputEvent;
import java.util.function.Consumer;

public class NewItemSimplePopupPanel extends JBPanel implements Disposable {
    protected final TextBoxWithExtensions myTextField;

    protected Consumer<? super InputEvent> myApplyAction;

    @RequiredUIAccess
    public NewItemSimplePopupPanel() {
        super(new BorderLayout());

        myTextField = createTextField();

        WrappedLayout layout = WrappedLayout.create(myTextField);
        layout.addBorder(BorderPosition.TOP, BorderStyle.LINE);

        add(TargetAWT.to(layout), BorderLayout.NORTH);
    }

    public void addValidator(@Nonnull ValidableComponent.Validator<String> validator) {
        myTextField.addValidator(value -> validator.validateValue(StringUtil.notNullize(value).trim()));
    }

    public void setApplyAction(@Nonnull Consumer<? super InputEvent> applyAction) {
        myApplyAction = applyAction;
    }

    @Override
    public void dispose() {
    }

    public TextBoxWithExtensions getTextField() {
        return myTextField;
    }

    @Nonnull
    protected TextBoxWithExtensions createTextField() {
        TextBoxWithExtensions res = TextBoxWithExtensions.create();

        res.setVisibleLength(30);

        res.addBorders(BorderStyle.EMPTY, null, 4);

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
