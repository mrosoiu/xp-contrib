<?php
/* This class is part of the XP framework
 *
 * $Id: WrapperTest.class.php 9772 2007-03-27 16:09:13Z friebe $ 
 */

  namespace net::xp_framework::unittest::scriptlet::workflow;

  ::uses(
    'unittest.TestCase',
    'scriptlet.xml.workflow.Wrapper',
    'scriptlet.xml.workflow.Handler',
    'scriptlet.xml.workflow.Context',
    'scriptlet.xml.XMLScriptletRequest'
  );

  /**
   * TestCase
   *
   * @see      xp://scriptlet.xml.workflow.Wrapper
   * @purpose  Unittest
   */
  class WrapperTest extends unittest::TestCase {
    protected
      $wrapper= NULL,
      $handler= NULL;
 
    /**
     * Sets up test case
     *
     */
    public function setUp() {
      $this->wrapper= new scriptlet::xml::workflow::Wrapper();
      $this->handler= ::newinstance('Handler', array(), '{}');
      $this->handler->setWrapper($this->wrapper);
      
      // Register parameters
      $this->wrapper->registerParamInfo(
        'orderdate',
        OCCURRENCE_OPTIONAL,
        util::Date::fromString('1977-12-14'),
        array('scriptlet.xml.workflow.casters.ToDate')
      );
      $this->wrapper->registerParamInfo(
        'shirt_size',
        OCCURRENCE_UNDEFINED,
        NULL,                // No default, required attribute
        NULL,                // No cast necessary
        NULL,                // No precheck necessary, non-empty suffices
        array('scriptlet.xml.workflow.checkers.OptionChecker', array('S', 'M', 'L', 'XL'))
      );
      $this->wrapper->registerParamInfo(
        'shirt_qty',
        OCCURRENCE_UNDEFINED,
        NULL,                // No default, required attribute
        array('scriptlet.xml.workflow.casters.ToInteger'),
        array('scriptlet.xml.workflow.checkers.NumericChecker'),
        array('scriptlet.xml.workflow.checkers.IntegerRangeChecker', 1, 10)
      );
      $this->wrapper->registerParamInfo(
        'notify_me',
        OCCURRENCE_OPTIONAL | OCCURRENCE_MULTIPLE,
        array(),
        NULL,
        NULL,
        NULL,
        'core:string',
        array('process', 'send')
      );
    }
    
    /**
     * Test the getParamNames() method
     *
     */
    #[@test]
    public function getParamNames() {
      $this->assertEquals(
        array('orderdate', 'shirt_size', 'shirt_qty', 'notify_me'), 
        $this->wrapper->getParamNames()
      );
    }

    /**
     * Test the getParamInfo() method
     *
     */
    #[@test]
    public function orderDateParamInfo() {
      $this->assertEquals(OCCURRENCE_OPTIONAL, $this->wrapper->getParamInfo('orderdate', PARAM_OCCURRENCE));
      $this->assertEquals(util::Date::fromString('1977-12-14'), $this->wrapper->getParamInfo('orderdate', PARAM_DEFAULT));
      $this->assertEquals(NULL, $this->wrapper->getParamInfo('orderdate', PARAM_PRECHECK));
      $this->assertEquals(NULL, $this->wrapper->getParamInfo('orderdate', PARAM_POSTCHECK));
      $this->assertEquals('core:string', $this->wrapper->getParamInfo('orderdate', PARAM_TYPE));
      $this->assertEquals(array(), $this->wrapper->getParamInfo('orderdate', PARAM_VALUES));
      $this->assertClass($this->wrapper->getParamInfo('orderdate', PARAM_CASTER), 'scriptlet.xml.workflow.casters.ToDate');
    }

    /**
     * Test the getParamInfo() method
     *
     */
    #[@test]
    public function shirtSizeParamInfo() {
      $this->assertEquals(OCCURRENCE_UNDEFINED, $this->wrapper->getParamInfo('shirt_size', PARAM_OCCURRENCE));
      $this->assertEquals(NULL, $this->wrapper->getParamInfo('shirt_size', PARAM_DEFAULT));
      $this->assertEquals(NULL, $this->wrapper->getParamInfo('shirt_size', PARAM_PRECHECK));
      $this->assertEquals(NULL, $this->wrapper->getParamInfo('shirt_size', PARAM_CASTER));
      $this->assertEquals('core:string', $this->wrapper->getParamInfo('shirt_size', PARAM_TYPE));
      $this->assertEquals(array(), $this->wrapper->getParamInfo('shirt_size', PARAM_VALUES));
      $this->assertClass($this->wrapper->getParamInfo('shirt_size', PARAM_POSTCHECK), 'scriptlet.xml.workflow.checkers.OptionChecker');
    }

    /**
     * Test the getValue() method
     *
     */
    #[@test]
    public function getValue() {
      $this->assertEquals(NULL, $this->wrapper->getValue('orderdate'));
    }

    /**
     * Test the setValue() method
     *
     */
    #[@test]
    public function setValue() {
      ::with ($d= util::Date::now()); {
        $this->wrapper->setValue('orderdate', $d);
        $this->assertEquals($d, $this->wrapper->getValue('orderdate'));
      }
    }
    
    /**
     * Helper method to simulate form submission
     *
     */
    protected function loadFromRequest($params= array()) {
      $r= new scriptlet::xml::XMLScriptletRequest();
      
      foreach ($params as $key => $value) {
        $r->setParam($key, $value);
      }

      $this->wrapper->load($r, $this->handler);
    }

    /**
     * Helper method to assert a certain form error is available.
     *
     * Will fail if either no errors have occured at all or if the 
     * given error can not be found.
     *
     * @throws    unittest.AssertionFailedError
     */
    protected function assertFormError($field, $code) {
      if (!$this->handler->errorsOccured()) {     // Catch border-case
        $this->fail('No errors have occured', NULL, $code.' in field '.$field);
      }

      foreach ($this->handler->errors as $error) {
        if ($error[0].$error[1] == $code.$field) return;
      }

      $this->fail(
        'Error '.$code.' in field '.$field.' not in formerrors', 
        $this->handler->errors, 
        '(exists)'
      );
    }

    /**
     * Test the load() method
     *
     */
    #[@test]
    public function defaultValueUsedForMissingValue() {
      $this->loadFromRequest(array(
        'shirt_size' => 'S',
        'shirt_qty'  => 1
      ));
      $this->assertFalse($this->handler->errorsOccured());
      $this->assertEquals(
        $this->wrapper->getParamInfo('orderdate', PARAM_DEFAULT), 
        $this->wrapper->getValue('orderdate')
      );
    }

    /**
     * Test the load() method
     *
     */
    #[@test]
    public function defaultValueUsedForEmptyValue() {
      $this->loadFromRequest(array(
        'orderdate'  => '',
        'shirt_size' => 'S',
        'shirt_qty'  => 1
      ));
      $this->assertFalse($this->handler->errorsOccured());
      $this->assertEquals(
        $this->wrapper->getParamInfo('orderdate', PARAM_DEFAULT), 
        $this->wrapper->getValue('orderdate')
      );
    }

    /**
     * Test the load() method
     *
     */
    #[@test]
    public function valueUsed() {
      $this->loadFromRequest(array(
        'orderdate'  => '1977-12-14',
        'shirt_size' => 'S',
        'shirt_qty'  => 1
      ));
      $this->assertFalse($this->handler->errorsOccured());
      $this->assertEquals(
        new util::Date('1977-12-14'),
        $this->wrapper->getValue('orderdate')
      );
    }

    /**
     * Test the load() method
     *
     */
    #[@test]
    public function missingSizeValue() {
      $this->loadFromRequest(array(
        'orderdate'  => '',
        'shirt_qty'  => 1
      ));
      $this->assertFormError('shirt_size', 'missing');
    }

    /**
     * Test the load() method
     *
     */
    #[@test]
    public function missingQtyValue() {
      $this->loadFromRequest(array(
        'orderdate'  => '',
        'shirt_size' => 'S',
      ));
      $this->assertFormError('shirt_qty', 'missing');
    }
    
    /**
     * Test the load() method
     *
     */
    #[@test]
    public function malformedSizeValue() {
      $this->loadFromRequest(array(
        'orderdate'  => '',
        'shirt_size' => '@',
        'shirt_qty'  => 1
      ));
      $this->assertFormError('shirt_size', 'scriptlet.xml.workflow.checkers.OptionChecker.invalidoption');
    }

    /**
     * Test the load() method
     *
     */
    #[@test]
    public function malformedQtyValue() {
      $this->loadFromRequest(array(
        'orderdate'  => '',
        'shirt_size' => 'S',
        'shirt_qty'  => -1
      ));
      $this->assertFormError('shirt_qty', 'scriptlet.xml.workflow.checkers.IntegerRangeChecker.toosmall');
    }

    /**
     * Test the load() method
     *
     */
    #[@test]
    public function multipleMalformedValues() {
      $this->loadFromRequest(array(
        'orderdate'  => '',
        'shirt_size' => '@',
        'shirt_qty'  => -1
      ));
      $this->assertFormError('shirt_size', 'scriptlet.xml.workflow.checkers.OptionChecker.invalidoption');
      $this->assertFormError('shirt_qty', 'scriptlet.xml.workflow.checkers.IntegerRangeChecker.toosmall');
    }

    /**
     * Test the load() method
     *
     */
    #[@test]
    public function missingValueForMultipleSelection() {
      $this->loadFromRequest(array(
        'orderdate'  => '',
        'shirt_size' => 'S',
        'shirt_qty'  => 1,
      ));
      $this->assertFalse($this->handler->errorsOccured());
      $this->assertEquals(array(), $this->wrapper->getValue('notify_me'));
    }

    /**
     * Test the load() method
     *
     */
    #[@test]
    public function emptyValueForMultipleSelection() {
      $this->loadFromRequest(array(
        'orderdate'  => '',
        'shirt_size' => 'S',
        'shirt_qty'  => 1,
        'notify_me'  => array()
      ));
      $this->assertFalse($this->handler->errorsOccured());
      $this->assertEquals(array(), $this->wrapper->getValue('notify_me'));
    }

    /**
     * Test the load() method
     *
     */
    #[@test]
    public function valueForMultipleSelection() {
      $this->loadFromRequest(array(
        'orderdate'  => '',
        'shirt_size' => 'S',
        'shirt_qty'  => 1,
        'notify_me'  => array('send')
      ));
      $this->assertFalse($this->handler->errorsOccured());
      $this->assertEquals(array('send'), $this->wrapper->getValue('notify_me'));
    }

    /**
     * Test the load() method
     *
     */
    #[@test]
    public function valuesForMultipleSelection() {
      $this->loadFromRequest(array(
        'orderdate'  => '',
        'shirt_size' => 'S',
        'shirt_qty'  => 1,
        'notify_me'  => array('send', 'process')
      ));
      $this->assertFalse($this->handler->errorsOccured());
      $this->assertEquals(array('send', 'process'), $this->wrapper->getValue('notify_me'));
    }
  }
?>