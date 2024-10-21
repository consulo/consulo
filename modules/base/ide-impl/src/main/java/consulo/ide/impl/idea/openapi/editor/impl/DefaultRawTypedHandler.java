// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.editor.impl;

import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.*;
import consulo.dataContext.DataContext;
import consulo.document.Document;
import consulo.document.DocumentRunnable;
import consulo.document.ReadOnlyFragmentModificationException;
import consulo.ide.impl.idea.openapi.command.CommandProcessorEx;
import consulo.ide.impl.idea.openapi.command.CommandToken;
import consulo.ide.impl.idea.openapi.editor.EditorModificationUtil;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.CommandProcessor;
import jakarta.annotation.Nonnull;

public class DefaultRawTypedHandler implements TypedActionHandlerEx {
    private final TypedAction myAction;
    private CommandToken myCurrentCommandToken;
    private boolean myInOuterCommand;

    DefaultRawTypedHandler(TypedAction action) {
        myAction = action;
    }

    @Override
    public void beforeExecute(@Nonnull Editor editor, char c, @Nonnull DataContext context, @Nonnull ActionPlan plan) {
        if (editor.isViewer() || !editor.getDocument().isWritable()) {
            return;
        }

        TypedActionHandler handler = myAction.getHandler();

        if (handler instanceof TypedActionHandlerEx) {
            ((TypedActionHandlerEx)handler).beforeExecute(editor, c, context, plan);
        }
    }

    @Override
    @RequiredUIAccess
    public void execute(@Nonnull final Editor editor, final char charTyped, @Nonnull final DataContext dataContext) {
        CommandProcessorEx commandProcessorEx = (CommandProcessorEx)CommandProcessor.getInstance();
        Project project = dataContext.getData(Project.KEY);
        if (myCurrentCommandToken != null) {
            throw new IllegalStateException("Unexpected reentrancy of DefaultRawTypedHandler");
        }
        myCurrentCommandToken = commandProcessorEx.newCommand()
            .withProject(project)
            .withGroupId(editor.getDocument())
            .start();
        myInOuterCommand = myCurrentCommandToken == null;
        try {
            if (!EditorModificationUtil.requestWriting(editor)) {
                return;
            }
            Application.get().runWriteAction(new DocumentRunnable(editor.getDocument(), editor.getProject()) {
                @Override
                public void run() {
                    Document doc = editor.getDocument();
                    doc.startGuardedBlockChecking();
                    try {
                        myAction.getHandler().execute(editor, charTyped, dataContext);
                    }
                    catch (ReadOnlyFragmentModificationException e) {
                        EditorActionManager.getInstance().getReadonlyFragmentModificationHandler(doc).handle(e);
                    }
                    finally {
                        doc.stopGuardedBlockChecking();
                    }
                }
            });
        }
        finally {
            if (!myInOuterCommand) {
                commandProcessorEx.finishCommand(myCurrentCommandToken, null);
                myCurrentCommandToken = null;
            }
            myInOuterCommand = false;
        }
    }

    public void beginUndoablePostProcessing() {
        if (myInOuterCommand) {
            return;
        }
        if (myCurrentCommandToken == null) {
            throw new IllegalStateException("Not in a typed action at this time");
        }
        CommandProcessorEx commandProcessorEx = (CommandProcessorEx)CommandProcessor.getInstance();
        Project project = myCurrentCommandToken.getProject();
        commandProcessorEx.finishCommand(myCurrentCommandToken, null);
        myCurrentCommandToken = commandProcessorEx.newCommand().withProject(project).start();
    }
}
