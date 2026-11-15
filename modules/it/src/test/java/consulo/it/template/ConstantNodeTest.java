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
package consulo.it.template;

import consulo.it.HeadlessApplicationExtension;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.editor.template.ConstantNode;
import consulo.language.editor.template.ExpressionContext;
import consulo.language.editor.template.Result;
import consulo.language.editor.template.TextResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lookup items and the advertisement of a {@link ConstantNode}.
 *
 * @author VISTALL
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class ConstantNodeTest {
    private static final ExpressionContext CONTEXT = null;

    @Test
    public void plainNodeHasNoLookupItems() {
        ConstantNode node = new ConstantNode("value");

        assertThat(node.calculateLookupItems(CONTEXT)).isEmpty();
        assertThat(resultText(node.calculateResult(CONTEXT))).isEqualTo("value");
        assertThat(node.requiresCommittedPSI()).isFalse();
        assertThat(node.getAdvertisingText()).isNull();
    }

    @Test
    public void lookupStrings() {
        ConstantNode node = new ConstantNode("a").withLookupStrings(List.of("a", "b", "c"));

        assertThat(lookupStrings(node)).containsExactly("a", "b", "c");
        assertThat(resultText(node.calculateResult(CONTEXT))).isEqualTo("a");
        assertThat(lookupStrings(new ConstantNode("x").withLookupStrings("x", "y"))).containsExactly("x", "y");
    }

    @Test
    public void lookupItemsAndAdvertisementAreKept() {
        ConstantNode node = new ConstantNode("a")
            .withPopupAdvertisement("press Tab")
            .withLookupItems(LookupElementBuilder.create("first"), LookupElementBuilder.create("second"));

        assertThat(lookupStrings(node)).containsExactly("first", "second");
        assertThat(node.getAdvertisingText()).isEqualTo("press Tab");
        assertThat(lookupStrings(node.withLookupItems(List.of(LookupElementBuilder.create("third"))))).containsExactly("third");
    }

    private static List<String> lookupStrings(ConstantNode node) {
        return Arrays.stream(node.calculateLookupItems(CONTEXT)).map(LookupElement::getLookupString).toList();
    }

    private static String resultText(Result result) {
        return ((TextResult) result).getText();
    }
}
