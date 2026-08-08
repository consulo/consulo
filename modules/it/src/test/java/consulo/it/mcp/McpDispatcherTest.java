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
package consulo.it.mcp;

import consulo.application.Application;
import consulo.application.json.JsonService;
import consulo.it.HeadlessApplicationExtension;
import consulo.mcp.protocol.JsonRpcErrorCodes;
import consulo.mcp.protocol.JsonRpcRequest;
import consulo.mcp.protocol.JsonRpcResponse;
import consulo.mcp.protocol.model.*;
import consulo.mcpServer.impl.internal.McpDispatcher;
import consulo.mcpServer.impl.internal.McpSession;
import consulo.mcpServer.impl.internal.setting.McpServerSettings;
import consulo.project.Project;
import consulo.project.ProjectManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(HeadlessApplicationExtension.class)
public class McpDispatcherTest {
    @Test
    public void initializeNegotiatesProtocolVersion(Application application) throws Exception {
        McpDispatcher dispatcher = application.getInstance(McpDispatcher.class);

        JsonRpcResponse response = call(dispatcher, request(1, McpProtocol.METHOD_INITIALIZE, """
            {"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"it","version":"1"}}"""));

        assertThat(response).isNotNull();
        assertThat(response.error).isNull();

        InitializeResult result = (InitializeResult) response.result;
        assertThat(result).isNotNull();
        assertThat(result.protocolVersion).isEqualTo("2025-03-26");
        assertThat(result.capabilities.tools).isNotNull();
        assertThat(result.capabilities.tools.listChanged).isTrue();
        assertThat(result.serverInfo.name).isNotEmpty();
    }

    @Test
    public void initializeFallsBackToLatestOnUnknownVersion(Application application) throws Exception {
        McpDispatcher dispatcher = application.getInstance(McpDispatcher.class);

        JsonRpcResponse response = call(dispatcher, request(1, McpProtocol.METHOD_INITIALIZE, """
            {"protocolVersion":"1999-01-01","capabilities":{}}"""));

        InitializeResult result = (InitializeResult) response.result;
        assertThat(result.protocolVersion).isEqualTo(McpProtocol.LATEST_VERSION);
    }

    @Test
    public void notificationProducesNoResponse(Application application) throws Exception {
        McpDispatcher dispatcher = application.getInstance(McpDispatcher.class);

        JsonRpcRequest notification = new JsonRpcRequest();
        notification.jsonrpc = "2.0";
        notification.method = McpProtocol.METHOD_INITIALIZED;

        assertThat(dispatcher.dispatch(notification, null).get(30, TimeUnit.SECONDS)).isNull();
    }

    @Test
    public void toolsListAdvertisesSchemaAndHints(Application application) throws Exception {
        McpDispatcher dispatcher = application.getInstance(McpDispatcher.class);

        JsonRpcResponse response = call(dispatcher, request(2, McpProtocol.METHOD_TOOLS_LIST, null));
        ToolsListResult result = (ToolsListResult) response.result;

        ToolDefinition echo = result.tools.stream()
            .filter(tool -> "it_echo".equals(tool.name))
            .findFirst()
            .orElseThrow();

        assertThat(echo.description).contains("Echoes");
        assertThat(echo.annotations).isNotNull();
        assertThat(echo.annotations.readOnlyHint).isTrue();
        assertThat(echo.annotations.destructiveHint).isNull();

        assertThat(echo.inputSchema.get("type").getAsString()).isEqualTo("object");
        assertThat(echo.inputSchema.getAsJsonObject("properties").keySet()).containsExactly("text", "times");
        assertThat(echo.inputSchema.getAsJsonArray("required").toString()).isEqualTo("[\"text\"]");
    }

    @Test
    public void toolsCallAppliesDeclaredDefaults(Application application) throws Exception {
        McpDispatcher dispatcher = application.getInstance(McpDispatcher.class);

        ToolsCallResult defaulted = callTool(dispatcher, """
            {"name":"it_echo","arguments":{"text":"ab"}}""");
        assertThat(defaulted.isError).isFalse();
        assertThat(defaulted.content.getFirst().text).isEqualTo("ab");

        ToolsCallResult explicit = callTool(dispatcher, """
            {"name":"it_echo","arguments":{"text":"ab","times":3}}""");
        assertThat(explicit.content.getFirst().text).isEqualTo("ababab");
    }

    @Test
    public void toolFailureIsReportedInBandNotAsRpcError(Application application) throws Exception {
        McpDispatcher dispatcher = application.getInstance(McpDispatcher.class);

        JsonRpcResponse response = call(dispatcher, request(3, McpProtocol.METHOD_TOOLS_CALL, """
            {"name":"it_fail","arguments":{}}"""));

        assertThat(response.error).isNull();

        ToolsCallResult result = (ToolsCallResult) response.result;
        assertThat(result.isError).isTrue();
        assertThat(result.content.getFirst().text).isEqualTo("expected failure");
    }

