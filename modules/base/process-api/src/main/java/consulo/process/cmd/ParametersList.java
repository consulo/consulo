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

import org.jspecify.annotations.Nullable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParametersList implements Cloneable {
  private static final Logger LOG = Logger.getInstance(ParametersList.class);

  private static final Pattern PROPERTY_PATTERN = Pattern.compile("-D(\\S+?)=(.+)");

  private List<String> myParameters = new ArrayList<String>();
  private Map<String, String> myMacroMap = null;
  private List<ParamsGroup> myGroups = new ArrayList<ParamsGroup>();

  public boolean hasParameter(String param) {
    return myParameters.contains(param);
  }

  public boolean hasProperty(String name) {
    return getPropertyValue(name) != null;
  }

  public @Nullable String getPropertyValue(String name) {
    String prefix = "-D" + name + "=";
    for (String parameter : myParameters) {

      if (parameter.startsWith(prefix)) {
        return parameter.substring(prefix.length());
      }
    }
    return null;
  }

  
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

  
  public String getParametersString() {
    return join(getList());
  }

  
  public String[] getArray() {
    return ArrayUtil.toStringArray(getList());
  }

  
  public List<String> getList() {
    if (myGroups.isEmpty()) {
      return Collections.unmodifiableList(myParameters);
    }

    List<String> params = new ArrayList<String>();
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

  public void prepend(String parameter) {
    addAt(0, parameter);
  }

  public void prependAll(String... parameter) {
    addAll(parameter);
    Collections.rotate(myParameters, parameter.length);
  }

  public void addParametersString(String parameters) {
    if (parameters != null) {
      String[] split = parse(parameters);
      for (String param : split) {
        add(param);
      }
    }
  }

  public void add(String parameter) {
    myParameters.add(expandMacros(parameter));
  }

  public ParamsGroup addParamsGroup(String groupId) {
    return addParamsGroup(new ParamsGroup(groupId));
  }

  public ParamsGroup addParamsGroup(ParamsGroup group) {
    myGroups.add(group);
    return group;
  }

  public ParamsGroup addParamsGroupAt(int index, ParamsGroup group) {
    myGroups.add(index, group);
    return group;
  }

  public ParamsGroup addParamsGroupAt(int index, String groupId) {
    ParamsGroup group = new ParamsGroup(groupId);
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

  public ParamsGroup getParamsGroupAt(int index) {
    return myGroups.get(index);
  }

  public @Nullable ParamsGroup getParamsGroup(String name) {
    for (ParamsGroup group : myGroups) {
      if (name.equals(group.getId())) return group;
    }
    return null;
  }

  public ParamsGroup removeParamsGroup(int index) {
    return myGroups.remove(index);
  }

  public void addAt(int index, String parameter) {
    myParameters.add(index, expandMacros(parameter));
  }

  public void defineProperty(String propertyName, String propertyValue) {
    addProperty(propertyName, propertyValue);
  }

  public void addProperty(String propertyName, String propertyValue) {
    //noinspection HardCodedStringLiteral
    myParameters.add("-D" + propertyName + "=" + propertyValue);
  }

  public void replaceOrAppend(String parameterPrefix, String replacement) {
    replaceOrAdd(parameterPrefix, replacement, myParameters.size());
  }

  private void replaceOrAdd(String parameterPrefix, String replacement, int position) {
    for (ListIterator<String> iterator = myParameters.listIterator(); iterator.hasNext(); ) {
      String param = iterator.next();
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

  public void replaceOrPrepend(String parameter, String replacement) {
    replaceOrAdd(parameter, replacement, 0);
  }

  public void set(int ind, String value) {
    myParameters.set(ind, value);
  }

  public String get(int ind) {
    return myParameters.get(ind);
  }

  public void add(String name, String value) {
    add(name);
    add(value);
  }

  public void addAll(String... parameters) {
    addAll(Arrays.asList(parameters));
  }

  public void addAll(List<String> parameters) {
    // Don't use myParameters.addAll(parameters) , it does not call expandMacros(parameter)
    for (String parameter : parameters) {
      add(parameter);
    }
  }

  @Override
  public ParametersList clone() {
    return copyTo(new ParametersList());
  }

  
  ParametersList copyTo(ParametersList target) {
    target.myParameters.addAll(myParameters);
    for (ParamsGroup group : myGroups) {
      target.myGroups.add(group.clone());
    }
    return target;
  }

  /**
   * @see ParametersListUtil#join(java.util.List)
   */
  
  public static String join(List<String> parameters) {
    return ParametersListUtil.join(parameters);
  }

  /**
   * @see ParametersListUtil#join(java.util.List)
   */
  
  public static String join(String... parameters) {
    return ParametersListUtil.join(parameters);
  }

  /**
   * @see ParametersListUtil#parseToArray(String)
   */
  
  public static String[] parse(String string) {
    return ParametersListUtil.parseToArray(string);
  }

  public String expandMacros(String text) {
    Map<String, String> macroMap = getMacroMap();
    Set<String> set = macroMap.keySet();
    for (String from : set) {
      String to = macroMap.get(from);
      text = StringUtil.replace(text, from, to, true);
    }
    return text;
  }

  private Map<String, String> getMacroMap() {
    if (myMacroMap == null) {
      // the insertion order is important for later iterations, so LinkedHashMap is used
      myMacroMap = new LinkedHashMap<String, String>();

      // ApplicationManager.getApplication() will return null if executed in ParameterListTest
      Application application = ApplicationManager.getApplication();
      if (application != null) {
        PathMacros pathMacros = PathMacros.getInstance();
        if (pathMacros != null) {
          for (String name : pathMacros.getUserMacroNames()) {
            String value = pathMacros.getValue(name);
            if (value != null) {
              myMacroMap.put("${" + name + "}", value);
            }
          }
        }
        Map<String, String> env = EnvironmentUtil.getEnvironmentMap();
        for (String name : env.keySet()) {
          String key = "${" + name + "}";
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
