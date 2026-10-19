/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.diff.impl.internal.util;

import consulo.codeEditor.Editor;
import consulo.codeEditor.ScrollingModel;
import consulo.codeEditor.internal.bridge.EditorBridgeConnector;
import consulo.diff.DiffContext;
import consulo.diff.DiffDialogHints;
import consulo.diff.DiffUserDataKeys;
import consulo.diff.DiffWindowMode;
import consulo.diff.content.DiffContent;
import consulo.diff.content.DocumentContent;
import consulo.diff.content.EmptyContent;
import consulo.diff.impl.internal.viewer.TextDiffViewerUtil;
import consulo.diff.internal.DiffImplUtil;
import consulo.diff.localize.DiffLocalize;
import consulo.diff.request.ContentDiffRequest;
import consulo.diff.request.DiffRequest;
import consulo.localize.LocalizeValue;
import consulo.platform.LineSeparator;
import consulo.ui.Component;
import consulo.ui.FocusManager;
import consulo.ui.HasFocus;
import consulo.ui.HorizontalAlignment;
import consulo.ui.HtmlLabel;
import consulo.ui.Label;
import consulo.ui.Point2D;
import consulo.ui.Space;
import consulo.ui.UIAccess;
import consulo.ui.VerticalAlignment;
import consulo.ui.Window;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.style.ComponentColors;
import consulo.ui.style.StandardColors;
import consulo.ui.util.TraverseUtil;
import consulo.ui.ex.action.AnAction;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.Lists;
import consulo.util.lang.StringUtil;
import consulo.util.lang.xml.XmlStringUtil;
import org.jspecify.annotations.Nullable;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
public final class UnifiedDiffUtil {
    private UnifiedDiffUtil() {
    }

    @RequiredUIAccess
    public static Component createMessagePanel(LocalizeValue message) {
        HtmlLabel label = HtmlLabel.create(message.map(text -> XmlStringUtil.escapeString(text).replace("\n", "<br>")));
        label.setForegroundColor(ComponentColors.DISABLED_TEXT);

        VerticalLayout layout = VerticalLayout.create(Space.NONE, HorizontalAlignment.CENTER, VerticalAlignment.CENTER);
        layout.add(label);
        layout.paddingBuilder().allSet(Space.X_SMALL).apply();
        return layout;
    }

    public static void scrollToPoint(@Nullable Editor editor, Point2D point, boolean animated) {
        if (editor == null) {
            return;
        }
        if (!animated) {
            editor.getScrollingModel().disableAnimation();
        }
        editor.getScrollingModel().scrollHorizontally(point.x());
        editor.getScrollingModel().scrollVertically(point.y());
        if (!animated) {
            editor.getScrollingModel().enableAnimation();
        }
    }

    public static Point2D getScrollingPosition(@Nullable Editor editor) {
        if (editor == null) {
            return new Point2D(0, 0);
        }
        ScrollingModel model = editor.getScrollingModel();
        return new Point2D(model.getHorizontalScrollOffset(), model.getVerticalScrollOffset());
    }

    public static List<Component> getCustomNotifications(DiffContext context, DiffRequest request) {
        List<Component> requestComponents = request.getUserData(DiffUserDataKeys.NOTIFICATIONS);
        List<Component> contextComponents = context.getUserData(DiffUserDataKeys.NOTIFICATIONS);
        return ContainerUtil.concat(Lists.notNullize(contextComponents), Lists.notNullize(requestComponents));
    }

    public static List<Component> getCustomNotifications(DiffContent content) {
        return Lists.notNullize(content.getUserData(DiffUserDataKeys.NOTIFICATIONS));
    }

    @RequiredUIAccess
    public static List<@Nullable Component> createSimpleTitles(ContentDiffRequest request) {
        List<DiffContent> contents = request.getContents();
        List<String> titles = request.getContentTitles();

        if (!ContainerUtil.exists(titles, Objects::nonNull)) {
            return Collections.nCopies(titles.size(), null);
        }

        List<@Nullable Component> components = new ArrayList<>(titles.size());
        for (int i = 0; i < contents.size(); i++) {
            Component title = createTitle(StringUtil.notNullize(titles.get(i)));
            components.add(createTitleWithNotifications(title, contents.get(i)));
        }

        return components;
    }

    @RequiredUIAccess
    public static List<@Nullable Component> createTextTitles(ContentDiffRequest request, List<? extends Editor> editors) {
        List<DiffContent> contents = request.getContents();
        List<String> titles = request.getContentTitles();

        boolean equalCharsets = TextDiffViewerUtil.areEqualCharsets(contents);
        boolean equalSeparators = TextDiffViewerUtil.areEqualLineSeparators(contents);

        if (equalCharsets && equalSeparators && !ContainerUtil.exists(titles, Objects::nonNull)) {
            return Collections.nCopies(titles.size(), null);
        }

        List<@Nullable Component> result = new ArrayList<>(contents.size());
        for (int i = 0; i < contents.size(); i++) {
            Component title =
                createTitle(StringUtil.notNullize(titles.get(i)), contents.get(i), equalCharsets, equalSeparators, editors.get(i));
            result.add(createTitleWithNotifications(title, contents.get(i)));
        }

        return result;
    }

