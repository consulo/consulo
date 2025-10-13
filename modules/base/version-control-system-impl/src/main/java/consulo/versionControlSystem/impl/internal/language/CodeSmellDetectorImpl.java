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
package consulo.versionControlSystem.impl.internal.language;

import consulo.annotation.component.ServiceImpl;
import consulo.application.ApplicationManager;
import consulo.application.internal.AbstractProgressIndicatorExBase;
import consulo.application.internal.ProgressIndicatorEx;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.component.ProcessCanceledException;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.util.TextRange;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.inspection.HighlightInfoTypeSeverityByKey;
import consulo.language.editor.internal.DaemonCodeAnalyzerInternal;
import consulo.language.editor.internal.DaemonProgressIndicator;
import consulo.language.editor.rawHighlight.HighlightDisplayKey;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.editor.rawHighlight.HighlightInfoType;
import consulo.language.editor.rawHighlight.SeverityRegistrar;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.logging.Logger;
import consulo.navigation.OpenFileDescriptor;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.MessageCategory;
import consulo.ui.ex.errorTreeView.NewErrorTreeViewPanel;
import consulo.ui.ex.errorTreeView.NewErrorTreeViewPanelFactory;
import consulo.util.lang.ExceptionUtil;
import consulo.util.lang.ref.Ref;
import consulo.versionControlSystem.AbstractVcsHelper;
import consulo.versionControlSystem.CodeSmellDetector;
import consulo.versionControlSystem.CodeSmellInfo;
import consulo.versionControlSystem.impl.internal.AbstractVcsHelperImpl;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author yole
 */
@Singleton
@ServiceImpl
public class CodeSmellDetectorImpl extends CodeSmellDetector {
    private final Project myProject;
    private static final Logger LOG = Logger.getInstance(CodeSmellDetectorImpl.class);

    @Inject
    public CodeSmellDetectorImpl(Project project) {
        myProject = project;
    }

    @Override
    public void showCodeSmellErrors(@Nonnull List<CodeSmellInfo> smellList) {
        Collections.sort(smellList, (o1, o2) -> o1.getTextRange().getStartOffset() - o2.getTextRange().getStartOffset());

        myProject.getApplication().invokeLater(() -> {
            if (myProject.isDisposed()) {
                return;
            }
            if (smellList.isEmpty()) {
                return;
            }

            NewErrorTreeViewPanel errorTreeView = myProject.getApplication().getInstance(NewErrorTreeViewPanelFactory.class).createPanel(myProject, null);
            errorTreeView.setCanHideWarningsOrInfos(false);

            AbstractVcsHelperImpl helper = (AbstractVcsHelperImpl) AbstractVcsHelper.getInstance(myProject);
            helper.openMessagesView(errorTreeView, VcsLocalize.codeSmellsErrorMessagesTabName().get());

            FileDocumentManager fileManager = FileDocumentManager.getInstance();

            OpenFileDescriptorFactory descriptorFactory = OpenFileDescriptorFactory.getInstance(myProject);
            for (CodeSmellInfo smellInfo : smellList) {
                VirtualFile file = fileManager.getFile(smellInfo.getDocument());

                OpenFileDescriptor navigatable = descriptorFactory.newBuilder(file)
                    .line(smellInfo.getStartLine())
                    .column(smellInfo.getStartColumn())
                    .build();

                String exportPrefix = NewErrorTreeViewPanel.createExportPrefix(smellInfo.getStartLine() + 1);
                String rendererPrefix =
                    NewErrorTreeViewPanel.createRendererPrefix(smellInfo.getStartLine() + 1, smellInfo.getStartColumn() + 1);
                if (smellInfo.getSeverity() == HighlightSeverity.ERROR) {
                    errorTreeView.addMessage(MessageCategory.ERROR, new String[]{smellInfo.getDescription()}, file.getPresentableUrl(), navigatable,
                        exportPrefix, rendererPrefix, null);
                }
                else {//if (smellInfo.getSeverity() == HighlightSeverity.WARNING) {
                    errorTreeView.addMessage(MessageCategory.WARNING, new String[]{smellInfo.getDescription()}, file.getPresentableUrl(),
                        navigatable, exportPrefix, rendererPrefix, null);
                }

            }
        });

    }

    @Nonnull
    @Override
    @RequiredUIAccess
    public List<CodeSmellInfo> findCodeSmells(@Nonnull final List<VirtualFile> filesToCheck) throws ProcessCanceledException {
        UIAccess.assertIsUIThread();
        final List<CodeSmellInfo> result = new ArrayList<>();
        PsiDocumentManager.getInstance(myProject).commitAllDocuments();
        if (ApplicationManager.getApplication().isWriteAccessAllowed()) {
            throw new RuntimeException("Must not run under write action");
        }

        final Ref<Exception> exception = Ref.create();
        ProgressManager.getInstance().run(new Task.Modal(myProject, VcsLocalize.checkingCodeSmellsProgressTitle(), true) {
            @Override
            public void run(@Nonnull ProgressIndicator progress) {
                try {
                    for (int i = 0; i < filesToCheck.size(); i++) {
                        if (progress.isCanceled()) {
                            throw new ProcessCanceledException();
                        }

                        VirtualFile file = filesToCheck.get(i);

                        progress.setTextValue(VcsLocalize.searchingForCodeSmellsProcessingFileProgressText(file.getPresentableUrl()));
                        progress.setFraction((double) i / (double) filesToCheck.size());

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
    private List<CodeSmellInfo> findCodeSmells(@Nonnull final VirtualFile file, @Nonnull ProgressIndicator progress) {
        final List<CodeSmellInfo> result = Collections.synchronizedList(new ArrayList<CodeSmellInfo>());

        final DaemonCodeAnalyzerInternal codeAnalyzer = (DaemonCodeAnalyzerInternal) DaemonCodeAnalyzer.getInstance(myProject);
        final ProgressIndicator daemonIndicator = new DaemonProgressIndicator();
        ((ProgressIndicatorEx) progress).addStateDelegate(new AbstractProgressIndicatorExBase() {
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
                        PsiFile psiFile = PsiManager.getInstance(myProject).findFile(file);
                        Document document = FileDocumentManager.getInstance().getDocument(file);
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
            HighlightSeverity severity = highlightInfo.getSeverity();
            if (SeverityRegistrar.getSeverityRegistrar(myProject).compare(severity, HighlightSeverity.WARNING) >= 0) {
                result.add(new CodeSmellInfo(document, getDescription(highlightInfo),
                    new TextRange(highlightInfo.getStartOffset(), highlightInfo.getEndOffset()), severity));
            }
        }
    }

    private static String getDescription(@Nonnull HighlightInfo highlightInfo) {
        String description = highlightInfo.getDescription();
        HighlightInfoType type = highlightInfo.getType();
        if (type instanceof HighlightInfoTypeSeverityByKey) {
            HighlightDisplayKey severityKey = ((HighlightInfoTypeSeverityByKey) type).getSeverityKey();
            String id = severityKey.getID();
            return "[" + id + "] " + description;
        }
        return description;
    }
}
