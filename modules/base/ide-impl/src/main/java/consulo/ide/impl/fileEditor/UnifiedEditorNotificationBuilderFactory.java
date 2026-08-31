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
package consulo.ide.impl.fileEditor;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.colorScheme.EditorColorsManager;
import consulo.dataContext.DataManager;
import consulo.fileEditor.EditorNotificationBuilder;
import consulo.fileEditor.internal.EditorNotificationBuilderFactory;
import consulo.ui.ex.action.ActionManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 2026-08-13
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.UNIFIED)
public class UnifiedEditorNotificationBuilderFactory implements EditorNotificationBuilderFactory {
    private final EditorColorsManager myEditorColorsManager;
    private final ActionManager myActionManager;
    private final DataManager myDataManager;

    @Inject
    public UnifiedEditorNotificationBuilderFactory(EditorColorsManager editorColorsManager, ActionManager actionManager, DataManager dataManager) {
        myEditorColorsManager = editorColorsManager;
        myActionManager = actionManager;
        myDataManager = dataManager;
    }

    @Override
    public EditorNotificationBuilder newBuilder() {
        return new UnifiedEditorNotificationPanel(myEditorColorsManager, myActionManager, myDataManager);
    }
}
