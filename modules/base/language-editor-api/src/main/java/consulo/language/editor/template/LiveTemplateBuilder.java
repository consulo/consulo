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
package consulo.language.editor.template;

import consulo.logging.Logger;

import jakarta.annotation.Nonnull;
import java.util.*;

/**
 * @author Eugene.Kudelevsky
 */
public final class LiveTemplateBuilder {
  private static final String END_PREFIX = "____END";
  private static final Logger LOGGER = Logger.getInstance(LiveTemplateBuilder.class);

  private final StringBuilder myText = new StringBuilder();
  private final List<Variable> myVariables = new ArrayList<>();
  private final Set<String> myVarNames = new HashSet<>();
  private final List<VarOccurence> myVariableOccurrences = new ArrayList<>();
  private final List<Marker> myMarkers = new ArrayList<>();
  private final int mySegmentLimit;
  private String myLastEndVarName;
  private boolean myIsToReformat = false;

  public LiveTemplateBuilder(int segmentLimit) {
    mySegmentLimit = segmentLimit;
  }

  public void setIsToReformat(boolean isToReformat) {
    myIsToReformat = isToReformat;
  }

  public CharSequence getText() {
    return myText;
  }

  public static boolean isEndVariable(@Nonnull String name) {
    return name.startsWith(END_PREFIX);
  }

  private static class VarOccurence {
    String myName;
    int myOffset;

    private VarOccurence(String name, int offset) {
      myName = name;
      myOffset = offset;
    }
  }

  public boolean findVarOccurence(String name) {
    for (VarOccurence occurence : myVariableOccurrences) {
      if (occurence.myName.equals(name)) {
        return true;
      }
    }
    return false;
  }

  @Nonnull
  public Template buildTemplate() {
    List<Variable> variables = getListWithLimit(myVariables);
    if (!findVarOccurence(Template.END)) {
      if (myLastEndVarName == null) {
        for (Variable variable : variables) {
          if (isEndVariable(variable.getName())) {
            myLastEndVarName = variable.getName();
            break;
          }
        }
      }
      if (myLastEndVarName != null) {
        int endOffset = -1;
        Iterator<VarOccurence> it = myVariableOccurrences.iterator();
        while (it.hasNext()) {
          VarOccurence occurence = it.next();
          if (occurence.myName.equals(myLastEndVarName)) {
            endOffset = occurence.myOffset;
            break;
          }
        }
        if (endOffset >= 0) {
          for (Iterator<Variable> it1 = variables.iterator(); it1.hasNext(); ) {
            Variable variable = it1.next();
            if (myLastEndVarName.equals(variable.getName()) && variable.isAlwaysStopAt()) {
              it.remove();
              it1.remove();
            }
          }
          myVariableOccurrences.add(new VarOccurence(Template.END, endOffset));
        }
      }
    }
    Template template = TemplateBuilderFactory.getInstance().createRawTemplate("", "");
    for (Variable variable : variables) {
      template.addVariable(variable.getName(), variable.getExpressionString(), variable.getDefaultValueString(), variable.isAlwaysStopAt());
    }

    List<VarOccurence> variableOccurrences = getListWithLimit(myVariableOccurrences);
    Collections.sort(variableOccurrences, (o1, o2) -> {
      if (o1.myOffset < o2.myOffset) {
        return -1;
      }
      if (o1.myOffset > o2.myOffset) {
        return 1;
      }
      return 0;
    });
    int last = 0;
    for (VarOccurence occurence : variableOccurrences) {
      template.addTextSegment(myText.substring(last, occurence.myOffset));
      template.addVariableSegment(occurence.myName);
      last = occurence.myOffset;
    }
    template.addTextSegment(myText.substring(last));
    template.setToReformat(myIsToReformat);
    return template;
  }

  private <T> List<T> getListWithLimit(List<T> list) {
    if (mySegmentLimit == 0) {
      return Collections.emptyList();
    }
    if (mySegmentLimit > 0 && list.size() > mySegmentLimit) {
      LOGGER.warn("Template with more than " + mySegmentLimit + " segments had been build (" + list.size() + "). Text: " + myText);
      return list.subList(0, Math.min(list.size(), mySegmentLimit));
    }
    return list;
  }

  public void insertText(int offset, String text, boolean disableEndVariable) {
    if (disableEndVariable) {
      String varName = null;
      for (VarOccurence occurence : myVariableOccurrences) {
        if (!isEndVariable(occurence.myName)) {
          continue;
        }
        if (occurence.myOffset == offset) {
          varName = occurence.myName;
          break;
        }
      }
      if (varName != null) {
        for (Variable variable : myVariables) {
          if (varName.equals(variable.getName())) {
            variable.setAlwaysStopAt(false);
            variable.setDefaultValueString("\"\"");
            break;
          }
        }
      }
    }
    int delta = text.length();
    for (VarOccurence occurence : myVariableOccurrences) {
      if (occurence.myOffset > offset || !disableEndVariable && occurence.myOffset == offset) {
        occurence.myOffset += delta;
      }
    }
    myText.insert(offset, text);
    updateMarkers(offset, text);
  }

