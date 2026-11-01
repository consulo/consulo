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
package consulo.language.spellchecker.editor.impl.text;

import consulo.annotation.component.ExtensionImpl;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.plain.PlainTextLanguage;
import consulo.language.plain.psi.PsiPlainText;
import consulo.language.psi.PsiElement;
import consulo.language.spellcheker.text.TextContent;
import consulo.language.spellcheker.text.TextContent.TextDomain;
import consulo.language.spellcheker.text.TextExtractor;
import consulo.language.spellcheker.text.util.Text;
import consulo.util.collection.ContainerUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static consulo.language.spellcheker.text.TextContent.TextDomain.PLAIN_TEXT;

@ExtensionImpl
public class PlainTextExtractor extends TextExtractor {
    private static final Pattern paragraphEnd = Pattern.compile("\\n\\s*?\\n\\s*");

    @Override
    protected List<TextContent> buildTextContents(PsiElement root, Set<TextDomain> allowedDomains) {
        if (root instanceof PsiPlainText && root.getContainingFile().getName().endsWith(".txt") && allowedDomains.contains(PLAIN_TEXT)) {
            String text = root.getText();
            List<TextContent> result = new ArrayList<>();
            int[] ends = IntStream.concat(
                Text.allOccurrences(paragraphEnd, text).stream().mapToInt(TextRange::getStartOffset),
                IntStream.of(text.length())
            ).toArray();
            for (int i = 0; i < ends.length; i++) {
                int start = i == 0 ? 0 : ends[i - 1];
                int end = ends[i];
                ContainerUtil.addIfNotNull(result, TextContent.builder().build(root, PLAIN_TEXT, new TextRange(start, end)));
            }
            return result;
        }
        return Collections.emptyList();
    }

    @Override
    public Language getLanguage() {
        return PlainTextLanguage.INSTANCE;
    }
}
