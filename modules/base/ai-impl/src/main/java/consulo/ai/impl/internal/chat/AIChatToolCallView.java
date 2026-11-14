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
package consulo.ai.impl.internal.chat;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import consulo.ai.AIChangesViewer;
import consulo.ai.AIContent;
import consulo.ai.AIToolCallContent;
import consulo.ai.AIToolCallStatus;
import consulo.ai.localize.AILocalize;
import consulo.localize.LocalizeValue;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.Component;
import consulo.ui.Hyperlink;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.HorizontalLayoutStyle;
import consulo.ui.messageList.MessageListItemPart;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A tool call of an agent in its answer: a header line - how far the call is, what it does, the file it works on and
 * how many lines it changed, which opens the change in the diff viewer - over what it produced, which the header shows
 * and hides.
 *
 * @author VISTALL
 * @since 2026-11-13
 */
final class AIChatToolCallView {
    private static final int MAX_OUTPUT_CHARS = 20_000;

    private final Project myProject;
    private final Consumer<String> myFeedback;
    private final Runnable myChanged;
    private final HorizontalLayout myHeader = HorizontalLayout.create(Space.SMALL);
    private final Label myStatus = Label.create();
    private final Label myTitle = Label.create();
    private final Hyperlink myFile;
    private final Button myChanges;
    private final Hyperlink myToggle;

    private AIContent.ToolCall myToolCall;
    private final List<Component> myImages = new ArrayList<>();
    private @Nullable Boolean myExpanded;
    private @Nullable String myFilePath;

    /**
     * @param changed asked to show the parts again - the call was shown or hidden
     */
    @RequiredUIAccess
    AIChatToolCallView(Project project, AIContent.ToolCall toolCall, Runnable changed, Consumer<String> feedback) {
        myFeedback = feedback;
        myProject = project;
        myChanged = changed;
        myToolCall = toolCall;

        myFile = Hyperlink.create(LocalizeValue.empty(), event -> openFile());
        myToggle = Hyperlink.create(LocalizeValue.empty(), event -> {
            myExpanded = !isExpanded();
            updateHeader();
            myChanged.run();
        });
        myChanges = Button.create(LocalizeValue.empty(), event -> AIChangesViewer.getInstance().show(myProject, diffs(myToolCall), 0, myFeedback));
        myChanges.addStyle(ButtonStyle.INPLACE);
        myChanges.setToolTipText(AILocalize.buttonShowChanges());

        myHeader.addStyle(HorizontalLayoutStyle.TRANSPARENT_BACKGROUND);
        myHeader.add(myStatus);
        myHeader.add(myTitle);
        myHeader.add(myFile);
        myHeader.add(myChanges);
        myHeader.add(myToggle);
        updateHeader();
    }

    private boolean myStopped;

    AIContent.ToolCall getToolCall() {
        return myToolCall;
    }

    @RequiredUIAccess
    void update(AIContent.ToolCall toolCall) {
        myToolCall = toolCall;
        updateHeader();
    }

    /**
     * The answer ended while the call still ran - it never comes to an end, so it stops spinning.
     */
    @RequiredUIAccess
    void stop() {
        AIToolCallStatus status = myToolCall.status();
        if (status == AIToolCallStatus.PENDING || status == AIToolCallStatus.IN_PROGRESS) {
            myStopped = true;
            updateHeader();
        }
    }

    /**
     * The header, and what the call produced while it is shown.
     */
    List<MessageListItemPart> getParts() {
        List<MessageListItemPart> parts = new ArrayList<>();
        parts.add(MessageListItemPart.component(myHeader));
        if (isExpanded()) {
            String body = body();
            if (!body.isEmpty()) {
                parts.add(MessageListItemPart.text(body));
            }
            for (Component image : images()) {
                parts.add(MessageListItemPart.component(image));
            }
        }
        return parts;
    }

    @RequiredUIAccess
    private void updateHeader() {
        AIContent.ToolCall toolCall = myToolCall;
        myStatus.setImage(myStopped ? PlatformIconGroup.runconfigurationsTestterminated() : statusImage(toolCall.status()));
        myTitle.setText(LocalizeValue.of(relative(toolCall.title())));

        myFilePath = filePath(toolCall);
        myFile.setText(myFilePath == null ? LocalizeValue.empty() : LocalizeValue.of(Path.of(myFilePath).getFileName().toString()));
        myFile.setToolTipText(myFilePath == null ? LocalizeValue.empty() : LocalizeValue.of(myFilePath));
        myFile.setVisible(myFilePath != null && !toolCall.title().contains(Path.of(myFilePath).getFileName().toString()));

        List<AIToolCallContent.Diff> diffs = diffs(toolCall);
        myChanges.setText(changeCount(diffs));
        myChanges.setVisible(!diffs.isEmpty());

        myToggle.setText(isExpanded() ? AILocalize.linkHide() : AILocalize.linkShow());
        myToggle.setVisible(!body().isEmpty() || myToolCall.content().stream().anyMatch(content -> content instanceof AIToolCallContent.Image));
    }

    /**
     * A call which produced a picture shows it at once - the picture is what it was for.
     */
    private boolean isExpanded() {
        Boolean expanded = myExpanded;
        if (expanded != null) {
            return expanded;
        }
        return myToolCall.content().stream().anyMatch(content -> content instanceof AIToolCallContent.Image);
    }

