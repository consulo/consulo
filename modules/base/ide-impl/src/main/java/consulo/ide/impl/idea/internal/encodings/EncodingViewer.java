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
package consulo.ide.impl.idea.internal.encodings;

import consulo.logging.Logger;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.util.io.CharsetToolkit;
import consulo.virtualFileSystem.VirtualFile;
import consulo.fileChooser.FileChooser;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;

public class EncodingViewer extends DialogWrapper {
  private static final Logger LOG = Logger.getInstance(EncodingViewer.class);

  private JPanel myPanel;
  private JTextArea myText;
  private JComboBox myEncoding;
  private JButton myLoadFile;
  private byte[] myBytes;

  public EncodingViewer() {
    super(false);
    initEncodings();
    myLoadFile.addActionListener(e -> FileChooser.chooseFile(FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor(), myPanel, null, null).doWhenDone(file -> {
      loadFrom(file);
    }));
    init();
  }

  @Override
  protected String getDimensionServiceKey() {
    return "EncodingViewer";
  }

  private void loadFrom(VirtualFile virtualFile) {
    try {
      myBytes = virtualFile.contentsToByteArray();
    }
    catch (IOException e) {
      LOG.error(e);
      return;
    }
    refreshText();
  }

  private void refreshText() {
    String selectedCharset = getSelectedCharset();
    if (myBytes == null || selectedCharset == null) return;
    try {
      myText.setText(new String(myBytes, selectedCharset));
    }
    catch (UnsupportedEncodingException e) {
      LOG.error(e);
    }
  }

  private String getSelectedCharset() {
    return ((Charset)myEncoding.getSelectedItem()).name();
  }

  private void initEncodings() {
    Charset[] availableCharsets = CharsetToolkit.getAvailableCharsets();
    myEncoding.setModel(new DefaultComboBoxModel(availableCharsets));
    int defaultIndex = Arrays.asList(availableCharsets).indexOf(CharsetToolkit.getDefaultSystemCharset());
    myEncoding.setSelectedIndex(defaultIndex);
    myEncoding.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
          refreshText();
        }
      }
    });
  }

  protected JComponent createCenterPanel() {
    return myPanel;
  }
}
