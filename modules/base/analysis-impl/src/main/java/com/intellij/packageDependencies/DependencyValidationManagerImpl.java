/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.packageDependencies;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.openapi.components.MainConfigurationStateSplitter;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.scope.packageSet.*;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.UIUtil;
import consulo.logging.Logger;
import consulo.ui.image.Image;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

@Singleton
@State(name = "DependencyValidationManager", storages = {
        @Storage(file = StoragePathMacros.PROJECT_CONFIG_DIR + "/scopes/", stateSplitter = DependencyValidationManagerImpl.ScopesStateSplitter.class)})
public class DependencyValidationManagerImpl extends DependencyValidationManager {
  private static final Logger LOG = Logger.getInstance(DependencyValidationManagerImpl.class);

  private final List<DependencyRule> myRules = new ArrayList<DependencyRule>();
  private final NamedScopeManager myNamedScopeManager;

  private boolean mySkipImportStatements;
  private boolean mySkipImportStatementsWasSpecified;

  @NonNls
  private static final String DENY_RULE_KEY = "deny_rule";
  @NonNls
  private static final String FROM_SCOPE_KEY = "from_scope";
  @NonNls
  private static final String TO_SCOPE_KEY = "to_scope";
  @NonNls
  private static final String IS_DENY_KEY = "is_deny";
  @NonNls
  private static final String UNNAMED_SCOPE = "unnamed_scope";
  @NonNls
  private static final String VALUE = "value";

  private final Map<String, PackageSet> myUnnamedScopes = new HashMap<String, PackageSet>();

  @Inject
  public DependencyValidationManagerImpl(final Project project, NamedScopeManager namedScopeManager) {
    super(project);
    myNamedScopeManager = namedScopeManager;
    namedScopeManager.addScopeListener(new ScopeListener() {
      @Override
      public void scopesChanged() {
        reloadScopes();
      }
    });
  }

  @Override
  @Nonnull
  public List<NamedScope> getPredefinedScopes() {
    final List<NamedScope> predefinedScopes = new ArrayList<NamedScope>();
    final CustomScopesProvider[] scopesProviders = CustomScopesProvider.CUSTOM_SCOPES_PROVIDER.getExtensions(myProject);
    for (CustomScopesProvider scopesProvider : scopesProviders) {
      predefinedScopes.addAll(scopesProvider.getCustomScopes());
    }
    return predefinedScopes;
  }

  @Override
  public NamedScope getPredefinedScope(@Nonnull String name) {
    final CustomScopesProvider[] scopesProviders = CustomScopesProvider.CUSTOM_SCOPES_PROVIDER.getExtensions(myProject);
    for (CustomScopesProvider scopesProvider : scopesProviders) {
      final NamedScope scope = scopesProvider instanceof CustomScopesProviderEx
                               ? ((CustomScopesProviderEx)scopesProvider).getCustomScope(name)
                               : CustomScopesProviderEx.findPredefinedScope(name, scopesProvider.getCustomScopes());
      if (scope != null) {
        return scope;
      }
    }
    return null;
  }

  @Override
  public boolean hasRules() {
    return !myRules.isEmpty();
  }

  @Override
  @javax.annotation.Nullable
  public DependencyRule getViolatorDependencyRule(@Nonnull PsiFile from, @Nonnull PsiFile to) {
    for (DependencyRule dependencyRule : myRules) {
      if (dependencyRule.isForbiddenToUse(from, to)) return dependencyRule;
    }

    return null;
  }

  @Override
  @Nonnull
  public DependencyRule[] getViolatorDependencyRules(@Nonnull PsiFile from, @Nonnull PsiFile to) {
    ArrayList<DependencyRule> result = new ArrayList<DependencyRule>();
    for (DependencyRule dependencyRule : myRules) {
      if (dependencyRule.isForbiddenToUse(from, to)) {
        result.add(dependencyRule);
      }
    }
    return result.toArray(new DependencyRule[result.size()]);
  }

