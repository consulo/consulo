package consulo.credentialStorage.impl.internal.util.jdom;

import consulo.application.util.SystemInfo;
import consulo.component.macro.PathMacroFilter;
import consulo.component.macro.ReplacePathToMacroMap;
import consulo.util.jdom.JDOMUtil;
import consulo.util.xml.serializer.Constants;
import org.jdom.*;
import org.jdom.output.Format;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

public class JbXmlOutputter extends BaseXmlOutputter {
    private final ElementOutputFilter elementFilter;
    private final ReplacePathToMacroMap macroMap;
    private final PathMacroFilter macroFilter;
    private final boolean isForbidSensitiveData;
    private final String storageFilePathForDebugPurposes;
    private final Format format;

    public JbXmlOutputter() {
        this("\n", null, null, null, true, null);
    }

    public JbXmlOutputter(String lineSeparator) {
        this(lineSeparator, null, null, null, true, null);
    }

    public JbXmlOutputter(String lineSeparator,
                          ElementOutputFilter elementFilter,
                          ReplacePathToMacroMap macroMap,
                          PathMacroFilter macroFilter,
                          boolean isForbidSensitiveData,
                          String storageFilePathForDebugPurposes) {
        super(lineSeparator);
        this.elementFilter = elementFilter;
        this.macroMap = macroMap;
        this.macroFilter = macroFilter;
        this.isForbidSensitiveData = isForbidSensitiveData;
        this.storageFilePathForDebugPurposes = storageFilePathForDebugPurposes;
        this.format = JDOMFormat.createFormat(lineSeparator);
    }

//    public static void collapseMacrosAndWrite(Element element, ComponentManager project, Writer writer) throws IOException {
//        createOutputter(project).output(element, writer);
//    }
//
//    public static JbXmlOutputter createOutputter(ComponentManager project) {
//        PathMacroManager macroManager = ProjectPathMacroManager.getInstance((Project) project);
//        return new JbXmlOutputter("\n", null, macroManager.getReplacePathMap(), macroManager.getMacroFilter(), true, null);
//    }
//
//    public static String collapseMacrosAndWrite(Element element, ComponentManager project) throws IOException {
//        StringWriter writer = new StringWriter();
//        collapseMacrosAndWrite(element, project, writer);
//        return writer.toString();
//    }

    public static String escapeElementEntities(String str) {
        return JDOMUtil.escapeText(str, false, false);
    }

    private static final Set<String> reportedSensitiveProblems = Collections.synchronizedSet(new HashSet<String>());

    // --- Instance methods ---

    public void output(Document doc, Writer out) throws IOException {
        printDeclaration(out, format.getEncoding());
        List<Content> content = doc.getContent();
        for (Object obj : content) {
            if (obj instanceof Element) {
                printElement(out, doc.getRootElement(), 0);
            }
            else if (obj instanceof DocType) {
                printDocType(out, doc.getDocType());
                writeLineSeparator(out);
            }
            newline(out);
            indent(out, 0);
        }
        writeLineSeparator(out);
        out.flush();
    }

    private void writeLineSeparator(Writer out) throws IOException {
        if (format.getLineSeparator() != null) {
            out.write(format.getLineSeparator());
        }
    }

    public void output(DocType doctype, Writer out) throws IOException {
        printDocType(out, doctype);
        out.flush();
    }

    public void output(Element element, Writer out) throws IOException {
        printElement(out, element, 0);
    }

    private void printDeclaration(Writer out, String encoding) throws IOException {
        if (!format.getOmitDeclaration()) {
            out.write("<?xml version=\"1.0\"");
            if (!format.getOmitEncoding()) {
                out.write(" encoding=\"" + encoding + "\"");
            }
            out.write("?>");
            writeLineSeparator(out);
        }
    }

    private void printCDATA(Writer out, CDATA cdata) throws IOException {
        String str;
        if (format.getTextMode() == Format.TextMode.NORMALIZE) {
            str = cdata.getTextNormalize();
        }
        else {
            str = cdata.getText();
            if (format.getTextMode() == Format.TextMode.TRIM) {
                str = str.trim();
            }
        }
        out.write("<![CDATA[");
        out.write(str);
        out.write("]]>");
    }

    private void printString(Writer out, String str) throws IOException {
        String normalizedString = str;
        if (format.getTextMode() == Format.TextMode.NORMALIZE) {
            normalizedString = Text.normalizeString(normalizedString);
        }
        else if (format.getTextMode() == Format.TextMode.TRIM) {
            normalizedString = normalizedString.trim();
        }
        if (macroMap != null) {
            normalizedString = macroMap.substitute(normalizedString, SystemInfo.isFileSystemCaseSensitive);
        }
        out.write(escapeElementEntities(normalizedString));
    }

    public void printElement(Writer out, Element element, int level) throws IOException {
        printElementImpl(out, element, level, macroFilter != null);
    }

