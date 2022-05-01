/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.diff;

import consulo.diff.chain.DiffRequestProducerException;
import consulo.diff.content.DiffContent;
import consulo.application.AllIcons;
import consulo.component.ProcessCanceledException;
import consulo.application.progress.ProgressIndicator;
import consulo.project.Project;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * @author Konstantin Bulenkov
 */
public class DiffErrorElement extends DiffElement {
  private final String myMessage;

  public DiffErrorElement() {
    this("Can't load children", "");
  }

  public DiffErrorElement(@Nonnull String message, @Nonnull String description) {
    myMessage = message;
  }

  @Override
  public String getPath() {
    return "";
  }

  @Nonnull
  @Override
  public String getName() {
    return myMessage;
  }

  @Override
  public long getSize() {
    return -1;
  }

  @Override
  public long getTimeStamp() {
    return -1;
  }

  @Override
  public boolean isContainer() {
    return false;
  }

  @Override
  public DiffElement[] getChildren() throws IOException {
    return EMPTY_ARRAY;
  }

  @javax.annotation.Nullable
  @Override
  public byte[] getContent() throws IOException {
    return null;
  }

  @Override
  public Object getValue() {
    return null;
  }

  @Override
  public Image getIcon() {
    return AllIcons.Nodes.ErrorIntroduction;
  }

  @Nonnull
  public DiffContent createDiffContent(@javax.annotation.Nullable Project project, @Nonnull ProgressIndicator indicator)
          throws DiffRequestProducerException, ProcessCanceledException {
    throw new DiffRequestProducerException(myMessage);
  }
}
