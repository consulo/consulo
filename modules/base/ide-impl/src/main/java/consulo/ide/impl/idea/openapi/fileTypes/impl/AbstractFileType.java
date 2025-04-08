// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.fileTypes.impl;

import consulo.component.persist.scheme.ExternalInfo;
import consulo.component.persist.scheme.ExternalizableScheme;
import consulo.execution.configuration.ui.SettingsEditor;
import consulo.ide.impl.idea.ide.highlighter.FileTypeRegistrator;
import consulo.ide.impl.idea.ide.highlighter.custom.impl.CustomFileTypeEditor;
import consulo.ide.impl.idea.openapi.fileTypes.UserFileType;
import consulo.ide.impl.idea.openapi.fileTypes.ex.ExternalizableFileType;
import consulo.util.lang.Comparing;
import consulo.ide.impl.idea.openapi.util.JDOMUtil;
import consulo.ide.impl.idea.util.ArrayUtilRt;
import consulo.ide.impl.idea.util.text.StringTokenizer;
import consulo.language.Commenter;
import consulo.language.custom.CustomSyntaxTableFileType;
import consulo.language.file.FileTypeManager;
import consulo.language.internal.custom.SyntaxTable;
import consulo.util.collection.SmartList;
import consulo.util.lang.Pair;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.virtualFileSystem.fileType.FileNameMatcher;
import consulo.virtualFileSystem.fileType.PlainTextLikeFileType;
import consulo.virtualFileSystem.fileType.matcher.ExactFileNameMatcher;
import consulo.virtualFileSystem.fileType.matcher.WildcardFileNameMatcher;
import consulo.virtualFileSystem.internal.matcher.ExtensionFileNameMatcherImpl;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.jetbrains.annotations.NonNls;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class AbstractFileType extends UserFileType<AbstractFileType> implements ExternalizableFileType, ExternalizableScheme, CustomSyntaxTableFileType, PlainTextLikeFileType {
    private static final String SEMICOLON = ";";
    protected SyntaxTable mySyntaxTable;
    private SyntaxTable myDefaultSyntaxTable;
    protected Commenter myCommenter;
    public static final String ELEMENT_HIGHLIGHTING = "highlighting";
    private static final String ELEMENT_OPTIONS = "options";
    private static final String ELEMENT_OPTION = "option";
    private static final String ATTRIBUTE_VALUE = "value";
    private static final String VALUE_LINE_COMMENT = "LINE_COMMENT";
    private static final String VALUE_COMMENT_START = "COMMENT_START";
    private static final String VALUE_COMMENT_END = "COMMENT_END";
    private static final String VALUE_HEX_PREFIX = "HEX_PREFIX";
    private static final String VALUE_NUM_POSTFIXES = "NUM_POSTFIXES";
    private static final String VALUE_HAS_BRACES = "HAS_BRACES";
    private static final String VALUE_HAS_BRACKETS = "HAS_BRACKETS";
    private static final String VALUE_HAS_PARENS = "HAS_PARENS";
    private static final String VALUE_HAS_STRING_ESCAPES = "HAS_STRING_ESCAPES";
    private static final String VALUE_LINE_COMMENT_AT_START = "LINE_COMMENT_AT_START";
    private static final String ELEMENT_KEYWORDS = "keywords";
    private static final String ATTRIBUTE_IGNORE_CASE = "ignore_case";
    private static final String ELEMENT_KEYWORD = "keyword";
    private static final String ELEMENT_KEYWORDS2 = "keywords2";
    private static final String ELEMENT_KEYWORDS3 = "keywords3";
    private static final String ELEMENT_KEYWORDS4 = "keywords4";
    private static final String ATTRIBUTE_NAME = "name";
    public static final String ELEMENT_EXTENSION_MAP = "extensionMap";

    private final ExternalInfo myExternalInfo = new ExternalInfo();

    public AbstractFileType(SyntaxTable syntaxTable) {
        mySyntaxTable = syntaxTable;
    }

    void initSupport() {
        for (FileTypeRegistrator registrator : FileTypeRegistrator.EP_NAME.getExtensionList()) {
            registrator.initFileType(this);
        }
    }

    @Override
    public SyntaxTable getSyntaxTable() {
        return mySyntaxTable;
    }

    public Commenter getCommenter() {
        return myCommenter;
    }

    public void setSyntaxTable(SyntaxTable syntaxTable) {
        mySyntaxTable = syntaxTable;
    }

    @Override
    public AbstractFileType clone() {
        return (AbstractFileType)super.clone();
    }

    @Nonnull
    @Override
    public String getName() {
        return getId();
    }

    @Override
    public void copyFrom(@Nonnull UserFileType newType) {
        super.copyFrom(newType);

        if (newType instanceof AbstractFileType abstractFileType) {
            mySyntaxTable = ((CustomSyntaxTableFileType)newType).getSyntaxTable();
            myExternalInfo.copy(abstractFileType.myExternalInfo);
        }
    }

    @Override
    public boolean isBinary() {
        return false;
    }

    @Override
    public void readExternal(@Nonnull Element typeElement) throws InvalidDataException {
        Element element = typeElement.getChild(ELEMENT_HIGHLIGHTING);
        if (element != null) {
            setSyntaxTable(readSyntaxTable(element));
        }
    }

    @Nonnull
    static SyntaxTable readSyntaxTable(@Nonnull Element root) {
        SyntaxTable table = new SyntaxTable();

        for (Element element : root.getChildren()) {
            if (ELEMENT_OPTIONS.equals(element.getName())) {
                for (Object o1 : element.getChildren(ELEMENT_OPTION)) {
                    Element e = (Element)o1;
                    String name = e.getAttributeValue(ATTRIBUTE_NAME);
                    String value = e.getAttributeValue(ATTRIBUTE_VALUE);
                    if (VALUE_LINE_COMMENT.equals(name)) {
                        table.setLineComment(value);
                    }
                    else if (VALUE_COMMENT_START.equals(name)) {
                        table.setStartComment(value);
                    }
                    else if (VALUE_COMMENT_END.equals(name)) {
                        table.setEndComment(value);
                    }
                    else if (VALUE_HEX_PREFIX.equals(name)) {
                        table.setHexPrefix(value);
                    }
                    else if (VALUE_NUM_POSTFIXES.equals(name)) {
                        table.setNumPostfixChars(value);
                    }
                    else if (VALUE_LINE_COMMENT_AT_START.equals(name)) {
                        table.lineCommentOnlyAtStart = Boolean.valueOf(value);
                    }
                    else if (VALUE_HAS_BRACES.equals(name)) {
                        table.setHasBraces(Boolean.valueOf(value));
                    }
                    else if (VALUE_HAS_BRACKETS.equals(name)) {
                        table.setHasBrackets(Boolean.valueOf(value));
                    }
                    else if (VALUE_HAS_PARENS.equals(name)) {
                        table.setHasParens(Boolean.valueOf(value));
                    }
                    else if (VALUE_HAS_STRING_ESCAPES.equals(name)) {
                        table.setHasStringEscapes(Boolean.valueOf(value));
                    }
                }
            }
            else if (ELEMENT_KEYWORDS.equals(element.getName())) {
                boolean ignoreCase = Boolean.valueOf(element.getAttributeValue(ATTRIBUTE_IGNORE_CASE));
                table.setIgnoreCase(ignoreCase);
                loadKeywords(element, table.getKeywords1());
            }
            else if (ELEMENT_KEYWORDS2.equals(element.getName())) {
                loadKeywords(element, table.getKeywords2());
            }
            else if (ELEMENT_KEYWORDS3.equals(element.getName())) {
                loadKeywords(element, table.getKeywords3());
            }
            else if (ELEMENT_KEYWORDS4.equals(element.getName())) {
                loadKeywords(element, table.getKeywords4());
            }
        }

        boolean DUMP_TABLE = false;
        if (DUMP_TABLE) {
            Element element = new Element("temp");
            writeTable(element, table);
            XMLOutputter outputter = JDOMUtil.createOutputter("\n");
            try {
                outputter.output((Element)element.getContent().get(0), System.out);
            }
            catch (IOException ignored) {
            }
        }
        return table;
    }

    private static void loadKeywords(Element element, Set<? super String> keywords) {
        String value = element.getAttributeValue(ELEMENT_KEYWORDS);
        if (value != null) {
            StringTokenizer tokenizer = new StringTokenizer(value, SEMICOLON);
            while (tokenizer.hasMoreElements()) {
                String keyword = tokenizer.nextToken().trim();
                if (!keyword.isEmpty()) {
                    keywords.add(keyword);
                }
            }
        }
        for (Object o1 : element.getChildren(ELEMENT_KEYWORD)) {
            keywords.add(((Element)o1).getAttributeValue(ATTRIBUTE_NAME));
        }
    }

    @Override
    public void writeExternal(@Nonnull Element element) {
        writeTable(element, getSyntaxTable());
    }

    private static void writeTable(@Nonnull Element element, @Nonnull SyntaxTable table) {
        Element highlightingElement = new Element(ELEMENT_HIGHLIGHTING);

        Element optionsElement = new Element(ELEMENT_OPTIONS);

        Element lineComment = new Element(ELEMENT_OPTION);
        lineComment.setAttribute(ATTRIBUTE_NAME, VALUE_LINE_COMMENT);
        lineComment.setAttribute(ATTRIBUTE_VALUE, table.getLineComment());
        optionsElement.addContent(lineComment);

        String commentStart = table.getStartComment();
        if (commentStart != null) {
            Element commentStartElement = new Element(ELEMENT_OPTION);
            commentStartElement.setAttribute(ATTRIBUTE_NAME, VALUE_COMMENT_START);
            commentStartElement.setAttribute(ATTRIBUTE_VALUE, commentStart);
            optionsElement.addContent(commentStartElement);
        }

        String endComment = table.getEndComment();

        if (endComment != null) {
            Element commentEndElement = new Element(ELEMENT_OPTION);
            commentEndElement.setAttribute(ATTRIBUTE_NAME, VALUE_COMMENT_END);
            commentEndElement.setAttribute(ATTRIBUTE_VALUE, endComment);
            optionsElement.addContent(commentEndElement);
        }

        String prefix = table.getHexPrefix();

        if (prefix != null) {
            Element hexPrefix = new Element(ELEMENT_OPTION);
            hexPrefix.setAttribute(ATTRIBUTE_NAME, VALUE_HEX_PREFIX);
            hexPrefix.setAttribute(ATTRIBUTE_VALUE, prefix);
            optionsElement.addContent(hexPrefix);
        }

        String chars = table.getNumPostfixChars();

        if (chars != null) {
            Element numPostfixes = new Element(ELEMENT_OPTION);
            numPostfixes.setAttribute(ATTRIBUTE_NAME, VALUE_NUM_POSTFIXES);
            numPostfixes.setAttribute(ATTRIBUTE_VALUE, chars);
            optionsElement.addContent(numPostfixes);
        }

        addElementOption(optionsElement, VALUE_HAS_BRACES, table.isHasBraces());
        addElementOption(optionsElement, VALUE_HAS_BRACKETS, table.isHasBrackets());
        addElementOption(optionsElement, VALUE_HAS_PARENS, table.isHasParens());
        addElementOption(optionsElement, VALUE_HAS_STRING_ESCAPES, table.isHasStringEscapes());
        addElementOption(optionsElement, VALUE_LINE_COMMENT_AT_START, table.lineCommentOnlyAtStart);

        highlightingElement.addContent(optionsElement);

        writeKeywords(table.getKeywords1(), ELEMENT_KEYWORDS, highlightingElement)
            .setAttribute(ATTRIBUTE_IGNORE_CASE, String.valueOf(table.isIgnoreCase()));
        writeKeywords(table.getKeywords2(), ELEMENT_KEYWORDS2, highlightingElement);
        writeKeywords(table.getKeywords3(), ELEMENT_KEYWORDS3, highlightingElement);
        writeKeywords(table.getKeywords4(), ELEMENT_KEYWORDS4, highlightingElement);

        element.addContent(highlightingElement);
    }

    private static void addElementOption(Element optionsElement, String valueHasParens, boolean hasParens) {
        if (!hasParens) {
            return;
        }

        Element supportParens = new Element(ELEMENT_OPTION);
        supportParens.setAttribute(ATTRIBUTE_NAME, valueHasParens);
        supportParens.setAttribute(ATTRIBUTE_VALUE, String.valueOf(true));
        optionsElement.addContent(supportParens);
    }

    private static Element writeKeywords(Set<String> keywords, String tagName, Element highlightingElement) {
        if (keywords.isEmpty() && !ELEMENT_KEYWORDS.equals(tagName)) {
            return null;
        }
        Element keywordsElement = new Element(tagName);
        String[] strings = ArrayUtilRt.toStringArray(keywords);
        Arrays.sort(strings);
        StringBuilder keywordsAttribute = new StringBuilder();

        for (String keyword : strings) {
            if (!keyword.contains(SEMICOLON)) {
                if (keywordsAttribute.length() != 0) {
                    keywordsAttribute.append(SEMICOLON);
                }
                keywordsAttribute.append(keyword);
            }
            else {
                Element e = new Element(ELEMENT_KEYWORD);
                e.setAttribute(ATTRIBUTE_NAME, keyword);
                keywordsElement.addContent(e);
            }
        }
        if (keywordsAttribute.length() != 0) {
            keywordsElement.setAttribute(ELEMENT_KEYWORDS, keywordsAttribute.toString());
        }
        highlightingElement.addContent(keywordsElement);
        return keywordsElement;
    }

    @Override
    public void markDefaultSettings() {
        myDefaultSyntaxTable = mySyntaxTable;
    }

    @Override
    public boolean isModified() {
        return !Comparing.equal(myDefaultSyntaxTable, getSyntaxTable());
    }

    static final String ELEMENT_MAPPING = "mapping";
    static final String ATTRIBUTE_EXT = "ext";
    static final String ATTRIBUTE_PATTERN = "pattern";
    static final String ATTRIBUTE_TYPE = "type";

    @Nonnull
    static List<Pair<FileNameMatcher, String>> readAssociations(@Nonnull Element element) {
        List<Element> children = element.getChildren(ELEMENT_MAPPING);
        if (children.isEmpty()) {
            return Collections.emptyList();
        }

        List<Pair<FileNameMatcher, String>> result = new SmartList<>();
        for (Element mapping : children) {
            String ext = mapping.getAttributeValue(ATTRIBUTE_EXT);
            String pattern = mapping.getAttributeValue(ATTRIBUTE_PATTERN);

            FileNameMatcher matcher = ext != null ? new ExtensionFileNameMatcherImpl(ext) : FileTypeManager.parseFromString(pattern);
            result.add(Pair.create(matcher, mapping.getAttributeValue(ATTRIBUTE_TYPE)));
        }
        return result;
    }

    @Nullable
    static Element writeMapping(String typeName, @Nonnull FileNameMatcher matcher, boolean specifyTypeName) {
        Element mapping = new Element(ELEMENT_MAPPING);
        if (matcher instanceof ExtensionFileNameMatcherImpl extensionFileNameMatcher) {
            mapping.setAttribute(ATTRIBUTE_EXT, extensionFileNameMatcher.getExtension());
        }
        else if (writePattern(matcher, mapping)) {
            return null;
        }

        if (specifyTypeName) {
            mapping.setAttribute(ATTRIBUTE_TYPE, typeName);
        }

        return mapping;
    }

    static boolean writePattern(FileNameMatcher matcher, Element mapping) {
        if (matcher instanceof WildcardFileNameMatcher wildcardFileNameMatcher) {
            mapping.setAttribute(ATTRIBUTE_PATTERN, wildcardFileNameMatcher.getPattern());
        }
        else if (matcher instanceof ExactFileNameMatcher exactFileNameMatcher) {
            mapping.setAttribute(ATTRIBUTE_PATTERN, exactFileNameMatcher.getFileName());
        }
        else {
            return true;
        }
        return false;
    }

    @Override
    public SettingsEditor<AbstractFileType> getEditor() {
        return new CustomFileTypeEditor();
    }

    public void setCommenter(Commenter commenter) {
        myCommenter = commenter;
    }

    @Override
    @Nonnull
    public ExternalInfo getExternalInfo() {
        return myExternalInfo;
    }
}
