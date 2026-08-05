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
package consulo.ai;

import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * A conversation with tool calling already wired up: when the model asks for tools, they are run and
 * the results are fed back automatically until it produces a final answer.
 * <p>
 * Deliberately free of UI, so the loop can be tested headlessly and reused by anything that wants a
 * conversation - the tool window is only one caller.
 *
 * @author VISTALL
 * @since 2026-08-04
 */
public final class AIChatSession {
    /**
     * Guards against a model that keeps asking for tools forever.
     */
    private static final int MAX_TOOL_ROUNDS = 16;

    private final Project myProject;
    private final AIProvider myProvider;
    private final AIModel myModel;
    private final List<AIMessage> myMessages = new ArrayList<>();

    private @Nullable String mySystemPrompt;
    private boolean myToolsEnabled = true;

    public AIChatSession(Project project, AIProvider provider, AIModel model) {
        myProject = project;
        myProvider = provider;
        myModel = model;
    }

    public AIChatSession systemPrompt(@Nullable String systemPrompt) {
        mySystemPrompt = systemPrompt;
        return this;
    }

    public AIChatSession toolsEnabled(boolean toolsEnabled) {
        myToolsEnabled = toolsEnabled;
        return this;
    }

    public AIModel getModel() {
        return myModel;
    }

    /**
     * Full history, including the tool traffic, so a UI can show what the model actually did.
     */
    public List<AIMessage> getMessages() {
        return List.copyOf(myMessages);
    }

    public void clear() {
        myMessages.clear();
    }

    /**
     * Adds the message and drives the conversation until the model stops asking for tools.
     */
    public CompletableFuture<AIResponse> send(String text, @Nullable AIStreamListener listener) {
        myMessages.add(AIMessage.user(text));
        return round(listener, 0);
    }

    private CompletableFuture<AIResponse> round(@Nullable AIStreamListener listener, int round) {
        List<AITool> tools = myToolsEnabled && myModel.hasCapability(AIModelCapability.TOOL_USE)
            ? AIToolManager.getInstance().getTools(myProject)
            : List.of();

        AIRequest request = AIRequest.builder(myModel)
            .systemPrompt(mySystemPrompt == null ? "" : mySystemPrompt)
            .messages(myMessages)
            .tools(tools)
            .build();

        return myProvider.chat(request, listener).thenCompose(response -> {
            myMessages.add(response.message());

            if (response.stopReason() != AIStopReason.TOOL_USE || response.getToolUses().isEmpty()) {
                return CompletableFuture.completedFuture(response);
            }
            if (round >= MAX_TOOL_ROUNDS) {
                myMessages.add(AIMessage.user("Tool call limit of " + MAX_TOOL_ROUNDS + " rounds reached."));
                return CompletableFuture.completedFuture(response);
            }

            return runTools(response).thenCompose(ignored -> round(listener, round + 1));
        });
    }

    private CompletableFuture<Void> runTools(AIResponse response) {
        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);

        for (AIContent.ToolUse toolUse : response.getToolUses()) {
            chain = chain.thenCompose(ignored -> callTool(toolUse));
        }
        return chain;
    }

    private CompletableFuture<Void> callTool(AIContent.ToolUse toolUse) {
        AITool tool = AIToolManager.getInstance().findTool(myProject, toolUse.toolName());
        if (tool == null) {
            addToolResult(toolUse, AIToolResult.error("No such tool: " + toolUse.toolName()));
            return CompletableFuture.completedFuture(null);
        }

        // a failing tool is reported back to the model rather than aborting the conversation
        return tool.call(myProject, toolUse.argumentsJson())
            .exceptionally(throwable -> AIToolResult.error(String.valueOf(throwable.getMessage())))
            .thenAccept(result -> addToolResult(toolUse, result));
    }

    private void addToolResult(AIContent.ToolUse toolUse, AIToolResult result) {
        myMessages.add(AIMessage.of(AIRole.USER, AIContent.toolResult(toolUse.id(), result.content(), result.error())));
    }
}
