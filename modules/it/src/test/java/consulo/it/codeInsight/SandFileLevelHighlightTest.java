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
package consulo.it.codeInsight;

import consulo.it.CodeInsightTestFixture;
import consulo.it.HeadlessProjectExtension;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.sandboxPlugin.lang.annotation.SandAddClassFix;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author VISTALL
 */
@ExtendWith(HeadlessProjectExtension.class)
public class SandFileLevelHighlightTest {
    @Test
    public void fileWithoutClassesIsWarnedAboutAsAWhole(CodeInsightTestFixture fixture) throws Exception {
        fixture.configureByText("empty.sand", "// nothing declared\n");

        List<HighlightInfo> fileLevel = fileLevelHighlights(fixture);

        assertThat(fileLevel).hasSize(1);
        HighlightInfo info = fileLevel.get(0);
        assertThat(info.getDescription().get()).isEqualTo("Sand file declares no classes");
        assertThat(info.getSeverity()).isEqualTo(HighlightSeverity.WARNING);

        List<IntentionAction> fixes = new ArrayList<>();
        info.forEachQuickFix((action, range) -> fixes.add(action));
        assertThat(fixes).hasSize(1).first().isInstanceOf(SandAddClassFix.class);
    }

    @Test
    public void fileWithAClassHasNoFileLevelWarning(CodeInsightTestFixture fixture) throws Exception {
        fixture.configureByText("some.sand", "class Item { \"body\" }\n");

        assertThat(fileLevelHighlights(fixture)).isEmpty();
    }

    private static List<HighlightInfo> fileLevelHighlights(CodeInsightTestFixture fixture) throws Exception {
        return fixture.doHighlighting().stream().filter(HighlightInfo::isFileLevelAnnotation).toList();
    }
}