  @Nonnull
  @Override
  public DependencyRule[] getApplicableRules(@Nonnull PsiFile file) {
    ArrayList<DependencyRule> result = new ArrayList<DependencyRule>();
    for (DependencyRule dependencyRule : myRules) {
      if (dependencyRule.isApplicable(file)) {
        result.add(dependencyRule);
      }
    }
    return result.toArray(new DependencyRule[result.size()]);
  }

  @Override
  public boolean skipImportStatements() {
    return mySkipImportStatements;
  }

  @Override
  public void setSkipImportStatements(final boolean skip) {
    mySkipImportStatements = skip;
  }

  @Nonnull
  @Override
  public Map<String, PackageSet> getUnnamedScopes() {
    return myUnnamedScopes;
  }

  @Nonnull
  @Override
  public DependencyRule[] getAllRules() {
    return myRules.toArray(new DependencyRule[myRules.size()]);
  }

  @Override
  public void removeAllRules() {
    myRules.clear();
  }

  @Override
  public void addRule(@Nonnull DependencyRule rule) {
    appendUnnamedScope(rule.getFromScope());
    appendUnnamedScope(rule.getToScope());
    myRules.add(rule);
  }

  @Override
  public void reloadRules() {
    final Element element = new Element("rules_2_reload");
    writeRules(element);
    readRules(element);
  }

  private void appendUnnamedScope(final NamedScope fromScope) {
    if (getScope(fromScope.getName()) == null) {
      final PackageSet packageSet = fromScope.getValue();
      if (packageSet != null && !myUnnamedScopes.containsKey(packageSet.getText())) {
        myUnnamedScopes.put(packageSet.getText(), packageSet);
      }
    }
  }

  @Override
  public String getDisplayName() {
    return IdeBundle.message("shared.scopes.node.text");
  }

  @Override
  public Image getIcon() {
    return AllIcons.Ide.SharedScope;
  }

  @Override
  public void loadState(Element element) {
    Element option = element.getChild("option");
    if (option != null && "SKIP_IMPORT_STATEMENTS".equals(option.getAttributeValue("name"))) {
      mySkipImportStatementsWasSpecified = !myProject.isDefault();
      mySkipImportStatements = Boolean.parseBoolean(option.getAttributeValue("value"));
    }

    super.loadState(element);
    myUnnamedScopes.clear();
    final List unnamedScopes = element.getChildren(UNNAMED_SCOPE);
    final PackageSetFactory packageSetFactory = PackageSetFactory.getInstance();
    for (Object unnamedScope : unnamedScopes) {
      try {
        final String packageSet = ((Element)unnamedScope).getAttributeValue(VALUE);
        myUnnamedScopes.put(packageSet, packageSetFactory.compile(packageSet));
      }
      catch (ParsingException ignored) {
        //skip pattern
      }
    }

    readRules(element);
  }

  private void readRules(Element element) {
    removeAllRules();

    for (Element rule1 : element.getChildren(DENY_RULE_KEY)) {
      DependencyRule rule = readRule(rule1);
      if (rule != null) {
        addRule(rule);
      }
    }
  }

  @Override
  public Element getState() {
    Element element = super.getState();
    assert element != null;
    if (mySkipImportStatements || mySkipImportStatementsWasSpecified) {
      element.addContent(new Element("option").setAttribute("name", "SKIP_IMPORT_STATEMENTS").setAttribute("value", Boolean.toString(mySkipImportStatements)));
    }

    if (!myUnnamedScopes.isEmpty()) {
      String[] unnamedScopes = myUnnamedScopes.keySet().toArray(new String[myUnnamedScopes.size()]);
      Arrays.sort(unnamedScopes);
      for (String unnamedScope : unnamedScopes) {
        element.addContent(new Element(UNNAMED_SCOPE).setAttribute(VALUE, unnamedScope));
      }
    }

    writeRules(element);
    return element;
  }

