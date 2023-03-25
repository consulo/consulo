// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.language.editor.impl.internal.inspection;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.impl.highlight.HighlightInfoProcessor;
import consulo.language.editor.impl.highlight.TextEditorHighlightingPass;
import consulo.language.editor.impl.highlight.TextEditorHighlightingPassManager;
import consulo.language.editor.impl.inspection.GlobalInspectionContextBase;
import consulo.language.editor.impl.internal.daemon.DaemonProgressIndicator;
import consulo.language.editor.impl.internal.highlight.GeneralHighlightingPass;
import consulo.language.editor.impl.internal.rawHighlight.DefaultHighlightVisitor;
import consulo.language.editor.impl.internal.rawHighlight.HighlightInfoImpl;
import consulo.language.editor.inspection.*;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.editor.rawHighlight.HighlightVisitor;
import consulo.language.editor.rawHighlight.HighlightVisitorFactory;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.annotations.Nls;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class DefaultHighlightVisitorBasedInspection extends GlobalSimpleInspectionTool {
  private final boolean highlightErrorElements;
  private final boolean runAnnotators;

  protected DefaultHighlightVisitorBasedInspection(boolean highlightErrorElements, boolean runAnnotators) {
    this.highlightErrorElements = highlightErrorElements;
    this.runAnnotators = runAnnotators;
  }

  @Nonnull
  @Override
  public HighlightDisplayLevel getDefaultLevel() {
    return HighlightDisplayLevel.ERROR;
  }

  @RequiredReadAction
  @Override
  public void checkFile(@Nonnull PsiFile originalFile,
                        @Nonnull InspectionManager manager,
                        @Nonnull ProblemsHolder problemsHolder,
                        @Nonnull GlobalInspectionContext globalContext,
                        @Nonnull ProblemDescriptionsProcessor problemDescriptionsProcessor,
                        @Nonnull Object state) {
    for (Pair<PsiFile, HighlightInfo> pair : runAnnotatorsInGeneralHighlighting(originalFile, highlightErrorElements, runAnnotators)) {
      PsiFile file = pair.first;
      HighlightInfoImpl info = (HighlightInfoImpl)pair.second;
      TextRange range = new TextRange(info.startOffset, info.endOffset);
      PsiElement element = file.findElementAt(info.startOffset);

      while (element != null && !element.getTextRange().contains(range)) {
        element = element.getParent();
      }

      if (element == null) {
        element = file;
      }

      GlobalInspectionUtil.createProblem(element,
                                         info,
                                         range.shiftRight(-element.getNode().getStartOffset()),
                                         info.getProblemGroup(),
                                         manager,
                                         problemDescriptionsProcessor,
                                         globalContext);
    }
  }

  @Nonnull
  public static List<Pair<PsiFile, HighlightInfo>> runAnnotatorsInGeneralHighlighting(@Nonnull PsiFile file,
                                                                                      boolean highlightErrorElements,
                                                                                      boolean runAnnotators) {
    ProgressIndicator indicator = ProgressManager.getGlobalProgressIndicator();
    MyPsiElementVisitor visitor = new MyPsiElementVisitor(highlightErrorElements, runAnnotators);
    if (indicator instanceof DaemonProgressIndicator) {
      file.accept(visitor);
    }
    else {
      ProgressManager.getInstance().runProcess(() -> file.accept(visitor), new DaemonProgressIndicator());
    }
    return visitor.result;
  }

  @Nls
  @Nonnull
  @Override
  public String getGroupDisplayName() {
    return getGeneralGroupName();
  }

  private static class MyPsiElementVisitor extends PsiElementVisitor {
    private final boolean highlightErrorElements;
    private final boolean runAnnotators;
    private final List<Pair<PsiFile, HighlightInfo>> result = new ArrayList<>();

    MyPsiElementVisitor(boolean highlightErrorElements, boolean runAnnotators) {
      this.highlightErrorElements = highlightErrorElements;
      this.runAnnotators = runAnnotators;
    }

    @Override
    public void visitFile(@Nonnull PsiFile file) {
      VirtualFile virtualFile = file.getVirtualFile();
      if (virtualFile == null) {
        return;
      }

      result.addAll(runAnnotatorsInGeneralHighlightingPass(file, highlightErrorElements, runAnnotators));
    }
  }

  @Nonnull
  private static List<Pair<PsiFile, HighlightInfo>> runAnnotatorsInGeneralHighlightingPass(@Nonnull PsiFile file,
                                                                                           boolean highlightErrorElements,
                                                                                           boolean runAnnotators) {
    Project project = file.getProject();
    Document document = PsiDocumentManager.getInstance(project).getDocument(file);
    if (document == null) return Collections.emptyList();
    ProgressIndicator progress = ProgressManager.getGlobalProgressIndicator();
    GlobalInspectionContextBase.assertUnderDaemonProgress();

    TextEditorHighlightingPassManager passRegistrarEx = TextEditorHighlightingPassManager.getInstance(project);
    List<TextEditorHighlightingPass> passes = passRegistrarEx.instantiateMainPasses(file, document, HighlightInfoProcessor.getEmpty());
    List<GeneralHighlightingPass> gpasses = ContainerUtil.filterIsInstance(passes, GeneralHighlightingPass.class);
    for (GeneralHighlightingPass gpass : gpasses) {
      gpass.setHighlightVisitorProducer(() -> List.of(new HighlightVisitorFactory() {

        @Override
        public boolean suitableForFile(@Nonnull PsiFile file) {
          return true;
        }

        @Nonnull
        @Override
        public HighlightVisitor createVisitor() {
          gpass.incVisitorUsageCount(1);

          return new DefaultHighlightVisitor(project, highlightErrorElements, runAnnotators, true);
        }
      }));
    }

    List<Pair<PsiFile, HighlightInfo>> result = new ArrayList<>();
    for (TextEditorHighlightingPass pass : gpasses) {
      pass.doCollectInformation(progress);
      List<HighlightInfo> infos = pass.getInfos();
      for (HighlightInfo info : infos) {
        if (info != null && info.getSeverity().compareTo(HighlightSeverity.INFORMATION) > 0) {
          result.add(Pair.create(file, info));
        }
      }
    }
    return result;
  }
}
