package com.intellij.coverage;

import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.util.xmlb.InvalidDataException;
import com.intellij.util.xmlb.WriteExternalException;
import org.jdom.Element;

/**
 * User: anna
 * Date: 9/30/11
 */
public class CoverageRunnerData implements RunnerSettings{
  @Override
  public void readExternal(Element element) throws InvalidDataException {}

  @Override
  public void writeExternal(Element element) throws WriteExternalException {}
}
