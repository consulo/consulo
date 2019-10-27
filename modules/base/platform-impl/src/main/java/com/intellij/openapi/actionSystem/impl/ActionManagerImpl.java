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
package com.intellij.openapi.actionSystem.impl;

import com.intellij.AbstractBundle;
import com.intellij.CommonBundle;
import com.intellij.diagnostic.PluginException;
import com.intellij.ide.ActivityTracker;
import com.intellij.ide.DataManager;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.idea.ActionsBundle;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationActivationListener;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.application.impl.LaterInvocator;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.keymap.ex.KeymapManagerEx;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.UIUtil;
import consulo.application.TransactionGuardEx;
import consulo.container.plugin.PluginDescriptor;
import consulo.extensions.ListOfElementsEP;
import consulo.logging.Logger;
import consulo.platform.impl.action.LastActionTracker;
import consulo.ui.image.Image;
import consulo.util.nodep.xml.node.SimpleXmlElement;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import gnu.trove.TObjectIntHashMap;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.Timer;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.*;

@Singleton
public final class ActionManagerImpl extends ActionManagerEx implements Disposable {
  private static final Logger LOG = Logger.getInstance("#com.intellij.openapi.actionSystem.impl.ActionManagerImpl");
  private static final int DEACTIVATED_TIMER_DELAY = 5000;
  private static final int TIMER_DELAY = 500;
  private static final int UPDATE_DELAY_AFTER_TYPING = 500;

  private final Object myLock = new Object();
  private final Map<String, AnAction> myId2Action = new THashMap<>();
  private final Map<PluginId, THashSet<String>> myPlugin2Id = new THashMap<>();
  private final TObjectIntHashMap<String> myId2Index = new TObjectIntHashMap<>();
  private final Map<Object, String> myAction2Id = new THashMap<>();
  private final List<String> myNotRegisteredInternalActionIds = new ArrayList<>();
  private MyTimer myTimer;

  private int myRegisteredActionsCount;
  private String myLastPreformedActionId;
  private final Application myApplication;
  private final KeymapManager myKeymapManager;
  private final DataManager myDataManager;
  private String myPrevPerformedActionId;
  private long myLastTimeEditorWasTypedIn = 0;
  @NonNls
  public static final String ACTION_ELEMENT_NAME = "action";
  @NonNls
  public static final String GROUP_ELEMENT_NAME = "group";
  @NonNls
  public static final String ACTIONS_ELEMENT_NAME = "actions";
  @NonNls
  public static final String CLASS_ATTR_NAME = "class";
  @NonNls
  public static final String ID_ATTR_NAME = "id";
  @NonNls
  public static final String INTERNAL_ATTR_NAME = "internal";
  @NonNls
  public static final String ICON_ATTR_NAME = "icon";
  @NonNls
  public static final String REQUIRE_MODULE_EXTENSIONS = "require-module-extensions";
  @NonNls
  public static final String CAN_USE_PROJECT_AS_DEFAULT = "can-use-project-as-default";
  @NonNls
  public static final String ADD_TO_GROUP_ELEMENT_NAME = "add-to-group";
  @NonNls
  public static final String SHORTCUT_ELEMENT_NAME = "keyboard-shortcut";
  @NonNls
  public static final String MOUSE_SHORTCUT_ELEMENT_NAME = "mouse-shortcut";
  @NonNls
  public static final String DESCRIPTION = "description";
  @NonNls
  public static final String TEXT_ATTR_NAME = "text";
  @NonNls
  public static final String POPUP_ATTR_NAME = "popup";
  @NonNls
  public static final String SEPARATOR_ELEMENT_NAME = "separator";
  @NonNls
  public static final String REFERENCE_ELEMENT_NAME = "reference";
  @NonNls
  public static final String GROUPID_ATTR_NAME = "group-id";
  @NonNls
  public static final String ANCHOR_ELEMENT_NAME = "anchor";
  @NonNls
  public static final String FIRST = "first";
  @NonNls
  public static final String LAST = "last";
  @NonNls
  public static final String BEFORE = "before";
  @NonNls
  public static final String AFTER = "after";
  @NonNls
  public static final String SECONDARY = "secondary";
  @NonNls
  public static final String RELATIVE_TO_ACTION_ATTR_NAME = "relative-to-action";
  @NonNls
  public static final String FIRST_KEYSTROKE_ATTR_NAME = "first-keystroke";
  @NonNls
  public static final String SECOND_KEYSTROKE_ATTR_NAME = "second-keystroke";
  @NonNls
  public static final String REMOVE_SHORTCUT_ATTR_NAME = "remove";
  @NonNls
  public static final String REPLACE_SHORTCUT_ATTR_NAME = "replace-all";
  @NonNls
  public static final String KEYMAP_ATTR_NAME = "keymap";
  @NonNls
  public static final String KEYSTROKE_ATTR_NAME = "keystroke";
  @NonNls
  public static final String REF_ATTR_NAME = "ref";
  @NonNls
  public static final String ACTIONS_BUNDLE = "messages.ActionsBundle";
  @NonNls
  public static final String USE_SHORTCUT_OF_ATTR_NAME = "use-shortcut-of";

  private final List<ActionPopupMenuImpl> myPopups = new ArrayList<>();

  private boolean myTransparentOnlyUpdate;

  private final List<AnActionListener> myActionListeners = ContainerUtil.createLockFreeCopyOnWriteList();
  private final AnActionListener myMessageBusPublisher;

  @Inject
  ActionManagerImpl(Application application, KeymapManager keymapManager, DataManager dataManager) {
    myApplication = application;
    myKeymapManager = keymapManager;
    myDataManager = dataManager;
    myMessageBusPublisher = application.getMessageBus().syncPublisher(AnActionListener.TOPIC);

    registerPluginActions();
  }

  @Override
  public void dispose() {
    if (myTimer != null) {
      myTimer.stop();
      myTimer = null;
    }
  }

  @Override
  public void addTimerListener(int delay, final TimerListener listener) {
    _addTimerListener(listener, false);
  }

