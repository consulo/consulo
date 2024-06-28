/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.execution.test.sm.runner;

import consulo.application.Application;
import consulo.execution.test.TestConsoleProperties;
import consulo.execution.test.sm.runner.history.ImportTestOutputExtension;
import consulo.process.ProcessHandler;
import consulo.ui.ex.awt.Messages;
import jakarta.annotation.Nonnull;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

public class ImportedToGeneralTestEventsConverter extends OutputToGeneralTestEventsConverter {

  @Nonnull
  private final TestConsoleProperties myConsoleProperties;
  @Nonnull
  private final File myFile;
  @Nonnull
  private final ProcessHandler myHandler;

  public ImportedToGeneralTestEventsConverter(@Nonnull String testFrameworkName,
                                              @Nonnull TestConsoleProperties consoleProperties,
                                              @Nonnull File file,
                                              @Nonnull ProcessHandler handler) {
    super(testFrameworkName, consoleProperties);
    myConsoleProperties = consoleProperties;
    myFile = file;
    myHandler = handler;
  }

  @Override
  public void onStartTesting() {
    Application.get().executeOnPooledThread((Runnable)() -> {
      parseTestResults();
      myHandler.detachProcess();
    });
  }

  private void parseTestResults() {
    try {
      parseTestResults(() -> {
        try {
          return new InputStreamReader(new FileInputStream(myFile), StandardCharsets.UTF_8);
        }
        catch (FileNotFoundException e) {
          return null;
        }
      }, getProcessor());
    }
    catch (IOException e) {
      final String message = e.getMessage();
      Application.get().invokeLater(
        () -> Messages.showErrorDialog(myConsoleProperties.getProject(), message, "Failed to Parse " + myFile.getName())
      );
    }
  }

  public static void parseTestResults(Supplier<Reader> readerSupplier, GeneralTestEventsProcessor processor) throws IOException {
    parseTestResults(readerSupplier.get(), ImportTestOutputExtension.findHandler(readerSupplier, processor));
  }

  public static void parseTestResults(Reader reader, final DefaultHandler contentHandler) throws IOException {
    try {
      SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
      parser.parse(new InputSource(reader), contentHandler);
    }
    catch (ParserConfigurationException | SAXException e) {
      throw new IOException(e);
    }
    finally {
      reader.close();
    }
  }
}
