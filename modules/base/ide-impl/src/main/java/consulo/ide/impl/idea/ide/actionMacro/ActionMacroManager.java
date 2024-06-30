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
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.util.registry.Registry;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ide.impl.idea.openapi.ui.playback.PlaybackContext;
import consulo.ide.impl.idea.openapi.ui.playback.PlaybackRunner;
import consulo.ide.impl.ui.IdeEventQueueProxy;
import consulo.logging.Logger;
import consulo.ide.localize.IdeLocalize;
import consulo.project.Project;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.StatusBarWidgetsManager;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.event.AnActionListener;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.internal.ActionManagerEx;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.JDOMExternalizable;
import consulo.util.xml.serializer.WriteExternalException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.*;
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

  private boolean myIsRecording;
  private final ActionManager myActionManager;
  private ActionMacro myLastMacro;
  private ActionMacro myRecordingMacro;
  private ArrayList<ActionMacro> myMacros = new ArrayList<>();
  private String myLastMacroName = null;
  private boolean myIsPlaying = false;
  private static final String ELEMENT_MACRO = "macro";
  private final Predicate<AWTEvent> myKeyProcessor;

  private Set<InputEvent> myLastActionInputEvent = new HashSet<>();

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
          if (event.getInputEvent() instanceof KeyEvent keyEvent) {
            shortcut = KeymapUtil.getKeystrokeText(KeyStroke.getKeyStrokeForEvent(keyEvent));
          }
          notifyUser(dataContext, id + (shortcut != null ? " (" + shortcut + ")" : ""), false);
          myLastActionInputEvent.add(event.getInputEvent());
        }
      }
    });

    myKeyProcessor = new MyKeyPostpocessor();

    IdeEventQueueProxy.getInstance().addPostprocessor(myKeyProcessor, null);
  }

  @Override
  public void readExternal(Element element) throws InvalidDataException {
    myMacros = new ArrayList<>();
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

  @RequiredUIAccess
  public void startRecording(Project project, String macroName) {
    LOG.assertTrue(!myIsRecording);
    myIsRecording = true;
    myRecordingMacro = new ActionMacro(macroName);

    StatusBarWidgetsManager.getInstance(project).updateWidget(ActionMacroWidgetFactory.class, UIAccess.current());
  }

  public void stopRecording(@Nonnull Project project) {
    LOG.assertTrue(myIsRecording);

    myIsRecording = false;
    myLastActionInputEvent.clear();
    String macroName;
    do {
      macroName = Messages.showInputDialog(
        project,
        IdeLocalize.promptEnterMacroName().get(),
        IdeLocalize.titleEnterMacroName().get(),
        Messages.getQuestionIcon()
      );
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

    StatusBarWidgetsManager.getInstance(project).updateWidget(ActionMacroWidgetFactory.class, UIAccess.current());
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
        if (IdeLocalize.macroNoname().get().equals(macro.getName())) {
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

    runner.run().doWhenDone(() -> frame.getStatusBar().setInfo("Script execution finished")).doWhenProcessed(() -> myIsPlaying = false);
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
    myMacros = new ArrayList<>();
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
    HashSet<String> registeredIds = new HashSet<>(); // to prevent exception if 2 or more targets have the same name

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
      if (Messages.showYesNoDialog(
        IdeLocalize.messageMacroExists(name).get(),
        IdeLocalize.titleMacroNameAlreadyUsed().get(),
        Messages.getWarningIcon()
      ) != 0) {
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
      if (isRecording() && e instanceof KeyEvent keyEvent) {
        postProcessKeyEvent(keyEvent);
      }
      return false;
    }

    public void postProcessKeyEvent(KeyEvent e) {
      if (e.getID() != KeyEvent.KEY_PRESSED) return;
      if (myLastActionInputEvent.contains(e)) {
        myLastActionInputEvent.remove(e);
        return;
      }
      final boolean modifierKeyIsPressed =
        e.getKeyCode() == KeyEvent.VK_CONTROL || e.getKeyCode() == KeyEvent.VK_ALT || e.getKeyCode() == KeyEvent.VK_META || e.getKeyCode() == KeyEvent.VK_SHIFT;
      if (modifierKeyIsPressed) return;

      final boolean ready = IdeEventQueueProxy.getInstance().isKeyEventDispatcherReady();
      final boolean isChar = e.getKeyChar() != KeyEvent.CHAR_UNDEFINED && UIUtil.isReallyTypedEvent(e);
      final boolean hasActionModifiers = e.isAltDown() | e.isControlDown() | e.isMetaDown();
      final boolean plainType = isChar && !hasActionModifiers;
      final boolean isEnter = e.getKeyCode() == KeyEvent.VK_ENTER;

      if (plainType && ready && !isEnter) {
        myRecordingMacro.appendKeytyped(e.getKeyChar(), e.getKeyCode(), e.getModifiers());
        notifyUser(DataManager.getInstance().getDataContext(e.getComponent()), Character.valueOf(e.getKeyChar()).toString(), true);
      }
      else if ((!plainType && ready) || isEnter) {
        final String stroke = KeyStroke.getKeyStrokeForEvent(e).toString();

        final int pressed = stroke.indexOf("pressed");
        String key = stroke.substring(pressed + "pressed".length());
        String modifiers = stroke.substring(0, pressed);

        String shortcut = (modifiers.replaceAll("ctrl", "control").trim() + " " + key.trim()).trim();

        myRecordingMacro.appendShortcut(shortcut);
        notifyUser(DataManager.getInstance().getDataContext(e.getComponent()),
                   KeymapUtil.getKeystrokeText(KeyStroke.getKeyStrokeForEvent(e)),
                   false);
      }
    }
  }

  private void notifyUser(DataContext dataContext, String text, boolean typing) {
    Project project = dataContext.getData(Project.KEY);
    if (project == null) {
      return;
    }

    StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
    if (statusBar == null) {
      return;
    }

    Optional<ActionMacroWidget> optional = statusBar.findWidget(widget -> widget instanceof ActionMacroWidget);
    optional.ifPresent(actionMacroWidget -> actionMacroWidget.notifyUser(text, typing));
  }
}
