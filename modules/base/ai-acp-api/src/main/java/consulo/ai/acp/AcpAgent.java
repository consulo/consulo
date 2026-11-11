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
package consulo.ai.acp;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import consulo.ai.AIChatInfo;
import consulo.ai.AIContent;
import consulo.ai.AIMessage;
import consulo.ai.AIResponse;
import consulo.ai.AIRole;
import consulo.ai.AIStopReason;
import consulo.ai.AIStreamListener;
import consulo.ai.AIToolPermissionDecision;
import consulo.ai.AIToolPermissionRequest;
import consulo.ai.AIUsage;
import consulo.process.ExecutionException;
import consulo.process.ProcessHandlerBuilderFactory;
import consulo.process.cmd.GeneralCommandLine;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An agent speaking version 1 of the Agent Client Protocol: the process, what it said it can do, and the sessions open
 * in it. A session is a chat; its id is the id of the chat, and a chat opened again is resumed or loaded when the agent
 * can do either.
 *
 * @author VISTALL
 * @since 2026-11-10
 */
public final class AcpAgent {
    public static final int PROTOCOL_VERSION = 1;

    /**
     * A way the agent signs in, as {@code initialize} listed it - {@link #authenticate} runs it.
     */
    public record AuthMethod(String id, String name, @Nullable String description) {
    }

    private static final class Turn {
        final @Nullable AIStreamListener myListener;
        final StringBuilder myText = new StringBuilder();
        final List<AIContent> myToolUses = new ArrayList<>();

        Turn(@Nullable AIStreamListener listener) {
            myListener = listener;
        }
    }

    private final AcpConnection myConnection;
    private final JsonObject myCapabilities;
    private final List<AuthMethod> myAuthMethods;
    private final @Nullable String myAgentName;
    private final @Nullable String myAgentVersion;

    private final Set<String> myOpenSessions = ConcurrentHashMap.newKeySet();
    /**
     * A chat the agent could not open again, continued in a new session of the agent instead - kept while the process
     * lives.
     */
    private final Map<String, String> myAliases = new ConcurrentHashMap<>();
    private final Map<String, Turn> myTurns = new ConcurrentHashMap<>();
    private final Map<String, List<AIMessage>> myReplays = new ConcurrentHashMap<>();

    private AcpAgent(AcpConnection connection, JsonObject initialized) {
        myConnection = connection;
        JsonObject capabilities = object(initialized, "agentCapabilities");
        myCapabilities = capabilities == null ? new JsonObject() : capabilities;

        List<AuthMethod> methods = new ArrayList<>();
        JsonElement authMethods = initialized.get("authMethods");
        if (authMethods != null && authMethods.isJsonArray()) {
            for (JsonElement element : authMethods.getAsJsonArray()) {
                if (element.isJsonObject()) {
                    JsonObject method = element.getAsJsonObject();
                    String description = string(method, "description");
                    methods.add(new AuthMethod(string(method, "id"), string(method, "name"), description.isEmpty() ? null : description));
                }
            }
        }
        myAuthMethods = List.copyOf(methods);

        JsonObject info = object(initialized, "agentInfo");
        myAgentName = info == null ? null : nonEmpty(string(info, "title"), nonEmpty(string(info, "name"), null));
        myAgentVersion = info == null ? null : nonEmpty(string(info, "version"), null);

        connection.onNotification("session/update", this::onUpdate);
        connection.onRequest("session/request_permission", this::onPermission);
    }

