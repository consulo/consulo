// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.comment;

import com.intellij.collaboration.util.ComputedResult;
import com.intellij.openapi.project.Project;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.StateFlow;
import jakarta.annotation.Nonnull;

public interface CodeReviewSubmittableTextViewModel {
  @Nonnull Project getProject();

  /**
   * Input text state
   */
  @Nonnull MutableStateFlow<String> getText();

  /**
   * State of submission progress.
   * null means that submission wasn't started yet.
   */
  @Nonnull StateFlow<ComputedResult<kotlin.Unit>> getState();

  @Nonnull Flow<kotlin.Unit> getFocusRequests();

  void requestFocus();
}
