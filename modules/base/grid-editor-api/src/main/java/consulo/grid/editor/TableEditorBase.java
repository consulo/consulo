// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

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
package consulo.grid.editor;

import consulo.dataContext.DataSink;
import consulo.dataContext.UiDataProvider;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorState;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.grid.DataGrid;
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.GridDataHookUp;
import consulo.ui.grid.GridRow;
import consulo.ui.layout.DockLayout;
import consulo.util.dataholder.UserDataHolderBase;
import kava.beans.PropertyChangeListener;
import kava.beans.PropertyChangeSupport;
import org.jspecify.annotations.Nullable;

/**
 * A file editor which shows a {@link DataGrid}.
 * <p/>
 * The component is built on the UI thread when it is first asked for: {@link #createUIComponent()} - by default the grid itself - goes
 * into a wrapper which provides the data of {@link #uiDataSnapshot}. The grid keeps its own data - the grid key, copy and delete - as the
 * nearest provider.
 *
 * @author gregsh
 */
public abstract class TableEditorBase extends UserDataHolderBase implements FileEditor {
    private final Project myProject;
    private final PropertyChangeSupport myPropertyChangeSupport = new PropertyChangeSupport(this);
    private @Nullable Component myComponent;

    protected TableEditorBase(Project project) {
        myProject = project;
    }

    public Project getProject() {
        return myProject;
    }

    /**
     * @return the grid of the editor; a subclass may create it on the first call, which comes on the UI thread
     */
    @RequiredUIAccess
    public abstract DataGrid getDataGrid();

    @RequiredUIAccess
    public GridDataHookUp<GridRow, GridColumn> getDataHookup() {
        return getDataGrid().getDataHookup();
    }

    @Override
    public String getName() {
        return LocalizeValue.localizeTODO("Data").get();
    }

    @Override
    public void setState(FileEditorState state) {
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        myPropertyChangeSupport.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        myPropertyChangeSupport.removePropertyChangeListener(listener);
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void dispose() {
    }

    @Override
    @RequiredUIAccess
    public Component getUIComponent() {
        Component component = myComponent;
        if (component == null) {
            DockLayout wrapper = DockLayout.create(Space.NONE);
            wrapper.center(createUIComponent());
            wrapper.putUserData(UiDataProvider.KEY, this::uiDataSnapshot);
            component = wrapper;
            myComponent = component;
        }
        return component;
    }

    /**
     * Consulo: the content of the editor, built once. The default is the grid; a subclass puts other components around it.
     */
    @RequiredUIAccess
    protected Component createUIComponent() {
        return getDataGrid();
    }

    protected void uiDataSnapshot(DataSink sink) {
    }

    @Override
    @RequiredUIAccess
    public @Nullable Component getPreferredFocusedUIComponent() {
        return getDataGrid();
    }
}
