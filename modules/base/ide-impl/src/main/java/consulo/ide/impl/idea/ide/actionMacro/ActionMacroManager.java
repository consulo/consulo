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
package consulo.ide.impl.idea.ide.actionMacro;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.AllIcons;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.util.registry.Registry;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.dataContext.DataContext;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.IdeBundle;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ide.impl.idea.openapi.ui.playback.PlaybackContext;
import consulo.ide.impl.idea.openapi.ui.playback.PlaybackRunner;
import consulo.ide.impl.idea.util.ui.BaseButtonBehavior;
import consulo.ide.impl.ui.IdeEventQueueProxy;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.wm.CustomStatusBarWidget;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.PositionTracker;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.*;
import consulo.ui.ex.action.event.AnActionListener;
import consulo.ui.ex.awt.AnimatedIconComponent;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.NonOpaquePanel;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.internal.ActionManagerEx;
import consulo.ui.ex.popup.Balloon;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.event.JBPopupAdapter;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import consulo.ui.image.Image;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.JDOMExternalizable;
import consulo.util.xml.serializer.WriteExternalException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author max
 */
@State(name = "ActionMacroManager", storages = @Storage("macros.xml"))
@Singleton
@ServiceAPI(value = ComponentScope.APPLICATION, lazy = false)
@ServiceImpl
public class ActionMacroManager implements JDOMExternalizable, Disposable {
  public static ActionMacroManager getInstance() {
    return ApplicationManager.getApplication().getComponent(ActionMacroManager.class);
  }

  private static final Logger LOG = Logger.getInstance(ActionMacroManager.class);

  private static final String TYPING_SAMPLE = "WWWWWWWWWWWWWWWWWWWW";
  private static final String RECORDED = "Recorded: ";

  private boolean myIsRecording;
  private final ActionManager myActionManager;
  private ActionMacro myLastMacro;
  private ActionMacro myRecordingMacro;
  private ArrayList<ActionMacro> myMacros = new ArrayList<ActionMacro>();
  private String myLastMacroName = null;
  private boolean myIsPlaying = false;
  @NonNls
  private static final String ELEMENT_MACRO = "macro";
  private final Predicate<AWTEvent> myKeyProcessor;

  private Set<InputEvent> myLastActionInputEvent = new HashSet<InputEvent>();
  private ActionMacroManager.Widget myWidget;

  private String myLastTyping = "";

  @Inject
  public ActionMacroManager(Application application, ActionManager actionManager) {
    myActionManager = actionManager;
    application.getMessageBus().connect(this).subscribe(AnActionListener.class, new AnActionListener() {
      @Override
      public void beforeActionPerformed(AnAction action, DataContext dataContext, final AnActionEvent event) {
        String id = myActionManager.getId(action);
        if (id == null) return;
        //noinspection HardCodedStringLiteral
        if ("StartStopMacroRecording".equals(id)) {
          myLastActionInputEvent.add(event.getInputEvent());
        }
        else if (myIsRecording) {
          myRecordingMacro.appendAction(id);
          String shortcut = null;
          if (event.getInputEvent() instanceof KeyEvent) {
            shortcut = KeymapUtil.getKeystrokeText(KeyStroke.getKeyStrokeForEvent((KeyEvent)event.getInputEvent()));
          }
          notifyUser(id + (shortcut != null ? " (" + shortcut + ")" : ""), false);
          myLastActionInputEvent.add(event.getInputEvent());
        }
      }
    });

    myKeyProcessor = new MyKeyPostpocessor();

    IdeEventQueueProxy.getInstance().addPostprocessor(myKeyProcessor, null);
  }

  @Override
  public void readExternal(Element element) throws InvalidDataException {
    myMacros = new ArrayList<ActionMacro>();
    final List macros = element.getChildren(ELEMENT_MACRO);
    for (final Object o : macros) {
      Element macroElement = (Element)o;
      ActionMacro macro = new ActionMacro();
      macro.readExternal(macroElement);
      myMacros.add(macro);
    }

    registerActions();
  }

  @Override
  public void writeExternal(Element element) throws WriteExternalException {
    for (ActionMacro macro : myMacros) {
      Element macroElement = new Element(ELEMENT_MACRO);
      macro.writeExternal(macroElement);
      element.addContent(macroElement);
    }
  }

