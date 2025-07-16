/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.internal;

import consulo.application.internal.ApplicationManagerEx;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.StringUtil;
import consulo.application.dumb.DumbAware;
import consulo.application.progress.ProgressManager;
import consulo.logging.Logger;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.virtualFileSystem.RawFileLoader;
import consulo.virtualFileSystem.VFileProperty;
import jakarta.annotation.Nonnull;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author anna
 * @since 2007-06-28
 */
public class LoadAllContentsAction extends AnAction implements DumbAware {
  private static final Logger LOG = Logger.getInstance(LoadAllContentsAction.class);
  public LoadAllContentsAction() {
    super("Load all files content", "Measure FileUtil.loadFile() for all files in the project", null);
  }


  AtomicInteger count = new AtomicInteger();
  AtomicLong totalSize = new AtomicLong();

  @Override
  @RequiredUIAccess
  public void actionPerformed(@Nonnull AnActionEvent e) {
    Project project = e.getRequiredData(Project.KEY);
    String m = "Started loading content";
    LOG.info(m);
    System.out.println(m);
    long start = System.currentTimeMillis();
    count.set(0);
    totalSize.set(0);
    ApplicationManagerEx.getApplicationEx().runProcessWithProgressSynchronously(
      () -> ProjectRootManager.getInstance(project).getFileIndex().iterateContent(fileOrDir -> {
        if (fileOrDir.isDirectory() || fileOrDir.is(VFileProperty.SPECIAL)) return true;
        try {
          count.incrementAndGet();
          byte[] bytes = RawFileLoader.getInstance().loadFileBytes(new File(fileOrDir.getPath()));
          totalSize.addAndGet(bytes.length);
          ProgressManager.getInstance().getProgressIndicator().setText(fileOrDir.getPresentableUrl());
        }
        catch (IOException e1) {
          LOG.error(e1);
        }
        return true;
      }), "Loading", false, project);
    long end = System.currentTimeMillis();
    String message = "Finished loading content of " + count + " files. Total size=" + StringUtil.formatFileSize(totalSize.get()) +
                     ". Elapsed=" + ((end - start) / 1000) + "sec.";
    LOG.info(message);
    System.out.println(message);
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    e.getPresentation().setEnabled(e.hasData(Project.KEY));
  }
}