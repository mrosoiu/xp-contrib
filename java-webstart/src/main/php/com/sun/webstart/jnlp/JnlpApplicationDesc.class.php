<?php
/* This class is part of the XP framework
 *
 * $Id$ 
 */

  /**
   * Represents JNLP application description
   *
   * @see      xp://com.sun.webstart.JnlpDocument
   * @purpose  Wrapper class
   */
  class JnlpApplicationDesc extends Object {
    public
      $main_class = '',
      $arguments  = array();

    /**
     * Set Main_class
     *
     * @param   string main_class
     */
    public function setMain_class($main_class) {
      $this->main_class= $main_class;
    }

    /**
     * Get Main_class
     *
     * @return  string
     */
    public function getMain_class() {
      return $this->main_class;
    }
    
    /**
     * Add an arguments
     *
     * @param   string argument
     */
    public function addArgument($argument) {
      $this->arguments[]= $argument;
    }

    /**
     * Set Arguments
     *
     * @param   string[] arguments
     */
    public function setArguments($arguments) {
      $this->arguments= $arguments;
    }

    /**
     * Get Arguments
     *
     * @return  string[]
     */
    public function getArguments() {
      return $this->arguments;
    }
  }
?>
