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
import consulo.ai.AIAgentFileSystem;
import consulo.ai.AIChatCarryover;
import consulo.ai.AIChatInfo;
import consulo.ai.AIChatSessionListener;
import consulo.ai.AICommand;
import consulo.ai.AIContent;
import consulo.ai.AIContextUsage;
import consulo.ai.AIMcpServer;
import consulo.ai.AIMcpServerProvider;
import consulo.ai.AIMessage;
import consulo.ai.AIPlanEntry;
import consulo.ai.AIPlanEntryPriority;
import consulo.ai.AIPlanEntryStatus;
import consulo.ai.AIResponse;
import consulo.ai.AIRole;
import consulo.ai.AISessionOption;
import consulo.ai.AISessionOptionValue;
import consulo.ai.AIStopReason;
import consulo.ai.AIStreamListener;
import consulo.ai.AIToolCallContent;
import consulo.ai.AIToolCallStatus;
import consulo.ai.AIToolPermissionDecision;
import consulo.ai.AIToolPermissionOption;
import consulo.ai.AIToolPermissionOptionKind;
import consulo.ai.AIToolPermissionRequest;
import consulo.ai.AIUsage;
import consulo.ai.acp.localize.AcpLocalize;
import consulo.application.Application;
import consulo.logging.Logger;
import consulo.process.ExecutionException;
import consulo.process.ProcessHandlerBuilderFactory;
import consulo.process.cmd.GeneralCommandLine;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * An agent speaking version 1 of the Agent Client Protocol: the process, what it said it can do, and the sessions open
 * in it. A session is a chat; its id is the id of the chat, and a chat opened again is resumed or loaded when the agent
 * can do either.
 *
 * @author VISTALL
 * @since 2026-11-10
 */
public final class AcpAgent {
    private static final Logger LOG = Logger.getInstance(AcpAgent.class);

    public static final int PROTOCOL_VERSION = 1;

    /**
     * A way the agent signs in, as {@code initialize} listed it - {@link #authenticate} runs it.
     */
    public record AuthMethod(String id, String name, @Nullable String description) {
    }

    /**
     * How long a turn may wait for the first word of the agent before it is given up.
     */
    private static final int FIRST_UPDATE_TIMEOUT_SECONDS = 90;
    private static final int WATCHDOG_INTERVAL_SECONDS = 5;
    /**
     * A tool call without progress this long is told to the user, and told again as long as it stays so.
     */
    private static final long TOOL_WARN_AFTER_MILLIS = TimeUnit.SECONDS.toMillis(30);
    /**
     * A tool call without progress this long ends the turn - the agent most likely waits for something which never
     * comes, an interactive command above all.
     */
    private static final long TOOL_FAIL_AFTER_MILLIS = TimeUnit.MINUTES.toMillis(5);
    /**
     * A turn the agent sent nothing for this long is given up.
     */
    private static final long TURN_IDLE_FAIL_AFTER_MILLIS = TimeUnit.MINUTES.toMillis(15);

    private static final class Turn {
        final @Nullable AIStreamListener myListener;
        final StringBuilder myText = new StringBuilder();
        final StringBuilder myThinking = new StringBuilder();
        final List<AIContent.Image> myImages = new ArrayList<>();
        final Map<String, AIContent.ToolCall> myToolCalls = new LinkedHashMap<>();
        volatile boolean myAnswered;
        volatile long myLastUpdate = System.currentTimeMillis();
        /**
         * The running tool calls, by the time of the last news of each.
         */
        final Map<String, Long> myRunning = new ConcurrentHashMap<>();
        final Map<String, Long> myWarned = new ConcurrentHashMap<>();
        /**
         * The questions the agent waits on the user for - time stands still for the watchdog while there are some.
         */
        final AtomicInteger myAsking = new AtomicInteger();

        void toolCallChanged(AIContent.ToolCall toolCall) {
            // a subagent tells its progress through tool calls of its own
            boolean running = toolCall.status() == AIToolCallStatus.PENDING || toolCall.status() == AIToolCallStatus.IN_PROGRESS;
            if (running && !AIContent.ToolCall.KIND_TASK.equals(toolCall.kind())) {
                myRunning.put(toolCall.id(), System.currentTimeMillis());
                myWarned.remove(toolCall.id());
            }
            else {
                myRunning.remove(toolCall.id());
                myWarned.remove(toolCall.id());
            }
        }

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
    /**
     * The sessions started in this process which had no prompt yet - they know nothing of the chat they serve.
     */
    private final Set<String> myFreshSessions = ConcurrentHashMap.newKeySet();
    /**
     * The working directory of every session opened in this process - an agent writes only below it.
     */
    private final Map<String, Path> myDirectories = new ConcurrentHashMap<>();
    /**
     * The chats which go on in a new session at their next prompt.
     */
    private final Set<String> myStartingOver = ConcurrentHashMap.newKeySet();
    private final Map<String, Turn> myTurns = new ConcurrentHashMap<>();
    private final List<BiConsumer<JsonObject, AIStreamListener>> myUpdateObservers = new CopyOnWriteArrayList<>();
    private final Map<String, List<AIMessage>> myReplays = new ConcurrentHashMap<>();
    private final Map<String, List<AISessionOption>> myConfigOptions = new ConcurrentHashMap<>();
    /**
     * The options of a session made of its {@code models} or {@code modes} rather than config options - set with
     * {@code session/set_model} and {@code session/set_mode}.
     */
    private final Map<String, Set<String>> mySynthesizedOptions = new ConcurrentHashMap<>();
    private final Map<String, List<AICommand>> myCommands = new ConcurrentHashMap<>();
    private final List<BiConsumer<String, List<AISessionOption>>> myConfigListeners = new CopyOnWriteArrayList<>();
    /**
     * The listeners of a chat - keyed by the chat, which may continue in another session of the agent.
     */
    private final Map<String, List<AIChatSessionListener>> mySessionListeners = new ConcurrentHashMap<>();

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
        connection.onRequest("fs/read_text_file", this::onReadTextFile);
        connection.onRequest("fs/write_text_file", this::onWriteTextFile);
    }

