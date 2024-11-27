// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.action.ui;

import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;

public class NewItemPopupUtil {

    private NewItemPopupUtil() {
    }

    public static JBPopup createNewItemPopup(@Nonnull String title,
                                             @Nonnull JComponent content,
                                             @Nullable JComponent preferableFocusComponent) {
        return JBPopupFactory.getInstance().createComponentPopupBuilder(content, preferableFocusComponent)
            .setTitle(title)
            .setResizable(false)
            .setModalContext(true)
            .setFocusable(true)
            .setRequestFocus(true)
            .setMovable(true)
            .setBelongsToGlobalPopupStack(true)
            .setCancelKeyEnabled(true)
            .setCancelOnWindowDeactivation(false)
            .setCancelOnClickOutside(true)
            .addUserData("SIMPLE_WINDOW")
            .createPopup();
    }
}
