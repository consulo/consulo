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
package consulo.project.ui.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.dataContext.DataSink;
import consulo.dataContext.DataSnapshot;
import consulo.dataContext.UiDataRule;
import consulo.project.Project;
import consulo.util.concurrent.coroutine.CoroutineContext;
import jakarta.inject.Inject;

/**
 * @author VISTALL
 * @since 2026-06-28
 */
@ExtensionImpl
public class CoroutineContextUiDataRule implements UiDataRule {
    private final Application myApplication;

    @Inject
    public CoroutineContextUiDataRule(Application application) {
        myApplication = application;
    }

    @Override
    public void uiDataSnapshot(DataSink sink, DataSnapshot snapshot) {
        sink.lazyValue(CoroutineContext.KEY, dataSnapshot -> {
            Project project = dataSnapshot.get(Project.KEY);
            if (project != null) {
                return project.coroutineContext();
            }

            return myApplication.coroutineContext();
        });
    }
}
