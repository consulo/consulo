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
package consulo.desktop.awt.internal.notification;

import consulo.application.util.DateFormatUtil;
import consulo.application.util.NotNullLazyValue;
import consulo.codeEditor.*;
import consulo.codeEditor.event.EditorMouseEvent;
import consulo.codeEditor.markup.*;
import consulo.colorScheme.*;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.execution.ui.console.ConsoleViewContentType;
import consulo.execution.ui.console.ConsoleViewUtil;
import consulo.execution.ui.console.HyperlinkInfo;
import consulo.ide.impl.idea.execution.impl.EditorHyperlinkSupport;
import consulo.ide.impl.idea.notification.impl.NotificationSettings;
import consulo.ide.impl.idea.notification.impl.NotificationsConfigurationImpl;
import consulo.ide.impl.idea.openapi.editor.ex.util.EditorUtil;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.event.ProjectManagerListener;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationDisplayType;
import consulo.project.ui.notification.NotificationType;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.style.StandardColors;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.SimpleReference;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author peter
 */
class EventLogConsole {
    private static final Key<String> GROUP_ID = Key.create("GROUP_ID");
    private static final Key<String> NOTIFICATION_ID = Key.create("NOTIFICATION_ID");

    private final NotNullLazyValue<Editor> myLogEditor = new NotNullLazyValue<>() {
        @Nonnull
        @Override
        protected Editor compute() {
            return createLogEditor();
        }
    };

    private final NotNullLazyValue<EditorHyperlinkSupport> myHyperlinkSupport = new NotNullLazyValue<>() {
        @Nonnull
        @Override
        protected EditorHyperlinkSupport compute() {
            return new EditorHyperlinkSupport(getConsoleEditor(), myProjectModel.getProject());
        }
    };
    private final LogModel myProjectModel;

    private String myLastDate;

    private List<RangeHighlighter> myNMoreHighlighters;

    EventLogConsole(LogModel model) {
        myProjectModel = model;
    }

    private Editor createLogEditor() {
        Project project = myProjectModel.getProject();
        final EditorEx editor = ConsoleViewUtil.setupConsoleEditor(project, false, false);
        editor.getSettings().setFoldingOutlineShown(true);
        editor.getSettings().setWhitespacesShown(false);
        editor.getSettings().setUseSoftWraps(true);

        installNotificationsFont(editor);
        myProjectModel.getProject().getMessageBus().connect().subscribe(
            ProjectManagerListener.class,
            new ProjectManagerListener() {
                @Override
                public void projectClosed(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
                    if (project == myProjectModel.getProject()) {
                        EditorFactory.getInstance().releaseEditor(editor);
                    }
                }
            }
        );

        editor.getMarkupModel().setErrorStripeVisible(true);

        final ClearLogAction clearLog = new ClearLogAction(this);

        editor.installPopupHandler(new ContextMenuPopupHandler() {
            @Nullable
            @Override
            public ActionGroup getActionGroup(@Nonnull EditorMouseEvent event) {
                ActionManager actionManager = ActionManager.getInstance();
                return createPopupActions(actionManager, clearLog, editor, event);
            }
        });
        return editor;
    }

    private void installNotificationsFont(@Nonnull EditorEx editor) {
        DelegateColorScheme globalScheme = new DelegateColorScheme(EditorColorsManager.getInstance().getGlobalScheme()) {
            @Override
            public String getEditorFontName() {
                return getConsoleFontName();
            }

            @Override
            public int getEditorFontSize() {
                return getConsoleFontSize();
            }

            @Override
            public String getConsoleFontName() {
                return NotificationsUtil.getFontName();
            }

            @Override
            public int getConsoleFontSize() {
                Pair<String, Integer> data = NotificationsUtil.getFontData();
                return data == null ? super.getConsoleFontSize() : data.second;
            }

            @Override
            public void setEditorFontName(String fontName) {
            }

            @Override
            public void setConsoleFontName(String fontName) {
            }

            @Override
            public void setEditorFontSize(int fontSize) {
            }

            @Override
            public void setConsoleFontSize(int fontSize) {
            }
        };
        EditorColorsManager.getInstance().addEditorColorsListener(
            scheme -> {
                globalScheme.setDelegate(EditorColorsManager.getInstance().getGlobalScheme());
                editor.reinitSettings();
            },
            myProjectModel
        );
        editor.setColorsScheme(ConsoleViewUtil.updateConsoleColorScheme(editor.createBoundColorSchemeDelegate(globalScheme)));
    }

