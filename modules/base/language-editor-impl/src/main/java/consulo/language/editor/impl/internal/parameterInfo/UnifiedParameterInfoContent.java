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
package consulo.language.editor.impl.internal.parameterInfo;

import consulo.language.editor.parameterInfo.SignatureStyle;
import consulo.ui.AdvancedLabel;
import consulo.ui.Component;
import consulo.ui.LightPopup;
import consulo.ui.PopupOptions;
import consulo.ui.PopupPosition;
import consulo.ui.Space;
import consulo.ui.TextAttribute;
import consulo.ui.TextEffect;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.style.ComponentColors;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
final class UnifiedParameterInfoContent {
    private static final int LINE_LENGTH = 80;

    private UnifiedParameterInfoContent() {
    }

    @RequiredUIAccess
    static LightPopup createPopup(ParameterInfoModel model) {
        LightPopup popup = LightPopup.create(
            PopupOptions.builder()
                .position(PopupPosition.TOP)
                .disableRequestFocus()
                .disableCancelOnClickOutside()
                .arrow()
                .build()
        );
        popup.setBackgroundColor(ComponentColors.PARAMETER_INFO_BACKGROUND);
        popup.setContent(create(model));
        return popup;
    }

    @RequiredUIAccess
    static Component create(ParameterInfoModel model) {
        VerticalLayout layout = VerticalLayout.create(Space.NONE);

        for (ParameterInfoSignature signature : model.signatures()) {
            VerticalLayout row = VerticalLayout.create(Space.NONE);
            row.paddingBuilder().verticalSet(Space.X_SMALL).horizontalSet(Space.LARGE).apply();
            if (signature.highlighted()) {
                row.setBackgroundColor(ComponentColors.PARAMETER_INFO_CURRENT_OVERLOAD_BACKGROUND);
            }

            if (signature.separatorAfter()) {
                row.borderBuilder().bottomSet(ComponentColors.PARAMETER_INFO_LINE_SEPARATOR).apply();
            }

            for (List<ParameterInfoRun> line : ParameterInfoLines.breakLines(signature, String::length, LINE_LENGTH)) {
                AdvancedLabel label = AdvancedLabel.create();
                label.updatePresentation(presentation -> {
                    for (ParameterInfoRun run : line) {
                        presentation.append(run.text(), toAttribute(run));
                    }
                });
                row.add(label);
            }

            layout.add(row);
        }

        if (model.switchHint().isNotEmpty()) {
            AdvancedLabel hint = AdvancedLabel.create();
            hint.updatePresentation(presentation -> presentation.append(
                model.switchHint(),
                new TextAttribute(TextAttribute.STYLE_PLAIN, ComponentColors.PARAMETER_INFO_INFO_FOREGROUND)
            ));
            hint.paddingBuilder().topSet(Space.MEDIUM).bottomSet(Space.X_SMALL).horizontalSet(Space.LARGE).apply();
            layout.add(hint);
        }

        return layout;
    }

    private static TextAttribute toAttribute(ParameterInfoRun run) {
        int style = TextAttribute.STYLE_PLAIN;
        ColorValue foreground = ComponentColors.PARAMETER_INFO_FOREGROUND;

        if (run.styles().contains(SignatureStyle.DISABLED)) {
            foreground = ComponentColors.PARAMETER_INFO_DISABLED_FOREGROUND;
        }

        if (run.styles().contains(SignatureStyle.HIGHLIGHT)) {
            style = TextAttribute.STYLE_BOLD;
            foreground = ComponentColors.PARAMETER_INFO_CURRENT_PARAMETER_FOREGROUND;
        }

        TextAttribute attribute = new TextAttribute(style, foreground);
        if (run.styles().contains(SignatureStyle.STRIKEOUT)) {
            attribute = attribute.withEffect(TextEffect.STRIKEOUT);
        }
        return attribute;
    }
}
