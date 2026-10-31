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
package consulo.ide.impl.idea.openapi.editor.richcopy;

import consulo.language.editor.impl.internal.richcopy.model.MarkupHandler;
import consulo.language.editor.impl.internal.richcopy.model.SyntaxInfo;
import consulo.language.editor.impl.internal.richcopy.model.SyntaxInfoCompressor;
import consulo.ui.color.RGBColor;
import consulo.ui.impl.font.TypefaceImpl;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SyntaxInfoTest {
    private static final List<String> EXPECTED = List.of(
        "foreground(3)", "style(1)", "text(0,5)", "foreground(1)", "text(5,6)", "font(1)", "background(2)", "text(6,1000006)"
    );

    @Test
    void unpacked() {
        assertThat(read(build(null))).isEqualTo(EXPECTED);
    }

    @Test
    void packed() {
        assertThat(read(build(Lz4SyntaxInfoCompressor.INSTANCE))).isEqualTo(EXPECTED);
    }

    @Test
    void readTwice() {
        SyntaxInfo info = build(Lz4SyntaxInfoCompressor.INSTANCE);
        assertThat(read(info)).isEqualTo(read(info));
    }

    private static SyntaxInfo build(@Nullable SyntaxInfoCompressor compressor) {
        SyntaxInfo.Builder builder = new SyntaxInfo.Builder(new RGBColor(0, 0, 0), new RGBColor(255, 255, 255), 12, compressor);
        builder.addForeground(new RGBColor(200, 0, 0));
        builder.addFontStyle(1);
        builder.addText(0, 5);
        builder.addForeground(new RGBColor(0, 0, 0));
        builder.addText(5, 6);
        builder.addTypeface(new TypefaceImpl("Monospaced", true));
        builder.addBackground(new RGBColor(255, 255, 255));
        builder.addText(6, 1_000_006);
        return builder.build();
    }

    private static List<String> read(SyntaxInfo info) {
        List<String> result = new ArrayList<>();
        info.processOutputInfo(new MarkupHandler() {
            @Override
            public void handleText(int startOffset, int endOffset) {
                result.add("text(" + startOffset + "," + endOffset + ")");
            }

            @Override
            public void handleForeground(int foregroundId) {
                result.add("foreground(" + foregroundId + ")");
            }

            @Override
            public void handleBackground(int backgroundId) {
                result.add("background(" + backgroundId + ")");
            }

            @Override
            public void handleFont(int fontNameId) {
                result.add("font(" + fontNameId + ")");
            }

            @Override
            public void handleStyle(int style) {
                result.add("style(" + style + ")");
            }

            @Override
            public boolean canHandleMore() {
                return true;
            }
        });
        return result;
    }
}
