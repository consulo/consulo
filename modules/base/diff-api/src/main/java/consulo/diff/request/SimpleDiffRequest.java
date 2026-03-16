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

import consulo.diff.content.DiffContent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.ContainerUtil;

import org.jspecify.annotations.Nullable;
import java.util.List;

public class SimpleDiffRequest extends ContentDiffRequest {
  @Nullable
  private final String myTitle;
  
  private final List<DiffContent> myContents;
  
  private final List<String> myContentTitles;

  public SimpleDiffRequest(@Nullable String title,
                           DiffContent content1,
                           DiffContent content2,
                           @Nullable String title1,
                           @Nullable String title2) {
    this(title, ContainerUtil.list(content1, content2), ContainerUtil.list(title1, title2));
  }

  public SimpleDiffRequest(@Nullable String title,
                           DiffContent content1,
                           DiffContent content2,
                           DiffContent content3,
                           @Nullable String title1,
                           @Nullable String title2,
                           @Nullable String title3) {
    this(title, ContainerUtil.list(content1, content2, content3), ContainerUtil.list(title1, title2, title3));
  }

  public SimpleDiffRequest(@Nullable String title,
                           List<DiffContent> contents,
                           List<String> titles) {
    assert contents.size() == titles.size();

    myTitle = title;
    myContents = contents;
    myContentTitles = titles;
  }

  
  @Override
  public List<DiffContent> getContents() {
    return myContents;
  }

  
  @Override
  public List<String> getContentTitles() {
    return myContentTitles;
  }

  @Nullable
  @Override
  public String getTitle() {
    return myTitle;
  }

  @RequiredUIAccess
  @Override
  public void onAssigned(boolean isAssigned) {
    for (DiffContent content : myContents) {
      content.onAssigned(isAssigned);
    }
  }
}
