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

package consulo.desktop.awt.uiOld.debugger.extensions;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.AllIcons;
import consulo.application.ApplicationManager;
import consulo.fileChooser.IdeaFileChooser;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.ide.impl.idea.openapi.fileChooser.FileElement;
import consulo.ide.impl.idea.openapi.fileChooser.ex.FileChooserKeys;
import consulo.project.Project;
import consulo.ui.ex.awt.Messages;
import consulo.ide.ServiceManager;
import consulo.ui.ex.awt.Splitter;
import consulo.ide.impl.idea.openapi.ui.playback.PlaybackContext;
import consulo.ide.impl.idea.openapi.ui.playback.PlaybackRunner;
import consulo.util.lang.StringUtil;
import consulo.ide.impl.idea.openapi.vfs.*;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.*;
import consulo.util.io.CharsetToolkit;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.encoding.EncodingRegistry;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.WindowManager;
import consulo.project.ui.internal.WindowManagerEx;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ide.impl.idea.ui.debugger.UiDebuggerExtension;
import consulo.ide.impl.idea.ui.debugger.extensions.UiScriptFileType;
import consulo.ide.impl.idea.util.WaitFor;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.desktop.awt.wm.impl.DesktopIdeFrameImpl;
import consulo.logging.Logger;
import consulo.ui.Window;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.VirtualFileAdapter;
import consulo.virtualFileSystem.event.VirtualFileEvent;
import consulo.project.ui.wm.IdeFrameUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

@ExtensionImpl
public class PlaybackDebugger implements UiDebuggerExtension, PlaybackRunner.StatusCallback {

  private static final Logger LOG = Logger.getInstance(PlaybackDebugger.class);

  private static final Color ERROR_COLOR = JBColor.RED;
  private static final Color MESSAGE_COLOR = Color.BLACK;
  private static final Color CODE_COLOR = JBColor.BLUE;
  private static final Color TEST_COLOR = JBColor.GREEN.darker();

  private JPanel myComponent;

  private PlaybackRunner myRunner;

  private JEditorPane myLog;

  private final JTextField myScriptsPath = new JTextField();

  private static final String EXT = "ijs";

  private static final String DOT_EXT = "." + EXT;

  private final JTextField myCurrentScript = new JTextField();

  private VirtualFileAdapter myVfsListener;

  private boolean myChanged;

  private PlaybackDebuggerState myState;
  private static final FileChooserDescriptor FILE_DESCRIPTOR = new ScriptFileChooserDescriptor();
  private JTextArea myCodeEditor;

  private void initUi() {
    myComponent = new JPanel(new BorderLayout());
    myLog = new JEditorPane();
    myLog.setEditorKit(new StyledEditorKit());
    myLog.setEditable(false);


    myState = ServiceManager.getService(PlaybackDebuggerState.class);

    final DefaultActionGroup controlGroup = new DefaultActionGroup();
    controlGroup.add(new RunOnFameActivationAction());
    controlGroup.add(new ActivateFrameAndRun());
    controlGroup.add(new StopAction());

    JPanel north = new JPanel(new BorderLayout());
    north.add(ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, controlGroup, true).getComponent(), BorderLayout.WEST);

    final JPanel right = new JPanel(new BorderLayout());
    right.add(myCurrentScript, BorderLayout.CENTER);
    myCurrentScript.setText(myState.currentScript);
    myCurrentScript.setEditable(false);

