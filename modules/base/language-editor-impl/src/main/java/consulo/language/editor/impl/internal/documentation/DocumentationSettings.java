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
package consulo.language.editor.impl.internal.documentation;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.ApplicationPropertiesComponent;
import consulo.colorScheme.FontSize;
import consulo.disposer.Disposable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author VISTALL
 * @since 2026-10-09
 */
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
@Singleton
public class DocumentationSettings {
    private static final String FONT_SIZE = "quick.doc.font.size";
    private static final String SHOW_IN_TOOL_WINDOW = "ShowDocumentationInToolWindow";
    private static final String AUTO_UPDATE = "DocumentationAutoUpdateEnabled";

    private final ApplicationPropertiesComponent myProperties;

    private final List<Runnable> myFontSizeListeners = new CopyOnWriteArrayList<>();

    @Inject
    public DocumentationSettings(ApplicationPropertiesComponent properties) {
        myProperties = properties;
    }

    public FontSize getFontSize() {
        String value = myProperties.getValue(FONT_SIZE);
        if (value != null) {
            for (FontSize fontSize : FontSize.values()) {
                if (fontSize.name().equals(value)) {
                    return fontSize;
                }
            }
        }
        return FontSize.SMALL;
    }

    public void setFontSize(FontSize fontSize) {
        myProperties.setValue(FONT_SIZE, fontSize.name());

        for (Runnable listener : myFontSizeListeners) {
            listener.run();
        }
    }

    public Disposable addFontSizeListener(Runnable listener) {
        myFontSizeListeners.add(listener);
        return () -> myFontSizeListeners.remove(listener);
    }

    public boolean isShowInToolWindow() {
        return myProperties.isTrueValue(SHOW_IN_TOOL_WINDOW);
    }

    public void setShowInToolWindow(boolean showInToolWindow) {
        myProperties.setValue(SHOW_IN_TOOL_WINDOW, showInToolWindow);
    }

    public boolean isAutoUpdate() {
        return myProperties.getBoolean(AUTO_UPDATE, true);
    }

    public void setAutoUpdate(boolean autoUpdate) {
        myProperties.setValue(AUTO_UPDATE, autoUpdate, true);
    }
}
