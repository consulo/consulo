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

import consulo.ai.AIChatInfo;
import consulo.ai.AIContent;
import consulo.ai.AIMessage;
import consulo.ai.AIModel;
import consulo.ai.AIProvider;
import consulo.ai.AIProviderTypeWithChats;
import consulo.ai.AIRequest;
import consulo.ai.AIResponse;
import consulo.ai.AIRole;
import consulo.ai.AIStreamListener;
import consulo.ai.acp.localize.AcpLocalize;
import consulo.platform.Platform;
import consulo.process.ProcessHandlerBuilderFactory;
import consulo.process.cmd.GeneralCommandLine;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A provider type whose accounts are agents of the Agent Client Protocol: the agent runs its own tools and keeps its
 * own sessions, which are the chats. A type says only how to run its agent - a plugin for one agent, or the generic
 * type which runs a command the user gives.
 * <p>
 * One process runs per account and is started again when it died or the account changed its command.
 *
 * @author VISTALL
 * @since 2026-11-10
 */
public abstract class AcpAIProviderTypeBase implements AIProviderTypeWithChats {
    private record Running(String commandLine, CompletableFuture<AcpAgent> agent) {
    }

    private final ProcessHandlerBuilderFactory myProcessHandlerBuilderFactory;
    private final Map<String, Running> myAgents = new ConcurrentHashMap<>();
    private volatile @Nullable List<AIModel> myModels;

    protected AcpAIProviderTypeBase(ProcessHandlerBuilderFactory processHandlerBuilderFactory) {
        myProcessHandlerBuilderFactory = processHandlerBuilderFactory;
    }

    /**
     * How to run the agent of the account, null when the account can not run one yet.
     */
    protected abstract @Nullable GeneralCommandLine createCommandLine(AIProvider provider);

    /**
     * The agent picks its model itself, so a type offers one model which leaves the choice to it.
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
        String key = commandLine.getCommandLineString() + "|" + commandLine.getEnvironment();
        Running running = myAgents.compute(provider.getName(), (name, current) -> {
            if (current != null && current.commandLine().equals(key) && !isDead(current.agent())) {
                return current;
            }
            if (current != null) {
                current.agent().thenAccept(AcpAgent::close);
            }
            return new Running(key, AcpAgent.start(myProcessHandlerBuilderFactory, commandLine));
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
        return getAgent(provider).thenCompose(agent -> agent.loadSession(chatId, defaultDirectory(project)));
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

    // endregion

    @Override
    public CompletableFuture<AIResponse> chat(AIProvider provider, AIRequest request, @Nullable AIStreamListener listener) {
        String text = lastUserText(request);
        if (text == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Nothing to send - the request has no user text"));
        }
        Path workingDirectory = request.getWorkingDirectory() != null
            ? Objects.requireNonNull(request.getWorkingDirectory())
            : Platform.current().user().homePath();
        String chatId = request.getSessionId();

        return getAgent(provider).thenCompose(agent -> {
            CompletableFuture<String> session = chatId == null ? agent.newSession(workingDirectory) : agent.openSession(chatId, workingDirectory);
            return session.thenCompose(sessionId -> {
                CompletableFuture<AIResponse> answer = agent.prompt(sessionId, text, listener);
                if (chatId == null) {
                    answer.whenComplete((response, error) -> agent.closeSession(sessionId));
                }
                return answer;
            });
        });
    }

    protected static Path defaultDirectory(Project project) {
        String basePath = project.getBasePath();
        return basePath != null ? Path.of(basePath) : Platform.current().user().homePath();
    }

    private static @Nullable String lastUserText(AIRequest request) {
        List<AIMessage> messages = request.getMessages();
        for (int i = messages.size() - 1; i >= 0; i--) {
            AIMessage message = messages.get(i);
            if (message.role() != AIRole.USER) {
                continue;
            }
            StringBuilder text = new StringBuilder();
            for (AIContent content : message.content()) {
                if (content instanceof AIContent.Text part) {
                    text.append(part.text());
                }
            }
            return text.isEmpty() ? null : text.toString();
        }
        return null;
    }
}
