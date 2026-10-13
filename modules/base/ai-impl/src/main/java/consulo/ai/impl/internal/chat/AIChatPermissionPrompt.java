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

import consulo.ai.AIToolPermissionDecision;
import consulo.ai.AIToolPermissionRequest;
import consulo.ai.localize.AILocalize;
import consulo.localize.LocalizeValue;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.HorizontalLayout;

import java.util.concurrent.CompletableFuture;

/**
 * Asks whether a tool may run: allow once, allow for the rest of the chat, or deny.
 *
 * @author VISTALL
 * @since 2026-10-13
 */
final class AIChatPermissionPrompt extends AIChatPrompt<AIToolPermissionDecision> {
    @RequiredUIAccess
    AIChatPermissionPrompt(AIToolPermissionRequest request, CompletableFuture<AIToolPermissionDecision> result) {
        super(result);

        myRoot.add(title(AILocalize.labelPermissionTitle(request.displayName())));
        String description = request.description();
        if (description != null && !description.isBlank()) {
            myRoot.add(Label.create(LocalizeValue.of(description)));
        }

        Button allow = Button.create(AILocalize.buttonAllow(), event -> result.complete(AIToolPermissionDecision.allow()));
        allow.addStyle(ButtonStyle.PRIMARY);
        Button allowAlways = Button.create(AILocalize.buttonAllowAlways(), event -> result.complete(AIToolPermissionDecision.allowAlways()));
        Button deny = Button.create(
            AILocalize.buttonDeny(),
            event -> result.complete(AIToolPermissionDecision.deny(AILocalize.messagePermissionDenied().get()))
        );

        HorizontalLayout buttons = buttonRow();
        buttons.add(allow);
        buttons.add(allowAlways);
        buttons.add(deny);
        myRoot.add(buttons);
    }
}
