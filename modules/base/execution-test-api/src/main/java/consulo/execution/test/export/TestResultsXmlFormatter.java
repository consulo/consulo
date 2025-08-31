/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.execution.test.export;

import consulo.application.Application;
import consulo.application.progress.ProgressManager;
import consulo.execution.DefaultExecutionTarget;
import consulo.execution.ExecutionTarget;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.localize.ExecutionLocalize;
import consulo.execution.test.*;
import consulo.execution.ui.console.ConsoleViewContentType;
import consulo.execution.ui.console.HyperlinkInfo;
import consulo.execution.util.ConsoleBuffer;
import consulo.localize.LocalizeValue;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.Ref;
import consulo.util.xml.serializer.WriteExternalException;
import jakarta.annotation.Nonnull;
import org.jdom.Attribute;
import org.jdom.Element;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import java.text.SimpleDateFormat;
import java.util.*;

public class TestResultsXmlFormatter {

  private static final String ELEM_RUN = "testrun";
  public static final String ELEM_TEST = "test";
  public static final String ELEM_SUITE = "suite";
  public static final String ATTR_NAME = "name";
  public static final String ATTR_DURATION = "duration";
  public static final String ATTR_LOCATION = "locationUrl";
  public static final String ATTR_METAINFO = "metainfo";
  public static final String ELEM_COUNT = "count";
  public static final String ATTR_VALUE = "value";
  public static final String ELEM_OUTPUT = "output";
  public static final String ATTR_OUTPUT_TYPE = "type";
  public static final String ATTR_STATUS = "status";
  public static final String TOTAL_STATUS = "total";
  private static final String ATTR_FOORTER_TEXT = "footerText";
  public static final String ATTR_CONFIG = "isConfig";
  public static final String STATUS_PASSED = "passed";
  public static final String STATUS_FAILED = "failed";
  public static final String STATUS_ERROR = "error";
  public static final String STATUS_IGNORED = "ignored";
  public static final String STATUS_SKIPPED = "skipped";

  public static final String ROOT_ELEM = "root";


  private final RunConfiguration myRuntimeConfiguration;
  private final ContentHandler myResultHandler;
  private final AbstractTestProxy myTestRoot;
  private final boolean myHidePassedConfig;
  private final ExecutionTarget myExecutionTarget;

  public static void execute(AbstractTestProxy root, RunConfiguration runtimeConfiguration, TestConsoleProperties properties, ContentHandler resultHandler) throws SAXException {
    new TestResultsXmlFormatter(root, runtimeConfiguration, properties, resultHandler).execute();
  }

  private TestResultsXmlFormatter(AbstractTestProxy root, RunConfiguration runtimeConfiguration, TestConsoleProperties properties, ContentHandler resultHandler) {
    myRuntimeConfiguration = runtimeConfiguration;
    myTestRoot = root;
    myResultHandler = resultHandler;
    myHidePassedConfig = TestConsoleProperties.HIDE_SUCCESSFUL_CONFIG.value(properties);
    myExecutionTarget = properties.getExecutionTarget();
  }

  private void execute() throws SAXException {
    myResultHandler.startDocument();

    TreeMap<String, Integer> counts = new TreeMap<>((o1, o2) -> {
      if (TOTAL_STATUS.equals(o1) && !TOTAL_STATUS.equals(o2)) return -1;
      if (TOTAL_STATUS.equals(o2) && !TOTAL_STATUS.equals(o1)) return 1;
      return o1.compareTo(o2);
    });
    for (AbstractTestProxy node : myTestRoot.getAllTests()) {
      if (!node.isLeaf()) continue;
      String status = getStatusString(node);
      increment(counts, status);
      increment(counts, TOTAL_STATUS);
    }

    Map<String, String> runAttrs = new HashMap<>();
    runAttrs.put(ATTR_NAME, myRuntimeConfiguration.getName());
    LocalizeValue footerText = ExecutionLocalize.exportTestResultsFooter(
      Application.get().getName(),
      new SimpleDateFormat().format(new Date())
    );
    runAttrs.put(ATTR_FOORTER_TEXT, footerText.get());
    Long duration = myTestRoot.getDuration();
    if (duration != null) {
      runAttrs.put(ATTR_DURATION, String.valueOf(duration));
    }
    startElement(ELEM_RUN, runAttrs);

    for (Map.Entry<String, Integer> entry : counts.entrySet()) {
      Map<String, String> a = new HashMap<>();
      a.put(ATTR_NAME, entry.getKey());
      a.put(ATTR_VALUE, String.valueOf(entry.getValue()));
      startElement(ELEM_COUNT, a);
      endElement(ELEM_COUNT);
    }

    Element config = new Element("config");
    try {
      myRuntimeConfiguration.writeExternal(config);
      config.setAttribute("configId", myRuntimeConfiguration.getType().getId());
      config.setAttribute("name", myRuntimeConfiguration.getName());
      if (!DefaultExecutionTarget.INSTANCE.equals(myExecutionTarget)) {
        config.setAttribute("target", myExecutionTarget.getId());
      }
    }
    catch (WriteExternalException ignore) {
    }
    processJDomElement(config);

    if (myTestRoot instanceof TestProxyRoot) {
      String presentation = ((TestProxyRoot)myTestRoot).getPresentation();
      if (presentation != null) {
        LinkedHashMap<String, String> rootAttrs = new LinkedHashMap<>();
        rootAttrs.put("name", presentation);
        String comment = ((TestProxyRoot)myTestRoot).getComment();
        if (comment != null) {
          rootAttrs.put("comment", comment);
        }
        String rootLocation = ((TestProxyRoot)myTestRoot).getRootLocation();
        if (rootLocation != null) {
          rootAttrs.put("location", rootLocation);
        }
        startElement(ROOT_ELEM, rootAttrs);
        writeOutput(myTestRoot);
        endElement(ROOT_ELEM);
      }
    }

    if (myTestRoot.shouldSkipRootNodeForExport()) {
      for (AbstractTestProxy node : myTestRoot.getChildren()) {
        processNode(node);
      }
    }
    else {
      processNode(myTestRoot);
    }
    endElement(ELEM_RUN);
    myResultHandler.endDocument();
  }

