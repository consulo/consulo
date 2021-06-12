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

package com.intellij.ide.fileTemplates;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.fileTemplates.impl.CustomFileTemplate;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.UnknownFileType;
import com.intellij.openapi.fileTypes.ex.FileTypeManagerEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.ClassLoaderUtil;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.util.ArrayUtil;
import consulo.annotation.DeprecationInfo;
import consulo.logging.Logger;
import consulo.ui.image.Image;
import consulo.util.collection.primitive.ints.IntObjectMap;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.runtime.parser.ParseException;
import org.apache.velocity.runtime.parser.Token;
import org.apache.velocity.runtime.parser.node.*;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author MYakovlev
 */
public class FileTemplateUtil {
  private static final Logger LOG = Logger.getInstance(FileTemplateUtil.class);
  private static final CreateFromTemplateHandler ourDefaultCreateFromTemplateHandler = new DefaultCreateFromTemplateHandler();

  private FileTemplateUtil() {
  }

  @Deprecated
  @DeprecationInfo("Use #calculateAttributes with Map parameter")
  public static String[] calculateAttributes(String templateContent, Properties properties, boolean includeDummies, Project project) throws ParseException {
    Set<String> propertiesNames = new HashSet<>();
    for (Enumeration e = properties.propertyNames(); e.hasMoreElements(); ) {
      propertiesNames.add((String)e.nextElement());
    }
    return calculateAttributes(templateContent, propertiesNames, includeDummies, project);
  }

  public static String[] calculateAttributes(String templateContent, Map<String, Object> properties, boolean includeDummies, Project project) throws ParseException {
    return calculateAttributes(templateContent, properties.keySet(), includeDummies, project);
  }

  private static String[] calculateAttributes(String templateContent, Set<String> propertiesNames, boolean includeDummies, Project project) throws ParseException {
    final Set<String> unsetAttributes = new LinkedHashSet<>();
    final Set<String> definedAttributes = new HashSet<>();
    SimpleNode template = VelocityWrapper.parse(new StringReader(templateContent), "MyTemplate");
    collectAttributes(unsetAttributes, definedAttributes, template, propertiesNames, includeDummies, new HashSet<>(), project);
    for (String definedAttribute : definedAttributes) {
      unsetAttributes.remove(definedAttribute);
    }
    return ArrayUtil.toStringArray(unsetAttributes);
  }

  private static void collectAttributes(Set<String> referenced,
                                        Set<String> defined,
                                        Node apacheNode,
                                        final Set<String> propertiesNames,
                                        final boolean includeDummies,
                                        Set<String> visitedIncludes,
                                        Project project) throws ParseException {
    int childCount = apacheNode.jjtGetNumChildren();
    for (int i = 0; i < childCount; i++) {
      Node apacheChild = apacheNode.jjtGetChild(i);
      collectAttributes(referenced, defined, apacheChild, propertiesNames, includeDummies, visitedIncludes, project);
      if (apacheChild instanceof ASTReference) {
        ASTReference apacheReference = (ASTReference)apacheChild;
        String s = apacheReference.literal();
        s = referenceToAttribute(s, includeDummies);
        if (s != null && s.length() > 0 && !propertiesNames.contains(s)) {
          referenced.add(s);
        }
      }
      else if (apacheChild instanceof ASTSetDirective) {
        ASTReference lhs = (ASTReference)apacheChild.jjtGetChild(0);
        String attr = referenceToAttribute(lhs.literal(), false);
        if (attr != null) {
          defined.add(attr);
        }
      }
      else if (apacheChild instanceof ASTDirective && "parse".equals(((ASTDirective)apacheChild).getDirectiveName()) && apacheChild.jjtGetNumChildren() == 1) {
        Node literal = apacheChild.jjtGetChild(0);
        if (literal instanceof ASTStringLiteral && literal.jjtGetNumChildren() == 0) {
          Token firstToken = literal.getFirstToken();
          if (firstToken != null) {
            String s = StringUtil.unquoteString(firstToken.toString());
            final FileTemplate includedTemplate = FileTemplateManager.getInstance(project).getTemplate(s);
            if (includedTemplate != null && visitedIncludes.add(s)) {
              SimpleNode template = VelocityWrapper.parse(new StringReader(includedTemplate.getText()), "MyTemplate");
              collectAttributes(referenced, defined, template, propertiesNames, includeDummies, visitedIncludes, project);
            }
          }
        }
      }
    }
  }


