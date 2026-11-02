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
package consulo.project.ui.impl.internal.wm.statusBar;

import consulo.ide.localize.IdeLocalize;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.LightPopup;
import consulo.ui.PopupOptions;
import consulo.ui.PopupPosition;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.LayoutStyle;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.ScrollableLayoutOptions;
import consulo.ui.layout.VerticalLayout;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The list of what is running, hung over the progress part of the status bar - the banners of the project on top, then
 * the newest task, a few of them on screen and the rest a scroll away.
 *
 * @author VISTALL
 * @since 2026-08-16
 */
public class ProcessBalloon {
    static final int MAX_VISIBLE = 4;

    private static final long REOPEN_DELAY_MS = 300;

    private static final int WIDTH = 360;

    private final List<InlineProgressIndicator> myIndicators = new ArrayList<>();
    private List<Component> myBanners = List.of();

    private @Nullable VerticalLayout myBannerArea;
    private @Nullable VerticalLayout myContent;
    private @Nullable LightPopup myPopup;
    private long myClosedAt;

    @RequiredUIAccess
    public void addIndicator(InlineProgressIndicator indicator) {
        myIndicators.add(0, indicator);

        rebuild();
    }

    @RequiredUIAccess
    public void removeIndicator(InlineProgressIndicator indicator) {
        if (!myIndicators.remove(indicator)) {
            return;
        }

        if (myIndicators.isEmpty()) {
            hide();
            return;
        }

        rebuild();
    }

    /**
     * What the project wants said over its running tasks - indexing, for one.
     */
    @RequiredUIAccess
    public void setBanners(List<Component> banners) {
        myBanners = List.copyOf(banners);

        rebuildBanners();
    }

    public boolean isShowing() {
        LightPopup popup = myPopup;
        return popup != null && popup.isVisible();
    }

    /**
     * @param explicit {@code false} for a balloon raised by a task starting - it stays up while the user goes on
     *                 working elsewhere, until it is closed or the tasks are over
     * @param focus    whether the balloon takes the focus - only when the user asked for it
     */
    @RequiredUIAccess
    public void show(Component target, boolean explicit, boolean focus) {
        if (isShowing()) {
            return;
        }

        VerticalLayout bannerArea = VerticalLayout.create(Space.SMALL);
        VerticalLayout content = VerticalLayout.create(Space.NONE);
        myBannerArea = bannerArea;
        myContent = content;
        rebuildBanners();
        rebuild();

        ScrollableLayout scrollable = ScrollableLayout.create(
            content,
            ScrollableLayoutOptions.builder().horizontalScrollPolicy(ScrollableLayoutOptions.ScrollPolicy.NEVER).build()
        );
        scrollable.setVisibleRowCount(MAX_VISIBLE);

        DockLayout root = DockLayout.create();
        root.addStyle(LayoutStyle.TRANSPARENT_BACKGROUND);
        root.top(bannerArea);
        root.center(scrollable);

        PopupOptions.Builder options = PopupOptions.builder().position(PopupPosition.TOP).arrow();
        if (!explicit) {
            options.disableCancelOnClickOutside();
        }
        if (!focus) {
            options.disableRequestFocus();
        }

        LightPopup popup = LightPopup.create(options.build());
        popup.setContent(root);
        popup.setMinimumWidth(WIDTH);
        popup.addCloseListener(event -> {
            myClosedAt = System.currentTimeMillis();
            if (myPopup == popup) {
                myPopup = null;
                myContent = null;
                myBannerArea = null;
            }
        });

        myPopup = popup;

        popup.showBy(target);
    }

    public boolean isJustClosed() {
        return System.currentTimeMillis() - myClosedAt < REOPEN_DELAY_MS;
    }

    @RequiredUIAccess
    public void hide() {
        LightPopup popup = myPopup;
        if (popup != null) {
            myPopup = null;
            myContent = null;
            myBannerArea = null;
            popup.close();
        }
    }

    @RequiredUIAccess
    private void rebuildBanners() {
        VerticalLayout bannerArea = myBannerArea;
        if (bannerArea == null) {
            return;
        }

        bannerArea.removeAll();
        for (Component banner : myBanners) {
            bannerArea.add(banner);
        }

        if (myBanners.isEmpty()) {
            bannerArea.paddingBuilder().allReset().apply();
            bannerArea.borderBuilder().bottomReset().apply();
        }
        else {
            bannerArea.paddingBuilder().allSet(Space.MEDIUM).apply();
            bannerArea.borderBuilder().bottomSet().apply();
        }

        LightPopup popup = myPopup;
        if (popup != null) {
            popup.pack();
        }
    }

    @RequiredUIAccess
    private void rebuild() {
        VerticalLayout content = myContent;
        if (content == null) {
            return;
        }

        content.removeAll();

        if (myIndicators.isEmpty()) {
            Label empty = Label.create(IdeLocalize.progressWindowEmptyText());
            empty.paddingBuilder().allSet(Space.MEDIUM).apply();
            content.add(empty);
        }

        for (int i = 0; i < myIndicators.size(); i++) {
            Component card = myIndicators.get(i).getUIComponent();
            if (i == 0) {
                card.borderBuilder().topReset().apply();
            }
            else {
                card.borderBuilder().topSet().apply();
            }
            content.add(card);
        }

        LightPopup popup = myPopup;
        if (popup != null) {
            popup.pack();
        }
    }
}
