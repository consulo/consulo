/*
 * Copyright 2013-2026 consulo.io
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
package consulo.codeEditor.impl;

import consulo.application.concurrent.ApplicationConcurrency;
import consulo.application.concurrent.MergingProcessingQueue;
import consulo.codeEditor.Editor;
import consulo.codeEditor.internal.*;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.disposer.util.DisposerUtil;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.Lists;
import org.jspecify.annotations.Nullable;

import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-03-12
 */
public class EditorMarkupModelImpl<E extends CodeEditorBase> extends MarkupModelImpl implements ErrorStripeMarkupModel {
    protected final E myEditor;

    // null renderer means we should not show traffic light icon
    protected ErrorStripeRenderer myErrorStripeRenderer;
    private final List<ErrorStripeListener> myErrorMarkerListeners = Lists.newLockFreeCopyOnWriteList();

    private final MergingProcessingQueue<String, EditorAnalyzeStatus, Project> myStatusUpdates;

    private volatile EditorAnalyzeStatus myLastEditorStatus;

    public EditorMarkupModelImpl(E editor) {
        super(editor.getDocument());
        myEditor = editor;

        Project project = Objects.requireNonNull(editor.getProject());
        ApplicationConcurrency concurrency = project.getApplication().getInstance(ApplicationConcurrency.class);
        myStatusUpdates = new MergingProcessingQueue<>(concurrency, project, 100) {
            @Override
            protected UIAccess getUIAccess(Project project) {
                return project.getUIAccess();
            }

            @Override
            protected void calculateValue(Project project, String key, Consumer<EditorAnalyzeStatus> consumer) {
                ErrorStripeRenderer renderer = myErrorStripeRenderer;
                if (renderer == null) {
                    return;
                }

                project.getApplication().tryRunReadAction(() -> consumer.accept(renderer.getStatus(editor)));
            }

            @Override
            protected void updateValueInsideUI(Project project, String key, EditorAnalyzeStatus newStatus) {
                tryToUpdateStatus(newStatus);

                repaintErrorPanel();
            }
        };
    }

    @Nullable
    public EditorAnalyzeStatus getLastEditorStatus() {
        return myLastEditorStatus;
    }

    public void repaintTrafficLightIcon() {
        myStatusUpdates.queueAdd("update");
    }

    protected boolean tryToUpdateStatus(EditorAnalyzeStatus status) {
        return false;
    }

    protected void repaintErrorPanel() {
    }

    @RequiredUIAccess
    protected void fireErrorMarkerClicked(RangeHighlighter marker, MouseEvent e) {
        UIAccess.assertIsUIThread();
        ErrorStripeEvent event = new ErrorStripeEvent(getEditor(), e, marker);
        for (ErrorStripeListener listener : myErrorMarkerListeners) {
            listener.errorMarkerClicked(event);
        }
    }

    @Override
    public void addErrorMarkerListener(ErrorStripeListener listener, Disposable parent) {
        DisposerUtil.add(listener, myErrorMarkerListeners, parent);
    }

    @Override
    @RequiredUIAccess
    public void setErrorStripeRenderer(ErrorStripeRenderer renderer) {
        UIAccess.assertIsUIThread();
        if (myErrorStripeRenderer instanceof Disposable disposable) {
            Disposer.dispose(disposable);
        }
        myErrorStripeRenderer = renderer;
    }

    @Override
    public ErrorStripeRenderer getErrorStripeRenderer() {
        return myErrorStripeRenderer;
    }

    public Editor getEditor() {
        return myEditor;
    }

    @Override
    public void dispose() {
        super.dispose();

        if (myErrorStripeRenderer instanceof Disposable disposable) {
            Disposer.dispose(disposable);
        }

        myErrorStripeRenderer = null;
    }
}