  /**
   * Removes each two leading '\', removes leading $, removes {}
   * Examples:
   * $qqq   -> qqq
   * \$qqq  -> qqq if dummy attributes are collected too, null otherwise
   * \\$qqq -> qqq
   * ${qqq} -> qqq
   */
  @Nullable
  private static String referenceToAttribute(String attrib, boolean includeDummies) {
    while (attrib.startsWith("\\\\")) {
      attrib = attrib.substring(2);
    }
    if (attrib.startsWith("\\$")) {
      if (includeDummies) {
        attrib = attrib.substring(1);
      }
      else {
        return null;
      }
    }
    if (!StringUtil.startsWithChar(attrib, '$')) {
      return null;
    }
    attrib = attrib.substring(1);
    if (StringUtil.startsWithChar(attrib, '{')) {
      String cleanAttribute = null;
      for (int i = 1; i < attrib.length(); i++) {
        char currChar = attrib.charAt(i);
        if (currChar == '{' || currChar == '.') {
          // Invalid match
          cleanAttribute = null;
          break;
        }
        else if (currChar == '}') {
          // Valid match
          cleanAttribute = attrib.substring(1, i);
          break;
        }
      }
      attrib = cleanAttribute;
    }
    else {
      for (int i = 0; i < attrib.length(); i++) {
        char currChar = attrib.charAt(i);
        if (currChar == '{' || currChar == '}' || currChar == '.') {
          attrib = attrib.substring(0, i);
          break;
        }
      }
    }
    return attrib;
  }

  public static String mergeTemplate(Map attributes, String content, boolean useSystemLineSeparators) throws IOException {
    VelocityContext context = new VelocityContext();
    for (final Object o : attributes.keySet()) {
      String name = (String)o;
      context.put(name, attributes.get(name));
    }
    return mergeTemplate(content, context, useSystemLineSeparators);
  }

  public static String mergeTemplate(Properties attributes, String content, boolean useSystemLineSeparators) throws IOException {
    VelocityContext context = new VelocityContext();
    Enumeration<?> names = attributes.propertyNames();
    while (names.hasMoreElements()) {
      String name = (String)names.nextElement();
      context.put(name, attributes.getProperty(name));
    }
    return mergeTemplate(content, context, useSystemLineSeparators);
  }

  private static String mergeTemplate(String templateContent, final VelocityContext context, boolean useSystemLineSeparators) throws IOException {
    final StringWriter stringWriter = new StringWriter();
    try {
      VelocityWrapper.evaluate(null, context, stringWriter, templateContent);
    }
    catch (final VelocityException e) {
      LOG.error("Error evaluating template:\n" + templateContent, e);
      ApplicationManager.getApplication().invokeLater(() -> Messages.showErrorDialog(IdeBundle.message("error.parsing.file.template", e.getMessage()), IdeBundle.message("title.velocity.error")));
    }
    final String result = stringWriter.toString();

    if (useSystemLineSeparators) {
      final String newSeparator = CodeStyleSettingsManager.getSettings(ProjectManagerEx.getInstanceEx().getDefaultProject()).getLineSeparator();
      if (!"\n".equals(newSeparator)) {
        return StringUtil.convertLineSeparators(result, newSeparator);
      }
    }

    return result;
  }

  @Deprecated
  public static PsiElement createFromTemplate(@Nonnull final FileTemplate template, @NonNls @Nullable final String fileName, @Nullable Properties props, @Nonnull final PsiDirectory directory)
          throws Exception {
    Map<String, Object> map;
    if (props != null) {
      map = new HashMap<>();
      putAll(map, props);
    }
    else {
      map = null;
    }
    return createFromTemplate(template, fileName, map, directory, null);
  }

  @Nonnull
  public static PsiElement createFromTemplate(@Nonnull final FileTemplate template, @NonNls @Nullable final String fileName, @Nullable Map<String, Object> props, @Nonnull final PsiDirectory directory)
          throws Exception {
    Map<String, Object> map;
    if (props != null) {
      map = new HashMap<>(props);
    }
    else {
      map = null;
    }
    return createFromTemplate(template, fileName, map, directory, null);
  }