  public void startRecording(String macroName) {
    LOG.assertTrue(!myIsRecording);
    myIsRecording = true;
    myRecordingMacro = new ActionMacro(macroName);

    final StatusBar statusBar = WindowManager.getInstance().getIdeFrame(null).getStatusBar();
    myWidget = new Widget(statusBar);
    statusBar.addWidget(myWidget);
  }

  private class Widget implements CustomStatusBarWidget, Consumer<MouseEvent> {

    private AnimatedIconComponent myIcon =
            new AnimatedIconComponent("Macro recording", new Image[]{AllIcons.Ide.Macro.Recording_1, AllIcons.Ide.Macro.Recording_2, AllIcons.Ide.Macro.Recording_3, AllIcons.Ide.Macro.Recording_4},
                                      AllIcons.Ide.Macro.Recording_1, 1000);
    private StatusBar myStatusBar;
    private final WidgetPresentation myPresentation;

    private JPanel myBalloonComponent;
    private Balloon myBalloon;
    private final JLabel myText;

    private Widget(StatusBar statusBar) {
      myStatusBar = statusBar;
      myPresentation = new WidgetPresentation() {
        @Override
        public String getTooltipText() {
          return "Macro is being recorded now";
        }

        @Override
        public Consumer<MouseEvent> getClickConsumer() {
          return Widget.this;
        }
      };


      new BaseButtonBehavior(myIcon) {
        @Override
        protected void execute(MouseEvent e) {
          showBalloon();
        }
      };

      myBalloonComponent = new NonOpaquePanel(new BorderLayout());

      final AnAction stopAction = ActionManager.getInstance().getAction("StartStopMacroRecording");
      final DefaultActionGroup group = new DefaultActionGroup();
      group.add(stopAction);
      final ActionToolbar tb = ActionManager.getInstance().createActionToolbar(ActionPlaces.STATUS_BAR_PLACE, group, true);
      tb.setMiniMode(true);

      final NonOpaquePanel top = new NonOpaquePanel(new BorderLayout());
      top.add(tb.getComponent(), BorderLayout.WEST);
      myText = new JLabel(RECORDED + "..." + TYPING_SAMPLE, SwingConstants.LEFT);
      final Dimension preferredSize = myText.getPreferredSize();
      myText.setPreferredSize(preferredSize);
      myText.setText("Macro recording started...");
      myLastTyping = "";
      top.add(myText, BorderLayout.CENTER);
      myBalloonComponent.add(top, BorderLayout.CENTER);
    }

    private void showBalloon() {
      if (myBalloon != null) {
        Disposer.dispose(myBalloon);
        return;
      }

      myBalloon = JBPopupFactory.getInstance().createBalloonBuilder(myBalloonComponent).setAnimationCycle(200).setCloseButtonEnabled(true).setHideOnAction(false).setHideOnClickOutside(false)
              .setHideOnFrameResize(false).setHideOnKeyOutside(false).setSmallVariant(true).setShadow(true).createBalloon();

      Disposer.register(myBalloon, new Disposable() {
        @Override
        public void dispose() {
          myBalloon = null;
        }
      });

      myBalloon.addListener(new JBPopupAdapter() {
        @Override
        public void onClosed(LightweightWindowEvent event) {
          if (myBalloon != null) {
            Disposer.dispose(myBalloon);
          }
        }
      });

      myBalloon.show(new PositionTracker<Balloon>(myIcon) {
        @Override
        public RelativePoint recalculateLocation(Balloon object) {
          return new RelativePoint(myIcon, new Point(myIcon.getSize().width / 2, 4));
        }
      }, Balloon.Position.above);
    }

    @Override
    public JComponent getComponent() {
      return myIcon;
    }

    @Nonnull
    @Override
    public String ID() {
      return "MacroRecording";
    }

    @Override
    public void accept(MouseEvent mouseEvent) {
    }

    @Override
    public WidgetPresentation getPresentation() {
      return myPresentation;
    }

    @Override
    public void install(@Nonnull StatusBar statusBar) {
      showBalloon();
    }

    @Override
    public void dispose() {
      myIcon.dispose();
      if (myBalloon != null) {
        Disposer.dispose(myBalloon);
      }
    }

    public void delete() {
      if (myBalloon != null) {
        Disposer.dispose(myBalloon);
      }
      myStatusBar.removeWidget(ID());
    }

    public void notifyUser(String text) {
      myText.setText(text);
      myText.revalidate();
      myText.repaint();
    }
  }

