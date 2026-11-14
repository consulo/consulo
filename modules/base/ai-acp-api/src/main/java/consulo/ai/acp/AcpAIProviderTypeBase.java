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

import com.google.gson.JsonObject;
import consulo.ai.AIChatCarryover;
import consulo.ai.AIChatInfo;
import consulo.ai.AIChatSessionListener;
import consulo.ai.AICommand;
import consulo.ai.AIContent;
import consulo.ai.AIEffortLevel;
import consulo.ai.AIMessage;
import consulo.ai.AIModel;
import consulo.ai.AIProvider;
import consulo.ai.AIProviderTypeWithChats;
import consulo.ai.AIRequest;
import consulo.ai.AIResponse;
import consulo.ai.AIRole;
import consulo.ai.AISessionOption;
import consulo.ai.AISessionOptionValue;
import consulo.ai.AIStreamListener;
import consulo.ai.acp.localize.AcpLocalize;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.process.ProcessHandlerBuilderFactory;
import consulo.process.cmd.GeneralCommandLine;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A provider type whose accounts are agents of the Agent Client Protocol: the agent runs its own tools and keeps its
 * own sessions, which are the chats. A type says only how to run its agent - a plugin for one agent, or the generic
 * type which runs a command the user gives.
 * <p>
 * One process runs per account and is started again when it died or the account changed its command. The models are the
 * values of the model option the sessions of an agent offer, and the efforts of a model the levels of its thought level
 * option - both are set on the session before a turn when the request asks for others.
 *
 * @author VISTALL
 * @since 2026-11-10
 */
public abstract class AcpAIProviderTypeBase implements AIProviderTypeWithChats {
    private static final Logger LOG = Logger.getInstance(AcpAIProviderTypeBase.class);

    private record Running(String commandLine, CompletableFuture<AcpAgent> agent) {
    }

    private final ProcessHandlerBuilderFactory myProcessHandlerBuilderFactory;
    private final Map<String, Running> myAgents = new ConcurrentHashMap<>();
    private final Map<String, List<AcpSessionListenerRegistration>> mySessionListeners = new ConcurrentHashMap<>();
    private volatile @Nullable List<AIModel> myModels;
    /**
     * The models a session of any agent of the type offered - done once one did.
     */
    private final CompletableFuture<List<AIModel>> myOfferedModels = new CompletableFuture<>();
    /**
     * The efforts of a model, as the agent offered them while the model was the current one of a session.
     */
    private final Map<String, List<AIEffortLevel>> myEffortLevels = new ConcurrentHashMap<>();

    protected AcpAIProviderTypeBase(ProcessHandlerBuilderFactory processHandlerBuilderFactory) {
        myProcessHandlerBuilderFactory = processHandlerBuilderFactory;
    }

    /**
     * How to run the agent of the account, null when the account can not run one yet.
     */
    protected abstract @Nullable GeneralCommandLine createCommandLine(AIProvider provider);

    /**
     * The {@code _meta} of the client capabilities the agent is started with - extensions of the protocol it knows.
     */
    protected @Nullable JsonObject getClientCapabilitiesMeta() {
        return null;
    }

    /**
     * Readies a started agent before any session - registers the extensions of one agent, signs in.
     */
    protected CompletableFuture<AcpAgent> prepareAgent(AIProvider provider, AcpAgent agent) {
        return CompletableFuture.completedFuture(agent);
    }

    /**
     * Sets what the account wants of the session before a turn, after the model and the effort of the request.
     */
    protected CompletableFuture<?> configureSession(AIProvider provider, AcpAgent agent, String sessionId) {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Until a session offered its models the agent picks the model itself, so a type offers one model which leaves the
     * choice to it.
     */
    @Override
    public List<AIModel> getModels() {
        List<AIModel> models = myModels;
        if (models == null) {
            models = List.of(new AcpModel(AcpModel.DEFAULT_ID, AcpLocalize.modelAgentDefault(), getId()));
            myModels = models;
        }
        return models;
    }

    /**
     * The models once a session of an agent offered them - the panel asks while no agent may run yet, so it hears of
     * them when the first session starts.
     */
    @Override
    public CompletableFuture<List<AIModel>> loadModels() {
        return myOfferedModels.isDone() ? CompletableFuture.completedFuture(getModels()) : myOfferedModels;
    }

    @Override
    public boolean requiresApiKey() {
        return false;
    }

    @Override
    public CompletableFuture<Boolean> isConfigured(AIProvider provider) {
        return CompletableFuture.completedFuture(createCommandLine(provider) != null);
    }

    /**
     * The running agent of the account, started when there is none.
     */
    public CompletableFuture<AcpAgent> getAgent(AIProvider provider) {
        GeneralCommandLine commandLine = createCommandLine(provider);
        if (commandLine == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("The account " + provider.getName() + " has no agent to run"));
        }
        // the environment map has no toString of its own
        String key = commandLine.getCommandLineString() + "|" + new TreeMap<>(commandLine.getEnvironment());
        Running running = myAgents.compute(provider.getName(), (name, current) -> {
            if (current != null && current.commandLine().equals(key) && !isDead(current.agent())) {
                return current;
            }
            if (current != null) {
                LOG.warn("Restarting the agent of " + name + (current.commandLine().equals(key) ? " - it is not alive" : " - its command line changed"));
                current.agent().thenAccept(AcpAgent::close);
            }
            CompletableFuture<AcpAgent> agent = AcpAgent.start(myProcessHandlerBuilderFactory, commandLine, getClientCapabilitiesMeta())
                .thenCompose(started -> {
                    started.addConfigListener((sessionId, options) -> configOffered(options));
                    // the chats listening on the account go on listening on its new process
                    for (AcpSessionListenerRegistration registration : mySessionListeners.getOrDefault(name, List.of())) {
                        registration.attach(started);
                    }
                    return prepareAgent(provider, started).whenComplete((prepared, error) -> {
                        if (error != null) {
                            started.close();
                        }
                    });
                });
            return new Running(key, agent);
        });
        return running.agent();
    }