  @Override
  public void removeTimerListener(TimerListener listener) {
    _removeTimerListener(listener, false);
  }

  @Override
  public void addTransparentTimerListener(int delay, TimerListener listener) {
    _addTimerListener(listener, true);
  }

  @Override
  public void removeTransparentTimerListener(TimerListener listener) {
    _removeTimerListener(listener, true);
  }

  private void _addTimerListener(final TimerListener listener, boolean transparent) {
    if (myApplication.isUnitTestMode()) return;
    if (myTimer == null) {
      myTimer = new MyTimer();
      myTimer.start();
    }

    myTimer.addTimerListener(listener, transparent);
  }

  private void _removeTimerListener(TimerListener listener, boolean transparent) {
    if (myApplication.isUnitTestMode()) return;
    LOG.assertTrue(myTimer != null);

    myTimer.removeTimerListener(listener, transparent);
  }

  public ActionPopupMenu createActionPopupMenu(String place, @Nonnull ActionGroup group, @Nullable PresentationFactory presentationFactory) {
    return new ActionPopupMenuImpl(place, group, this, presentationFactory);
  }

  @Override
  public ActionPopupMenu createActionPopupMenu(String place, @Nonnull ActionGroup group) {
    return new ActionPopupMenuImpl(place, group, this, null);
  }

  @Override
  public ActionToolbar createActionToolbar(final String place, final ActionGroup group, final boolean horizontal) {
    return createActionToolbar(place, group, horizontal, false);
  }

  @Override
  public ActionToolbar createActionToolbar(final String place, final ActionGroup group, final boolean horizontal, final boolean decorateButtons) {
    return new ActionToolbarImpl(place, group, horizontal, decorateButtons, myDataManager, this, (KeymapManagerEx)myKeymapManager);
  }

  private void registerPluginActions() {
    for (PluginDescriptor plugin : consulo.container.plugin.PluginManager.getPlugins()) {
      if (PluginManagerCore.shouldSkipPlugin(plugin)) {
        continue;
      }

      final List<SimpleXmlElement> elementList = plugin.getActionsDescriptionElements();
      for (SimpleXmlElement e : elementList) {
        processActionsChildElement(plugin.getPluginClassLoader(), plugin.getPluginId(), e);
      }
    }
  }

  @Override
  public AnAction getAction(@Nonnull String id) {
    return getActionImpl(id, false);
  }

  private AnAction getActionImpl(String id, boolean canReturnStub) {
    AnAction action;
    synchronized (myLock) {
      action = myId2Action.get(id);
      if (canReturnStub || !(action instanceof ActionStub)) {
        return action;
      }
    }
    AnAction converted = convertStub((ActionStub)action);
    if (converted == null) return null;

    synchronized (myLock) {
      action = myId2Action.get(id);
      if (action instanceof ActionStub) {
        action = replaceStub((ActionStub)action, converted);
      }
      return action;
    }
  }

  /**
   * Converts action's stub to normal action.
   */
  @SuppressWarnings("unchecked")
  public AnAction convertStub(ActionStub stub) {
    Object obj;
    String className = stub.getClassName();
    Class actionClass = stub.resolveClass();
    if (actionClass == null) {
      PluginId pluginId = stub.getPluginId();
      if (pluginId != null) {
        throw new PluginException("class with name \"" + className + "\" not found", pluginId);
      }
      else {
        throw new IllegalStateException("class with name \"" + className + "\" not found");
      }
    }

    try {
      obj = myApplication.getInjectingContainer().getUnbindedInstance(actionClass);
    }
    catch (UnsupportedClassVersionError e) {
      PluginId pluginId = stub.getPluginId();
      if (pluginId != null) {
        throw new PluginException(e, pluginId);
      }
      else {
        throw new IllegalStateException(e);
      }
    }
    catch (Exception e) {
      PluginId pluginId = stub.getPluginId();
      if (pluginId != null) {
        throw new PluginException("cannot create class \"" + className + "\"", e, pluginId);
      }
      else {
        throw new IllegalStateException("cannot create class \"" + className + "\"", e);
      }
    }

    if (!(obj instanceof AnAction)) {
      throw new IllegalStateException("class with name \"" + className + "\" should be instance of " + AnAction.class.getName());
    }

    AnAction anAction = (AnAction)obj;
    stub.initAction(anAction);
    if (StringUtil.isNotEmpty(stub.getText())) {
      anAction.getTemplatePresentation().setText(stub.getText());
    }
    String iconPath = stub.getIconPath();
    if (iconPath != null) {
      setIconFromClass(anAction.getClass(), anAction.getClass().getClassLoader(), iconPath, stub.getClassName(), anAction.getTemplatePresentation(), stub.getPluginId());
    }

    return anAction;
  }

  @Nonnull
  private AnAction replaceStub(@Nonnull ActionStub stub, AnAction anAction) {
    LOG.assertTrue(myAction2Id.containsKey(stub));
    myAction2Id.remove(stub);

    LOG.assertTrue(myId2Action.containsKey(stub.getId()));

    AnAction action = myId2Action.remove(stub.getId());
    LOG.assertTrue(action != null);
    LOG.assertTrue(action.equals(stub));

    myAction2Id.put(anAction, stub.getId());

    return addToMap(stub.getId(), anAction);
  }

  private AnAction addToMap(String actionId, AnAction action) {
    myId2Action.put(actionId, action);
    return action;
  }

  @Override
  public String getId(@Nonnull AnAction action) {
    LOG.assertTrue(!(action instanceof ActionStub));
    synchronized (myLock) {
      return myAction2Id.get(action);
    }
  }

  @Override
  public String[] getActionIds(@Nonnull String idPrefix) {
    synchronized (myLock) {
      ArrayList<String> idList = new ArrayList<>();
      for (String id : myId2Action.keySet()) {
        if (id.startsWith(idPrefix)) {
          idList.add(id);
        }
      }
      return ArrayUtil.toStringArray(idList);
    }
  }