  private void processJDomElement(Element config) throws SAXException {
    String name = config.getName();
    LinkedHashMap<String, String> attributes = new LinkedHashMap<>();
    for (Attribute attribute : config.getAttributes()) {
      attributes.put(attribute.getName(), attribute.getValue());
    }
    startElement(name, attributes);
    for (Element child : config.getChildren()) {
      processJDomElement(child);
    }
    endElement(name);
  }

  private static void increment(Map<String, Integer> counts, String status) {
    Integer count = counts.get(status);
    counts.put(status, count != null ? count + 1 : 1);
  }

  private void processNode(AbstractTestProxy node) throws SAXException {
    ProgressManager.checkCanceled();
    Map<String, String> attrs = new HashMap<>();
    attrs.put(ATTR_NAME, node.getName());
    attrs.put(ATTR_STATUS, getStatusString(node));
    Long duration = node.getDuration();
    if (duration != null) {
      attrs.put(ATTR_DURATION, String.valueOf(duration));
    }
    String locationUrl = node.getLocationUrl();
    if (locationUrl != null) {
      attrs.put(ATTR_LOCATION, locationUrl);
    }
    String metainfo = node.getMetainfo();
    if (metainfo != null) {
      attrs.put(ATTR_METAINFO, metainfo);
    }
    if (node.isConfig()) {
      attrs.put(ATTR_CONFIG, "true");
    }
    boolean started = false;
    String elemName = node.isLeaf() ? ELEM_TEST : ELEM_SUITE;
    if (node.isLeaf()) {
      started = true;
      startElement(elemName, attrs);
      writeOutput(node);
    }
    else {
      for (AbstractTestProxy child : node.getChildren()) {
        if (myHidePassedConfig && child.isConfig() && child.isPassed()) {
          //ignore configurations during export
          continue;
        }
        if (!started) {
          started = true;
          startElement(elemName, attrs);
        }
        processNode(child);
      }
    }
    if (started) {
      endElement(elemName);
    }
  }

  private void writeOutput(AbstractTestProxy node) throws SAXException {
    final StringBuilder buffer = new StringBuilder();
    final Ref<ConsoleViewContentType> lastType = new Ref<>();
    final Ref<SAXException> error = new Ref<>();

    final int bufferSize = ConsoleBuffer.useCycleBuffer() ? ConsoleBuffer.getCycleBufferSize() : -1;
    Printer printer = new Printer() {
      @Override
      public void print(String text, ConsoleViewContentType contentType) {
        ProgressManager.checkCanceled();
        if (contentType != lastType.get()) {
          if (buffer.length() > 0) {
            try {
              writeOutput(lastType.get(), buffer);
            }
            catch (SAXException e) {
              error.set(e);
            }
          }
          lastType.set(contentType);
        }
        if (bufferSize < 0 || buffer.length() < bufferSize) {
          buffer.append(text);
        }
      }

      @Override
      public void onNewAvailable(@Nonnull Printable printable) {
      }

      @Override
      public void printHyperlink(String text, HyperlinkInfo info) {
      }

      @Override
      public void mark() {
      }
    };
    node.printOwnPrintablesOn(printer);
    if (!error.isNull()) {
      throw error.get();
    }
    if (buffer.length() > 0) {
      writeOutput(lastType.get(), buffer);
    }
  }

  private void writeOutput(ConsoleViewContentType type, StringBuilder text) throws SAXException {
    StringBuilder output = new StringBuilder();
    StringTokenizer t = new StringTokenizer(text.toString(), "\n");
    while (t.hasMoreTokens()) {
      output.append(StringUtil.escapeXml(t.nextToken())).append("\n");
    }

    Map<String, String> a = new HashMap<>();
    a.put(ATTR_OUTPUT_TYPE, getTypeString(type));
    startElement(ELEM_OUTPUT, a);
    writeText(output.toString());
    text.delete(0, text.length());
    endElement(ELEM_OUTPUT);
  }

  private static String getTypeString(ConsoleViewContentType type) {
    return type == ConsoleViewContentType.ERROR_OUTPUT ? "stderr" : "stdout";
  }

  private static String getStatusString(AbstractTestProxy node) {
    int magnitude = node.getMagnitude();
    // TODO enumeration!
    switch (magnitude) {
      case 0:
        return STATUS_SKIPPED;
      case 2:
      case 4:
        return STATUS_SKIPPED;
      case 5:
        return STATUS_IGNORED;
      case 1:
        return STATUS_PASSED;
      case 6:
        return STATUS_FAILED;
      case 8:
        return STATUS_ERROR;
      default:
        return node.isPassed() ? STATUS_PASSED : STATUS_FAILED;
    }
  }

  private void startElement(String name, Map<String, String> attributes) throws SAXException {
    AttributesImpl attrs = new AttributesImpl();
    for (Map.Entry<String, String> entry : attributes.entrySet()) {
      attrs.addAttribute("", entry.getKey(), entry.getKey(), "CDATA", entry.getValue());
    }
    myResultHandler.startElement("", name, name, attrs);
  }

  private void endElement(String name) throws SAXException {
    myResultHandler.endElement("", name, name);
  }

  private void writeText(String text) throws SAXException {
    char[] chars = text.toCharArray();
    myResultHandler.characters(chars, 0, chars.length);
  }
}