    /**
     * The agent of the account when it runs already - asked where starting one is not worth it.
     */
    protected @Nullable AcpAgent getRunningAgent(AIProvider provider) {
        Running running = myAgents.get(provider.getName());
        if (running == null || !running.agent().isDone() || running.agent().isCompletedExceptionally()) {
            return null;
        }
        AcpAgent agent = running.agent().join();
        return agent.isAlive() ? agent : null;
    }

    /**
     * Stops the agent of the account, for example when it signs out.
     */
    public void stopAgent(AIProvider provider) {
        Running running = myAgents.remove(provider.getName());
        if (running != null) {
            running.agent().thenAccept(AcpAgent::close);
        }
    }

    /**
     * Stops the agents of every account - after the agent signed in or out, they start again with the new login.
     */
    public void stopAgents() {
        for (Running running : myAgents.values()) {
            running.agent().thenAccept(AcpAgent::close);
        }
        myAgents.clear();
    }

    // region models

    private void configOffered(List<AISessionOption> options) {
        AISessionOption model = findModelOption(options);
        if (model == null) {
            return;
        }
        String current = model.currentValue();
        AISessionOption effort = findEffortOption(options);
        if (current != null) {
            List<AIEffortLevel> levels = new ArrayList<>();
            if (effort != null) {
                for (AISessionOptionValue value : effort.values()) {
                    levels.add(new AIEffortLevel(value.value(), LocalizeValue.of(value.name())));
                }
            }
            myEffortLevels.put(current, List.copyOf(levels));
        }

        List<AIModel> models = new ArrayList<>();
        for (AISessionOptionValue value : model.values()) {
            String description = value.description();
            models.add(new AcpModel(
                value.value(),
                LocalizeValue.of(value.name()),
                description == null ? LocalizeValue.empty() : LocalizeValue.of(description),
                getId(),
                myEffortLevels.getOrDefault(value.value(), List.of())
            ));
        }
        List<AIModel> offered = List.copyOf(models);
        myModels = offered;
        myOfferedModels.complete(offered);
    }

    /**
     * Sets the model and the effort the request asks for on the session, when the session offers them and has others.
     * A setting the agent refuses leaves the one it has - the turn still goes on.
     */
    private CompletableFuture<?> applyRequest(AcpAgent agent, String sessionId, AIRequest request) {
        AISessionOption model = findModelOption(agent.getConfigOptions(sessionId));
        String modelId = request.getModel().getId();
        CompletableFuture<?> modelSet = CompletableFuture.completedFuture(null);
        if (model != null && !modelId.equals(model.currentValue()) && model.findValue(modelId) != null) {
            modelSet = setOption(agent, sessionId, model.id(), modelId);
        }
        String effort = request.getEffort();
        if (effort == null) {
            return modelSet;
        }
        return modelSet.thenCompose(ignored -> {
            // the efforts are the ones of the model now current
            AISessionOption option = findEffortOption(agent.getConfigOptions(sessionId));
            if (option == null || effort.equals(option.currentValue()) || option.findValue(effort) == null) {
                return CompletableFuture.completedFuture(null);
            }
            return setOption(agent, sessionId, option.id(), effort);
        });
    }

    /**
     * Sets an option of the session - a value the agent refuses is logged, and the session keeps the one it has.
     */
    protected static CompletableFuture<?> setOption(AcpAgent agent, String sessionId, String configId, String value) {
        return agent.setConfigOption(sessionId, configId, value).handle((options, error) -> {
            if (error != null) {
                LOG.warn("The agent refused " + configId + "=" + value + ": " + error.getMessage());
            }
            return null;
        });
    }

