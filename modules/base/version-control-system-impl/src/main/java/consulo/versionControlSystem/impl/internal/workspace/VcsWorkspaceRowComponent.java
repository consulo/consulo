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
package consulo.versionControlSystem.impl.internal.workspace;

import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.AdvancedLabel;
import consulo.ui.Space;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.image.Image;
import consulo.ui.layout.DockLayout;
import consulo.util.dataholder.Key;
import consulo.versionControlSystem.workspace.VcsWorkspace;
import consulo.versionControlSystem.workspace.VcsWorkspaceBadge;

import java.util.List;

public final class VcsWorkspaceRowComponent {
    public static final Key<VcsWorkspaceRowComponent> KEY = Key.create(VcsWorkspaceRowComponent.class);

    private final DockLayout myComponent;
    private final AdvancedLabel myFirstLine;
    private final AdvancedLabel mySecondLine;

    @RequiredUIAccess
    public VcsWorkspaceRowComponent() {
        myFirstLine = AdvancedLabel.create();
        mySecondLine = AdvancedLabel.create();

        myComponent = DockLayout.create(Space.X_SMALL);
        myComponent.top(myFirstLine);
        myComponent.center(mySecondLine);
        myComponent.paddingBuilder().horizontalSet(Space.MEDIUM).verticalSet(Space.SMALL).apply();
        myComponent.putUserData(KEY, this);
    }

    public DockLayout getComponent() {
        return myComponent;
    }

    @RequiredUIAccess
    public void configure(VcsWorkspaceListEntry entry) {
        VcsWorkspace workspace = entry.workspace();
        boolean dimmed = workspace.isPending() || !workspace.isAvailable();

        Image leadingIcon;
        if (workspace.isPending()) {
            leadingIcon = Image.busy();
        }
        else if (workspace.isCurrent()) {
            leadingIcon = PlatformIconGroup.actionsChecked();
        }
        else {
            leadingIcon = Image.empty(Image.DEFAULT_ICON_SIZE);
        }

        TextAttribute nameStyle = dimmed
            ? TextAttribute.GRAYED
            : workspace.isMain() ? TextAttribute.REGULAR_BOLD : TextAttribute.REGULAR;
        TextAttribute secondaryStyle = dimmed ? TextAttribute.GRAYED : TextAttribute.GRAY;

        myFirstLine.updatePresentation(presentation -> {
            presentation.clearText();
            presentation.withIcon(leadingIcon);
            presentation.append(LocalizeValue.of(workspace.getName()), nameStyle);
            appendBadges(presentation, workspace.getNameBadges(), TextAttribute.GRAY);
        });

        mySecondLine.updatePresentation(presentation -> {
            presentation.clearText();
            presentation.withIcon(Image.empty(Image.DEFAULT_ICON_SIZE));
            Image revisionIcon = workspace.getRevisionIcon();
            if (revisionIcon != null) {
                presentation.appendIcon(revisionIcon);
                presentation.append(LocalizeValue.of(" "));
            }
            presentation.append(workspace.getRevisionText(), secondaryStyle);
            appendBadges(presentation, workspace.getRevisionBadges(), secondaryStyle);
        });

        ColorValue color = entry.multiRoot() ? entry.group().getColor() : null;
        if (color != null) {
            myComponent.borderBuilder().leftSet(color).apply();
        }
        else {
            myComponent.borderBuilder().leftReset().apply();
        }
        myComponent.setToolTipText(workspace.getTooltip());
    }

    private static void appendBadges(TextItemPresentation presentation, List<VcsWorkspaceBadge> badges, TextAttribute textStyle) {
        for (VcsWorkspaceBadge badge : badges) {
            presentation.append(LocalizeValue.of("  "));
            Image icon = badge.getIcon();
            if (icon != null) {
                presentation.appendIcon(icon);
            }
            if (badge.getText().isNotEmpty()) {
                if (icon != null) {
                    presentation.append(LocalizeValue.of(" "));
                }
                presentation.append(badge.getText(), textStyle);
            }
        }
    }
}
