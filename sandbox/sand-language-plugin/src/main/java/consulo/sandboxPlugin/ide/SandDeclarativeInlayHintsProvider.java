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
        return (DeclarativeInlayHintsCollector.SharedBypassCollector) (element, sink) -> {
            if (PsiUtilCore.getElementType(element) == SandTokens.IDENTIFIER) {
                sink.addPresentation(
                    new DeclarativeInlayPosition.InlineInlayPosition(element.getTextOffset(), true),
                    null,
                    null,
                    new HintFormat(HintColorKind.TextWithoutBackground, HintFontSize.AsInEditor, HintMarginPadding.OnlyPadding),
                    presentationTreeBuilder -> {
                        Image image = ImageEffects.colorFilled(12, 12, StandardColors.RED);
                        presentationTreeBuilder.icon(image);
                    }
                );
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
    public String getId() {
        return "sand.inlay";
    }

    @Nonnull
    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Sand Test");
    }

    @Nonnull
    @Override
    public LocalizeValue getDescription() {
        return LocalizeValue.localizeTODO("Sand Test ");
    }

    @Nonnull
    @Override
    public LocalizeValue getPreviewFileText() {
        return LocalizeValue.of();
    }

    @Nonnull
    @Override
    public InlayGroup getGroup() {
        return InlayGroup.OTHER_GROUP;
    }
}
