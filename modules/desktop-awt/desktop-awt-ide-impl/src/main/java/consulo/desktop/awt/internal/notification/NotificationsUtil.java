/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import consulo.application.ui.UIFontManager;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.event.NotificationListener;
import consulo.ide.impl.idea.openapi.ui.MessageType;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.ide.impl.idea.xml.util.XmlStringUtil;
import consulo.application.AllIcons;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.logging.Logger;
import consulo.ui.image.Image;
import consulo.util.lang.Pair;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;

/**
 * @author spleaner
 */
public class NotificationsUtil {
  private static final Logger LOG = Logger.getInstance(NotificationsUtil.class);
  private static final int TITLE_LIMIT = 1000;
  private static final int CONTENT_LIMIT = 10000;

  @Nonnull
  public static String buildHtml(@Nonnull final Notification notification, @Nullable String style) {
    String title = notification.getTitle();
    String content = notification.getContent();
    if (title.length() > TITLE_LIMIT || content.length() > CONTENT_LIMIT) {
      LOG.info("Too large notification " + notification + " of " + notification.getClass() +
               "\nListener=" + notification.getListener() +
               "\nTitle=" + title +
               "\nContent=" + content);
      title = StringUtil.trimLog(title, TITLE_LIMIT);
      content = StringUtil.trimLog(content, CONTENT_LIMIT);
    }
    return buildHtml(title, null, content, style, "#" + ColorUtil.toHex(getMessageType(notification).getTitleForeground()), null, null);
  }

  @Nonnull
  public static String buildHtml(@Nonnull final Notification notification,
                                 @Nullable String style,
                                 boolean isContent,
                                 @Nullable Color color,
                                 @Nullable String contentStyle) {
    String title = !isContent ? notification.getTitle() : "";
    String subtitle = !isContent ? notification.getSubtitle() : null;
    String content = isContent ? notification.getContent() : "";
    if (title.length() > TITLE_LIMIT || StringUtil.length(subtitle) > TITLE_LIMIT || content.length() > CONTENT_LIMIT) {
      LOG.info("Too large notification " + notification + " of " + notification.getClass() +
               "\nListener=" + notification.getListener() +
               "\nTitle=" + title +
               "\nSubtitle=" + subtitle +
               "\nContent=" + content);
      title = StringUtil.trimLog(title, TITLE_LIMIT);
      subtitle = StringUtil.trimLog(StringUtil.notNullize(subtitle), TITLE_LIMIT);
      content = StringUtil.trimLog(content, CONTENT_LIMIT);
    }
    if (isContent) {
      content = StringUtil.replace(content, "<p/>", "<br>");
    }
    String colorText = color == null ? null : "#" + ColorUtil.toHex(color);
    return buildHtml(title, subtitle, content, style, isContent ? null : colorText, isContent ? colorText : null, contentStyle);
  }

  @Nonnull
  public static String buildHtml(@Nullable String title,
                                 @Nullable String subtitle,
                                 @Nullable String content,
                                 @Nullable String style,
                                 @Nullable String titleColor,
                                 @Nullable String contentColor,
                                 @Nullable String contentStyle) {
    if (StringUtil.isEmpty(title) && !StringUtil.isEmpty(subtitle)) {
      title = subtitle;
      subtitle = null;
    }
    else if (!StringUtil.isEmpty(title) && !StringUtil.isEmpty(subtitle)) {
      title += ":";
    }

    StringBuilder result = new StringBuilder();
    if (style != null) {
      result.append("<div style=\"").append(style).append("\">");
    }
    if (!StringUtil.isEmpty(title)) {
      result.append("<b").append(titleColor == null ? ">" : " color=\"" + titleColor + "\">").append(title).append("</b>");
    }
    if (!StringUtil.isEmpty(subtitle)) {
      result.append("&nbsp;").append(titleColor == null ? "" : "<span color=\"" + titleColor + "\">").append(subtitle)
              .append(titleColor == null ? "" : "</span>");
    }
    if (!StringUtil.isEmpty(content)) {
      result.append("<p").append(contentStyle == null ? "" : " style=\"" + contentStyle + "\"")
              .append(contentColor == null ? ">" : " color=\"" + contentColor + "\">").append(content).append("</p>");
    }
    if (style != null) {
      result.append("</div>");
    }
    return XmlStringUtil.wrapInHtml(result.toString());
  }

  @Nullable
  public static String getFontStyle() {
    String fontName = getFontName();
    return StringUtil.isEmpty(fontName) ? null : "font-family:" + fontName + ";";
  }

  @Nullable
  public static Pair<String, Integer> getFontData() {
    UIFontManager uiFontManager = UIFontManager.getInstance();
    return Pair.create(uiFontManager.getFontName(), uiFontManager.getFontSize());
  }

  @Nullable
  public static String getFontName() {
    Pair<String, Integer> data = getFontData();
    return data == null ? null : data.first;
  }

  @Nullable
  public static HyperlinkListener wrapListener(@Nonnull final Notification notification) {
    final NotificationListener listener = notification.getListener();
    if (listener == null) return null;

    return new HyperlinkListener() {
      public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
          final NotificationListener listener1 = notification.getListener();
          if (listener1 != null) {
            listener1.hyperlinkUpdate(notification, e);
          }
        }
      }
    };
  }

  @Nonnull
  public static Image getIcon(@Nonnull final Notification notification) {
    Image icon = notification.getIcon();
    if (icon != null) {
      return icon;
    }

    switch (notification.getType()) {
      case WARNING:
        return AllIcons.General.BalloonWarning;
      case ERROR:
        return AllIcons.Ide.FatalError;
      case INFORMATION:
      default:
        return AllIcons.General.BalloonInformation;
    }
  }

  @Nonnull
  public static MessageType getMessageType(@Nonnull Notification notification) {
    switch (notification.getType()) {
      case WARNING:
        return MessageType.WARNING;
      case ERROR:
        return MessageType.ERROR;
      case INFORMATION:
      default:
        return MessageType.INFO;
    }
  }
}
