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

import consulo.ai.AIContent;
import consulo.ai.AIMessage;
import consulo.ai.AIRequest;
import consulo.ai.AIResponse;
import consulo.ai.AIRole;
import consulo.ai.AITool;
import consulo.ai.AIToolManager;
import consulo.ai.AIToolResult;
import consulo.application.Application;
import consulo.it.HeadlessApplicationExtension;
import consulo.ai.AIProvider;
import consulo.ai.AIProviderTable;
import consulo.ai.AIProviderType;
import consulo.it.internal.HeadlessAIProvider;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.ProjectOpenContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The point where MCP and AI meet: the IDE's own MCP toolsets have to surface as {@link AITool}s
 * without {@code ai-api} knowing anything about MCP.
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class AIToolSeamTest {
    @Test
    public void mcpToolsetsSurfaceAsAiTools(Application application, ProjectManager projectManager) throws Exception {
        Project project = openProject(application, projectManager);

        List<AITool> tools = AIToolManager.getInstance().getTools(project);

        assertThat(tools).extracting(AITool::getName).contains("it_echo");

        AITool echo = AIToolManager.getInstance().findTool(project, "it_echo");
        assertThat(echo).isNotNull();
        assertThat(echo.getDescription()).contains("Echoes");
        // the schema has to be usable JSON Schema, since it is handed to a model verbatim
        assertThat(echo.getInputSchema()).contains("\"type\":\"object\"").contains("\"text\"");
    }

    @Test
    public void callingAnAiToolRunsTheMcpHandler(Application application, ProjectManager projectManager) throws Exception {
        Project project = openProject(application, projectManager);

        AITool echo = AIToolManager.getInstance().findTool(project, "it_echo");
        assertThat(echo).isNotNull();

        AIToolResult result = echo.call(project, "{\"text\":\"ab\",\"times\":2}").get(30, TimeUnit.SECONDS);
        assertThat(result.error()).isFalse();
        assertThat(result.content()).isEqualTo("abab");
    }

    @Test
    public void toolFailuresComeBackAsErrorResults(Application application, ProjectManager projectManager) throws Exception {
        Project project = openProject(application, projectManager);

        AITool failing = AIToolManager.getInstance().findTool(project, "it_fail");
        assertThat(failing).isNotNull();

        AIToolResult result = failing.call(project, "{}").get(30, TimeUnit.SECONDS);
        assertThat(result.error()).isTrue();
        assertThat(result.content()).contains("expected failure");
    }

    @Test
    public void malformedArgumentsDoNotThrow(Application application, ProjectManager projectManager) throws Exception {
        Project project = openProject(application, projectManager);

        AITool echo = AIToolManager.getInstance().findTool(project, "it_echo");
        assertThat(echo).isNotNull();

        AIToolResult result = echo.call(project, "not json").get(30, TimeUnit.SECONDS);
        assertThat(result.error()).isTrue();
    }

    @Test
    public void requestCarriesToolsAndStreamsTheAnswer(Application application, ProjectManager projectManager) throws Exception {
        Project project = openProject(application, projectManager);

        AIProviderType type = AIProviderTable.getInstance().findType(HeadlessAIProvider.ID);
        assertThat(type).isNotNull();
        AIProvider provider = new AIProvider("seam", type);

        AIRequest request = AIRequest.builder(type.getModels().getFirst())
            .systemPrompt("be terse")
            .message(AIMessage.user("hello there"))
            .tools(AIToolManager.getInstance().getTools(project))
            .maxTokens(64)
            .build();

        assertThat(request.getTools()).isNotEmpty();
        assertThat(request.getSystemPrompt()).isEqualTo("be terse");

        List<String> deltas = new ArrayList<>();
        AIResponse response = provider.chat(request, deltas::add).get(30, TimeUnit.SECONDS);

        assertThat(response.getText()).isEqualTo("echo: hello there");
        // streamed fragments must reassemble into exactly the final text
        assertThat(String.join("", deltas).strip()).isEqualTo(response.getText());
        assertThat(response.message().role()).isEqualTo(AIRole.ASSISTANT);
    }

    /**
     * The three execution contexts a tool body can ask for. The UI one in particular needs a UIAccess,
     * which only the project's coroutine context carries - an application-wide scope has none.
     */
    @Test
    public void toolsRunInReadWriteAndUiContexts(Application application, ProjectManager projectManager) throws Exception {
        Project project = openProject(application, projectManager);

        for (String[] toolAndAnswer : new String[][]{{"it_read_action", "read"}, {"it_write_action", "write"}, {"it_ui_action", "ui"}}) {
            AITool tool = AIToolManager.getInstance().findTool(project, toolAndAnswer[0]);
            assertThat(tool).describedAs(toolAndAnswer[0]).isNotNull();

            AIToolResult result = tool.call(project, "{}").get(30, TimeUnit.SECONDS);
            assertThat(result.error()).describedAs(toolAndAnswer[0]).isFalse();
            assertThat(result.content()).isEqualTo(toolAndAnswer[1]);
        }
    }

    @Test
    public void messageExposesTextAndToolUsesSeparately() {
        AIMessage message = AIMessage.of(AIRole.ASSISTANT,
                                         AIContent.text("calling a tool"),
                                         AIContent.toolUse("id-1", "it_echo", "{\"text\":\"x\"}"));

        assertThat(message.getText()).isEqualTo("calling a tool");
        assertThat(message.getToolUses()).singleElement()
            .satisfies(toolUse -> assertThat(toolUse.toolName()).isEqualTo("it_echo"));
    }

    private static Project openProject(Application application, ProjectManager projectManager) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-ai-seam");
        return projectManager
            .openProjectAsync(directory, application.getLastUIAccess(), new ProjectOpenContext())
            .get(30, TimeUnit.SECONDS);
    }
}
