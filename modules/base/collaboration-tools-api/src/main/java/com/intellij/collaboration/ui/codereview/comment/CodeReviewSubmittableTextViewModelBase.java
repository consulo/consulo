// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.comment;

import com.intellij.collaboration.async.ChildScopeKt;
import com.intellij.collaboration.util.ComputedResult;
import com.intellij.collaboration.util.SingleCoroutineLauncher;
import com.intellij.openapi.project.Project;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.channels.BufferOverflow;
import kotlinx.coroutines.channels.Channel;
import kotlinx.coroutines.channels.ChannelKt;
import kotlinx.coroutines.flow.*;
import jakarta.annotation.Nonnull;

import java.util.concurrent.CancellationException;

public abstract class CodeReviewSubmittableTextViewModelBase implements CodeReviewSubmittableTextViewModel {
  private final @Nonnull Project project;
  protected final @Nonnull CoroutineScope cs;
  private final @Nonnull SingleCoroutineLauncher taskLauncher;

  private final @Nonnull MutableStateFlow<String> text;
  private final @Nonnull MutableStateFlow<ComputedResult<Unit>> _state;
  private final @Nonnull StateFlow<ComputedResult<Unit>> state;
  private final @Nonnull Channel<Unit> _focusRequestsChannel;

  protected CodeReviewSubmittableTextViewModelBase(@Nonnull Project project,
                                                   @Nonnull CoroutineScope parentCs,
                                                   @Nonnull String initialText) {
    this.project = project;
    this.cs = ChildScopeKt.childScope(parentCs, getClass());
    this.taskLauncher = new SingleCoroutineLauncher(cs);

    this.text = StateFlowKt.MutableStateFlow(initialText);
    this._state = StateFlowKt.MutableStateFlow(null);
    this.state = FlowKt.asStateFlow(_state);
    this._focusRequestsChannel = ChannelKt.Channel(1, BufferOverflow.DROP_OLDEST, null);
  }

  @Override
  public @Nonnull Project getProject() {
    return project;
  }

  @Override
  public final @Nonnull MutableStateFlow<String> getText() {
    return text;
  }

  @Override
  public final @Nonnull StateFlow<ComputedResult<Unit>> getState() {
    return state;
  }

  @Override
  public final @Nonnull kotlinx.coroutines.flow.Flow<Unit> getFocusRequests() {
    return kotlinx.coroutines.flow.FlowKt.receiveAsFlow(_focusRequestsChannel);
  }

  @Override
  public final void requestFocus() {
    kotlinx.coroutines.CoroutineScopeKt.launch(cs, null, null, (scope, continuation) -> {
      _focusRequestsChannel.send(Unit.INSTANCE, continuation);
      return null;
    });
  }

  protected void launchTask(@Nonnull kotlin.jvm.functions.Function1<Continuation<? super Unit>, Object> task) {
    taskLauncher.launch((scope, continuation) -> {
      _state.setValue(ComputedResult.loading());
      try {
        task.invoke(continuation);
      }
      finally {
        _state.setValue(null);
      }
      return null;
    });
  }

  protected void submit(@Nonnull kotlin.jvm.functions.Function2<String, Continuation<? super Unit>, Object> submitter) {
    taskLauncher.launch((scope, continuation) -> {
      String newText = text.getValue();
      _state.setValue(ComputedResult.loading());
      try {
        submitter.invoke(newText, continuation);
        _state.setValue(ComputedResult.success(Unit.INSTANCE));
      }
      catch (CancellationException ce) {
        _state.setValue(null);
        throw ce;
      }
      catch (Exception e) {
        _state.setValue(ComputedResult.failure(e));
      }
      return null;
    });
  }
}
