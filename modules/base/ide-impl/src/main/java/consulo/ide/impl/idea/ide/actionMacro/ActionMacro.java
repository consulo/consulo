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

import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorActionManager;
import consulo.codeEditor.action.TypedAction;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.openapi.ui.playback.commands.KeyCodeTypeCommand;
import consulo.ide.impl.idea.openapi.ui.playback.commands.TypeCommand;
import consulo.ide.localize.IdeLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.awt.AWTConstants;
import consulo.ui.ex.internal.ActionUpdateInvoker;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.JDOMExternalizable;
import consulo.util.xml.serializer.WriteExternalException;
import org.jdom.Element;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author max
 */
public class ActionMacro implements JDOMExternalizable {
  private String myName;

  private final List<ActionDescriptor> myActions = new ArrayList<>();
  public static final String MACRO_ACTION_PREFIX = "Macro.";
  private static final String ATTRIBUTE_NAME = "name";
  private static final String ELEMENT_TYPING = "typing";

  private static final String ELEMENT_SHORTCUT = "shortuct";
  private static final String ATTRIBUTE_TEXT = "text";
  private static final String ATTRIBUTE_KEY_CODES = "text-keycode";
  private static final String ELEMENT_ACTION = "action";
  private static final String ATTRIBUTE_ID = "id";

  public ActionMacro() {
  }

  public ActionMacro(String name) {
    myName = name;
  }

  public String getName() {
    return myName;
  }

  public void setName(String name) {
    myName = name;
  }

  public ActionDescriptor[] getActions() {
    return myActions.toArray(ActionDescriptor[]::new);
  }

  @Override
  public void readExternal(Element macro) throws InvalidDataException {
    setName(macro.getAttributeValue(ATTRIBUTE_NAME));
    List actions = macro.getChildren();
    for (Object o : actions) {
      Element action = (Element)o;
      if (ELEMENT_TYPING.equals(action.getName())) {
        Pair<List<Integer>, List<Integer>> codes = parseKeyCodes(action.getAttributeValue(ATTRIBUTE_KEY_CODES));

        String text = action.getText();
        if (text == null || text.length() == 0) {
          text = action.getAttributeValue(ATTRIBUTE_TEXT);
        }
        text = text.replaceAll("&#x20;", " ");

        if (!StringUtil.isEmpty(text)) {
          myActions.add(new TypedDescriptor(text, codes.getFirst(), codes.getSecond()));
        }
      }
      else if (ELEMENT_ACTION.equals(action.getName())) {
        myActions.add(new IdActionDescriptor(action.getAttributeValue(ATTRIBUTE_ID)));
      }
      else if (ELEMENT_SHORTCUT.equals(action.getName())) {
        myActions.add(new ShortcutActionDesciption(action.getAttributeValue(ATTRIBUTE_TEXT)));
      }
    }
  }

  private static Pair<List<Integer>, List<Integer>> parseKeyCodes(String keyCodesText) {
    return KeyCodeTypeCommand.parseKeyCodes(keyCodesText);
  }

  public static String unparseKeyCodes(Pair<List<Integer>, List<Integer>> keyCodes) {
    return KeyCodeTypeCommand.unparseKeyCodes(keyCodes);
  }

  @Override
  public void writeExternal(Element macro) throws WriteExternalException {
    macro.setAttribute(ATTRIBUTE_NAME, myName);
    ActionDescriptor[] actions = getActions();
    for (ActionDescriptor action : actions) {
      Element actionNode = null;
      if (action instanceof TypedDescriptor typedDescriptor) {
        actionNode = new Element(ELEMENT_TYPING);
        actionNode.setText(typedDescriptor.getText().replaceAll(" ", "&#x20;"));
        actionNode.setAttribute(ATTRIBUTE_KEY_CODES, unparseKeyCodes(
          new Pair<>(typedDescriptor.getKeyCodes(), typedDescriptor.getKeyModifiers())));
      }
      else if (action instanceof IdActionDescriptor idActionDescriptor) {
        actionNode = new Element(ELEMENT_ACTION);
        actionNode.setAttribute(ATTRIBUTE_ID, idActionDescriptor.getActionId());
      }
      else if (action instanceof ShortcutActionDesciption shortcutActionDesciption) {
        actionNode = new Element(ELEMENT_SHORTCUT);
        actionNode.setAttribute(ATTRIBUTE_TEXT, shortcutActionDesciption.getText());
      }

      assert actionNode != null : action;

      macro.addContent(actionNode);
    }
  }

  @Override
  public String toString() {
    return myName;
  }

  @Override
  protected Object clone() {
    ActionMacro copy = new ActionMacro(myName);
    for (int i = 0; i < myActions.size(); i++) {
      ActionDescriptor action = myActions.get(i);
      copy.myActions.add((ActionDescriptor)action.clone());
    }

    return copy;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    return this == o
      || o instanceof ActionMacro that && myActions.equals(that.myActions) && myName.equals(that.myName);
  }

  @Override
  public int hashCode() {
    return 29 * myName.hashCode() + myActions.hashCode();
  }

  public void deleteAction(int idx) {
    myActions.remove(idx);
  }

  public void appendAction(String actionId) {
    myActions.add(new IdActionDescriptor(actionId));
  }

  public void appendShortcut(String text) {
    myActions.add(new ShortcutActionDesciption(text));
  }

