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

package consulo.ide.impl.idea.codeEditor.printing;

import consulo.language.editor.gutter.LineMarkerInfo;
import consulo.language.editor.highlight.HighlighterFactory;
import consulo.document.Document;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.codeEditor.EditorHighlighter;
import consulo.codeEditor.HighlighterIterator;
import consulo.colorScheme.TextAttributes;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.project.Project;
import consulo.util.lang.Comparing;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.language.codeStyle.CodeStyleSettingsManager;
import consulo.language.psi.PsiPackageHelper;
import consulo.annotation.access.RequiredReadAction;
import consulo.logging.Logger;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.ui.style.StandardColors;
import org.jetbrains.annotations.NonNls;

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;

class HTMLTextPainter {
  private static final Logger LOG = Logger.getInstance(HTMLTextPainter.class);

  private int myOffset = 0;
  private final EditorHighlighter myHighlighter;
  private final String myText;
  private final String myFileName;
  private final String myHTMLFileName;
  private int mySegmentEnd;
  private final PsiFile myPsiFile;
  private int lineCount;
  private int myFirstLineNumber;
  private final boolean myPrintLineNumbers;
  private int myColumn;
  private final LineMarkerInfo[] myMethodSeparators;
  private int myCurrentMethodSeparator;
  private final Project myProject;
  private final Map<TextAttributes, String> myStyleMap = new HashMap<>();

  @RequiredReadAction
  public HTMLTextPainter(PsiFile psiFile, Project project, String dirName, boolean printLineNumbers) {
    myProject = project;
    myPsiFile = psiFile;
    myPrintLineNumbers = printLineNumbers;
    myHighlighter = HighlighterFactory.createHighlighter(project, psiFile.getVirtualFile());

//    String fileType = FileTypeManager.getInstance().getType(psiFile.getVirtualFile().getName());
//    myForceFonts =
//      FileTypeManager.TYPE_HTML.equals(fileType) ||
//      FileTypeManager.TYPE_XML.equals(fileType) ||
//      FileTypeManager.TYPE_JSP.equals(fileType);

    myText = psiFile.getText();
    myHighlighter.setText(myText);
    mySegmentEnd = myText.length();
    myFileName = psiFile.getVirtualFile().getPresentableUrl();
    myHTMLFileName = dirName + File.separator + ExportToHTMLManager.getHTMLFileName(psiFile);

    PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(project);
    Document document = psiDocumentManager.getDocument(psiFile);

    List<LineMarkerInfo> methodSeparators = new ArrayList<>();
    if (document != null) {
      final List<LineMarkerInfo> separators = FileSeparatorUtil.getFileSeparators(psiFile, document);
      methodSeparators.addAll(separators);
    }

    myMethodSeparators = methodSeparators.toArray(new LineMarkerInfo[methodSeparators.size()]);
    myCurrentMethodSeparator = 0;
  }

  public void setSegment(int segmentStart, int segmentEnd, int firstLineNumber) {
    myOffset = segmentStart;
    mySegmentEnd = segmentEnd;
    myFirstLineNumber = firstLineNumber;
  }

  @RequiredReadAction
  @SuppressWarnings({"HardCodedStringLiteral"})
  public void paint(TreeMap refMap, FileType fileType) throws FileNotFoundException {
    HighlighterIterator hIterator = myHighlighter.createIterator(myOffset);
    if (hIterator.atEnd()) return;
    OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(myHTMLFileName), StandardCharsets.UTF_8);
    lineCount = myFirstLineNumber;
    TextAttributes prevAttributes = null;
    Iterator refKeys = null;

    int refOffset = -1;
    PsiReference ref = null;
    if (refMap != null) {
      refKeys = refMap.keySet().iterator();
      if (refKeys.hasNext()) {
        Integer key = (Integer)refKeys.next();
        ref = (PsiReference)refMap.get(key);
        refOffset = key;
      }
    }

