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

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import consulo.logging.Logger;
import consulo.process.ExecutionException;
import consulo.process.ProcessHandler;
import consulo.process.ProcessHandlerBuilderFactory;
import consulo.process.ProcessOutputTypes;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.event.ProcessEvent;
import consulo.process.event.ProcessListener;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * JSON-RPC 2.0 with an agent process - a message a line on its standard input and output, the way the Agent Client
 * Protocol carries it. Requests of the agent are answered by the handler registered for their method.
 *
 * @author VISTALL
 * @since 2026-11-10
 */
public final class AcpConnection {
    private static final Logger LOG = Logger.getInstance(AcpConnection.class);

    private static final int STDERR_TAIL = 20;
    private static final int METHOD_NOT_FOUND = -32601;
    private static final int INTERNAL_ERROR = -32603;

    /**
     * An error the agent answered with.
     */
    public static final class AcpException extends RuntimeException {
        private final int myCode;

        public AcpException(int code, String message) {
            super(message);
            myCode = code;
        }

        public int getCode() {
            return myCode;
        }
    }

    private final ProcessHandler myHandler;
    private final AtomicLong myNextId = new AtomicLong();
    private final Map<Long, CompletableFuture<JsonElement>> myPending = new ConcurrentHashMap<>();
    private final Map<String, Function<JsonObject, CompletableFuture<JsonElement>>> myRequestHandlers = new ConcurrentHashMap<>();
    private final Map<String, Consumer<JsonObject>> myNotificationHandlers = new ConcurrentHashMap<>();
    private final StringBuilder myLine = new StringBuilder();
    private final Deque<String> myStderr = new ArrayDeque<>();
    private volatile boolean myTerminated;

    private AcpConnection(ProcessHandler handler) {
        myHandler = handler;
    }

    public static AcpConnection start(ProcessHandlerBuilderFactory factory, GeneralCommandLine commandLine) throws ExecutionException {
        ProcessHandler handler = factory.newBuilder(commandLine.withCharset(StandardCharsets.UTF_8)).killable().build();
        AcpConnection connection = new AcpConnection(handler);
        handler.addProcessListener(new ProcessListener() {
            @Override
            public void onTextAvailable(ProcessEvent event, Key outputType) {
                if (outputType == ProcessOutputTypes.STDOUT) {
                    connection.onStdout(event.getText());
                }
                else if (outputType == ProcessOutputTypes.STDERR) {
                    connection.onStderr(event.getText());
                }
            }

            @Override
            public void processTerminated(ProcessEvent event) {
                connection.onTerminated(event.getExitCode());
            }
        });
        handler.startNotify();
        return connection;
    }

    public void onRequest(String method, Function<JsonObject, CompletableFuture<JsonElement>> handler) {
        myRequestHandlers.put(method, handler);
    }

    public void onNotification(String method, Consumer<JsonObject> handler) {
        myNotificationHandlers.put(method, handler);
    }

    public CompletableFuture<JsonElement> request(String method, JsonObject params) {
        if (myTerminated) {
            return CompletableFuture.failedFuture(new IOException(terminatedMessage(null)));
        }
        long id = myNextId.incrementAndGet();
        CompletableFuture<JsonElement> result = new CompletableFuture<>();
        myPending.put(id, result);

        JsonObject message = new JsonObject();
        message.addProperty("jsonrpc", "2.0");
        message.addProperty("id", id);
        message.addProperty("method", method);
        message.add("params", params);
        try {
            write(message);
        }
        catch (IOException e) {
            myPending.remove(id);
            result.completeExceptionally(e);
        }
        return result;
    }

    public void notify(String method, JsonObject params) {
        JsonObject message = new JsonObject();
        message.addProperty("jsonrpc", "2.0");
        message.addProperty("method", method);
        message.add("params", params);
        try {
            write(message);
        }
        catch (IOException e) {
            LOG.warn("Cannot notify the agent of " + method, e);
        }
    }

    public boolean isAlive() {
        return !myTerminated && !myHandler.isProcessTerminated();
    }

    public void close() {
        if (!myHandler.isProcessTerminated()) {
            myHandler.destroyProcess();
        }
    }

    private void write(JsonObject message) throws IOException {
        OutputStream input = myHandler.getProcessInput();
        if (input == null) {
            throw new IOException("The agent has no input stream");
        }
        synchronized (input) {
            input.write((message + "\n").getBytes(StandardCharsets.UTF_8));
            input.flush();
        }
    }

