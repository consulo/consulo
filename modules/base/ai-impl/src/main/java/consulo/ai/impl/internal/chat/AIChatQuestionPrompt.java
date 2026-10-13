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
package consulo.ai.impl.internal.chat;

import consulo.ai.AIQuestion;
import consulo.ai.AIQuestionOption;
import consulo.ai.localize.AILocalize;
import consulo.localize.LocalizeValue;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.CheckBox;
import consulo.ui.Label;
import consulo.ui.LabelStyle;
import consulo.ui.RadioButton;
import consulo.ui.RadioGroup;
import consulo.ui.Space;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.style.ComponentColors;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.VerticalLayout;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * The questions of the model, each with its choices - one or several - and a field for an answer in the user's own
 * words.
 *
 * @author VISTALL
 * @since 2026-10-13
 */
final class AIChatQuestionPrompt extends AIChatPrompt<Map<String, String>> {
    @RequiredUIAccess
    AIChatQuestionPrompt(List<AIQuestion> questions, CompletableFuture<Map<String, String>> result) {
        super(result);

        Map<String, Supplier<String>> answers = new LinkedHashMap<>();
        for (AIQuestion question : questions) {
            if (!question.header().isBlank()) {
                myRoot.add(title(LocalizeValue.of(question.header())));
            }
            myRoot.add(Label.create(LocalizeValue.of(question.question())));

            VerticalLayout options = VerticalLayout.create();
            Supplier<List<String>> chosen = question.multiSelect() ? checkBoxes(question, options) : radioButtons(question, options);
            myRoot.add(options);

            TextBox other = TextBox.create();
            myRoot.add(DockLayout.create().left(Label.create(AILocalize.labelQuestionOther())).center(other));

            answers.put(question.question(), () -> {
                List<String> parts = new ArrayList<>(chosen.get());
                String text = other.getValue();
                if (text != null && !text.isBlank()) {
                    parts.add(text.strip());
                }
                return String.join(", ", parts);
            });
        }

        Button answer = Button.create(AILocalize.buttonAnswer(), event -> {
            Map<String, String> values = new LinkedHashMap<>();
            answers.forEach((question, value) -> values.put(question, value.get()));
            result.complete(values);
        });
        answer.addStyle(ButtonStyle.PRIMARY);
        Button skip = Button.create(AILocalize.buttonSkip(), event -> result.completeExceptionally(new CancellationException()));

        HorizontalLayout buttons = buttonRow();
        buttons.add(answer);
        buttons.add(skip);
        myRoot.add(buttons);
    }

    @RequiredUIAccess
    private static Supplier<List<String>> radioButtons(AIQuestion question, VerticalLayout layout) {
        RadioGroup<AIQuestionOption> group = RadioGroup.create();
        for (AIQuestionOption option : question.options()) {
            RadioButton button = group.newButton(LocalizeValue.of(option.label()), option);
            layout.add(button);
            description(layout, option);
        }
        return () -> {
            AIQuestionOption value = group.getValue();
            return value == null ? List.of() : List.of(value.label());
        };
    }

    @RequiredUIAccess
    private static Supplier<List<String>> checkBoxes(AIQuestion question, VerticalLayout layout) {
        Map<AIQuestionOption, CheckBox> boxes = new LinkedHashMap<>();
        for (AIQuestionOption option : question.options()) {
            CheckBox box = CheckBox.create(LocalizeValue.of(option.label()));
            boxes.put(option, box);
            layout.add(box);
            description(layout, option);
        }
        return () -> {
            List<String> labels = new ArrayList<>();
            boxes.forEach((option, box) -> {
                if (box.getValueOrError()) {
                    labels.add(option.label());
                }
            });
            return labels;
        };
    }

    @RequiredUIAccess
    private static void description(VerticalLayout layout, AIQuestionOption option) {
        if (option.description().isBlank()) {
            return;
        }
        Label label = Label.create(LocalizeValue.of(option.description()));
        label.addStyle(LabelStyle.FONT_SMALL);
        label.setForegroundColor(ComponentColors.INFO_FOREGROUND);
        label.paddingBuilder().leftSet(Space.XX_LARGE).apply();
        layout.add(label);
    }
}
