/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.process.cmd;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.macro.PathMacros;
import consulo.logging.Logger;
import consulo.process.local.EnvironmentUtil;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.StringUtil;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParametersList implements Cloneable {
  private static final Logger LOG = Logger.getInstance(ParametersList.class);

  private static final Pattern PROPERTY_PATTERN = Pattern.compile("-D(\\S+?)=(.+)");

  private List<String> myParameters = new ArrayList<String>();
  private Map<String, String> myMacroMap = null;
  private List<ParamsGroup> myGroups = new ArrayList<ParamsGroup>();

  public boolean hasParameter(@NonNls final String param) {
    return myParameters.contains(param);
  }

  public boolean hasProperty(@NonNls final String name) {
    return getPropertyValue(name) != null;
  }

  @Nullable
  public String getPropertyValue(@Nonnull @NonNls final String name) {
    final String prefix = "-D" + name + "=";
    for (String parameter : myParameters) {

      if (parameter.startsWith(prefix)) {
        return parameter.substring(prefix.length());
      }
    }
    return null;
  }

  @Nonnull
  public Map<String, String> getProperties() {
    Map<String, String> result = new HashMap<String, String>();
    for (String parameter : myParameters) {
      Matcher matcher = PROPERTY_PATTERN.matcher(parameter);
      if (matcher.matches()) {
        result.put(matcher.group(1), matcher.group(2));
      }
    }
    return result;
  }

  @Nonnull
  public String getParametersString() {
    return join(getList());
  }

  @Nonnull
  public String[] getArray() {
    return ArrayUtil.toStringArray(getList());
  }

  @Nonnull
  public List<String> getList() {
    if (myGroups.isEmpty()) {
      return Collections.unmodifiableList(myParameters);
    }

    final List<String> params = new ArrayList<String>();
    params.addAll(myParameters);
    for (ParamsGroup group : myGroups) {
      params.addAll(group.getParameters());
    }
    return Collections.unmodifiableList(params);
  }

  public void clearAll() {
    myParameters.clear();
    myGroups.clear();
  }

  public void prepend(@NonNls final String parameter) {
    addAt(0, parameter);
  }

  public void prependAll(@NonNls final String... parameter) {
    addAll(parameter);
    Collections.rotate(myParameters, parameter.length);
  }

  public void addParametersString(final String parameters) {
    if (parameters != null) {
      final String[] split = parse(parameters);
      for (String param : split) {
        add(param);
      }
    }
  }

  public void add(@NonNls final String parameter) {
    myParameters.add(expandMacros(parameter));
  }

  public ParamsGroup addParamsGroup(@Nonnull final String groupId) {
    return addParamsGroup(new ParamsGroup(groupId));
  }

  public ParamsGroup addParamsGroup(@Nonnull final ParamsGroup group) {
    myGroups.add(group);
    return group;
  }

  public ParamsGroup addParamsGroupAt(final int index, @Nonnull final ParamsGroup group) {
    myGroups.add(index, group);
    return group;
  }

  public ParamsGroup addParamsGroupAt(final int index, @Nonnull final String groupId) {
    final ParamsGroup group = new ParamsGroup(groupId);
    myGroups.add(index, group);
    return group;
  }

  public int getParamsGroupsCount() {
    return myGroups.size();
  }

  public List<String> getParameters() {
    return Collections.unmodifiableList(myParameters);
  }

  public List<ParamsGroup> getParamsGroups() {
    return Collections.unmodifiableList(myGroups);
  }

  public ParamsGroup getParamsGroupAt(final int index) {
    return myGroups.get(index);
  }

  @Nullable
  public ParamsGroup getParamsGroup(@Nonnull final String name) {
    for (ParamsGroup group : myGroups) {
      if (name.equals(group.getId())) return group;
    }
    return null;
  }

  public ParamsGroup removeParamsGroup(final int index) {
    return myGroups.remove(index);
  }

  public void addAt(final int index, @Nonnull final String parameter) {
    myParameters.add(index, expandMacros(parameter));
  }

  public void defineProperty(@NonNls final String propertyName, @NonNls final String propertyValue) {
    addProperty(propertyName, propertyValue);
  }

  public void addProperty(@NonNls final String propertyName, @NonNls final String propertyValue) {
    //noinspection HardCodedStringLiteral
    myParameters.add("-D" + propertyName + "=" + propertyValue);
  }

  public void replaceOrAppend(final @NonNls String parameterPrefix, final @NonNls String replacement) {
    replaceOrAdd(parameterPrefix, replacement, myParameters.size());
  }

  private void replaceOrAdd(final @NonNls String parameterPrefix, final @NonNls String replacement, final int position) {
    for (ListIterator<String> iterator = myParameters.listIterator(); iterator.hasNext(); ) {
      final String param = iterator.next();
      if (param.startsWith(parameterPrefix)) {
        if (replacement != null && replacement.isEmpty()) {
          iterator.remove();
        }
        else {
          iterator.set(replacement);
        }
        return;
      }
    }
    if (replacement != null && !replacement.isEmpty()) {
      myParameters.add(position, replacement);
    }
  }

  public void replaceOrPrepend(final @NonNls String parameter, final @NonNls String replacement) {
    replaceOrAdd(parameter, replacement, 0);
  }

  public void set(int ind, final @NonNls String value) {
    myParameters.set(ind, value);
  }

  public String get(int ind) {
    return myParameters.get(ind);
  }

  public void add(@NonNls final String name, @NonNls final String value) {
    add(name);
    add(value);
  }

  public void addAll(final String... parameters) {
    addAll(Arrays.asList(parameters));
  }

  public void addAll(final List<String> parameters) {
    // Don't use myParameters.addAll(parameters) , it does not call expandMacros(parameter)
    for (String parameter : parameters) {
      add(parameter);
    }
  }

  @Override
  public ParametersList clone() {
    return copyTo(new ParametersList());
  }

  @Nonnull
  ParametersList copyTo(@Nonnull ParametersList target) {
    target.myParameters.addAll(myParameters);
    for (ParamsGroup group : myGroups) {
      target.myGroups.add(group.clone());
    }
    return target;
  }

  /**
   * @see ParametersListUtil#join(java.util.List)
   */
  @Nonnull
  public static String join(@Nonnull final List<String> parameters) {
    return ParametersListUtil.join(parameters);
  }

  /**
   * @see ParametersListUtil#join(java.util.List)
   */
  @Nonnull
  public static String join(final String... parameters) {
    return ParametersListUtil.join(parameters);
  }

  /**
   * @see ParametersListUtil#parseToArray(String)
   */
  @Nonnull
  public static String[] parse(@Nonnull final String string) {
    return ParametersListUtil.parseToArray(string);
  }

  public String expandMacros(String text) {
    final Map<String, String> macroMap = getMacroMap();
    final Set<String> set = macroMap.keySet();
    for (final String from : set) {
      final String to = macroMap.get(from);
      text = StringUtil.replace(text, from, to, true);
    }
    return text;
  }

  private Map<String, String> getMacroMap() {
    if (myMacroMap == null) {
      // the insertion order is important for later iterations, so LinkedHashMap is used
      myMacroMap = new LinkedHashMap<String, String>();

      // ApplicationManager.getApplication() will return null if executed in ParameterListTest
      final Application application = ApplicationManager.getApplication();
      if (application != null) {
        final PathMacros pathMacros = PathMacros.getInstance();
        if (pathMacros != null) {
          for (String name : pathMacros.getUserMacroNames()) {
            final String value = pathMacros.getValue(name);
            if (value != null) {
              myMacroMap.put("${" + name + "}", value);
            }
          }
        }
        final Map<String, String> env = EnvironmentUtil.getEnvironmentMap();
        for (String name : env.keySet()) {
          final String key = "${" + name + "}";
          if (!myMacroMap.containsKey(key)) {
            myMacroMap.put(key, env.get(name));
          }
        }
      }
    }
    return myMacroMap;
  }

  @Override
  public String toString() {
    return myParameters.toString();
  }

}
