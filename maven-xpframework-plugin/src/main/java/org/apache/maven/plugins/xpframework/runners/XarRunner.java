/**
 * This file is part of the XP-Framework
 *
 * Maven XP-Framework plugin
 * Copyright (c) 2011, XP-Framework Team
 */
package org.apache.maven.plugins.xpframework.runners;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

import org.apache.maven.plugins.xpframework.util.ExecuteUtils;
import org.apache.maven.plugins.xpframework.runners.input.XarRunnerInput;

/**
 * Wrapper over XP-Framework "xar" runner
 *
 */
public class XarRunner extends AbstractRunner {
  XarRunnerInput input;

  /**
   * Constructor
   *
   * @param  org.apache.maven.plugins.xpframework.runners.input.XarRunnerInput input
   */
  public XarRunner(File executable, XarRunnerInput input) {
    super(executable);
    this.input= input;
  }

  /**
   * {@inheritDoc}
   *
   */
  public void execute() throws RunnerException {

    // Build arguments
    List<String> arguments= new ArrayList<String>();

    // Add operation
    switch (input.operation) {
      case CREATE: {
        arguments.add("cf");
        break;
      }

      case MERGE: {
        arguments.add("mf");
        break;
      }

      default: {
        throw new RunnerException("Unsupported xar operation [" + input.operation.toString() + "]");
      }
    }

    // Add output file
    if (null == input.outputFile) {
      throw new RunnerException("Output xar file not set");
    }
    arguments.add(input.outputFile.getAbsolutePath());

    // Add sources
    for (File src : this.input.sources) {
      arguments.add(src.getAbsolutePath());
    }

    // Execute command
    this.executeCommand(arguments);
  }
}
