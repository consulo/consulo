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

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-08-04
 */
public final class AIRequest {
    public static Builder builder(AIModel model) {
        return new Builder(model);
    }

    public static final class Builder {
        private final AIModel myModel;
        private final List<AIMessage> myMessages = new ArrayList<>();
        private final List<AITool> myTools = new ArrayList<>();

        private @Nullable String mySystemPrompt;
        private int myMaxTokens = -1;
        private double myTemperature = -1;

        private Builder(AIModel model) {
            myModel = model;
        }

        public Builder systemPrompt(String systemPrompt) {
            mySystemPrompt = systemPrompt;
            return this;
        }

        public Builder message(AIMessage message) {
            myMessages.add(message);
            return this;
        }

        public Builder messages(List<AIMessage> messages) {
            myMessages.addAll(messages);
            return this;
        }

        public Builder tool(AITool tool) {
            myTools.add(tool);
            return this;
        }

        public Builder tools(List<AITool> tools) {
            myTools.addAll(tools);
            return this;
        }

        /**
         * Negative leaves the provider's own default in place.
         */
        public Builder maxTokens(int maxTokens) {
            myMaxTokens = maxTokens;
            return this;
        }

        /**
         * Negative leaves the provider's own default in place.
         */
        public Builder temperature(double temperature) {
            myTemperature = temperature;
            return this;
        }

        public AIRequest build() {
            return new AIRequest(myModel, mySystemPrompt, myMessages, myTools, myMaxTokens, myTemperature);
        }
    }

    private final AIModel myModel;
    private final @Nullable String mySystemPrompt;
    private final List<AIMessage> myMessages;
    private final List<AITool> myTools;
    private final int myMaxTokens;
    private final double myTemperature;

    private AIRequest(AIModel model,
                      @Nullable String systemPrompt,
                      List<AIMessage> messages,
                      List<AITool> tools,
                      int maxTokens,
                      double temperature) {
        myModel = model;
        mySystemPrompt = systemPrompt;
        myMessages = List.copyOf(messages);
        myTools = List.copyOf(tools);
        myMaxTokens = maxTokens;
        myTemperature = temperature;
    }

    public AIModel getModel() {
        return myModel;
    }

    public @Nullable String getSystemPrompt() {
        return mySystemPrompt;
    }

    public List<AIMessage> getMessages() {
        return myMessages;
    }

    public List<AITool> getTools() {
        return myTools;
    }

    public int getMaxTokens() {
        return myMaxTokens;
    }

    public double getTemperature() {
        return myTemperature;
    }
}