    /**
     * Starts the process and negotiates the protocol with it.
     */
    public static CompletableFuture<AcpAgent> start(ProcessHandlerBuilderFactory factory, GeneralCommandLine commandLine) {
        AcpConnection connection;
        try {
            connection = AcpConnection.start(factory, commandLine);
        }
        catch (ExecutionException e) {
            return CompletableFuture.failedFuture(e);
        }

        JsonObject fs = new JsonObject();
        fs.addProperty("readTextFile", false);
        fs.addProperty("writeTextFile", false);
        JsonObject clientCapabilities = new JsonObject();
        clientCapabilities.add("fs", fs);
        clientCapabilities.addProperty("terminal", false);
        JsonObject clientInfo = new JsonObject();
        clientInfo.addProperty("name", "consulo");
        clientInfo.addProperty("title", "Consulo");

        JsonObject params = new JsonObject();
        params.addProperty("protocolVersion", PROTOCOL_VERSION);
        params.add("clientCapabilities", clientCapabilities);
        params.add("clientInfo", clientInfo);

        // the handlers of the agent are registered before any session update can come
        return connection.request("initialize", params).thenApply(result -> {
            JsonObject initialized = result.isJsonObject() ? result.getAsJsonObject() : new JsonObject();
            return new AcpAgent(connection, initialized);
        }).whenComplete((agent, error) -> {
            if (error != null) {
                connection.close();
            }
        });
    }

    // region capabilities

    public @Nullable String getAgentName() {
        return myAgentName;
    }

    public @Nullable String getAgentVersion() {
        return myAgentVersion;
    }

    public List<AuthMethod> getAuthMethods() {
        return myAuthMethods;
    }

    public boolean canLoad() {
        return bool(myCapabilities, "loadSession");
    }

    public boolean canList() {
        return hasSessionCapability("list");
    }

    public boolean canResume() {
        return hasSessionCapability("resume");
    }

    public boolean canClose() {
        return hasSessionCapability("close");
    }

    public boolean canDelete() {
        return hasSessionCapability("delete");
    }

    private boolean hasSessionCapability(String name) {
        JsonObject sessions = object(myCapabilities, "sessionCapabilities");
        return sessions != null && sessions.get(name) != null && sessions.get(name).isJsonObject();
    }

    public boolean isAlive() {
        return myConnection.isAlive();
    }

    public void close() {
        myConnection.close();
    }

    // endregion

    public CompletableFuture<?> authenticate(String methodId) {
        JsonObject params = new JsonObject();
        params.addProperty("methodId", methodId);
        return myConnection.request("authenticate", params);
    }

    public CompletableFuture<String> newSession(Path workingDirectory) {
        return myConnection.request("session/new", sessionParams(null, workingDirectory)).thenApply(result -> {
            String id = result.isJsonObject() ? string(result.getAsJsonObject(), "sessionId") : "";
            if (id.isEmpty()) {
                throw new IllegalStateException("The agent started a session without an id");
            }
            myOpenSessions.add(id);
            return id;
        });
    }

    /**
     * The sessions of the agent in the directory, every page of them - empty when the agent can not list them.
     */
    public CompletableFuture<List<AIChatInfo>> listSessions(@Nullable Path workingDirectory) {
        if (!canList()) {
            return CompletableFuture.completedFuture(List.of());
        }
        List<AIChatInfo> sessions = new ArrayList<>();
        return listPage(workingDirectory, null, sessions).thenApply(ignored -> {
            sessions.sort((a, b) -> b.updated().compareTo(a.updated()));
            return sessions;
        });
    }

    private CompletableFuture<?> listPage(@Nullable Path workingDirectory, @Nullable String cursor, List<AIChatInfo> sessions) {
        JsonObject params = new JsonObject();
        if (workingDirectory != null) {
            params.addProperty("cwd", workingDirectory.toString());
        }
        if (cursor != null) {
            params.addProperty("cursor", cursor);
        }
        return myConnection.request("session/list", params).thenCompose(result -> {
            JsonObject page = result.isJsonObject() ? result.getAsJsonObject() : new JsonObject();
            JsonElement list = page.get("sessions");
            if (list != null && list.isJsonArray()) {
                for (JsonElement element : list.getAsJsonArray()) {
                    if (element.isJsonObject()) {
                        JsonObject session = element.getAsJsonObject();
                        String cwd = string(session, "cwd");
                        sessions.add(new AIChatInfo(
                            string(session, "sessionId"),
                            string(session, "title"),
                            instant(string(session, "updatedAt")),
                            cwd.isEmpty() ? null : Path.of(cwd)
                        ));
                    }
                }
            }
            String next = string(page, "nextCursor");
            return next.isEmpty() ? CompletableFuture.completedFuture(null) : listPage(workingDirectory, next, sessions);
        });
    }

