/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.application.dumb;

import consulo.logging.attachment.Attachment;
import consulo.logging.attachment.AttachmentFactory;
import consulo.logging.attachment.ExceptionWithAttachments;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Thrown on accessing indices when they're not ready, in so-called dumb mode. Possible fixes:
 * <ul>
 * <li> If {@link consulo.ide.impl.idea.openapi.actionSystem.AnAction#actionPerformed(consulo.ide.impl.idea.openapi.actionSystem.AnActionEvent)} is in stack trace,
 * consider making the action not implement {@link DumbAware}.
 *
 * <li> A {@link DumbAware} action, having got this exception, may just notify the user that the requested activity is not possible while
 * indexing is in progress. It can be done via a dialog (see {@link consulo.ide.impl.idea.openapi.ui.Messages}) or a status bar balloon
 * (see {@link DumbService#showDumbModeNotification(String)}, {@link consulo.ide.impl.idea.openapi.actionSystem.ex.ActionUtil#showDumbModeWarning(consulo.ide.impl.idea.openapi.actionSystem.AnActionEvent...)}).
 *
 * <li> If index access is performed from some non-urgent invokeLater activity, consider replacing it with
 * {@link DumbService#smartInvokeLater(Runnable)}. Note that this 'later' can be very late, several minutes may pass. So if that code
 * involves user interaction, {@link DumbService#smartInvokeLater(Runnable)} should probably not be used to avoid dialogs popping out of the blue.
 *
 * <li> If it's a non-urgent background process (e.g. compilation, usage search), consider replacing topmost read-action with
 * {@link DumbService#runReadActionInSmartMode(java.util.function.Supplier)}.
 *
 * <li> If the exception comes from within Java's findClass call, and the IDE is currently performing a user-initiated action or a
 * task when skipping findClass would lead to very negative consequences (e.g. not stopping at a breakpoint), then it might be possible
 * to avoid index query by using alternative resolve (and findClass) strategy, which is significantly slower and might return null. To do this,
 * use {@link DumbService#setAlternativeResolveEnabled(boolean)}.
 *
 * <li> If you're performing a long modal operation which leads to a root change in the middle (or otherwise causes indexing),
 * but you need indices after that, you can call {@link DumbService#completeJustSubmittedTasks()} before performing
 * those index queries.
 *
 * <li> It's preferable to avoid the exception entirely by adding {@link DumbService#isDumb()} checks where necessary.
 * </ul>
 *
 * @author peter
 * @see DumbService
 * @see DumbAware
 */
public class IndexNotReadyException extends RuntimeException implements ExceptionWithAttachments {
  @Nullable
  private final Throwable myStartTrace;

  // constructor is private to not let ForkJoinTask.getThrowableException() clone this by reflection causing invalid nesting etc
  private IndexNotReadyException(@Nullable Throwable startTrace) {
    super("Please change caller according to " + IndexNotReadyException.class.getName() + " documentation");
    myStartTrace = startTrace;
  }

  @Nonnull
  @Override
  public Attachment[] getAttachments() {
    return myStartTrace == null ? Attachment.EMPTY_ARRAY : new Attachment[]{AttachmentFactory.get().create("indexingStart", myStartTrace)};
  }

  @Nonnull
  public static IndexNotReadyException create() {
    return create(null);
  }

  @Nonnull
  public static IndexNotReadyException create(@Nullable Throwable startTrace) {
    return new IndexNotReadyException(startTrace);
  }
}