    int referenceEnd = -1;
    try {
      writeHeader(writer, new File(myFileName).getName());
      if (myFirstLineNumber == 0) {
        writeLineNumber(writer);
      }
      String closeTag = null;

      while (myCurrentMethodSeparator < myMethodSeparators.length) {
        LineMarkerInfo marker = myMethodSeparators[myCurrentMethodSeparator];
        if (marker != null && marker.startOffset >= hIterator.getStart()) break;
        myCurrentMethodSeparator++;
      }

      while (!hIterator.atEnd()) {
        TextAttributes textAttributes = hIterator.getTextAttributes();
        int hStart = hIterator.getStart();
        int hEnd = hIterator.getEnd();
        if (hEnd > mySegmentEnd) break;

        boolean haveNonWhiteSpace = false;
        for (int offset = hStart; offset < hEnd; offset++) {
          char c = myText.charAt(offset);
          if (c != ' ' && c != '\t') {
            haveNonWhiteSpace = true;
            break;
          }
        }
        if (!haveNonWhiteSpace) {
          // don't write separate spans for whitespace-only text fragments
          writeString(writer, myText, hStart, hEnd - hStart, fileType);
          hIterator.advance();
          continue;
        }

        if (refOffset > 0 && hStart <= refOffset && hEnd > refOffset) {
          referenceEnd = writeReferenceTag(writer, ref);
        }
//        if (myForceFonts || !equals(prevAttributes, textAttributes)) {
        if (!equals(prevAttributes, textAttributes) && referenceEnd < 0 ) {
          if (closeTag != null) {
            writer.write(closeTag);
          }
          closeTag = writeFontTag(writer, textAttributes);
          prevAttributes = textAttributes;
        }

        if (myCurrentMethodSeparator < myMethodSeparators.length) {
          LineMarkerInfo marker = myMethodSeparators[myCurrentMethodSeparator];
          if (marker != null && marker.startOffset <= hEnd) {
            writer.write("<hr>");
            myCurrentMethodSeparator++;
          }
        }

        writeString(writer, myText, hStart, hEnd - hStart, fileType);
//        if (closeTag != null) {
//          writer.write(closeTag);
//        }
        if (referenceEnd > 0 && hEnd >= referenceEnd) {
          writer.write("</a>");
          referenceEnd = -1;
          if (refKeys.hasNext()) {
            Integer key = (Integer)refKeys.next();
            ref = (PsiReference)refMap.get(key);
            refOffset = key;
          }
        }
        hIterator.advance();
      }
      if (closeTag != null) {
        writer.write(closeTag);
      }
      writeFooter(writer);
    }
    catch (IOException e) {
      LOG.error(e);
    }
    finally {
      try {
        writer.close();
      }
      catch (IOException e) {
        LOG.error(e);
      }
    }
  }

  @RequiredReadAction
  private int writeReferenceTag(Writer writer, PsiReference ref) throws IOException {
    PsiElement refClass = ref.resolve();

    PsiFile refFile = refClass.getContainingFile();
    String refPackageName = PsiPackageHelper.getInstance(myProject).getQualifiedName(refFile.getContainingDirectory(), false);
    String psiPackageName = PsiPackageHelper.getInstance(myProject).getQualifiedName(myPsiFile.getContainingDirectory(), false);

    StringBuilder fileName = new StringBuilder();
    if (!psiPackageName.equals(refPackageName)) {
      StringTokenizer tokens = new StringTokenizer(psiPackageName, ".");
      while (tokens.hasMoreTokens()) {
        tokens.nextToken();
        fileName.append("../");
      }

      StringTokenizer refTokens = new StringTokenizer(refPackageName, ".");
      while (refTokens.hasMoreTokens()) {
        String token = refTokens.nextToken();
        fileName.append(token);
        fileName.append('/');
      }
    }
    fileName.append(ExportToHTMLManager.getHTMLFileName(refFile));
    //noinspection HardCodedStringLiteral
    writer.write("<a href=\""+fileName+"\">");
    return ref.getElement().getTextRange().getEndOffset();
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  private String writeFontTag(Writer writer, TextAttributes textAttributes) throws IOException {
    writer.write("<span class=\"" + myStyleMap.get(textAttributes) + "\">");
    return "</span>";
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  private void writeString(Writer writer, CharSequence charArray, int start, int length, FileType fileType) throws IOException {
    for (int i=start; i<start+length; i++) {
      char c = charArray.charAt(i);
      if (c=='<') {
        writeChar(writer, "&lt;");
      }
      else if (c=='>') {
        writeChar(writer, "&gt;");
      }
      else if (c=='&') {
        writeChar(writer, "&amp;");
      }
      else if (c=='\"') {
        writeChar(writer, "&quot;");
      }
      else if (c == '\t') {
        int tabSize = CodeStyleSettingsManager.getSettings(myProject).getTabSize(fileType);
        if (tabSize <= 0) tabSize = 1;
        int nSpaces = tabSize - myColumn % tabSize;
        for (int j = 0; j < nSpaces; j++) {
          writeChar(writer, " ");
        }
      }
      else if (c == '\n' || c == '\r') {
        if (c == '\r' && i+1 < start+length && charArray.charAt(i+1) == '\n') {
          writeChar(writer, " \r");
          i++;
        }
        else if (c == '\n') {
          writeChar(writer, " ");
        }
        writeLineNumber(writer);
      }
      else {
        writeChar(writer, String.valueOf(c));
      }
    }
  }

  private void writeChar(Writer writer, String s) throws IOException {
    writer.write(s);
    myColumn++;
  }

  private void writeLineNumber(@NonNls Writer writer) throws IOException {
    writer.write('\n');
    myColumn = 0;
    lineCount++;
    if (myPrintLineNumbers) {
      writer.write("<a name=\"l" + lineCount + "\">");

//      String numberCloseTag = writeFontTag(writer, ourLineNumberAttributes);

      writer.write("<span class=\"ln\">");
      String s = Integer.toString(lineCount);
      writer.write(s);
      int extraSpaces = 4 - s.length();
      do {
        writer.write(' ');
      } while (extraSpaces-- > 0);
      writer.write("</span></a>");

//      if (numberCloseTag != null) {
//        writer.write(numberCloseTag);
//      }
    }
  }

  private void writeHeader(@NonNls Writer writer, String title) throws IOException {
    EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
    writer.write("<html>\r\n");
    writer.write("<head>\r\n");
    writer.write("<title>" + title + "</title>\r\n");
    writer.write("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\r\n");
    writeStyles(writer);
    writer.write("</head>\r\n");
    ColorValue color = scheme.getDefaultBackground();
    if (color == null) color = StandardColors.GRAY;
    writer.write("<BODY BGCOLOR=\"#" + Integer.toString(RGBColor.toRGBValue(color.toRGB()) & 0xFFFFFF, 16) + "\">\r\n");
    writer.write("<TABLE CELLSPACING=0 CELLPADDING=5 COLS=1 WIDTH=\"100%\" BGCOLOR=\"#C0C0C0\" >\r\n");
    writer.write("<TR><TD><CENTER>\r\n");
    writer.write("<FONT FACE=\"Arial, Helvetica\" COLOR=\"#000000\">\r\n");
    writer.write(title + "</FONT>\r\n");
    writer.write("</center></TD></TR></TABLE>\r\n");
    writer.write("<pre>\r\n");
  }

  private void writeStyles(@NonNls final Writer writer) throws IOException {
    writer.write("<style type=\"text/css\">\n");
    writer.write(".ln { color: rgb(0,0,0); font-weight: normal; font-style: normal; }\n");
    HighlighterIterator hIterator = myHighlighter.createIterator(myOffset);
    while (!hIterator.atEnd()) {
      TextAttributes textAttributes = hIterator.getTextAttributes();
      if (!myStyleMap.containsKey(textAttributes)) {
        String styleName = "s" + myStyleMap.size();
        myStyleMap.put(textAttributes, styleName);
        writer.write("." + styleName + " { ");
        final ColorValue foreColor = textAttributes.getForegroundColor();
        if (foreColor != null) {
          RGBColor rgb = foreColor.toRGB();
          writer.write("color: rgb(" + rgb.getRed() + "," + rgb.getGreen() + "," + rgb.getBlue() + "); ");
        }
        if ((textAttributes.getFontType() & Font.BOLD) != 0) {
          writer.write("font-weight: bold; ");
        }
        if ((textAttributes.getFontType() & Font.ITALIC) != 0) {
          writer.write("font-style: italic; ");
        }
        writer.write("}\n");
      }
      hIterator.advance();
    }
    writer.write("</style>\n");
  }

  private static void writeFooter(@NonNls Writer writer) throws IOException {
    writer.write("</pre>\r\n");
    writer.write("</body>\r\n");
    writer.write("</html>");
  }

  private static boolean equals(TextAttributes attributes1, TextAttributes attributes2) {
    if (attributes2 == null) {
      return attributes1 == null;
    }
    return attributes1 != null
      && Comparing.equal(attributes1.getForegroundColor(), attributes2.getForegroundColor())
      && attributes1.getFontType() == attributes2.getFontType()
      && Comparing.equal(attributes1.getBackgroundColor(), attributes2.getBackgroundColor())
      && Comparing.equal(attributes1.getEffectColor(), attributes2.getEffectColor());
  }

  public String getHTMLFileName() {
    return myHTMLFileName;
  }
}
