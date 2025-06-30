/*
 * Copyright 2013-2022 consulo.io
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
package consulo.desktop.awt.fileEditor.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.colorScheme.EditorColorsManager;
import consulo.fileEditor.EditorNotificationBuilder;
import consulo.fileEditor.internal.EditorNotificationBuilderFactory;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 04-Aug-22
 */
@Singleton
@ServiceImpl
public class DesktopAWTEditorNotificationBuilderFactory implements EditorNotificationBuilderFactory {
    private final EditorColorsManager myEditorColorsManager;

    @Inject
    public DesktopAWTEditorNotificationBuilderFactory(EditorColorsManager editorColorsManager) {
        myEditorColorsManager = editorColorsManager;
    }

    @Override
    public EditorNotificationBuilder newBuilder() {
        return new DesktopAWTNotificationPanel(myEditorColorsManager);
    }
}
