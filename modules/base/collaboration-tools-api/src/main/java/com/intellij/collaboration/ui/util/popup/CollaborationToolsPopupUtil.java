// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.util.popup;

import com.intellij.collaboration.ui.codereview.list.search.PopupConfig;
import com.intellij.collaboration.ui.codereview.list.search.ShowDirection;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.awt.SearchTextField;
import consulo.ui.ex.awt.TextComponentEmptyText;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.event.JBPopupListener;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;

final class CollaborationToolsPopupUtil {
    private CollaborationToolsPopupUtil() {
    }

    static void configureSearchField(@Nonnull JBPopup popup, @Nonnull PopupConfig popupConfig) {
        SearchTextField searchTextField = UIUtil.findComponentOfType(popup.getContent(), SearchTextField.class);
        if (searchTextField != null) {
            tuneSearchFieldForNewUI(searchTextField);
            setSearchFieldPlaceholder(searchTextField, popupConfig.getSearchTextPlaceHolder());
        }
    }

    private static void tuneSearchFieldForNewUI(@Nonnull SearchTextField searchTextField) {
        if (!ExperimentalUI.isNewUI()) {
            return;
        }
        AbstractPopup.customizeSearchFieldLook(searchTextField, true);
    }

    private static void setSearchFieldPlaceholder(
        @Nonnull SearchTextField searchTextField,
        @Nullable @NlsContexts.StatusText String placeholderText
    ) {
        if (placeholderText == null) {
            return;
        }
        searchTextField.getTextEditor().getEmptyText().setText(placeholderText);
        TextComponentEmptyText.setupPlaceholderVisibility(searchTextField.getTextEditor());
    }

    static void showPopup(@Nonnull JBPopup popup, @Nonnull RelativePoint relativePoint, @Nonnull ShowDirection showDirection) {
        popup.addListener(new JBPopupListener() {
            @Override
            public void beforeShown(@Nonnull LightweightWindowEvent event) {
                Point location = new Point(popup.getLocationOnScreen());
                if (showDirection == ShowDirection.ABOVE) {
                    location.y = relativePoint.getScreenPoint().y - popup.getSize().height;
                }
                popup.setLocation(location);
                popup.removeListener(this);
            }
        });
        popup.show(relativePoint);
    }
}