  @Nonnull
  public static FileTemplate createTemplate(@Nonnull String prefName, @Nonnull String extension, @Nonnull String content, FileTemplate[] templates) {
    final Set<String> names = new HashSet<>();
    for (FileTemplate template : templates) {
      names.add(template.getName());
    }
    String name = prefName;
    int i = 0;
    while (names.contains(name)) {
      name = prefName + " (" + ++i + ")";
    }
    final FileTemplate newTemplate = new CustomFileTemplate(name, extension);
    newTemplate.setText(content);
    return newTemplate;
  }

  @Deprecated
  @DeprecationInfo("Use #createFromTemplate with Map parameter")
  public static PsiElement createFromTemplate(@Nonnull final FileTemplate template,
                                              @NonNls @Nullable String fileName,
                                              @Nullable Properties props,
                                              @Nonnull final PsiDirectory directory,
                                              @Nullable ClassLoader classLoader) throws Exception {
    Map<String, Object> map;
    if (props != null) {
      map = new HashMap<>();
      putAll(map, props);
    }
    else {
      map = null;
    }
    return createFromTemplate(template, fileName, map, directory, classLoader);
  }

  public static PsiElement createFromTemplate(@Nonnull final FileTemplate template,
                                              @NonNls @Nullable String fileName,
                                              @Nullable Map<String, Object> additionalProperties,
                                              @Nonnull final PsiDirectory directory,
                                              @Nullable ClassLoader classLoader) throws Exception {
    final Project project = directory.getProject();

    Map<String, Object> properties = new HashMap<>();
    FileTemplateManager.getInstance(project).fillDefaultVariables(properties);

    if(additionalProperties != null) {
      properties.putAll(additionalProperties);
    }

    FileTemplateManager.getInstance(project).addRecentName(template.getName());
    fillDefaultProperties(properties, directory);

    final CreateFromTemplateHandler handler = findHandler(template);
    if (fileName != null && properties.get(FileTemplate.ATTRIBUTE_NAME) == null) {
      properties.put(FileTemplate.ATTRIBUTE_NAME, fileName);
    }
    else if (fileName == null && handler.isNameRequired()) {
      fileName = (String)properties.get(FileTemplate.ATTRIBUTE_NAME);
      if (fileName == null) {
        throw new Exception("File name must be specified");
      }
    }

    //Set escaped references to dummy values to remove leading "\" (if not already explicitely set)
    String[] dummyRefs = calculateAttributes(template.getText(), properties, true, project);
    for (String dummyRef : dummyRefs) {
      properties.put(dummyRef, "");
    }

    handler.prepareProperties(properties);

    final String fileName_ = fileName;
    String mergedText =
            ClassLoaderUtil.runWithClassLoader(classLoader != null ? classLoader : FileTemplateUtil.class.getClassLoader(), (ThrowableComputable<String, IOException>)() -> template.getText(properties));
    final String templateText = StringUtil.convertLineSeparators(mergedText);
    final Exception[] commandException = new Exception[1];
    final PsiElement[] result = new PsiElement[1];
    CommandProcessor.getInstance().executeCommand(project, () -> ApplicationManager.getApplication().runWriteAction(() -> {
      try {
        result[0] = handler.createFromTemplate(project, directory, fileName_, template, templateText, properties);
      }
      catch (Exception ex) {
        commandException[0] = ex;
      }
    }), handler.commandName(template), null);
    if (commandException[0] != null) {
      throw commandException[0];
    }
    return result[0];
  }

  public static CreateFromTemplateHandler findHandler(final FileTemplate template) {
    for (CreateFromTemplateHandler handler : Extensions.getExtensions(CreateFromTemplateHandler.EP_NAME)) {
      if (handler.handlesTemplate(template)) {
        return handler;
      }
    }
    return ourDefaultCreateFromTemplateHandler;
  }

  public static void fillDefaultProperties(final Map<String, Object> props, final PsiDirectory directory) {
    final DefaultTemplatePropertiesProvider[] providers = Extensions.getExtensions(DefaultTemplatePropertiesProvider.EP_NAME);
    for (DefaultTemplatePropertiesProvider provider : providers) {
      provider.fillProperties(directory, props);
    }
  }

