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

import consulo.localize.LocalizeValue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

/**
 * Incremental delivery of an answer. Called on a background thread, so implementations must hop to
 * the UI themselves before touching components.
 *
 * @author VISTALL
 * @since 2026-08-04
 */
@FunctionalInterface
public interface AIStreamListener {
    /**
     * A piece of the answer text. Fragments are additive - concatenating them yields the full text.
     */
    void onTextDelta(String delta);

    /**
     * A piece of the model's reasoning, when the model reports it separately.
     */
    default void onThinkingDelta(String delta) {
    }

    /**
     * The provider named the conversation - a title it generated. The title of a chat the user named stays.
     */
    default void onTitle(String title) {
    }

    /**
     * A complete tool request. Not streamed piecewise, since partial arguments are not parseable.
     */
    default void onToolUse(AIContent.ToolUse toolUse) {
    }

    /**
     * An image in the answer - a picture the model drew, a screenshot it took.
     */
    default void onImage(AIContent.Image image) {
    }

    /**
     * A tool the agent runs on its own side started or changed - the call with the same id replaces the one before.
     */
    default void onToolCall(AIContent.ToolCall toolCall) {
    }

    /**
     * A note about the turn while it runs - a tool which has been running for a while, say. The next note or answer
     * replaces it.
     */
    default void onStatus(LocalizeValue status) {
    }

    /**
     * The plan the agent works through - the whole of it, every time it changed.
     */
    default void onPlan(List<AIPlanEntry> entries) {
    }

    /**
     * The agent saved the plan of the turn to a file - the one to show when the user asks for the plan.
     */
    default void onPlanFile(Path file) {
    }

    /**
     * How much of its context window the session filled.
     */
    default void onContextUsage(AIContextUsage usage) {
    }

    /**
     * The model wants to run a tool which needs the user's consent. The answer is awaited, so the turn pauses until
     * the returned future completes. Without anyone to ask the tool is denied.
     */
    default CompletableFuture<AIToolPermissionDecision> onToolPermission(AIToolPermissionRequest request) {
        return CompletableFuture.completedFuture(AIToolPermissionDecision.deny("Nobody can approve the tool here"));
    }

    /**
     * The model asks the user one or more questions. The future completes with an answer per question
     * ({@link AIQuestion#question()} to answer, several choices joined by {@code ", "}), or fails when the user
     * declines to answer.
     */
    default CompletableFuture<Map<String, String>> onQuestion(List<AIQuestion> questions) {
        return CompletableFuture.failedFuture(new CancellationException("Nobody can answer here"));
    }
}