    private static @Nullable AISessionOption findModelOption(List<AISessionOption> options) {
        for (AISessionOption option : options) {
            if (option.isModel()) {
                return option;
            }
        }
        return null;
    }

    private static @Nullable AISessionOption findEffortOption(List<AISessionOption> options) {
        for (AISessionOption option : options) {
            if (option.isEffort()) {
                return option;
            }
        }
        return null;
    }

    // endregion

    private static boolean isDead(CompletableFuture<AcpAgent> agent) {
        return agent.isCompletedExceptionally() || agent.isDone() && !agent.join().isAlive();
    }

    // region chats

    @Override
    public CompletableFuture<String> newChat(AIProvider provider, Project project, @Nullable Path workingDirectory) {
        Path directory = workingDirectory != null ? workingDirectory : defaultDirectory(project);
        return getAgent(provider).thenCompose(agent -> agent.newSession(directory));
    }

    /**
     * An agent which can load a session keeps the chat; one which can not leaves it to the platform. While the agent
     * did not start yet it is taken to keep it - the platform reads its own copy when the agent has nothing.
     */
    @Override
    public boolean keepsChats(AIProvider provider) {
        AcpAgent agent = getRunningAgent(provider);
        return agent == null || agent.canLoad();
    }

    @Override
    public CompletableFuture<List<AIChatInfo>> getChats(AIProvider provider, Project project) {
        return getAgent(provider).thenCompose(agent -> agent.listSessions(defaultDirectory(project)));
    }

    @Override
    public CompletableFuture<List<AIMessage>> loadChat(AIProvider provider, Project project, String chatId) {
        return getAgent(provider).thenCompose(agent -> agent.loadSession(chatId, defaultDirectory(project))).thenApply(this::replayed);
    }

    /**
     * The history an agent replayed, as the chat shows it - an agent may tell the blocks of a prompt in its own words,
     * which a subclass turns back into what was sent.
     */
    protected List<AIMessage> replayed(List<AIMessage> history) {
        return history;
    }

    @Override
    public CompletableFuture<?> deleteChat(AIProvider provider, Project project, String chatId) {
        AcpAgent agent = getRunningAgent(provider);
        if (agent == null) {
            return getAgent(provider).thenCompose(started -> started.deleteSession(chatId));
        }
        return agent.deleteSession(chatId);
    }

    @Override
    public void closeSession(AIProvider provider, String sessionId) {
        AcpAgent agent = getRunningAgent(provider);
        if (agent != null) {
            agent.closeSession(sessionId);
        }
    }

    @Override
    public List<AISessionOption> getSessionOptions(AIProvider provider, String sessionId) {
        AcpAgent agent = getRunningAgent(provider);
        return sessionOptions(provider, agent == null ? List.of() : agent.getConfigOptions(agent.sessionOf(sessionId)));
    }

    /**
     * The settings a chat shows for a session - the ones of the agent, and any the provider keeps beside them.
     */
    protected List<AISessionOption> sessionOptions(AIProvider provider, List<AISessionOption> agentOptions) {
        return agentOptions;
    }