  private void writeRules(Element element) {
    for (DependencyRule rule : myRules) {
      Element ruleElement = writeRule(rule);
      if (ruleElement != null) {
        element.addContent(ruleElement);
      }
    }
  }

  @Override
  @javax.annotation.Nullable
  public NamedScope getScope(@javax.annotation.Nullable final String name) {
    final NamedScope scope = super.getScope(name);
    if (scope == null) {
      final PackageSet packageSet = myUnnamedScopes.get(name);
      if (packageSet != null) {
        return new NamedScope.UnnamedScope(packageSet);
      }
    }
    //compatibility for predefined scopes: rename Project -> All
    if (scope == null && Comparing.strEqual(name, "Project")) {
      return super.getScope("All");
    }
    return scope;
  }

  @javax.annotation.Nullable
  private static Element writeRule(DependencyRule rule) {
    NamedScope fromScope = rule.getFromScope();
    NamedScope toScope = rule.getToScope();
    if (fromScope == null || toScope == null) return null;
    Element ruleElement = new Element(DENY_RULE_KEY);
    ruleElement.setAttribute(FROM_SCOPE_KEY, fromScope.getName());
    ruleElement.setAttribute(TO_SCOPE_KEY, toScope.getName());
    ruleElement.setAttribute(IS_DENY_KEY, Boolean.valueOf(rule.isDenyRule()).toString());
    return ruleElement;
  }

  @Nullable
  private DependencyRule readRule(Element ruleElement) {
    String fromScope = ruleElement.getAttributeValue(FROM_SCOPE_KEY);
    String toScope = ruleElement.getAttributeValue(TO_SCOPE_KEY);
    String denyRule = ruleElement.getAttributeValue(IS_DENY_KEY);
    if (fromScope == null || toScope == null || denyRule == null) return null;
    final NamedScope fromNamedScope = getScope(fromScope);
    final NamedScope toNamedScope = getScope(toScope);
    if (fromNamedScope == null || toNamedScope == null) return null;
    return new DependencyRule(fromNamedScope, toNamedScope, Boolean.valueOf(denyRule).booleanValue());
  }

  static final class ScopesStateSplitter extends MainConfigurationStateSplitter {
    @Nonnull
    @Override
    protected String getSubStateFileName(@Nonnull Element element) {
      return element.getAttributeValue("name");
    }

    @Nonnull
    @Override
    protected String getComponentStateFileName() {
      return "scope_settings";
    }

    @Nonnull
    @Override
    protected String getSubStateTagName() {
      return "scope";
    }
  }

  private final List<Pair<NamedScope, NamedScopesHolder>> myScopes = ContainerUtil.createLockFreeCopyOnWriteList();

  private void reloadScopes() {
    UIUtil.invokeLaterIfNeeded(new Runnable() {
      @Override
      public void run() {
        if (getProject().isDisposed()) return;
        List<Pair<NamedScope, NamedScopesHolder>> scopeList = new ArrayList<Pair<NamedScope, NamedScopesHolder>>();
        addScopesToList(scopeList, DependencyValidationManagerImpl.this);
        addScopesToList(scopeList, myNamedScopeManager);
        myScopes.clear();
        myScopes.addAll(scopeList);
        reloadRules();
      }
    });
  }

  private static void addScopesToList(@Nonnull final List<Pair<NamedScope, NamedScopesHolder>> scopeList, @Nonnull final NamedScopesHolder holder) {
    for (NamedScope scope : holder.getScopes()) {
      scopeList.add(Pair.create(scope, holder));
    }
  }

  @Nonnull
  public List<Pair<NamedScope, NamedScopesHolder>> getScopeBasedHighlightingCachedScopes() {
    return myScopes;
  }

  @Override
  public void fireScopeListeners() {
    super.fireScopeListeners();
    reloadScopes();
  }
}