  @Override
  public boolean isGroup(@Nonnull String actionId) {
    return getActionImpl(actionId, true) instanceof ActionGroup;
  }

  @Override
  public JComponent createButtonToolbar(final String actionPlace, final ActionGroup messageActionGroup) {
    return new ButtonToolbarImpl(actionPlace, messageActionGroup, myDataManager, this);
  }

  @Override
  public AnAction getActionOrStub(String id) {
    return getActionImpl(id, true);
  }

  /**
   * @return instance of ActionGroup or ActionStub. The method never returns real subclasses
   * of <code>AnAction</code>.
   */
  @Nullable
  private AnAction processActionElement(SimpleXmlElement element, final ClassLoader loader, PluginId pluginId) {
    final IdeaPluginDescriptor plugin = PluginManager.getPlugin(pluginId);
    ResourceBundle bundle = getActionsResourceBundle(loader, plugin);

    if (!ACTION_ELEMENT_NAME.equals(element.getName())) {
      reportActionError(pluginId, "unexpected name of element \"" + element.getName() + "\"");
      return null;
    }
    String className = element.getAttributeValue(CLASS_ATTR_NAME);
    if (className == null || className.length() == 0) {
      reportActionError(pluginId, "action element should have specified \"class\" attribute");
      return null;
    }
    // read ID and register loaded action
    String id = element.getAttributeValue(ID_ATTR_NAME);
    if (id == null || id.length() == 0) {
      id = StringUtil.getShortName(className);
    }
    if (Boolean.valueOf(element.getAttributeValue(INTERNAL_ATTR_NAME)).booleanValue() && !myApplication.isInternal()) {
      myNotRegisteredInternalActionIds.add(id);
      return null;
    }

    String text = loadTextValueForElement(element, bundle, id, ACTION_ELEMENT_NAME, TEXT_ATTR_NAME);

    String iconPath = element.getAttributeValue(ICON_ATTR_NAME);

    if (text == null) {
      @NonNls String message = "'text' attribute is mandatory (action ID=" + id + ";" + (plugin == null ? "" : " plugin path: " + plugin.getPath()) + ")";
      reportActionError(pluginId, message);
      return null;
    }

    ActionStub stub = new ActionStub(className, id, text, loader, pluginId, iconPath);
    Presentation presentation = stub.getTemplatePresentation();
    presentation.setText(text);

    // description
    presentation.setDescription(loadTextValueForElement(element, bundle, id, ACTION_ELEMENT_NAME, DESCRIPTION));

    processModuleExtensionOptions(element, stub);

    // process all links and key bindings if any
    for (final SimpleXmlElement e : element.getChildren()) {
      if (ADD_TO_GROUP_ELEMENT_NAME.equals(e.getName())) {
        processAddToGroupNode(stub, e, pluginId, isSecondary(e));
      }
      else if (SHORTCUT_ELEMENT_NAME.equals(e.getName())) {
        processKeyboardShortcutNode(e, id, pluginId);
      }
      else if (MOUSE_SHORTCUT_ELEMENT_NAME.equals(e.getName())) {
        processMouseShortcutNode(e, id, pluginId);
      }
      else {
        reportActionError(pluginId, "unexpected name of element \"" + e.getName() + "\"");
        return null;
      }
    }
    if (element.getAttributeValue(USE_SHORTCUT_OF_ATTR_NAME) != null) {
      ((KeymapManagerEx)myKeymapManager).bindShortcuts(element.getAttributeValue(USE_SHORTCUT_OF_ATTR_NAME), id);
    }

    // register action
    registerAction(id, stub, pluginId);
    return stub;
  }

  private static void processModuleExtensionOptions(SimpleXmlElement element, AnAction action) {
    String canUseProjectAsDefaultText = element.getAttributeValue(CAN_USE_PROJECT_AS_DEFAULT);
    boolean canUseProjectAsDefault = !StringUtil.isEmpty(canUseProjectAsDefaultText) && Boolean.parseBoolean(canUseProjectAsDefaultText);

    action.setCanUseProjectAsDefault(canUseProjectAsDefault);

    String requestModuleExtensionsValue = element.getAttributeValue(REQUIRE_MODULE_EXTENSIONS);
    action.setModuleExtensionIds(ListOfElementsEP.getValuesOfVariableIfFound(requestModuleExtensionsValue));
  }

  @Nullable
  private static ResourceBundle getActionsResourceBundle(ClassLoader loader, PluginDescriptor plugin) {
    @NonNls final String resBundleName = plugin.getResourceBundleBaseName();
    ResourceBundle bundle = null;
    if (resBundleName != null) {
      bundle = AbstractBundle.getResourceBundle(resBundleName, loader);
    }
    return bundle;
  }

  private static boolean isSecondary(SimpleXmlElement element) {
    return "true".equalsIgnoreCase(element.getAttributeValue(SECONDARY));
  }

  private static void setIcon(@Nullable final String iconPath, final String className, final ClassLoader loader, final Presentation presentation, final PluginId pluginId) {
    if (iconPath == null) return;

    try {
      final Class actionClass = Class.forName(className, true, loader);
      setIconFromClass(actionClass, loader, iconPath, className, presentation, pluginId);
    }
    catch (ClassNotFoundException | NoClassDefFoundError e) {
      LOG.error(e);
      reportActionError(pluginId, "class with name \"" + className + "\" not found");
    }
  }

  private static void setIconFromClass(@Nonnull final Class actionClass,
                                       @Nonnull final ClassLoader classLoader,
                                       @Nonnull final String iconPath,
                                       final String className,
                                       final Presentation presentation,
                                       final PluginId pluginId) {

    Image lazyIcon = Image.lazy(() -> {
      //try to find icon in idea class path
      Image icon = IconLoader.findIcon(iconPath, actionClass, true);
      if (icon == null) {
        icon = IconLoader.findIcon(iconPath, classLoader);
      }

      if (icon == null) {
        reportActionError(pluginId, "Icon cannot be found in '" + iconPath + "', action class='" + className + "'");
      }

      return icon;
    });

    presentation.setIcon(lazyIcon);
  }

