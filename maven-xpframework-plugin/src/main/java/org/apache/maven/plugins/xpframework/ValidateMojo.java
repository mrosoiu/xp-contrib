/**
 * This file is part of the XP-Framework
 *
 * Maven XP-Framework plugin
 * Copyright (c) 2011, XP-Framework Team
 */
package org.apache.maven.plugins.xpframework;

import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;

import org.apache.maven.plugins.xpframework.util.FileUtils;
import org.apache.maven.plugins.xpframework.util.ExecuteUtils;
import org.apache.maven.plugins.xpframework.util.IniProperties;

import net.xp_forge.xar.XarArchive;

/**
 * Setup XP-Framework bootstrap and customize default Maven configuration
 *
 * @goal validate
 * @requiresDependencyResolution compile
 */
public class ValidateMojo extends AbstractXpFrameworkMojo {

  /**
   * {@inheritDoc}
   *
   */
  public void execute() throws MojoExecutionException {
    getLog().info(LINE_SEPARATOR);
    getLog().info("VALIDATE XP-FRAMEWORK INSTALL");
    getLog().info(LINE_SEPARATOR);

    // Setup bootstrap
    this.setupBootstrap();
    getLog().info("Using runners from:  [" + this.bootstrapRunners + "]");
    getLog().info("Bootstrap classpath: [" + this.bootstrapClasspath + "]");

    // Alter default Maven settings
    this.alterSourceDirectories();
    getLog().info(LINE_SEPARATOR);
  }

  /**
   * Setup XP-Framework bootstrap
   *
   * @return void
   * @throws org.apache.maven.plugin.MojoExecutionException
   */
  private void setupBootstrap() throws MojoExecutionException {

    // Case (1): Both ${xpframework.bootstrap.runners} AND ${xpframework.bootstrap.classpath} are set
    if (null != this.bootstrapRunners && null != this.bootstrapClasspath) {

      // The caller is responsible for bootstrap; nothing to see here, move along!
      getLog().debug("Case (1): Bootstrap provided by caller. Idling...");
      return;
    }

    // Case (2): [pom.xml/project/dependencies/dependency] contains [net.xp_framework:core] AND [net.xp_framework:tools]
    Artifact coreArtifact  = this.findArtifact("net.xp_framework", "core");
    Artifact toolsArtifact = this.findArtifact("net.xp_framework", "tools");
    if (null != coreArtifact && null != toolsArtifact) {
      getLog().debug("Case (2): Cooking our own bootstrap using resources und artifacts...");
      this.setupBootstrapFromDependencies(coreArtifact, toolsArtifact);
      return;
    }

    // Case (3): Use local install of XP-Framework (by looking for runners directory in PATH and using [xp.ini])
    getLog().debug("Case (3): Bootstrap using local XP-Framework install...");
    try {
      File bootstrapRunners= ExecuteUtils.getExecutable("xp").getParentFile();
      this.setupBootstrapFromLocalInstall(bootstrapRunners);
      return;

    // Out of options
    } catch (FileNotFoundException ex) {
      throw new MojoExecutionException(String.format("%s\n%s\n%s\n%s",
        "Call me dumb, but I don't know how to bootstrap XP-Framework. There are 3 possible options:",
        " 1. Tell me what XP-Framework to use using -Dxpframework.bootstrap.runners=... and -Dxpframework.bootstrap.classpath=...",
        " 2. Add [net.xp_framework:core] and [net.xp_framework:tools] to project's <dependencies> in pom.xml",
        " 3. Install XP-Framework locally and add the runners directory to [PATH] environment variable"
      ));
    }
  }

