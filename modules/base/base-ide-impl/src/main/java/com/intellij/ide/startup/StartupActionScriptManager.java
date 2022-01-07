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
package com.intellij.ide.startup;

import com.intellij.ide.actions.ImportSettingsFilenameFilter;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.util.io.ZipUtil;
import consulo.container.boot.ContainerPathManager;
import consulo.logging.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.input.sax.XMLReaders;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import javax.swing.*;
import java.io.*;
import java.text.MessageFormat;
import java.util.*;

/**
 * @author cdr
 * <p>
 * FIXME [VISTALL] In ideal world this class must be moved to containter-impl module
 */
public class StartupActionScriptManager {
  public interface ActionCommand {
    void execute(Logger logger) throws IOException;
  }

  public static class UnzipCommand implements ActionCommand {
    private static final String action = "unzip";

    private File mySource;
    private FilenameFilter myFilenameFilter;
    private File myDestination;

    public UnzipCommand(File source, File destination) {
      this(source, destination, null);
    }

    public UnzipCommand(File source, File destination, FilenameFilter filenameFilter) {
      myDestination = destination;
      mySource = source;
      myFilenameFilter = filenameFilter;
    }

    public File getSource() {
      return mySource;
    }

    public File getDestination() {
      return myDestination;
    }

    public FilenameFilter getFilenameFilter() {
      return myFilenameFilter;
    }

    public String toString() {
      return action + "[" + mySource.getAbsolutePath() + (myDestination == null ? "" : ", " + myDestination.getAbsolutePath()) + (myFilenameFilter == null ? "" : "," + myFilenameFilter) + "]";
    }