    /**
     * Opens the session and answers the history the agent replays for it - empty when the agent can not load a
     * session. The session stays open, so the chat goes on in it.
     */
    public CompletableFuture<List<AIMessage>> loadSession(String chatId, Path workingDirectory) {
        if (!canLoad()) {
            return CompletableFuture.completedFuture(List.of());
        }
        if (myOpenSessions.contains(chatId)) {
            // the agent replays a session only when it opens it
            return CompletableFuture.completedFuture(List.of());
        }
        List<AIMessage> history = new ArrayList<>();
        myReplays.put(chatId, history);
        return myConnection.request("session/load", sessionParams(chatId, workingDirectory)).handle((result, error) -> {
            myReplays.remove(chatId);
            if (error != null) {
                throw error instanceof RuntimeException runtime ? runtime : new IllegalStateException(error);
            }
            myOpenSessions.add(chatId);
            synchronized (history) {
                return List.copyOf(history);
            }
        });
    }

    /**
     * The session the chat talks to in this process - the chat itself once resumed or loaded, or a new session when
     * the agent can do neither.
     */
    public CompletableFuture<String> openSession(String chatId, Path workingDirectory) {
        String alias = myAliases.get(chatId);
        if (alias != null) {
            return CompletableFuture.completedFuture(alias);
        }
        if (myOpenSessions.contains(chatId)) {
            return CompletableFuture.completedFuture(chatId);
        }

        CompletableFuture<?> open;
        if (canResume()) {
            open = myConnection.request("session/resume", sessionParams(chatId, workingDirectory));
        }
        else if (canLoad()) {
            myReplays.put(chatId, new ArrayList<>());
            open = myConnection.request("session/load", sessionParams(chatId, workingDirectory))
                .whenComplete((result, error) -> myReplays.remove(chatId));
        }
        else {
            open = CompletableFuture.failedFuture(new UnsupportedOperationException("The agent can not open a session again"));
        }
        return open.handle((result, error) -> error == null).thenCompose(opened -> {
            if (opened) {
                myOpenSessions.add(chatId);
                return CompletableFuture.completedFuture(chatId);
            }
            return newSession(workingDirectory).thenApply(id -> {
                myAliases.put(chatId, id);
                return id;
            });
        });
    }

    /**
     * Sends the text and completes once the agent ended the turn. Cancelling the future cancels the turn.
     */
    public CompletableFuture<AIResponse> prompt(String sessionId, String text, @Nullable AIStreamListener listener) {
        Turn turn = new Turn(listener);
        if (myTurns.putIfAbsent(sessionId, turn) != null) {
            return CompletableFuture.failedFuture(new IllegalStateException("The chat is still answering"));
        }

        JsonObject block = new JsonObject();
        block.addProperty("type", "text");
        block.addProperty("text", text);
        JsonArray prompt = new JsonArray();
        prompt.add(block);
        JsonObject params = new JsonObject();
        params.addProperty("sessionId", sessionId);
        params.add("prompt", prompt);

        CompletableFuture<AIResponse> response = myConnection.request("session/prompt", params).handle((result, error) -> {
            myTurns.remove(sessionId, turn);
            if (error != null) {
                throw error instanceof RuntimeException runtime ? runtime : new IllegalStateException(error);
            }
            String stopReason = result.isJsonObject() ? string(result.getAsJsonObject(), "stopReason") : "";
            List<AIContent> content = new ArrayList<>();
            synchronized (turn) {
                content.add(AIContent.text(turn.myText.toString()));
                content.addAll(turn.myToolUses);
            }
            return new AIResponse(new AIMessage(AIRole.ASSISTANT, content), stopReason(stopReason), AIUsage.UNKNOWN);
        });
        response.whenComplete((value, error) -> {
            if (response.isCancelled()) {
                JsonObject cancel = new JsonObject();
                cancel.addProperty("sessionId", sessionId);
                myConnection.notify("session/cancel", cancel);
            }
        });
        return response;
    }

