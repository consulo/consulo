/*
 * Copyright 2013-2018 consulo.io
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
package consulo.ide.impl.fileChooser;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.RoamingType;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2018-06-28
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
@State(name = "FileOperateDialogSettings", storages = @Storage(value = "ide.file.dialog", roamingType = RoamingType.PER_OS))
public class FileOperateDialogSettings implements PersistentStateComponent<FileOperateDialogSettingsState> {

    private FileOperateDialogSettingsState myState = new FileOperateDialogSettingsState();

    public static FileOperateDialogSettings getInstance() {
        return Application.get().getService(FileOperateDialogSettings.class);
    }

    public String getFileChooseDialogId() {
        return myState.myFileChooseDialogId;
    }

    public void setFileChooseDialogId(String fileChooseDialogId) {
        myState.myFileChooseDialogId = fileChooseDialogId;
    }

    public String getFileSaveDialogId() {
        return myState.myFileSaveDialogId;
    }

    public void setFileSaveDialogId(String fileSaveDialogId) {
        myState.myFileSaveDialogId = fileSaveDialogId;
    }

    @Override
    public @Nullable FileOperateDialogSettingsState getState() {
        return myState;
    }

    @Override
    public void loadState(FileOperateDialogSettingsState state) {
        myState = state;
    }
}