    final DefaultActionGroup fsGroup = new DefaultActionGroup();
    SaveAction saveAction = new SaveAction();
    saveAction.registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke("control S")), myComponent);
    fsGroup.add(saveAction);
    SetScriptFileAction setScriptFileAction = new SetScriptFileAction();
    setScriptFileAction.registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke("control O")), myComponent);
    fsGroup.add(setScriptFileAction);
    AnAction newScriptAction = new NewScriptAction();
    newScriptAction.registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke("control N")), myComponent);
    fsGroup.add(newScriptAction);

    final ActionToolbar tb = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, fsGroup, true);
    tb.setLayoutPolicy(ActionToolbar.NOWRAP_LAYOUT_POLICY);
    right.add(tb.getComponent(), BorderLayout.EAST);
    north.add(right, BorderLayout.CENTER);

    myComponent.add(north, BorderLayout.NORTH);

    myCodeEditor = new JTextArea();
    myCodeEditor.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void insertUpdate(DocumentEvent e) {
        myChanged = true;
      }

      @Override
      public void removeUpdate(DocumentEvent e) {
        myChanged = true;
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        myChanged = true;
      }
    });
    if (pathToFile() != null) {
      loadFrom(pathToFile());
    }

    final Splitter script2Log = new Splitter(true);
    script2Log.setFirstComponent(ScrollPaneFactory.createScrollPane(myCodeEditor));

    script2Log.setSecondComponent(ScrollPaneFactory.createScrollPane(myLog));

    myComponent.add(script2Log, BorderLayout.CENTER);

    myVfsListener = new VirtualFileAdapter() {
      @Override
      public void contentsChanged(@Nonnull VirtualFileEvent event) {
        final VirtualFile file = pathToFile();
        if (file != null && file.equals(event.getFile())) {
          loadFrom(event.getFile());
        }
      }
    };
    LocalFileSystem.getInstance().addVirtualFileListener(myVfsListener);
  }

  private class SaveAction extends AnAction {
    private SaveAction() {
      super("Save", "", AllIcons.Actions.Menu_saveall);
    }

    @Override
    public void update(AnActionEvent e) {
      e.getPresentation().setEnabled(myChanged);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
      if (pathToFile() == null) {
        VirtualFile selectedFile = IdeaFileChooser.chooseFile(FILE_DESCRIPTOR, myComponent, e == null ? null : e.getData(Project.KEY), null);
        if (selectedFile != null) {
          myState.currentScript = selectedFile.getPresentableUrl();
          myCurrentScript.setText(myState.currentScript);
        }
        else {
          Messages.showErrorDialog("File to save is not selected.", "Cannot save script");
          return;
        }
      }
      ApplicationManager.getApplication().runWriteAction(new Runnable() {
        @Override
        public void run() {
          save();
        }
      });
    }
  }

  private static class ScriptFileChooserDescriptor extends FileChooserDescriptor {
    public ScriptFileChooserDescriptor() {
      super(true, false, false, false, false, false);
      putUserData(FileChooserKeys.NEW_FILE_TYPE, UiScriptFileType.getInstance());
      putUserData(FileChooserKeys.NEW_FILE_TEMPLATE_TEXT, "");
    }

    @Override
    public boolean isFileVisible(VirtualFile file, boolean showHiddenFiles) {
      if (!showHiddenFiles && FileElement.isFileHidden(file)) return false;
      return file.getExtension() != null && file.getExtension().equalsIgnoreCase(UiScriptFileType.myExtension) ||
             super.isFileVisible(file, showHiddenFiles) && file.isDirectory();
    }
  }

  private class SetScriptFileAction extends AnAction {

    private SetScriptFileAction() {
      super("Set Script File", "", AllIcons.Actions.Menu_open);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
      VirtualFile selectedFile = IdeaFileChooser.chooseFile(FILE_DESCRIPTOR, myComponent, e == null ? null : e.getData(Project.KEY), pathToFile());
      if (selectedFile != null) {
        myState.currentScript = selectedFile.getPresentableUrl();
        loadFrom(selectedFile);
        myCurrentScript.setText(myState.currentScript);
      }
    }
  }

  private class NewScriptAction extends AnAction {
    private NewScriptAction() {
      super("New Script", "", AllIcons.Actions.New);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
      myState.currentScript = "";
      myCurrentScript.setText(myState.currentScript);
      fillDocument("");
    }
  }

  private void fillDocument(final String text) {
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        myCodeEditor.setText(text == null ? "" : text);
      }
    });
  }

  @Nullable
  private VirtualFile pathToFile() {
    if (myState.currentScript.length() == 0) {
      return null;
    }
    return LocalFileSystem.getInstance().findFileByPath(myState.currentScript);
  }

  private void save() {
    try {
      VirtualFile file = pathToFile();
      final String toWrite = myCodeEditor.getText();
      String text = toWrite != null ? toWrite : "";
      VfsUtil.saveText(file, text);
      myChanged = false;
    }
    catch (IOException e) {
      Messages.showErrorDialog(e.getMessage(), "Cannot save script");
    }
  }

  private void loadFrom(@Nonnull VirtualFile file) {
    try {
      final String text = CharsetToolkit.bytesToString(file.contentsToByteArray(), EncodingRegistry.getInstance().getDefaultCharset());
      fillDocument(text);
      myChanged = false;
    }
    catch (IOException e) {
      Messages.showErrorDialog(e.getMessage(), "Cannot load file");
    }
  }

  private File getScriptsFile() {
    final String text = myScriptsPath.getText();
    if (text == null) return null;

    final File file = new File(text);
    return file.exists() ? file : null;
  }

  private class StopAction extends AnAction {
    private StopAction() {
      super("Stop", null, AllIcons.Actions.Suspend);
    }

    @Override
    public void update(AnActionEvent e) {
      e.getPresentation().setEnabled(myRunner != null);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
      if (myRunner != null) {
        myRunner.stop();
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            myRunner = null;
          }
        });
      }
    }
  }

  private class ActivateFrameAndRun extends AnAction {
    private ActivateFrameAndRun() {
      super("Activate Frame And Run", "", AllIcons.Nodes.Deploy);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
      activateAndRun();
    }

    @Override
    public void update(AnActionEvent e) {
      e.getPresentation().setEnabled(myRunner == null);
    }
  }

  private class RunOnFameActivationAction extends AnAction {

    private RunOnFameActivationAction() {
      super("Run On Frame Activation", "", AllIcons.RunConfigurations.TestState.Run);
    }

    @Override
    public void update(AnActionEvent e) {
      e.getPresentation().setEnabled(myRunner == null);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
      runOnFrame();
    }
  }

  private void activateAndRun() {
    assert myRunner == null;

    myLog.setText(null);

    final DesktopIdeFrameImpl frame = getFrame();

    final Component c = ((WindowManagerEx)WindowManager.getInstance()).getFocusedComponent(TargetAWT.to(frame.getWindow()));

    if (c != null) {
      IdeFocusManager.getGlobalInstance().doForceFocusWhenFocusSettlesDown(c);
    }
    else {
      IdeFocusManager.getGlobalInstance().doForceFocusWhenFocusSettlesDown(TargetAWT.to(frame.getWindow()));
    }

    //noinspection SSBasedInspection
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        startWhenFrameActive();
      }
    });

  }

  private DesktopIdeFrameImpl getFrame() {
    final Frame[] all = Frame.getFrames();
    for (Frame each : all) {
      Window uiWindow = TargetAWT.from(each);

      IdeFrame ideFrame = uiWindow.getUserData(IdeFrame.KEY);
      if(IdeFrameUtil.isRootFrame(ideFrame)) {
        return (DesktopIdeFrameImpl)ideFrame;
      }
    }

    throw new IllegalStateException("Cannot find IdeFrame to run on");
  }

  private void runOnFrame() {
    assert myRunner == null;

    startWhenFrameActive();
  }

  private void startWhenFrameActive() {
    myLog.setText(null);

    addInfo("Waiting for IDE frame activation", -1, MESSAGE_COLOR, 0);
    myRunner = new PlaybackRunner(myCodeEditor.getText(), this, false, true, false);
    VirtualFile file = pathToFile();
    if (file != null) {
      VirtualFile scriptDir = file.getParent();
      if (scriptDir != null) {
        myRunner.setScriptDir(new File(scriptDir.getPresentableUrl()));
      }
    }

    new Thread() {
      @Override
      public void run() {
        new WaitFor() {
          @Override
          protected boolean condition() {
            java.awt.Window window = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
            return window != null && TargetAWT.from(window).getUserData(IdeFrame.KEY) != null || myRunner == null;
          }
        };

        if (myRunner == null) {
          message(null, "Script stopped", -1, Type.message, true);
          return;
        }

        message(null, "Starting script...", -1, Type.message, true);

        try {
          sleep(1000);
        }
        catch (InterruptedException e) {
        }


        if (myRunner == null) {
          message(null, "Script stopped", -1, Type.message, true);
          return;
        }

        final PlaybackRunner runner = myRunner;

        myRunner.run().doWhenProcessed(new Runnable() {
          @Override
          public void run() {
            if (runner == myRunner) {
              SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                  myRunner = null;
                }
              });
            }
          }
        });
      }
    }.start();
  }

  @Override
  public void message(@Nullable final PlaybackContext context, final String text, final Type type) {
    message(context, text, context != null ? context.getCurrentLine() : -1, type, false);
  }

  private void message(@Nullable final PlaybackContext context, final String text, final int currentLine, final Type type, final boolean forced) {
    final int depth = context != null ? context.getCurrentStageDepth() : 0;

    UIUtil.invokeLaterIfNeeded(new Runnable() {
      @Override
      public void run() {
        if (!forced && (context != null && context.isDisposed())) return;

        switch (type) {
          case message:
            addInfo(text, currentLine, MESSAGE_COLOR, depth);
            break;
          case error:
            addInfo(text, currentLine, ERROR_COLOR, depth);
            break;
          case code:
            addInfo(text, currentLine, CODE_COLOR, depth);
            break;
          case test:
            addInfo(text, currentLine, TEST_COLOR, depth);
            break;
        }
      }
    });
  }

  @Override
  public JComponent getComponent() {
    if (myComponent == null) {
      initUi();
    }

    return myComponent;
  }

  @Override
  public String getName() {
    return "Playback";
  }

  public void dispose() {
    disposeUiResources();
  }

  @Override
  public void disposeUiResources() {
    myComponent = null;
    LocalFileSystem.getInstance().removeVirtualFileListener(myVfsListener);
    myCurrentScript.setText("");
    myLog.setText(null);
  }

  private void addInfo(String text, int line, Color fg, int depth) {
    if (text == null || text.length() == 0) return;

    String inset = StringUtil.repeat("   ", depth);

    Document doc = myLog.getDocument();
    SimpleAttributeSet attr = new SimpleAttributeSet();
    StyleConstants.setFontFamily(attr, UIManager.getFont("Label.font").getFontName());
    StyleConstants.setFontSize(attr, UIManager.getFont("Label.font").getSize());
    StyleConstants.setForeground(attr, fg);
    try {
      doc.insertString(doc.getLength(), inset + text + "\n", attr);
    }
    catch (BadLocationException e) {
      LOG.error(e);
    }
    scrollToLast();
  }

  private void scrollToLast() {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        if (myLog.getDocument().getLength() == 0) return;

        Rectangle bounds = myLog.getBounds();
        myLog.scrollRectToVisible(new Rectangle(0, (int)bounds.getMaxY() - 1, (int)bounds.getWidth(), 1));
      }
    });
  }

}