  public void stopRecording(@Nullable Project project) {
    LOG.assertTrue(myIsRecording);

    if (myWidget != null) {
      myWidget.delete();
      myWidget = null;
    }

    myIsRecording = false;
    myLastActionInputEvent.clear();
    String macroName;
    do {
      macroName = Messages.showInputDialog(project, IdeBundle.message("prompt.enter.macro.name"), IdeBundle.message("title.enter.macro.name"), Messages.getQuestionIcon());
      if (macroName == null) {
        myRecordingMacro = null;
        return;
      }

      if (macroName.isEmpty()) macroName = null;
    }
    while (macroName != null && !checkCanCreateMacro(macroName));

    myLastMacro = myRecordingMacro;
    addRecordedMacroWithName(macroName);
    registerActions();
  }

  private void addRecordedMacroWithName(@Nullable String macroName) {
    if (macroName != null) {
      myRecordingMacro.setName(macroName);
      myMacros.add(myRecordingMacro);
      myRecordingMacro = null;
    }
    else {
      for (int i = 0; i < myMacros.size(); i++) {
        ActionMacro macro = myMacros.get(i);
        if (IdeBundle.message("macro.noname").equals(macro.getName())) {
          myMacros.set(i, myRecordingMacro);
          myRecordingMacro = null;
          break;
        }
      }
      if (myRecordingMacro != null) {
        myMacros.add(myRecordingMacro);
        myRecordingMacro = null;
      }
    }
  }

  public void playbackLastMacro() {
    if (myLastMacro != null) {
      playbackMacro(myLastMacro);
    }
  }

  private void playbackMacro(ActionMacro macro) {
    final IdeFrame frame = WindowManager.getInstance().getIdeFrame(null);
    assert frame != null;

    StringBuffer script = new StringBuffer();
    ActionMacro.ActionDescriptor[] actions = macro.getActions();
    for (ActionMacro.ActionDescriptor each : actions) {
      each.generateTo(script);
    }

    final PlaybackRunner runner = new PlaybackRunner(script.toString(), new PlaybackRunner.StatusCallback.Edt() {

      @Override
      public void messageEdt(PlaybackContext context, String text, Type type) {
        if (type == Type.message || type == Type.error) {
          if (context != null) {
            frame.getStatusBar().setInfo("Line " + context.getCurrentLine() + ": " + text);
          }
          else {
            frame.getStatusBar().setInfo(text);
          }
        }
      }
    }, Registry.is("actionSystem.playback.useDirectActionCall"), true, Registry.is("actionSystem.playback.useTypingTargets"));

    myIsPlaying = true;

    runner.run().doWhenDone(new Runnable() {
      @Override
      public void run() {
        frame.getStatusBar().setInfo("Script execution finished");
      }
    }).doWhenProcessed(new Runnable() {
      @Override
      public void run() {
        myIsPlaying = false;
      }
    });
  }

  public boolean isRecording() {
    return myIsRecording;
  }

  @Override
  public void dispose() {
    IdeEventQueueProxy.getInstance().removePostprocessor(myKeyProcessor);
  }

  public ActionMacro[] getAllMacros() {
    return myMacros.toArray(new ActionMacro[myMacros.size()]);
  }

  public void removeAllMacros() {
    if (myLastMacro != null) {
      myLastMacroName = myLastMacro.getName();
      myLastMacro = null;
    }
    myMacros = new ArrayList<ActionMacro>();
  }

  public void addMacro(ActionMacro macro) {
    myMacros.add(macro);
    if (myLastMacroName != null && myLastMacroName.equals(macro.getName())) {
      myLastMacro = macro;
      myLastMacroName = null;
    }
  }

  public void playMacro(ActionMacro macro) {
    playbackMacro(macro);
    myLastMacro = macro;
  }

  public boolean hasRecentMacro() {
    return myLastMacro != null;
  }

  public void registerActions() {
    unregisterActions();
    HashSet<String> registeredIds = new HashSet<String>(); // to prevent exception if 2 or more targets have the same name

    ActionMacro[] macros = getAllMacros();
    for (final ActionMacro macro : macros) {
      String actionId = macro.getActionId();

      if (!registeredIds.contains(actionId)) {
        registeredIds.add(actionId);
        myActionManager.registerAction(actionId, new InvokeMacroAction(macro));
      }
    }
  }

