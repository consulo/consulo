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
package consulo.execution.impl.internal.console;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import jakarta.inject.Singleton;

import java.util.*;

/**
 * @author peter
 */
@Singleton
@State(name = "ConsoleFoldingSettings", storages = @Storage(file = StoragePathMacros.APP_CONFIG + "/consoleFolding.xml"))
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class ConsoleFoldingSettings implements PersistentStateComponent<ConsoleFoldingSettings.MyBean> {
  private final List<String> myPositivePatterns = new ArrayList<>();
  private final List<String> myNegativePatterns = new ArrayList<>();

  public ConsoleFoldingSettings() {
    ConsoleFoldingRegistratorImpl registrator = ConsoleFoldingRegistratorImpl.last();
    myPositivePatterns.addAll(registrator.getAddSet());
    myNegativePatterns.addAll(registrator.getRemoveSet());
  }

  public static ConsoleFoldingSettings getSettings() {
    return Application.get().getInstance(ConsoleFoldingSettings.class);
  }

  public boolean shouldFoldLine(String line) {
    return containsAny(line, myPositivePatterns) && !containsAny(line, myNegativePatterns);
  }

  private static boolean containsAny(String line, List<String> patterns) {
    for (String pattern : patterns) {
      if (line.contains(pattern)) {
        return true;
      }
    }
    return false;
  }

  public List<String> getPositivePatterns() {
    return myPositivePatterns;
  }

  public List<String> getNegativePatterns() {
    return myNegativePatterns;
  }

  @Override
  public MyBean getState() {
    MyBean result = new MyBean();
    writeDiff(result.addedPositive, result.removedPositive, false);
    writeDiff(result.addedNegative, result.removedNegative, true);
    return result;
  }

  private void writeDiff(List<String> added, List<String> removed, boolean negated) {
    Set<String> baseline = new LinkedHashSet<>();

    ConsoleFoldingRegistratorImpl registrator = ConsoleFoldingRegistratorImpl.last();

    Set<String> targets = negated ? registrator.getRemoveSet() : registrator.getAddSet();

    baseline.addAll(targets);

    final List<String> current = patternList(negated);
    added.addAll(current);
    added.removeAll(baseline);

    baseline.removeAll(current);
    removed.addAll(baseline);
  }

  private List<String> patternList(boolean negated) {
    return negated ? myNegativePatterns : myPositivePatterns;
  }

  private Collection<String> filterEmptyStringsFromCollection(Collection<String> collection) {
    return ContainerUtil.filter(collection, input -> !StringUtil.isEmpty(input));
  }

  @Override
  public void loadState(MyBean state) {
    myPositivePatterns.clear();
    myNegativePatterns.clear();

    Set<String> removedPositive = new HashSet<>(state.removedPositive);
    Set<String> removedNegative = new HashSet<>(state.removedNegative);

    ConsoleFoldingRegistratorImpl registrator = ConsoleFoldingRegistratorImpl.last();
    for (String addFold : registrator.getAddSet()) {
      if (!removedPositive.contains(addFold)) {
        myPositivePatterns.add(addFold);
      }
    }

    for (String removeFold : registrator.getRemoveSet()) {
      if (!removedNegative.contains(removeFold)) {
        myNegativePatterns.add(removeFold);
      }
    }

    myPositivePatterns.addAll(filterEmptyStringsFromCollection(state.addedPositive));
    myNegativePatterns.addAll(filterEmptyStringsFromCollection(state.addedNegative));
  }

  public static class MyBean {
    public List<String> addedPositive = new ArrayList<>();
    public List<String> addedNegative = new ArrayList<>();
    public List<String> removedPositive = new ArrayList<>();
    public List<String> removedNegative = new ArrayList<>();
  }

}