    @Override
    public CompletableFuture<List<AISessionOption>> setSessionOption(AIProvider provider, String sessionId, String optionId, String value) {
        AcpAgent agent = getRunningAgent(provider);
        if (agent == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("The agent of " + provider.getName() + " does not run"));
        }
        return agent.setConfigOption(agent.sessionOf(sessionId), optionId, value).thenApply(options -> sessionOptions(provider, options));
    }

    @Override
    public List<AICommand> getCommands(AIProvider provider, String sessionId) {
        AcpAgent agent = getRunningAgent(provider);
        return agent == null ? List.of() : agent.getCommands(agent.sessionOf(sessionId));
    }

    /**
     * Listens on the agent of the account once it runs - a chat panel subscribes before its first turn starts it. A
     * session the agent could not open again goes on in a new one, which the listener follows.
     */
    @Override
    public void addSessionListener(AIProvider provider, String sessionId, AIChatSessionListener listener, Disposable parentDisposable) {
        AcpSessionListenerRegistration registration = new AcpSessionListenerRegistration(sessionId, new AIChatSessionListener() {
            @Override
            public void optionsChanged(List<AISessionOption> options) {
                listener.optionsChanged(sessionOptions(provider, options));
            }

            @Override
            public void commandsChanged(List<AICommand> commands) {
                listener.commandsChanged(commands);
            }

            @Override
            public void sessionChanged(String newSessionId) {
                listener.sessionChanged(newSessionId);
            }
        });
        List<AcpSessionListenerRegistration> registrations = mySessionListeners.computeIfAbsent(provider.getName(), name -> new CopyOnWriteArrayList<>());
        registrations.add(registration);
        Disposer.register(parentDisposable, () -> {
            registrations.remove(registration);
            registration.remove();
        });
        getAgent(provider).thenAccept(registration::attach);
    }

    // endregion

    @Override
    public CompletableFuture<AIResponse> chat(AIProvider provider, AIRequest request, @Nullable AIStreamListener listener) {
        List<AIContent> text = lastUserContent(request);
        if (text == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Nothing to send - the request has no user text"));
        }
        Path workingDirectory = request.getWorkingDirectory() != null
            ? Objects.requireNonNull(request.getWorkingDirectory())
            : Platform.current().user().homePath();
        String chatId = request.getSessionId();

        // cancelling the answer cancels the turn of the agent, once the turn started
        AtomicReference<@Nullable CompletableFuture<AIResponse>> turn = new AtomicReference<>();
        CompletableFuture<AIResponse> answer = getAgent(provider).thenCompose(agent -> {
            CompletableFuture<AIResponse> first = turn(provider, agent, request, chatId, workingDirectory, text, listener, turn);
            if (chatId == null) {
                return first;
            }
            // a conversation the model can hold no more goes on in a new session which is told what was said
            return first.exceptionallyCompose(error -> {
                if (!isContextExhausted(error)) {
                    return CompletableFuture.failedFuture(error);
                }
                if (listener != null) {
                    listener.onStatus(AcpLocalize.statusContextExhausted());
                }
                agent.startOver(chatId);
                return turn(provider, agent, request, chatId, workingDirectory, text, listener, turn);
            });
        });
        answer.whenComplete((response, error) -> {
            CompletableFuture<AIResponse> prompt = turn.get();
            if (answer.isCancelled() && prompt != null) {
                prompt.cancel(false);
            }
        });
        return answer;
    }

    private CompletableFuture<AIResponse> turn(AIProvider provider,
                                               AcpAgent agent,
                                               AIRequest request,
                                               @Nullable String chatId,
                                               Path workingDirectory,
                                               List<AIContent> text,
                                               @Nullable AIStreamListener listener,
                                               AtomicReference<@Nullable CompletableFuture<AIResponse>> turn) {
        CompletableFuture<String> session = chatId == null ? agent.newSession(workingDirectory) : agent.openSession(chatId, workingDirectory);
        return session.thenCompose(sessionId -> applyRequest(agent, sessionId, request)
            .thenCompose(ignored -> configureSession(provider, agent, sessionId))
            .thenCompose(ignored -> {
                // a session which never saw the chat - the old one is gone, or the history changed - is told it first
                List<AIContent> content = text;
                if (chatId != null && agent.isFresh(sessionId)) {
                    String carryover = AIChatCarryover.build(request.getMessages());
                    if (carryover != null) {
                        content = new ArrayList<>();
                        content.add(AIContent.text(carryover));
                        content.addAll(text);
                    }
                }
                CompletableFuture<AIResponse> prompt = agent.prompt(sessionId, content, listener);
                turn.set(prompt);
                if (chatId == null) {
                    prompt.whenComplete((response, error) -> agent.closeSession(sessionId));
                }
                return prompt;
            }));
    }

    private static boolean isContextExhausted(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            String message = cause.getMessage();
            if (message == null) {
                continue;
            }
            String lower = message.toLowerCase(Locale.ROOT);
            if (lower.contains("resource_exhausted") || lower.contains("context_length_exceeded") || lower.contains("maximum context length")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void startOver(AIProvider provider, String sessionId, List<AIMessage> history) {
        AcpAgent agent = getRunningAgent(provider);
        if (agent != null) {
            agent.startOver(sessionId);
        }
    }

    protected static Path defaultDirectory(Project project) {
        String basePath = project.getBasePath();
        return basePath != null ? Path.of(basePath) : Platform.current().user().homePath();
    }

    /**
     * What the user said last - the text, and the files and selections it carries.
     */
    private static @Nullable List<AIContent> lastUserContent(AIRequest request) {
        List<AIMessage> messages = request.getMessages();
        for (int i = messages.size() - 1; i >= 0; i--) {
            AIMessage message = messages.get(i);
            if (message.role() != AIRole.USER) {
                continue;
            }
            List<AIContent> content = new ArrayList<>();
            for (AIContent part : message.content()) {
                if (part instanceof AIContent.Text || part instanceof AIContent.ResourceLink || part instanceof AIContent.Resource || part instanceof AIContent.Image) {
                    content.add(part);
                }
            }
            return content.isEmpty() ? null : content;
        }
        return null;
    }
}