  private String loadTextValueForElement(final SimpleXmlElement element, final ResourceBundle bundle, final String id, String elementType, String type) {
    final String value = element.getAttributeValue(type);
    String key = elementType + "." + id + "." + type;
    String text = CommonBundle.messageOrDefault(bundle, key, value == null ? "" : value);
    return getDefaultInInternalOrNull(key, text);
  }

  @Nonnull
  private String getDefaultInInternalOrNull(@Nonnull String key, @Nonnull String text) {
    if (!StringUtil.isEmpty(text)) {
      return text;
    }

    text = ActionsBundle.message(key);
    if (text.isEmpty()) {
      return text;
    }
    // bundle will return value like !some.key! if not found in bunde
    if (text.charAt(0) == '!' && text.charAt(text.length() - 1) == '!') {
      return "";
    }
    else {
      return text;
    }
  }

  private AnAction processGroupElement(SimpleXmlElement element, final ClassLoader loader, PluginId pluginId) {
    final IdeaPluginDescriptor plugin = PluginManager.getPlugin(pluginId);
    ResourceBundle bundle = getActionsResourceBundle(loader, plugin);

    if (!GROUP_ELEMENT_NAME.equals(element.getName())) {
      reportActionError(pluginId, "unexpected name of element \"" + element.getName() + "\"");
      return null;
    }
    String className = element.getAttributeValue(CLASS_ATTR_NAME);
    if (className == null) { // use default group if class isn't specified
      className = DefaultActionGroup.class.getName();
    }
    try {
      Class aClass = Class.forName(className, true, loader);
      Object obj = Application.get().getInjectingContainer().getUnbindedInstance(aClass);

      if (!(obj instanceof ActionGroup)) {
        reportActionError(pluginId, "class with name \"" + className + "\" should be instance of " + ActionGroup.class.getName());
        return null;
      }
      if (element.getChildren().size() != element.getChildren(ADD_TO_GROUP_ELEMENT_NAME).size()) {  //
        if (!(obj instanceof DefaultActionGroup)) {
          reportActionError(pluginId, "class with name \"" + className + "\" should be instance of " + DefaultActionGroup.class.getName() + " because there are children specified");
          return null;
        }
      }
      ActionGroup group = (ActionGroup)obj;
      // read ID and register loaded group
      String id = element.getAttributeValue(ID_ATTR_NAME);
      if (id != null && id.length() == 0) {
        reportActionError(pluginId, "ID of the group cannot be an empty string");
        return null;
      }
      if (Boolean.valueOf(element.getAttributeValue(INTERNAL_ATTR_NAME)).booleanValue() && !myApplication.isInternal()) {
        myNotRegisteredInternalActionIds.add(id);
        return null;
      }

      if (id != null) {
        registerAction(id, group);
      }
      Presentation presentation = group.getTemplatePresentation();

      // text
      String text = loadTextValueForElement(element, bundle, id, GROUP_ELEMENT_NAME, TEXT_ATTR_NAME);
      // don't override value which was set in API with empty value from xml descriptor
      if (!StringUtil.isEmpty(text) || presentation.getText() == null) {
        presentation.setText(text);
      }

      // description
      String description = loadTextValueForElement(element, bundle, id, GROUP_ELEMENT_NAME, DESCRIPTION);
      // don't override value which was set in API with empty value from xml descriptor
      if (!StringUtil.isEmpty(description) || presentation.getDescription() == null) {
        presentation.setDescription(description);
      }

      processModuleExtensionOptions(element, group);

      // icon
      setIcon(element.getAttributeValue(ICON_ATTR_NAME), className, loader, presentation, pluginId);
      // popup
      String popup = element.getAttributeValue(POPUP_ATTR_NAME);
      if (popup != null) {
        group.setPopup(Boolean.valueOf(popup).booleanValue());
      }
      // process all group's children. There are other groups, actions, references and links
      for (final SimpleXmlElement o : element.getChildren()) {
        String name = o.getName();
        if (ACTION_ELEMENT_NAME.equals(name)) {
          AnAction action = processActionElement(o, loader, pluginId);
          if (action != null) {
            assertActionIsGroupOrStub(action);
            ((DefaultActionGroup)group).addAction(action, Constraints.LAST, this).setAsSecondary(isSecondary(o));
          }
        }
        else if (SEPARATOR_ELEMENT_NAME.equals(name)) {
          processSeparatorNode((DefaultActionGroup)group, o, pluginId);
        }
        else if (GROUP_ELEMENT_NAME.equals(name)) {
          AnAction action = processGroupElement(o, loader, pluginId);
          if (action != null) {
            ((DefaultActionGroup)group).add(action, this);
          }
        }
        else if (ADD_TO_GROUP_ELEMENT_NAME.equals(name)) {
          processAddToGroupNode(group, o, pluginId, isSecondary(o));
        }
        else if (REFERENCE_ELEMENT_NAME.equals(name)) {
          AnAction action = processReferenceElement(o, pluginId);
          if (action != null) {
            ((DefaultActionGroup)group).addAction(action, Constraints.LAST, this).setAsSecondary(isSecondary(o));
          }
        }
        else {
          reportActionError(pluginId, "unexpected name of element \"" + name + "\n");
          return null;
        }
      }
      return group;
    }
    catch (ClassNotFoundException e) {
      reportActionError(pluginId, "class with name \"" + className + "\" not found");
      return null;
    }
    catch (NoClassDefFoundError e) {
      reportActionError(pluginId, "class with name \"" + e.getMessage() + "\" not found");
      return null;
    }
    catch (UnsupportedClassVersionError e) {
      reportActionError(pluginId, "unsupported class version for " + className);
      return null;
    }
    catch (Exception e) {
      final String message = "cannot create class \"" + className + "\"";
      if (pluginId == null) {
        LOG.error(message, e);
      }
      else {
        LOG.error(new PluginException(message, e, pluginId));
      }
      return null;
    }
  }

