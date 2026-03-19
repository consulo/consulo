/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal.patch.tool;

import consulo.diff.content.DocumentContent;
import consulo.diff.request.DiffRequest;
import consulo.versionControlSystem.impl.internal.patch.apply.AppliedTextPatch;
import org.jspecify.annotations.Nullable;

public class ApplyPatchDiffRequest extends DiffRequest implements ApplyPatchRequest {
  
  private final DocumentContent myResultContent;
  
  private final AppliedTextPatch myAppliedPatch;

  
  private final String myLocalContent;

  private final @Nullable String myWindowTitle;
  
  private final String myLocalTitle;
  
  private final String myResultTitle;
  
  private final String myPatchTitle;

  public ApplyPatchDiffRequest(
    DocumentContent resultContent,
    AppliedTextPatch appliedPatch,
    String localContent,
    @Nullable String windowTitle,
    String localTitle,
    String resultTitle,
    String patchTitle
  ) {
    myResultContent = resultContent;
    myAppliedPatch = appliedPatch;
    myLocalContent = localContent;
    myWindowTitle = windowTitle;
    myLocalTitle = localTitle;
    myResultTitle = resultTitle;
    myPatchTitle = patchTitle;
  }

  @Override
  
  public DocumentContent getResultContent() {
    return myResultContent;
  }

  @Override
  
  public String getLocalContent() {
    return myLocalContent;
  }

  @Override
  
  public AppliedTextPatch getPatch() {
    return myAppliedPatch;
  }

  @Nullable
  @Override
  public String getTitle() {
    return myWindowTitle;
  }

  @Override
  
  public String getLocalTitle() {
    return myLocalTitle;
  }

  @Override
  
  public String getResultTitle() {
    return myResultTitle;
  }

  @Override
  
  public String getPatchTitle() {
    return myPatchTitle;
  }
}
