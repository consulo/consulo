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

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import consulo.ai.AIChangesViewer;
import consulo.ai.AIToolCallContent;
import consulo.ai.AIToolPermissionDecision;
import consulo.ai.AIToolPermissionOption;
import consulo.ai.AIToolPermissionOptionKind;
import consulo.ai.AIToolPermissionRequest;
import consulo.ai.localize.AILocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.Hyperlink;
import consulo.ui.Label;
import consulo.ui.OptionButton;
import consulo.ui.OptionButtonItem;
import consulo.ui.Space;
import consulo.ui.TextArea;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.FlowLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.LayoutStyle;
import consulo.ui.layout.VerticalLayout;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Asks whether a tool may run. Shows what the call is about to do - its text, its changes, its arguments - and the
 * choices of the agent, or allow once / allow for the rest of the chat / deny when the agent offers none. A question
 * the agent asks through a permission request shows its answers instead.
 *
 * @author VISTALL
 * @since 2026-10-13
 */
final class AIChatPermissionPrompt extends AIChatPrompt<AIToolPermissionDecision> {
    private static final int MAX_ARGUMENT_ROWS = 12;

    /**
     * @param runEverything allows this call and every later one of the chat without asking
     */
    @RequiredUIAccess
    AIChatPermissionPrompt(Project project,
                           AIToolPermissionRequest request,
                           CompletableFuture<AIToolPermissionDecision> result,
                           Runnable runEverything) {
        super(result);

        myRoot.add(title(request.interactive()
            ? AILocalize.labelPermissionQuestion(AIChatTexts.escape(request.displayName()))
            : AILocalize.labelPermissionTitle(AIChatTexts.escape(request.displayName()))));
        String description = request.description();
        if (description != null && !description.isBlank()) {
            myRoot.add(Label.create(AIChatTexts.plain(description)));
        }
        for (AIToolCallContent content : request.content()) {
            if (content instanceof AIToolCallContent.Text text && !text.text().isBlank()) {
                myRoot.add(Label.create(AIChatTexts.plain(text.text().strip())));
            }
        }

        HorizontalLayout links = buttonRow();
        List<AIToolCallContent.Diff> diffs = request.content().stream()
            .filter(AIToolCallContent.Diff.class::isInstance)
            .map(AIToolCallContent.Diff.class::cast)
            .toList();
        if (!diffs.isEmpty()) {
            Button changes = Button.create(AIChatToolCallView.changeCount(diffs), event -> AIChangesViewer.getInstance().show(project, diffs, 0));
            changes.addStyle(ButtonStyle.INPLACE);
            changes.setToolTipText(AILocalize.buttonShowChanges());
            links.add(changes);
        }
        String arguments = pretty(request.argumentsJson());
        DockLayout argumentsArea = null;
        if (!arguments.isEmpty()) {
            TextArea area = TextArea.create(arguments);
            area.setEditable(false);
            area.setMaxRows(MAX_ARGUMENT_ROWS);
            // the dock stretches the area over the width of the card
            DockLayout dock = DockLayout.create();
            dock.addStyle(LayoutStyle.TRANSPARENT_BACKGROUND);
            dock.center(area);
            dock.setVisible(false);
            argumentsArea = dock;
            Hyperlink toggle = Hyperlink.create(AILocalize.linkPermissionArgumentsShow());
            toggle.addHyperlinkListener(event -> {
                dock.setVisible(!dock.isVisible());
                toggle.setText(dock.isVisible() ? AILocalize.linkPermissionArgumentsHide() : AILocalize.linkPermissionArgumentsShow());
            });
            links.add(toggle);
        }
        if (!diffs.isEmpty() || argumentsArea != null) {
            myRoot.add(links);
        }
        if (argumentsArea != null) {
            myRoot.add(argumentsArea);
        }

        if (request.interactive()) {
            VerticalLayout answers = VerticalLayout.create();
            answers.addStyle(LayoutStyle.TRANSPARENT_BACKGROUND);
            for (AIToolPermissionOption option : request.options()) {
                answers.add(Button.create(AIChatTexts.plain(option.name()), event -> result.complete(AIToolPermissionDecision.option(option))));
            }
            myRoot.add(answers);
            return;
        }

        // the choices of an agent can be many and long - they wrap instead of running out of the card
        FlowLayout buttons = FlowLayout.create(Space.SMALL);
        buttons.addStyle(LayoutStyle.TRANSPARENT_BACKGROUND);
        if (request.options().isEmpty()) {
            OptionButton allow = OptionButton.create(AILocalize.buttonAllow(), event -> result.complete(AIToolPermissionDecision.allow()));
            allow.addStyle(ButtonStyle.PRIMARY);
            allow.setOptions(List.of(OptionButtonItem.of(AILocalize.buttonAllowAlways(),
                event -> result.complete(AIToolPermissionDecision.allowAlways()))));
            buttons.add(allow);
            buttons.add(Button.create(
                AILocalize.buttonDeny(),
                event -> result.complete(AIToolPermissionDecision.deny(AILocalize.messagePermissionDenied().get()))
            ));
        }
        else {
            // the answers of an agent come as a few buttons - the main one of each side, the rest in its drop-down
            List<AIToolPermissionOption> allows = new ArrayList<>();
            List<AIToolPermissionOption> others = new ArrayList<>();
            for (AIToolPermissionOption option : request.options()) {
                (option.kind().isAllow() ? allows : others).add(option);
            }
            allows.sort(Comparator.comparing(option -> option.kind() != AIToolPermissionOptionKind.ALLOW_ONCE));
            others.sort(Comparator.comparing(option -> option.kind() != AIToolPermissionOptionKind.REJECT_ONCE));
            Button allow = group(allows, result);
            if (allow != null) {
                allow.addStyle(ButtonStyle.PRIMARY);
                buttons.add(allow);
            }
            Button reject = group(others, result);
            if (reject != null) {
                buttons.add(reject);
            }
        }
        Hyperlink everything = Hyperlink.create(AILocalize.linkPermissionRunEverything(), event -> runEverything.run());
        everything.setToolTipText(AILocalize.linkPermissionRunEverythingTooltip());
        buttons.add(everything);
        myRoot.add(buttons);
    }

    /**
     * The first answer as a button - with the others in its drop-down when there are more.
     */
    @RequiredUIAccess
    private static @Nullable Button group(List<AIToolPermissionOption> options, CompletableFuture<AIToolPermissionDecision> result) {
        if (options.isEmpty()) {
            return null;
        }
        AIToolPermissionOption main = options.getFirst();
        if (options.size() == 1) {
            return Button.create(AIChatTexts.plain(main.name()), event -> result.complete(AIToolPermissionDecision.option(main)));
        }
        OptionButton button = OptionButton.create(AIChatTexts.plain(main.name()), event -> result.complete(AIToolPermissionDecision.option(main)));
        List<OptionButtonItem> items = new ArrayList<>();
        for (AIToolPermissionOption option : options.subList(1, options.size())) {
            items.add(OptionButtonItem.of(AIChatTexts.plain(option.name()), event -> result.complete(AIToolPermissionDecision.option(option))));
        }
        button.setOptions(items);
        return button;
    }

    private static String pretty(String json) {
        if (json.isBlank() || json.equals("{}") || json.equals("null")) {
            return "";
        }
        try {
            return new GsonBuilder().setPrettyPrinting().create().toJson(JsonParser.parseString(json));
        }
        catch (RuntimeException e) {
            return json;
        }
    }
}
