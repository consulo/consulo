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

import consulo.ai.*;
import consulo.ai.localize.AILocalize;
import consulo.application.Application;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Button;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.TextBox;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Chat UI built only from {@code consulo.ui}, so it runs on the AWT, Qt and web frontends alike.
 *
 * @author VISTALL
 * @since 2026-08-04
 */
public class AIChatPanel {
    private final Project myProject;
    private final UIAccess myUIAccess;

    private final VerticalLayout myRoot = VerticalLayout.create();
    private final VerticalLayout myConversation = VerticalLayout.create();
    private final TextBox myPrompt = TextBox.create();
    private final MutableFlatDataModel<AIModel> myModels = FlatDataModel.of(List.of());
    private final ComboBox<AIModel> myModelBox = ComboBox.create(myModels);
    private final Button mySend = Button.create(AILocalize.buttonSend());

    private @Nullable AIChatSession mySession;
    private @Nullable Label myStreamingLabel;

    @RequiredUIAccess
    public AIChatPanel(Project project, UIAccess uiAccess) {
        myProject = project;
        myUIAccess = uiAccess;

        List<AIProvider> providers = AIProviderTable.getInstance().getConfiguredProviders();
        if (providers.isEmpty()) {
            myRoot.add(Label.create(AILocalize.labelNoModel()));
            return;
        }

        myRoot.add(buildHeader(providers));
        myRoot.add(ScrollableLayout.create(myConversation));
        myRoot.add(buildPromptBar());
    }

    public Component getComponent() {
        return myRoot;
    }

    @RequiredUIAccess
    private Component buildHeader(List<AIProvider> providers) {
        AIProviderTable table = AIProviderTable.getInstance();

        ComboBox<AIProvider> providerBox = ComboBox.create(providers);
        providerBox.setTextRenderer(provider -> provider == null ? LocalizeValue.empty() : LocalizeValue.of(provider.getName()));

        AIProvider defaultProvider = table.getDefaultProvider();
        providerBox.setValue(defaultProvider == null ? providers.getFirst() : defaultProvider);

        myModelBox.setTextRenderer(model -> model == null ? LocalizeValue.empty() : model.getDisplayName());
        myModelBox.addValueListener(event -> rebuildSession(providerBox.getValue(), event.getValue()));

        // the model list belongs to the type, so it has to follow the selected instance
        providerBox.addValueListener(event -> fillModels(event.getValue(), table.getDefaultModel()));
        fillModels(providerBox.getValue(), table.getDefaultModel());

        Button clear = Button.create(AILocalize.buttonClear(), event -> {
            if (mySession != null) {
                mySession.clear();
            }
            myConversation.removeAll();
        });

        return DockLayout.create().left(providerBox).center(myModelBox).right(clear);
    }

    @RequiredUIAccess
    private void fillModels(@Nullable AIProvider provider, @Nullable AIModel preferred) {
        List<AIModel> models = provider == null ? List.of() : provider.getModels();

        myModels.removeAll();
        models.forEach(myModels::add);

        AIModel selected = preferred != null && models.contains(preferred)
            ? preferred
            : (models.isEmpty() ? null : models.getFirst());
        myModelBox.setValue(selected);

        rebuildSession(provider, selected);
    }

    @RequiredUIAccess
    private Component buildPromptBar() {
        myPrompt.setPlaceholder(AILocalize.labelPromptPlaceholder());
        mySend.addClickListener(event -> send());

        return DockLayout.create().center(myPrompt).right(mySend);
    }

    private void rebuildSession(@Nullable AIProvider provider, @Nullable AIModel model) {
        mySession = provider == null || model == null ? null : new AIChatSession(myProject, provider, model);
    }

    @RequiredUIAccess
    private void send() {
        AIChatSession session = mySession;
        String text = myPrompt.getValueOrError();
        if (session == null || text.isBlank()) {
            return;
        }

        myPrompt.setValue("");
        mySend.setEnabled(false);

        append(AILocalize.labelYou(), text);
        myStreamingLabel = append(AILocalize.labelAssistant(), "");

        Label streaming = myStreamingLabel;
        session.send(text, delta -> myUIAccess.give(() -> streaming.setText(streaming.getText().get() + delta)))
            .whenComplete((response, throwable) -> myUIAccess.give(() -> finish(session, streaming, response, throwable)));
    }

    @RequiredUIAccess
    private void finish(AIChatSession session, Label streaming, @Nullable AIResponse response, @Nullable Throwable throwable) {
        mySend.setEnabled(true);
        myStreamingLabel = null;

        if (throwable != null) {
            streaming.setText(AILocalize.labelFailed(String.valueOf(throwable.getMessage())));
            return;
        }

        // a non-streaming provider never fed the label, so fill it in from the finished answer
        if (response != null && streaming.getText().get().isEmpty()) {
            streaming.setText(LocalizeValue.of(response.getText()));
        }

        // show what the model actually ran, since tool calls happen without the user asking
        for (AIMessage message : session.getMessages()) {
            for (AIContent.ToolUse toolUse : message.getToolUses()) {
                append(LocalizeValue.empty(), AILocalize.labelToolCall(toolUse.toolName()).get());
            }
        }
    }

    @RequiredUIAccess
    private Label append(LocalizeValue speaker, String text) {
        if (speaker != LocalizeValue.empty()) {
            myConversation.add(Label.create(speaker));
        }

        Label label = Label.create(LocalizeValue.of(text));
        myConversation.add(label);
        return label;
    }
}