    /**
     * Starts the process and negotiates the protocol with it.
     */
    public static CompletableFuture<AcpAgent> start(ProcessHandlerBuilderFactory factory, GeneralCommandLine commandLine) {
        return start(factory, commandLine, null);
    }

    /**
     * @param capabilitiesMeta the {@code _meta} of the client capabilities - extensions of the protocol one agent knows
     */
    public static CompletableFuture<AcpAgent> start(ProcessHandlerBuilderFactory factory,
                                                    GeneralCommandLine commandLine,
                                                    @Nullable JsonObject capabilitiesMeta) {
        AcpConnection connection;
        try {
            connection = AcpConnection.start(factory, commandLine);
        }
        catch (ExecutionException e) {
            return CompletableFuture.failedFuture(e);
        }

        JsonObject fs = new JsonObject();
        // files go through the documents of the IDE - unsaved text is read, writes are undoable and show at once
        fs.addProperty("readTextFile", true);
        fs.addProperty("writeTextFile", true);
        JsonObject clientCapabilities = new JsonObject();
        clientCapabilities.add("fs", fs);
        clientCapabilities.addProperty("terminal", false);
        if (capabilitiesMeta != null) {
            clientCapabilities.add("_meta", capabilitiesMeta);
        }
        JsonObject clientInfo = new JsonObject();
        clientInfo.addProperty("name", "consulo");
        clientInfo.addProperty("title", "Consulo");
        // the protocol requires a version - an agent which validates its input refuses to start without one
        clientInfo.addProperty("version", Application.get().getBuildNumber().asString());

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
            myFreshSessions.add(id);
            myDirectories.put(id, workingDirectory.toAbsolutePath().normalize());
            rememberConfig(id, result);
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
            rememberConfig(chatId, result);
            synchronized (history) {
                return AIChatCarryover.strip(history);
            }
        });
    }

    /**
     * The session the chat talks to in this process - the chat itself once resumed or loaded, or a new session when
     * the agent can do neither.
     */
    public CompletableFuture<String> openSession(String chatId, Path workingDirectory) {
        if (myStartingOver.remove(chatId)) {
            return newSession(workingDirectory).thenApply(id -> alias(chatId, id));
        }
        String alias = myAliases.get(chatId);
        if (alias != null) {
            return CompletableFuture.completedFuture(alias);
        }
        if (myOpenSessions.contains(chatId)) {
            return CompletableFuture.completedFuture(chatId);
        }

        CompletableFuture<JsonElement> open;
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
        return open.handle((result, error) -> {
            if (error == null) {
                rememberConfig(chatId, result);
            }
            return error == null;
        }).thenCompose(opened -> {
            if (opened) {
                myOpenSessions.add(chatId);
                return CompletableFuture.completedFuture(chatId);
            }
            return newSession(workingDirectory).thenApply(id -> alias(chatId, id));
        });
    }

    private String alias(String chatId, String sessionId) {
        myAliases.put(chatId, sessionId);
        for (AIChatSessionListener listener : mySessionListeners.getOrDefault(chatId, List.of())) {
            listener.sessionChanged(sessionId);
        }
        return sessionId;
    }

    /**
     * Whether the session was started in this process and had no prompt yet - it knows nothing of the chat it serves,
     * so the first prompt has to tell it.
     */
    public boolean isFresh(String sessionId) {
        return myFreshSessions.contains(sessionId);
    }

    /**
     * The chat goes on in a new session at its next prompt - the session it had keeps a history which no longer holds.
     */
    public void startOver(String chatId) {
        myAliases.remove(chatId);
        myStartingOver.add(chatId);
    }

    /**
     * Sends the content and completes once the agent ended the turn. Cancelling the future cancels the turn. A block
     * the agent can not take - an image, an embedded resource - goes as the closest one it can.
     */
    public CompletableFuture<AIResponse> prompt(String sessionId, List<AIContent> content, @Nullable AIStreamListener listener) {
        Turn turn = new Turn(listener);
        if (myTurns.putIfAbsent(sessionId, turn) != null) {
            return CompletableFuture.failedFuture(new IllegalStateException("The chat is still answering"));
        }
        myFreshSessions.remove(sessionId);

        JsonObject params = new JsonObject();
        params.addProperty("sessionId", sessionId);
        params.add("prompt", promptBlocks(content));

        CompletableFuture<AIResponse> response = new CompletableFuture<>();
        myConnection.request("session/prompt", params).whenComplete((result, error) -> {
            myTurns.remove(sessionId, turn);
            if (error != null) {
                response.completeExceptionally(error);
                return;
            }
            JsonObject ended = result.isJsonObject() ? result.getAsJsonObject() : new JsonObject();
            response.complete(new AIResponse(new AIMessage(AIRole.ASSISTANT, answer(turn)), stopReason(string(ended, "stopReason")), usage(object(ended, "usage"))));
        });

        CompletableFuture.delayedExecutor(FIRST_UPDATE_TIMEOUT_SECONDS, TimeUnit.SECONDS).execute(() -> {
            if (!turn.myAnswered && !response.isDone()) {
                response.completeExceptionally(new IOException("The agent did not answer within " + FIRST_UPDATE_TIMEOUT_SECONDS + " seconds"));
            }
        });
        watch(turn, response);
        response.whenComplete((value, error) -> {
            // given up on this side - the agent is told to stop the turn
            if (response.isCompletedExceptionally() && myTurns.get(sessionId) == turn) {
                JsonObject cancel = new JsonObject();
                cancel.addProperty("sessionId", sessionId);
                myConnection.notify("session/cancel", cancel);
            }
        });
        return response;
    }

    /**
     * Gives up a turn which hangs: a tool call without progress for long, or an agent which went silent.
     */
    private static void watch(Turn turn, CompletableFuture<AIResponse> response) {
        CompletableFuture.delayedExecutor(WATCHDOG_INTERVAL_SECONDS, TimeUnit.SECONDS).execute(() -> {
            if (response.isDone()) {
                return;
            }
            long now = System.currentTimeMillis();
            if (turn.myAsking.get() > 0) {
                // the user thinks - nothing hangs
                turn.myLastUpdate = now;
                turn.myRunning.replaceAll((id, since) -> now);
                watch(turn, response);
                return;
            }
            for (Map.Entry<String, Long> entry : turn.myRunning.entrySet()) {
                long elapsed = now - entry.getValue();
                String title = toolTitle(turn, entry.getKey());
                if (elapsed >= TOOL_FAIL_AFTER_MILLIS) {
                    response.completeExceptionally(new IOException(
                        title + " made no progress for " + TimeUnit.MILLISECONDS.toMinutes(elapsed) + " minutes - the turn was stopped"
                    ));
                    return;
                }
                Long warned = turn.myWarned.get(entry.getKey());
                if (elapsed >= TOOL_WARN_AFTER_MILLIS && (warned == null || now - warned >= TOOL_WARN_AFTER_MILLIS)) {
                    turn.myWarned.put(entry.getKey(), now);
                    AIStreamListener listener = turn.myListener;
                    if (listener != null) {
                        listener.onStatus(AcpLocalize.statusToolRunning(title, TimeUnit.MILLISECONDS.toSeconds(elapsed)));
                    }
                }
            }
            if (now - turn.myLastUpdate >= TURN_IDLE_FAIL_AFTER_MILLIS) {
                response.completeExceptionally(new IOException(
                    "The agent sent nothing for " + TimeUnit.MILLISECONDS.toMinutes(TURN_IDLE_FAIL_AFTER_MILLIS) + " minutes - the turn was stopped"
                ));
                return;
            }
            watch(turn, response);
        });
    }

    private static String toolTitle(Turn turn, String toolCallId) {
        AIContent.ToolCall toolCall;
        synchronized (turn) {
            toolCall = turn.myToolCalls.get(toolCallId);
        }
        return toolCall == null || toolCall.title().isBlank() ? toolCallId : toolCall.title();
    }

    private static List<AIContent> answer(Turn turn) {
        List<AIContent> content = new ArrayList<>();
        synchronized (turn) {
            if (!turn.myThinking.isEmpty()) {
                content.add(AIContent.thinking(turn.myThinking.toString()));
            }
            content.add(AIContent.text(turn.myText.toString()));
            content.addAll(turn.myImages);
            content.addAll(turn.myToolCalls.values());
        }
        return content;
    }

    private JsonArray promptBlocks(List<AIContent> content) {
        JsonObject promptCapabilities = object(myCapabilities, "promptCapabilities");
        boolean images = promptCapabilities != null && bool(promptCapabilities, "image");
        boolean embedded = promptCapabilities != null && bool(promptCapabilities, "embeddedContext");

        JsonArray blocks = new JsonArray();
        for (AIContent part : content) {
            JsonObject block = new JsonObject();
            switch (part) {
                case AIContent.Text text -> {
                    block.addProperty("type", "text");
                    block.addProperty("text", text.text());
                }
                case AIContent.ResourceLink link -> resourceLink(block, link.uri(), link.name(), link.mimeType());
                case AIContent.Resource resource when embedded -> {
                    JsonObject embeddedResource = new JsonObject();
                    embeddedResource.addProperty("uri", resource.uri());
                    if (resource.mimeType() != null) {
                        embeddedResource.addProperty("mimeType", resource.mimeType());
                    }
                    embeddedResource.addProperty("text", resource.text());
                    block.addProperty("type", "resource");
                    block.add("resource", embeddedResource);
                }
                case AIContent.Resource resource -> {
                    // the text goes along, as the agent can not take it embedded
                    block.addProperty("type", "text");
                    block.addProperty("text", resource.uri() + ":\n```\n" + resource.text() + "\n```");
                }
                case AIContent.Image image when images -> {
                    block.addProperty("type", "image");
                    block.addProperty("mimeType", image.mediaType());
                    block.addProperty("data", Base64.getEncoder().encodeToString(image.data()));
                }
                default -> {
                    continue;
                }
            }
            blocks.add(block);
        }
        return blocks;
    }

    private static void resourceLink(JsonObject block, String uri, String name, @Nullable String mimeType) {
        block.addProperty("type", "resource_link");
        block.addProperty("uri", uri);
        block.addProperty("name", name);
        if (mimeType != null) {
            block.addProperty("mimeType", mimeType);
        }
    }

    public CompletableFuture<?> closeSession(String chatId) {
        String sessionId = myAliases.getOrDefault(chatId, chatId);
        myAliases.remove(chatId);
        myConfigOptions.remove(sessionId);
        mySynthesizedOptions.remove(sessionId);
        myCommands.remove(sessionId);
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

    // region config options

    /**
     * The settings the session offers, as the agent last told them - empty when it offers none.
     */
    public List<AISessionOption> getConfigOptions(String sessionId) {
        return myConfigOptions.getOrDefault(sessionId, List.of());
    }

    /**
     * Called with the session and its settings whenever the agent tells them - a session opened, a setting changed.
     */
    public void addConfigListener(BiConsumer<String, List<AISessionOption>> listener) {
        myConfigListeners.add(listener);
    }

    /**
     * Sets a setting of the session and answers all of its settings as they are now - a changed model may bring
     * settings of its own and drop the ones of the model before.
     */
    public CompletableFuture<List<AISessionOption>> setConfigOption(String sessionId, String configId, String value) {
        JsonObject params = new JsonObject();
        params.addProperty("sessionId", sessionId);
        if (mySynthesizedOptions.getOrDefault(sessionId, Set.of()).contains(configId)) {
            boolean model = "model".equals(configId);
            params.addProperty(model ? "modelId" : "modeId", value);
            return myConnection.request(model ? "session/set_model" : "session/set_mode", params).thenApply(result -> {
                setCurrentValue(sessionId, configId, value);
                return getConfigOptions(sessionId);
            });
        }
        params.addProperty("configId", configId);
        params.addProperty("value", value);
        return myConnection.request("session/set_config_option", params).thenApply(result -> {
            rememberConfig(sessionId, result);
            return getConfigOptions(sessionId);
        });
    }

    public List<AICommand> getCommands(String sessionId) {
        return myCommands.getOrDefault(sessionId, List.of());
    }

    /**
     * The session a chat talks to in this process - the chat itself, unless it continued in another session.
     */
    public String sessionOf(String chatId) {
        return myAliases.getOrDefault(chatId, chatId);
    }

    /**
     * Tells the listener of the settings and the commands of the session the chat talks to, until the returned action
     * runs.
     */
    public Runnable addSessionListener(String chatId, AIChatSessionListener listener) {
        mySessionListeners.computeIfAbsent(chatId, id -> new CopyOnWriteArrayList<>()).add(listener);
        return () -> {
            List<AIChatSessionListener> listeners = mySessionListeners.get(chatId);
            if (listeners != null) {
                listeners.remove(listener);
            }
        };
    }

    private void rememberConfig(String sessionId, @Nullable JsonElement result) {
        if (result == null || !result.isJsonObject()) {
            return;
        }
        JsonObject object = result.getAsJsonObject();
        List<AISessionOption> options = new ArrayList<>(configOptions(object.get("configOptions")));
        Set<String> synthesized = new HashSet<>();
        JsonObject models = object(object, "models");
        if (models != null && options.stream().noneMatch(AISessionOption::isModel)) {
            AISessionOption model = synthesizedOption(models, "availableModels", "modelId", "currentModelId", "model", "Model", AISessionOption.CATEGORY_MODEL);
            if (model != null) {
                options.addFirst(model);
                synthesized.add(model.id());
            }
        }
        JsonObject modes = object(object, "modes");
        if (modes != null && options.stream().noneMatch(AISessionOption::isMode)) {
            AISessionOption mode = synthesizedOption(modes, "availableModes", "id", "currentModeId", "mode", "Mode", AISessionOption.CATEGORY_MODE);
            if (mode != null) {
                options.addFirst(mode);
                synthesized.add(mode.id());
            }
        }
        if (!synthesized.isEmpty()) {
            mySynthesizedOptions.put(sessionId, Set.copyOf(synthesized));
        }
        if (!options.isEmpty() || object.has("configOptions")) {
            updateConfig(sessionId, options);
        }
    }

    private void setCurrentValue(String sessionId, String optionId, String value) {
        List<AISessionOption> options = new ArrayList<>();
        for (AISessionOption option : getConfigOptions(sessionId)) {
            options.add(option.id().equals(optionId) ? withCurrentValue(option, value) : option);
        }
        updateConfig(sessionId, options);
    }

    private void updateConfig(String sessionId, List<AISessionOption> options) {
        List<AISessionOption> copy = List.copyOf(options);
        myConfigOptions.put(sessionId, copy);
        for (BiConsumer<String, List<AISessionOption>> listener : myConfigListeners) {
            try {
                listener.accept(sessionId, copy);
            }
            catch (RuntimeException e) {
                LOG.error(e);
            }
        }
        forEachSessionListener(sessionId, listener -> listener.optionsChanged(copy));
    }

    private void updateCommands(String sessionId, List<AICommand> commands) {
        List<AICommand> copy = List.copyOf(commands);
        myCommands.put(sessionId, copy);
        forEachSessionListener(sessionId, listener -> listener.commandsChanged(copy));
    }

    private void forEachSessionListener(String sessionId, Consumer<AIChatSessionListener> action) {
        mySessionListeners.forEach((chatId, listeners) -> {
            if (!sessionOf(chatId).equals(sessionId)) {
                return;
            }
            for (AIChatSessionListener listener : listeners) {
                try {
                    action.accept(listener);
                }
                catch (RuntimeException e) {
                    LOG.error(e);
                }
            }
        });
    }

    // endregion

    // region extensions

    /**
     * Answers a request of the agent outside the protocol - an extension of one agent. The handler gets the listener
     * of the turn the session is in, null when it answers none.
     */
    public void onSessionRequest(String method, BiFunction<JsonObject, @Nullable AIStreamListener, CompletableFuture<JsonElement>> handler) {
        myConnection.onRequest(method, params -> {
            // the agent waits for the answer - the watchdog of the turn waits with it
            Turn turn = myTurns.get(string(params, "sessionId"));
            if (turn == null) {
                return handler.apply(params, listenerOf(params));
            }
            turn.myAsking.incrementAndGet();
            return handler.apply(params, listenerOf(params)).whenComplete((result, error) -> turn.myAsking.decrementAndGet());
        });
    }

    /**
     * Takes a notification of the agent outside the protocol, with the listener of the turn its session is in.
     */
    public void onSessionNotification(String method, BiConsumer<JsonObject, @Nullable AIStreamListener> handler) {
        myConnection.onNotification(method, params -> handler.accept(params, listenerOf(params)));
    }

    /**
     * Sees every update of a session while it answers a turn, after the protocol handled it - for what an agent tells
     * in the text of an update, the file it saved a plan to, say. The observer gets the listener of the turn.
     */
    public void onSessionUpdate(BiConsumer<JsonObject, AIStreamListener> observer) {
        myUpdateObservers.add(observer);
    }

    /**
     * Adds text to the answer of the turn the session is in - for what an extension of the agent says beside its
     * messages, a plan for one. Nothing happens while the session answers no turn.
     */
    public void appendText(String sessionId, String text) {
        Turn turn = myTurns.get(sessionId);
        if (turn == null || text.isEmpty()) {
            return;
        }
        synchronized (turn) {
            turn.myText.append(text);
        }
        if (turn.myListener != null) {
            turn.myListener.onTextDelta(text);
        }
    }

    /**
     * Reports a tool call of the turn the session is in which the agent told outside the protocol - a subagent, for
     * one. The call replaces the one with the same id.
     */
    public void reportToolCall(String sessionId, AIContent.ToolCall toolCall) {
        Turn turn = myTurns.get(sessionId);
        if (turn == null) {
            return;
        }
        synchronized (turn) {
            turn.myToolCalls.put(toolCall.id(), toolCall);
        }
        turn.toolCallChanged(toolCall);
        if (turn.myListener != null) {
            turn.myListener.onToolCall(toolCall);
        }
    }

    /**
     * The tool call of the turn the session is in with the id, null when there is none.
     */
    public AIContent.@Nullable ToolCall findToolCall(String sessionId, String toolCallId) {
        Turn turn = myTurns.get(sessionId);
        if (turn == null) {
            return null;
        }
        synchronized (turn) {
            return turn.myToolCalls.get(toolCallId);
        }
    }

    private @Nullable AIStreamListener listenerOf(JsonObject params) {
        String sessionId = string(params, "sessionId");
        Turn turn = sessionId.isEmpty() ? null : myTurns.get(sessionId);
        return turn == null ? null : turn.myListener;
    }

    // endregion

    // region agent messages

    private void onUpdate(JsonObject params) {
        String sessionId = string(params, "sessionId");
        JsonObject update = object(params, "update");
        if (update == null) {
            return;
        }

        // the state of the session - it changes while no turn runs too
        String kind = string(update, "sessionUpdate");
        switch (kind) {
            case "config_option_update", "config_options_update" -> {
                if (update.has("configOptions")) {
                    updateConfig(sessionId, configOptions(update.get("configOptions")));
                }
                return;
            }
            case "current_mode_update" -> {
                String mode = orElse(string(update, "currentModeId"), string(update, "modeId"));
                for (AISessionOption option : getConfigOptions(sessionId)) {
                    if (option.isMode() && !mode.isEmpty()) {
                        setCurrentValue(sessionId, option.id(), mode);
                    }
                }
                return;
            }
            case "available_commands_update" -> {
                updateCommands(sessionId, commands(update.get("availableCommands")));
                return;
            }
            default -> {
            }
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
        turn.myAnswered = true;
        turn.myLastUpdate = System.currentTimeMillis();
        AIStreamListener listener = turn.myListener;
        if (listener != null) {
            for (BiConsumer<JsonObject, AIStreamListener> observer : myUpdateObservers) {
                try {
                    observer.accept(update, listener);
                }
                catch (RuntimeException e) {
                    LOG.error(e);
                }
            }
        }
        switch (kind) {
            case "agent_message_chunk" -> {
                AIContent.Image image = image(object(update, "content"));
                if (image != null) {
                    synchronized (turn) {
                        turn.myImages.add(image);
                    }
                    if (listener != null) {
                        listener.onImage(image);
                    }
                    return;
                }
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
                synchronized (turn) {
                    turn.myThinking.append(text);
                }
                if (listener != null && !text.isEmpty()) {
                    listener.onThinkingDelta(text);
                }
            }
            case "tool_call", "tool_call_update" -> {
                AIContent.ToolCall toolCall;
                synchronized (turn) {
                    toolCall = toolCall(turn.myToolCalls.get(string(update, "toolCallId")), update);
                    turn.myToolCalls.put(toolCall.id(), toolCall);
                }
                turn.toolCallChanged(toolCall);
                if (listener != null) {
                    listener.onToolCall(toolCall);
                }
            }
            case "plan" -> {
                if (listener != null) {
                    listener.onPlan(planEntries(update.get("entries")));
                }
            }
            case "usage_update" -> {
                if (listener != null) {
                    listener.onContextUsage(contextUsage(update));
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
     * one message, and an update of a tool call replaces the call.
     */
    private static void replay(List<AIMessage> history, JsonObject update) {
        AIRole role;
        AIContent content;
        switch (string(update, "sessionUpdate")) {
            case "user_message_chunk" -> {
                role = AIRole.USER;
                AIContent.Image image = image(object(update, "content"));
                content = image != null ? image : AIContent.text(text(object(update, "content")));
            }
            case "agent_message_chunk" -> {
                role = AIRole.ASSISTANT;
                AIContent.Image image = image(object(update, "content"));
                content = image != null ? image : AIContent.text(text(object(update, "content")));
            }
            case "agent_thought_chunk" -> {
                role = AIRole.ASSISTANT;
                content = AIContent.thinking(text(object(update, "content")));
            }
            case "tool_call", "tool_call_update" -> {
                synchronized (history) {
                    replayToolCall(history, update);
                }
                return;
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
            AIContent previous = merged.isEmpty() ? null : merged.getLast();
            if (content instanceof AIContent.Text text && previous instanceof AIContent.Text before) {
                merged.set(merged.size() - 1, AIContent.text(before.text() + text.text()));
            }
            else if (content instanceof AIContent.Thinking thinking && previous instanceof AIContent.Thinking before) {
                merged.set(merged.size() - 1, AIContent.thinking(before.text() + thinking.text()));
            }
            else {
                merged.add(content);
            }
            history.set(history.size() - 1, new AIMessage(role, merged));
        }
    }

    private static void replayToolCall(List<AIMessage> history, JsonObject update) {
        String id = string(update, "toolCallId");
        for (int i = history.size() - 1; i >= 0; i--) {
            AIMessage message = history.get(i);
            List<AIContent> content = message.content();
            for (int j = 0; j < content.size(); j++) {
                if (content.get(j) instanceof AIContent.ToolCall previous && previous.id().equals(id)) {
                    List<AIContent> merged = new ArrayList<>(content);
                    merged.set(j, toolCall(previous, update));
                    history.set(i, new AIMessage(message.role(), merged));
                    return;
                }
            }
        }
        AIContent.ToolCall toolCall = toolCall(null, update);
        AIMessage last = history.isEmpty() ? null : history.getLast();
        if (last != null && last.role() == AIRole.ASSISTANT) {
            List<AIContent> merged = new ArrayList<>(last.content());
            merged.add(toolCall);
            history.set(history.size() - 1, new AIMessage(AIRole.ASSISTANT, merged));
        }
        else {
            history.add(new AIMessage(AIRole.ASSISTANT, List.of(toolCall)));
        }
    }

    /**
     * Asks the user through the listener of the turn, and picks the option of the agent which says the same.
     */
    private CompletableFuture<JsonElement> onPermission(JsonObject params) {
        Turn turn = myTurns.get(string(params, "sessionId"));
        if (turn == null || turn.myListener == null) {
            return CompletableFuture.completedFuture(cancelled());
        }
        AIStreamListener listener = turn.myListener;

        JsonObject toolCall = object(params, "toolCall");
        String toolCallId = toolCall == null ? "" : string(toolCall, "toolCallId");
        String title = toolCall == null ? "" : string(toolCall, "title");
        String kind = toolCall == null ? "" : string(toolCall, "kind");
        JsonElement rawInput = toolCall == null ? null : toolCall.get("rawInput");
        JsonElement options = params.get("options");
        List<AIToolPermissionOption> choices = permissionOptions(options);
        // a question of the agent comes as a permission request with its answers as options - it is never a yes
        boolean interactive = "other".equals(kind) || choices.stream().anyMatch(option -> option.id().contains("ask_question"));
        AIToolPermissionRequest request = new AIToolPermissionRequest(
            toolCallId,
            kind.isEmpty() ? title : kind,
            title.isEmpty() ? kind : title,
            null,
            rawInput == null || rawInput.isJsonNull() ? "{}" : rawInput.toString(),
            choices,
            toolCall == null ? List.of() : toolCallContent(toolCall.get("content")),
            interactive
        );

        turn.myAsking.incrementAndGet();
        return listener.onToolPermission(request).handle((decision, error) -> {
            turn.myAsking.decrementAndGet();
            if (decision == null || options == null || !options.isJsonArray()) {
                return cancelled();
            }
            String chosen = decision.optionId();
            String optionId = chosen != null && choices.stream().anyMatch(option -> option.id().equals(chosen))
                ? chosen
                : pickOption(options.getAsJsonArray(), decision);
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

    // region client files

    private CompletableFuture<JsonElement> onReadTextFile(JsonObject params) {
        Path path = Path.of(string(params, "path"));
        JsonElement line = params.get("line");
        JsonElement limit = params.get("limit");
        return AIAgentFileSystem.getInstance().readText(path).thenApply(text -> {
            JsonObject result = new JsonObject();
            result.addProperty("content", slice(text,
                line == null || line.isJsonNull() ? 1 : line.getAsInt(),
                limit == null || limit.isJsonNull() ? -1 : limit.getAsInt()));
            return result;
        });
    }

    private CompletableFuture<JsonElement> onWriteTextFile(JsonObject params) {
        Path path = Path.of(string(params, "path")).toAbsolutePath().normalize();
        Path directory = myDirectories.get(string(params, "sessionId"));
        // the agent works in the directory of its session - a write anywhere else is not its business
        if (directory == null || !path.startsWith(directory)) {
            return CompletableFuture.failedFuture(new AcpConnection.AcpException(
                AcpConnection.INVALID_PARAMS,
                "Writing outside of the working directory " + directory + " is not allowed: " + path
            ));
        }
        return AIAgentFileSystem.getInstance().writeText(path, string(params, "content")).thenApply(ignored -> JsonNull.INSTANCE);
    }

    /**
     * The lines of the text from the 1-based {@code line} on, at most {@code limit} of them when it is not negative.
     */
    private static String slice(String text, int line, int limit) {
        if (line <= 1 && limit < 0) {
            return text;
        }
        String[] lines = text.split("\n", -1);
        int from = Math.max(0, line - 1);
        int to = limit < 0 ? lines.length : Math.min(lines.length, from + limit);
        return from >= lines.length ? "" : String.join("\n", Arrays.asList(lines).subList(from, to));
    }

    // endregion

    private static List<AIToolPermissionOption> permissionOptions(@Nullable JsonElement options) {
        List<AIToolPermissionOption> result = new ArrayList<>();
        if (options == null || !options.isJsonArray()) {
            return result;
        }
        for (JsonElement element : options.getAsJsonArray()) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject option = element.getAsJsonObject();
            String id = string(option, "optionId");
            if (id.isEmpty()) {
                continue;
            }
            String name = string(option, "name");
            AIToolPermissionOptionKind kind = switch (string(option, "kind")) {
                case "allow_once" -> AIToolPermissionOptionKind.ALLOW_ONCE;
                case "allow_always" -> AIToolPermissionOptionKind.ALLOW_ALWAYS;
                case "reject_once" -> AIToolPermissionOptionKind.REJECT_ONCE;
                case "reject_always" -> AIToolPermissionOptionKind.REJECT_ALWAYS;
                default -> AIToolPermissionOptionKind.OTHER;
            };
            result.add(new AIToolPermissionOption(id, name.isEmpty() ? id : name, kind));
        }
        return result;
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

    private JsonObject sessionParams(@Nullable String sessionId, Path workingDirectory) {
        JsonObject params = new JsonObject();
        if (sessionId != null) {
            params.addProperty("sessionId", sessionId);
            myDirectories.put(sessionId, workingDirectory.toAbsolutePath().normalize());
        }
        params.addProperty("cwd", workingDirectory.toAbsolutePath().toString());
        params.add("mcpServers", mcpServers());
        return params;
    }

    /**
     * The MCP servers of the IDE - the agent searches and navigates with the indexes of the IDE through them. Only for
     * an agent which takes servers over http.
     */
    private JsonArray mcpServers() {
        JsonArray servers = new JsonArray();
        JsonObject mcpCapabilities = object(myCapabilities, "mcpCapabilities");
        if (mcpCapabilities == null || !bool(mcpCapabilities, "http")) {
            return servers;
        }
        Application.get().getExtensionPoint(AIMcpServerProvider.class).forEach(provider -> {
            for (AIMcpServer server : provider.getServers()) {
                JsonObject entry = new JsonObject();
                entry.addProperty("type", "http");
                entry.addProperty("name", server.name());
                entry.addProperty("url", server.url());
                JsonArray headers = new JsonArray();
                server.headers().forEach((name, value) -> {
                    JsonObject header = new JsonObject();
                    header.addProperty("name", name);
                    header.addProperty("value", value);
                    headers.add(header);
                });
                entry.add("headers", headers);
                servers.add(entry);
            }
        });
        return servers;
    }

    /**
     * The tool call an update tells, on top of the call before it - an update carries only what changed.
     */
    private static AIContent.ToolCall toolCall(AIContent.@Nullable ToolCall previous, JsonObject update) {
        String title = string(update, "title");
        String kind = string(update, "kind");
        JsonElement rawInput = update.get("rawInput");
        JsonElement rawOutput = update.get("rawOutput");
        JsonElement locations = update.get("locations");
        JsonElement content = update.get("content");
        JsonObject meta = object(update, "_meta");
        String parentId = meta == null ? "" : string(meta, "parentToolCallId");

        List<String> paths = new ArrayList<>();
        if (locations != null && locations.isJsonArray()) {
            for (JsonElement location : locations.getAsJsonArray()) {
                if (location.isJsonObject() && !string(location.getAsJsonObject(), "path").isEmpty()) {
                    paths.add(string(location.getAsJsonObject(), "path"));
                }
            }
        }

        return new AIContent.ToolCall(
            string(update, "toolCallId"),
            !parentId.isEmpty() ? parentId : previous == null ? null : previous.parentId(),
            !title.isEmpty() ? title : previous == null ? kind : previous.title(),
            !kind.isEmpty() ? kind : previous == null ? "other" : previous.kind(),
            update.has("status") ? toolCallStatus(string(update, "status")) : previous == null ? AIToolCallStatus.PENDING : previous.status(),
            locations != null || previous == null ? List.copyOf(paths) : previous.locations(),
            content != null || previous == null ? toolCallContent(content) : previous.content(),
            rawInput != null && !rawInput.isJsonNull() ? rawInput.toString() : previous == null ? "{}" : previous.inputJson(),
            rawOutput != null && !rawOutput.isJsonNull() ? rawOutput.toString() : previous == null ? "" : previous.outputJson()
        );
    }

    private static AIToolCallStatus toolCallStatus(String value) {
        return switch (value) {
            case "in_progress" -> AIToolCallStatus.IN_PROGRESS;
            case "completed" -> AIToolCallStatus.COMPLETED;
            case "failed" -> AIToolCallStatus.FAILED;
            default -> AIToolCallStatus.PENDING;
        };
    }

    private static List<AIToolCallContent> toolCallContent(@Nullable JsonElement element) {
        List<AIToolCallContent> result = new ArrayList<>();
        if (element == null || !element.isJsonArray()) {
            return result;
        }
        for (JsonElement item : element.getAsJsonArray()) {
            if (!item.isJsonObject()) {
                continue;
            }
            JsonObject content = item.getAsJsonObject();
            switch (string(content, "type")) {
                case "content" -> {
                    AIContent.Image image = image(object(content, "content"));
                    if (image != null) {
                        result.add(new AIToolCallContent.Image(image.mediaType(), image.data()));
                        continue;
                    }
                    String text = text(object(content, "content"));
                    if (!text.isEmpty()) {
                        result.add(new AIToolCallContent.Text(text));
                    }
                }
                case "diff" -> {
                    JsonElement oldElement = content.get("oldText");
                    String oldText = oldElement == null || oldElement.isJsonNull() ? null : oldElement.getAsString();
                    String newText = string(content, "newText");
                    // Cursor tells a created file with the header of a patch in place of no old text
                    if ("-- /dev/null".equals(oldText)) {
                        oldText = null;
                        if (newText.startsWith("++ b/")) {
                            int newLine = newText.indexOf('\n');
                            newText = newLine < 0 ? "" : newText.substring(newLine + 1);
                        }
                    }
                    result.add(new AIToolCallContent.Diff(string(content, "path"), oldText, newText));
                }
                case "terminal" -> result.add(new AIToolCallContent.Terminal(string(content, "terminalId")));
                default -> {
                }
            }
        }
        return List.copyOf(result);
    }

    private static List<AIPlanEntry> planEntries(@Nullable JsonElement element) {
        List<AIPlanEntry> entries = new ArrayList<>();
        if (element == null || !element.isJsonArray()) {
            return entries;
        }
        for (JsonElement item : element.getAsJsonArray()) {
            if (item.isJsonObject()) {
                JsonObject entry = item.getAsJsonObject();
                entries.add(new AIPlanEntry(string(entry, "content"), planPriority(string(entry, "priority")), planStatus(string(entry, "status"))));
            }
        }
        return List.copyOf(entries);
    }

    public static AIPlanEntryPriority planPriority(String value) {
        return switch (value) {
            case "high" -> AIPlanEntryPriority.HIGH;
            case "low" -> AIPlanEntryPriority.LOW;
            default -> AIPlanEntryPriority.MEDIUM;
        };
    }

    public static AIPlanEntryStatus planStatus(String value) {
        return switch (value) {
            case "in_progress" -> AIPlanEntryStatus.IN_PROGRESS;
            case "completed" -> AIPlanEntryStatus.COMPLETED;
            default -> AIPlanEntryStatus.PENDING;
        };
    }

    private static List<AICommand> commands(@Nullable JsonElement element) {
        List<AICommand> commands = new ArrayList<>();
        if (element == null || !element.isJsonArray()) {
            return commands;
        }
        for (JsonElement item : element.getAsJsonArray()) {
            if (item.isJsonPrimitive()) {
                commands.add(new AICommand(item.getAsString(), "", null));
            }
            else if (item.isJsonObject()) {
                JsonObject command = item.getAsJsonObject();
                JsonObject input = object(command, "input");
                String hint = input == null ? "" : string(input, "hint");
                String name = orElse(string(command, "name"), orElse(string(command, "id"), string(command, "command")));
                if (!name.isEmpty()) {
                    commands.add(new AICommand(name, string(command, "description"), hint.isEmpty() ? null : hint));
                }
            }
        }
        return commands;
    }

    private static AIContextUsage contextUsage(JsonObject update) {
        JsonObject cost = object(update, "cost");
        JsonElement amount = cost == null ? null : cost.get("amount");
        return new AIContextUsage(
            number(update, "used"),
            number(update, "size"),
            amount != null && amount.isJsonPrimitive() && amount.getAsJsonPrimitive().isNumber() ? amount.getAsDouble() : null,
            cost == null ? null : nonEmpty(string(cost, "currency"), null)
        );
    }

    private static long number(JsonObject object, String name) {
        JsonElement element = object.get(name);
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber() ? element.getAsLong() : -1;
    }

    private static List<AISessionOption> configOptions(@Nullable JsonElement element) {
        List<AISessionOption> options = new ArrayList<>();
        if (element == null || !element.isJsonArray()) {
            return options;
        }
        for (JsonElement item : element.getAsJsonArray()) {
            if (!item.isJsonObject()) {
                continue;
            }
            JsonObject option = item.getAsJsonObject();
            String id = string(option, "id");
            if (id.isEmpty()) {
                continue;
            }
            List<AISessionOptionValue> values = new ArrayList<>();
            addValues(option.get("options"), values);
            String type = string(option, "type");
            options.add(new AISessionOption(
                id,
                orElse(string(option, "name"), id),
                nonEmpty(string(option, "description"), null),
                nonEmpty(string(option, "category"), null),
                type.isEmpty() ? "select" : type,
                nonEmpty(string(option, "currentValue"), null),
                List.copyOf(values)
            ));
        }
        return options;
    }

    /**
     * The values of a select, the ones of its groups too.
     */
    private static void addValues(@Nullable JsonElement element, List<AISessionOptionValue> values) {
        if (element == null || !element.isJsonArray()) {
            return;
        }
        for (JsonElement item : element.getAsJsonArray()) {
            if (!item.isJsonObject()) {
                continue;
            }
            JsonObject value = item.getAsJsonObject();
            if (value.has("value")) {
                String id = string(value, "value");
                values.add(new AISessionOptionValue(id, orElse(string(value, "name"), id), nonEmpty(string(value, "description"), null)));
            }
            else {
                addValues(value.get("options"), values);
            }
        }
    }

    /**
     * The {@code models} or {@code modes} an agent lists instead of a config option, as one.
     */
    private static @Nullable AISessionOption synthesizedOption(JsonObject block,
                                                               String listName,
                                                               String idName,
                                                               String currentName,
                                                               String optionId,
                                                               String optionName,
                                                               String category) {
        JsonElement available = block.get(listName);
        if (available == null || !available.isJsonArray()) {
            return null;
        }
        List<AISessionOptionValue> values = new ArrayList<>();
        for (JsonElement item : available.getAsJsonArray()) {
            if (item.isJsonObject()) {
                JsonObject value = item.getAsJsonObject();
                String id = string(value, idName);
                if (!id.isEmpty()) {
                    values.add(new AISessionOptionValue(id, orElse(string(value, "name"), id), nonEmpty(string(value, "description"), null)));
                }
            }
        }
        if (values.isEmpty()) {
            return null;
        }
        return new AISessionOption(optionId, optionName, null, category, "select", nonEmpty(string(block, currentName), null), List.copyOf(values));
    }

    private static AISessionOption withCurrentValue(AISessionOption option, String value) {
        return new AISessionOption(option.id(), option.name(), option.description(), option.category(), option.type(), value, option.values());
    }

    private static AIUsage usage(@Nullable JsonObject usage) {
        if (usage == null) {
            return AIUsage.UNKNOWN;
        }
        return new AIUsage(
            tokens(usage, "inputTokens", "input_tokens"),
            tokens(usage, "outputTokens", "output_tokens"),
            tokens(usage, "cachedReadTokens", "cached_read_tokens")
        );
    }

    private static int tokens(JsonObject usage, String name, String snakeName) {
        JsonElement element = usage.has(name) ? usage.get(name) : usage.get(snakeName);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            return -1;
        }
        return (int) Math.min(Integer.MAX_VALUE, element.getAsLong());
    }

    private static AIStopReason stopReason(String value) {
        return switch (value) {
            case "end_turn" -> AIStopReason.END_TURN;
            case "max_tokens", "max_turn_requests" -> AIStopReason.MAX_TOKENS;
            case "cancelled" -> AIStopReason.CANCELLED;
            default -> AIStopReason.OTHER;
        };
    }

    /**
     * An image block - its data in base64.
     */
    private static AIContent.@Nullable Image image(@Nullable JsonObject content) {
        if (content == null || !"image".equals(string(content, "type"))) {
            return null;
        }
        String data = string(content, "data");
        if (data.isEmpty()) {
            return null;
        }
        try {
            return AIContent.image(orElse(string(content, "mimeType"), "image/png"), Base64.getDecoder().decode(data));
        }
        catch (IllegalArgumentException e) {
            LOG.warn("The agent sent an image which is not base64", e);
            return null;
        }
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

    private static String orElse(String value, String fallback) {
        return value.isEmpty() ? fallback : value;
    }

    private static @Nullable String nonEmpty(String value, @Nullable String fallback) {
        return value.isEmpty() ? fallback : value;
    }
}
