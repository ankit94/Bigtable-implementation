package tests;

import java.io.*;
import java.util.*;
import java.lang.*;
import chainexception.*;
import global.SystemDefs;

//    Major Changes:
//    1. Change the return type of test() functions from 'int' to 'boolean'
//       to avoid defining static int TRUE/FALSE, which makes it easier for
//       derived functions to return the right type.
//    2. Function runTest is not implemented to avoid dealing with function
//       pointers.  Instead, it's flattened in runAllTests() function.
//    3. Change
//          Status TestDriver::runTests()
//     	    Status TestDriver::runAllTests()
//       to
//          public boolean runTests();
//          protected boolean runAllTests();

/** 
 * TestDriver class is a base class for various test driver
 * objects.
 * <br>
 * Note that the code written so far is very machine dependent.  It assumes
 * the users are on UNIX system.  For example, in function runTests, a UNIX
 * command is called to clean up the working directories.
 * 
 */

public class TestDriver {

  public final static boolean OK   = true; 
  public final static boolean FAIL = false; 

  protected String dbpath;  
  protected String logpath;
  

  /** 
   * TestDriver Constructor 
   *
   * @param nameRoot The name of the test being run
   */

  protected TestDriver (String nameRoot) {

  //  sprintf( dbpath, MINIBASE_DB, nameRoot, getpid() );
  //  sprintf( logpath, MINIBASE_LOG, nameRoot, getpid() );

    //NOTE: Assign random numbers to the dbpath doesn't work because
    //we can never open the same database again if everytime we are
    //given a different number.  

    //To port it to a different platform, get "user.name" should
    //still work well because this feature is not meant to be UNIX
    //dependent. 
    dbpath = "D:\\minibase_db\\"+nameRoot+System.getProperty("user.name")+".minibase-db"; 
    logpath = "D:\\minibase_db\\"+nameRoot +System.getProperty("user.name")+".minibase-log"; 
  }

  /**
   * Another Constructor
   */

  protected TestDriver () {}

  /** 
   * @return whether the test has completely successfully 
 * @throws IOException 
   */
  protected boolean test1 () { return true; }
  
  /** 
   * @return whether the test has completely successfully 
   */
  protected boolean test2 () { return true; }

  /** 
   * @return whether the test has completely successfully 
   */
  protected boolean test3 () { return true; }

  /** 
   * @return whether the test has completely successfully 
   */
  protected boolean test4 () { return true; }

  /** 
   * @return whether the test has completely successfully 
   */
  protected boolean test5 () { return true; }

  /** 
   * @return whether the test has completely successfully 
   */
  protected boolean test6 () { return true; }

  /** 
   * @return <code>String</code> object which contains the name of the test
   */
  protected String testName() { 

    //A little reminder to subclassers 
    return "*** unknown ***"; 

  }

  public void setup() {
      SystemDefs sysdef = new SystemDefs(dbpath, 100, 100, "Clock");
  }

  public void cleanUp() {
    // Kill anything that might be hanging around
    String newdbpath;
    String newlogpath;
    String remove_logcmd;
    String remove_dbcmd;
    String remove_cmd = "/bin/rm -rf ";

    newdbpath = dbpath;
    newlogpath = logpath;

    remove_logcmd = remove_cmd + logpath;
    remove_dbcmd = remove_cmd + dbpath;

    // Commands here is very machine dependent. We assume
    // user are on UNIX system here
    try {
      Runtime.getRuntime().exec(remove_logcmd);
      Runtime.getRuntime().exec(remove_dbcmd);
    } catch (IOException e) {
      System.err.println("IO error: " + e);
    }

    remove_logcmd = remove_cmd + newlogpath;
    remove_dbcmd = remove_cmd + newdbpath;

    try {
      Runtime.getRuntime().exec(remove_logcmd);
      Runtime.getRuntime().exec(remove_dbcmd);
    } catch (IOException e) {
      System.err.println("IO error: " + e);
    }
  }

  /**
   * This function does the preparation/cleaning work for the
   * running tests.
   *
   * @return a boolean value indicates whether ALL the tests have passed
 * @throws IOException 
   */
  public boolean runTests() throws IOException {
      cleanUp();
      setup();

      System.out.println("\n" + "Running " + testName() + " tests...." + "\n");

      // Run the tests. Return type different from C++
      boolean _pass = runAllTests();

      // Clean up again
      cleanUp();

      System.out.print("\n" + "..." + testName() + " tests ");
      System.out.print(_pass == OK ? "completely successfully" : "failed");
      System.out.print(".\n\n");

      return _pass;
  }

  protected boolean runAllTests() throws IOException {

    boolean _passAll = OK;

    //The following code checks whether appropriate erros have been logged,
    //which, if implemented, should be done for each test case.  

    //minibase_errors.clear_errors();
    //int result = test();
    //if ( !result || minibase_errors.error() ) {
    //  status = FAIL;
    //  if ( minibase_errors.error() )
    //    cerr << (result? "*** Unexpected error(s) logged, test failed:\n"
    //    : "Errors logged:\n");
    //    minibase_errors.show_errors(cerr);
    //}
    
    //The following runs all the test functions without checking
    //the logged error types. 

    //Running test1() to test6()
    if (!test1()) { _passAll = FAIL; }
    if (_passAll && !test2()) { _passAll = FAIL; }
    if (_passAll && !test3()) { _passAll = FAIL; }
    if (_passAll && !test4()) { _passAll = FAIL; }
    if (_passAll && !test5()) { _passAll = FAIL; }
    if (_passAll && !test6()) { _passAll = FAIL; }

    return _passAll;
  }

  /**
   * Used to verify whether the exception thrown from
   * the bottom layer is the one expected.
   */
  public boolean checkException (ChainException e, 
				 String expectedException) {

    boolean notCaught = true;
    while (true) {
      
      String exception = e.getClass().getName();
      
      if (exception.equals(expectedException)) {
	return (!notCaught);
      }
      
      if ( e.prev==null ) {
	return notCaught;
      }
      e = (ChainException)e.prev;
    }
    
  } // end of checkException
  
} // end of TestDriver  
