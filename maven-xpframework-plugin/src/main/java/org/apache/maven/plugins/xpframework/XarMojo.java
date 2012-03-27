/**
 * This file is part of the XP-Framework
 *
 * Maven XP-Framework plugin
 * Copyright (c) 2011, XP-Framework Team
 */
package org.apache.maven.plugins.xpframework;

import java.io.File;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Package classes and resources into a XAR package
 *
 * @goal package
 * @requiresDependencyResolution runtime
 */
public class XarMojo extends AbstractXarMojo {

  /**
   * Directory containing the classes and resource files that should be packaged into the XAR
   *
   * @parameter expression="${project.build.outputDirectory}"
   * @required
   */
  private File classesDirectory;

  /**
   * Include XAR dependencies into the final uber-XAR
   *
   * @parameter expression="${xpframework.xar.mergeDependencies}" default-value="false"
   * @required
   */
  protected boolean mergeDependencies;

  /**
   * {@inheritDoc}
   *
   */
  public void execute() throws MojoExecutionException {
    getLog().info(LINE_SEPARATOR);
    getLog().info("BUILD XAR PACKAGE");
    getLog().info(LINE_SEPARATOR);

    // Assemble XAR archive
    File xarFile= AbstractXarMojo.getXarFile(this.outputDirectory, this.finalName, this.classifier);
    this.executeXar(this.classesDirectory, xarFile);
    getLog().info(LINE_SEPARATOR);

    // Merge dependencies into an uber-XAR?
    if (!this.mergeDependencies) return;

    getLog().info("BUILD UBER-XAR PACKAGE");
    getLog().info(LINE_SEPARATOR);

    // Assemble uber-XAR archive
    File uberXarFile= AbstractXarMojo.getUberXarFile(this.outputDirectory, this.finalName, this.classifier);
    this.executeUberXar(xarFile, uberXarFile);

    getLog().info(LINE_SEPARATOR);
  }
}
