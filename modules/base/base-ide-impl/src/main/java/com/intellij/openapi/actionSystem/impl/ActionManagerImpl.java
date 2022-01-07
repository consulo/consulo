// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.actionSystem.impl;

import com.intellij.ide.ActivityTracker;
import com.intellij.ide.DataManager;
import com.intellij.internal.statistic.collectors.fus.actions.persistence.ActionIdProvider;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.actionSystem.ex.ActionPopupMenuListener;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.openapi.application.*;
import com.intellij.openapi.application.impl.LaterInvocator;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.keymap.ex.KeymapManagerEx;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.psi.PsiFile;
import com.intellij.util.ObjectUtils;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.UIUtil;
import consulo.actionSystem.ActionToolbarFactory;
import consulo.actionSystem.impl.LocalizeHelper;
import consulo.actionSystem.impl.UnifiedActionPopupMenuImpl;
import consulo.application.TransactionGuardEx;
import consulo.container.PluginException;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginManager;
import consulo.container.util.StatCollector;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.extensions.ListOfElementsEP;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.impl.action.LastActionTracker;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.ui.image.ImageKey;
import consulo.ui.style.StandardColors;
import consulo.util.collection.ArrayUtil;
import consulo.util.nodep.xml.node.SimpleXmlElement;
import gnu.trove.TObjectIntHashMap;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.Timer;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.*;

public final class ActionManagerImpl extends ActionManagerEx implements Disposable {
  private static final String ACTION_ELEMENT_NAME = "action";
  private static final String GROUP_ELEMENT_NAME = "group";
  private static final String CLASS_ATTR_NAME = "class";
  private static final String ID_ATTR_NAME = "id";
  private static final String INTERNAL_ATTR_NAME = "internal";
  private static final String ICON_ATTR_NAME = "icon";
  private static final String ADD_TO_GROUP_ELEMENT_NAME = "add-to-group";
  private static final String SHORTCUT_ELEMENT_NAME = "keyboard-shortcut";
  private static final String MOUSE_SHORTCUT_ELEMENT_NAME = "mouse-shortcut";
  private static final String DESCRIPTION = "description";
  private static final String TEXT_ATTR_NAME = "text";
  private static final String POPUP_ATTR_NAME = "popup";
  private static final String COMPACT_ATTR_NAME = "compact";
  private static final String SEPARATOR_ELEMENT_NAME = "separator";
  private static final String REFERENCE_ELEMENT_NAME = "reference";
  private static final String ABBREVIATION_ELEMENT_NAME = "abbreviation";
  private static final String GROUPID_ATTR_NAME = "group-id";
  private static final String ANCHOR_ELEMENT_NAME = "anchor";
  private static final String FIRST = "first";
  private static final String LAST = "last";
  private static final String BEFORE = "before";
  private static final String AFTER = "after";
  private static final String SECONDARY = "secondary";
  private static final String RELATIVE_TO_ACTION_ATTR_NAME = "relative-to-action";
  private static final String FIRST_KEYSTROKE_ATTR_NAME = "first-keystroke";
  private static final String SECOND_KEYSTROKE_ATTR_NAME = "second-keystroke";
  private static final String REMOVE_SHORTCUT_ATTR_NAME = "remove";
  private static final String REPLACE_SHORTCUT_ATTR_NAME = "replace-all";
  private static final String KEYMAP_ATTR_NAME = "keymap";
  private static final String KEYSTROKE_ATTR_NAME = "keystroke";
  private static final String REF_ATTR_NAME = "ref";
  private static final String VALUE_ATTR_NAME = "value";
  private static final String USE_SHORTCUT_OF_ATTR_NAME = "use-shortcut-of";
  private static final String OVERRIDES_ATTR_NAME = "overrides";
  private static final String KEEP_CONTENT_ATTR_NAME = "keep-content";
  private static final String REQUIRE_MODULE_EXTENSIONS = "require-module-extensions";
  private static final String CAN_USE_PROJECT_AS_DEFAULT = "can-use-project-as-default";

  @Deprecated
  private static final String PROJECT_TYPE = "project-type";
  private static final String UNREGISTER_ELEMENT_NAME = "unregister";

  private static final Logger LOG = Logger.getInstance(ActionManagerImpl.class);
  private static final int DEACTIVATED_TIMER_DELAY = 5000;
  private static final int TIMER_DELAY = 500;
  private static final int UPDATE_DELAY_AFTER_TYPING = 500;

  private final Object myLock = new Object();
  private final Map<String, AnAction> myId2Action = new HashMap<>();
  private final MultiMap<PluginId, String> myPlugin2Id = new MultiMap<>();
  private final TObjectIntHashMap<String> myId2Index = new TObjectIntHashMap<>();
  private final Map<Object, String> myAction2Id = new HashMap<>();
  private final MultiMap<String, String> myId2GroupId = new MultiMap<>();
  private final List<String> myNotRegisteredInternalActionIds = new ArrayList<>();
  private final List<AnActionListener> myActionListeners = ContainerUtil.createLockFreeCopyOnWriteList();
  private final List<ActionPopupMenuListener> myActionPopupMenuListeners = ContainerUtil.createLockFreeCopyOnWriteList();
  private final List<Object/*ActionPopupMenuImpl|JBPopup*/> myPopups = new ArrayList<>();
  private MyTimer myTimer;
  private int myRegisteredActionsCount;
  private String myLastPreformedActionId;
  private String myPrevPerformedActionId;
  private long myLastTimeEditorWasTypedIn;
  private boolean myTransparentOnlyUpdate;
  private final Map<OverridingAction, AnAction> myBaseActions = new HashMap<>();
  private int myAnonymousGroupIdCounter;

  private ActionToolbarFactory myToolbarFactory;

  @Inject
  ActionManagerImpl(ActionToolbarFactory toolbarFactory) {
    myToolbarFactory = toolbarFactory;

    StatCollector collector = new StatCollector();

    for (PluginDescriptor plugin : PluginManager.getEnabledPlugins()) {
      collector.markWith(plugin.getPluginId().toString(), () -> {
        LocalizeHelper localizeHelper = LocalizeHelper.build(plugin);

        registerPluginActions(plugin, localizeHelper);
      });
    }

    collector.dump("ActionManager statistics", LOG::info);
  }

