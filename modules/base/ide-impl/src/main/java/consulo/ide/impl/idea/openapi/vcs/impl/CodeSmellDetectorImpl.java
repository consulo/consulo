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
package consulo.ide.impl.idea.openapi.vcs.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.ide.impl.idea.codeInsight.CodeSmellInfo;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.ide.impl.idea.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl;
import consulo.application.impl.internal.progress.DaemonProgressIndicator;
import consulo.ide.impl.idea.codeInsight.daemon.impl.SeverityRegistrarImpl;
import consulo.ide.impl.idea.ide.errorTreeView.NewErrorTreeViewPanelImpl;
import consulo.ide.impl.idea.openapi.fileEditor.OpenFileDescriptorImpl;
import consulo.application.impl.internal.progress.AbstractProgressIndicatorExBase;
import consulo.vcs.AbstractVcsHelper;
import consulo.ide.impl.idea.openapi.vcs.CodeSmellDetector;
import consulo.vcs.VcsBundle;
import consulo.ide.impl.idea.util.ExceptionUtil;
import consulo.ui.ex.MessageCategory;
import consulo.application.ApplicationManager;
import consulo.application.progress.*;
import consulo.component.ProcessCanceledException;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.util.TextRange;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.rawHighlight.HighlightDisplayKey;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.editor.rawHighlight.HighlightInfoType;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.logging.Logger;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.util.lang.ref.Ref;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * @author yole
 */
@Singleton
@ServiceImpl
public class CodeSmellDetectorImpl extends CodeSmellDetector {
  private final Project myProject;
  private static final Logger LOG = Logger.getInstance(CodeSmellDetectorImpl.class);

  @Inject
  public CodeSmellDetectorImpl(final Project project) {
    myProject = project;
  }

  @Override
  public void showCodeSmellErrors(@Nonnull final List<CodeSmellInfo> smellList) {
    Collections.sort(smellList, new Comparator<CodeSmellInfo>() {
      @Override
      public int compare(final CodeSmellInfo o1, final CodeSmellInfo o2) {
        return o1.getTextRange().getStartOffset() - o2.getTextRange().getStartOffset();
      }
    });

    ApplicationManager.getApplication().invokeLater(new Runnable() {
      @Override
      public void run() {
        if (myProject.isDisposed()) return;
        if (smellList.isEmpty()) {
          return;
        }

        final VcsErrorViewPanel errorTreeView = new VcsErrorViewPanel(myProject);
        AbstractVcsHelperImpl helper = (AbstractVcsHelperImpl)AbstractVcsHelper.getInstance(myProject);
        helper.openMessagesView(errorTreeView, VcsBundle.message("code.smells.error.messages.tab.name"));

        FileDocumentManager fileManager = FileDocumentManager.getInstance();

        for (CodeSmellInfo smellInfo : smellList) {
          final VirtualFile file = fileManager.getFile(smellInfo.getDocument());
          final OpenFileDescriptorImpl navigatable =
                  new OpenFileDescriptorImpl(myProject, file, smellInfo.getStartLine(), smellInfo.getStartColumn());
          final String exportPrefix = NewErrorTreeViewPanelImpl.createExportPrefix(smellInfo.getStartLine() + 1);
          final String rendererPrefix =
                  NewErrorTreeViewPanelImpl.createRendererPrefix(smellInfo.getStartLine() + 1, smellInfo.getStartColumn() + 1);
          if (smellInfo.getSeverity() == HighlightSeverity.ERROR) {
            errorTreeView.addMessage(MessageCategory.ERROR, new String[]{smellInfo.getDescription()}, file.getPresentableUrl(), navigatable,
                                     exportPrefix, rendererPrefix, null);
          }
          else {//if (smellInfo.getSeverity() == HighlightSeverity.WARNING) {
            errorTreeView.addMessage(MessageCategory.WARNING, new String[]{smellInfo.getDescription()}, file.getPresentableUrl(),
                                     navigatable, exportPrefix, rendererPrefix, null);
          }

        }
      }
    });

  }

