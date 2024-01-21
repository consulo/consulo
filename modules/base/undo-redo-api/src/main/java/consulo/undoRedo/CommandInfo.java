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
package consulo.undoRedo;

import consulo.document.Document;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2023-12-24
 */
public record CommandInfo(@Nullable Project project, @Nullable String name, @Nullable Object groupId, @Nonnull UndoConfirmationPolicy confirmationPolicy, @Nullable Document document, boolean shouldRecordCommandForActiveDocument) {
  public static class Builder {
    private Project myProject;
    private String myName;
    private Object myGroupId;
    private UndoConfirmationPolicy myUndoConfirmationPolicy = UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION;
    private Document myDocument;
    private boolean myShouldRecordCommandForActiveDocument = true;

    private Builder() {
    }

    public Builder withProject(Project project) {
      myProject = project;
      return this;
    }

    public Builder withName(String name) {
      myName = name;
      return this;
    }

    public Builder withGroupId(Object groupId) {
      myGroupId = groupId;
      return this;
    }

    public Builder withUndoConfirmationPolicy(UndoConfirmationPolicy policy) {
      myUndoConfirmationPolicy = policy;
      return this;
    }

    public Builder withDocument(Document document) {
      myDocument = document;
      return this;
    }

    /**
     * @param shouldRecordCommandForActiveDocument {@code false} if the action is not supposed to be recorded into the currently open document's history.
     *                                             Examples of such actions: Create New File, Change Project Settings etc.
     *                                             Default is {@code true}.
     */
    public Builder withShouldRecordCommandForActiveDocument(boolean shouldRecordCommandForActiveDocument) {
      myShouldRecordCommandForActiveDocument = shouldRecordCommandForActiveDocument;
      return this;
    }

    public CommandInfo build() {
      return new CommandInfo(myProject, myName, myGroupId, myUndoConfirmationPolicy, myDocument, myShouldRecordCommandForActiveDocument);
    }
  }

  @Nonnull
  public static Builder newBuilder() {
    return new Builder();
  }
}
