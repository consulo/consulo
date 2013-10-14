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
package org.jetbrains.plugins.groovy.highlighter;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import icons.JetgroovyIcons;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author ven
 */
public class GroovyColorsAndFontsPage implements ColorSettingsPage {
  @NotNull
  public String getDisplayName() {
    return "Groovy";
  }

  @Nullable
  public Icon getIcon() {
    return JetgroovyIcons.Groovy.Groovy_16x16;
  }

  @NotNull
  public AttributesDescriptor[] getAttributeDescriptors() {
    return ATTRS;
  }

  private static final AttributesDescriptor[] ATTRS =
    new AttributesDescriptor[]{
      new AttributesDescriptor("Line comment", GroovyHighlighterColors.LINE_COMMENT),
      new AttributesDescriptor("Block comment", GroovyHighlighterColors.BLOCK_COMMENT),
      new AttributesDescriptor("Groovydoc comment", GroovyHighlighterColors.DOC_COMMENT_CONTENT),
      new AttributesDescriptor("Groovydoc tag", GroovyHighlighterColors.DOC_COMMENT_TAG),
      new AttributesDescriptor("Keyword", GroovyHighlighterColors.KEYWORD),
      new AttributesDescriptor("Number", GroovyHighlighterColors.NUMBER),
      new AttributesDescriptor("GString", GroovyHighlighterColors.GSTRING),
      new AttributesDescriptor("String", GroovyHighlighterColors.STRING),
      new AttributesDescriptor("Braces", GroovyHighlighterColors.BRACES),
      new AttributesDescriptor("Brackets", GroovyHighlighterColors.BRACKETS),
      new AttributesDescriptor("Parentheses", GroovyHighlighterColors.PARENTHESES),
      new AttributesDescriptor("Operation sign", GroovyHighlighterColors.OPERATION_SIGN),
      new AttributesDescriptor("Bad character", GroovyHighlighterColors.BAD_CHARACTER),
      //new AttributesDescriptor("Wrong string literal", DefaultHighlighter.WRONG_STRING),
      new AttributesDescriptor("Unresolved reference access", GroovyHighlighterColors.UNRESOLVED_ACCESS),
      new AttributesDescriptor("List/map to object conversion", GroovyHighlighterColors.LITERAL_CONVERSION),
      new AttributesDescriptor("Annotation", GroovyHighlighterColors.ANNOTATION),
      new AttributesDescriptor("Local variable", GroovyHighlighterColors.LOCAL_VARIABLE),
      new AttributesDescriptor("Reassigned local variable", GroovyHighlighterColors.REASSIGNED_LOCAL_VARIABLE),
      new AttributesDescriptor("Parameter", GroovyHighlighterColors.PARAMETER),
      new AttributesDescriptor("Reassigned parameter", GroovyHighlighterColors.REASSIGNED_PARAMETER),
      new AttributesDescriptor("Static field", GroovyHighlighterColors.STATIC_FIELD),
      new AttributesDescriptor("Instance field", GroovyHighlighterColors.INSTANCE_FIELD),
      new AttributesDescriptor("Constructor call", GroovyHighlighterColors.CONSTRUCTOR_CALL),
      new AttributesDescriptor("Instance method call", GroovyHighlighterColors.METHOD_CALL),
      new AttributesDescriptor("Static method call", GroovyHighlighterColors.STATIC_METHOD_ACCESS),
      new AttributesDescriptor("Method declaration", GroovyHighlighterColors.METHOD_DECLARATION),
      new AttributesDescriptor("Constructor declaration", GroovyHighlighterColors.CONSTRUCTOR_DECLARATION),
      new AttributesDescriptor("Class reference", GroovyHighlighterColors.CLASS_REFERENCE),
      new AttributesDescriptor("Type parameter reference", GroovyHighlighterColors.TYPE_PARAMETER),
      new AttributesDescriptor("Map key accessed as a property", GroovyHighlighterColors.MAP_KEY),
      new AttributesDescriptor("Instance property reference", GroovyHighlighterColors.INSTANCE_PROPERTY_REFERENCE),
      new AttributesDescriptor("Static property reference", GroovyHighlighterColors.STATIC_PROPERTY_REFERENCE),
      new AttributesDescriptor("Valid string escape", GroovyHighlighterColors.VALID_STRING_ESCAPE),
      new AttributesDescriptor("Invalid string escape", GroovyHighlighterColors.INVALID_STRING_ESCAPE),
      new AttributesDescriptor("Label", GroovyHighlighterColors.LABEL),
    };

  @NotNull
  public ColorDescriptor[] getColorDescriptors() {
    return new ColorDescriptor[0];
  }

  @NotNull
  public SyntaxHighlighter getHighlighter() {
    return new GroovySyntaxHighlighter();
  }

