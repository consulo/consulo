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

import consulo.application.ReadAction;
import consulo.it.CodeInsightTestFixture;
import consulo.it.HeadlessProjectExtension;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.sandboxPlugin.lang.psi.SandClass;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reference resolution at a {@code <caret>} written into the test data, which is {@code PsiFile.findReferenceAt}
 * underneath. An include is a file reference and needs nothing but the directory; a supertype goes through
 * {@code SandClassSearch} and therefore through the stub index, so it resolves only once indexing has settled.
 *
 * @author VISTALL
 */
@ExtendWith(HeadlessProjectExtension.class)
public class SandResolveAtCaretTest {
    @Test
    public void includeResolvesToTheIncludedFile(CodeInsightTestFixture fixture) throws Exception {
        fixture.addFileToProject("src/some.sand", "class Item { \"body\" }\n");
        fixture.configureByText("main.sand", "#include \"<caret>some.sand\"\nclass User : Item {}\n");

        PsiElement resolved = ReadAction.compute(fixture.getReferenceAtCaretPositionWithAssertion()::resolve);

        assertThat(resolved).isInstanceOf(PsiFile.class);
        assertThat(((PsiFile) resolved).getName()).isEqualTo("some.sand");
    }

    @Test
    public void supertypeResolvesAcrossFilesThroughTheIndex(CodeInsightTestFixture fixture) throws Exception {
        fixture.addFileToProject("src/some.sand", "class Item { \"body\" }\n");
        fixture.configureByText("main.sand", "#include \"some.sand\"\nclass User : <caret>Item {}\n");

        PsiElement resolved = ReadAction.compute(fixture.getReferenceAtCaretPositionWithAssertion()::resolve);

        assertThat(resolved).isInstanceOf(SandClass.class);
        assertThat(ReadAction.compute(() -> resolved.getContainingFile().getName())).isEqualTo("some.sand");
    }
}
