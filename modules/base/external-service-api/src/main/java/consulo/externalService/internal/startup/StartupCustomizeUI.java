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
package consulo.externalService.internal.startup;

import consulo.localize.LocalizeValue;
import consulo.ui.Card;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.RadioGroup;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import org.jspecify.annotations.Nullable;

/**
 * The pieces the pages of the startup wizard are built from, so that they look alike.
 *
 * @author VISTALL
 * @since 2026-10-11
 */
public final class StartupCustomizeUI {
    private StartupCustomizeUI() {
    }

    @RequiredUIAccess
    public static Component page(LocalizeValue header, Component content) {
        DockLayout layout = DockLayout.create(Space.LARGE);
        layout.top(Label.create(header));
        layout.center(content);
        return layout;
    }

    /**
     * A card standing for one option of the group - a click anywhere on it chooses the option.
     */
    @RequiredUIAccess
    public static <V> Card choiceCard(RadioGroup<V> group, V value, LocalizeValue title, LocalizeValue subtitle, @Nullable Component media) {
        Card card = Card.create();
        card.setHeaderPrefix(group.newButton(LocalizeValue.empty(), value));
        card.setTitle(title);
        card.setSubtitle(subtitle);
        if (media != null) {
            card.setMedia(media);
        }
        card.addClickListener(event -> group.setValue(value));
        return card;
    }
}