    public CompletableFuture<?> closeSession(String chatId) {
        String sessionId = myAliases.getOrDefault(chatId, chatId);
        myAliases.remove(chatId);
        if (!myOpenSessions.remove(sessionId) || !canClose()) {
            return CompletableFuture.completedFuture(null);
        }
        JsonObject params = new JsonObject();
        params.addProperty("sessionId", sessionId);
        return myConnection.request("session/close", params);
    }

    public CompletableFuture<?> deleteSession(String chatId) {
        return closeSession(chatId).handle((ignored, error) -> null).thenCompose(ignored -> {
            if (!canDelete()) {
                return CompletableFuture.completedFuture(null);
            }
            JsonObject params = new JsonObject();
            params.addProperty("sessionId", chatId);
            return myConnection.request("session/delete", params);
        });
    }

    // region agent messages

    private void onUpdate(JsonObject params) {
        String sessionId = string(params, "sessionId");
        JsonObject update = object(params, "update");
        if (update == null) {
            return;
        }

        List<AIMessage> replay = myReplays.get(sessionId);
        if (replay != null) {
            replay(replay, update);
            return;
        }

        Turn turn = myTurns.get(sessionId);
        if (turn == null) {
            return;
        }
        AIStreamListener listener = turn.myListener;
        switch (string(update, "sessionUpdate")) {
            case "agent_message_chunk" -> {
                String text = text(object(update, "content"));
                synchronized (turn) {
                    turn.myText.append(text);
                }
                if (listener != null && !text.isEmpty()) {
                    listener.onTextDelta(text);
                }
            }
            case "agent_thought_chunk" -> {
                String text = text(object(update, "content"));
                if (listener != null && !text.isEmpty()) {
                    listener.onThinkingDelta(text);
                }
            }
            case "tool_call" -> {
                AIContent.ToolUse toolUse = toolUse(update);
                synchronized (turn) {
                    turn.myToolUses.add(toolUse);
                }
                if (listener != null) {
                    listener.onToolUse(toolUse);
                }
            }
            case "session_info_update" -> {
                String title = string(update, "title");
                if (listener != null && !title.isBlank()) {
                    listener.onTitle(title);
                }
            }
            default -> {
            }
        }
    }

    /**
     * Turns the history the agent replays while loading a session into messages - chunks of one side in a row make
     * one message.
     */
    private static void replay(List<AIMessage> history, JsonObject update) {
        AIRole role;
        AIContent content;
        switch (string(update, "sessionUpdate")) {
            case "user_message_chunk" -> {
                role = AIRole.USER;
                content = AIContent.text(text(object(update, "content")));
            }
            case "agent_message_chunk" -> {
                role = AIRole.ASSISTANT;
                content = AIContent.text(text(object(update, "content")));
            }
            case "tool_call" -> {
                role = AIRole.ASSISTANT;
                content = toolUse(update);
            }
            default -> {
                return;
            }
        }

        synchronized (history) {
            AIMessage last = history.isEmpty() ? null : history.getLast();
            if (last == null || last.role() != role) {
                history.add(new AIMessage(role, List.of(content)));
                return;
            }
            List<AIContent> merged = new ArrayList<>(last.content());
            if (content instanceof AIContent.Text text && !merged.isEmpty() && merged.getLast() instanceof AIContent.Text previous) {
                merged.set(merged.size() - 1, AIContent.text(previous.text() + text.text()));
            }
            else {
                merged.add(content);
            }
            history.set(history.size() - 1, new AIMessage(role, merged));
        }
    }