    @Test
    public void missingRequiredArgumentFailsTheCall(Application application) throws Exception {
        McpDispatcher dispatcher = application.getInstance(McpDispatcher.class);

        ToolsCallResult result = callTool(dispatcher, """
            {"name":"it_echo","arguments":{}}""");

        assertThat(result.isError).isTrue();
        assertThat(result.content.getFirst().text).contains("text");
    }

    /**
     * The harness shares one application across every test class, so how many projects are open here
     * depends on execution order. Both outcomes are real contract, so both are asserted.
     */
    @Test
    public void projectResolutionMatchesTheNumberOfOpenProjects(Application application, ProjectManager projectManager)
        throws Exception {
        McpDispatcher dispatcher = application.getInstance(McpDispatcher.class);

        ToolsCallResult result = callTool(dispatcher, """
            {"name":"it_project_name","arguments":{}}""");

        Project[] openProjects = projectManager.getOpenProjects();
        if (openProjects.length == 1) {
            assertThat(result.isError).isFalse();
            assertThat(result.content.getFirst().text).isEqualTo(openProjects[0].getName());
        }
        else {
            assertThat(result.isError).isTrue();
            assertThat(result.content.getFirst().text)
                .contains(openProjects.length == 0 ? "No project" : "Several projects");
        }
    }

    /**
     * A disabled tool must vanish from the listing and stop being callable - advertising it while
     * refusing the call, or the reverse, would both mislead an agent.
     */
    @Test
    public void disabledToolsAreNeitherListedNorCallable(Application application) throws Exception {
        McpDispatcher dispatcher = application.getInstance(McpDispatcher.class);
        McpServerSettings settings = application.getInstance(McpServerSettings.class);

        assertThat(toolNames(dispatcher)).contains("it_echo");

        settings.setToolEnabled("it_echo", false);
        try {
            assertThat(toolNames(dispatcher)).doesNotContain("it_echo");

            JsonRpcResponse response = call(dispatcher, request(7, McpProtocol.METHOD_TOOLS_CALL, """
                {"name":"it_echo","arguments":{"text":"x"}}"""));
            assertThat(response.error).isNotNull();
            assertThat(response.error.code).isEqualTo(JsonRpcErrorCodes.INVALID_PARAMS);
        }
        finally {
            settings.setToolEnabled("it_echo", true);
        }

        assertThat(toolNames(dispatcher)).contains("it_echo");
    }

    private static List<String> toolNames(McpDispatcher dispatcher) throws Exception {
        JsonRpcResponse response = call(dispatcher, request(6, McpProtocol.METHOD_TOOLS_LIST, null));
        return ((ToolsListResult) response.result).tools.stream().map(tool -> tool.name).toList();
    }

    @Test
    public void unknownToolIsAnInvalidParamsError(Application application) throws Exception {
        McpDispatcher dispatcher = application.getInstance(McpDispatcher.class);

        JsonRpcResponse response = call(dispatcher, request(4, McpProtocol.METHOD_TOOLS_CALL, """
            {"name":"nope","arguments":{}}"""));

        assertThat(response.error).isNotNull();
        assertThat(response.error.code).isEqualTo(JsonRpcErrorCodes.INVALID_PARAMS);
    }

    @Test
    public void unknownMethodIsMethodNotFound(Application application) throws Exception {
        McpDispatcher dispatcher = application.getInstance(McpDispatcher.class);

        JsonRpcResponse response = call(dispatcher, request(5, "resources/list", null));

        assertThat(response.error).isNotNull();
        assertThat(response.error.code).isEqualTo(JsonRpcErrorCodes.METHOD_NOT_FOUND);
    }

    private static ToolsCallResult callTool(McpDispatcher dispatcher, String params) throws Exception {
        JsonRpcResponse response = call(dispatcher, request(9, McpProtocol.METHOD_TOOLS_CALL, params));
        assertThat(response.error).isNull();
        return (ToolsCallResult) response.result;
    }

    private static JsonRpcResponse call(McpDispatcher dispatcher, JsonRpcRequest request) throws Exception {
        McpSession session = McpProtocol.METHOD_INITIALIZE.equals(request.method) ? dispatcher.openSession(request) : null;
        JsonRpcResponse response = dispatcher.dispatch(request, session).get(30, TimeUnit.SECONDS);
        assertThat(response).isNotNull();
        return response;
    }

    private static JsonRpcRequest request(int id, String method, String params) {
        String json = params == null
            ? "{\"jsonrpc\":\"2.0\",\"id\":%d,\"method\":\"%s\"}".formatted(id, method)
            : "{\"jsonrpc\":\"2.0\",\"id\":%d,\"method\":\"%s\",\"params\":%s}".formatted(id, method, params);
        return JsonService.getInstance().fromJson(json, JsonRpcRequest.class);
    }
}
