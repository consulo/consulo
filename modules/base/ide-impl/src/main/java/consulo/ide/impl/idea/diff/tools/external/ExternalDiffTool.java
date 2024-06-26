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
package consulo.ide.impl.idea.diff.tools.external;

import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.component.ProcessCanceledException;
import consulo.diff.DiffDialogHints;
import consulo.diff.DiffNotificationGroups;
import consulo.diff.chain.DiffRequestChain;
import consulo.diff.chain.DiffRequestProducer;
import consulo.diff.chain.DiffRequestProducerException;
import consulo.diff.chain.SimpleDiffRequestChain;
import consulo.diff.content.DiffContent;
import consulo.diff.request.ContentDiffRequest;
import consulo.diff.request.DiffRequest;
import consulo.ide.impl.idea.diff.DiffManagerEx;
import consulo.util.lang.StringUtil;
import consulo.logging.Logger;
import consulo.process.ExecutionException;
import consulo.project.Project;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationType;
import consulo.ui.ex.awt.Messages;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.util.lang.ref.Ref;

import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExternalDiffTool {
  public static final Logger LOG = Logger.getInstance(ExternalDiffTool.class);

  public static boolean isDefault() {
    return ExternalDiffSettings.getInstance().isDiffEnabled() && ExternalDiffSettings.getInstance().isDiffDefault();
  }

  public static boolean isEnabled() {
    return ExternalDiffSettings.getInstance().isDiffEnabled();
  }

  public static void show(@jakarta.annotation.Nullable final Project project, @Nonnull final DiffRequestChain chain, @Nonnull final DiffDialogHints hints) {
    try {
      //noinspection unchecked
      final Ref<List<DiffRequest>> requestsRef = new Ref<List<DiffRequest>>();
      final Ref<Throwable> exceptionRef = new Ref<Throwable>();
      ProgressManager.getInstance().run(new Task.Modal(project, "Loading Requests", true) {
        @Override
        public void run(@Nonnull ProgressIndicator indicator) {
          try {
            requestsRef.set(collectRequests(project, chain, indicator));
          }
          catch (Throwable e) {
            exceptionRef.set(e);
          }
        }
      });

      if (!exceptionRef.isNull()) throw exceptionRef.get();

      List<DiffRequest> showInBuiltin = new ArrayList<DiffRequest>();
      for (DiffRequest request : requestsRef.get()) {
        if (canShow(request)) {
          showRequest(project, request);
        }
        else {
          showInBuiltin.add(request);
        }
      }

      if (!showInBuiltin.isEmpty()) {
        DiffManagerEx.getInstance().showDiffBuiltin(project, new SimpleDiffRequestChain(showInBuiltin), hints);
      }
    }
    catch (ProcessCanceledException ignore) {
    }
    catch (Throwable e) {
      LOG.error(e);
      Messages.showErrorDialog(project, e.getMessage(), "Can't Show Diff In External Tool");
    }
  }

  @Nonnull
  private static List<DiffRequest> collectRequests(@jakarta.annotation.Nullable Project project, @Nonnull final DiffRequestChain chain, @Nonnull ProgressIndicator indicator) {
    List<DiffRequest> requests = new ArrayList<DiffRequest>();

    UserDataHolderBase context = new UserDataHolderBase();
    List<String> errorRequests = new ArrayList<String>();

    // TODO: show all changes on explicit selection
    List<? extends DiffRequestProducer> producers = Collections.singletonList(chain.getRequests().get(chain.getIndex()));

    for (DiffRequestProducer producer : producers) {
      try {
        requests.add(producer.process(context, indicator));
      }
      catch (DiffRequestProducerException e) {
        LOG.warn(e);
        errorRequests.add(producer.getName());
      }
    }

    if (!errorRequests.isEmpty()) {
      new Notification(DiffNotificationGroups.DIFF, "Can't load some changes", StringUtil.join(errorRequests, "<br>"), NotificationType.ERROR).notify(project);
    }

    return requests;
  }

  public static void showRequest(@jakarta.annotation.Nullable Project project, @Nonnull DiffRequest request) throws ExecutionException, IOException {
    request.onAssigned(true);

    ExternalDiffSettings settings = ExternalDiffSettings.getInstance();

    List<DiffContent> contents = ((ContentDiffRequest)request).getContents();
    List<String> titles = ((ContentDiffRequest)request).getContentTitles();

    ExternalDiffToolUtil.execute(settings, contents, titles, request.getTitle());

    request.onAssigned(false);
  }

  public static boolean canShow(@Nonnull DiffRequest request) {
    if (!(request instanceof ContentDiffRequest)) return false;
    List<DiffContent> contents = ((ContentDiffRequest)request).getContents();
    if (contents.size() != 2 && contents.size() != 3) return false;
    for (DiffContent content : contents) {
      if (!ExternalDiffToolUtil.canCreateFile(content)) return false;
    }
    return true;
  }
}