    private void printElementImpl(Writer out, Element element, int level, boolean substituteMacro) throws IOException {
        if (elementFilter != null && !elementFilter.accept(element, level)) {
            return;
        }
        boolean currentSubstituteMacro = substituteMacro && (macroFilter != null && !macroFilter.skipPathMacros(element));
        out.write('<');
        printQualifiedName(out, element);
        if (element.hasAttributes()) {
            printAttributes(out, element.getAttributes(), currentSubstituteMacro);
        }
        if (!writeContent(out, element, level, currentSubstituteMacro)) {
            return;
        }
        out.write("</");
        printQualifiedName(out, element);
        out.write('>');
    }

    protected boolean writeContent(Writer out, Element element, int level, boolean substituteMacro) throws IOException {
        if (isForbidSensitiveData) {
            checkIsElementContainsSensitiveInformation(element);
        }
        List<Content> content = element.getContent();
        int start = skipLeadingWhite(content, 0);
        int size = content.size();
        if (start >= size) {
            out.write(" />");
            return false;
        }
        out.write('>');
        if (nextNonText(content, start) < size) {
            newline(out);
            printContentRange(out, content, start, size, level + 1, substituteMacro);
            newline(out);
            indent(out, level);
        }
        else {
            printTextRange(out, content, start, size);
        }
        return true;
    }

    private void printContentRange(Writer out, List<Content> content, int start, int end, int level, boolean substituteMacro) throws IOException {
        int index = start;
        while (index < end) {
            boolean firstNode = (index == start);
            Content next = content.get(index);
            if (next instanceof Text || next instanceof EntityRef) {
                int first = skipLeadingWhite(content, index);
                index = nextNonText(content, first);
                if (first < index) {
                    if (!firstNode) {
                        newline(out);
                    }
                    indent(out, level);
                    printTextRange(out, content, first, index);
                }
                continue;
            }
            if (!firstNode) {
                newline(out);
            }
            indent(out, level);
            if (next instanceof Element) {
                printElementImpl(out, (Element) next, level, substituteMacro);
            }
            index++;
        }
    }

    private void printTextRange(Writer out, List<Content> content, int start, int end) throws IOException {
        int newStart = skipLeadingWhite(content, start);
        if (newStart >= content.size()) {
            return;
        }
        int newEnd = skipTrailingWhite(content, end);
        String previous = null;
        for (int i = newStart; i < newEnd; i++) {
            Content node = content.get(i);
            String next;
            if (node instanceof Text) {
                next = ((Text) node).getText();
            }
            else if (node instanceof EntityRef) {
                next = "&" + ((EntityRef) node).getValue() + ";";
            }
            else {
                throw new IllegalStateException("Should see only CDATA, Text, or EntityRef");
            }
            if (next == null || next.isEmpty()) {
                continue;
            }
            if (previous != null && (format.getTextMode() == Format.TextMode.NORMALIZE || format.getTextMode() == Format.TextMode.TRIM)) {
                if (endsWithWhite(previous) || startsWithWhite(next)) {
                    out.write(' ');
                }
            }
            if (node instanceof CDATA) {
                printCDATA(out, (CDATA) node);
            }
            else if (node instanceof EntityRef) {
                printEntityRef(out, (EntityRef) node);
            }
            else {
                printString(out, next);
            }
            previous = next;
        }
    }

    private void printAttributes(Writer out, List<Attribute> attributes, boolean substituteMacro) throws IOException {
        for (Attribute attribute : attributes) {
            out.write(' ');
            printQualifiedName(out, attribute);
            out.write('=');
            out.write('"');
            String value;
            if (macroMap != null && substituteMacro && (macroFilter == null || !macroFilter.skipPathMacros(attribute))) {
                value = macroMap.getAttributeValue(attribute, macroFilter, SystemInfo.isFileSystemCaseSensitive, false);
            }
            else {
                value = attribute.getValue();
            }
            if (isForbidSensitiveData && doesNameSuggestSensitiveInformation(attribute.getName())) {
                logSensitiveInformationError("@" + attribute.getName(), "Attribute", attribute.getParent());
            }
            out.write(escapeAttributeEntities(value));
            out.write('"');
        }
    }

    private void newline(Writer out) throws IOException {
        if (format.getIndent() != null) {
            writeLineSeparator(out);
        }
    }

    private void indent(Writer out, int level) throws IOException {
        String indent = format.getIndent();
        if (indent == null || indent.isEmpty()) {
            return;
        }
        for (int i = 0; i < level; i++) {
            out.write(indent);
        }
    }

    private int skipLeadingWhite(List<Content> content, int start) {
        int index = start < 0 ? 0 : start;
        int size = content.size();
        Format.TextMode textMode = format.getTextMode();
        if (textMode == Format.TextMode.TRIM_FULL_WHITE || textMode == Format.TextMode.NORMALIZE || textMode == Format.TextMode.TRIM) {
            while (index < size) {
                if (!isAllWhitespace(content.get(index))) {
                    return index;
                }
                index++;
            }
        }
        return index;
    }

