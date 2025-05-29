/*
 * Copyright 2013-2025 consulo.io
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
package consulo.sandboxPlugin.ide;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.language.Language;
import consulo.language.editor.inlay.*;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.localize.LocalizeValue;
import consulo.sandboxPlugin.lang.SandLanguage;
import consulo.sandboxPlugin.lang.psi.SandTokens;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2025-05-29
 */
@ExtensionImpl
public class SandInlayHintsProvider implements InlayHintsProvider {
    @Override
    public InlayHintsCollector createCollector(PsiFile file, Editor editor) {
        return new SharedBypassCollector() {
            @Override
            public void collectFromElement(PsiElement element, InlayTreeSink sink) {
                if (PsiUtilCore.getElementType(element) == SandTokens.IDENTIFIER) {
                    sink.addPresentation(
                        new InlayPosition.InlineInlayPosition(element.getTextOffset(), true),
                        null,
                        "Test",
                        HintFormat.DEFAULT,
                        presentationTreeBuilder -> {
                            presentationTreeBuilder.text("Inlay");
                        }
                    );
                }
            }
        };
    }

    @Nonnull
    @Override
    public Language getLanguage() {
        return SandLanguage.INSTANCE;
    }

    @Nonnull
    @Override
    public String getProviderId() {
        return "sand.inlay";
    }

    @Nonnull
    @Override
    public LocalizeValue getProviderName() {
        return LocalizeValue.localizeTODO("Sand Test");
    }
}
