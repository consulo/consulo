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
package consulo.ai.impl.internal.chat;

import consulo.ai.AIPlanEntry;
import consulo.ai.AIPlanEntryStatus;
import consulo.ai.localize.AILocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.Component;
import consulo.ui.Hyperlink;
import consulo.ui.Label;
import consulo.ui.LabelStyle;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.LayoutStyle;
import consulo.ui.layout.VerticalLayout;

import java.util.List;

/**
 * The plan the agent works through, over the message box: how far it is, and the steps, which fold away.
 *
 * @author VISTALL
 * @since 2026-11-13
 */
final class AIChatPlanPanel {
    private final VerticalLayout myRoot = VerticalLayout.create(Space.X_SMALL);
    private final Label myTitle = Label.create();
    private Hyperlink myToggle;
    private final VerticalLayout myEntries = VerticalLayout.create(Space.X_SMALL);
    private boolean myExpanded = true;

    @RequiredUIAccess
    AIChatPlanPanel() {
        myTitle.addStyle(LabelStyle.FONT_BOLD);
        myToggle = Hyperlink.create(AILocalize.linkHide(), event -> {
            myExpanded = !myExpanded;
            myToggle.setText(myExpanded ? AILocalize.linkHide() : AILocalize.linkShow());
            myEntries.setVisible(myExpanded);
        });
        myRoot.addStyle(LayoutStyle.TRANSPARENT_BACKGROUND);
        myEntries.addStyle(LayoutStyle.TRANSPARENT_BACKGROUND);
        DockLayout header = DockLayout.create().left(myTitle).right(myToggle);
        header.addStyle(LayoutStyle.TRANSPARENT_BACKGROUND);
        myRoot.add(header);
        myRoot.add(myEntries);
        myRoot.paddingBuilder().topSet(Space.SMALL).bottomSet(Space.SMALL).apply();
        myRoot.setVisible(false);
    }

    Component getComponent() {
        return myRoot;
    }

    @RequiredUIAccess
    void update(List<AIPlanEntry> entries) {
        myEntries.removeAll();
        int done = 0;
        for (AIPlanEntry entry : entries) {
            if (entry.status() == AIPlanEntryStatus.COMPLETED) {
                done++;
            }
            Label label = Label.create(LocalizeValue.of(entry.content()));
            label.setImage(image(entry.status()));
            label.addStyle(LabelStyle.FONT_SMALL);
            myEntries.add(label);
        }
        myTitle.setText(AILocalize.labelPlan(done, entries.size()));
        myRoot.setVisible(!entries.isEmpty());
    }

    private static Image image(AIPlanEntryStatus status) {
        return switch (status) {
            case PENDING -> PlatformIconGroup.generalTododefault();
            case IN_PROGRESS -> Image.busy();
            case COMPLETED -> PlatformIconGroup.runconfigurationsTestpassed();
        };
    }
}
