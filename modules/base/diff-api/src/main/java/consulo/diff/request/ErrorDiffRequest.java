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
package consulo.diff.request;

import consulo.diff.chain.DiffRequestProducer;
import org.jspecify.annotations.Nullable;

public class ErrorDiffRequest extends MessageDiffRequest {
  private final @Nullable DiffRequestProducer myProducer;
  private final @Nullable Throwable myException;

  public ErrorDiffRequest(String message) {
    this(null, message, null, null);
  }

  public ErrorDiffRequest(@Nullable String title, String message) {
    this(title, message, null, null);
  }

  public ErrorDiffRequest(@Nullable String title, Throwable e) {
    this(title, e.getMessage(), null, e);
  }

  public ErrorDiffRequest(Throwable e) {
    this(null, e.getMessage(), null, e);
  }

  public ErrorDiffRequest(@Nullable DiffRequestProducer producer, Throwable e) {
    this(producer != null ? producer.getName() : null, e.getMessage(), producer, e);
  }

  public ErrorDiffRequest(@Nullable DiffRequestProducer producer, String message) {
    this(producer != null ? producer.getName() : null, message, producer, null);
  }

  public ErrorDiffRequest(@Nullable String title,
                          String message,
                          @Nullable DiffRequestProducer producer,
                          @Nullable Throwable e) {
    super(title, message);
    myProducer = producer;
    myException = e;
  }

  public @Nullable DiffRequestProducer getProducer() {
    return myProducer;
  }

  public @Nullable Throwable getException() {
    return myException;
  }
}
