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
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Records what a renderer appends, without drawing anything - the title of a tab, so that it answers the same on every
 * frontend and for a tab which was never shown, or the caption of a list separator.
 *
 * @author VISTALL
 * @since 2026-10-10
 */
public final class PlainTextPresentation implements TextItemPresentation {
    private final List<LocalizeValue> myTexts = new ArrayList<>();

    public static LocalizeValue textOf(Consumer<TextItemPresentation> renderer) {
        PlainTextPresentation presentation = new PlainTextPresentation();
        renderer.accept(presentation);
        return presentation.getText();
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

    public LocalizeValue getText() {
        return switch (myTexts.size()) {
            case 0 -> LocalizeValue.empty();
            case 1 -> myTexts.getFirst();
            default -> LocalizeValue.join(myTexts.toArray(LocalizeValue[]::new));
        };
    }
}