  @NonNls
  @NotNull
  public String getDemoText() {
    return "<keyword>import</keyword> <classref>javax.swing.JPanel</classref>\n" +
           "  ### \n" +
           "<gdoc>/**\n" +
           " * This is Groovydoc comment\n" +
           " * <doctag>@see</doctag> <classref>java.lang.String</classref>#equals\n" +
           " */</gdoc>\n" +
           "<annotation>@SpecialBean</annotation> \n" +
           "<keyword>class</keyword> <classref>Demo</classref> {\n" +
           "  <keyword>public</keyword> <constructor>Demo</constructor>() {}\n" +
           "  <keyword>def</keyword> <instfield>property</instfield>\n" +
           "//This is a line comment\n" +
           "/* This is a block comment */\n" +
           "  <keyword>static</keyword> <keyword>def</keyword> <method>foo</method>(<keyword>int</keyword> <param>i</param>, <keyword>int</keyword> <reParam>j</reParam>) {\n" +
           "    <classref>Map</classref> <local>map</local> = [<mapkey>key</mapkey>:1, <mapkey>b</mapkey>:2]\n" +
           "    <reParam>j</reParam>++\n" +
           "    print map.<mapkey>key</mapkey>\n" +
           "    return [<param>i</param>, <instfield>property</instfield>]\n" +
           "  }\n" +
           "  <keyword>static</keyword> <keyword>def</keyword> <statfield>panel</statfield> = <keyword>new</keyword> <classref>JPanel</classref>()\n" +
           "  <keyword>def</keyword> <<typeparam>T</typeparam>> foo() {" +
           "    <typeparam>T</typeparam> list = <keyword>null</keyword>" +
           "  }\n" +
           "}\n" +
           "\n" +
           "<classref>Demo</classref>.<statfield>panel</statfield>.size = " +
           "<classref>Demo</classref>.<statmet>foo</statmet>(\"123${456}789\".<instmet>toInteger</instmet>()) \n" +
           "'JetBrains'.<instmet>matches</instmet>(/Jw+Bw+/) \n" +
           "<keyword>def</keyword> <local>x</local>=1 + <unresolved>unresolved</unresolved>\n" +
           "<label>label</label>:<keyword>def</keyword> <reLocal>f1</reLocal> = []\n" +
           "<reLocal>f1</reLocal> = [2]\n" +
           "<classref>File</classref> <local>f</local>=<literal>[</literal>'path'<literal>]</literal>\n" +
           "<instmet>print</instmet> <keyword>new</keyword> <constructorCall>Demo</constructorCall>().<prop>property</prop>\n" +
           "<instmet>print</instmet> '<validescape>\\n</validescape> <invalidescape>\\x</invalidescape>'"

      ;
  }

  @Nullable
  public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
    Map<String, TextAttributesKey> map = new HashMap<String, TextAttributesKey>();
    map.put("keyword", GroovyHighlighterColors.KEYWORD);
    map.put("annotation", GroovyHighlighterColors.ANNOTATION);
    map.put("statmet", GroovyHighlighterColors.STATIC_METHOD_ACCESS);
    map.put("instmet", GroovyHighlighterColors.METHOD_CALL);
    map.put("constructorCall", GroovyHighlighterColors.CONSTRUCTOR_CALL);
    map.put("statfield", GroovyHighlighterColors.STATIC_FIELD);
    map.put("instfield", GroovyHighlighterColors.INSTANCE_FIELD);
    map.put("gdoc", GroovyHighlighterColors.DOC_COMMENT_CONTENT);
    map.put("doctag", GroovyHighlighterColors.DOC_COMMENT_TAG);
    map.put("unresolved", GroovyHighlighterColors.UNRESOLVED_ACCESS);
    map.put("classref", GroovyHighlighterColors.CLASS_REFERENCE);
    map.put("typeparam", GroovyHighlighterColors.TYPE_PARAMETER);
    map.put("literal", GroovyHighlighterColors.LITERAL_CONVERSION);
    map.put("mapkey", GroovyHighlighterColors.MAP_KEY);
    map.put("prop", GroovyHighlighterColors.INSTANCE_PROPERTY_REFERENCE);
    map.put("staticprop", GroovyHighlighterColors.STATIC_PROPERTY_REFERENCE);
    map.put("validescape", GroovyHighlighterColors.VALID_STRING_ESCAPE);
    map.put("invalidescape", GroovyHighlighterColors.INVALID_STRING_ESCAPE);
    map.put("local", GroovyHighlighterColors.LOCAL_VARIABLE);
    map.put("reLocal", GroovyHighlighterColors.REASSIGNED_LOCAL_VARIABLE);
    map.put("param", GroovyHighlighterColors.PARAMETER);
    map.put("reParam", GroovyHighlighterColors.REASSIGNED_PARAMETER);
    map.put("method", GroovyHighlighterColors.METHOD_DECLARATION);
    map.put("constructor", GroovyHighlighterColors.CONSTRUCTOR_DECLARATION);
    map.put("label", GroovyHighlighterColors.LABEL);
    return map;
  }
}