  @Nonnull
  @Override
  public List<CodeSmellInfo> findCodeSmells(@Nonnull final List<VirtualFile> filesToCheck) throws ProcessCanceledException {
    ApplicationManager.getApplication().assertIsDispatchThread();
    final List<CodeSmellInfo> result = new ArrayList<>();
    PsiDocumentManager.getInstance(myProject).commitAllDocuments();
    if (ApplicationManager.getApplication().isWriteAccessAllowed()) throw new RuntimeException("Must not run under write action");

    final Ref<Exception> exception = Ref.create();
    ProgressManager.getInstance().run(new Task.Modal(myProject, VcsBundle.message("checking.code.smells.progress.title"), true) {
      @Override
      public void run(@Nonnull ProgressIndicator progress) {
        try {
          for (int i = 0; i < filesToCheck.size(); i++) {
            if (progress.isCanceled()) throw new ProcessCanceledException();

            final VirtualFile file = filesToCheck.get(i);

            progress.setText(VcsBundle.message("searching.for.code.smells.processing.file.progress.text", file.getPresentableUrl()));
            progress.setFraction((double)i / (double)filesToCheck.size());

            result.addAll(findCodeSmells(file, progress));
          }
        }
        catch (ProcessCanceledException e) {
          exception.set(e);
        }
        catch (Exception e) {
          LOG.error(e);
          exception.set(e);
        }
      }
    });
    if (!exception.isNull()) {
      ExceptionUtil.rethrowAllAsUnchecked(exception.get());
    }

    return result;
  }

  @Nonnull
  private List<CodeSmellInfo> findCodeSmells(@Nonnull final VirtualFile file, @Nonnull final ProgressIndicator progress) {
    final List<CodeSmellInfo> result = Collections.synchronizedList(new ArrayList<CodeSmellInfo>());

    final DaemonCodeAnalyzerImpl codeAnalyzer = (DaemonCodeAnalyzerImpl)DaemonCodeAnalyzer.getInstance(myProject);
    final ProgressIndicator daemonIndicator = new DaemonProgressIndicator();
    ((ProgressIndicatorEx)progress).addStateDelegate(new AbstractProgressIndicatorExBase() {
      @Override
      public void cancel() {
        super.cancel();
        daemonIndicator.cancel();
      }
    });
    ProgressManager.getInstance().runProcess(new Runnable() {
      @Override
      public void run() {
        DumbService.getInstance(myProject).runReadActionInSmartMode(new Runnable() {
          @Override
          public void run() {
            final PsiFile psiFile = PsiManager.getInstance(myProject).findFile(file);
            final Document document = FileDocumentManager.getInstance().getDocument(file);
            if (psiFile == null || document == null) {
              return;
            }
            List<HighlightInfo> infos = codeAnalyzer.runMainPasses(psiFile, document, daemonIndicator);
            convertErrorsAndWarnings(infos, result, document);
          }
        });
      }
    }, daemonIndicator);

    return result;
  }

  private void convertErrorsAndWarnings(@Nonnull Collection<HighlightInfo> highlights,
                                        @Nonnull List<CodeSmellInfo> result,
                                        @Nonnull Document document) {
    for (HighlightInfo highlightInfo : highlights) {
      final HighlightSeverity severity = highlightInfo.getSeverity();
      if (SeverityRegistrarImpl.getSeverityRegistrar(myProject).compare(severity, HighlightSeverity.WARNING) >= 0) {
        result.add(new CodeSmellInfo(document, getDescription(highlightInfo),
                                     new TextRange(highlightInfo.getStartOffset(), highlightInfo.getEndOffset()), severity));
      }
    }
  }

  private static String getDescription(@Nonnull HighlightInfo highlightInfo) {
    final String description = highlightInfo.getDescription();
    final HighlightInfoType type = highlightInfo.getType();
    if (type instanceof HighlightInfoType.HighlightInfoTypeSeverityByKey) {
      final HighlightDisplayKey severityKey = ((HighlightInfoType.HighlightInfoTypeSeverityByKey)type).getSeverityKey();
      final String id = severityKey.getID();
      return "[" + id + "] " + description;
    }
    return description;
  }


}