  public int length() {
    return myText.length();
  }

  private void updateMarkers(int offset, String text) {
    for (Marker marker : myMarkers) {
      if (offset < marker.getStartOffset()) {
        marker.myStartOffset += text.length();
      }
      else if (offset <= marker.getEndOffset()) {
        marker.myEndOffset += text.length();
      }
    }
  }

  private String generateUniqueVarName(Set<String> existingNames, boolean end) {
    String prefix = end ? END_PREFIX : "VAR";
    int i = 0;
    while (myVarNames.contains(prefix + i) || existingNames.contains(prefix + i)) {
      i++;
    }
    return prefix + i;
  }

  public int insertTemplate(int offset, Template template, Map<String, String> predefinedVarValues) {
    myIsToReformat = myText.length() > 0 || template.isToReformat();
    removeEndVarAtOffset(offset);

    String text = template.getTemplateText();
    insertText(offset, text, false);
    Map<String, String> newVarNames = new HashMap<>();
    Set<String> oldVarNames = new HashSet<>();
    for (int i = 0; i < template.getVariableCount(); i++) {
      String varName = template.getVariableNameAt(i);
      oldVarNames.add(varName);
    }
    for (int i = 0; i < template.getVariableCount(); i++) {
      String varName = template.getVariableNameAt(i);
      if (!Template.INTERNAL_VARS_SET.contains(varName)) {
        if (predefinedVarValues != null && predefinedVarValues.containsKey(varName)) {
          continue;
        }
        String newVarName;
        if (myVarNames.contains(varName)) {
          oldVarNames.remove(varName);
          newVarName = generateUniqueVarName(oldVarNames, isEndVariable(varName));
          newVarNames.put(varName, newVarName);
          if (varName.equals(myLastEndVarName)) {
            myLastEndVarName = newVarName;
          }
        }
        else {
          newVarName = varName;
        }
        Variable var = new Variable(newVarName, template.getExpressionStringAt(i), template.getDefaultValueStringAt(i), template.isAlwaysStopAt(i));
        myVariables.add(var);
        myVarNames.add(newVarName);
      }
    }
    int end = -1;

    for (int i = 0; i < template.getSegmentsCount(); i++) {
      String segmentName = template.getSegmentName(i);
      int localOffset = template.getSegmentOffset(i);
      if (Template.END.equals(segmentName)) {
        end = offset + localOffset;
      }
      else {
        if (predefinedVarValues != null && predefinedVarValues.containsKey(segmentName)) {
          String value = predefinedVarValues.get(segmentName);
          insertText(offset + localOffset, value, false);
          offset += value.length();
          continue;
        }
        if (newVarNames.containsKey(segmentName)) {
          segmentName = newVarNames.get(segmentName);
        }
        myVariableOccurrences.add(new VarOccurence(segmentName, offset + localOffset));
      }
    }
    int endOffset = end >= 0 ? end : offset + text.length();
    if (endOffset > 0 && endOffset != offset + text.length() && endOffset < myText.length() && !hasVarAtOffset(endOffset)) {
      myLastEndVarName = generateUniqueVarName(myVarNames, true);
      myVariables.add(new Variable(myLastEndVarName, "", "", true));
      myVarNames.add(myLastEndVarName);
      myVariableOccurrences.add(new VarOccurence(myLastEndVarName, endOffset));
    }
    return endOffset;
  }

  private void removeEndVarAtOffset(int offset) {
    for (Iterator<VarOccurence> it = myVariableOccurrences.iterator(); it.hasNext(); ) {
      VarOccurence occurence = it.next();
      if (!isEndVariable(occurence.myName)) {
        continue;
      }
      if (occurence.myOffset == offset) {
        it.remove();
        for (Iterator<Variable> it1 = myVariables.iterator(); it1.hasNext(); ) {
          Variable variable = it1.next();
          if (occurence.myName.equals(variable.getName())) {
            it1.remove();
          }
        }
      }
    }
  }

  private boolean hasVarAtOffset(int offset) {
    boolean flag = false;
    for (VarOccurence occurence : myVariableOccurrences) {
      if (occurence.myOffset == offset) {
        flag = true;
      }
    }
    return flag;
  }

  public Marker createMarker(int offset) {
    Marker marker = new Marker(offset, offset);
    myMarkers.add(marker);
    return marker;
  }

  public static class Marker {
    int myStartOffset;
    int myEndOffset;

    private Marker(int startOffset, int endOffset) {
      myStartOffset = startOffset;
      myEndOffset = endOffset;
    }

    public int getStartOffset() {
      return myStartOffset;
    }

    public int getEndOffset() {
      return myEndOffset;
    }
  }
}
