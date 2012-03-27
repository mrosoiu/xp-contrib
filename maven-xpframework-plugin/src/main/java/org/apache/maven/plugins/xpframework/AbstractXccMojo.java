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
import java.util.Set;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

import org.apache.maven.model.Resource;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;

import org.apache.maven.plugins.xpframework.runners.XccRunner;
import org.apache.maven.plugins.xpframework.runners.RunnerException;
import org.apache.maven.plugins.xpframework.runners.input.XccRunnerInput;
import org.apache.maven.plugins.xpframework.util.FileUtils;
import org.apache.maven.plugins.xpframework.util.MavenResourceUtils;

/**
 * XP Compiler
 *
 * Usage:
 * ========================================================================
 * $ xcc [options] [path [path [... ]]]
 * ========================================================================
 *
 * Options is one of:
 *
 *   * -v:
 *     Display verbose diagnostics
 *
 *   * -cp [path]:
 *     Add path to classpath
 *
 *   * -sp [path]:
 *     Adds path to source path (source path will equal classpath initially)
 *
 *   * -e [emitter]:
 *     Use emitter, defaults to "source"
 *
 *   * -p [profile[,profile[,...]]]:
 *     Use compiler profiles (defaults to ["default"]) - xp/compiler/{profile}.xcp.ini
 *
 *   * -o [outputdir]:
 *     Write compiled files to outputdir (will be created if not existent)
 *
 *   * -t [level[,level[...]]]:
 *     Set trace level (all, none, info, warn, error, debug)
 *
 *
 * Path may be:
 *
 *   * [file.ext]:
 *     This file will be compiled
 *
 *   * [folder]:
 *     All files in this folder with all supported syntaxes will be compiled
 *
 *   * -N [folder]:
 *     Same as above, but not performed recursively
 *
 *
 * ========================================================================
 * Syntax support:
 *   * [php  ] PHP 5.3 Syntax (no alternative syntax)
 *   * [xp   ] XP Language Syntax
 */
public abstract class AbstractXccMojo extends AbstractXpFrameworkMojo {

  /**
   * Display verbose diagnostics
   *
   * The -v argument for the xcc compiler
   *
   * @parameter expression="${xpframework.xcc.verbose}" default-value="false"
   */
  protected boolean verbose;

  /**
   * Add path to classpath
   *
   * The -cp argument for the xcc compiler
   *
   * @parameter expression="${xpframework.xcc.classpaths}"
   */
  protected ArrayList<String> classpaths;

  /**
   * Adds path to source path (source path will equal classpath initially)
   *
   * The -sp argument for the xcc compiler
   *
   * @parameter expression="${xpframework.xcc.sourcepaths}"
   */
  protected ArrayList<String> sourcepaths;

  /**
   * Use emitter, defaults to "source"
   *
   * The -e argument for the xcc compiler
   *
   * @parameter expression="${xpframework.xcc.emitter}"
   */
  protected String emitter;

  /**
   * Use compiler profiles (defaults to ["default"]) - xp/compiler/{profile}.xcp.ini
   *
   * The -p argument for the xcc compiler
   *
   * @parameter expression="${xpframework.xcc.profiles}"
   */
  protected ArrayList<String> profiles;

  /**
   * Execute XP-Framework XCC compiler
   *
   * @param  java.util.List<String> sourceDirectories Source where .xp file are
   * @param  java.io.File classesDirectory Destination where to output compiled classes
   * @return void
   * @throws org.apache.maven.plugin.MojoExecutionException When execution of the xcc runner failed
   */
  public void executeXcc(List<String> sourceDirectories, File classesDirectory) throws MojoExecutionException {

    // Compile each source root
    for (String srcDir : sourceDirectories) {
      this.executeXcc(srcDir, classesDirectory);
    }
  }

