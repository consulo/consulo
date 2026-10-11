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
package consulo.ui.internal;

import consulo.localize.LocalizeValue;
import consulo.ui.Tab;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Records what a tab renderer appends, without drawing anything, so that {@link Tab#getTitle()} answers the same on
 * every frontend and for a tab which was never shown.
 *
 * @author VISTALL
 * @since 2026-10-10
 */
public final class TabTitlePresentation implements TextItemPresentation {
    private final List<LocalizeValue> myTexts = new ArrayList<>();

    public static LocalizeValue titleOf(Tab tab, BiConsumer<Tab, TextItemPresentation> renderer) {
        TabTitlePresentation presentation = new TabTitlePresentation();
        renderer.accept(tab, presentation);
        return presentation.getTitle();
    }

    @Override
    public void clearText() {
        myTexts.clear();
    }

    @Override
    public void append(LocalizeValue text, TextAttribute textAttribute) {
        if (text != LocalizeValue.empty()) {
            myTexts.add(text);
        }
    }

    public LocalizeValue getTitle() {
        return switch (myTexts.size()) {
            case 0 -> LocalizeValue.empty();
            case 1 -> myTexts.getFirst();
            default -> LocalizeValue.join(myTexts.toArray(LocalizeValue[]::new));
        };
    }
}
