/*
 * Copyright 2013-2016 consulo.io
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
package consulo.start;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/**
 * @author VISTALL
 * @since 23-Dec-16
 */
public class CommandLineArgs {
  @Option(name = "--no-splash", usage = "Disable splash at start")
  private boolean myNoSplash;

  @Option(name = "--no-recent-projects", usage = "Disable opening recent projects at start")
  private boolean myNoRecentProjects;

  @Option(name = "--line", metaVar = "<line>", usage = "Line of file")
  private int myLine = -1;

  @Option(name = "--json", metaVar = "<json>", usage = "JSON file of API request after start")
  private String myJson;

  @Option(name = "--version", usage = "Print version")
  private boolean myShowVersion;

  @Option(name = "--help", help = true, usage = "Show help")
  private boolean myShowHelp;

  @Argument(usage = "File or project for open", metaVar = "<file>")
  private String file;

  public boolean isShowVersion() {
    return myShowVersion;
  }

  public boolean isNoRecentProjects() {
    return myNoRecentProjects;
  }

  public int getLine() {
    return myLine;
  }

  public boolean isNoSplash() {
    return myNoSplash;
  }

  public boolean isShowHelp() {
    return myShowHelp;
  }

  public void setFile(String file) {
    this.file = file;
  }

  public void setNoRecentProjects(boolean noRecentProjects) {
    myNoRecentProjects = noRecentProjects;
  }

  public String getFile() {
    return file;
  }

  public String getJson() {
    return myJson;
  }

  public void setJson(String json) {
    myJson = json;
  }

  public static CommandLineArgs parse(String[] args) {
    CommandLineArgs o = new CommandLineArgs();

    CmdLineParser parser = new CmdLineParser(o);

    try {
      parser.parseArgument(args);
    }
    catch (CmdLineException e) {
      parser.printUsage(System.out);
    }

    return o;
  }

  public static void printUsage() {
    System.out.println("Command line options:");
    CmdLineParser parser = new CmdLineParser(new CommandLineArgs());
    parser.printUsage(System.out);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("CommandLineArgs{");
    sb.append("myNoSplash=").append(myNoSplash);
    sb.append(", myLine=").append(myLine);
    sb.append(", myShowVersion=").append(myShowVersion);
    sb.append(", myShowHelp=").append(myShowHelp);
    sb.append(", file='").append(file).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