  private void processReferenceNode(final SimpleXmlElement element, final PluginId pluginId) {
    final AnAction action = processReferenceElement(element, pluginId);

    for (final SimpleXmlElement child : element.getChildren()) {
      if (ADD_TO_GROUP_ELEMENT_NAME.equals(child.getName())) {
        processAddToGroupNode(action, child, pluginId, isSecondary(child));
      }
    }
  }

  /**
   * \
   *
   * @param element   description of link
   * @param pluginId
   * @param secondary
   */
  private void processAddToGroupNode(AnAction action, SimpleXmlElement element, final PluginId pluginId, boolean secondary) {
    // Real subclasses of AnAction should not be here
    if (!(action instanceof AnSeparator)) {
      assertActionIsGroupOrStub(action);
    }

    String actionName = action instanceof ActionStub ? ((ActionStub)action).getClassName() : action.getClass().getName();

    if (!ADD_TO_GROUP_ELEMENT_NAME.equals(element.getName())) {
      reportActionError(pluginId, "unexpected name of element \"" + element.getName() + "\"");
      return;
    }

    // parent group
    final AnAction parentGroup = getParentGroup(element.getAttributeValue(GROUPID_ATTR_NAME), actionName, pluginId);
    if (parentGroup == null) {
      return;
    }

    // anchor attribute
    final Anchor anchor = parseAnchor(element.getAttributeValue(ANCHOR_ELEMENT_NAME), actionName, pluginId);
    if (anchor == null) {
      return;
    }

    final String relativeToActionId = element.getAttributeValue(RELATIVE_TO_ACTION_ATTR_NAME);
    if (!checkRelativeToAction(relativeToActionId, anchor, actionName, pluginId)) {
      return;
    }
    final DefaultActionGroup group = (DefaultActionGroup)parentGroup;
    group.addAction(action, new Constraints(anchor, relativeToActionId), this).setAsSecondary(secondary);
  }

  public static boolean checkRelativeToAction(final String relativeToActionId, @Nonnull final Anchor anchor, @Nonnull final String actionName, @Nullable final PluginId pluginId) {
    if ((Anchor.BEFORE == anchor || Anchor.AFTER == anchor) && relativeToActionId == null) {
      reportActionError(pluginId, actionName + ": \"relative-to-action\" cannot be null if anchor is \"after\" or \"before\"");
      return false;
    }
    return true;
  }

  @Nullable
  public static Anchor parseAnchor(final String anchorStr, @Nullable final String actionName, @Nullable final PluginId pluginId) {
    if (anchorStr == null) {
      return Anchor.LAST;
    }

    if (FIRST.equalsIgnoreCase(anchorStr)) {
      return Anchor.FIRST;
    }
    else if (LAST.equalsIgnoreCase(anchorStr)) {
      return Anchor.LAST;
    }
    else if (BEFORE.equalsIgnoreCase(anchorStr)) {
      return Anchor.BEFORE;
    }
    else if (AFTER.equalsIgnoreCase(anchorStr)) {
      return Anchor.AFTER;
    }
    else {
      reportActionError(pluginId, actionName + ": anchor should be one of the following constants: \"first\", \"last\", \"before\" or \"after\"");
      return null;
    }
  }

  @Nullable
  public AnAction getParentGroup(final String groupId, @Nullable final String actionName, @Nullable final PluginId pluginId) {
    if (groupId == null || groupId.length() == 0) {
      reportActionError(pluginId, actionName + ": attribute \"group-id\" should be defined");
      return null;
    }
    AnAction parentGroup = getActionImpl(groupId, true);
    if (parentGroup == null) {
      reportActionError(pluginId, actionName + ": group with id \"" + groupId + "\" isn't registered");
      return null;
    }

    if (!(parentGroup instanceof DefaultActionGroup)) {
      reportActionError(pluginId, actionName + ": group with id \"" + groupId + "\" should be instance of " + DefaultActionGroup.class.getName() + " but was " + parentGroup.getClass());
      return null;
    }
    return parentGroup;
  }

  /**
   * @param parentGroup group which is the parent of the separator. It can be <code>null</code> in that
   *                    case separator will be added to group described in the <add-to-group ....> subelement.
   * @param element     XML element which represent separator.
   */
  private void processSeparatorNode(@Nullable DefaultActionGroup parentGroup, SimpleXmlElement element, PluginId pluginId) {
    if (!SEPARATOR_ELEMENT_NAME.equals(element.getName())) {
      reportActionError(pluginId, "unexpected name of element \"" + element.getName() + "\"");
      return;
    }
    AnSeparator separator = AnSeparator.getInstance();
    if (parentGroup != null) {
      parentGroup.add(separator, this);
    }
    // try to find inner <add-to-parent...> tag
    for (final SimpleXmlElement child : element.getChildren()) {
      if (ADD_TO_GROUP_ELEMENT_NAME.equals(child.getName())) {
        processAddToGroupNode(separator, child, pluginId, isSecondary(child));
      }
    }
  }

  private void processKeyboardShortcutNode(SimpleXmlElement element, String actionId, PluginId pluginId) {
    String firstStrokeString = element.getAttributeValue(FIRST_KEYSTROKE_ATTR_NAME);
    if (firstStrokeString == null) {
      reportActionError(pluginId, "\"first-keystroke\" attribute must be specified for action with id=" + actionId);
      return;
    }
    KeyStroke firstKeyStroke = getKeyStroke(firstStrokeString);
    if (firstKeyStroke == null) {
      reportActionError(pluginId, "\"first-keystroke\" attribute has invalid value for action with id=" + actionId);
      return;
    }

    KeyStroke secondKeyStroke = null;
    String secondStrokeString = element.getAttributeValue(SECOND_KEYSTROKE_ATTR_NAME);
    if (secondStrokeString != null) {
      secondKeyStroke = getKeyStroke(secondStrokeString);
      if (secondKeyStroke == null) {
        reportActionError(pluginId, "\"second-keystroke\" attribute has invalid value for action with id=" + actionId);
        return;
      }
    }

    String keymapName = element.getAttributeValue(KEYMAP_ATTR_NAME);
    if (keymapName == null || keymapName.trim().length() == 0) {
      reportActionError(pluginId, "attribute \"keymap\" should be defined");
      return;
    }
    Keymap keymap = myKeymapManager.getKeymap(keymapName);
    if (keymap == null) {
      reportActionError(pluginId, "keymap \"" + keymapName + "\" not found");
      return;
    }
    final String removeOption = element.getAttributeValue(REMOVE_SHORTCUT_ATTR_NAME);
    final KeyboardShortcut shortcut = new KeyboardShortcut(firstKeyStroke, secondKeyStroke);
    final String replaceOption = element.getAttributeValue(REPLACE_SHORTCUT_ATTR_NAME);
    if (Boolean.valueOf(removeOption)) {
      keymap.removeShortcut(actionId, shortcut);
    }
    if (Boolean.valueOf(replaceOption)) {
      keymap.removeAllActionShortcuts(actionId);
    }
    if (!Boolean.valueOf(removeOption)) {
      keymap.addShortcut(actionId, shortcut);
    }
  }

