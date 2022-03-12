// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.codeStyle.autodetect;

import consulo.language.codeStyle.CodeStyle;
import consulo.language.codeStyle.Block;
import consulo.language.codeStyle.FormattingContext;
import consulo.language.codeStyle.FormattingModel;
import consulo.language.codeStyle.FormattingModelBuilder;
import consulo.language.codeStyle.LanguageFormatting;
import com.intellij.openapi.util.io.FileUtil;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.application.progress.ProgressIndicator;
import consulo.document.Document;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.application.dumb.IndexNotReadyException;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static consulo.language.codeStyle.CommonCodeStyleSettings.IndentOptions;

public class IndentOptionsDetectorImpl implements IndentOptionsDetector {
  private final PsiFile myFile;
  private final Project myProject;
  private final Document myDocument;
  private final ProgressIndicator myProgressIndicator;

  public IndentOptionsDetectorImpl(@Nonnull PsiFile file, @Nonnull ProgressIndicator indicator) {
    myFile = file;
    myProject = file.getProject();
    myDocument = PsiDocumentManager.getInstance(myProject).getDocument(myFile);
    myProgressIndicator = indicator;
  }

  @TestOnly
  public IndentOptionsDetectorImpl(@Nonnull PsiFile file) {
    myFile = file;
    myProject = file.getProject();
    myDocument = PsiDocumentManager.getInstance(myProject).getDocument(myFile);
    myProgressIndicator = null;
  }

  @Override
  @Nullable
  public IndentOptionsAdjuster getIndentOptionsAdjuster() {
    try {
      List<LineIndentInfo> linesInfo = calcLineIndentInfo(myProgressIndicator);
      if (linesInfo != null) {
        return new IndentOptionsAdjusterImpl(new IndentUsageStatisticsImpl(linesInfo));
      }
    }
    catch (IndexNotReadyException ignore) {
    }
    return null;
  }

  @Override
  @Nonnull
  public IndentOptions getIndentOptions() {
    IndentOptions indentOptions = (IndentOptions)CodeStyle.getSettings(myFile).getIndentOptions(myFile.getFileType()).clone();

    IndentOptionsAdjuster adjuster = getIndentOptionsAdjuster();
    if (adjuster != null) {
      adjuster.adjust(indentOptions);
    }

    return indentOptions;
  }

  @Nullable
  private List<LineIndentInfo> calcLineIndentInfo(@Nullable ProgressIndicator indicator) {
    if (myDocument == null || myDocument.getLineCount() < 3 || isFileBigToDetect()) {
      return null;
    }

    CodeStyleSettings settings = CodeStyle.getSettings(myFile);
    FormattingModelBuilder modelBuilder = LanguageFormatting.INSTANCE.forContext(myFile);
    if (modelBuilder == null) return null;

    FormattingModel model = modelBuilder.createModel(FormattingContext.create(myFile, settings));
    Block rootBlock = model.getRootBlock();
    return new FormatterBasedLineIndentInfoBuilder(myDocument, rootBlock, indicator).build();
  }

  private boolean isFileBigToDetect() {
    VirtualFile file = myFile.getVirtualFile();
    if (file != null && file.getLength() > FileUtil.MEGABYTE) {
      return true;
    }
    return false;
  }
}

