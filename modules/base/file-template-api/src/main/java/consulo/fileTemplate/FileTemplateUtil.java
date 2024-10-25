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

package consulo.fileTemplate;

import consulo.annotation.DeprecationInfo;
import consulo.application.util.function.ThrowableComputable;
import consulo.language.codeStyle.CodeStyleSettingsManager;
import consulo.language.file.FileTypeManager;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.undoRedo.CommandProcessor;
import consulo.util.collection.primitive.ints.IntObjectMap;
import consulo.util.lang.ClassLoaderUtil;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.SimpleReference;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author MYakovlev
 */
public class FileTemplateUtil {
    private FileTemplateUtil() {
    }

    @Deprecated
    @RequiredUIAccess
    public static PsiElement createFromTemplate(
        @Nonnull final FileTemplate template,
        @Nullable final String fileName,
        @Nullable Properties props,
        @Nonnull final PsiDirectory directory
    )
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
    @RequiredUIAccess
    public static PsiElement createFromTemplate(
        @Nonnull final FileTemplate template,
        @Nullable final String fileName,
        @Nullable Map<String, Object> props,
        @Nonnull final PsiDirectory directory
    )
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

    @Deprecated
    @DeprecationInfo("Use #createFromTemplate with Map parameter")
    @RequiredUIAccess
    public static PsiElement createFromTemplate(
        @Nonnull final FileTemplate template,
        @Nullable String fileName,
        @Nullable Properties props,
        @Nonnull final PsiDirectory directory,
        @Nullable ClassLoader classLoader
    ) throws Exception {
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

    @RequiredUIAccess
    public static PsiElement createFromTemplate(
        @Nonnull final FileTemplate template,
        @Nullable String fileName,
        @Nullable Map<String, Object> additionalProperties,
        @Nonnull final PsiDirectory directory,
        @Nullable ClassLoader classLoader
    ) throws Exception {
        final Project project = directory.getProject();

        Map<String, Object> properties = new HashMap<>();
        FileTemplateManager.getInstance(project).fillDefaultVariables(properties);

        if (additionalProperties != null) {
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
        String[] dummyRefs = template.getUnsetAttributes(properties, true, project);
        for (String dummyRef : dummyRefs) {
            properties.put(dummyRef, "");
        }

        handler.prepareProperties(properties);

        final String fileName_ = fileName;
        String mergedText = ClassLoaderUtil.runWithClassLoader(
            classLoader != null ? classLoader : FileTemplateUtil.class.getClassLoader(),
            (ThrowableComputable<String, IOException>)() -> template.getText(properties)
        );
        final String templateText = StringUtil.convertLineSeparators(mergedText);
        final SimpleReference<Exception> commandException = new SimpleReference<>();
        final SimpleReference<PsiElement> result = new SimpleReference<>();
        CommandProcessor.getInstance().newCommand(() -> {
                try {
                    result.set(handler.createFromTemplate(project, directory, fileName_, template, templateText, properties));
                }
                catch (Exception ex) {
                    commandException.set(ex);
                }
            })
            .withProject(project)
            .withName(handler.commandName(template))
            .executeInWriteAction();
        if (!commandException.isNull()) {
            throw commandException.get();
        }
        return result.get();
    }

    @Nonnull
    public static CreateFromTemplateHandler findHandler(final FileTemplate template) {
        CreateFromTemplateHandler templateHandler =
            CreateFromTemplateHandler.EP_NAME.computeSafeIfAny(it -> it.handlesTemplate(template) ? it : null);
        if (templateHandler != null) {
            return templateHandler;
        }
        throw new IllegalArgumentException("DefaultCreateFromTemplateHandler not registered");
    }

    public static void fillDefaultProperties(final Map<String, Object> props, final PsiDirectory directory) {
        DefaultTemplatePropertiesProvider.EP_NAME.forEachExtensionSafe(it -> it.fillProperties(directory, props));
    }

    public static String indent(String methodText, Project project, FileType fileType) {
        int indent = CodeStyleSettingsManager.getSettings(project).getIndentSize(fileType);
        return methodText.replaceAll("\n", "\n" + StringUtil.repeatSymbol(' ', indent));
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

    public static Pattern getTemplatePattern(
        @Nonnull FileTemplate template,
        @Nonnull Project project,
        @Nonnull IntObjectMap<String> offsetToProperty
    ) {
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