  public void unregisterActions() {

    // unregister Tool actions
    String[] oldIds = myActionManager.getActionIds(ActionMacro.MACRO_ACTION_PREFIX);
    for (final String oldId : oldIds) {
      myActionManager.unregisterAction(oldId);
    }
  }

  public boolean checkCanCreateMacro(String name) {
    final ActionManagerEx actionManager = (ActionManagerEx)ActionManager.getInstance();
    final String actionId = ActionMacro.MACRO_ACTION_PREFIX + name;
    if (actionManager.getAction(actionId) != null) {
      if (Messages.showYesNoDialog(IdeBundle.message("message.macro.exists", name), IdeBundle.message("title.macro.name.already.used"), Messages.getWarningIcon()) != 0) {
        return false;
      }
      actionManager.unregisterAction(actionId);
      removeMacro(name);
    }

    return true;
  }

  private void removeMacro(String name) {
    for (int i = 0; i < myMacros.size(); i++) {
      ActionMacro macro = myMacros.get(i);
      if (name.equals(macro.getName())) {
        myMacros.remove(i);
        break;
      }
    }
  }

  public boolean isPlaying() {
    return myIsPlaying;
  }

  private static class InvokeMacroAction extends AnAction {
    private final ActionMacro myMacro;

    InvokeMacroAction(ActionMacro macro) {
      myMacro = macro;
      getTemplatePresentation().setText(macro.getName(), false);
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
      IdeEventQueueProxy.getInstance().doWhenReady(() -> getInstance().playMacro(myMacro));
    }

    @RequiredUIAccess
    @Override
    public void update(AnActionEvent e) {
      super.update(e);
      e.getPresentation().setEnabled(!getInstance().isPlaying());
    }
  }

  private class MyKeyPostpocessor implements Predicate<AWTEvent> {

    @Override
    public boolean test(AWTEvent e) {
      if (isRecording() && e instanceof KeyEvent) {
        postProcessKeyEvent((KeyEvent)e);
      }
      return false;
    }

    public void postProcessKeyEvent(KeyEvent e) {
      if (e.getID() != KeyEvent.KEY_PRESSED) return;
      if (myLastActionInputEvent.contains(e)) {
        myLastActionInputEvent.remove(e);
        return;
      }
      final boolean modifierKeyIsPressed = e.getKeyCode() == KeyEvent.VK_CONTROL || e.getKeyCode() == KeyEvent.VK_ALT || e.getKeyCode() == KeyEvent.VK_META || e.getKeyCode() == KeyEvent.VK_SHIFT;
      if (modifierKeyIsPressed) return;

      final boolean ready = IdeEventQueueProxy.getInstance().isKeyEventDispatcherReady();
      final boolean isChar = e.getKeyChar() != KeyEvent.CHAR_UNDEFINED && UIUtil.isReallyTypedEvent(e);
      final boolean hasActionModifiers = e.isAltDown() | e.isControlDown() | e.isMetaDown();
      final boolean plainType = isChar && !hasActionModifiers;
      final boolean isEnter = e.getKeyCode() == KeyEvent.VK_ENTER;

      if (plainType && ready && !isEnter) {
        myRecordingMacro.appendKeytyped(e.getKeyChar(), e.getKeyCode(), e.getModifiers());
        notifyUser(Character.valueOf(e.getKeyChar()).toString(), true);
      }
      else if ((!plainType && ready) || isEnter) {
        final String stroke = KeyStroke.getKeyStrokeForEvent(e).toString();

        final int pressed = stroke.indexOf("pressed");
        String key = stroke.substring(pressed + "pressed".length());
        String modifiers = stroke.substring(0, pressed);

        String shortcut = (modifiers.replaceAll("ctrl", "control").trim() + " " + key.trim()).trim();

        myRecordingMacro.appendShortcut(shortcut);
        notifyUser(KeymapUtil.getKeystrokeText(KeyStroke.getKeyStrokeForEvent(e)), false);
      }
    }
  }

  private void notifyUser(String text, boolean typing) {
    String actualText = text;
    if (typing) {
      int maxLength = TYPING_SAMPLE.length();
      myLastTyping += text;
      if (myLastTyping.length() > maxLength) {
        myLastTyping = "..." + myLastTyping.substring(myLastTyping.length() - maxLength);
      }
      actualText = myLastTyping;
    }
    else {
      myLastTyping = "";
    }

    if (myWidget != null) {
      myWidget.notifyUser(RECORDED + actualText);
    }
  }
}
