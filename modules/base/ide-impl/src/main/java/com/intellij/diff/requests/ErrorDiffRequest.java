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
package com.intellij.diff.requests;

import com.intellij.diff.chains.DiffRequestProducer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ErrorDiffRequest extends MessageDiffRequest {
  @javax.annotation.Nullable
  private final DiffRequestProducer myProducer;
  @javax.annotation.Nullable
  private final Throwable myException;

  public ErrorDiffRequest(@Nonnull String message) {
    this(null, message, null, null);
  }

  public ErrorDiffRequest(@Nullable String title, @Nonnull String message) {
    this(title, message, null, null);
  }

  public ErrorDiffRequest(@javax.annotation.Nullable String title, @Nonnull Throwable e) {
    this(title, e.getMessage(), null, e);
  }

  public ErrorDiffRequest(@Nonnull Throwable e) {
    this(null, e.getMessage(), null, e);
  }

  public ErrorDiffRequest(@javax.annotation.Nullable DiffRequestProducer producer, @Nonnull Throwable e) {
    this(producer != null ? producer.getName() : null, e.getMessage(), producer, e);
  }

  public ErrorDiffRequest(@javax.annotation.Nullable DiffRequestProducer producer, @Nonnull String message) {
    this(producer != null ? producer.getName() : null, message, producer, null);
  }

  public ErrorDiffRequest(@javax.annotation.Nullable String title,
                          @Nonnull String message,
                          @Nullable DiffRequestProducer producer,
                          @javax.annotation.Nullable Throwable e) {
    super(title, message);
    myProducer = producer;
    myException = e;
  }

  @javax.annotation.Nullable
  public DiffRequestProducer getProducer() {
    return myProducer;
  }

  @javax.annotation.Nullable
  public Throwable getException() {
    return myException;
  }
}