  /**
   * Execute XP-Framework XCC compiler
   *
   * @param  java.lang.String sourceDirectory Source where .xp file are
   * @param  java.io.File classesDirectory Destination where to output compiled classes
   * @return void
   * @throws org.apache.maven.plugin.MojoExecutionException When execution of the xcc runner failed
   */
  @SuppressWarnings("unchecked")
  public void executeXcc(String sourceDirectory, File classesDirectory) throws MojoExecutionException {

    // Debug info
    getLog().info("Source directory [" + sourceDirectory + "]");
    getLog().debug("Classes directory [" + classesDirectory + "]");
    getLog().debug("Sourcepaths       [" + (null == this.sourcepaths ? "NULL" : this.sourcepaths.toString()) + "]");
    getLog().debug("Classpaths        [" + (null == this.classpaths  ? "NULL" : this.classpaths.toString())  + "]");

    // Prepare [xcc] input
    XccRunnerInput input= new XccRunnerInput();
    input.verbose= this.verbose;

    // Add classpaths
    if (null != this.classpaths) {
      for (String cp : this.classpaths) {
        input.addClasspath(new File(cp));
      }
    }

    // Add xar dependencies to classpath
    input.addClasspath(this.project.getArtifacts());

    // Add sourcepaths
    if (null != this.sourcepaths) {
      for (String sp : this.sourcepaths) {
        input.addSourcepath(new File(sp));
      }
    }

    // Add emitter
    input.emitter= this.emitter;

    // Add profiles
    if (null != this.profiles) {
      for (String profile : this.profiles) {
        input.addProfile(profile);
      }
    }

    // Add outputdir
    input.outputdir= classesDirectory;

    // Add source
    input.addSource(new File(sourceDirectory));

    // Configure [xcc] runner
    File executable= new File(this.bootstrapRunners, "xcc");
    XccRunner runner= new XccRunner(executable, input);
    runner.setTrace(getLog());

    // Set runner working directory
    try {
      runner.setWorkingDirectory(this.basedir);
    } catch (FileNotFoundException ex) {
      throw new MojoExecutionException("Cannot set [xcc] runner working directory", ex);
    }

    // Set [USE_XP] environment variable
    runner.setEnvironmentVariable("USE_XP", this.bootstrapClasspath);

    // Execute runner
    try {
      runner.execute();
    } catch (RunnerException ex) {
      throw new MojoExecutionException("Execution of [xcc] runner failed", ex);
    }
  }

  /**
   * Add an entry to sourcepaths
   *
   * @param  java.lang.String sourcepath
   * @return void
   */
  protected void addSourcepath(String sourcepath) {
    if (null == this.sourcepaths) this.sourcepaths= new ArrayList<String>();

    // Make sourcepath absolute
    String absoluteSourcepath= FileUtils.getAbsolutePath(sourcepath, this.basedir);
    if (null == absoluteSourcepath) return;

    // Add to sourcepaths
    this.sourcepaths.add(absoluteSourcepath);
  }

  /**
   * Add an entry to classpaths
   *
   * @param  java.lang.String classpath
   * @return void
   */
  protected void addClasspath(String classpath) {
    if (null == this.classpaths) this.classpaths= new ArrayList<String>();

    // Make classpath absolute
    String absoluteClasspath= FileUtils.getAbsolutePath(classpath, this.basedir);
    if (null == absoluteClasspath) return;

    // Add to sourcepaths
    this.classpaths.add(absoluteClasspath);
  }

  /**
   * Copy "/src/main|test/php/**.class.php" files "/target/classes|test-classes/"
   *
   * @param  java.util.List<String> phpSourceRoots Source where raw PHP files are
   * @param  java.io.File classesDirectory Destination where to copy PHP files
   * @return void
   * @throws org.apache.maven.plugin.MojoExecutionException When copy of the PHP source files failed
   */
  protected void copyPhpSources(List<String> phpSourceRoots, File classesDirectory) throws MojoExecutionException {

    // Debug info
    getLog().debug("PHP source directories [" + (null == phpSourceRoots ? "NULL" : phpSourceRoots.toString()) + "]");

    // Ignore non-existing raw PHP files
    if (null == phpSourceRoots || phpSourceRoots.isEmpty()) {
      getLog().info("There are no PHP sources to copy");
      return;
    }

    // Create resources
    List<Resource> resources= new ArrayList<Resource>();

    // Build a resource for each phpSourceRoots
    for (String phpSourceRoot : phpSourceRoots) {

      // Check directory exists
      if (null == FileUtils.getAbsolutePath(phpSourceRoot, this.basedir)) {
        getLog().info("Skip non-existing PHP source directory [" + phpSourceRoot + "]");
        continue;
      }

      Resource resource= new Resource();
      resource.addInclude("**/*.class.php");
      resource.setFiltering(false);
      resource.setDirectory(phpSourceRoot);
      resources.add(resource);

      // Filter resources
      try {
        MavenResourceUtils.copyResources(resources, classesDirectory, this.project, this.session, this.mavenResourcesFiltering);

      } catch(IOException ex) {
        throw new MojoExecutionException("Failed to copy PHP sources", ex);
      }
    }
  }
}
