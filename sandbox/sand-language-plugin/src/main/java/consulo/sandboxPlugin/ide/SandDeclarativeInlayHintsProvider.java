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
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.ui.style.StandardColors;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2025-05-29
 */
@ExtensionImpl
public class SandDeclarativeInlayHintsProvider implements DeclarativeInlayHintsProvider {
    @Override
    public DeclarativeInlayHintsCollector createCollector(PsiFile file, Editor editor) {
        return new DeclarativeInlayHintsCollector.SharedBypassCollector() {
            @Override
            public void collectFromElement(PsiElement element, DeclarativeInlayTreeSink sink) {
                if (PsiUtilCore.getElementType(element) == SandTokens.IDENTIFIER) {
                    sink.addPresentation(
                        new DeclarativeInlayPosition.InlineInlayPosition(element.getTextOffset(), true),
                        presentationTreeBuilder -> {
                            Image image = ImageEffects.colorFilled(12, 12, StandardColors.RED);
                            presentationTreeBuilder.icon(image);
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
