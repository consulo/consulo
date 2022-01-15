// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.editor.impl;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import consulo.logging.Logger;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This class allows to disable (temporarily or permanently) showing certain popups on mouse hover (currently, error/warning descriptions
 * and quick documentation on mouse hover are impacted). If several requests to disable popups have been performed, corresponding number of
 * enabling requests must be performed to turn on hover popups again.
 */
@Singleton
public class EditorMouseHoverPopupControl {
  @Nonnull
  public static EditorMouseHoverPopupControl getInstance() {
    return ServiceManager.getService(EditorMouseHoverPopupControl.class);
  }

  private static final Logger LOG = Logger.getInstance(EditorMouseHoverPopupControl.class);
  private static final Key<Integer> MOUSE_TRACKING_DISABLED_COUNT = Key.create("MOUSE_TRACKING_DISABLED_COUNT");
  private final Collection<Runnable> myListeners = new CopyOnWriteArrayList<>();

  public static void disablePopups(@Nonnull Editor editor) {
    setTrackingDisabled(editor, true);
  }

  public static void enablePopups(@Nonnull Editor editor) {
    setTrackingDisabled(editor, false);
  }

  public static void disablePopups(@Nonnull Document document) {
    setTrackingDisabled(document, true);
  }

  public static void enablePopups(@Nonnull Document document) {
    setTrackingDisabled(document, false);
  }

  public static void disablePopups(@Nonnull Project project) {
    setTrackingDisabled(project, true);
  }

  public static void enablePopups(@Nonnull Project project) {
    setTrackingDisabled(project, false);
  }

  private static void setTrackingDisabled(@Nonnull UserDataHolder holder, boolean value) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    Integer userData = holder.getUserData(MOUSE_TRACKING_DISABLED_COUNT);
    int count = (userData == null ? 0 : userData) + (value ? 1 : -1);
    if (count < 0) {
      LOG.warn(new IllegalStateException("Editor mouse hover popups weren't disabled previously"));
      count = 0;
    }
    holder.putUserData(MOUSE_TRACKING_DISABLED_COUNT, count == 0 ? null : count);
    if ((userData == null) != (count == 0)) {
      EditorMouseHoverPopupControl instance = getInstance();
      if (instance != null) {
        instance.myListeners.forEach(Runnable::run);
      }
    }
  }

  public static boolean arePopupsDisabled(@Nonnull Editor editor) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    Project project = editor.getProject();
    return editor.getUserData(MOUSE_TRACKING_DISABLED_COUNT) != null ||
           editor.getDocument().getUserData(MOUSE_TRACKING_DISABLED_COUNT) != null ||
           project != null && project.getUserData(MOUSE_TRACKING_DISABLED_COUNT) != null;
  }

  public void addListener(@Nonnull Runnable listener) {
    myListeners.add(listener);
  }
}
