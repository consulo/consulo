/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.tools;

import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.ide.ServiceManager;
import consulo.project.Project;
import consulo.util.xml.serializer.annotation.Tag;

import consulo.component.persist.PersistentStateComponent;
import jakarta.inject.Singleton;

/**
 * @author lene
 *         Date: 06.08.12
 */
@Singleton
@State(
  name = "ToolsProjectConfig",
  storages = {@Storage(file = StoragePathMacros.WORKSPACE_FILE)}
)
public class ToolsProjectConfig implements PersistentStateComponent<ToolsProjectConfig.State> {
  private String myAfterCommitToolsId;

  public static ToolsProjectConfig getInstance(final Project project) {
    return ServiceManager.getService(project, ToolsProjectConfig.class);
  }

  public String getAfterCommitToolsId() {
    return myAfterCommitToolsId;
  }

  public void setAfterCommitToolId(String id) {
    myAfterCommitToolsId = id;
  }

  @Override
  public State getState() {
    return new State(myAfterCommitToolsId);
  }

  @Override
  public void loadState(State state) {
    myAfterCommitToolsId = state.getAfterCommitToolId();
  }


  @SuppressWarnings("UnusedDeclaration")
  public static class State {
    private String myAfterCommitToolId;

    public State() {
    }

    public State(String afterCommitToolId) {
      myAfterCommitToolId = afterCommitToolId;
    }

    @Tag(value = "afterCommitToolId")
    public String getAfterCommitToolId() {
      return myAfterCommitToolId;
    }

    public void setAfterCommitToolId(String id) {
      myAfterCommitToolId = id;
    }
  }
}