    private int skipTrailingWhite(List<Content> content, int end) {
        int index = end;
        int size = content.size();
        if (index > size) {
            index = size;
        }
        Format.TextMode textMode = format.getTextMode();
        if (textMode == Format.TextMode.TRIM_FULL_WHITE || textMode == Format.TextMode.NORMALIZE || textMode == Format.TextMode.TRIM) {
            while (index > 0) {
                if (!isAllWhitespace(content.get(index - 1))) {
                    break;
                }
                index--;
            }
        }
        return index;
    }

    private boolean isAllWhitespace(Content obj) {
        if (!(obj instanceof Text)) return false;
        String str = ((Text) obj).getText();
        for (char ch : str.toCharArray()) {
            if (!org.jdom.Verifier.isXMLWhitespace(ch)) {
                return false;
            }
        }
        return true;
    }

    private boolean startsWithWhite(String str) {
        return !str.isEmpty() && org.jdom.Verifier.isXMLWhitespace(str.charAt(0));
    }

    private boolean endsWithWhite(String str) {
        return !str.isEmpty() && org.jdom.Verifier.isXMLWhitespace(str.charAt(str.length() - 1));
    }

    private String escapeAttributeEntities(String str) {
        return JDOMUtil.escapeText(str, false, true);
    }

    private void printQualifiedName(Writer out, Element e) throws IOException {
        String prefix = e.getNamespacePrefix();
        if (prefix != null && !prefix.isEmpty()) {
            out.write(prefix);
            out.write(':');
        }
        out.write(e.getName());
    }

    private void printQualifiedName(Writer out, Attribute a) throws IOException {
        String prefix = a.getNamespacePrefix();
        if (prefix != null && !prefix.isEmpty()) {
            out.write(prefix);
            out.write(':');
        }
        out.write(a.getName());
    }

    // --- Static helper functions from top-level in Kotlin ---

    private static int nextNonText(List<Content> content, int start) {
        int index = start < 0 ? 0 : start;
        int size = content.size();
        while (index < size) {
            Content node = content.get(index);
            if (!(node instanceof Text || node instanceof EntityRef)) {
                return index;
            }
            index++;
        }
        return size;
    }

    private static void printEntityRef(Writer out, EntityRef entity) throws IOException {
        out.write("&");
        out.write(entity.getName());
        out.write(";");
    }

    private void checkIsElementContainsSensitiveInformation(Element element) {
        String name = element.getName();
        if (!shouldCheckElement(element)) {
            return;
        }
        if (name != null && doesNameSuggestSensitiveInformation(name) && !element.getContent().isEmpty()) {
            logSensitiveInformationError(name, "Element", element.getParentElement());
        }
        String attrName = element.getAttributeValue(Constants.NAME);
        if (attrName != null && doesNameSuggestSensitiveInformation(attrName) && element.getAttribute("value") != null) {
            logSensitiveInformationError("@name=" + attrName, "Element", element);
        }
    }

    private boolean shouldCheckElement(Element element) {
        String parentName = element.getParentElement() != null ? element.getParentElement().getName() : null;
        return !("property".equals(element.getName()) &&
            ("driver-properties".equals(parentName) || "driver".equals(parentName)));
    }

    private void logSensitiveInformationError(String name, String elementKind, Element parentElement) {
// TODO maybe restore it later?
//        String parentPath;
//        if (parentElement == null) {
//            parentPath = null;
//        }
//        else {
//            List<String> ids = new ArrayList<>();
//            Element parent = parentElement;
//            while (parent != null) {
//                String parentId = parent.getName();
//                if (ComponentStorageUtil.COMPONENT.equals(parentId)) {
//                    String componentName = parent.getAttributeValue(ComponentStorageUtil.NAME);
//                    if (componentName != null) {
//                        parentId += "@" + componentName;
//                    }
//                }
//                ids.add(parentId);
//                parent = parent.getParentElement();
//            }
//            if (ids.isEmpty()) {
//                parentPath = null;
//            }
//            else {
//                Collections.reverse(ids);
//                parentPath = String.join(".", ids);
//            }
//        }
//        String message = elementKind + " " + (parentPath == null ? "" : parentPath + ".") + name +
//            " probably contains sensitive information";
//        if (storageFilePathForDebugPurposes != null) {
//            message += " (file: " + storageFilePathForDebugPurposes.replace(FileUtil.toSystemIndependentName(SystemProperties.getUserHome()), "~") + ")";
//        }
//        if (reportedSensitiveProblems.add(message)) {
//            Logger.getInstance(JbXmlOutputter.class).error(message);
//        }
    }
}
