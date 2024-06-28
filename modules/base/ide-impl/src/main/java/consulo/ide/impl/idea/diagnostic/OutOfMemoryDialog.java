/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.ide.impl.idea.diagnostic;

import consulo.application.Application;
import consulo.container.ExitCodes;
import consulo.ide.impl.idea.diagnostic.VMOptions.MemoryKind;
import consulo.ide.impl.idea.util.MemoryDumpHelper;
import consulo.platform.Platform;
import consulo.platform.base.localize.DiagnosticLocalize;
import consulo.platform.base.localize.IdeLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.Messages;
import consulo.util.lang.TimeoutUtil;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Locale;

public class OutOfMemoryDialog extends DialogWrapper {
  private final MemoryKind myMemoryKind;

  private JPanel myContentPane;
  private JBLabel myIconLabel;
  private JBLabel myMessageLabel;
  private JBLabel myHeapSizeLabel;
  private JTextField myHeapSizeField;
  private JBLabel myHeapUnitsLabel;
  private JBLabel myHeapCurrentValueLabel;
  private JBLabel myMetaspaceSizeLabel;
  private JTextField myMetaspaceSizeField;
  private JBLabel myMetaspaceUnitsLabel;
  private JBLabel myMetaspaceCurrentValueLabel;
  private JBLabel myCodeCacheSizeLabel;
  private JTextField myCodeCacheSizeField;
  private JBLabel myCodeCacheUnitsLabel;
  private JBLabel myCodeCacheCurrentValueLabel;
  private JBLabel mySettingsFileHintLabel;
  private JBLabel myDumpMessageLabel;
  private final Action myContinueAction;
  private final Action myShutdownAction;
  private final Action myHeapDumpAction;

  public OutOfMemoryDialog(@Nonnull MemoryKind memoryKind) {
    super(false);
    myMemoryKind = memoryKind;
    setTitle(DiagnosticLocalize.diagnosticOutOfMemoryTitle());

    myIconLabel.setIcon(Messages.getErrorIcon());
    myMessageLabel.setText(DiagnosticLocalize.diagnosticOutOfMemoryError(memoryKind.optionName).get());
    myMessageLabel.setBorder(JBUI.Borders.emptyBottom(10));

    File file = VMOptions.getWriteFile();
    if (file != null) {
      mySettingsFileHintLabel.setText(DiagnosticLocalize.diagnosticOutOfMemoryWillbesavedto(file.getPath()).get());
      mySettingsFileHintLabel.setBorder(JBUI.Borders.emptyTop(10));
    }
    else {
      mySettingsFileHintLabel.setVisible(false);
      myHeapSizeField.setEnabled(false);
      myMetaspaceSizeField.setEnabled(false);
      myCodeCacheSizeField.setEnabled(false);
    }

    myContinueAction = new DialogWrapperAction(DiagnosticLocalize.diagnosticOutOfMemoryContinue()) {
      @Override
      protected void doAction(ActionEvent e) {
        save();
        close(0);
      }
    };

    myShutdownAction = new DialogWrapperAction(IdeLocalize.ideShutdownAction()) {
      @Override
      protected void doAction(ActionEvent e) {
        save();
        System.exit(ExitCodes.OUT_OF_MEMORY);
      }
    };
    myShutdownAction.putValue(DialogWrapper.DEFAULT_ACTION, true);

    boolean heapDump = memoryKind == MemoryKind.HEAP && MemoryDumpHelper.memoryDumpAvailable();
    myHeapDumpAction = !heapDump ? null : new DialogWrapperAction(DiagnosticLocalize.diagnosticOutOfMemoryDump()) {
      @Override
      protected void doAction(ActionEvent e) {
        snapshot();
      }
    };

    configControls(
      MemoryKind.HEAP,
      myHeapSizeLabel,
      myHeapSizeField,
      myHeapUnitsLabel,
      myHeapCurrentValueLabel
    );
    configControls(
      MemoryKind.METASPACE,
      myMetaspaceSizeLabel,
      myMetaspaceSizeField,
      myMetaspaceUnitsLabel,
      myMetaspaceCurrentValueLabel
    );
    configControls(
      MemoryKind.CODE_CACHE,
      myCodeCacheSizeLabel,
      myCodeCacheSizeField,
      myCodeCacheUnitsLabel,
      myCodeCacheCurrentValueLabel
    );

    init();
  }

