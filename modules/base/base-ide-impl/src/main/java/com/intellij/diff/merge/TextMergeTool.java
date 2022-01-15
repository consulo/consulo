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
package com.intellij.diff.merge;

import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;

public class TextMergeTool implements MergeTool {
  public static final TextMergeTool INSTANCE = new TextMergeTool();

  public static final Logger LOG = Logger.getInstance(TextMergeTool.class);

  @RequiredUIAccess
  @Nonnull
  @Override
  public MergeViewer createComponent(@Nonnull MergeContext context, @Nonnull MergeRequest request) {
    return new TextMergeViewer(context, ((TextMergeRequest)request));
  }

  @Override
  public boolean canShow(@Nonnull MergeContext context, @Nonnull MergeRequest request) {
    return request instanceof TextMergeRequest;
  }
}
