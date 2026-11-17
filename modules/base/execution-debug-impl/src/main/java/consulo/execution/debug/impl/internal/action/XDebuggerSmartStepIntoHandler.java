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
package consulo.execution.debug.impl.internal.action;

import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.XSourcePosition;
import consulo.execution.debug.impl.internal.action.handler.XDebuggerSuspendedActionHandler;
import consulo.execution.debug.impl.internal.ui.DebuggerUIImplUtil;
import consulo.execution.debug.step.HasSmartStepInto;
import consulo.execution.debug.step.XSmartStepIntoHandler;
import consulo.execution.debug.step.XSmartStepIntoVariant;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.TextEditor;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.ex.popup.PopupStep;
import consulo.ui.image.Image;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * @author nik
 */
public class XDebuggerSmartStepIntoHandler extends XDebuggerSuspendedActionHandler {
    private static final Logger LOG = Logger.getInstance(XDebuggerSmartStepIntoHandler.class);

    @Override
    protected boolean isEnabled(XDebugSession session, DataContext dataContext) {
        return super.isEnabled(session, dataContext) && session.getDebugProcess() instanceof HasSmartStepInto;
    }

    @Override
    public boolean isHidden(Project project, AnActionEvent event) {
        return isHiddenWithout(HasSmartStepInto.class, project, event, false);
    }

    @Override
    @RequiredUIAccess
    protected void perform(XDebugSession session, DataContext dataContext) {
        XSmartStepIntoHandler<?> handler = session.getDebugProcess() instanceof HasSmartStepInto hasSmartStepInto
            ? hasSmartStepInto.getSmartStepIntoHandler()
            : null;
        XSourcePosition position = session.getTopFramePosition();
        if (position != null && handler != null) {
            FileEditor editor = FileEditorManager.getInstance(session.getProject()).getSelectedEditor(position.getFile());
            if (editor instanceof TextEditor textEditor) {
                doSmartStepInto(handler, position, session, textEditor.getEditor());
                return;
            }
        }
        session.stepInto();
    }

    @RequiredUIAccess
    private <V extends XSmartStepIntoVariant> void doSmartStepInto(XSmartStepIntoHandler<V> handler,
                                                                   XSourcePosition position,
                                                                   XDebugSession session,
                                                                   Editor editor) {
        UIAccess uiAccess = UIAccess.current();
        computeVariants(handler, position).whenComplete((variants, throwable) -> uiAccess.giveIfNeed(() -> {
            if (throwable != null) {
                Throwable cause = throwable instanceof CompletionException && throwable.getCause() != null ? throwable.getCause() : throwable;
                if (!(cause instanceof UnsupportedOperationException) && !(cause instanceof CancellationException)) {
                    LOG.warn("Exception while computing smart step into variants, falling back to step into", cause);
                }
                session.stepInto();
            }
            else if (!handleSimpleCases(handler, variants, session)) {
                choose(handler, variants, position, session, editor);
            }
        }));
    }

    protected <V extends XSmartStepIntoVariant> CompletableFuture<List<V>> computeVariants(XSmartStepIntoHandler<V> handler,
                                                                                           XSourcePosition position) {
        return handler.computeSmartStepVariantsAsync(position);
    }

    @RequiredUIAccess
    protected <V extends XSmartStepIntoVariant> boolean handleSimpleCases(XSmartStepIntoHandler<V> handler,
                                                                          List<? extends V> variants,
                                                                          XDebugSession session) {
        if (variants.isEmpty()) {
            handler.stepIntoEmpty(session);
            return true;
        }
        else if (variants.size() == 1) {
            session.smartStepInto(handler, variants.get(0));
            return true;
        }
        return false;
    }

    @RequiredUIAccess
    private static <V extends XSmartStepIntoVariant> void choose(XSmartStepIntoHandler<V> handler,
                                                                 List<? extends V> variants,
                                                                 XSourcePosition position,
                                                                 XDebugSession session,
                                                                 Editor editor) {
        ListPopup popup = JBPopupFactory.getInstance().createListPopup(new BaseListPopupStep<V>(handler.getPopupTitle(position), variants) {
            @Override
            public Image getIconFor(V aValue) {
                return aValue.getIcon();
            }

            @Override
            public String getTextFor(V value) {
                return value.getText();
            }

            @Override
            public PopupStep onChosen(V selectedValue, boolean finalChoice) {
                session.smartStepInto(handler, selectedValue);
                return FINAL_CHOICE;
            }
        });
        DebuggerUIImplUtil.showPopupForEditorLine(popup, editor, position.getLine());
    }
}
