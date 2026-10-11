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
package consulo.web.ui.impl.internal;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.dom.Element;
import consulo.ui.internal.SearchHighlightUtil;
import consulo.ui.impl.style.StyleColorKeys;
import consulo.ui.style.ComponentColors;
import consulo.ui.style.StyleColorValue;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * The search highlight of a browser text: the matched runs are {@code mark} elements between text nodes, so the text
 * is never parsed as markup and a mnemonic marker stripped before still stays out of it. A text which is markup has its
 * shown text marked and its tags left alone.
 *
 * @author VISTALL
 * @since 2026-10-10
 */
public final class WebSearchHighlight {
    private static final String BACKGROUND = cssVariable(ComponentColors.SEARCH_MATCH_BACKGROUND);
    private static final String FOREGROUND = cssVariable(ComponentColors.SEARCH_MATCH_FOREGROUND);
    private static final String RADIUS = "2px";

    private static final String OPEN_TAG =
        "<mark style=\"background-color:" + BACKGROUND + ";color:" + FOREGROUND + ";border-radius:" + RADIUS + "\">";
    private static final String CLOSE_TAG = "</mark>";

    private static final String TEXT_CLASS = "consulo-search-highlight-text";

    private WebSearchHighlight() {
    }

    private static String cssVariable(StyleColorValue colorValue) {
        return "var(" + WebStyleCssRegistry.toVariableName(Objects.requireNonNull(StyleColorKeys.getKey(colorValue))) + ")";
    }

    public static void appendText(Element parent, String text, @Nullable String query) {
        SearchHighlightUtil.forEachFragment(text, query, (fragment, matched) -> {
            if (matched) {
                Element mark = new Element("mark");
                mark.getStyle().set("background-color", BACKGROUND).set("color", FOREGROUND).set("border-radius", RADIUS);
                mark.setText(fragment);
                parent.appendChild(mark);
            }
            else {
                parent.appendChild(Element.createText(fragment));
            }
        });
    }

    public static void setText(Element parent, String text, @Nullable String query) {
        parent.removeAllChildren();
        appendText(parent, text, query);
    }

    public static String markupHtml(String html, @Nullable String query) {
        return SearchHighlightUtil.markupHtml(html, query, OPEN_TAG, CLOSE_TAG);
    }

    /**
     * The text of a button keeps its own node, which the button rebuilds around its icon, so a marked text sits in a
     * span of its own next to it while the node is left empty.
     */
    public static void setButtonText(Button button, String text, @Nullable String query) {
        Element element = button.getElement();
        element.getChildren()
            .filter(child -> !child.isTextNode() && child.getClassList().contains(TEXT_CLASS))
            .toList()
            .forEach(Element::removeFromParent);

        if (SearchHighlightUtil.matches(text, query)) {
            Element span = new Element("span");
            span.getClassList().add(TEXT_CLASS);
            appendText(span, text, query);
            element.appendChild(span);

            button.setText("");
        }
        else {
            button.setText(text);
        }
    }
}