  private static void processMouseShortcutNode(SimpleXmlElement element, String actionId, PluginId pluginId) {
    String keystrokeString = element.getAttributeValue(KEYSTROKE_ATTR_NAME);
    if (keystrokeString == null || keystrokeString.trim().length() == 0) {
      reportActionError(pluginId, "\"keystroke\" attribute must be specified for action with id=" + actionId);
      return;
    }
    MouseShortcut shortcut;
    try {
      shortcut = KeymapUtil.parseMouseShortcut(keystrokeString);
    }
    catch (Exception ex) {
      reportActionError(pluginId, "\"keystroke\" attribute has invalid value for action with id=" + actionId);
      return;
    }

    String keymapName = element.getAttributeValue(KEYMAP_ATTR_NAME);
    if (keymapName == null || keymapName.length() == 0) {
      reportActionError(pluginId, "attribute \"keymap\" should be defined");
      return;
    }
    Keymap keymap = KeymapManager.getInstance().getKeymap(keymapName);
    if (keymap == null) {
      reportActionError(pluginId, "keymap \"" + keymapName + "\" not found");
      return;
    }

    final String removeOption = element.getAttributeValue(REMOVE_SHORTCUT_ATTR_NAME);
    if (Boolean.valueOf(removeOption)) {
      keymap.removeShortcut(actionId, shortcut);
    }
    else {
      keymap.addShortcut(actionId, shortcut);
    }
  }

  @Nullable
  private AnAction processReferenceElement(SimpleXmlElement element, PluginId pluginId) {
    if (!REFERENCE_ELEMENT_NAME.equals(element.getName())) {
      reportActionError(pluginId, "unexpected name of element \"" + element.getName() + "\"");
      return null;
    }
    String ref = element.getAttributeValue(REF_ATTR_NAME);

    if (ref == null) {
      // support old style references by id
      ref = element.getAttributeValue(ID_ATTR_NAME);
    }

    if (ref == null || ref.length() == 0) {
      reportActionError(pluginId, "ID of reference element should be defined");
      return null;
    }

    AnAction action = getActionImpl(ref, true);

    if (action == null) {
      if (!myNotRegisteredInternalActionIds.contains(ref)) {
        reportActionError(pluginId, "action specified by reference isn't registered (ID=" + ref + ")");
      }
      return null;
    }
    assertActionIsGroupOrStub(action);
    return action;
  }

  private void processActionsChildElement(final ClassLoader loader, final PluginId pluginId, final SimpleXmlElement child) {
    String name = child.getName();
    if (ACTION_ELEMENT_NAME.equals(name)) {
      AnAction action = processActionElement(child, loader, pluginId);
      if (action != null) {
        assertActionIsGroupOrStub(action);
      }
    }
    else if (GROUP_ELEMENT_NAME.equals(name)) {
      processGroupElement(child, loader, pluginId);
    }
    else if (SEPARATOR_ELEMENT_NAME.equals(name)) {
      processSeparatorNode(null, child, pluginId);
    }
    else if (REFERENCE_ELEMENT_NAME.equals(name)) {
      processReferenceNode(child, pluginId);
    }
    else {
      reportActionError(pluginId, "unexpected name of element \"" + name + "\n");
    }
  }

  private static void assertActionIsGroupOrStub(final AnAction action) {
    if (!(action instanceof ActionGroup || action instanceof ActionStub)) {
      LOG.error("Action : " + action + "; class: " + action.getClass());
    }
  }

  @Override
  public void registerAction(@Nonnull String actionId, @Nonnull AnAction action, @Nullable PluginId pluginId) {
    synchronized (myLock) {
      if (myId2Action.containsKey(actionId)) {
        reportActionError(pluginId, "action with the ID \"" +
                                    actionId +
                                    "\" was already registered. Action being registered is " +
                                    action.toString() +
                                    "; Registered action is " +
                                    myId2Action.get(actionId) +
                                    getPluginInfo(pluginId));
        return;
      }
      if (myAction2Id.containsKey(action)) {
        reportActionError(pluginId, "action was already registered for another ID. ID is " + myAction2Id.get(action) + getPluginInfo(pluginId));
        return;
      }
      myId2Action.put(actionId, action);
      myId2Index.put(actionId, myRegisteredActionsCount++);
      myAction2Id.put(action, actionId);
      if (pluginId != null && !(action instanceof ActionGroup)) {
        THashSet<String> pluginActionIds = myPlugin2Id.get(pluginId);
        if (pluginActionIds == null) {
          pluginActionIds = new THashSet<>();
          myPlugin2Id.put(pluginId, pluginActionIds);
        }
        pluginActionIds.add(actionId);
      }
      action.registerCustomShortcutSet(new ProxyShortcutSet(actionId, myKeymapManager), null);
    }
  }

  private static void reportActionError(final PluginId pluginId, @NonNls final String message) {
    if (pluginId == null) {
      LOG.error(message);
    }
    else {
      LOG.error(new PluginException(message, null, pluginId));
    }
  }

