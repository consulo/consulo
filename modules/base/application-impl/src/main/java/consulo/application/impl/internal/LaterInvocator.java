// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.impl.internal;

import consulo.disposer.Disposable;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ModalityState;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ModalityStateListener;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.TestOnly;

import java.awt.*;

@Deprecated
public final class LaterInvocator {
  private static final Logger LOG = Logger.getInstance(LaterInvocator.class);

  private LaterInvocator() {
  }

  public static void addModalityStateListener(@Nonnull ModalityStateListener listener, @Nonnull Disposable parentDisposable) {

  }

  @Nonnull
  public static IdeaModalityStateEx modalityStateForWindow(@Nonnull Window window) {
    return (IdeaModalityStateEx)IdeaModalityStateEx.NON_MODAL;
  }

  private static boolean isModalDialog(@Nonnull Object window) {
    return window instanceof Dialog && ((Dialog)window).isModal();
  }

  public static void enterModal(@Nonnull Object modalEntity) {
  }

  public static void enterModal(@Nonnull Object modalEntity, @Nonnull IdeaModalityStateEx appendedState) {
  }

  public static void enterModal(Project project, @Nonnull Dialog dialog) {
  }

  /**
   * Marks the given modality state (not {@code any()}} as transparent, i.e. {@code invokeLater} calls with its "parent" modality state
   * will also be executed within it. NB: this will cause all VFS/PSI/etc events be executed inside your modal dialog, so you'll need
   * to handle them appropriately, so please consider making the dialog non-modal instead of using this API.
   */
  public static void markTransparent(@Nonnull ModalityState state) {

  }

  public static void leaveModal(Project project, @Nonnull Dialog dialog) {

  }


  public static void leaveModal(@Nonnull Object modalEntity) {

  }

  @TestOnly
  public static void leaveAllModals() {
  }

  @Nonnull
  public static Object [] getCurrentModalEntities() {
    return new Object[]{getCurrentModalityState()};
  }

  @Nonnull
  public static IdeaModalityStateEx getCurrentModalityState() {
    return (IdeaModalityStateEx)IdeaModalityStateEx.NON_MODAL;
  }

  public static boolean isInModalContextForProject(final Project project) {
    return false;
  }

  public static boolean isInModalContext() {
    return false;
  }

  @RequiredUIAccess
  private static void assertIsDispatchThread() {
    UIAccess.assertIsUIThread();
  }
}