  private void configControls(
    MemoryKind option,
    JLabel sizeLabel,
    JTextField sizeField,
    JLabel unitsLabel,
    JLabel currentLabel
  ) {
    sizeLabel.setText('-' + option.optionName);

    int effective = VMOptions.readOption(option, true);
    int stored = VMOptions.readOption(option, false);
    if (stored == -1) stored = effective;
    sizeField.setText(format(stored));
    currentLabel.setText(DiagnosticLocalize.diagnosticOutOfMemoryCurrentvalue(format(effective)).get());

    if (option == myMemoryKind) {
      sizeLabel.setForeground(JBColor.RED);
      sizeField.setForeground(JBColor.RED);
      unitsLabel.setForeground(JBColor.RED);
      currentLabel.setForeground(JBColor.RED);
    }
  }

  private static String format(int value) {
    return value == -1 ? DiagnosticLocalize.diagnosticOutOfMemoryCurrentvalueUnknown().get() : String.valueOf(value);
  }

  private void save() {
    try {
      int heapSize = Integer.parseInt(myHeapSizeField.getText());
      VMOptions.writeOption(MemoryKind.HEAP, heapSize);
    }
    catch (NumberFormatException ignored) { }

    try {
      int codeCacheSize = Integer.parseInt(myMetaspaceSizeField.getText());
      VMOptions.writeOption(MemoryKind.METASPACE, codeCacheSize);
    }
    catch (NumberFormatException ignored) { }

    try {
      int codeCacheSize = Integer.parseInt(myCodeCacheSizeField.getText());
      VMOptions.writeOption(MemoryKind.CODE_CACHE, codeCacheSize);
    }
    catch (NumberFormatException ignored) { }
  }

  @SuppressWarnings("SSBasedInspection")
  private void snapshot() {
    enableControls(false);
    myDumpMessageLabel.setVisible(true);
    myDumpMessageLabel.setText("Dumping memory...");

    Runnable task = () -> {
      TimeoutUtil.sleep(250);  // to give UI chance to update
      String message = "";
      try {
        String name = Application.get().getName().get()
          .replace(' ', '-')
          .toLowerCase(Locale.US);
        String path = Platform.current().user().homePath() + File.separator +
          "heapDump-" + name + '-' + System.currentTimeMillis() + ".hprof.zip";
        MemoryDumpHelper.captureMemoryDumpZipped(path);
        message = "Dumped to " + path;
      }
      catch (Throwable t) {
        message = "Error: " + t.getMessage();
      }
      finally {
        final String _message = message;
        SwingUtilities.invokeLater(() -> {
          myDumpMessageLabel.setText(_message);
          enableControls(true);
        });
      }
    };
    new Thread(task, "OOME Heap Dump").start();
  }

  @SuppressWarnings("Duplicates")
  private void enableControls(boolean enabled) {
    myHeapSizeField.setEnabled(enabled);
    myMetaspaceSizeField.setEnabled(enabled);
    myCodeCacheSizeField.setEnabled(enabled);
    myShutdownAction.setEnabled(enabled);
    myContinueAction.setEnabled(enabled);
    myHeapDumpAction.setEnabled(enabled);
  }

  @Override
  protected JComponent createCenterPanel() {
    return myContentPane;
  }

  @Nonnull
  @Override
  protected Action[] createActions() {
    return myHeapDumpAction != null
      ? new Action[]{myShutdownAction, myContinueAction, myHeapDumpAction}
      : new Action[]{myShutdownAction, myContinueAction};
  }

  @RequiredUIAccess
  @Override
  public JComponent getPreferredFocusedComponent() {
    return myMemoryKind == MemoryKind.METASPACE
      ? myMetaspaceSizeField
      : myMemoryKind == MemoryKind.CODE_CACHE
      ? myCodeCacheSizeField
      : myHeapSizeField;
  }
}