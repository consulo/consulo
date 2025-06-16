/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.language.codeStyle.impl.internal.autoDetect;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.ReadAction;
import consulo.application.internal.ProgressIndicatorUtils;
import consulo.application.progress.DumbProgressIndicator;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressIndicatorProvider;
import consulo.application.util.concurrent.SequentialTaskExecutor;
import consulo.document.Document;
import consulo.language.codeStyle.CodeStyle;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.CodeStyleSettingsManager;
import consulo.language.codeStyle.CommonCodeStyleSettings.IndentOptions;
import consulo.language.codeStyle.impl.internal.TimeStampedIndentOptions;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.lang.EmptyRunnable;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;


class DetectAndAdjustIndentOptionsTask {
  private static final ExecutorService BOUNDED_EXECUTOR = SequentialTaskExecutor.createSequentialApplicationPoolExecutor("DetectableIndentOptionsProvider Pool");
  private static final Logger LOG = Logger.getInstance(DetectAndAdjustIndentOptionsTask.class);
  private static final int INDENT_COMPUTATION_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(5);

  private final Document myDocument;
  private final Project myProject;
  private final TimeStampedIndentOptions myOptionsToAdjust;

  DetectAndAdjustIndentOptionsTask(@Nonnull Project project, @Nonnull Document document, @Nonnull TimeStampedIndentOptions toAdjust) {
    myProject = project;
    myDocument = document;
    myOptionsToAdjust = toAdjust;
  }

  private PsiFile getFile() {
    return PsiDocumentManager.getInstance(myProject).getPsiFile(myDocument);
  }

  @Nonnull
  private Runnable calcIndentAdjuster(@Nonnull ProgressIndicator indicator) {
    PsiFile file = getFile();
    IndentOptionsAdjuster adjuster = file == null ? null : new IndentOptionsDetectorImpl(file, indicator).getIndentOptionsAdjuster();
    return adjuster != null ? () -> adjustOptions(adjuster) : EmptyRunnable.INSTANCE;
  }

  private void adjustOptions(IndentOptionsAdjuster adjuster) {
    PsiFile file = getFile();
    if (file == null) return;

    IndentOptions currentDefault = getDefaultIndentOptions(file, myDocument);
    myOptionsToAdjust.copyFrom(currentDefault);

    adjuster.adjust(myOptionsToAdjust);
    myOptionsToAdjust.setTimeStamp(myDocument.getModificationStamp());
    myOptionsToAdjust.setOriginalIndentOptionsHash(currentDefault.hashCode());

    if (!currentDefault.equals(myOptionsToAdjust)) {
      myOptionsToAdjust.setDetected(true);
      myOptionsToAdjust.setOverrideLanguageOptions(true);
      CodeStyleSettingsManager.getInstance(myProject).fireCodeStyleSettingsChanged(file);
    }
  }

  private void logTooLongComputation() {
    PsiFile file = getFile();
    String fileName = file != null ? file.getName() : "";
    LOG.debug("Indent detection is too long for: " + fileName);
  }

  void scheduleInBackgroundForCommittedDocument() {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      PsiDocumentManager.getInstance(myProject).commitDocument(myDocument);
      calcIndentAdjuster(new DumbProgressIndicator()).run();
    }
    else {
      ReadAction.nonBlocking(() -> {
        Runnable indentAdjuster = ProgressIndicatorUtils.withTimeout(INDENT_COMPUTATION_TIMEOUT, () -> calcIndentAdjuster(Objects.requireNonNull(ProgressIndicatorProvider.getGlobalProgressIndicator())));
        if (indentAdjuster == null) {
          logTooLongComputation();
          return EmptyRunnable.INSTANCE;
        }
        return indentAdjuster;
      }).finishOnUiThread(Application::getDefaultModalityState, Runnable::run).withDocumentsCommitted(myProject).submit(BOUNDED_EXECUTOR);
    }
  }

  @Nonnull
  static TimeStampedIndentOptions getDefaultIndentOptions(@Nonnull PsiFile file, @Nonnull Document document) {
    FileType fileType = file.getFileType();
    CodeStyleSettings settings = CodeStyle.getSettings(file);
    return new TimeStampedIndentOptions(settings.getIndentOptions(fileType), document.getModificationStamp());
  }


}