    private static DefaultActionGroup createPopupActions(
        ActionManager actionManager,
        ClearLogAction action,
        EditorEx editor,
        EditorMouseEvent event
    ) {
        AnAction[] children = ((ActionGroup) actionManager.getAction(IdeActions.GROUP_CONSOLE_EDITOR_POPUP)).getChildren(null);
        DefaultActionGroup group = new DefaultActionGroup();
        addConfigureNotificationAction(editor, event, group);
        group.addAll(children);
        group.addSeparator();
        group.add(action);
        return group;
    }

    private static void addConfigureNotificationAction(
        @Nonnull EditorEx editor,
        @Nonnull EditorMouseEvent event,
        @Nonnull DefaultActionGroup actions
    ) {
        LogicalPosition position = editor.xyToLogicalPosition(event.getMouseEvent().getPoint());
        if (EditorUtil.inVirtualSpace(editor, position)) {
            return;
        }
        int offset = editor.logicalPositionToOffset(position);
        editor.getMarkupModel().processRangeHighlightersOverlappingWith(
            offset,
            offset,
            rangeHighlighter -> {
                String groupId = GROUP_ID.get(rangeHighlighter);
                if (groupId != null) {
                    addConfigureNotificationAction(actions, groupId);
                    return false;
                }
                return true;
            }
        );
    }

    private static void addConfigureNotificationAction(@Nonnull DefaultActionGroup actions, @Nonnull String groupId) {
        DefaultActionGroup displayTypeGroup =
            new DefaultActionGroup(LocalizeValue.localizeTODO("VcsBranchMappingChangedNotification Display Type"), true);
        NotificationSettings settings = NotificationsConfigurationImpl.getSettings(groupId);
        NotificationDisplayType current = settings.getDisplayType();

        for (NotificationDisplayType type : NotificationDisplayType.values()) {
            if (type != NotificationDisplayType.TOOL_WINDOW || NotificationsConfigurationImpl.getInstanceImpl()
                .hasToolWindowCapability(groupId)) {
                displayTypeGroup.add(new DisplayTypeAction(settings, type, current));
            }
        }

        actions.add(displayTypeGroup);
        actions.addSeparator();
    }

    private static class DisplayTypeAction extends ToggleAction {
        private final NotificationSettings mySettings;
        private final NotificationDisplayType myType;
        private final NotificationDisplayType myCurrent;

        public DisplayTypeAction(
            @Nonnull NotificationSettings settings,
            @Nonnull NotificationDisplayType type,
            @Nonnull NotificationDisplayType current
        ) {
            super(type.getTitle());
            mySettings = settings;
            myType = type;
            myCurrent = current;
        }

        @Override
        public boolean isSelected(@Nonnull AnActionEvent e) {
            return myType == myCurrent;
        }

        @Override
        public void setSelected(@Nonnull AnActionEvent e, boolean state) {
            if (state) {
                NotificationsConfigurationImpl.getInstanceImpl().changeSettings(mySettings.withDisplayType(myType));
            }
        }
    }

