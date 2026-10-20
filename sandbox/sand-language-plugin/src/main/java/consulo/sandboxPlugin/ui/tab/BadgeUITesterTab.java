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
package consulo.sandboxPlugin.ui.tab;

import consulo.annotation.component.ExtensionImpl;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.Badge;
import consulo.ui.BadgeStyle;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.tester.UITesterTab;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.VerticalLayout;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-20
 */
@ExtensionImpl(id = "badge", order = "after avatar")
public class BadgeUITesterTab implements UITesterTab {
    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Badge");
    }

    @RequiredUIAccess
    @Override
    public Component createComponent(Disposable uiDisposable) {
        VerticalLayout root = VerticalLayout.create(Space.LARGE);
        root.add(row(LocalizeValue.localizeTODO("Default"), null));
        root.add(row(LocalizeValue.localizeTODO("Small"), BadgeStyle.SMALL));
        root.add(row(LocalizeValue.localizeTODO("Filled"), BadgeStyle.FILLED));

        HorizontalLayout icons = HorizontalLayout.create(Space.MEDIUM);
        icons.add(Label.create(LocalizeValue.localizeTODO("With icon")));
        Badge iconBadge = Badge.create(LocalizeValue.localizeTODO("Passed"));
        iconBadge.addStyle(BadgeStyle.SUCCESS);
        iconBadge.setIcon(PlatformIconGroup.statusSuccess());
        icons.add(iconBadge);
        root.add(icons);
        return root;
    }

    @RequiredUIAccess
    private static Component row(LocalizeValue title, @Nullable BadgeStyle modifier) {
        HorizontalLayout row = HorizontalLayout.create(Space.MEDIUM);
        row.add(Label.create(title));
        row.add(badge("Primary", null, modifier));
        row.add(badge("Success", BadgeStyle.SUCCESS, modifier));
        row.add(badge("Warning", BadgeStyle.WARNING, modifier));
        row.add(badge("Error", BadgeStyle.ERROR, modifier));
        row.add(badge("Contrast", BadgeStyle.CONTRAST, modifier));
        return row;
    }

    @RequiredUIAccess
    private static Badge badge(String text, @Nullable BadgeStyle tone, @Nullable BadgeStyle modifier) {
        Badge badge = Badge.create(LocalizeValue.of(text));
        if (tone != null) {
            badge.addStyle(tone);
        }
        if (modifier != null) {
            badge.addStyle(modifier);
        }
        return badge;
    }
}