  /**
   * Setup XP-Framework bootstrap using dependencies specified in pom.xml
   *
   * @param  org.apache.maven.artifact.Artifact coreArtifact
   * @param  org.apache.maven.artifact.Artifact toolsArtifact
   * @return void
   * @throws org.apache.maven.plugin.MojoExecutionException
   */
  private void setupBootstrapFromDependencies(Artifact coreArtifact, Artifact toolsArtifact) throws MojoExecutionException {

    // Setup runners and update ${xpframework.bootstrap.runners} property
    this.bootstrapRunners= new File(this.outputDirectory, "runners");
    this.project.getProperties().setProperty("xpframework.bootstrap.runners", this.bootstrapRunners.getAbsolutePath());

    // Save runners to ${xpframework.bootstrap.runners}
    try {
      ExecuteUtils.saveRunner("xp", this.bootstrapRunners);
      ExecuteUtils.saveRunner("xcc", this.bootstrapRunners);
      ExecuteUtils.saveRunner("xar", this.bootstrapRunners);
      ExecuteUtils.saveRunner("xpcli", this.bootstrapRunners);
      ExecuteUtils.saveRunner("unittest", this.bootstrapRunners);

    } catch (IOException ex) {
      throw new MojoExecutionException("Cannot extract (all) runners from resources", ex);
    }

    // Setup bootstrap directory to "${project.build.directory}/bootstrap"
    File bootstrapDirectory= new File(this.outputDirectory, "bootstrap");

    // Extract [lang.base.php] from [net.xp_framework:core] XAR artifact
    File coreDirectory= new File(bootstrapDirectory, "core");

    File langFile= new File(coreDirectory, "lang.base.php");
    this.extract("lang.base.php", coreArtifact, langFile);

    // Extract [tools/class.php], [tools/web.php] and [tools/xar.php] from [net.xp_framework:tools] XAR artifact
    File toolsDirectory= new File(bootstrapDirectory, "tools");

    File classFile= new File(toolsDirectory, "class.php");
    this.extract("tools/class.php", toolsArtifact, classFile);

    File webFile= new File(toolsDirectory, "web.php");
    this.extract("tools/web.php", toolsArtifact, webFile);

    File xarFile= new File(toolsDirectory, "xar.php");
    this.extract("tools/xar.php", toolsArtifact, xarFile);

    List<File> bootstrap= new ArrayList<File>();
    bootstrap.add(coreDirectory);
    bootstrap.add(toolsDirectory);
    bootstrap.add(coreArtifact.getFile());
    bootstrap.add(toolsArtifact.getFile());

    // Create [boot.pth]
    String bootPthContent= "";
    for (File boot : bootstrap) {
      bootPthContent+= boot.getAbsolutePath() + "\n";
    }
    try {
      FileUtils.setFileContents(new File(bootstrapDirectory, "boot.pth"), bootPthContent);
    } catch (IOException ex) {
      throw new MojoExecutionException("Cannot create [boot.pth] file [" + bootstrapDirectory + "]");
    }

    // Update ${xpframework.bootstrap.classpath} property
    this.bootstrapClasspath= bootstrapDirectory.getAbsolutePath();
    this.project.getProperties().setProperty("xpframework.bootstrap.classpath", this.bootstrapClasspath);
  }

  /**
   * Setup XP-Framework bootstrap using local install of XP-Framework
   *
   * @param  java.io.File bootstrapRunners
   * @return void
   * @throws org.apache.maven.plugin.MojoExecutionException
   */
  private void setupBootstrapFromLocalInstall(File bootstrapRunners) throws MojoExecutionException {

    // Update ${xpframework.bootstrap.runners} property
    this.bootstrapRunners= bootstrapRunners;
    this.project.getProperties().setProperty("xpframework.bootstrap.runners", this.bootstrapRunners.getAbsolutePath());

    IniProperties ini= new IniProperties();
    File iniFile= new File(bootstrapRunners, "xp.ini");
    try {
      ini.load(iniFile);
    } catch (IOException ex) {
      throw new MojoExecutionException("Cannot read ini properties from [" + iniFile + "]", ex);
    }

    // Read bootstrap classpath from [xp.ini]
    this.bootstrapClasspath= ini.getProperty("use");
    if (null == this.bootstrapClasspath) {
      throw new MojoExecutionException("Cannot find [use] property in [" + iniFile + "]");
    }

    // Update ${xpframework.bootstrap.classpath} property
    this.project.getProperties().setProperty("xpframework.bootstrap.classpath", this.bootstrapClasspath);
  }

