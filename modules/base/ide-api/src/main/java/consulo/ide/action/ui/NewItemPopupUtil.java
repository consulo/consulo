// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.action.ui;

import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.ex.popup.ComponentPopupBuilder;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import org.jspecify.annotations.Nullable;

import javax.swing.*;

public class NewItemPopupUtil {

    private NewItemPopupUtil() {
    }

    public static JBPopup createNewItemPopup(LocalizeValue title,
                                             Component content,
                                             @Nullable Component preferableFocusComponent) {
        return configure(
            JBPopupFactory.getInstance().createComponentPopupBuilder(content, preferableFocusComponent),
            title
        );
    }

    private static JBPopup configure(ComponentPopupBuilder builder, LocalizeValue title) {
        return builder
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
