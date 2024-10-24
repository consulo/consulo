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
package consulo.ide.impl.idea.codeInsight.actions;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.util.diff.FilesTooBigForDiffException;
import consulo.codeEditor.SelectionModel;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.ide.ServiceManager;
import consulo.ide.impl.psi.codeStyle.arrangement.engine.ArrangementEngine;
import consulo.language.codeStyle.arrangement.Rearranger;
import consulo.language.editor.CodeInsightBundle;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.undoRedo.CommandProcessor;
import consulo.util.collection.SmartList;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.concurrent.FutureTask;

public class RearrangeCodeProcessor extends AbstractLayoutCodeProcessor {
    public static final String COMMAND_NAME = "Rearrange code";
    public static final String PROGRESS_TEXT = CodeInsightBundle.message("process.rearrange.code");

    private static final Logger LOG = Logger.getInstance(RearrangeCodeProcessor.class);
    private SelectionModel mySelectionModel;

    public RearrangeCodeProcessor(@Nonnull AbstractLayoutCodeProcessor previousProcessor) {
        super(previousProcessor, COMMAND_NAME, PROGRESS_TEXT);
    }

    public RearrangeCodeProcessor(@Nonnull AbstractLayoutCodeProcessor previousProcessor, @Nonnull SelectionModel selectionModel) {
        super(previousProcessor, COMMAND_NAME, PROGRESS_TEXT);
        mySelectionModel = selectionModel;
    }

    public RearrangeCodeProcessor(@Nonnull PsiFile file, @Nonnull SelectionModel selectionModel) {
        super(file.getProject(), file, PROGRESS_TEXT, COMMAND_NAME, false);
        mySelectionModel = selectionModel;
    }

    public RearrangeCodeProcessor(@Nonnull PsiFile file) {
        super(file.getProject(), file, PROGRESS_TEXT, COMMAND_NAME, false);
    }

    @RequiredReadAction
    public RearrangeCodeProcessor(
        @Nonnull Project project,
        @Nonnull PsiFile[] files,
        @Nonnull String commandName,
        @Nullable Runnable postRunnable
    ) {
        super(project, files, PROGRESS_TEXT, commandName, postRunnable, false);
    }

    @Nonnull
    @Override
    protected FutureTask<Boolean> prepareTask(@Nonnull final PsiFile file, final boolean processChangedTextOnly) {
        return new FutureTask<>(() -> {
            try {
                Collection<TextRange> ranges = getRangesToFormat(file, processChangedTextOnly);
                Document document = PsiDocumentManager.getInstance(myProject).getDocument(file);

                if (document != null && Rearranger.forLanguage(file.getLanguage()) != null) {
                    PsiDocumentManager.getInstance(myProject).doPostponedOperationsAndUnblockDocument(document);
                    PsiDocumentManager.getInstance(myProject).commitDocument(document);
                    Runnable command = prepareRearrangeCommand(file, ranges);
                    try {
                        CommandProcessor.getInstance().executeCommand(myProject, command, COMMAND_NAME, null);
                    }
                    finally {
                        PsiDocumentManager.getInstance(myProject).commitDocument(document);
                    }
                }

                return true;
            }
            catch (FilesTooBigForDiffException e) {
                handleFileTooBigException(LOG, e, file);
                return false;
            }
        });
    }

    @Nonnull
    private Runnable prepareRearrangeCommand(@Nonnull final PsiFile file, @Nonnull final Collection<TextRange> ranges) {
        final ArrangementEngine engine = ServiceManager.getService(myProject, ArrangementEngine.class);
        return () -> {
            engine.arrange(file, ranges);
            if (getInfoCollector() != null) {
                String info = engine.getUserNotificationInfo();
                getInfoCollector().setRearrangeCodeNotification(info);
            }
        };
    }

    @RequiredReadAction
    public Collection<TextRange> getRangesToFormat(
        @Nonnull PsiFile file,
        boolean processChangedTextOnly
    ) throws FilesTooBigForDiffException {
        if (mySelectionModel != null) {
            return getSelectedRanges(mySelectionModel);
        }

        if (processChangedTextOnly) {
            return FormatChangedTextUtil.getInstance().getChangedTextRanges(myProject, file);
        }

        return new SmartList<>(file.getTextRange());
    }
}
