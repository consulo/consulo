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
package consulo.ide.impl.wm.impl.status;

import consulo.ide.impl.idea.openapi.wm.impl.status.InlineProgressIndicator;
import consulo.ide.localize.IdeLocalize;
import consulo.ui.Component;
import consulo.ui.LightPopup;
import consulo.ui.PopupOptions;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.VerticalLayout;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The list of what is running, hung off the progress panel of the status bar. Unified counterpart of the awt
 * {@code ProcessPopup}.
 *
 * @author VISTALL
 * @since 2026-08-16
 */
public class UnifiedProcessPopup {
    private final List<InlineProgressIndicator> myIndicators = new ArrayList<>();

    private @Nullable VerticalLayout myContent;
    private @Nullable LightPopup myPopup;

    @RequiredUIAccess
    public void addIndicator(InlineProgressIndicator indicator) {
        myIndicators.add(indicator);

        VerticalLayout content = myContent;
        if (content != null) {
            content.add(indicator.getUIComponent());
        }
    }

    @RequiredUIAccess
    public void removeIndicator(InlineProgressIndicator indicator) {
        myIndicators.remove(indicator);

        VerticalLayout content = myContent;
        if (content != null) {
            content.remove(indicator.getUIComponent());
        }

        if (myIndicators.isEmpty()) {
            hide();
        }
    }

    public boolean isShowing() {
        LightPopup popup = myPopup;
        return popup != null && popup.isVisible();
    }

    /**
     * The content is rebuilt on every show - an indicator which was taken out while the popup was down has no
     * component left to remove, and one added in the meantime has never been placed.
     */
    @RequiredUIAccess
    public void show(Component target) {
        if (isShowing()) {
            return;
        }

        VerticalLayout content = VerticalLayout.create();
        content.paddingBuilder().allSet(Space.MEDIUM).apply();
        for (InlineProgressIndicator indicator : myIndicators) {
            content.add(indicator.getUIComponent());
        }
        myContent = content;

        LightPopup popup = LightPopup.create(PopupOptions.builder().build());
        popup.setTitle(IdeLocalize.progressWindowTitle().get());
        popup.setContent(ScrollableLayout.create(content));
        popup.addCloseListener(event -> {
            myPopup = null;
            myContent = null;
        });

        myPopup = popup;

        popup.showBy(target);
    }

    @RequiredUIAccess
    public void hide() {
        LightPopup popup = myPopup;
        if (popup != null) {
            myPopup = null;
            myContent = null;
            popup.close();
        }
    }
}
