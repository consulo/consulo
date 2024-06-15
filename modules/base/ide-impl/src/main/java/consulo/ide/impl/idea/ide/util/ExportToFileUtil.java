/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.util;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.EditorSettings;
import consulo.document.Document;
import consulo.document.impl.DocumentImpl;
import consulo.ide.impl.idea.openapi.editor.impl.EditorFactoryImpl;
import consulo.language.plain.PlainTextFileType;
import consulo.logging.Logger;
import consulo.platform.base.localize.CommonLocalize;
import consulo.platform.base.localize.IdeLocalize;
import consulo.project.Project;
import consulo.project.macro.ProjectPathMacroManager;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.ex.action.ExporterToTextFile;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.lang.StringUtil;
import consulo.util.lang.SystemProperties;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.TooManyListenersException;

public class ExportToFileUtil {
  private static final Logger LOG = Logger.getInstance(ExportToFileUtil.class);

  public static void exportTextToFile(Project project, String fileName, String textToExport) {
    String prepend = "";
    File file = new File(fileName);
    if (file.exists()) {
      int result = Messages.showYesNoCancelDialog(
        project,
        IdeLocalize.errorTextFileAlreadyExists(fileName).get(),
        IdeLocalize.titleWarning().get(),
        IdeLocalize.actionOverwrite().get(),
        IdeLocalize.actionAppend().get(),
        CommonLocalize.buttonCancel().get(),
        Messages.getWarningIcon()
      );

      if (result != 1 && result != 0) {
        return;
      }
      if (result == 1) {
        char[] buf = new char[(int)file.length()];
        try {
          FileReader reader = new FileReader(fileName);
          try {
            reader.read(buf, 0, (int)file.length());
            prepend = new String(buf) + SystemProperties.getLineSeparator();
          }
          finally {
            reader.close();
          }
        }
        catch (IOException e) {
        }
      }
    }

    try {
      FileWriter writer = new FileWriter(fileName);
      try {
        writer.write(prepend + textToExport);
      }
      finally {
        writer.close();
      }
    }
    catch (IOException e) {
      Messages.showMessageDialog(
        project,
        IdeLocalize.errorWritingToFile(fileName).get(),
        CommonLocalize.titleError().get(),
        Messages.getErrorIcon()
      );
    }
  }

  public static class ExportDialogBase extends DialogWrapper {
    private final Project myProject;
    private final ExporterToTextFile myExporter;
    protected Editor myTextArea;
    protected JTextField myTfFile;
    protected JButton myFileButton;
    private ChangeListener myListener;

    public ExportDialogBase(Project project, ExporterToTextFile exporter) {
      super(project, true);
      myProject = project;
      myExporter = exporter;

      myTfFile = new JTextField();
      myFileButton = new FixedSizeButton(myTfFile);

      setHorizontalStretch(1.5f);
      setTitle(IdeLocalize.titleExportPreview());
      setOKButtonText(IdeLocalize.buttonSave().get());
      init();
      try {
        myListener = e -> initText();
        myExporter.addSettingsChangedListener(myListener);
      }
      catch (TooManyListenersException e) {
        LOG.error(e);
      }
      initText();
    }

    @Override
    public void dispose() {
      myExporter.removeSettingsChangedListener(myListener);
      EditorFactory.getInstance().releaseEditor(myTextArea);
      super.dispose();
    }

    private void initText() {
      myTextArea.getDocument().setText(myExporter.getReportText());
    }

    @Override
    protected JComponent createCenterPanel() {
      final Document document = ((EditorFactoryImpl)EditorFactory.getInstance()).createDocument(true);
      ((DocumentImpl)document).setAcceptSlashR(true);

      myTextArea = EditorFactory.getInstance().createEditor(document, myProject, PlainTextFileType.INSTANCE, true);
      final EditorSettings settings = myTextArea.getSettings();
      settings.setLineNumbersShown(false);
      settings.setLineMarkerAreaShown(false);
      settings.setFoldingOutlineShown(false);
      settings.setRightMarginShown(false);
      settings.setAdditionalLinesCount(0);
      settings.setAdditionalColumnsCount(0);
      settings.setAdditionalPageAtBottom(false);
      ((EditorEx)myTextArea).setBackgroundColor(TargetAWT.from(UIUtil.getInactiveTextFieldBackgroundColor()));
      return myTextArea.getComponent();
    }

    @Override
    protected JComponent createNorthPanel() {
      JPanel filePanel = createFilePanel(myTfFile, myFileButton);
      JComponent settingsPanel = myExporter.getSettingsEditor();
      if (settingsPanel == null) {
        return filePanel;
      }
      JPanel northPanel = new JPanel(new BorderLayout());
      northPanel.add(filePanel, BorderLayout.NORTH);
      northPanel.add(settingsPanel, BorderLayout.CENTER);
      return northPanel;
    }

    protected JPanel createFilePanel(JTextField textField, JButton button) {
      JPanel panel = new JPanel();
      panel.setLayout(new GridBagLayout());
      GridBagConstraints gbConstraints = new GridBagConstraints();
      gbConstraints.fill = GridBagConstraints.HORIZONTAL;
      JLabel promptLabel = new JLabel(IdeLocalize.editboxExportToFile().get());
      gbConstraints.weightx = 0;
      panel.add(promptLabel, gbConstraints);
      gbConstraints.weightx = 1;
      panel.add(textField, gbConstraints);
      gbConstraints.fill = 0;
      gbConstraints.weightx = 0;
      gbConstraints.insets = new Insets(0, 0, 0, 0);
      panel.add(button, gbConstraints);

      String defaultFilePath = myExporter.getDefaultFilePath();
      if (! new File(defaultFilePath).isAbsolute()) {
        defaultFilePath = ProjectPathMacroManager.getInstance(myProject).collapsePath(defaultFilePath).replace('/', File.separatorChar);
      } else {
        defaultFilePath = defaultFilePath.replace('/', File.separatorChar);
      }
      textField.setText(defaultFilePath);

      button.addActionListener(
        e -> browseFile()
      );

      return panel;
    }

    protected void browseFile() {
      JFileChooser chooser = new JFileChooser();
      if (myTfFile != null) {
        chooser.setCurrentDirectory(new File(myTfFile.getText()));
      }
      chooser.showOpenDialog(TargetAWT.to(WindowManager.getInstance().suggestParentWindow(myProject)));
      if (chooser.getSelectedFile() != null) {
        myTfFile.setText(chooser.getSelectedFile().getAbsolutePath());
      }
    }

    public String getText() {
      return myTextArea.getDocument().getText();
    }

    public void setFileName(String s) {
      myTfFile.setText(s);
    }

    public String getFileName() {
      return myTfFile.getText();
    }

    @Override
    @Nonnull
    protected Action[] createActions() {
      return new Action[]{getOKAction(), new CopyToClipboardAction(), getCancelAction()};
    }

    @Override
    protected String getDimensionServiceKey() {
      return "#consulo.ide.impl.idea.ide.util.ExportDialog";
    }

    protected class CopyToClipboardAction extends AbstractAction {
      public CopyToClipboardAction() {
        super(IdeLocalize.buttonCopy().get());
        putValue(AbstractAction.SHORT_DESCRIPTION, IdeLocalize.descriptionCopyTextToClipboard().get());
      }

      @Override
      public void actionPerformed(ActionEvent e) {
        String s = StringUtil.convertLineSeparators(getText());
        CopyPasteManager.getInstance().setContents(new StringSelection(s));
      }
    }
  }
}
