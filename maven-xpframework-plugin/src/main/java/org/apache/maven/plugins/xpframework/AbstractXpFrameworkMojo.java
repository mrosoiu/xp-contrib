/**
 * This file is part of the XP-Framework
 *
 * Maven XP-Framework plugin
 * Copyright (c) 2011, XP-Framework Team
 */
package org.apache.maven.plugins.xpframework;

import java.io.File;
import java.util.Iterator;

import org.apache.maven.model.Dependency;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;

/**
 * Base class for all MOJO's
 *
 */
public abstract class AbstractXpFrameworkMojo extends AbstractMojo {
  public static final String LINE_SEPARATOR= "------------------------------------------------------------------------";

  /**
   * The Maven project
   *
   * @parameter default-value="${project}"
   * @required
   * @readonly
   */
  protected MavenProject project;

  /**
   * The Maven session
   *
   * @parameter default-value="${session}"
   * @readonly
   * @required
   */
  protected MavenSession session;

  /**
   * Maven project helper
   *
   * @component
   */
  protected MavenProjectHelper projectHelper;

  /**
   * Maven resource filtering
   *
   * @component role="org.apache.maven.shared.filtering.MavenResourcesFiltering" role-hint="default"
   * @required
   */
  protected MavenResourcesFiltering mavenResourcesFiltering;

  /**
   * Project base directory
   *
   * @parameter expression="${basedir}" default-value="${basedir}"
   * @required
   * @readonly
   */
  protected File basedir;

  /**
   * Directory containing the generated XAR
   *
   * @parameter expression="${project.build.directory}"
   * @required
   */
  protected File outputDirectory;

  /**
   * Directory where XP runners are located. If not set, runners will be
   * extracted from resources to "${project.build.directory}/runners"
   *
   * @parameter default-value="${xpframework.bootstrap.runners}"
   */
  protected File bootstrapRunners;

  /**
   * Bootstrap classpath (contents of USE_XP). If not set, "core" and "tools" must
   * be specified as [dependencies] and a bootstrap environment will be created at
   * "${project.build.directory}/bootstrap"
   *
   * @parameter default-value="${xpframework.bootstrap.classpath}"
   */
  protected String bootstrapClasspath;

  /**
   * Helper function to find a project dependency
   *
   *
   * @param  java.lang.String groupId
   * @param  java.lang.String artifactId
   * @return org.apache.maven.model.Dependency null if the specified dependency cannot be found
   */
  @SuppressWarnings("unchecked")
  protected Dependency findDependency(String groupId, String artifactId) {
    for (Dependency dependency : (Iterable<Dependency>)this.project.getDependencies()) {
      if (dependency.getGroupId().equals(groupId) && dependency.getArtifactId().equals(artifactId)) {
        return dependency;
      }
    }

    // Specified dependency not found
    return null;
  }

  /**
   * Helper function to find a project artifact
   *
   *
   * @param  java.lang.String groupId
   * @param  java.lang.String artifactId
   * @return org.apache.maven.artifact.Artifact null if the specified artifact cannot be found
   */
  @SuppressWarnings("unchecked")
  protected Artifact findArtifact(String groupId, String artifactId) {
    for (Artifact artifact : (Iterable<Artifact>)this.project.getArtifacts()) {
      if (artifact.getGroupId().equals(groupId) && artifact.getArtifactId().equals(artifactId)) {
        return artifact;
      }
    }

    // Specified artifact not found
    return null;
  }
}
