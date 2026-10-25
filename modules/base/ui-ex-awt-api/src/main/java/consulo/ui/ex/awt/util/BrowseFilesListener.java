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
package consulo.ui.ex.awt.util;

import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.ui.UIAccess;
import consulo.fileChooser.FileChooserFactory;
import consulo.fileChooser.util.FileChooserUtil;

import org.jspecify.annotations.Nullable;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Path;

/**
 * @author Eugene Zhuravlev
 * @since 2003-12-28
 */
public class BrowseFilesListener implements ActionListener {
  public static final FileChooserDescriptor SINGLE_DIRECTORY_DESCRIPTOR = FileChooserDescriptorFactory.createSingleFolderDescriptor();
  public static final FileChooserDescriptor SINGLE_FILE_DESCRIPTOR = FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor();

  private final JTextField myTextField;
  private final String myTitle;
  private final String myDescription;
  protected final FileChooserDescriptor myChooserDescriptor;

  public BrowseFilesListener(JTextField textField, String title, String description, FileChooserDescriptor chooserDescriptor) {
    myTextField = textField;
    myTitle = title;
    myDescription = description;
    myChooserDescriptor = chooserDescriptor;
  }

  protected @Nullable Path getFileToSelect() {
    return FileChooserUtil.findExistingPathOrParent(myTextField.getText());
  }

  protected void doSetText(String path) {
    myTextField.setText(path);
  }

  public void actionPerformed( ActionEvent e ) {
    Path fileToSelect = getFileToSelect();
    myChooserDescriptor.setTitle(myTitle); // important to set title and description here because a shared descriptor instance can be used
    myChooserDescriptor.setDescription(myDescription);
    UIAccess uiAccess = UIAccess.current();
    FileChooserFactory.getInstance()
      .newBuilder(myChooserDescriptor)
      .toSelect(fileToSelect)
      .showMultiple()
      .whenCompleteAsync((paths, error) -> {
        if (paths != null && !paths.isEmpty()) {
          doSetText(paths.get(0).toString());
        }
      }, uiAccess);
  }
}