    void doPrintNotification(Notification notification) {
        Editor editor = getConsoleEditor();
        if (editor.isDisposed()) {
            return;
        }

        Document document = editor.getDocument();
        boolean scroll = document.getTextLength() == editor.getCaretModel().getOffset() || !editor.getContentComponent().hasFocus();

        if (document.getTextLength() > 0) {
            append(document, "\n");
        }

        String lastDate = DateFormatUtil.formatDate(notification.getTimestamp());
        if (document.getTextLength() == 0 || !lastDate.equals(myLastDate)) {
            myLastDate = lastDate;
            append(document, lastDate + "\n");
        }

        int startDateOffset = document.getTextLength();

        String date = DateFormatUtil.formatTime(notification.getTimestamp()) + "\t";
        append(document, date);

        int tabs = calculateTabs(editor, startDateOffset);

        int titleStartOffset = document.getTextLength();
        int startLine = document.getLineCount() - 1;

        EventLog.LogEntry pair = EventLog.formatForLog(notification, StringUtil.repeatSymbol('\t', tabs));

        NotificationType type = notification.getType();
        TextAttributesKey key = switch (type) {
            case ERROR -> ConsoleViewContentType.LOG_ERROR_OUTPUT_KEY;
            case WARNING -> ConsoleViewContentType.LOG_WARNING_OUTPUT_KEY;
            case INFORMATION -> ConsoleViewContentType.NORMAL_OUTPUT_KEY;
        };

        int msgStart = document.getTextLength();
        append(document, pair.message);

        TextAttributes attributes = EditorColorsManager.getInstance().getGlobalScheme().getAttributes(key);
        int layer = HighlighterLayer.CARET_ROW + 1;
        RangeHighlighter highlighter = editor.getMarkupModel()
            .addRangeHighlighter(msgStart, document.getTextLength(), layer, attributes, HighlighterTargetArea.EXACT_RANGE);
        GROUP_ID.set(highlighter, notification.getGroupId());
        NOTIFICATION_ID.set(highlighter, notification.id);

        for (Pair<TextRange, HyperlinkInfo> link : pair.links) {
            RangeHighlighter rangeHighlighter = myHyperlinkSupport.getValue()
                .createHyperlink(link.first.getStartOffset() + msgStart, link.first.getEndOffset() + msgStart, null, link.second);
            if (link.second instanceof EventLog.ShowBalloon showBalloon) {
                showBalloon.setRangeHighlighter(rangeHighlighter);
            }
        }

        append(document, "\n");

        if (scroll) {
            editor.getCaretModel().moveToOffset(document.getTextLength());
            editor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
        }

        if (notification.isImportant()) {
            highlightNotification(notification, pair.status, startLine, document.getLineCount() - 1, titleStartOffset, pair.titleLength);
        }
    }

    private static int calculateTabs(@Nonnull Editor editor, int startDateOffset) {
        Document document = editor.getDocument();
        int startOffset = document.getTextLength();
        Point dateStartPoint = editor.logicalPositionToXY(editor.offsetToLogicalPosition(startDateOffset));
        Point dateEndPoint = editor.logicalPositionToXY(editor.offsetToLogicalPosition(startOffset));
        int width = dateEndPoint.x - dateStartPoint.x;

        document.insertString(startOffset, "\n");

        Point startPoint = editor.logicalPositionToXY(editor.offsetToLogicalPosition(startOffset + 1));

        for (int count = 1; ; count++) {
            document.insertString(startOffset + count, "\t");

            Point endPoint = editor.logicalPositionToXY(editor.offsetToLogicalPosition(document.getTextLength()));
            int tabWidth = endPoint.x - startPoint.x;

            if (width <= tabWidth) {
                document.deleteString(startOffset, document.getTextLength());
                return count;
            }
        }
    }

    private void highlightNotification(
        Notification notification,
        String message,
        int startLine,
        int endLine,
        int titleOffset,
        int titleLength
    ) {
        MarkupModel markupModel = getConsoleEditor().getMarkupModel();
        TextAttributes bold = new TextAttributes(null, null, null, null, Font.BOLD);
        RangeHighlighter colorHighlighter = markupModel.addRangeHighlighter(
            titleOffset,
            titleOffset + titleLength,
            HighlighterLayer.CARET_ROW + 1,
            bold,
            HighlighterTargetArea.EXACT_RANGE
        );
        ColorValue color = switch (notification.getType()) {
            case ERROR -> StandardColors.RED;
            case WARNING -> StandardColors.YELLOW;
            case INFORMATION -> StandardColors.GREEN;
        };
        colorHighlighter.setErrorStripeMarkColor(color);
        colorHighlighter.setErrorStripeTooltip(message);

        Runnable removeHandler = () -> {
            if (colorHighlighter.isValid()) {
                markupModel.removeHighlighter(colorHighlighter);
            }

            TextAttributes italic = new TextAttributes(null, null, null, null, Font.ITALIC);
            for (int line = startLine; line < endLine; line++) {
                for (RangeHighlighter highlighter : myHyperlinkSupport.getValue().findAllHyperlinksOnLine(line)) {
                    markupModel.addRangeHighlighter(
                        highlighter.getStartOffset(),
                        highlighter.getEndOffset(),
                        HighlighterLayer.CARET_ROW + 2,
                        italic,
                        HighlighterTargetArea.EXACT_RANGE
                    );
                    myHyperlinkSupport.getValue().removeHyperlink(highlighter);
                }
            }
        };
        if (!notification.isExpired()) {
            myProjectModel.removeHandlers.put(notification, removeHandler);
        }
        else {
            removeHandler.run();
        }
    }

