// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.impl.internal;

import consulo.disposer.Disposable;
import consulo.project.Project;
import consulo.proxy.EventDispatcher;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ModalityStateListener;
import consulo.ui.internal.UIAccessInternal;
import consulo.util.dataholder.Key;
import org.jetbrains.annotations.TestOnly;
import org.jspecify.annotations.Nullable;

import java.awt.*;

public final class LaterInvocator {
  private static final Key<ModalityContext> CONTEXT_KEY = Key.create(ModalityContext.class);

  private static final EventDispatcher<ModalityStateListener> ourModalityStateMulticaster = EventDispatcher.create(ModalityStateListener.class);

  private LaterInvocator() {
  }

  public static ModalityContext of(UIAccess uiAccess) {
    UIAccess original = UIAccessInternal.original(uiAccess);
    ModalityContext context = original.getUserData(CONTEXT_KEY);
    if (context != null) {
      return context;
    }
    return original.putUserDataIfAbsent(CONTEXT_KEY, new ModalityContext(original::give));
  }

  @RequiredUIAccess
  public static ModalityContext current() {
    return of(UIAccess.current());
  }

  public static void addModalityStateListener(@RequiredUIAccess ModalityStateListener listener, Disposable parentDisposable) {
    if (!ourModalityStateMulticaster.getListeners().contains(listener)) {
      ourModalityStateMulticaster.addListener(listener, parentDisposable);
    }
  }

  static void fireBeforeModalityStateChanged(boolean entering, Object modalEntity) {
    ourModalityStateMulticaster.getMulticaster().beforeModalityStateChanged(entering, modalEntity);
  }

  @RequiredUIAccess
  public static void enterModal(Object modalEntity) {
    current().enterModal(modalEntity);
  }

  @RequiredUIAccess
  public static void enterModal(Object modalEntity, IdeaModalityStateEx appendedState) {
    current().enterModal(modalEntity, appendedState);
  }

  @RequiredUIAccess
  public static void enterModal(@Nullable Project project, Dialog dialog) {
    current().enterModal(project, dialog);
  }

  @RequiredUIAccess
  public static void leaveModal(@Nullable Project project, Dialog dialog) {
    current().leaveModal(project, dialog);
  }

  @RequiredUIAccess
  public static void leaveModal(Object modalEntity) {
    current().leaveModal(modalEntity);
  }

  @TestOnly
  @RequiredUIAccess
  public static void leaveAllModals() {
    current().leaveAllModals();
  }

  @RequiredUIAccess
  public static Object[] getCurrentModalEntities() {
    return current().getCurrentModalEntities();
  }

  @RequiredUIAccess
  public static IdeaModalityStateEx getCurrentModalityState() {
    return current().getCurrentModalityState();
  }

  @RequiredUIAccess
  public static boolean isInModalContextForProject(@Nullable Project project) {
    return current().isInModalContextForProject(project);
  }

  @RequiredUIAccess
  public static boolean isInModalContext() {
    return current().isInModalContext();
  }

  public static void purgeExpiredItems() {
    for (UIAccess uiAccess : UIAccess.listAll()) {
      of(uiAccess).purgeExpiredItems();
    }
  }
}