  @NonNls
  private static String getPluginInfo(@Nullable PluginId id) {
    if (id != null) {
      final PluginDescriptor plugin = consulo.container.plugin.PluginManager.findPlugin(id);
      if (plugin != null) {
        String name = plugin.getName();
        if (name == null) {
          name = id.getIdString();
        }
        return " Plugin: " + name;
      }
    }
    return "";
  }

  @Override
  public void registerAction(@Nonnull String actionId, @Nonnull AnAction action) {
    registerAction(actionId, action, null);
  }

  @Override
  public AnAction unregisterActionEx(@Nonnull String actionId) {
    synchronized (myLock) {
      if (!myId2Action.containsKey(actionId)) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("action with ID " + actionId + " wasn't registered");
          return null;
        }
      }
      AnAction oldValue = myId2Action.remove(actionId);
      myAction2Id.remove(oldValue);
      myId2Index.remove(actionId);
      for (PluginId pluginName : myPlugin2Id.keySet()) {
        final THashSet<String> pluginActions = myPlugin2Id.get(pluginName);
        if (pluginActions != null) {
          pluginActions.remove(actionId);
        }
      }
      return oldValue;
    }
  }

  @Override
  public Comparator<String> getRegistrationOrderComparator() {
    return new Comparator<String>() {
      @Override
      public int compare(String id1, String id2) {
        return myId2Index.get(id1) - myId2Index.get(id2);
      }
    };
  }

  @Override
  public String[] getPluginActions(PluginId pluginName) {
    if (myPlugin2Id.containsKey(pluginName)) {
      final THashSet<String> pluginActions = myPlugin2Id.get(pluginName);
      return ArrayUtil.toStringArray(pluginActions);
    }
    return ArrayUtil.EMPTY_STRING_ARRAY;
  }

  public void addActionPopup(final ActionPopupMenuImpl menu) {
    myPopups.add(menu);
  }

  public void removeActionPopup(final ActionPopupMenuImpl menu) {
    final boolean removed = myPopups.remove(menu);
    //if (removed && menu instanceof ActionPopupMenu) {
    //  for (ActionPopupMenuListener listener : myActionPopupMenuListeners) {
    //    listener.actionPopupMenuReleased((ActionPopupMenu)menu);
    //  }
    //}
  }

  @Override
  public void queueActionPerformedEvent(final AnAction action, DataContext context, AnActionEvent event) {
    if (myPopups.isEmpty()) {
      fireAfterActionPerformed(action, context, event);
    }
  }

  @Override
  public boolean isActionPopupStackEmpty() {
    return myPopups.isEmpty();
  }

  @Override
  public boolean isTransparentOnlyActionsUpdateNow() {
    return myTransparentOnlyUpdate;
  }

  @Override
  public void addAnActionListener(AnActionListener listener) {
    myActionListeners.add(listener);
  }

  @Override
  public void removeAnActionListener(AnActionListener listener) {
    myActionListeners.remove(listener);
  }

  @Override
  public void fireBeforeActionPerformed(@Nonnull AnAction action, @Nonnull DataContext dataContext, @Nonnull AnActionEvent event) {
    myPrevPerformedActionId = myLastPreformedActionId;
    myLastPreformedActionId = getId(action);

    LastActionTracker.ourLastActionId = myLastPreformedActionId;

    for (AnActionListener listener : myActionListeners) {
      listener.beforeActionPerformed(action, dataContext, event);
    }

    myMessageBusPublisher.beforeActionPerformed(action, dataContext, event);
  }

  @Override
  public void fireAfterActionPerformed(@Nonnull AnAction action, @Nonnull DataContext dataContext, @Nonnull AnActionEvent event) {
    myPrevPerformedActionId = myLastPreformedActionId;
    myLastPreformedActionId = getId(action);

    LastActionTracker.ourLastActionId = myLastPreformedActionId;

    for (AnActionListener listener : myActionListeners) {
      try {
        listener.afterActionPerformed(action, dataContext, event);
      }
      catch (AbstractMethodError ignored) {
      }
    }

    myMessageBusPublisher.afterActionPerformed(action, dataContext, event);
  }

  @Override
  public KeyboardShortcut getKeyboardShortcut(@Nonnull String actionId) {
    AnAction action = ActionManager.getInstance().getAction(actionId);
    final ShortcutSet shortcutSet = action.getShortcutSet();
    final Shortcut[] shortcuts = shortcutSet.getShortcuts();
    for (final Shortcut shortcut : shortcuts) {
      // Shortcut can be MouseShortcut here.
      // For example IdeaVIM often assigns them
      if (shortcut instanceof KeyboardShortcut) {
        final KeyboardShortcut kb = (KeyboardShortcut)shortcut;
        if (kb.getSecondKeyStroke() == null) {
          return (KeyboardShortcut)shortcut;
        }
      }
    }

    return null;
  }

  @Override
  public void fireBeforeEditorTyping(char c, DataContext dataContext) {
    myLastTimeEditorWasTypedIn = System.currentTimeMillis();
    for (AnActionListener listener : myActionListeners) {
      listener.beforeEditorTyping(c, dataContext);
    }
    myMessageBusPublisher.beforeEditorTyping(c, dataContext);
  }

  @Override
  public String getLastPreformedActionId() {
    return myLastPreformedActionId;
  }

  @Override
  public String getPrevPreformedActionId() {
    return myPrevPerformedActionId;
  }

  public Set<String> getActionIds() {
    synchronized (myLock) {
      return new HashSet<>(myId2Action.keySet());
    }
  }

  public void preloadActions(ProgressIndicator indicator) {
    final Application application = myApplication;

    for (String id : getActionIds()) {
      indicator.checkCanceled();
      if (application.isDisposed()) return;

      final AnAction action = getAction(id);
      if (action instanceof PreloadableAction) {
        ((PreloadableAction)action).preload();
      }
      // don't preload ActionGroup.getChildren() because that would unstub child actions
      // and make it impossible to replace the corresponding actions later
      // (via unregisterAction+registerAction, as some app components do)
    }
  }

  private class MyTimer extends Timer implements ActionListener {
    private final List<TimerListener> myTimerListeners = Collections.synchronizedList(new ArrayList<TimerListener>());
    private final List<TimerListener> myTransparentTimerListeners = Collections.synchronizedList(new ArrayList<TimerListener>());
    private int myLastTimePerformed;

    MyTimer() {
      super(TIMER_DELAY, null);
      addActionListener(this);
      setRepeats(true);
      final MessageBusConnection connection = myApplication.getMessageBus().connect();
      connection.subscribe(ApplicationActivationListener.TOPIC, new ApplicationActivationListener.Adapter() {
        @Override
        public void applicationActivated(IdeFrame ideFrame) {
          setDelay(TIMER_DELAY);
          restart();
        }

        @Override
        public void applicationDeactivated(IdeFrame ideFrame) {
          setDelay(DEACTIVATED_TIMER_DELAY);
        }
      });
    }

    @Override
    public String toString() {
      return "Action manager timer";
    }

    public void addTimerListener(TimerListener listener, boolean transparent) {
      if (transparent) {
        myTransparentTimerListeners.add(listener);
      }
      else {
        myTimerListeners.add(listener);
      }
    }

    public void removeTimerListener(TimerListener listener, boolean transparent) {
      if (transparent) {
        myTransparentTimerListeners.remove(listener);
      }
      else {
        myTimerListeners.remove(listener);
      }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (myLastTimeEditorWasTypedIn + UPDATE_DELAY_AFTER_TYPING > System.currentTimeMillis()) {
        return;
      }

      final int lastEventCount = myLastTimePerformed;
      myLastTimePerformed = ActivityTracker.getInstance().getCount();

      boolean transparentOnly = myLastTimePerformed == lastEventCount;

      try {
        HashSet<TimerListener> notified = new HashSet<>();
        myTransparentOnlyUpdate = transparentOnly;
        notifyListeners(myTransparentTimerListeners, notified);

        if (transparentOnly) {
          return;
        }

        notifyListeners(myTimerListeners, notified);
      }
      finally {
        myTransparentOnlyUpdate = false;
      }
    }

    private void notifyListeners(final List<TimerListener> timerListeners, final Set<TimerListener> notified) {
      final TimerListener[] listeners = timerListeners.toArray(new TimerListener[timerListeners.size()]);
      for (TimerListener listener : listeners) {
        if (timerListeners.contains(listener)) {
          if (!notified.contains(listener)) {
            notified.add(listener);
            runListenerAction(listener);
          }
        }
      }
    }

    private void runListenerAction(final TimerListener listener) {
      ModalityState modalityState = listener.getModalityState();
      if (modalityState == null) return;
      if (!ModalityState.current().dominates(modalityState)) {
        try {
          listener.run();
        }
        catch (ProcessCanceledException ex) {
          // ignore
        }
        catch (Throwable e) {
          LOG.error(e);
        }
      }
    }
  }

  @Override
  public ActionCallback tryToExecute(@Nonnull final AnAction action, @Nonnull final InputEvent inputEvent, @Nullable final Component contextComponent, @Nullable final String place, boolean now) {

    final Application app = myApplication;
    assert app.isDispatchThread();

    final AsyncResult<Void> result = new AsyncResult<>();
    final Runnable doRunnable = new Runnable() {
      @Override
      public void run() {
        tryToExecuteNow(action, inputEvent, contextComponent, place, result);
      }
    };

    if (now) {
      doRunnable.run();
    }
    else {
      //noinspection SSBasedInspection
      SwingUtilities.invokeLater(doRunnable);
    }

    return result;

  }

  private void tryToExecuteNow(final AnAction action, final InputEvent inputEvent, final Component contextComponent, final String place, final AsyncResult<Void> result) {
    final Presentation presentation = action.getTemplatePresentation().clone();

    IdeFocusManager.findInstanceByContext(getContextBy(contextComponent)).doWhenFocusSettlesDown(() -> ((TransactionGuardEx)TransactionGuard.getInstance()).performUserActivity(() -> {
      final DataContext context = getContextBy(contextComponent);

      AnActionEvent event = new AnActionEvent(inputEvent, context, place != null ? place : ActionPlaces.UNKNOWN, presentation, this, inputEvent.getModifiersEx());

      ActionUtil.performDumbAwareUpdate(LaterInvocator.isInModalContext(), action, event, false);
      if (!event.getPresentation().isEnabled()) {
        result.setRejected();
        return;
      }

      ActionUtil.lastUpdateAndCheckDumb(action, event, false);
      if (!event.getPresentation().isEnabled()) {
        result.setRejected();
        return;
      }

      Component component = context.getData(PlatformDataKeys.CONTEXT_COMPONENT);
      if (component != null && !component.isShowing()) {
        result.setRejected();
        return;
      }

      fireBeforeActionPerformed(action, context, event);

      Disposable eventListenerDisposable = Disposer.newDisposable("tryToExecuteNow");
      result.doWhenProcessed(() -> Disposer.dispose(eventListenerDisposable));

      UIUtil.addAwtListener(e -> {
        if (e.getID() == WindowEvent.WINDOW_OPENED || e.getID() == WindowEvent.WINDOW_ACTIVATED) {
          if (!result.isProcessed()) {
            final WindowEvent we = (WindowEvent)e;
            IdeFocusManager.findInstanceByComponent(we.getWindow()).doWhenFocusSettlesDown(result.createSetDoneRunnable());
          }
        }
      }, AWTEvent.WINDOW_EVENT_MASK, eventListenerDisposable);

      ActionUtil.performActionDumbAware(action, event);
      result.setDone();
      queueActionPerformedEvent(action, context, event);
    }));
  }

  private static DataContext getContextBy(Component contextComponent) {
    final DataManager dataManager = DataManager.getInstance();
    return contextComponent != null ? dataManager.getDataContext(contextComponent) : dataManager.getDataContext();
  }
}
