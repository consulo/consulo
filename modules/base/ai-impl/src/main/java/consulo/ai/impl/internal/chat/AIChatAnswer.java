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

import consulo.ai.AIChangesViewer;
import consulo.ai.AIContent;
import consulo.ai.AIToolCallContent;
import consulo.ai.localize.AILocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.logging.Logger;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.Component;
import consulo.ui.ImageBox;
import consulo.ui.Label;
import consulo.ui.LabelStyle;
import consulo.ui.Space;
import consulo.ui.Hyperlink;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.HorizontalLayoutStyle;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.messageList.MessageListItem;
import consulo.ui.messageList.MessageListItemPart;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * One answer of the assistant as it comes: its text, what it thought, the tools it ran, in the order they came. While
 * the answer is text alone it streams into the item; once it holds more the item shows it as parts, shown again at
 * most every {@value #RENDER_DELAY_MILLIS} ms while text streams in.
 *
 * @author VISTALL
 * @since 2026-11-13
 */
final class AIChatAnswer {
    private static final Logger LOG = Logger.getInstance(AIChatAnswer.class);

    private static final long RENDER_DELAY_MILLIS = 120;
    private static final int MAX_IMAGE_WIDTH = 480;

    private sealed interface Segment {
    }

    private static final class TextSegment implements Segment {
        final StringBuilder myText = new StringBuilder();
    }

    private static final class ThinkingSegment implements Segment {
        final StringBuilder myText = new StringBuilder();
        boolean myExpanded;
        @Nullable Hyperlink myToggle;
    }

    private record ToolCallSegment(AIChatToolCallView view) implements Segment {
    }

    private record ImageSegment(Component image) implements Segment {
    }

    private final Project myProject;
    private final UIAccess myUIAccess;
    private final MessageListItem myItem;
    private final List<Segment> mySegments = new ArrayList<>();
    private final Map<String, AIChatToolCallView> myToolCalls = new LinkedHashMap<>();
    /**
     * Whether the item shows parts rather than its text.
     */
    private boolean myParts;
    private @Nullable ThinkingSegment myThinking;
    /**
     * Whether something else came since the model last thought - the next thought starts a paragraph of its own.
     */
    private boolean myThinkingInterrupted;
    private boolean myRenderScheduled;
    /**
     * The files the answer changed, under it - the count of the lines opens the list of the files.
     */
    private final VerticalLayout myChanges = VerticalLayout.create(Space.X_SMALL);
    private final Button myChangesButton;
    private final VerticalLayout myChangedFiles = VerticalLayout.create(Space.X_SMALL);
    private boolean myChangesExpanded;

    @RequiredUIAccess
    AIChatAnswer(Project project, UIAccess uiAccess, MessageListItem item) {
        myProject = project;
        myUIAccess = uiAccess;
        myItem = item;

        myChangesButton = Button.create(LocalizeValue.empty(), event -> {
            myChangesExpanded = !myChangesExpanded;
            render();
        });
        myChangesButton.addStyle(ButtonStyle.INPLACE);
        // a row keeps the button at its own width - a vertical layout stretches it, and its text lands in the middle
        HorizontalLayout changesRow = HorizontalLayout.create();
        changesRow.addStyle(HorizontalLayoutStyle.TRANSPARENT_BACKGROUND);
        changesRow.add(myChangesButton);
        myChanges.add(changesRow);
        myChanges.add(myChangedFiles);
    }

    MessageListItem getItem() {
        return myItem;
    }

    boolean isEmpty() {
        return mySegments.isEmpty();
    }

    @RequiredUIAccess
    void appendText(String delta) {
        myThinkingInterrupted = true;
        TextSegment segment;
        if (lastSegment() instanceof TextSegment last) {
            segment = last;
        }
        else {
            segment = new TextSegment();
            mySegments.add(segment);
        }
        segment.myText.append(delta);
        if (myParts) {
            scheduleRender();
        }
        else {
            myItem.appendText(delta);
        }
    }

    /**
     * The reasoning of the whole answer goes into one block at its top - a model thinks a little before every tool,
     * and a block each would bury the answer. The block lives only while the model works, see {@link #finish()}.
     */
    @RequiredUIAccess
    void appendThinking(String delta) {
        ThinkingSegment segment = myThinking;
        if (segment == null) {
            ThinkingSegment thinking = new ThinkingSegment();
            thinking.myToggle = Hyperlink.create(AILocalize.linkThinking(), event -> {
                thinking.myExpanded = !thinking.myExpanded;
                render();
            });
            segment = thinking;
            myThinking = thinking;
            mySegments.addFirst(thinking);
            toParts();
        }
        else if (myThinkingInterrupted && !segment.myText.isEmpty()) {
            segment.myText.append("\n\n");
        }
        myThinkingInterrupted = false;
        segment.myText.append(delta);
        if (segment.myExpanded) {
            scheduleRender();
        }
    }

    @RequiredUIAccess
    void image(AIContent.Image image) {
        Component component = imageComponent(image);
        if (component == null) {
            return;
        }
        myThinkingInterrupted = true;
        mySegments.add(new ImageSegment(component));
        toParts();
    }

    /**
     * The image no wider than {@value #MAX_IMAGE_WIDTH} - a screenshot would make the chat scroll sideways.
     */
    @RequiredUIAccess
    static @Nullable Component imageComponent(AIContent.Image image) {
        Image.ImageType type = switch (image.mediaType()) {
            case "image/jpeg", "image/jpg" -> Image.ImageType.JPEG;
            case "image/gif" -> Image.ImageType.GIF;
            case "image/svg+xml" -> Image.ImageType.SVG;
            default -> Image.ImageType.PNG;
        };
        try {
            Image picture = Image.fromBytes(type, image.data());
            int width = picture.getWidth();
            if (width > MAX_IMAGE_WIDTH && !type.vector()) {
                picture = Image.fromBytes(type, image.data(), MAX_IMAGE_WIDTH, Math.max(1, picture.getHeight() * MAX_IMAGE_WIDTH / width));
            }
            // a row keeps the image at its own size and on the left - a frontend may stretch a part over the width
            HorizontalLayout row = HorizontalLayout.create();
            row.addStyle(HorizontalLayoutStyle.TRANSPARENT_BACKGROUND);
            row.add(ImageBox.create(picture));
            return row;
        }
        catch (IOException e) {
            LOG.warn("Cannot show an image of the answer", e);
            return null;
        }
    }

    @RequiredUIAccess
    void toolCall(AIContent.ToolCall toolCall) {
        myThinkingInterrupted = true;
        AIChatToolCallView view = myToolCalls.get(toolCall.id());
        if (view != null) {
            view.update(toolCall);
            render();
            return;
        }
        view = new AIChatToolCallView(myProject, toolCall, this::render);
        myToolCalls.put(toolCall.id(), view);
        mySegments.add(new ToolCallSegment(view));
        toParts();
    }

    /**
     * Shows a message of the history - its blocks in order.
     */
    @RequiredUIAccess
    void show(List<AIContent> content) {
        for (AIContent part : content) {
            switch (part) {
                case AIContent.Text text -> appendText(text.text());
                case AIContent.ToolCall toolCall -> toolCall(toolCall);
                case AIContent.Image image -> image(image);
                default -> {
                }
            }
        }
        render();
    }

    /**
     * The answer is complete - what still waits to be shown is shown now. The reasoning goes away: it told the user
     * what the model was busy with, the answer says what came of it.
     */
    @RequiredUIAccess
    void finish() {
        ThinkingSegment thinking = myThinking;
        if (thinking != null) {
            mySegments.remove(thinking);
            myThinking = null;
        }
        if (myParts) {
            render();
        }
    }

    private @Nullable Segment lastSegment() {
        return mySegments.isEmpty() ? null : mySegments.getLast();
    }

    @RequiredUIAccess
    private void toParts() {
        myParts = true;
        render();
    }

    private void scheduleRender() {
        if (myRenderScheduled) {
            return;
        }
        myRenderScheduled = true;
        CompletableFuture.delayedExecutor(RENDER_DELAY_MILLIS, TimeUnit.MILLISECONDS).execute(() -> myUIAccess.give(() -> {
            myRenderScheduled = false;
            render();
        }));
    }

    @RequiredUIAccess
    private void render() {
        if (!myParts) {
            return;
        }
        List<MessageListItemPart> parts = new ArrayList<>();
        for (Segment segment : mySegments) {
            switch (segment) {
                case TextSegment text -> {
                    if (!text.myText.isEmpty()) {
                        parts.add(MessageListItemPart.text(text.myText.toString()));
                    }
                }
                case ThinkingSegment thinking -> {
                    Hyperlink toggle = Objects.requireNonNull(thinking.myToggle);
                    toggle.setText(thinking.myExpanded ? AILocalize.linkThinkingHide() : AILocalize.linkThinking());
                    parts.add(MessageListItemPart.component(toggle));
                    if (thinking.myExpanded) {
                        parts.add(MessageListItemPart.text(quote(thinking.myText.toString())));
                    }
                }
                case ToolCallSegment toolCall -> parts.addAll(toolCall.view().getParts());
                case ImageSegment image -> parts.add(MessageListItemPart.component(image.image()));
            }
        }
        List<AIToolCallContent.Diff> changes = changes();
        if (!changes.isEmpty()) {
            updateChanges(changes);
            parts.add(MessageListItemPart.component(myChanges));
        }
        myItem.setParts(parts);
    }

    /**
     * The changes of the answer a file each - what the file was before the first change of it against what the last
     * one made of it.
     */
    private List<AIToolCallContent.Diff> changes() {
        Map<String, AIToolCallContent.Diff> byPath = new LinkedHashMap<>();
        for (AIChatToolCallView view : myToolCalls.values()) {
            for (AIToolCallContent.Diff diff : AIChatToolCallView.diffs(view.getToolCall())) {
                AIToolCallContent.Diff first = byPath.get(diff.path());
                byPath.put(diff.path(), first == null ? diff : new AIToolCallContent.Diff(diff.path(), first.oldText(), diff.newText()));
            }
        }
        return List.copyOf(byPath.values());
    }

    @RequiredUIAccess
    private void updateChanges(List<AIToolCallContent.Diff> changes) {
        myChangesButton.setText(AILocalize.labelChangedFiles(AIChatToolCallView.changeCount(changes), changes.size()));
        myChangesButton.setIcon(myChangesExpanded ? PlatformIconGroup.generalArrowdown() : PlatformIconGroup.generalArrowright());
        myChangedFiles.removeAll();
        myChangedFiles.setVisible(myChangesExpanded);
        if (!myChangesExpanded) {
            return;
        }
        for (int i = 0; i < changes.size(); i++) {
            int index = i;
            AIToolCallContent.Diff change = changes.get(i);
            Hyperlink file = Hyperlink.create(LocalizeValue.of(relative(change.path())),
                event -> AIChangesViewer.getInstance().show(myProject, changes, index));
            Label count = Label.create(AIChatToolCallView.changeCount(List.of(change)));
            count.addStyle(LabelStyle.FONT_SMALL);
            HorizontalLayout row = HorizontalLayout.create(Space.SMALL);
            row.addStyle(HorizontalLayoutStyle.TRANSPARENT_BACKGROUND);
            row.add(file);
            row.add(count);
            myChangedFiles.add(row);
        }
    }

    private String relative(String path) {
        String basePath = myProject.getBasePath();
        return basePath != null && path.startsWith(basePath + "/") ? path.substring(basePath.length() + 1) : path;
    }

    /**
     * The reasoning as a markdown quote, set apart from the answer.
     */
    private static String quote(String text) {
        StringBuilder quoted = new StringBuilder();
        for (String line : text.strip().split("\n", -1)) {
            quoted.append("> ").append(line).append('\n');
        }
        return quoted.toString();
    }
}
