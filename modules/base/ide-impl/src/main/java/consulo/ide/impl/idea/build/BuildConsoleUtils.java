// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.build;

import consulo.build.ui.event.Failure;
import consulo.build.ui.issue.BuildIssue;
import consulo.build.ui.issue.BuildIssueQuickFix;
import consulo.build.ui.progress.BuildProgressListener;
import consulo.dataContext.DataProvider;
import consulo.execution.ui.console.ConsoleView;
import consulo.execution.ui.console.ConsoleViewContentType;
import consulo.execution.ui.console.HyperlinkInfo;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationService;
import consulo.project.ui.notification.event.NotificationListener;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.IJSwingUtilities;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static consulo.util.lang.StringUtil.stripHtml;

/**
 * @author Vladislav.Soroka
 */
public final class BuildConsoleUtils {
    private static final Logger LOG = Logger.getInstance(BuildConsoleUtils.class);
    private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]*>");
    private static final Pattern A_PATTERN = Pattern.compile("<a ([^>]* )?href=[\"']([^>]*)[\"'][^>]*>");
    private static final String A_CLOSING = "</a>";
    private static final Set<String> NEW_LINES = Set.of("<br>", "</br>", "<br/>", "<p>", "</p>", "<p/>", "<pre>", "</pre>");

    public static void printDetails(@Nonnull ConsoleView consoleView, @Nullable Failure failure, @Nullable String details) {
        String text = failure == null ? details : ObjectUtil.chooseNotNull(failure.getDescription(), failure.getMessage());
        if (text == null && failure != null && failure.getError() != null) {
            text = failure.getError().getMessage();
        }
        if (text == null) {
            return;
        }
        Notification notification = failure == null ? null : failure.getNotification();
        print(consoleView, notification, text);
    }

    public static void print(@Nonnull BuildTextConsoleView consoleView, @Nonnull NotificationGroup group, @Nonnull BuildIssue buildIssue) {
        Project project = consoleView.getProject();
        Map<String, NotificationListener> listenerMap = new LinkedHashMap<>();
        for (BuildIssueQuickFix quickFix : buildIssue.getQuickFixes()) {
            listenerMap.put(quickFix.getId(), (notification, event) -> {
                BuildView buildView = findBuildView(consoleView);
                quickFix.runQuickFix(project, buildView == null ? consoleView : buildView);
            });
        }

        Notification notification = NotificationService.getInstance()
            .newWarn(group)
            .title(LocalizeValue.localizeTODO(buildIssue.getTitle()))
            .content(LocalizeValue.localizeTODO(buildIssue.getDescription()))
            .hyperlinkListener(new NotificationListener.Adapter() {
                @Override
                @RequiredUIAccess
                protected void hyperlinkActivated(@Nonnull Notification notification, @Nonnull HyperlinkEvent event) {
                    if (event.getEventType() != HyperlinkEvent.EventType.ACTIVATED) {
                        return;
                    }

                    NotificationListener notificationListener = listenerMap.get(event.getDescription());
                    if (notificationListener != null) {
                        notificationListener.hyperlinkUpdate(notification, event);
                    }
                }
            })
            .create();
        print(consoleView, notification, buildIssue.getDescription());
    }

    private static void print(@Nonnull ConsoleView consoleView, @Nullable Notification notification, @Nonnull String text) {
        String content = StringUtil.convertLineSeparators(text);
        while (true) {
            Matcher tagMatcher = TAG_PATTERN.matcher(content);
            if (!tagMatcher.find()) {
                consoleView.print(content, ConsoleViewContentType.ERROR_OUTPUT);
                break;
            }
            String tagStart = tagMatcher.group();
            consoleView.print(content.substring(0, tagMatcher.start()), ConsoleViewContentType.ERROR_OUTPUT);
            Matcher aMatcher = A_PATTERN.matcher(tagStart);
            if (aMatcher.matches()) {
                final String href = aMatcher.group(2);
                int linkEnd = content.indexOf(A_CLOSING, tagMatcher.end());
                if (linkEnd > 0) {
                    String linkText = content.substring(tagMatcher.end(), linkEnd).replaceAll(TAG_PATTERN.pattern(), "");
                    consoleView.printHyperlink(linkText, new HyperlinkInfo() {
                        @RequiredUIAccess
                        @Override
                        public void navigate(Project project) {
                            if (notification != null && notification.getListener() != null) {
                                notification.getListener()
                                    .hyperlinkUpdate(notification, IJSwingUtilities.createHyperlinkEvent(href, consoleView.getComponent()));
                            }
                        }
                    });
                    content = content.substring(linkEnd + A_CLOSING.length());
                    continue;
                }
            }
            if (NEW_LINES.contains(tagStart)) {
                consoleView.print("\n", ConsoleViewContentType.SYSTEM_OUTPUT);
            }
            else {
                consoleView.print(content.substring(tagMatcher.start(), tagMatcher.end()), ConsoleViewContentType.ERROR_OUTPUT);
            }
            content = content.substring(tagMatcher.end());
        }

        consoleView.print("\n", ConsoleViewContentType.SYSTEM_OUTPUT);
    }

    //@ApiStatus.Internal
    @Nonnull
    public static String getMessageTitle(@Nonnull String message) {
        message = stripHtml(message, true);
        int sepIndex = message.indexOf(". ");
        int eolIndex = message.indexOf("\n");
        if (sepIndex < 0 || sepIndex > eolIndex && eolIndex > 0) {
            sepIndex = eolIndex;
        }
        if (sepIndex > 0) {
            message = message.substring(0, sepIndex);
        }
        return StringUtil.trimEnd(message.trim(), '.');
    }

    //@ApiStatus.Experimental
    @Nonnull
    public static DataProvider getDataProvider(@Nonnull Object buildId, @Nonnull AbstractViewManager buildListener) {
        BuildView buildView = buildListener.getBuildView(buildId);
        return (buildView != null) ? buildView : dataId -> null;
    }

    //@ApiStatus.Experimental
    @Nonnull
    public static DataProvider getDataProvider(@Nonnull Object buildId, @Nonnull BuildProgressListener buildListener) {
        DataProvider provider;
        if (buildListener instanceof BuildView buildView) {
            provider = buildView;
        }
        else if (buildListener instanceof AbstractViewManager abstractViewManager) {
            provider = getDataProvider(buildId, abstractViewManager);
        }
        else {
            LOG.error("BuildView or AbstractViewManager expected to obtain proper DataProvider for build console quick fixes");
            provider = dataId -> null;
        }
        return provider;
    }


    @Nullable
    private static BuildView findBuildView(@Nonnull Component component) {
        Component parent = component;
        while ((parent = parent.getParent()) != null) {
            if (parent instanceof BuildView buildView) {
                return buildView;
            }
        }
        return null;
    }
}
