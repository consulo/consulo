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
package consulo.it.ai;

import consulo.ai.*;
import consulo.application.Application;
import consulo.it.HeadlessApplicationExtension;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.ProjectOpenContext;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The tool-calling loop is the part of the chat that can silently misbehave, so it is driven here
 * with a scripted provider rather than through the UI.
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class AIChatSessionTest {
    /**
     * Answers with whatever the script says, so a tool round can be forced deterministically.
     */
    private static final class ScriptedType implements AIProviderType {
        private final List<AIResponse> myScript = new ArrayList<>();
        private final List<AIRequest> myRequests = new ArrayList<>();
        private final AtomicInteger myIndex = new AtomicInteger();

        private final AIModel myModel = new AIModel() {
            @Override
            public String getId() {
                return "scripted";
            }

            @Override
            public String getProviderTypeId() {
                return "scripted";
            }

            @Override
            public LocalizeValue getDisplayName() {
                return LocalizeValue.of("Scripted");
            }

            @Override
            public int getContextWindow() {
                return 1000;
            }

            @Override
            public Set<AIModelCapability> getCapabilities() {
                return Set.of(AIModelCapability.TOOL_USE);
            }
        };

        private ScriptedType then(AIResponse response) {
            myScript.add(response);
            return this;
        }

        @Override
        public String getId() {
            return "scripted";
        }

        @Override
        public LocalizeValue getDisplayName() {
            return LocalizeValue.of("Scripted");
        }

        @Override
        public Image getIcon() {
            return ImageEffects.empty(16);
        }

        @Override
        public List<AIModel> getModels() {
            return List.of(myModel);
        }

        @Override
        public CompletableFuture<AIResponse> chat(AIProvider provider, AIRequest request, @Nullable AIStreamListener listener) {
            myRequests.add(request);
            return CompletableFuture.completedFuture(myScript.get(Math.min(myIndex.getAndIncrement(), myScript.size() - 1)));
        }
    }

    private static AIResponse toolUse(String id, String tool, String argumentsJson) {
        return new AIResponse(AIMessage.of(AIRole.ASSISTANT, AIContent.toolUse(id, tool, argumentsJson)),
                              AIStopReason.TOOL_USE,
                              AIUsage.UNKNOWN);
    }

    private static AIResponse answer(String text) {
        return new AIResponse(AIMessage.assistant(text), AIStopReason.END_TURN, AIUsage.UNKNOWN);
    }

    @Test
    public void plainAnswerNeedsNoToolRound(Application application, ProjectManager projectManager) throws Exception {
        Project project = openProject(application, projectManager);

        ScriptedType type = new ScriptedType().then(answer("hello"));
        AIChatSession session = new AIChatSession(project, new AIProvider("scripted", type), type.myModel);

        AIResponse response = session.send("hi", null).get(30, TimeUnit.SECONDS);

        assertThat(response.getText()).isEqualTo("hello");
        assertThat(type.myRequests).hasSize(1);
        assertThat(session.getMessages()).hasSize(2);
    }

    @Test
    public void toolRequestIsRunAndFedBack(Application application, ProjectManager projectManager) throws Exception {
        Project project = openProject(application, projectManager);

        ScriptedType type = new ScriptedType()
            .then(toolUse("t1", "it_echo", "{\"text\":\"ab\",\"times\":2}"))
            .then(answer("done"));
        AIChatSession session = new AIChatSession(project, new AIProvider("scripted", type), type.myModel);

        AIResponse response = session.send("use a tool", null).get(30, TimeUnit.SECONDS);

        assertThat(response.getText()).isEqualTo("done");
        // one call for the tool request, a second carrying the result back
        assertThat(type.myRequests).hasSize(2);

        // the real MCP tool ran, and its output reached the model
        assertThat(session.getMessages())
            .filteredOn(message -> message.content().stream().anyMatch(block -> block instanceof AIContent.ToolResult))
            .singleElement()
            .satisfies(message -> {
                AIContent.ToolResult result = (AIContent.ToolResult) message.content().getFirst();
                assertThat(result.toolUseId()).isEqualTo("t1");
                assertThat(result.content()).isEqualTo("abab");
                assertThat(result.error()).isFalse();
            });
    }

    @Test
    public void unknownToolIsReportedBackInsteadOfFailingTheConversation(Application application, ProjectManager projectManager)
        throws Exception {
        Project project = openProject(application, projectManager);

        ScriptedType type = new ScriptedType()
            .then(toolUse("t1", "no_such_tool", "{}"))
            .then(answer("recovered"));
        AIChatSession session = new AIChatSession(project, new AIProvider("scripted", type), type.myModel);

        AIResponse response = session.send("use a tool", null).get(30, TimeUnit.SECONDS);

        assertThat(response.getText()).isEqualTo("recovered");
        assertThat(session.getMessages())
            .anySatisfy(message -> assertThat(message.content()).anySatisfy(block -> {
                assertThat(block).isInstanceOf(AIContent.ToolResult.class);
                assertThat(((AIContent.ToolResult) block).error()).isTrue();
            }));
    }

    @Test
    public void toolLoopIsBounded(Application application, ProjectManager projectManager) throws Exception {
        Project project = openProject(application, projectManager);

        // always asks for another tool, so only the guard can end this
        ScriptedType type = new ScriptedType().then(toolUse("t1", "it_echo", "{\"text\":\"x\"}"));
        AIChatSession session = new AIChatSession(project, new AIProvider("scripted", type), type.myModel);

        AIResponse response = session.send("loop forever", null).get(60, TimeUnit.SECONDS);

        assertThat(response.stopReason()).isEqualTo(AIStopReason.TOOL_USE);
        assertThat(type.myRequests).hasSizeLessThanOrEqualTo(17);
        assertThat(session.getMessages()).last()
            .satisfies(message -> assertThat(message.getText()).contains("Tool call limit"));
    }

    @Test
    public void toolsAreWithheldWhenDisabledOrUnsupported(Application application, ProjectManager projectManager) throws Exception {
        Project project = openProject(application, projectManager);

        ScriptedType type = new ScriptedType().then(answer("hi"));
        new AIChatSession(project, new AIProvider("scripted", type), type.myModel).toolsEnabled(false).send("x", null).get(30, TimeUnit.SECONDS);

        assertThat(type.myRequests.getFirst().getTools()).isEmpty();

        ScriptedType withToolsType = new ScriptedType().then(answer("hi"));
        new AIChatSession(project, new AIProvider("scripted-tools", withToolsType), withToolsType.myModel).send("x", null).get(30, TimeUnit.SECONDS);

        assertThat(withToolsType.myRequests.getFirst().getTools()).isNotEmpty();
    }

    private static Project openProject(Application application, ProjectManager projectManager) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-ai-chat");
        return projectManager
            .openProjectAsync(directory, application.getLastUIAccess(), new ProjectOpenContext())
            .get(30, TimeUnit.SECONDS);
    }
}