    public Editor getConsoleEditor() {
        return myLogEditor.getValue();
    }

    public void clearNMore() {
        if (myNMoreHighlighters != null) {
            MarkupModel model = getConsoleEditor().getMarkupModel();
            for (RangeHighlighter highlighter : myNMoreHighlighters) {
                model.removeHighlighter(highlighter);
            }
            myNMoreHighlighters = null;
        }
    }

    public void showNotification(@Nonnull List<String> ids) {
        clearNMore();
        myNMoreHighlighters = new ArrayList<>();

        EditorEx editor = (EditorEx) getConsoleEditor();
        List<RangeHighlighterEx> highlighters = ContainerUtil.mapNotNull(ids, EventLogConsole.this::findHighlighter);

        if (!highlighters.isEmpty()) {
            editor.getCaretModel().moveToOffset(highlighters.get(0).getStartOffset());
            editor.getScrollingModel().scrollToCaret(ScrollType.CENTER_UP);

            List<Point> ranges = new ArrayList<>();
            Point currentRange = null;
            Document document = editor.getDocument();

            for (RangeHighlighter highlighter : highlighters) {
                int startLine = document.getLineNumber(highlighter.getStartOffset());
                int endLine = document.getLineNumber(highlighter.getEndOffset()) + 1;
                if (currentRange != null && startLine - 1 == currentRange.y) {
                    currentRange.y = endLine;
                }
                else {
                    ranges.add(currentRange = new Point(startLine, endLine));
                }
            }

            //noinspection UseJBColor
            TextAttributes attributes = new TextAttributes(
                null,
                TargetAWT.from(ColorUtil.mix(TargetAWT.to(editor.getBackgroundColor()), new Color(0x808080), 0.1)),
                null,
                EffectType.BOXED,
                Font.PLAIN
            );
            MarkupModelEx markupModel = editor.getMarkupModel();

            for (Point range : ranges) {
                int start = document.getLineStartOffset(range.x);
                int end = document.getLineStartOffset(range.y);
                myNMoreHighlighters.add(markupModel.addRangeHighlighter(
                    start,
                    end,
                    HighlighterLayer.CARET_ROW + 2,
                    attributes,
                    HighlighterTargetArea.EXACT_RANGE
                ));
            }
        }
    }

    @Nullable
    private RangeHighlighterEx findHighlighter(@Nonnull String id) {
        EditorEx editor = (EditorEx) getConsoleEditor();
        SimpleReference<RangeHighlighterEx> highlighter = new SimpleReference<>();

        editor.getMarkupModel().processRangeHighlightersOverlappingWith(
            0,
            editor.getDocument().getTextLength(),
            rangeHighlighter -> {
                if (id.equals(NOTIFICATION_ID.get(rangeHighlighter))) {
                    highlighter.set(rangeHighlighter);
                    return false;
                }
                return true;
            }
        );

        return highlighter.get();
    }

    @Nullable
    @RequiredUIAccess
    public RelativePoint getRangeHighlighterLocation(RangeHighlighter range) {
        Editor editor = getConsoleEditor();
        Project project = editor.getProject();
        Window window = NotificationsManagerImpl.getNotificationsManager().findWindowForBalloon(project);
        if (range != null && window != null) {
            Point point = editor.visualPositionToXY(editor.offsetToVisualPosition(range.getStartOffset()));
            return new RelativePoint(window, SwingUtilities.convertPoint(editor.getContentComponent(), point, window));
        }
        return null;
    }

    private static void append(Document document, String s) {
        document.insertString(document.getTextLength(), s);
    }

    public static class ClearLogAction extends DumbAwareAction {
        private EventLogConsole myConsole;

        public ClearLogAction(EventLogConsole console) {
            super(
                LocalizeValue.localizeTODO("Clear All"),
                LocalizeValue.localizeTODO("Clear the contents of the Event Log"),
                PlatformIconGroup.actionsGc()
            );
            myConsole = console;
        }

        @Override
        @RequiredUIAccess
        public void update(@Nonnull AnActionEvent e) {
            LogModel model = myConsole.myProjectModel;
            e.getPresentation().setEnabled(!model.getNotifications().isEmpty());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            LogModel model = myConsole.myProjectModel;
            for (Notification notification : model.getNotifications()) {
                notification.expire();
                model.removeNotification(notification);
            }
        }
    }
}
