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
package consulo.ai.impl.internal.debug;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.builtinWebServer.BuiltInServerManager;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The runtime evidence of Debug mode - a log file per chat, written by the code the agent instruments, directly or
 * through an address of the built-in server for code which cannot write files. Every Debug turn starts a fresh log.
 * Reads and writes the disk - never on the ui thread.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class AIDebugLogManager {
    static final String PATH = "ai-debug";

    private static final String DIRECTORY = ".consulo/ai-debug";
    private static final long MAX_FILE_BYTES = 2L * 1024 * 1024;
    private static final String TOKEN_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int TOKEN_LENGTH = 24;

    private final Provider<BuiltInServerManager> myServerManager;
    private final SecureRandom myRandom = new SecureRandom();
    private final Map<String, AIDebugCapture> myCaptures = new ConcurrentHashMap<>();

    @Inject
    public AIDebugLogManager(Provider<BuiltInServerManager> serverManager) {
        myServerManager = serverManager;
    }

    /**
     * Empties the log of the chat - or creates it - for a new Debug turn.
     */
    public AIDebugEndpoint beginTurn(Path directory, String chatId) throws IOException {
        String id = sanitize(chatId);
        Path logFile = directory.resolve(DIRECTORY).resolve(id + ".log");
        Files.createDirectories(logFile.getParent());
        Files.writeString(logFile, "", StandardCharsets.UTF_8);

        String token = token();
        myCaptures.put(id, new AIDebugCapture(token, logFile));

        String url = null;
        BuiltInServerManager serverManager = myServerManager.get().waitForStart();
        int port = serverManager.getPort();
        if (port > 0) {
            url = "http://127.0.0.1:" + port + "/" + PATH + "/log/" + id + "/" + token;
        }
        return new AIDebugEndpoint(logFile, url);
    }

    /**
     * The log of the last Debug turn of the chat.
     */
    public @Nullable Path getLogFile(Path directory, String chatId) {
        String id = sanitize(chatId);
        AIDebugCapture capture = myCaptures.get(id);
        if (capture != null) {
            return capture.logFile();
        }
        Path logFile = directory.resolve(DIRECTORY).resolve(id + ".log");
        return Files.isRegularFile(logFile) ? logFile : null;
    }

    /**
     * Appends a line the instrumented code sent - false when the address is not of a running capture.
     */
    boolean append(String id, String token, String body) throws IOException {
        AIDebugCapture capture = find(id, token);
        if (capture == null) {
            return false;
        }
        String line = body.endsWith("\n") ? body : body + "\n";
        synchronized (capture) {
            Path logFile = capture.logFile();
            if (Files.exists(logFile) && Files.size(logFile) + line.length() > MAX_FILE_BYTES) {
                // the newest half stays - the start of a run is the least of its evidence
                String text = Files.readString(logFile, StandardCharsets.UTF_8);
                Files.writeString(logFile, text.substring(text.length() / 2) + line, StandardCharsets.UTF_8);
            }
            else {
                Files.writeString(logFile, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
        }
        return true;
    }

    @Nullable String read(String id, String token) throws IOException {
        AIDebugCapture capture = find(id, token);
        if (capture == null) {
            return null;
        }
        synchronized (capture) {
            return Files.exists(capture.logFile()) ? Files.readString(capture.logFile(), StandardCharsets.UTF_8) : "";
        }
    }

    private @Nullable AIDebugCapture find(String id, String token) {
        AIDebugCapture capture = myCaptures.get(id);
        return capture != null && capture.token().equals(token) ? capture : null;
    }

    private String token() {
        char[] chars = new char[TOKEN_LENGTH];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = TOKEN_ALPHABET.charAt(myRandom.nextInt(TOKEN_ALPHABET.length()));
        }
        return new String(chars);
    }

    static String sanitize(String chatId) {
        StringBuilder builder = new StringBuilder();
        for (char c : chatId.strip().toCharArray()) {
            builder.append(Character.isLetterOrDigit(c) || c == '-' || c == '_' ? c : '-');
        }
        String id = builder.length() > 80 ? builder.substring(0, 80) : builder.toString();
        return id.isEmpty() ? "chat" : id;
    }
}
