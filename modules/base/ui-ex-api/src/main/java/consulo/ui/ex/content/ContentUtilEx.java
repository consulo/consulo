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
package consulo.ui.ex.content;

import consulo.application.ApplicationPropertiesComponent;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.util.lang.Comparing;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author Konstantin Bulenkov
 */
public class ContentUtilEx extends ContentsUtil {

  public static void addTabbedContent(@Nonnull ContentManager manager,
                                      @Nonnull JComponent contentComponent,
                                      @Nonnull String groupPrefix,
                                      @Nonnull String tabName,
                                      boolean select) {
    addTabbedContent(manager, contentComponent, groupPrefix, tabName, select, null);
  }

  public static void addTabbedContent(@Nonnull ContentManager manager,
                                      @Nonnull JComponent contentComponent,
                                      @Nonnull String groupPrefix,
                                      @Nonnull String tabName,
                                      boolean select,
                                      @Nullable Disposable childDisposable) {
    if (ApplicationPropertiesComponent.getInstance().getBoolean(TabbedContent.SPLIT_PROPERTY_PREFIX + groupPrefix)) {
      final Content content = ContentFactory.getInstance().createContent(contentComponent, getFullName(groupPrefix, tabName), true);
      content.putUserData(Content.TABBED_CONTENT_KEY, Boolean.TRUE);
      content.putUserData(Content.TAB_GROUP_NAME_KEY, groupPrefix);

      for (Content c : manager.getContents()) {
        if (c.getComponent() == contentComponent) {
          if (select) {
            manager.setSelectedContent(c);
          }
          return;
        }
      }
      addContent(manager, content, select);

      registerDisposable(content, childDisposable, contentComponent);

      return;
    }

    TabbedContent tabbedContent = findTabbedContent(manager, groupPrefix);

    if (tabbedContent == null) {
      final Disposable disposable = Disposable.newDisposable();
      tabbedContent = ContentFactory.getInstance().createTabbedContent(contentComponent, tabName, true, groupPrefix);
      ContentsUtil.addOrReplaceContent(manager, tabbedContent, select);
      Disposer.register(tabbedContent, disposable);
    }
    else {
      for (Pair<String, JComponent> tab : new ArrayList<>(tabbedContent.getTabs())) {
        if (Comparing.equal(tab.second, contentComponent)) {
          tabbedContent.removeContent(tab.second);
        }
      }
      if (select) {
        manager.setSelectedContent(tabbedContent, true, true);
      }
      tabbedContent.addContent(contentComponent, tabName, true);
    }

    registerDisposable(tabbedContent, childDisposable, contentComponent);
  }

  private static void registerDisposable(@Nonnull Content content,
                                         @Nullable Disposable childDisposable,
                                         @Nonnull JComponent contentComponent) {
    if (childDisposable != null) {
      Disposer.register(content, childDisposable);
      assert contentComponent.getClientProperty(DISPOSABLE_KEY) == null;
      contentComponent.putClientProperty(DISPOSABLE_KEY, childDisposable);
      Disposer.register(childDisposable, () -> contentComponent.putClientProperty(DISPOSABLE_KEY, null));
    }
    else {
      Object disposableByKey = contentComponent.getClientProperty(DISPOSABLE_KEY);
      if (disposableByKey != null && disposableByKey instanceof Disposable) {
        Disposer.register(content, (Disposable)disposableByKey);
      }
    }
  }

  @Nullable
  public static TabbedContent findTabbedContent(@Nonnull ContentManager manager, @Nonnull String groupPrefix) {
    TabbedContent tabbedContent = null;
    for (Content content : manager.getContents()) {
      if (content instanceof TabbedContent && content.getTabName().startsWith(getFullPrefix(groupPrefix))) {
        tabbedContent = (TabbedContent)content;
        break;
      }
    }
    return tabbedContent;
  }

  public static boolean isContentTab(@Nonnull Content content, @Nonnull String groupPrefix) {
    return (content instanceof TabbedContent && content.getTabName().startsWith(getFullPrefix(groupPrefix))) ||
           groupPrefix.equals(content.getUserData(Content.TAB_GROUP_NAME_KEY));
  }

  @Nonnull
  public static String getFullName(@Nonnull String groupPrefix, @Nonnull String tabName) {
    return getFullPrefix(groupPrefix) + tabName;
  }

  @Nonnull
  private static String getFullPrefix(@Nonnull String groupPrefix) {
    return groupPrefix + ": ";
  }

  /**
   * Searches through all {@link Content simple} and {@link TabbedContent tabbed} contents of the given ContentManager,
   * and selects the one which holds the specified {@code contentComponent}.
   *
   * @return true if the necessary content was found (and thus selected) among content components of the given ContentManager.
   */
  public static boolean selectContent(@Nonnull ContentManager manager, @Nonnull final JComponent contentComponent, boolean requestFocus) {
    for (Content content : manager.getContents()) {
      if (content instanceof TabbedContent tabbedContent) {
        boolean found = tabbedContent.findAndSelectContent(contentComponent);
        if (found) {
          manager.setSelectedContent(content, requestFocus);
          return true;
        }
      }
      else if (Comparing.equal(content.getComponent(), contentComponent)) {
        manager.setSelectedContent(content, requestFocus);
        return true;
      }
    }
    return false;
  }

  /**
   * Searches through all {@link Content simple} and {@link TabbedContent tabbed} contents of the given ContentManager,
   * trying to find the first one which matches the given condition.
   */
  @Nullable
  public static JComponent findContentComponent(@Nonnull ContentManager manager, @Nonnull Predicate<JComponent> condition) {
    for (Content content : manager.getContents()) {
      if (content instanceof TabbedContent tabbedContent) {
        List<Pair<String, JComponent>> tabs = tabbedContent.getTabs();
        for (Pair<String, JComponent> tab : tabs) {
          if (condition.test(tab.second)) {
            return tab.second;
          }
        }
      }
      else if (condition.test(content.getComponent())) {
        return content.getComponent();
      }
    }
    return null;
  }

  public static int getSelectedTab(@Nonnull TabbedContent content) {
    final JComponent current = content.getComponent();
    int index = 0;
    for (Pair<String, JComponent> tab : content.getTabs()) {
      if (tab.second == current) {
        return index;
      }
      index++;
    }
    return -1;
  }

  @Nullable
  public static String getTabNameWithoutPrefix(@Nonnull TabbedContent content, @Nonnull String fullTabName) {
    int fullPrefixLength = getFullPrefix(content.getTitlePrefix()).length();
    if (fullTabName.startsWith(content.getTitlePrefix())) {
      return fullTabName.substring(fullPrefixLength);
    }
    return null;
  }
}
