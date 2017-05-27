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

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.util.io.ZipUtil;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import java.io.*;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * @author cdr
 */
public class StartupActionScriptManager {
  private static final Logger LOG = Logger.getInstance(StartupActionScriptManager.class);
  @NonNls
  public static final String STARTUP_WIZARD_MODE = "StartupWizardMode";

  @NonNls
  private static final String ACTION_SCRIPT_FILE = "action.script";

  private StartupActionScriptManager() {
  }

  public static synchronized void executeActionScript(Logger logger) throws IOException {
    List<ActionCommand> commands = loadActionScript(logger);
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

    if (commands.size() > 0) {
      commands.clear();

      logger.info("start: saved empty list");

      saveActionScript(commands);
    }
  }

  public static synchronized void addActionCommand(ActionCommand command) throws IOException {
    addActionCommand(LOG, command);
  }

  public static synchronized void addActionCommand(Logger logger, ActionCommand command) throws IOException {
    if (Boolean.getBoolean(STARTUP_WIZARD_MODE)) {
      command.execute(logger);
      return;
    }
    final List<ActionCommand> commands = loadActionScript(logger);
    commands.add(command);
    saveActionScript(commands);
  }

  private static String getActionScriptPath() {
    String systemPath = PathManager.getPluginTempPath();
    return systemPath + File.separator + ACTION_SCRIPT_FILE;
  }

  private static List<ActionCommand> loadActionScript(Logger logger) throws IOException {
    File file = new File(getActionScriptPath());
    if (file.exists()) {
      ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
      try {
        //noinspection unchecked
        return (List<ActionCommand>)ois.readObject();
      }
      catch (ClassNotFoundException e) {
        // problem with scrambled code
        // fas fixed, but still appear because corrupted file still exists
        // return empty list.
        LOG.error("Internal file was corrupted. Problem is fixed.\nIf some plugins has been installed/uninstalled, please re-install/-uninstall them.", e);

        return new ArrayList<ActionCommand>();
      }
      finally {
        ois.close();
      }
    }
    else {
      logger.warn("start: No actionscript file");
      return new ArrayList<ActionCommand>();
    }
  }

  private static void saveActionScript(List<ActionCommand> commands) throws IOException {
    File temp = new File(PathManager.getPluginTempPath());
    boolean exists = true;
    if (!temp.exists()) {
      exists = temp.mkdirs();
    }

    if (exists) {
      File file = new File(getActionScriptPath());
      ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file, false));
      try {
        oos.writeObject(commands);
      }
      finally {
        oos.close();
      }
    }
  }

  private static boolean canCreateFile(File file) {
    return FileUtilRt.ensureCanCreateFile(file);
  }

  public interface ActionCommand {
    void execute(Logger logger) throws IOException;
  }

  public static class CopyCommand implements Serializable, ActionCommand {
    @NonNls
    private static final String action = "copy";
    private final File mySource;
    private final File myDestination;

    public CopyCommand(File source, File destination) {
      myDestination = destination;
      mySource = source;
    }

    public String toString() {
      return action + "[" + mySource.getAbsolutePath() + (myDestination == null ? "" : ", " + myDestination.getAbsolutePath()) + "]";
    }

    @Override
    public void execute(Logger logger) throws IOException {
      // create dirs for destination
      File parentFile = myDestination.getParentFile();
      if (!parentFile.exists()) {
        if (!myDestination.getParentFile().mkdirs()) {
          JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), MessageFormat
                                                .format("<html>Cannot create parent directory [{0}] of {1}<br>Please, check your access rights on folder <br>{2}",
                                                        parentFile.getAbsolutePath(), myDestination.getAbsolutePath(), parentFile.getParent()), "Installing Plugin",
                                        JOptionPane.ERROR_MESSAGE);
        }
      }

      if (!mySource.exists()) {
        logger.error("Source file " + mySource.getAbsolutePath() + " does not exist for action " + this);
      }
      else if (!canCreateFile(myDestination)) {
        JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), MessageFormat
                .format("<html>Cannot copy {0}<br>to<br>{1}<br>Please, check your access rights on folder <br>{2}", mySource.getAbsolutePath(),
                        myDestination.getAbsolutePath(), myDestination.getParent()), "Installing Plugin", JOptionPane.ERROR_MESSAGE);
      }
      else {
        FileUtilRt.copy(mySource, myDestination);
      }
    }

  }

  public static class UnzipCommand implements Serializable, ActionCommand {
    @NonNls
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

    public String toString() {
      return action + "[" + mySource.getAbsolutePath() + (myDestination == null ? "" : ", " + myDestination.getAbsolutePath()) + "]";
    }

    @Override
    public void execute(Logger logger) throws IOException {
      if (!mySource.exists()) {
        logger.error("Source file " + mySource.getAbsolutePath() + " does not exist for action " + this);
      }
      else if (!canCreateFile(myDestination)) {
        JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), MessageFormat
                .format("<html>Cannot unzip {0}<br>to<br>{1}<br>Please, check your access rights on folder <br>{2}", mySource.getAbsolutePath(),
                        myDestination.getAbsolutePath(), myDestination), "Installing Plugin", JOptionPane.ERROR_MESSAGE);
      }
      else {
        try {
          ZipUtil.extract(mySource, myDestination, myFilenameFilter);
        }
        catch (Exception ex) {
          logger.error(ex);

          JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), MessageFormat
                  .format("<html>Failed to extract ZIP file {0}<br>to<br>{1}<br>You may need to re-download the plugin you tried to install.",
                          mySource.getAbsolutePath(), myDestination.getAbsolutePath()), "Installing Plugin", JOptionPane.ERROR_MESSAGE);
        }
      }
    }

  }

  public static class DeleteCommand implements Serializable, ActionCommand {
    @NonNls
    private static final String action = "delete";
    private final File mySource;

    public DeleteCommand(File source) {
      mySource = source;
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

        JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), MessageFormat
                .format("<html>Cannot delete {0}<br>Please, check your access rights on folder <br>{1}", mySource.getAbsolutePath(),
                        mySource.getAbsolutePath()), "Installing Plugin", JOptionPane.ERROR_MESSAGE);
      }
    }
  }
}
