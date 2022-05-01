/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ui;

import consulo.component.ComponentManager;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserFactory;
import consulo.ui.ex.awt.ComponentWithBrowseButton;
import consulo.ui.ex.awt.TextComponentAccessor;
import consulo.ui.ex.awt.TextFieldWithHistory;

import javax.annotation.Nullable;

/**
 * User: anna
 */
public class TextFieldWithHistoryWithBrowseButton extends ComponentWithBrowseButton<TextFieldWithHistory> {
  public TextFieldWithHistoryWithBrowseButton() {
    super(new TextFieldWithHistory(), null);
  }

  @Override
  public void addBrowseFolderListener(@Nullable String title,
                                      @Nullable String description,
                                      @Nullable ComponentManager project,
                                      FileChooserDescriptor fileChooserDescriptor,
                                      TextComponentAccessor<TextFieldWithHistory> accessor) {
    super.addBrowseFolderListener(title, description, project, fileChooserDescriptor, accessor);
    FileChooserFactory.getInstance().installFileCompletion(getChildComponent().getTextEditor(), fileChooserDescriptor, false, project);
  }

  @Override
  public void addBrowseFolderListener(@Nullable String title,
                                      @Nullable String description,
                                      @Nullable ComponentManager project,
                                      FileChooserDescriptor fileChooserDescriptor,
                                      TextComponentAccessor<TextFieldWithHistory> accessor,
                                      boolean autoRemoveOnHide) {
    super.addBrowseFolderListener(title, description, project, fileChooserDescriptor, accessor, autoRemoveOnHide);
    FileChooserFactory.getInstance().installFileCompletion(getChildComponent().getTextEditor(), fileChooserDescriptor, false, project);
  }
}
