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
package consulo.repository.ui.impl.internal;

import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.Component;
import consulo.ui.ComponentItemRenderer;
import consulo.ui.Label;
import consulo.ui.LabelStyle;
import consulo.ui.Length;
import consulo.ui.RenderItem;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.style.ComponentColors;
import consulo.util.dataholder.Key;

import java.util.Objects;

public class PackageRowRenderer {
    public static final Length ROW_HEIGHT = Length.ofFont(2).plusPixel(20);

    private static final Key<Label> NAME_KEY = Key.create("PackageRowRenderer.name");
    private static final Key<Label> VERSION_KEY = Key.create("PackageRowRenderer.version");
    private static final Key<Label> STATE_KEY = Key.create("PackageRowRenderer.state");

    public static ComponentItemRenderer<PackageRow> reusable() {
        return ComponentItemRenderer.reusable(PackageRowRenderer::createRow, PackageRowRenderer::bindRow);
    }

    @RequiredUIAccess
    private static DockLayout createRow() {
        Label nameLabel = Label.create(LocalizeValue.empty());
        nameLabel.addStyle(LabelStyle.FONT_LARGE);

        Label versionLabel = Label.create(LocalizeValue.empty());
        versionLabel.addStyle(LabelStyle.FONT_XX_SMALL);
        versionLabel.setForegroundColor(ComponentColors.DISABLED_TEXT);

        VerticalLayout text = VerticalLayout.create(Space.LARGE);
        text.add(nameLabel);
        text.add(versionLabel);
        text.paddingBuilder().horizontalSet(Space.MEDIUM).apply();

        Label stateLabel = Label.create(LocalizeValue.empty());

        DockLayout row = DockLayout.create(Space.NONE);
        row.center(text);
        row.right(stateLabel);
        row.paddingBuilder().allSet(Space.LARGE).apply();

        row.putUserData(NAME_KEY, nameLabel);
        row.putUserData(VERSION_KEY, versionLabel);
        row.putUserData(STATE_KEY, stateLabel);
        return row;
    }

    @RequiredUIAccess
    private static void bindRow(Component row, RenderItem<PackageRow> item) {
        Label nameLabel = Objects.requireNonNull(row.getUserData(NAME_KEY));
        Label versionLabel = Objects.requireNonNull(row.getUserData(VERSION_KEY));
        Label stateLabel = Objects.requireNonNull(row.getUserData(STATE_KEY));

        PackageRow value = item.getValue();
        if (value == null) {
            nameLabel.setText(LocalizeValue.empty());
            versionLabel.setText(LocalizeValue.empty());
            stateLabel.setImage(null);
            return;
        }

        nameLabel.setText(LocalizeValue.of(value.name()));

        String installedVersion = value.installedVersion();
        String latestVersion = value.latestVersion();
        String versions;
        if (installedVersion != null && value.updateAvailable() && latestVersion != null) {
            versions = installedVersion + " → " + latestVersion;
        }
        else if (installedVersion != null) {
            versions = installedVersion;
        }
        else {
            versions = latestVersion == null ? "" : latestVersion;
        }
        versionLabel.setText(LocalizeValue.of(versions));

        if (value.updateAvailable()) {
            stateLabel.setImage(PlatformIconGroup.actionsMoveup());
        }
        else if (value.installed() != null) {
            stateLabel.setImage(PlatformIconGroup.actionsChecked());
        }
        else {
            stateLabel.setImage(null);
        }
    }
}
