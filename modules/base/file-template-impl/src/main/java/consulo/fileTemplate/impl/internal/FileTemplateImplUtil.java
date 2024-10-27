/*
 * Copyright 2013-2022 consulo.io
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
package consulo.fileTemplate.impl.internal;

import consulo.annotation.DeprecationInfo;
import consulo.application.Application;
import consulo.fileTemplate.*;
import consulo.fileTemplate.localize.FileTemplateLocalize;
import consulo.language.codeStyle.CodeStyleSettingsManager;
import consulo.language.file.FileTypeManager;
import consulo.language.psi.PsiDirectory;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.ui.Alerts;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.UnknownFileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.runtime.parser.ParseException;
import org.apache.velocity.runtime.parser.Token;
import org.apache.velocity.runtime.parser.node.*;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;

/**
 * @author VISTALL
 * @since 27-Mar-22
 */
public class FileTemplateImplUtil {
    private static final Logger LOG = Logger.getInstance(FileTemplateImplUtil.class);

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

    @Nonnull
    public static FileTemplate createTemplate(
        @Nonnull String prefName,
        @Nonnull String extension,
        @Nonnull String content,
        FileTemplate[] templates
    ) {
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

    public static boolean canCreateFromTemplate(PsiDirectory[] dirs, FileTemplate template) {
        FileType fileType = FileTypeManager.getInstance().getFileTypeByExtension(template.getExtension());
        if (fileType.equals(UnknownFileType.INSTANCE)) {
            return false;
        }
        CreateFromTemplateHandler handler = FileTemplateUtil.findHandler(template);
        return handler.canCreate(dirs);
    }

    private static String mergeTemplate(String templateContent, final VelocityContext context, boolean useSystemLineSeparators)
        throws IOException {
        final StringWriter stringWriter = new StringWriter();
        try {
            VelocityWrapper.evaluate(null, context, stringWriter, templateContent);
        }
        catch (ParseErrorException e) {
            throw new FileTemplateParseException(e);
        }
        catch (final VelocityException e) {
            LOG.error("Error evaluating template:\n" + templateContent, e);
            Application.get().invokeLater(
                () -> Alerts.okError(FileTemplateLocalize.errorParsingFileTemplate(e.getMessage())).showAsync()
            );
        }
        final String result = stringWriter.toString();

        if (useSystemLineSeparators) {
            final String newSeparator =
                CodeStyleSettingsManager.getSettings(ProjectManager.getInstance().getDefaultProject()).getLineSeparator();
            if (!"\n".equals(newSeparator)) {
                return StringUtil.convertLineSeparators(result, newSeparator);
            }
        }

        return result;
    }

    @Deprecated
    @DeprecationInfo("Use #calculateAttributes with Map parameter")
    public static String[] calculateAttributes(
        String templateContent,
        Properties properties,
        boolean includeDummies,
        Project project
    ) throws FileTemplateParseException {
        Set<String> propertiesNames = new HashSet<>();
        for (Enumeration e = properties.propertyNames(); e.hasMoreElements(); ) {
            propertiesNames.add((String)e.nextElement());
        }
        return calculateAttributes(templateContent, propertiesNames, includeDummies, project);
    }

    public static String[] calculateAttributes(
        String templateContent,
        Map<String, Object> properties,
        boolean includeDummies,
        Project project
    ) throws FileTemplateParseException {
        return calculateAttributes(templateContent, properties.keySet(), includeDummies, project);
    }

    private static String[] calculateAttributes(
        String templateContent,
        Set<String> propertiesNames,
        boolean includeDummies,
        Project project
    ) throws FileTemplateParseException {
        try {
            final Set<String> unsetAttributes = new LinkedHashSet<>();
            final Set<String> definedAttributes = new HashSet<>();
            SimpleNode template = VelocityWrapper.parse(new StringReader(templateContent), "MyTemplate");
            collectAttributes(
                unsetAttributes,
                definedAttributes,
                template,
                propertiesNames,
                includeDummies,
                new HashSet<>(),
                project
            );
            for (String definedAttribute : definedAttributes) {
                unsetAttributes.remove(definedAttribute);
            }
            return ArrayUtil.toStringArray(unsetAttributes);
        }
        catch (ParseException e) {
            throw new FileTemplateParseException(e);
        }
    }

    private static void collectAttributes(
        Set<String> referenced,
        Set<String> defined,
        Node apacheNode,
        final Set<String> propertiesNames,
        final boolean includeDummies,
        Set<String> visitedIncludes,
        Project project
    ) throws ParseException {
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
            else if (
                apacheChild instanceof ASTDirective astDirective
                    && "parse".equals(astDirective.getDirectiveName())
                    && apacheChild.jjtGetNumChildren() == 1
            ) {
                Node literal = apacheChild.jjtGetChild(0);
                if (literal instanceof ASTStringLiteral && literal.jjtGetNumChildren() == 0) {
                    Token firstToken = literal.getFirstToken();
                    if (firstToken != null) {
                        String s = StringUtil.unquoteString(firstToken.toString());
                        final FileTemplate includedTemplate = FileTemplateManager.getInstance(project).getTemplate(s);
                        if (includedTemplate != null && visitedIncludes.add(s)) {
                            SimpleNode template =
                                VelocityWrapper.parse(new StringReader(includedTemplate.getText()), "MyTemplate");
                            collectAttributes(
                                referenced,
                                defined,
                                template,
                                propertiesNames,
                                includeDummies,
                                visitedIncludes,
                                project
                            );
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
}
