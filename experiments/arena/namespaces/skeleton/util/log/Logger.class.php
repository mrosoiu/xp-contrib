<?php
/* This class is part of the XP framework
 *
 * $Id: Logger.class.php 10848 2007-07-20 11:56:05Z kiesel $ 
 */

  namespace util::log;

  ::uses('util.log.LogCategory', 'util.Configurable');
  
  define('LOG_DEFINES_DEFAULT', 'default');
  
  /**
   * A singleton logger
   * 
   * Example output:
   * <pre>
   * [20:45:30 16012 info ] Starting work on 2002/05/29/ 
   * [20:45:30 16012 info ] Done, 0 order(s) processed, 0 error(s) occured 
   * [20:45:30 16012 info ] Finish 
   * </pre>
   *
   * The format of the line prefix (noted in square brackets above) can be configured by:
   * <ul>
   *   <li>the identifier (an id which has recognizing value, e.g. the PID)</li>
   *   <li>the variable "format" (a printf-string)</li>
   * </ul>
   *
   * Note:
   * <ul>
   *   <li>The identifier defaults to the PID of the current process</li>
   *   <li>
   *     The argument order for the format parameter is:<br/>
   *     1) Current date<br/>
   *     2) Identifier<br/>
   *     3) Indicator [info, warn, error, debug]<br/>
   *   </li>
   *   <li>The format string defaults to "[%1$s %2$s %3$s]"</li>
   *   <li>The date format defaults to "H:i:s"</li>
   * </ul>
   *
   * Example [Setting up a logger]:
   * <code>
   *   $cat= Logger::getInstance()->getCategory();
   *   $cat->addAppender(new FileAppender('php://stderr'));
   * </code>
   *
   * Example [Configuring a logger]:
   * <code>
   *   Logger::getInstance()->configure(new Properties('etc/log.ini'));
   * </code>
   *
   * Example [Usage somewhere later on]:
   * <code>
   *   $cat= Logger::getInstance()->getCategory();
   * </code>
   *
   * Property file sample:
   * <pre>
   * [default]
   * appenders="util.log.FileAppender"
   * appender.util.log.FileAppender.params="filename"
   * appender.util.log.FileAppender.param.filename="/var/log/xp/service_%Y-%m-%d.log"
   * appender.util.log.FileAppender.flags="LOGGER_FLAG_ERROR|LOGGER_FLAG_WARN"
   * 
   * [info.binford6100.webservices.EventHandler]
   * appenders="util.log.FileAppender"
   * appender.util.log.FileAppender.params="filename"
   * appender.util.log.FileAppender.param.filename="/var/log/xp/event_%Y-%m-%d.log"
   * 
   * [info.binford6100.webservices.SubscriberHandler]
   * appenders="util.log.FileAppender"
   * appender.util.log.FileAppender.params="filename"
   * appender.util.log.FileAppender.param.filename="/var/log/xp/subscribe_%Y-%m-%d.log"
   * </pre>
   *
   * @test     xp://net.xp_framework.unittest.logging.LoggerTest
   * @purpose  Singleton logger
   */
  class Logger extends lang::Object implements util::Configurable {
    protected static 
      $instance     = NULL;
    
    public 
      $category     = array();
    
    public
      $defaultIdentifier,
      $defaultDateformat,
      $defaultFormat,
      $defaultFlags,
      $defaultAppenders;
  
    public
      $_finalized   = FALSE;

    static function __static() {
      self::$instance= new self();
      self::$instance->defaultIdentifier= getmypid();
      self::$instance->defaultFormat= '[%1$s %2$5s %3$5s]';
      self::$instance->defaultDateformat= 'H:i:s';
      self::$instance->defaultFlags= LOGGER_FLAG_ALL;
      self::$instance->defaultAppenders= array();
      
      // Create an empty LogCategory
      self::$instance->category[LOG_DEFINES_DEFAULT]= new LogCategory(
        self::$instance->defaultIdentifier,
        self::$instance->defaultFormat,
        self::$instance->defaultDateformat,
        self::$instance->defaultFlags
      );
    }

    /**
     * Constructor.
     *
     */
    protected function __construct() {
    }

    /**
     * Get a category
     *
     * @param   string name default LOG_DEFINES_DEFAULT
     * @return  util.log.LogCategory
     */ 
    public function getCategory($name= LOG_DEFINES_DEFAULT) {
      if (!isset($this->category[$name])) $name= LOG_DEFINES_DEFAULT;
      return $this->category[$name];
    }
    
    /**
     * Configure this logger
     *
     * @param   util.Properties prop instance of a Properties object
     */
    public function configure($prop) {
      $class= array();
      
      // Read default properties
      $this->defaultIdentifier= $prop->readString(LOG_DEFINES_DEFAULT, 'identifier', $this->defaultIdentifier);
      $this->defaultFormat= $prop->readString(LOG_DEFINES_DEFAULT, 'format', $this->defaultFormat);
      $this->defaultDateformat= $prop->readString(LOG_DEFINES_DEFAULT, 'date.format', $this->defaultDateformat);
      $this->defaultFlags= $prop->readInteger(LOG_DEFINES_DEFAULT, 'flags', $this->defaultFlags);
      $this->defaultAppenders= $prop->readArray(LOG_DEFINES_DEFAULT, 'appenders', $this->defaultAppenders);
      
      // Read all other properties
      $section= $prop->getFirstSection();
      do {
        $catclass= lang::XPClass::forName($prop->readString($section, 'category', 'util.log.LogCategory'));

        $this->category[$section]= $catclass->newInstance(
          $this->defaultIdentifier,
          $prop->readString($section, 'format', $this->defaultFormat),
          $prop->readString($section, 'date.format', $this->defaultDateformat),
          $prop->readInteger($section, 'flags', $this->defaultFlags)
        );
        
        // Has an appender?
        $param_section= $section;
        if (NULL === ($appenders= $prop->readArray($section, 'appenders', NULL))) {
          $appenders= $this->defaultAppenders;
          $param_section= LOG_DEFINES_DEFAULT;
        }

        // Go through all of the appenders, loading classes as necessary
        foreach ($appenders as $appender) {
          if (!isset($class[$appender])) {
            $class[$appender]= lang::XPClass::forName($appender);
          }
          
          // Read flags string, evaluate it
          $flags= $prop->readArray($param_section, 'appender.'.$appender.'.flags', LOGGER_FLAG_ALL);
          if (!is_int ($flags)) {
            $arrflags= $flags; $flags= 0;
            foreach ($arrflags as $f) { if (defined ($f)) $flags |= constant ($f); }
          }
          
          $a= $this->category[$section]->addAppender($class[$appender]->newInstance(), $flags);
          $params= $prop->readArray($param_section, 'appender.'.$appender.'.params', array());
          
          // Params
          foreach ($params as $param) {
            $a->{$param}= strftime(
              $prop->readString(
                $param_section, 
                'appender.'.$appender.'.param.'.$param,
                ''
              )
            );
          }
        }
      } while ($section= $prop->getNextSection());
    }
    
    /**
     * Tells all categories to finalize themselves
     *
     */
    public function finalize() {
      if (!$this->_finalized) foreach (array_keys($this->category) as $name) {
        $this->category[$name]->finalize();
      }
      $this->_finalized= TRUE;
    }
    
    /**
     * Returns an instance of this class
     *
     * @return  util.log.Logger a logger object
     */
    public static function getInstance() {
      return self::$instance;
    }
  } 
?>