    @RequiredUIAccess
    private static @Nullable Component createTitleWithNotifications(@Nullable Component title, DiffContent content) {
        List<Component> notifications = getCustomNotifications(content);
        if (notifications.isEmpty()) {
            return title;
        }

        VerticalLayout layout = VerticalLayout.create(Space.X_SMALL);
        if (title != null) {
            layout.add(title);
        }
        for (Component notification : notifications) {
            layout.add(notification);
        }
        return layout;
    }

    @RequiredUIAccess
    private static @Nullable Component createTitle(
        String title,
        DiffContent content,
        boolean equalCharsets,
        boolean equalSeparators,
        @Nullable Editor editor
    ) {
        if (content instanceof EmptyContent) {
            return null;
        }

        Charset charset = equalCharsets ? null : ((DocumentContent) content).getCharset();
        LineSeparator separator = equalSeparators ? null : ((DocumentContent) content).getLineSeparator();
        boolean isReadOnly = editor == null || editor.isViewer() || !DiffImplUtil.canMakeWritable(editor.getDocument());

        return createTitle(title, charset, separator, isReadOnly);
    }

    @RequiredUIAccess
    public static Component createTitle(String title) {
        return createTitle(title, null, null, false);
    }

    @RequiredUIAccess
    public static Component createTitle(
        String title,
        @Nullable Charset charset,
        @Nullable LineSeparator separator,
        boolean readOnly
    ) {
        LocalizeValue text = LocalizeValue.of(title);
        if (readOnly) {
            text = LocalizeValue.join(text, LocalizeValue.space(), DiffLocalize.diffContentReadOnlyContentTitleSuffix());
        }

        DockLayout panel = DockLayout.create(Space.NONE);
        panel.paddingBuilder().verticalSet(Space.X_SMALL).horizontalSet(Space.SMALL).apply();
        panel.center(Label.create(text));

        if (charset != null || separator != null) {
            HorizontalLayout labels = HorizontalLayout.create(Space.X_SMALL);
            if (charset != null) {
                labels.add(createCharsetPanel(charset));
            }
            if (separator != null) {
                labels.add(createSeparatorPanel(separator));
            }
            panel.right(labels);
        }
        return panel;
    }

    @RequiredUIAccess
    private static Component createCharsetPanel(Charset charset) {
        Label label = Label.create(LocalizeValue.of(charset.displayName()));
        ColorValue color;
        if (charset.equals(StandardCharsets.UTF_8)) {
            color = StandardColors.BLUE;
        }
        else if (charset.equals(StandardCharsets.ISO_8859_1)) {
            color = StandardColors.RED;
        }
        else {
            color = StandardColors.BLACK;
        }
        label.setForegroundColor(color);
        return label;
    }

    @RequiredUIAccess
    private static Component createSeparatorPanel(LineSeparator separator) {
        Label label = Label.create(LocalizeValue.of(separator.name()));
        ColorValue color;
        if (separator == LineSeparator.CRLF) {
            color = StandardColors.RED;
        }
        else if (separator == LineSeparator.LF) {
            color = StandardColors.BLUE;
        }
        else if (separator == LineSeparator.CR) {
            color = StandardColors.MAGENTA;
        }
        else {
            color = StandardColors.BLACK;
        }
        label.setForegroundColor(color);
        return label;
    }

    @RequiredUIAccess
    public static boolean isFocusedComponent(@Nullable Component component) {
        if (component == null) {
            return false;
        }
        Component focused = FocusManager.get().getFocusedComponent();
        return focused != null && TraverseUtil.isDescendingFrom(focused, component);
    }

    @RequiredUIAccess
    public static boolean isWindowFocused(Component component) {
        Window window = TraverseUtil.getWindowAncestor(component);
        if (window != null) {
            return window.isActive();
        }
        return isFocusedComponent(component);
    }

    @RequiredUIAccess
    public static void requestFocus(@Nullable Component component) {
        if (component instanceof HasFocus focusable) {
            focusable.focus();
        }
    }

    @RequiredUIAccess
    public static void registerAction(AnAction action, Component component) {
        action.registerCustomShortcutSet(action.getShortcutSet(), component);
    }

    @RequiredUIAccess
    public static DiffWindowMode getWindowMode(DiffDialogHints hints) {
        DiffWindowMode mode = hints.getMode();
        if (mode == null) {
            boolean isUnderDialog = UIAccess.current().isInModalContext();
            return isUnderDialog ? DiffWindowMode.MODAL : DiffWindowMode.FRAME;
        }
        return mode;
    }

    public static List<EditorBridgeConnector> createConnectors(DividerPaintable paintable) {
        List<EditorBridgeConnector> connectors = new ArrayList<>();
        paintable.process(new DividerPaintable.Handler() {
            @Override
            public boolean process(int startLine1, int endLine1, int startLine2, int endLine2, ColorValue color) {
                return process(startLine1, endLine1, startLine2, endLine2, color, false);
            }

            @Override
            public boolean process(int startLine1, int endLine1, int startLine2, int endLine2, ColorValue color, boolean resolved) {
                connectors.add(new EditorBridgeConnector(startLine1, endLine1, startLine2, endLine2, color, resolved));
                return true;
            }
        });
        return connectors;
    }
}
