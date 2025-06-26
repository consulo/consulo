/*
 * Copyright 2013-2023 consulo.io
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
package consulo.ide.impl.idea.find;

import consulo.dataContext.DataProvider;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.EventListener;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 27/06/2023
 */
public interface SearchReplaceComponent extends DataProvider {
    public interface Listener extends EventListener {
        void searchFieldDocumentChanged();

        void replaceFieldDocumentChanged();

        void multilineStateChanged();
    }

    @Nonnull
    public static SearchReplaceComponentBuilder buildFor(@Nullable Project project, @Nonnull JComponent component) {
        return new SearchReplaceComponentBuilder(project, component);
    }

    void setRegularBackground();

    void setNotFoundBackground();

    JComponent getComponent();

    void setStatusText(@Nonnull String status);

    void resetUndoRedoActions();

    void updateActions();

    boolean isMultiline();

    void addListener(@Nonnull Listener listener);

    Project getProject();

    String getSearchText();

    void setSearchText(String text);

    String getReplaceText();

    void setReplaceText(String text);

    void update(@Nonnull String findText, @Nonnull String replaceText, boolean replaceMode, boolean multiline);

    void selectSearchAll();

    void requestFocusInTheSearchFieldAndSelectContent(Project project);

    String getStatusText();

    @Nonnull
    Color getStatusColor();

    void addTextToRecent(@Nonnull String text, boolean search);

    void updateEmptyText(Supplier<String> textSupplier);

    boolean isJustClearedSearch();

    @Nonnull
    @RequiredUIAccess
    CompletableFuture<?> prepareAsync();
}