  @Nonnull
  private static AnActionListener publisher() {
    return ApplicationManager.getApplication().getMessageBus().syncPublisher(AnActionListener.TOPIC);
  }

  @Nullable
  static AnAction convertStub(@Nonnull ActionStub stub) {
    AnAction anAction = instantiate(stub.getClassName(), stub.getLoader(), stub.getPluginId(), AnAction.class);
    if (anAction == null) return null;

    stub.initAction(anAction);
    updateIconFromStub(stub, anAction);
    return anAction;
  }

  @Nullable
  @SuppressWarnings("unchecked")
  private static <T> T instantiate(String stubClassName, ClassLoader classLoader, PluginId pluginId, Class<T> expectedClass) {
    Object obj;
    try {
      Class<?> actionClass = Class.forName(stubClassName, true, classLoader);
      obj = Application.get().getInjectingContainer().getUnbindedInstance(actionClass);
    }
    catch (ProcessCanceledException e) {
      throw e;
    }
    catch (Throwable e) {
      LOG.error(new PluginException(e, pluginId));
      return null;
    }

    if (!expectedClass.isInstance(obj)) {
      LOG.error(new PluginException("class with name '" + stubClassName + "' must be an instance of '" + expectedClass.getName() + "'; got " + obj, pluginId));
      return null;
    }
    //noinspection unchecked
    return (T)obj;
  }

  private static void updateIconFromStub(@Nonnull ActionStubBase stub, AnAction anAction) {
    String iconPath = stub.getIconPath();
    if (iconPath != null) {
      ImageKey imageKey = ImageKey.fromString(iconPath, Image.DEFAULT_ICON_SIZE, Image.DEFAULT_ICON_SIZE);
      if(imageKey != null) {
        anAction.getTemplatePresentation().setIcon(imageKey);
      }
      else {
        LOG.warn("Wrong icon path: " + iconPath);
        anAction.getTemplatePresentation().setIcon(ImageEffects.colorFilled(Image.DEFAULT_ICON_SIZE, Image.DEFAULT_ICON_SIZE, StandardColors.MAGENTA));
      }
    }
  }

  @Nullable
  static ActionGroup convertGroupStub(@Nonnull ActionGroupStub stub, @Nonnull ActionManager actionManager) {
    ActionGroup group = instantiate(stub.getActionClass(), stub.getClassLoader(), stub.getPluginId(), ActionGroup.class);
    if (group == null) return null;
    stub.initGroup(group, actionManager);
    updateIconFromStub(stub, group);
    return group;
  }

  private static void processAbbreviationNode(@Nonnull SimpleXmlElement e, @Nonnull String id) {
    final String abbr = e.getAttributeValue(VALUE_ATTR_NAME);
    if (!StringUtil.isEmpty(abbr)) {
      final AbbreviationManagerImpl abbreviationManager = (AbbreviationManagerImpl)AbbreviationManager.getInstance();
      abbreviationManager.register(abbr, id, true);
    }
  }

  private static void processModuleExtensionOptions(SimpleXmlElement element, AnAction action) {
    String canUseProjectAsDefaultText = element.getAttributeValue(CAN_USE_PROJECT_AS_DEFAULT);
    boolean canUseProjectAsDefault = !StringUtil.isEmpty(canUseProjectAsDefaultText) && Boolean.parseBoolean(canUseProjectAsDefaultText);

    action.setCanUseProjectAsDefault(canUseProjectAsDefault);

    String requestModuleExtensionsValue = element.getAttributeValue(REQUIRE_MODULE_EXTENSIONS);
    action.setModuleExtensionIds(ListOfElementsEP.getValuesOfVariableIfFound(requestModuleExtensionsValue));
  }

  private static boolean isSecondary(SimpleXmlElement element) {
    return "true".equalsIgnoreCase(element.getAttributeValue(SECONDARY));
  }

  @Nonnull
  private static LocalizeValue computeDescription(LocalizeHelper localizeHelper, String id, String elementType, String descriptionValue) {
    if(!StringUtil.isEmpty(descriptionValue)) {
      return LocalizeValue.of(descriptionValue);
    }

    final String key = elementType + "." + id + ".description";
    return localizeHelper.getValue(key);
  }

  @Nonnull
  private static LocalizeValue computeActionText(LocalizeHelper localizeHelper, String id, String elementType, String textValue) {
    if(!StringUtil.isEmptyOrSpaces(textValue)) {
      return LocalizeValue.of(textValue);
    }
    String key = elementType + "." + id + "." + TEXT_ATTR_NAME;
    return localizeHelper.getValue(key);
  }

  private static boolean checkRelativeToAction(final String relativeToActionId, @Nonnull final Anchor anchor, @Nonnull final String actionName, @Nullable final PluginId pluginId) {
    if ((Anchor.BEFORE == anchor || Anchor.AFTER == anchor) && relativeToActionId == null) {
      reportActionError(pluginId, actionName + ": \"relative-to-action\" cannot be null if anchor is \"after\" or \"before\"");
      return false;
    }
    return true;
  }

