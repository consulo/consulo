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
import consulo.ide.ServiceManager;
import consulo.util.xml.serializer.XmlSerializerUtil;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.RoamingType;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.fileChooser.provider.FileOperateDialogProvider;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2018-06-28
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
@State(name = "FileOperateDialogSettings", storages = @Storage(value = "ide.file.dialog.xml", roamingType = RoamingType.PER_OS))
public class FileOperateDialogSettings implements PersistentStateComponent<FileOperateDialogSettings.State> {
  public static class State {
    public String myFileChooseDialogId = FileOperateDialogProvider.APPLICATION_ID;
    public String myFileSaveDialogId = FileOperateDialogProvider.APPLICATION_ID;
  }

  private State myState = new State();

  @Nonnull
  public static FileOperateDialogSettings getInstance() {
    return ServiceManager.getService(FileOperateDialogSettings.class);
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

  @Nullable
  @Override
  public State getState() {
    return myState;
  }

  @Override
  public void loadState(State state) {
    XmlSerializerUtil.copyBean(state, myState);
  }
}