  /**
   * Extract the specified xar entry to the specified destination
   *
   * @param  java.lang.String entry
   * @param  org.apache.maven.artifact.Artifact artifact
   * @param  java.io.File out
   * @return void
   * @throws org.apache.maven.plugin.MojoExecutionException
   */
  private void extract(String entry, Artifact artifact, File out) throws MojoExecutionException {
    try {
      FileUtils.setFileContents(out, new XarArchive(artifact.getFile()).getEntry(entry).getInputStream());
    } catch (IOException ex) {
      throw new MojoExecutionException(
        "Cannot extract [" + entry + "] from [" + artifact.getFile() + "] to [" + out + "]",
        ex
      );
    }
  }

  /**
   * This will alter ${project.sourceDirectory} and ${project.testSourceDirectory} only if they have
   * the default values set up in the Maven Super POM:
   * - src/main/java
   * - src/test/java
   *
   * to the following values:
   * - src/main/xp
   * - src/test/xp
   *
   * @return void
   */
  @SuppressWarnings("unchecked")
  private void alterSourceDirectories() {

    // Check ${project.sourceDirectory} ends with [src/main/java]
    String oldDirectory = this.project.getBuild().getSourceDirectory();
    String xpDirectory  = this.basedir.getAbsolutePath() + File.separator + "src" + File.separator + "main" + File.separator + "xp";
    if (oldDirectory.endsWith("src" + File.separator + "main" + File.separator + "java")) {

      // Alter ${project.sourceDirectory}
      this.project.getBuild().setSourceDirectory(xpDirectory);
      getLog().debug("Set ${project.sourceDirectory} to [" + xpDirectory + "]");

      // Maven2 limitation: changing ${project.sourceDirectory} doesn't change ${project.compileSourceRoots}
      List<String> newRoots= new ArrayList<String>();
      for (String oldRoot : (Iterable<String>)this.project.getCompileSourceRoots()) {
        if (oldRoot.equals(oldDirectory)) {
          newRoots.add(xpDirectory);
        } else {
          newRoots.add(oldRoot);
        }
      }

      // Replace ${project.compileSourceRoots} with new list
      this.project.getCompileSourceRoots().clear();
      for (String newRoot : newRoots) {
        this.project.addCompileSourceRoot(newRoot);
      }
    }

    // Check ${project.testSourceDirectory} ends with [src/test/java]
    oldDirectory= this.project.getBuild().getTestSourceDirectory();
    xpDirectory= this.basedir.getAbsolutePath() + File.separator + "src" + File.separator + "test" + File.separator + "xp";
    if (oldDirectory.endsWith("src" + File.separator + "test" + File.separator + "java")) {

      // Alter ${project.testSourceDirectory}
      this.project.getBuild().setTestSourceDirectory(xpDirectory);
      getLog().debug("Set ${project.testSourceDirectory} to [" + xpDirectory + "]");

      // Maven2 limitation: changing ${project.testSourceDirectory} doesn't change ${project.testCompileSourceRoots}
      List<String> newRoots= new ArrayList<String>();
      for (String oldRoot : (Iterable<String>)this.project.getTestCompileSourceRoots()) {
        if (oldRoot.equals(oldDirectory)) {
          newRoots.add(xpDirectory);
        } else {
          newRoots.add(oldRoot);
        }
      }

      // Replace ${project.testCompileSourceRoots} with new list
      this.project.getTestCompileSourceRoots().clear();
      for (String newRoot : newRoots) {
        this.project.addTestCompileSourceRoot(newRoot);
      }
    }
  }
}