    /**
     * The pictures of the call, made once - a call is shown again on every update of the answer.
     */
    @RequiredUIAccess
    private List<Component> images() {
        List<Component> images = new ArrayList<>();
        int index = 0;
        for (AIToolCallContent content : myToolCall.content()) {
            if (content instanceof AIToolCallContent.Image image) {
                if (index >= myImages.size()) {
                    Component component = AIChatAnswer.imageComponent(AIContent.image(image.mediaType(), image.data()));
                    if (component == null) {
                        continue;
                    }
                    myImages.add(component);
                }
                images.add(myImages.get(index));
                index++;
            }
        }
        return images;
    }

    static List<AIToolCallContent.Diff> diffs(AIContent.ToolCall toolCall) {
        List<AIToolCallContent.Diff> diffs = new ArrayList<>();
        for (AIToolCallContent content : toolCall.content()) {
            if (content instanceof AIToolCallContent.Diff diff) {
                diffs.add(diff);
            }
        }
        return diffs;
    }

    /**
     * How many lines the changes add and remove, as {@code +3 -1}.
     */
    static LocalizeValue changeCount(List<AIToolCallContent.Diff> diffs) {
        int added = 0;
        int removed = 0;
        for (AIToolCallContent.Diff diff : diffs) {
            AIChatLineDiff lineDiff = AIChatLineDiff.of(diff.oldText(), diff.newText());
            added += lineDiff.added();
            removed += lineDiff.removed();
        }
        return LocalizeValue.of("+" + added + " \u2212" + removed);
    }

    /**
     * What the call produced as markdown - its changes as diffs, its text, the output of a command it ran.
     */
    private String body() {
        StringBuilder body = new StringBuilder();
        for (AIToolCallContent content : myToolCall.content()) {
            switch (content) {
                case AIToolCallContent.Diff diff -> {
                    // the changes open in the diff viewer
                }
                case AIToolCallContent.Text text -> body.append(text.text()).append('\n');
                case AIToolCallContent.Terminal terminal -> body.append(AILocalize.labelTerminal(terminal.terminalId()).get()).append('\n');
                case AIToolCallContent.Image image -> {
                    // shown as a picture under the text
                }
            }
        }
        String output = output(myToolCall.outputJson());
        if (!output.isEmpty() && body.indexOf(output) < 0) {
            body.append(output);
        }
        return body.toString().strip();
    }

    /**
     * The output a command of the agent printed and how it ended, when the call tells them as {@code stdout},
     * {@code stderr} and {@code exitCode}; other output as it is.
     */
    private static String output(String outputJson) {
        if (outputJson.isBlank()) {
            return "";
        }
        JsonElement element;
        try {
            element = JsonParser.parseString(outputJson);
        }
        catch (RuntimeException e) {
            return fenced(outputJson);
        }
        if (!element.isJsonObject()) {
            return element.isJsonPrimitive() ? fenced(element.getAsString()) : "";
        }
        JsonObject object = element.getAsJsonObject();
        if (!object.has("stdout") && !object.has("stderr") && !object.has("exitCode")) {
            return "";
        }
        StringBuilder output = new StringBuilder();
        String text = (string(object, "stdout") + "\n" + string(object, "stderr")).strip();
        if (!text.isEmpty()) {
            output.append(fenced(text));
        }
        JsonElement exitCode = object.get("exitCode");
        if (exitCode != null && exitCode.isJsonPrimitive() && exitCode.getAsInt() != 0) {
            output.append(AILocalize.labelExitCode(exitCode.getAsInt()).get()).append('\n');
        }
        return output.toString();
    }

    private static String fenced(String text) {
        String shown = text.length() > MAX_OUTPUT_CHARS ? text.substring(0, MAX_OUTPUT_CHARS) + "\n..." : text;
        return "```\n" + shown.strip() + "\n```\n";
    }

    private static String string(JsonObject object, String name) {
        JsonElement element = object.get(name);
        return element == null || !element.isJsonPrimitive() ? "" : element.getAsString();
    }

    /**
     * The text with the paths in the project made relative to it - an agent tells absolute paths.
     */
    private String relative(String text) {
        String basePath = myProject.getBasePath();
        return basePath == null ? text : text.replace(basePath + "/", "");
    }

    private static @Nullable String filePath(AIContent.ToolCall toolCall) {
        if (!toolCall.locations().isEmpty()) {
            return toolCall.locations().getFirst();
        }
        for (AIToolCallContent content : toolCall.content()) {
            if (content instanceof AIToolCallContent.Diff diff && !diff.path().isEmpty()) {
                return diff.path();
            }
        }
        return null;
    }

    private static Image statusImage(AIToolCallStatus status) {
        return switch (status) {
            case PENDING, IN_PROGRESS -> Image.busy();
            case COMPLETED -> PlatformIconGroup.runconfigurationsTestpassed();
            case FAILED -> PlatformIconGroup.runconfigurationsTestfailed();
        };
    }

    @RequiredUIAccess
    private void openFile() {
        String path = myFilePath;
        if (path == null) {
            return;
        }
        // the agent wrote the file on its own, past the file system of the IDE
        VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByPath(path);
        if (file != null) {
            OpenFileDescriptorFactory.getInstance(myProject).builder(file).build().navigate(true);
        }
    }
}
