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
package consulo.diff.impl.internal;

import consulo.application.Application;
import consulo.diff.DiffRequestPanel;
import consulo.diff.DiffUserDataKeys;
import consulo.diff.internal.DiffUserDataKeysEx;
import consulo.diff.request.DiffRequest;
import consulo.diff.request.NoDiffRequest;
import consulo.disposer.Disposer;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.UIAccess;
import consulo.ui.Window;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

public class DiffRequestPanelImpl implements DiffRequestPanel {
    private final MyDiffRequestProcessor myProcessor;

    @RequiredUIAccess
    public DiffRequestPanelImpl(@Nullable Project project, @Nullable Window window) {
        myProcessor = new MyDiffRequestProcessor(project, window);
        myProcessor.putContextUserData(DiffUserDataKeys.DO_NOT_CHANGE_WINDOW_TITLE, true);

        UIAccess.current().give(() -> myProcessor.updateRequest());
    }

    @Override
    public void setRequest(@Nullable DiffRequest request) {
        setRequest(request, null);
    }

    @Override
    public void setRequest(@Nullable DiffRequest request, @Nullable Object identity) {
        myProcessor.setRequest(request, identity);
    }

    @RequiredUIAccess
    @Override
    public <T> void putContextHints(Key<T> key, @Nullable T value) {
        myProcessor.putContextUserData(key, value);
    }

    @Override
    public Component getComponent() {
        return myProcessor.getComponent();
    }

    @Override
    public @Nullable Component getPreferredFocusedComponent() {
        return myProcessor.getPreferredFocusedComponent();
    }

    @Override
    public void dispose() {
        Disposer.dispose(myProcessor);
    }

    private static class MyDiffRequestProcessor extends DiffRequestProcessor {
        private final @Nullable Window myWindow;

        private DiffRequest myRequest = NoDiffRequest.INSTANCE;
        private @Nullable Object myRequestIdentity = null;

        @RequiredUIAccess
        public MyDiffRequestProcessor(@Nullable Project project, @Nullable Window window) {
            super(project);
            myWindow = window;
        }

        public synchronized void setRequest(@Nullable DiffRequest request, @Nullable Object identity) {
            if (myRequestIdentity != null && identity != null && myRequestIdentity.equals(identity)) {
                return;
            }

            myRequest = request != null ? request : NoDiffRequest.INSTANCE;
            myRequestIdentity = identity;

            if (UIAccess.isUIThread()) {
                updateRequest();
            }
            else {
                Application.get().invokeLater(() -> updateRequest());
            }
        }

        @Override
        @RequiredUIAccess
        public synchronized void updateRequest(boolean force, DiffUserDataKeysEx.@Nullable ScrollToPolicy scrollToChangePolicy) {
            applyRequest(myRequest, force, scrollToChangePolicy);
        }

        @Override
        @RequiredUIAccess
        protected void setWindowTitle(String title) {
            if (myWindow != null) {
                myWindow.setTitle(title);
            }
        }
    }
}