    @Override
    public void execute(final Logger logger) throws IOException {
      if (!mySource.exists()) {
        logger.error("Source file " + mySource.getAbsolutePath() + " does not exist for action " + this);
      }
      else if (!canCreateFile(myDestination)) {
        JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), MessageFormat
                                              .format("<html>Cannot unzip {0}<br>to<br>{1}<br>Please, check your access rights on folder <br>{2}", mySource.getAbsolutePath(), myDestination.getAbsolutePath(), myDestination),
                                      "Installing Plugin", JOptionPane.ERROR_MESSAGE);
      }
      else {
        try {
          ZipUtil.extract(mySource, myDestination, myFilenameFilter);
        }
        catch (Exception ex) {
          logger.error(ex);

          JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), MessageFormat
                  .format("<html>Failed to extract ZIP file {0}<br>to<br>{1}<br>You may need to re-download the plugin you tried to install.", mySource.getAbsolutePath(),
                          myDestination.getAbsolutePath()), "Installing Plugin", JOptionPane.ERROR_MESSAGE);
        }
      }
    }

  }

  public static class CreateFileCommand implements ActionCommand {
    private static final String action = "createFile";

    private final File myTargetFile;

    public CreateFileCommand(File targetFile) {
      myTargetFile = targetFile;
    }

    public File getTargetFile() {
      return myTargetFile;
    }

    @Override
    public String toString() {
      return action + "[" + myTargetFile.getAbsolutePath() + "]";
    }

    @Override
    public void execute(Logger logger) throws IOException {
      FileUtilRt.createIfNotExists(myTargetFile);
    }
  }

  public static class DeleteCommand implements ActionCommand {
    private static final String action = "delete";

    private final File mySource;

    public DeleteCommand(File source) {
      mySource = source;
    }

    public File getSource() {
      return mySource;
    }

    public String toString() {
      return action + "[" + mySource.getAbsolutePath() + "]";
    }

    @Override
    public void execute(Logger logger) throws IOException {
      if (mySource == null) {
        return;
      }

      if (!mySource.exists()) {
        logger.error("Source file " + mySource.getAbsolutePath() + " does not exist for action " + this);
      }
      else if (!FileUtilRt.delete(mySource)) {
        logger.error("Action " + this + " failed.");

        JOptionPane.showMessageDialog(JOptionPane.getRootFrame(),
                                      MessageFormat.format("<html>Cannot delete {0}<br>Please, check your access rights on folder <br>{1}", mySource.getAbsolutePath(), mySource.getAbsolutePath()),
                                      "Installing Plugin", JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  public static final String STARTUP_WIZARD_MODE = "StartupWizardMode";

  private static final String ourStartXmlFileName = "start.xml";

  private StartupActionScriptManager() {
  }

  public static synchronized void executeActionScript() throws IOException {
    Logger logger = Logger.getInstance(StartupActionScriptManager.class);

    List<ActionCommand> commands = loadStartActions(logger);

    for (ActionCommand command : commands) {
      logger.info("start: load command " + command);
    }

    for (ActionCommand command : commands) {
      try {
        logger.info("start: executing command " + command);
        command.execute(logger);
      }
      catch (IOException e) {
        logger.error(e);

        throw e;
      }
    }

    logger.info("start: saved empty list");

    saveStartActions(new ArrayList<>());
  }

  public static synchronized void addActionCommand(ActionCommand command) throws IOException {
    addActionCommand(Logger.getInstance(StartupActionScriptManager.class), command);
  }

  public static synchronized void addActionCommand(Logger logger, ActionCommand command) throws IOException {
    if (Boolean.getBoolean(STARTUP_WIZARD_MODE)) {
      command.execute(logger);
      return;
    }

    List<ActionCommand> newCommands = new ArrayList<ActionCommand>(loadStartActions(logger));
    newCommands.add(command);
    saveStartActions(newCommands);
  }

  private static String getStartXmlFilePath() {
    String systemPath = ContainerPathManager.get().getPluginTempPath();
    return systemPath + File.separator + ourStartXmlFileName;
  }

  private static List<ActionCommand> loadStartActions(Logger logger) throws IOException {
    List<ActionCommand> actionCommands = loadObsoleteActionScriptFile(logger);
    if (!actionCommands.isEmpty()) {
      return actionCommands;
    }

    File file = new File(getStartXmlFilePath());

    if (file.exists()) {
      try {
        SAXBuilder builder = new SAXBuilder(XMLReaders.NONVALIDATING);

        Document document = builder.build(file);

        Element rootElement = document.getRootElement();
        List<ActionCommand> list = new ArrayList<ActionCommand>();

        for (Element element : rootElement.getChildren()) {
          String name = element.getName();
          if (DeleteCommand.action.equals(name)) {
            String path = element.getAttributeValue("source");
            list.add(new DeleteCommand(new File(path)));
          }
          else if (CreateFileCommand.action.equals(name)) {
            String path = element.getAttributeValue("target");
            list.add(new CreateFileCommand(new File(path)));
          }
          else if (UnzipCommand.action.equals(name)) {
            String sourceValue = element.getAttributeValue("source");
            String descriptionValue = element.getAttributeValue("description");
            String filter = element.getAttributeValue("filter");
            FilenameFilter filenameFilter = null;
            if ("import".equals(filter)) {
              Set<String> names = new HashSet<String>();
              for (Element child : element.getChildren()) {
                names.add(child.getTextTrim());
              }
              filenameFilter = new ImportSettingsFilenameFilter(names);
            }

            list.add(new UnzipCommand(new File(sourceValue), new File(descriptionValue), filenameFilter));
          }
        }

        return list;
      }
      catch (Exception e) {
        logger.error(e);
        return Collections.emptyList();
      }
    }
    else {
      logger.warn("No " + ourStartXmlFileName + " file");
      return Collections.emptyList();
    }
  }

  @SuppressWarnings("unchecked")
  private static List<ActionCommand> loadObsoleteActionScriptFile(Logger logger) {
    File file = new File(ContainerPathManager.get().getPluginTempPath(), "action.script");
    if (file.exists()) {
      ObjectInputStream stream = null;
      try {
        stream = new ObjectInputStream(new FileInputStream(file));
        return (ArrayList<ActionCommand>)stream.readObject();
      }
      catch (Exception e) {
        return Collections.emptyList();
      }
      finally {
        try {
          if (stream != null) {
            stream.close();
          }
        }
        catch (IOException ignored) {
        }

        try {
          file.delete();
        }
        catch (Exception ignored) {
        }
      }
    }
    else {
      return Collections.emptyList();
    }
  }

  private static void saveStartActions(List<ActionCommand> commands) throws IOException {
    File temp = new File(ContainerPathManager.get().getPluginTempPath());
    boolean exists = true;
    if (!temp.exists()) {
      exists = temp.mkdirs();
    }

    if (exists) {
      File file = new File(getStartXmlFilePath());

      Element rootElement = new Element("list");
      for (ActionCommand command : commands) {
        if (command instanceof DeleteCommand) {
          Element element = new Element(DeleteCommand.action);
          rootElement.addContent(element);

          element.setAttribute("source", ((DeleteCommand)command).getSource().getPath());
        }
        else if(command instanceof CreateFileCommand cf) {
          Element element = new Element(CreateFileCommand.action);
          rootElement.addContent(element);

          element.setAttribute("target", cf.getTargetFile().getPath());
        }
        else if (command instanceof UnzipCommand) {
          Element element = new Element(UnzipCommand.action);
          rootElement.addContent(element);

          element.setAttribute("source", ((UnzipCommand)command).getSource().getPath());
          element.setAttribute("description", ((UnzipCommand)command).getDestination().getPath());

          FilenameFilter filenameFilter = ((UnzipCommand)command).getFilenameFilter();
          if (filenameFilter instanceof ImportSettingsFilenameFilter) {
            element.setAttribute("filter", "import");

            Set<String> relativeNamesToExtract = ((ImportSettingsFilenameFilter)filenameFilter).getRelativeNamesToExtract();
            for (String value : relativeNamesToExtract) {
              Element child = new Element("name").setText(value);

              element.addContent(child);
            }
          }
          else if (filenameFilter != null) {
            throw new UnsupportedOperationException("Unsupported fiter type: " + filenameFilter.getClass().getName());
          }
        }
      }

      OutputStream stream = null;
      try {
        XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());

        xmlOutputter.output(new Document(rootElement), stream = new FileOutputStream(file));
      }
      finally {
        if (stream != null) {
          stream.close();
        }
      }
    }
  }

  private static boolean canCreateFile(File file) {
    return FileUtilRt.ensureCanCreateFile(file);
  }
}