  @Nullable
  private static Anchor parseAnchor(final String anchorStr, @Nullable final String actionName, @Nullable final PluginId pluginId) {
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

  private static void processMouseShortcutNode(SimpleXmlElement element, String actionId, PluginId pluginId, @Nonnull KeymapManager keymapManager) {
    String keystrokeString = element.getAttributeValue(KEYSTROKE_ATTR_NAME);
    if (keystrokeString == null || keystrokeString.trim().isEmpty()) {
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
    if (keymapName == null || keymapName.isEmpty()) {
      reportActionError(pluginId, "attribute \"keymap\" should be defined");
      return;
    }
    Keymap keymap = keymapManager.getKeymap(keymapName);
    if (keymap == null) {
      reportKeymapNotFoundWarning(pluginId, keymapName);
      return;
    }
    processRemoveAndReplace(element, actionId, keymap, shortcut);
  }

  private void assertActionIsGroupOrStub(final AnAction action) {
    if (!(action instanceof ActionGroup || action instanceof ActionStub)) {
      LOG.error("Action : " + action + "; class: " + action.getClass());
    }
  }

  private static void reportActionError(@Nullable PluginId pluginId, @Nonnull String message) {
    reportActionError(pluginId, message, null);
  }

  private static void reportActionError(@Nullable PluginId pluginId, @Nonnull String message, @Nullable Throwable cause) {
    if (pluginId != null) {
      LOG.error(new PluginException(message, cause, pluginId));
    }
    else if (cause != null) {
      LOG.error(message, cause);
    }
    else {
      LOG.error(message);
    }
  }

  private static void reportKeymapNotFoundWarning(@Nullable PluginId pluginId, @Nonnull String keymapName) {
    //if (DefaultBundledKeymaps.isBundledKeymapHidden(keymapName)) return;
    String message = "keymap \"" + keymapName + "\" not found";
    LOG.warn(pluginId == null ? message : new PluginException(message, null, pluginId).getMessage());
  }

  private static String getPluginInfo(@Nullable PluginId id) {
    if (id != null) {
      final PluginDescriptor plugin = PluginManager.findPlugin(id);
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

  @Nonnull
  private static DataContext getContextBy(Component contextComponent) {
    final DataManager dataManager = DataManager.getInstance();
    return contextComponent != null ? dataManager.getDataContext(contextComponent) : dataManager.getDataContext();
  }

  @Override
  public void dispose() {
    if (myTimer != null) {
      myTimer.stop();
      myTimer = null;
    }
  }

  @Override
  public void addTimerListener(int delay, @Nonnull final TimerListener listener) {
    _addTimerListener(listener, false);
  }

  @Override
  public void removeTimerListener(@Nonnull TimerListener listener) {
    _removeTimerListener(listener, false);
  }

  @Override
  public void addTransparentTimerListener(int delay, @Nonnull TimerListener listener) {
    _addTimerListener(listener, true);
  }

  @Override
  public void removeTransparentTimerListener(@Nonnull TimerListener listener) {
    _removeTimerListener(listener, true);
  }

  private void _addTimerListener(final TimerListener listener, boolean transparent) {
    if (ApplicationManager.getApplication().isUnitTestMode()) return;
    if (myTimer == null) {
      myTimer = new MyTimer();
      myTimer.start();
    }

    myTimer.addTimerListener(listener, transparent);
  }

  private void _removeTimerListener(TimerListener listener, boolean transparent) {
    if (ApplicationManager.getApplication().isUnitTestMode()) return;
    if (LOG.assertTrue(myTimer != null)) {
      myTimer.removeTimerListener(listener, transparent);
    }
  }

  @Nonnull
  public ActionPopupMenu createActionPopupMenu(@Nonnull String place, @Nonnull ActionGroup group, @Nullable PresentationFactory presentationFactory) {
    if(Application.get().isUnifiedApplication()) {
      return new UnifiedActionPopupMenuImpl(place, group, this, presentationFactory);
    }
    return new DesktopActionPopupMenuImpl(place, group, this, presentationFactory);
  }

  @Nonnull
  @Override
  public ActionPopupMenu createActionPopupMenu(@Nonnull String place, @Nonnull ActionGroup group) {
    if (Application.get().isUnifiedApplication()) {
      return new UnifiedActionPopupMenuImpl(place, group, this, null);
    }
    return new DesktopActionPopupMenuImpl(place, group, this, null);
  }

  @Nonnull
  @Override
  public ActionToolbar createActionToolbar(@Nonnull final String place, @Nonnull final ActionGroup group, final boolean horizontal) {
    return createActionToolbar(place, group, horizontal, false);
  }

  @Nonnull
  @Override
  public ActionToolbar createActionToolbar(@Nonnull final String place, @Nonnull final ActionGroup group, final boolean horizontal, final boolean decorateButtons) {
    return myToolbarFactory.createActionToolbar(place, group, horizontal, decorateButtons);
  }

  public void registerPluginActions(@Nonnull PluginDescriptor plugin, LocalizeHelper localizeHelper) {
    final List<SimpleXmlElement> elementList = plugin.getActionsDescriptionElements();
    if (elementList != null) {
      //long startTime = StartUpMeasurer.getCurrentTime();
      for (SimpleXmlElement e : elementList) {
        processActionsChildElement(plugin, e, localizeHelper);
      }
      //StartUpMeasurer.addPluginCost(plugin.getPluginId().getIdString(), "Actions", StartUpMeasurer.getCurrentTime() - startTime);
    }
  }

  @Override
  @Nullable
  public AnAction getAction(@Nonnull String id) {
    return getActionImpl(id, false);
  }

  @Nullable
  private AnAction getActionImpl(@Nonnull String id, boolean canReturnStub) {
    AnAction action;
    synchronized (myLock) {
      action = myId2Action.get(id);
      if (canReturnStub || !(action instanceof ActionStubBase)) {
        return action;
      }
    }
    AnAction converted = action instanceof ActionStub ? convertStub((ActionStub)action) : convertGroupStub((ActionGroupStub)action, this);
    if (converted == null) {
      unregisterAction(id);
      return null;
    }

    synchronized (myLock) {
      action = myId2Action.get(id);
      if (action instanceof ActionStubBase) {
        action = replaceStub((ActionStubBase)action, converted);
      }
      return action;
    }
  }

  @Nonnull
  private AnAction replaceStub(@Nonnull ActionStubBase stub, AnAction anAction) {
    LOG.assertTrue(myAction2Id.containsKey(stub));
    myAction2Id.remove(stub);

    LOG.assertTrue(myId2Action.containsKey(stub.getId()));

    AnAction action = myId2Action.remove(stub.getId());
    LOG.assertTrue(action != null);
    LOG.assertTrue(action.equals(stub));

    myAction2Id.put(anAction, stub.getId());

    return addToMap(stub.getId(), anAction);
  }

  @Override
  public String getId(@Nonnull AnAction action) {
    if (action instanceof ActionStubBase) {
      return ((ActionStubBase)action).getId();
    }
    synchronized (myLock) {
      return myAction2Id.get(action);
    }
  }

  @Nonnull
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

  @Nonnull
  @Override
  public JComponent createButtonToolbar(@Nonnull final String actionPlace, @Nonnull final ActionGroup messageActionGroup) {
    return new ButtonToolbarImpl(actionPlace, messageActionGroup);
  }

  @Override
  public AnAction getActionOrStub(@Nonnull String id) {
    return getActionImpl(id, true);
  }

  /**
   * @return instance of ActionGroup or ActionStub. The method never returns real subclasses of {@code AnAction}.
   */
  @Nullable
  private AnAction processActionElement(@Nonnull SimpleXmlElement element, @Nonnull PluginDescriptor plugin, @Nonnull LocalizeHelper localizeHelper) {
    PluginId pluginId = plugin.getPluginId();

    String className = element.getAttributeValue(CLASS_ATTR_NAME);
    if (className == null || className.isEmpty()) {
      reportActionError(pluginId, "action element should have specified \"class\" attribute");
      return null;
    }

    // read ID and register loaded action
    String id = obtainActionId(element, className);
    if (Boolean.valueOf(element.getAttributeValue(INTERNAL_ATTR_NAME))&& !ApplicationManager.getApplication().isInternal()) {
      myNotRegisteredInternalActionIds.add(id);
      return null;
    }

    String iconPath = element.getAttributeValue(ICON_ATTR_NAME);

    String textValue = element.getAttributeValue(TEXT_ATTR_NAME);
    String descriptionValue = element.getAttributeValue(DESCRIPTION);

    ActionStub stub = new ActionStub(className, id, plugin, iconPath, () -> {
      Presentation presentation = new Presentation();
      presentation.setTextValue(computeActionText(localizeHelper, id, ACTION_ELEMENT_NAME, textValue));
      presentation.setDescriptionValue(computeDescription(localizeHelper, id, ACTION_ELEMENT_NAME, descriptionValue));
      return presentation;
    });

    processModuleExtensionOptions(element, stub);

    KeymapManagerEx keymapManager = KeymapManagerEx.getInstanceEx();
    // process all links and key bindings if any
    for (SimpleXmlElement e : element.getChildren()) {
      if (ADD_TO_GROUP_ELEMENT_NAME.equals(e.getName())) {
        processAddToGroupNode(stub, e, pluginId, isSecondary(e));
      }
      else if (SHORTCUT_ELEMENT_NAME.equals(e.getName())) {
        processKeyboardShortcutNode(e, id, pluginId, keymapManager);
      }
      else if (MOUSE_SHORTCUT_ELEMENT_NAME.equals(e.getName())) {
        processMouseShortcutNode(e, id, pluginId, keymapManager);
      }
      else if (ABBREVIATION_ELEMENT_NAME.equals(e.getName())) {
        processAbbreviationNode(e, id);
      }
      else {
        reportActionError(pluginId, "unexpected name of element \"" + e.getName() + "\"");
        return null;
      }
    }
    if (element.getAttributeValue(USE_SHORTCUT_OF_ATTR_NAME) != null) {
      keymapManager.bindShortcuts(element.getAttributeValue(USE_SHORTCUT_OF_ATTR_NAME), id);
    }

    registerOrReplaceActionInner(element, id, stub, pluginId);
    return stub;
  }

  private static String obtainActionId(SimpleXmlElement element, String className) {
    String id = element.getAttributeValue(ID_ATTR_NAME);
    return StringUtil.isEmpty(id) ? StringUtil.getShortName(className) : id;
  }

  private void registerOrReplaceActionInner(@Nonnull SimpleXmlElement element, @Nonnull String id, @Nonnull AnAction action, @Nullable PluginId pluginId) {
    synchronized (myLock) {
      if (Boolean.parseBoolean(element.getAttributeValue(OVERRIDES_ATTR_NAME))) {
        if (getActionOrStub(id) == null) {
          LOG.error(element.getName() + " '" + id + "' doesn't override anything");
          return;
        }
        AnAction prev = replaceAction(id, action, pluginId);
        if (action instanceof DefaultActionGroup && prev instanceof DefaultActionGroup) {
          if (Boolean.parseBoolean(element.getAttributeValue(KEEP_CONTENT_ATTR_NAME))) {
            ((DefaultActionGroup)action).copyFromGroup((DefaultActionGroup)prev);
          }
        }
      }
      else {
        registerAction(id, action, pluginId, element.getAttributeValue(PROJECT_TYPE));
      }
      //ActionsCollectorImpl.onActionLoadedFromXml(action, id, pluginId);
    }
  }

  private AnAction processGroupElement(@Nonnull SimpleXmlElement element, @Nonnull PluginDescriptor plugin, @Nonnull LocalizeHelper localizeHelper) {
    PluginId pluginId = plugin.getPluginId();
    ClassLoader pluginClassLoader = plugin.getPluginClassLoader();

    if (!GROUP_ELEMENT_NAME.equals(element.getName())) {
      reportActionError(pluginId, "unexpected name of element \"" + element.getName() + "\"");
      return null;
    }
    String className = element.getAttributeValue(CLASS_ATTR_NAME);
    if (className == null) { // use default group if class isn't specified
      className = "true".equals(element.getAttributeValue(COMPACT_ATTR_NAME)) ? DefaultCompactActionGroup.class.getName() : DefaultActionGroup.class.getName();
    }
    try {
      String id = element.getAttributeValue(ID_ATTR_NAME);
      if (id != null && id.isEmpty()) {
        reportActionError(pluginId, "ID of the group cannot be an empty string");
        return null;
      }

      ActionGroup group;
      boolean customClass = false;
      if (DefaultActionGroup.class.getName().equals(className)) {
        group = new DefaultActionGroup();
      }
      else if (DefaultCompactActionGroup.class.getName().equals(className)) {
        group = new DefaultCompactActionGroup();
      }
      else if (id == null) {
        Class<?> aClass = Class.forName(className, true, pluginClassLoader);
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
        customClass = true;
        group = (ActionGroup)obj;
      }
      else {
        group = new ActionGroupStub(id, className, plugin);
        customClass = true;
      }
      // read ID and register loaded group
      if (Boolean.valueOf(element.getAttributeValue(INTERNAL_ATTR_NAME)) && !ApplicationManager.getApplication().isInternal()) {
        myNotRegisteredInternalActionIds.add(id);
        return null;
      }

      if (id == null) {
        id = "<anonymous-group-" + myAnonymousGroupIdCounter++ + ">";
      }

      registerOrReplaceActionInner(element, id, group, pluginId);
      Presentation presentation = group.getTemplatePresentation();

      processModuleExtensionOptions(element, group);

      // text
      LocalizeValue textValue = computeActionText(localizeHelper, id, GROUP_ELEMENT_NAME, element.getAttributeValue(TEXT_ATTR_NAME));
      // don't override value which was set in API with empty value from xml descriptor
      if (textValue != LocalizeValue.empty() || presentation.getText() == null) {
        presentation.setTextValue(textValue);
      }

      // description
      LocalizeValue description = computeDescription(localizeHelper, id, GROUP_ELEMENT_NAME, element.getAttributeValue(DESCRIPTION));
      // don't override value which was set in API with empty value from xml descriptor
      if (description != LocalizeValue.empty() || presentation.getDescriptionValue() == LocalizeValue.empty()) {
        presentation.setDescriptionValue(description);
      }

      // icon
      String iconPath = element.getAttributeValue(ICON_ATTR_NAME);
      if (group instanceof ActionGroupStub) {
        ((ActionGroupStub)group).setIconPath(iconPath);
      }
      else if(iconPath != null) {
        if (iconPath.contains("@")) {
          String[] groupIdAndImageId = iconPath.split("@");
          group.getTemplatePresentation().setIcon(ImageKey.of(groupIdAndImageId[0], groupIdAndImageId[1], Image.DEFAULT_ICON_SIZE, Image.DEFAULT_ICON_SIZE));
        }
        else {
          LOG.warn("Wrong icon path: " + iconPath);
          group.getTemplatePresentation().setIcon(ImageEffects.colorFilled(Image.DEFAULT_ICON_SIZE, Image.DEFAULT_ICON_SIZE, StandardColors.MAGENTA));
        }
      }

      // popup
      String popup = element.getAttributeValue(POPUP_ATTR_NAME);
      if (popup != null) {
        group.setPopup(Boolean.valueOf(popup));
        if (group instanceof ActionGroupStub) {
          ((ActionGroupStub)group).setPopupDefinedInXml(true);
        }
      }
      if (customClass && element.getAttributeValue(USE_SHORTCUT_OF_ATTR_NAME) != null) {
        KeymapManagerEx.getInstanceEx().bindShortcuts(element.getAttributeValue(USE_SHORTCUT_OF_ATTR_NAME), id);
      }

      // process all group's children. There are other groups, actions, references and links
      for (SimpleXmlElement child : element.getChildren()) {
        String name = child.getName();
        if (ACTION_ELEMENT_NAME.equals(name)) {
          AnAction action = processActionElement(child, plugin, localizeHelper);
          if (action != null) {
            assertActionIsGroupOrStub(action);
            addToGroupInner(group, action, Constraints.LAST, isSecondary(child));
          }
        }
        else if (SEPARATOR_ELEMENT_NAME.equals(name)) {
          processSeparatorNode((DefaultActionGroup)group, child, pluginId);
        }
        else if (GROUP_ELEMENT_NAME.equals(name)) {
          AnAction action = processGroupElement(child, plugin, localizeHelper);
          if (action != null) {
            addToGroupInner(group, action, Constraints.LAST, false);
          }
        }
        else if (ADD_TO_GROUP_ELEMENT_NAME.equals(name)) {
          processAddToGroupNode(group, child, pluginId, isSecondary(child));
        }
        else if (REFERENCE_ELEMENT_NAME.equals(name)) {
          AnAction action = processReferenceElement(child, pluginId);
          if (action != null) {
            addToGroupInner(group, action, Constraints.LAST, isSecondary(child));
          }
        }
        else {
          reportActionError(pluginId, "unexpected name of element \"" + name + "\n");
          return null;
        }
      }
      return group;
    }
    catch (Exception e) {
      String message = "cannot create class \"" + className + "\"";
      reportActionError(pluginId, message, e);
      return null;
    }
  }

  private void processReferenceNode(final SimpleXmlElement element, final PluginId pluginId) {
    final AnAction action = processReferenceElement(element, pluginId);
    if (action == null) return;

    for (SimpleXmlElement child : element.getChildren()) {
      if (ADD_TO_GROUP_ELEMENT_NAME.equals(child.getName())) {
        processAddToGroupNode(action, child, pluginId, isSecondary(child));
      }
    }
  }

  /**
   * @param element description of link
   */
  private void processAddToGroupNode(AnAction action, SimpleXmlElement element, final PluginId pluginId, boolean secondary) {
    // Real subclasses of AnAction should not be here
    if (!(action instanceof AnSeparator)) {
      assertActionIsGroupOrStub(action);
    }

    String name = action instanceof ActionStub ? ((ActionStub)action).getClassName() : action.getClass().getName();
    String id = action instanceof ActionStub ? ((ActionStub)action).getId() : myAction2Id.get(action);
    String actionName = name + " (" + id + ")";

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
    addToGroupInner(parentGroup, action, new Constraints(anchor, relativeToActionId), secondary);
  }

  private void addToGroupInner(AnAction group, AnAction action, Constraints constraints, boolean secondary) {
    String actionId = action instanceof ActionStub ? ((ActionStub)action).getId() : myAction2Id.get(action);
    ((DefaultActionGroup)group).addAction(action, constraints, this).setAsSecondary(secondary);
    myId2GroupId.putValue(actionId, myAction2Id.get(group));
  }

  @Nullable
  public AnAction getParentGroup(final String groupId, @Nullable final String actionName, @Nullable final PluginId pluginId) {
    if (groupId == null || groupId.isEmpty()) {
      reportActionError(pluginId, actionName + ": attribute \"group-id\" should be defined");
      return null;
    }
    AnAction parentGroup = getActionImpl(groupId, true);
    if (parentGroup == null) {
      reportActionError(pluginId, actionName + ": group with id \"" + groupId + "\" isn't registered; action will be added to the \"Other\" group");
      parentGroup = getActionImpl(IdeActions.GROUP_OTHER_MENU, true);
    }
    if (!(parentGroup instanceof DefaultActionGroup)) {
      reportActionError(pluginId, actionName +
                                  ": group with id \"" +
                                  groupId +
                                  "\" should be instance of " +
                                  DefaultActionGroup.class.getName() +
                                  " but was " +
                                  (parentGroup != null ? parentGroup.getClass() : "[null]"));
      return null;
    }
    return parentGroup;
  }

  /**
   * @param parentGroup group which is the parent of the separator. It can be {@code null} in that
   *                    case separator will be added to group described in the <add-to-group ....> subelement.
   * @param element     XML element which represent separator.
   */
  private void processSeparatorNode(@Nullable DefaultActionGroup parentGroup, SimpleXmlElement element, PluginId pluginId) {
    if (!SEPARATOR_ELEMENT_NAME.equals(element.getName())) {
      reportActionError(pluginId, "unexpected name of element \"" + element.getName() + "\"");
      return;
    }
    String text = element.getAttributeValue(TEXT_ATTR_NAME);
    AnSeparator separator = text != null ? new AnSeparator(text) : AnSeparator.getInstance();
    if (parentGroup != null) {
      parentGroup.add(separator, this);
    }
    // try to find inner <add-to-parent...> tag
    for (SimpleXmlElement child : element.getChildren()) {
      if (ADD_TO_GROUP_ELEMENT_NAME.equals(child.getName())) {
        processAddToGroupNode(separator, child, pluginId, isSecondary(child));
      }
    }
  }

  private void processUnregisterNode(SimpleXmlElement element, PluginId pluginId) {
    String id = element.getAttributeValue(ID_ATTR_NAME);
    if (id == null) {
      reportActionError(pluginId, "'id' attribute is required for 'unregister' elements");
      return;
    }
    AnAction action = getAction(id);
    if (action == null) {
      reportActionError(pluginId, "Trying to unregister non-existing action " + id);
      return;
    }

    AbbreviationManager.getInstance().removeAllAbbreviations(id);
    unregisterAction(id);
  }

  private static void processKeyboardShortcutNode(SimpleXmlElement element, String actionId, PluginId pluginId, @Nonnull KeymapManagerEx keymapManager) {
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
    if (keymapName == null || keymapName.trim().isEmpty()) {
      reportActionError(pluginId, "attribute \"keymap\" should be defined");
      return;
    }
    Keymap keymap = keymapManager.getKeymap(keymapName);
    if (keymap == null) {
      reportKeymapNotFoundWarning(pluginId, keymapName);
      return;
    }
    final KeyboardShortcut shortcut = new KeyboardShortcut(firstKeyStroke, secondKeyStroke);
    processRemoveAndReplace(element, actionId, keymap, shortcut);
  }

  private static void processRemoveAndReplace(@Nonnull SimpleXmlElement element, String actionId, @Nonnull Keymap keymap, @Nonnull Shortcut shortcut) {
    boolean remove = Boolean.parseBoolean(element.getAttributeValue(REMOVE_SHORTCUT_ATTR_NAME));
    boolean replace = Boolean.parseBoolean(element.getAttributeValue(REPLACE_SHORTCUT_ATTR_NAME));
    if (remove) {
      keymap.removeShortcut(actionId, shortcut);
    }
    if (replace) {
      keymap.removeAllActionShortcuts(actionId);
    }
    if (!remove) {
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

    if (ref == null || ref.isEmpty()) {
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

  private void processActionsChildElement(@Nonnull PluginDescriptor plugin, @Nonnull SimpleXmlElement child, LocalizeHelper localizeHelper) {
    String name = child.getName();
    if (ACTION_ELEMENT_NAME.equals(name)) {
      AnAction action = processActionElement(child, plugin, localizeHelper);
      if (action != null) {
        assertActionIsGroupOrStub(action);
      }
    }
    else if (GROUP_ELEMENT_NAME.equals(name)) {
      processGroupElement(child, plugin, localizeHelper);
    }
    else if (SEPARATOR_ELEMENT_NAME.equals(name)) {
      processSeparatorNode(null, child, plugin.getPluginId());
    }
    else if (REFERENCE_ELEMENT_NAME.equals(name)) {
      processReferenceNode(child, plugin.getPluginId());
    }
    //else if (UNREGISTER_ELEMENT_NAME.equals(name)) {
    //  processUnregisterNode(child, pluginId);
    //}
    else {
      reportActionError(plugin.getPluginId(), "unexpected name of element \"" + name + "\n");
    }
  }

  public boolean canUnloadActions(PluginDescriptor pluginDescriptor) {
    List<SimpleXmlElement> elements = pluginDescriptor.getActionsDescriptionElements();
    if (elements == null) return true;
    for (SimpleXmlElement element : elements) {
      if (!element.getName().equals(ACTION_ELEMENT_NAME) && !(element.getName().equals(GROUP_ELEMENT_NAME) && element.getAttributeValue(ID_ATTR_NAME) != null)) {
        LOG.info("Plugin " + pluginDescriptor.getPluginId() + " is not unload-safe because of action element " + element.getName());
        return false;
      }
    }
    return true;
  }

  public void unloadActions(PluginDescriptor pluginDescriptor) {
    List<SimpleXmlElement> elements = pluginDescriptor.getActionsDescriptionElements();
    if (elements == null) return;
    for (SimpleXmlElement element : elements) {
      if (element.getName().equals(ACTION_ELEMENT_NAME)) {
        String className = element.getAttributeValue(CLASS_ATTR_NAME);
        String id = obtainActionId(element, className);
        unregisterAction(id);
      }
      else if (element.getName().equals(GROUP_ELEMENT_NAME)) {
        String id = element.getAttributeValue(ID_ATTR_NAME);
        if (id == null) {
          throw new IllegalStateException("Cannot unload groups with no ID");
        }
        unregisterAction(id);
      }
    }
  }

  @Override
  public void registerAction(@Nonnull String actionId, @Nonnull AnAction action, @Nullable PluginId pluginId) {
    registerAction(actionId, action, pluginId, null);
  }

  public void registerAction(@Nonnull String actionId, @Nonnull AnAction action, @Nullable PluginId pluginId, @Nullable String projectType) {
    synchronized (myLock) {
      if (addToMap(actionId, action) == null) return;
      if (myAction2Id.containsKey(action)) {
        reportActionError(pluginId, "action was already registered for another ID. ID is " + myAction2Id.get(action) + getPluginInfo(pluginId));
        return;
      }
      myId2Index.put(actionId, myRegisteredActionsCount++);
      myAction2Id.put(action, actionId);
      if (pluginId != null && !(action instanceof ActionGroup)) {
        myPlugin2Id.putValue(pluginId, actionId);
      }
      action.registerCustomShortcutSet(new ProxyShortcutSet(actionId), null);
    }
  }

  private AnAction addToMap(String actionId, AnAction action) {
    myId2Action.put(actionId, action);
    return action;
  }

  @Override
  public void registerAction(@Nonnull String actionId, @Nonnull AnAction action) {
    registerAction(actionId, action, null);
  }

  @Override
  public void unregisterAction(@Nonnull String actionId) {
    unregisterAction(actionId, true);
  }

  private void unregisterAction(@Nonnull String actionId, boolean removeFromGroups) {
    synchronized (myLock) {
      if (!myId2Action.containsKey(actionId)) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("action with ID " + actionId + " wasn't registered");
        }
        return;
      }
      AnAction oldValue = myId2Action.remove(actionId);
      myAction2Id.remove(oldValue);
      myId2Index.remove(actionId);

      for (final Map.Entry<PluginId, Collection<String>> entry : myPlugin2Id.entrySet()) {
        Collection<String> pluginActions = entry.getValue();
        pluginActions.remove(actionId);
      }
      if (removeFromGroups) {
        //CustomActionsSchema customActionSchema = ApplicationManager.getApplication().getServiceIfCreated(CustomActionsSchema.class);
        for (String groupId : myId2GroupId.get(actionId)) {
          //if (customActionSchema != null) {
          //  customActionSchema.invalidateCustomizedActionGroup(groupId);
          //}
          DefaultActionGroup group = ObjectUtils.assertNotNull((DefaultActionGroup)getActionOrStub(groupId));
          group.remove(oldValue, actionId);
        }
      }
      if (oldValue instanceof ActionGroup) {
        myId2GroupId.values().remove(actionId);
      }
    }
  }

  @Nonnull
  @Override
  public Comparator<String> getRegistrationOrderComparator() {
    return Comparator.comparingInt(myId2Index::get);
  }

  @Nonnull
  @Override
  public String[] getPluginActions(@Nonnull PluginId pluginName) {
    return ArrayUtil.toStringArray(myPlugin2Id.get(pluginName));
  }

  public void addActionPopup(@Nonnull Object menu) {
    myPopups.add(menu);
    if (menu instanceof ActionPopupMenu) {
      for (ActionPopupMenuListener listener : myActionPopupMenuListeners) {
        listener.actionPopupMenuCreated((ActionPopupMenu)menu);
      }
    }
  }

  void removeActionPopup(@Nonnull Object menu) {
    final boolean removed = myPopups.remove(menu);
    if (removed && menu instanceof ActionPopupMenu) {
      for (ActionPopupMenuListener listener : myActionPopupMenuListeners) {
        listener.actionPopupMenuReleased((ActionPopupMenu)menu);
      }
    }
  }

  @Override
  public void queueActionPerformedEvent(@Nonnull final AnAction action, @Nonnull DataContext context, @Nonnull AnActionEvent event) {
    if (myPopups.isEmpty()) {
      fireAfterActionPerformed(action, context, event);
    }
  }

  public boolean isToolWindowContextMenuVisible() {
    for (Object popup : myPopups) {
      if (popup instanceof DesktopActionPopupMenuImpl && ((DesktopActionPopupMenuImpl)popup).isToolWindowContextMenu()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isActionPopupStackEmpty() {
    return myPopups.isEmpty();
  }

  @Override
  public boolean isTransparentOnlyActionsUpdateNow() {
    return myTransparentOnlyUpdate;
  }

  //@Override
  public void addActionPopupMenuListener(@Nonnull ActionPopupMenuListener listener, @Nonnull Disposable parentDisposable) {
    myActionPopupMenuListeners.add(listener);
    Disposer.register(parentDisposable, () -> myActionPopupMenuListeners.remove(listener));
  }

  //@Override
  public void replaceAction(@Nonnull String actionId, @Nonnull AnAction newAction) {
    Class<?> callerClass = ReflectionUtil.getGrandCallerClass();
    PluginId pluginId = callerClass != null ? PluginManager.getPluginId(callerClass) : null;
    replaceAction(actionId, newAction, pluginId);
  }

  private AnAction replaceAction(@Nonnull String actionId, @Nonnull AnAction newAction, @Nullable PluginId pluginId) {
    AnAction oldAction = newAction instanceof OverridingAction ? getAction(actionId) : getActionOrStub(actionId);
    if (oldAction != null) {
      if (newAction instanceof OverridingAction) {
        myBaseActions.put((OverridingAction)newAction, oldAction);
      }
      boolean isGroup = oldAction instanceof ActionGroup;
      if (isGroup != newAction instanceof ActionGroup) {
        throw new IllegalStateException("cannot replace a group with an action and vice versa: " + actionId);
      }
      for (String groupId : myId2GroupId.get(actionId)) {
        DefaultActionGroup group = (DefaultActionGroup)getActionOrStub(groupId);
        if (group == null) {
          throw new IllegalStateException("Trying to replace action which has been added to a non-existing group " + groupId);
        }
        group.replaceAction(oldAction, newAction);
      }
      unregisterAction(actionId, false);
    }
    registerAction(actionId, newAction, pluginId);
    return oldAction;
  }

  /**
   * Returns the action overridden by the specified overriding action (with overrides="true" in plugin.xml).
   */
  public AnAction getBaseAction(OverridingAction overridingAction) {
    return myBaseActions.get(overridingAction);
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
    if (myLastPreformedActionId == null && action instanceof ActionIdProvider) {
      myLastPreformedActionId = ((ActionIdProvider)action).getId();
    }
    //noinspection AssignmentToStaticFieldFromInstanceMethod
    LastActionTracker.ourLastActionId = myLastPreformedActionId;
    final PsiFile file = dataContext.getData(CommonDataKeys.PSI_FILE);
    final Language language = file != null ? file.getLanguage() : null;
    //ActionsCollector.getInstance().record(CommonDataKeys.PROJECT.getData(dataContext), action, event, language);
    for (AnActionListener listener : myActionListeners) {
      listener.beforeActionPerformed(action, dataContext, event);
    }
    publisher().beforeActionPerformed(action, dataContext, event);
  }

  @Override
  public void fireAfterActionPerformed(@Nonnull AnAction action, @Nonnull DataContext dataContext, @Nonnull AnActionEvent event) {
    myPrevPerformedActionId = myLastPreformedActionId;
    myLastPreformedActionId = getId(action);
    //noinspection AssignmentToStaticFieldFromInstanceMethod
    LastActionTracker.ourLastActionId = myLastPreformedActionId;
    for (AnActionListener listener : myActionListeners) {
      try {
        listener.afterActionPerformed(action, dataContext, event);
      }
      catch (AbstractMethodError ignored) {
      }
    }
    publisher().afterActionPerformed(action, dataContext, event);
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
  public void fireBeforeEditorTyping(char c, @Nonnull DataContext dataContext) {
    myLastTimeEditorWasTypedIn = System.currentTimeMillis();
    for (AnActionListener listener : myActionListeners) {
      listener.beforeEditorTyping(c, dataContext);
    }
    publisher().beforeEditorTyping(c, dataContext);
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

  public void preloadActions(@Nonnull ProgressIndicator indicator) {
    List<String> ids;
    synchronized (myLock) {
      ids = new ArrayList<>(myId2Action.keySet());
    }
    for (String id : ids) {
      indicator.checkCanceled();
      
      getActionImpl(id, false);
      // don't preload ActionGroup.getChildren() because that would un-stub child actions
      // and make it impossible to replace the corresponding actions later
      // (via unregisterAction+registerAction, as some app components do)
    }
  }

  @Nonnull
  @Override
  public ActionCallback tryToExecute(@Nonnull final AnAction action, @Nonnull final InputEvent inputEvent, @Nullable final Component contextComponent, @Nullable final String place, boolean now) {

    final Application app = ApplicationManager.getApplication();
    assert app.isDispatchThread();

    final ActionCallback result = new ActionCallback();
    final Runnable doRunnable = () -> tryToExecuteNow(action, inputEvent, contextComponent, place, result);

    if (now) {
      doRunnable.run();
    }
    else {
      //noinspection SSBasedInspection
      SwingUtilities.invokeLater(doRunnable);
    }

    return result;
  }

  private void tryToExecuteNow(@Nonnull AnAction action, final InputEvent inputEvent, final Component contextComponent, final String place, final ActionCallback result) {
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
      if (component != null && !component.isShowing() && !ActionPlaces.TOUCHBAR_GENERAL.equals(place)) {
        result.setRejected();
        return;
      }

      fireBeforeActionPerformed(action, context, event);

      consulo.disposer.Disposable eventListenerDisposable = consulo.disposer.Disposable.newDisposable("tryToExecuteNow");
      result.doWhenProcessed(() -> consulo.disposer.Disposer.dispose(eventListenerDisposable));

      UIUtil.addAwtListener(event1 -> {
        if (event1.getID() == WindowEvent.WINDOW_OPENED || event1.getID() == WindowEvent.WINDOW_ACTIVATED) {
          if (!result.isProcessed()) {
            final WindowEvent we = (WindowEvent)event1;
            IdeFocusManager.findInstanceByComponent(we.getWindow()).doWhenFocusSettlesDown(result.createSetDoneRunnable(), ModalityState.defaultModalityState());
          }
        }
      }, AWTEvent.WINDOW_EVENT_MASK, eventListenerDisposable);

      ActionUtil.performActionDumbAware(action, event);
      result.setDone();
      queueActionPerformedEvent(action, context, event);
    }), ModalityState.defaultModalityState());
  }

  private class MyTimer extends Timer implements ActionListener {
    private final List<TimerListener> myTimerListeners = ContainerUtil.createLockFreeCopyOnWriteList();
    private final List<TimerListener> myTransparentTimerListeners = ContainerUtil.createLockFreeCopyOnWriteList();
    private int myLastTimePerformed;

    private MyTimer() {
      super(TIMER_DELAY, null);
      addActionListener(this);
      setRepeats(true);
      final MessageBusConnection connection = ApplicationManager.getApplication().getMessageBus().connect();
      connection.subscribe(ApplicationActivationListener.TOPIC, new ApplicationActivationListener() {
        @Override
        public void applicationActivated(@Nonnull IdeFrame ideFrame) {
          setDelay(TIMER_DELAY);
          restart();
        }

        @Override
        public void applicationDeactivated(@Nonnull IdeFrame ideFrame) {
          setDelay(DEACTIVATED_TIMER_DELAY);
        }
      });
    }

    @Override
    public String toString() {
      return "Action manager timer";
    }

    void addTimerListener(@Nonnull TimerListener listener, boolean transparent) {
      (transparent ? myTransparentTimerListeners : myTimerListeners).add(listener);
    }

    void removeTimerListener(@Nonnull TimerListener listener, boolean transparent) {
      (transparent ? myTransparentTimerListeners : myTimerListeners).remove(listener);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (myLastTimeEditorWasTypedIn + UPDATE_DELAY_AFTER_TYPING > System.currentTimeMillis()) {
        return;
      }

      final int lastEventCount = myLastTimePerformed;
      myLastTimePerformed = ActivityTracker.getInstance().getCount();

      if (myLastTimePerformed == lastEventCount && !Registry.is("actionSystem.always.update.toolbar.actions")) {
        return;
      }

      boolean transparentOnly = myLastTimePerformed == lastEventCount;

      try {
        myTransparentOnlyUpdate = transparentOnly;
        Set<TimerListener> notified = new HashSet<>();
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

    private void notifyListeners(final List<? extends TimerListener> timerListeners, final Set<? super TimerListener> notified) {
      for (TimerListener listener : timerListeners) {
        if (notified.add(listener)) {
          runListenerAction(listener);
        }
      }
    }

    private void runListenerAction(@Nonnull TimerListener listener) {
      ModalityState modalityState = listener.getModalityState();
      if (modalityState == null) return;
      LOG.debug("notify ", listener);
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
}