    private void onStdout(String text) {
        List<String> lines = new ArrayList<>();
        synchronized (myLine) {
            myLine.append(text);
            int newLine;
            while ((newLine = myLine.indexOf("\n")) >= 0) {
                lines.add(myLine.substring(0, newLine));
                myLine.delete(0, newLine + 1);
            }
        }
        for (String line : lines) {
            if (!line.isBlank()) {
                onMessage(line);
            }
        }
    }

    private void onMessage(String line) {
        JsonObject message;
        try {
            message = JsonParser.parseString(line).getAsJsonObject();
        }
        catch (RuntimeException e) {
            // an agent may log to its standard output before it speaks the protocol
            LOG.debug("Not a JSON-RPC line from the agent: " + line);
            return;
        }

        JsonElement method = message.get("method");
        JsonElement id = message.get("id");
        JsonObject params = message.has("params") && message.get("params").isJsonObject() ? message.getAsJsonObject("params") : new JsonObject();
        if (method != null && method.isJsonPrimitive()) {
            if (id == null || id.isJsonNull()) {
                Consumer<JsonObject> handler = myNotificationHandlers.get(method.getAsString());
                if (handler != null) {
                    try {
                        handler.accept(params);
                    }
                    catch (RuntimeException e) {
                        LOG.warn("Cannot handle " + method.getAsString() + " of the agent", e);
                    }
                }
                return;
            }
            onAgentRequest(id, method.getAsString(), params);
            return;
        }

        if (id == null || !id.isJsonPrimitive()) {
            return;
        }
        CompletableFuture<JsonElement> pending = myPending.remove(id.getAsLong());
        if (pending == null) {
            return;
        }
        JsonElement error = message.get("error");
        if (error != null && error.isJsonObject()) {
            JsonObject object = error.getAsJsonObject();
            int code = object.has("code") ? object.get("code").getAsInt() : INTERNAL_ERROR;
            String text = object.has("message") ? object.get("message").getAsString() : "Error " + code;
            pending.completeExceptionally(new AcpException(code, text));
            return;
        }
        JsonElement result = message.get("result");
        pending.complete(result == null ? JsonNull.INSTANCE : result);
    }

    private void onAgentRequest(JsonElement id, String method, JsonObject params) {
        Function<JsonObject, CompletableFuture<JsonElement>> handler = myRequestHandlers.get(method);
        if (handler == null) {
            respondError(id, METHOD_NOT_FOUND, "The client does not support " + method);
            return;
        }
        CompletableFuture<JsonElement> answer;
        try {
            answer = handler.apply(params);
        }
        catch (RuntimeException e) {
            answer = CompletableFuture.failedFuture(e);
        }
        answer.whenComplete((result, error) -> {
            if (error != null) {
                respondError(id, INTERNAL_ERROR, String.valueOf(error.getMessage()));
                return;
            }
            JsonObject response = new JsonObject();
            response.addProperty("jsonrpc", "2.0");
            response.add("id", id);
            response.add("result", result == null ? JsonNull.INSTANCE : result);
            send(response);
        });
    }

    private void respondError(JsonElement id, int code, String text) {
        JsonObject error = new JsonObject();
        error.addProperty("code", code);
        error.addProperty("message", text);
        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");
        response.add("id", id);
        response.add("error", error);
        send(response);
    }

    private void send(JsonObject message) {
        try {
            write(message);
        }
        catch (IOException e) {
            LOG.warn("Cannot answer the agent", e);
        }
    }

    private synchronized void onStderr(String text) {
        for (String line : text.split("\\R")) {
            if (!line.isBlank()) {
                myStderr.addLast(line);
                if (myStderr.size() > STDERR_TAIL) {
                    myStderr.removeFirst();
                }
            }
        }
    }

    private void onTerminated(int exitCode) {
        myTerminated = true;
        IOException error = new IOException(terminatedMessage(exitCode));
        for (CompletableFuture<JsonElement> pending : myPending.values()) {
            pending.completeExceptionally(error);
        }
        myPending.clear();
    }

    private synchronized String terminatedMessage(@Nullable Integer exitCode) {
        StringBuilder message = new StringBuilder("The agent exited");
        if (exitCode != null) {
            message.append(" with code ").append(exitCode);
        }
        if (!myStderr.isEmpty()) {
            message.append(": ").append(String.join("\n", myStderr));
        }
        return message.toString();
    }
}