    /**
     * Asks the user through the listener of the turn, and picks the option of the agent which says the same.
     */
    private CompletableFuture<JsonElement> onPermission(JsonObject params) {
        Turn turn = myTurns.get(string(params, "sessionId"));
        AIStreamListener listener = turn == null ? null : turn.myListener;
        if (listener == null) {
            return CompletableFuture.completedFuture(cancelled());
        }

        JsonObject toolCall = object(params, "toolCall");
        String toolCallId = toolCall == null ? "" : string(toolCall, "toolCallId");
        String title = toolCall == null ? "" : string(toolCall, "title");
        String kind = toolCall == null ? "" : string(toolCall, "kind");
        JsonElement rawInput = toolCall == null ? null : toolCall.get("rawInput");
        AIToolPermissionRequest request = new AIToolPermissionRequest(
            toolCallId,
            kind.isEmpty() ? title : kind,
            title.isEmpty() ? kind : title,
            null,
            rawInput == null || rawInput.isJsonNull() ? "{}" : rawInput.toString()
        );

        JsonElement options = params.get("options");
        return listener.onToolPermission(request).handle((decision, error) -> {
            if (decision == null || options == null || !options.isJsonArray()) {
                return cancelled();
            }
            String optionId = pickOption(options.getAsJsonArray(), decision);
            if (optionId == null) {
                return cancelled();
            }
            JsonObject outcome = new JsonObject();
            outcome.addProperty("outcome", "selected");
            outcome.addProperty("optionId", optionId);
            JsonObject result = new JsonObject();
            result.add("outcome", outcome);
            return result;
        });
    }

    private static @Nullable String pickOption(JsonArray options, AIToolPermissionDecision decision) {
        List<String> kinds = decision.allowed()
            ? (decision.always() ? List.of("allow_always", "allow_once") : List.of("allow_once", "allow_always"))
            : List.of("reject_once", "reject_always");
        for (String kind : kinds) {
            for (JsonElement element : options) {
                if (element.isJsonObject() && kind.equals(string(element.getAsJsonObject(), "kind"))) {
                    return string(element.getAsJsonObject(), "optionId");
                }
            }
        }
        return null;
    }

    private static JsonObject cancelled() {
        JsonObject outcome = new JsonObject();
        outcome.addProperty("outcome", "cancelled");
        JsonObject result = new JsonObject();
        result.add("outcome", outcome);
        return result;
    }

    // endregion

    private static JsonObject sessionParams(@Nullable String sessionId, Path workingDirectory) {
        JsonObject params = new JsonObject();
        if (sessionId != null) {
            params.addProperty("sessionId", sessionId);
        }
        params.addProperty("cwd", workingDirectory.toAbsolutePath().toString());
        params.add("mcpServers", new JsonArray());
        return params;
    }

    private static AIContent.ToolUse toolUse(JsonObject update) {
        JsonElement rawInput = update.get("rawInput");
        String name = string(update, "title");
        return AIContent.toolUse(
            string(update, "toolCallId"),
            name.isEmpty() ? string(update, "kind") : name,
            rawInput == null || rawInput.isJsonNull() ? "{}" : rawInput.toString()
        );
    }

    private static AIStopReason stopReason(String value) {
        return switch (value) {
            case "end_turn" -> AIStopReason.END_TURN;
            case "max_tokens", "max_turn_requests" -> AIStopReason.MAX_TOKENS;
            case "cancelled" -> AIStopReason.CANCELLED;
            default -> AIStopReason.OTHER;
        };
    }

    private static String text(@Nullable JsonObject content) {
        return content != null && "text".equals(string(content, "type")) ? string(content, "text") : "";
    }

    private static Instant instant(String value) {
        if (value.isEmpty()) {
            return Instant.EPOCH;
        }
        try {
            return Instant.parse(value);
        }
        catch (DateTimeParseException e) {
            return Instant.EPOCH;
        }
    }

    private static @Nullable JsonObject object(JsonObject object, String name) {
        JsonElement element = object.get(name);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static String string(JsonObject object, String name) {
        JsonElement element = object.get(name);
        return element == null || element instanceof JsonNull || !element.isJsonPrimitive() ? "" : element.getAsString();
    }

    private static boolean bool(JsonObject object, String name) {
        JsonElement element = object.get(name);
        return element != null && element.isJsonPrimitive() && element.getAsBoolean();
    }

    private static @Nullable String nonEmpty(String value, @Nullable String fallback) {
        return value.isEmpty() ? fallback : value;
    }
}