  public static String indent(String methodText, Project project, FileType fileType) {
    int indent = CodeStyleSettingsManager.getSettings(project).getIndentSize(fileType);
    return methodText.replaceAll("\n", "\n" + StringUtil.repeatSymbol(' ', indent));
  }


  public static boolean canCreateFromTemplate(PsiDirectory[] dirs, FileTemplate template) {
    FileType fileType = FileTypeManagerEx.getInstanceEx().getFileTypeByExtension(template.getExtension());
    if (fileType.equals(UnknownFileType.INSTANCE)) return false;
    CreateFromTemplateHandler handler = findHandler(template);
    return handler.canCreate(dirs);
  }

  @Nullable
  public static Image getIcon(@Nonnull FileTemplate fileTemplate) {
    String extension = fileTemplate.getExtension();
    return FileTypeManager.getInstance().getFileTypeByExtension(extension).getIcon();
  }

  @Deprecated
  public static void putAll(final Map<String, Object> props, final Properties p) {
    for (Enumeration<?> e = p.propertyNames(); e.hasMoreElements(); ) {
      String s = (String)e.nextElement();
      props.put(s, p.getProperty(s));
    }
  }

  @Nonnull
  public static Map<String, Object> convert2Map(final Properties p) {
    Map<String, Object> map = new HashMap<>();
    for (Enumeration<?> e = p.propertyNames(); e.hasMoreElements(); ) {
      String s = (String)e.nextElement();
      map.put(s, p.getProperty(s));
    }
    return map;
  }

  public static Pattern getTemplatePattern(@Nonnull FileTemplate template, @Nonnull Project project, @Nonnull IntObjectMap<String> offsetToProperty) {
    String templateText = template.getText().trim();
    String regex = templateToRegex(templateText, offsetToProperty, project);
    regex = StringUtil.replace(regex, "with", "(?:with|by)");
    regex = ".*(" + regex + ").*";
    return Pattern.compile(regex, Pattern.DOTALL);
  }

  private static String templateToRegex(String text, IntObjectMap<String> offsetToProperty, Project project) {
    List<String> properties = new ArrayList<>(FileTemplateManager.getInstance(project).getDefaultVariables().keySet());
    properties.add(FileTemplate.ATTRIBUTE_PACKAGE_NAME);

    String regex = escapeRegexChars(text);
    // first group is a whole file header
    int groupNumber = 1;
    for (String name : properties) {
      String escaped = escapeRegexChars("${" + name + "}");
      boolean first = true;
      for (int i = regex.indexOf(escaped); i != -1 && i < regex.length(); i = regex.indexOf(escaped, i + 1)) {
        String replacement = first ? "([^\\n]*)" : "\\" + groupNumber;
        int delta = escaped.length() - replacement.length();
        int[] offs = offsetToProperty.keys();
        for (int off : offs) {
          if (off > i) {
            String prop = offsetToProperty.remove(off);
            offsetToProperty.put(off - delta, prop);
          }
        }
        offsetToProperty.put(i, name);
        regex = regex.substring(0, i) + replacement + regex.substring(i + escaped.length());
        if (first) {
          groupNumber++;
          first = false;
        }
      }
    }
    return regex;
  }

  private static String escapeRegexChars(String regex) {
    regex = StringUtil.replace(regex, "|", "\\|");
    regex = StringUtil.replace(regex, ".", "\\.");
    regex = StringUtil.replace(regex, "*", "\\*");
    regex = StringUtil.replace(regex, "+", "\\+");
    regex = StringUtil.replace(regex, "?", "\\?");
    regex = StringUtil.replace(regex, "$", "\\$");
    regex = StringUtil.replace(regex, "(", "\\(");
    regex = StringUtil.replace(regex, ")", "\\)");
    regex = StringUtil.replace(regex, "[", "\\[");
    regex = StringUtil.replace(regex, "]", "\\]");
    regex = StringUtil.replace(regex, "{", "\\{");
    regex = StringUtil.replace(regex, "}", "\\}");
    return regex;
  }
}