  public void appendKeytyped(char c, int keyCode, @AWTConstants.InputEventMask int modifiers) {
    ActionDescriptor lastAction = myActions.size() > 0 ? myActions.get(myActions.size() - 1) : null;
    if (lastAction instanceof TypedDescriptor typedDescriptor) {
      typedDescriptor.addChar(c, keyCode, modifiers);
    }
    else {
      myActions.add(new TypedDescriptor(c, keyCode, modifiers));
    }
  }

  public String getActionId() {
    return MACRO_ACTION_PREFIX + myName;
  }

  public interface ActionDescriptor {
    Object clone();

    @RequiredUIAccess
    void playBack(DataContext context);

    void generateTo(StringBuffer script);
  }

  public static class TypedDescriptor implements ActionDescriptor {

    private String myText;

    private final List<Integer> myKeyCodes = new ArrayList<>();
    private final List<Integer> myModifiers = new ArrayList<>();

    public TypedDescriptor(String text, List<Integer> keyCodes, List<Integer> modifiers) {
      myText = text;
      myKeyCodes.addAll(keyCodes);
      myModifiers.addAll(modifiers);

      assert myKeyCodes.size() == myModifiers.size() : "codes=" + myKeyCodes.toString() + " modifiers=" + myModifiers.toString();
    }

    public TypedDescriptor(char c, int keyCode, @AWTConstants.InputEventMask int modifiers) {
      myText = String.valueOf(c);
      myKeyCodes.add(keyCode);
      myModifiers.add(modifiers);
    }

    public void addChar(char c, int keyCode, @AWTConstants.InputEventMask int modifier) {
      myText += c;
      myKeyCodes.add(keyCode);
      myModifiers.add(modifier);
    }

    public String getText() {
      return myText;
    }

    public Object clone() {
      return new TypedDescriptor(myText, myKeyCodes, myModifiers);
    }

    @Override
    public boolean equals(@Nullable Object o) {
      return this == o
        || o instanceof TypedDescriptor that && myText.equals(that.myText);
    }

    @Override
    public int hashCode() {
      return myText.hashCode();
    }

    @Override
    public void generateTo(StringBuffer script) {
      if (TypeCommand.containsUnicode(myText)) {
        script.append(KeyCodeTypeCommand.PREFIX).append(" ");

        for (int i = 0; i < myKeyCodes.size(); i++) {
          Integer each = myKeyCodes.get(i);
          script.append(each.toString());
          script.append(KeyCodeTypeCommand.MODIFIER_DELIMITER);
          script.append(myModifiers.get(i));
          if (i < myKeyCodes.size() - 1) {
            script.append(KeyCodeTypeCommand.CODE_DELIMITER);
          }
        }
        script.append(" ").append(myText).append("\n");
      }
      else {
        script.append(myText);
        script.append("\n");
      }
    }

    @Override
    public String toString() {
      return IdeLocalize.actionDescriptorTyping(myText).get();
    }

    @Override
    public void playBack(DataContext context) {
      Editor editor = context.getData(Editor.KEY);
      if (editor == null) {
        return;
      }
      TypedAction typedAction = EditorActionManager.getInstance().getTypedAction();
      for (char aChar : myText.toCharArray()) {
        typedAction.actionPerformed(editor, aChar, context);
      }
    }

    public List<Integer> getKeyCodes() {
      return myKeyCodes;
    }

    public List<Integer> getKeyModifiers() {
      return myModifiers;
    }
  }

  public static class ShortcutActionDesciption implements ActionDescriptor {
    private final String myKeyStroke;

    public ShortcutActionDesciption(String stroke) {
      myKeyStroke = stroke;
    }

    @Override
    public Object clone() {
      return new ShortcutActionDesciption(myKeyStroke);
    }

    @Override
    public void playBack(DataContext context) {
    }

    @Override
    public void generateTo(StringBuffer script) {
      script.append("%[").append(myKeyStroke).append("]\n");
    }

    @Override
    public String toString() {
      return IdeLocalize.actionDescriptorKeystroke(myKeyStroke).get();
    }

    public String getText() {
      return myKeyStroke;
    }
  }

  public static class IdActionDescriptor implements ActionDescriptor {
    private final String actionId;

    public IdActionDescriptor(String id) {
      this.actionId = id;
    }

    public String getActionId() {
      return actionId;
    }

    @Override
    public String toString() {
      return IdeLocalize.actionDescriptorAction(actionId).get();
    }

    @Override
    public Object clone() {
      return new IdActionDescriptor(actionId);
    }

    @Override
    public boolean equals(@Nullable Object o) {
      return this == o
        || o instanceof IdActionDescriptor that && actionId.equals(that.actionId);
    }

    @Override
    public int hashCode() {
      return actionId.hashCode();
    }

    @Override
    @RequiredUIAccess
    public void playBack(DataContext context) {
      AnAction action = ActionManager.getInstance().getAction(getActionId());
      if (action == null) return;
      Presentation presentation = action.getTemplatePresentation().clone();
      AnActionEvent event = new AnActionEvent(null, context, "MACRO_PLAYBACK", presentation, ActionManager.getInstance(), 0);
      ActionUpdateInvoker.updateSync(action, event);
      if (!presentation.isEnabled()) {
        return;
      }
      action.actionPerformed(event);
    }

    @Override
    public void generateTo(StringBuffer script) {
      script.append("%action ").append(getActionId()).append("\n");
    }
  }
}
