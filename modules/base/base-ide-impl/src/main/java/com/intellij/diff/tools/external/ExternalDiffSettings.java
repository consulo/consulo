/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.diff.tools.external;

import com.intellij.diff.util.DiffUtil;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import consulo.util.lang.StringUtil;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Singleton
@State(name = "ExternalDiffSettings", storages = @Storage(file = DiffUtil.DIFF_CONFIG))
public class ExternalDiffSettings implements PersistentStateComponent<ExternalDiffSettings.State> {
  public static class State {
    public boolean DIFF_ENABLED = false;
    public boolean DIFF_DEFAULT = false;
    @Nullable
    public String DIFF_EXE_PATH = "";
    @Nullable
    public String DIFF_PARAMETERS = "%1 %2 %3";

    public boolean MERGE_ENABLED = false;
    @Nullable
    public String MERGE_EXE_PATH = "";
    @Nullable
    public String MERGE_PARAMETERS = "%1 %2 %3 %4";

    public boolean MERGE_TRUST_EXIT_CODE = false;
  }

  private State myState = new State();

  @Override
  public State getState() {
    return myState;
  }

  @Override
  public void loadState(State state) {
    myState = state;
  }

  public static ExternalDiffSettings getInstance() {
    return ServiceManager.getService(ExternalDiffSettings.class);
  }

  public boolean isDiffEnabled() {
    return myState.DIFF_ENABLED;
  }

  public void setDiffEnabled(boolean value) {
    myState.DIFF_ENABLED = value;
  }

  public boolean isDiffDefault() {
    return myState.DIFF_DEFAULT;
  }

  public void setDiffDefault(boolean value) {
    myState.DIFF_DEFAULT = value;
  }

  @Nonnull
  public String getDiffExePath() {
    return StringUtil.notNullize(myState.DIFF_EXE_PATH);
  }

  public void setDiffExePath(@Nonnull String path) {
    myState.DIFF_EXE_PATH = path;
  }

  @Nonnull
  public String getDiffParameters() {
    return myState.DIFF_PARAMETERS;
  }

  public void setDiffParameters(@Nonnull String path) {
    myState.DIFF_PARAMETERS = path;
  }


  public boolean isMergeEnabled() {
    return myState.MERGE_ENABLED;
  }

  public void setMergeEnabled(boolean value) {
    myState.MERGE_ENABLED = value;
  }

  @Nonnull
  public String getMergeExePath() {
    return StringUtil.notNullize(myState.MERGE_EXE_PATH);
  }

  public void setMergeExePath(@Nonnull String path) {
    myState.MERGE_EXE_PATH = path;
  }

  @Nonnull
  public String getMergeParameters() {
    return StringUtil.notNullize(myState.MERGE_PARAMETERS);
  }

  public void setMergeParameters(@Nonnull String path) {
    myState.MERGE_PARAMETERS = path;
  }

  public boolean isMergeTrustExitCode() {
    return myState.MERGE_TRUST_EXIT_CODE;
  }

  public void setMergeTrustExitCode(boolean value) {
    myState.MERGE_TRUST_EXIT_CODE = value;
  }
}
