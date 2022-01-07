/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.openapi.actionSystem.ex;

import com.intellij.ide.actions.QuickSwitchSchemeAction;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.BundledQuickListsProvider;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.options.BaseSchemeProcessor;
import com.intellij.openapi.options.SchemesManager;
import com.intellij.openapi.options.SchemesManagerFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.util.PathUtilRt;
import com.intellij.util.ThrowableConvertor;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Singleton
public class QuickListsManager {
  public static QuickListsManager getInstance() {
    return ApplicationManager.getApplication().getComponent(QuickListsManager.class);
  }

  static final String FILE_SPEC = StoragePathMacros.ROOT_CONFIG + "/quicklists";

  private static final String LIST_TAG = "list";

  private final ActionManager myActionManager;
  private final SchemesManager<QuickList, QuickList> mySchemesManager;

  @Inject
  public QuickListsManager(@Nonnull ActionManager actionManager, @Nonnull SchemesManagerFactory schemesManagerFactory) {
    myActionManager = actionManager;
    mySchemesManager = schemesManagerFactory.createSchemesManager(FILE_SPEC, new BaseSchemeProcessor<QuickList>() {
      @Nonnull
      @Override
      public QuickList readScheme(@Nonnull Element element) {
        return createItem(element);
      }

      @Override
      public Element writeScheme(@Nonnull QuickList scheme) {
        Element element = new Element(LIST_TAG);
        scheme.writeExternal(element);
        return element;
      }
    }, RoamingType.PER_USER);

    for (BundledQuickListsProvider provider : BundledQuickListsProvider.EP_NAME.getExtensions()) {
      for (final String path : provider.getBundledListsRelativePaths()) {
        mySchemesManager.loadBundledScheme(path, provider, new ThrowableConvertor<Element, QuickList, Throwable>() {
          @Override
          public QuickList convert(Element element) throws Throwable {
            QuickList item = createItem(element);
            item.getExternalInfo().setHash(JDOMUtil.getTreeHash(element, true));
            item.getExternalInfo().setPreviouslySavedName(item.getName());
            item.getExternalInfo().setCurrentFileName(PathUtilRt.getFileName(path));
            return item;
          }
        });
      }
    }
    mySchemesManager.loadSchemes();
    registerActions();
  }

  @Nonnull
  private static QuickList createItem(@Nonnull Element element) {
    QuickList item = new QuickList();
    item.readExternal(element);
    return item;
  }

  @Nonnull
  public QuickList[] getAllQuickLists() {
    Collection<QuickList> lists = mySchemesManager.getAllSchemes();
    return lists.toArray(new QuickList[lists.size()]);
  }

  private void registerActions() {
    // to prevent exception if 2 or more targets have the same name
    Set<String> registeredIds = new HashSet<String>();
    for (QuickList list : mySchemesManager.getAllSchemes()) {
      String actionId = list.getActionId();
      if (registeredIds.add(actionId)) {
        myActionManager.registerAction(actionId, new InvokeQuickListAction(list));
      }
    }
  }

  private void unregisterActions() {
    for (String oldId : myActionManager.getActionIds(QuickList.QUICK_LIST_PREFIX)) {
      myActionManager.unregisterAction(oldId);
    }
  }

  public void setQuickLists(@Nonnull QuickList[] quickLists) {
    mySchemesManager.clearAllSchemes();
    unregisterActions();
    for (QuickList quickList : quickLists) {
      mySchemesManager.addNewScheme(quickList, true);
    }
    registerActions();
  }

  private static class InvokeQuickListAction extends QuickSwitchSchemeAction {
    private final QuickList myQuickList;

    public InvokeQuickListAction(@Nonnull QuickList quickList) {
      myQuickList = quickList;
      myActionPlace = ActionPlaces.ACTION_PLACE_QUICK_LIST_POPUP_ACTION;
      getTemplatePresentation().setDescription(myQuickList.getDescription());
      getTemplatePresentation().setText(myQuickList.getName(), false);
    }

    @Override
    protected void fillActions(Project project, @Nonnull DefaultActionGroup group, @Nonnull DataContext dataContext) {
      ActionManager actionManager = ActionManager.getInstance();
      for (String actionId : myQuickList.getActionIds()) {
        if (QuickList.SEPARATOR_ID.equals(actionId)) {
          group.addSeparator();
        }
        else {
          AnAction action = actionManager.getAction(actionId);
          if (action != null) {
            group.add(action);
          }
        }
      }
    }

    @Override
    protected boolean isEnabled() {
      return true;
    }
  }
}
