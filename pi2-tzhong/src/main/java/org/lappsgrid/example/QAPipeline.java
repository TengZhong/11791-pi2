package org.lappsgrid.example;

public class QAPipeline extends Pipeline{
  
  /*
   * Treat this as the entry point to your Web Service pipeline.
   * The abstract class Pipeline is a collection of web services with getter and setter methods.
   * Read more about abstract classes here - https://www.tutorialspoint.com/java/java_abstraction.htm
   * 
   * What other design patterns would fit better? (Think around the lines of Interface
   * and abstract class implementing the interface)  
   */
  
  @Override
  String readInput(String filePath) {
    /*
     * Read in the file into a String
     */
    return null;
  }

  @Override
  void writeOutput(String filePath, String outputJson) {
    /*
     * The Pipeline returns a JSON String
     * Write the output in the desired format using this method
     */
  } 
  
  @Override
  void runPipeline() {
    /*
     * One possible implementation could be this
     * String stageInput = getPipelineInput();
     * String intermediateOutput = "";
     * for(WebService service: getPipelineStages())
     *  {
     *    intermediateOutput = service.execute(stageInput);
     *    stageInput = intermediateOutput;
     *  }
     *  setOutput(intermediateOutput);
     */
  }
  
  public static void main(String[] args) {
    
    String inputPath = args[1];
    String outputPath = args[2];
    
    /*
     * A guideline for implementing this method
     * 1) Implement the readInput() method
     * 2) Create a Pipeline object and setup the Pipeline by initializing 
     *    input to the pipeline, the web service sequence and run the pipeline
     *    You can do this by:
     *    - set the input to the pipeline using setPipelineInput()
     *    - add the services into your pipeline using addService()
     *      Eg: pipelineObject.addService(new TestElementAnnotation());
     *    - call the runPipeline() method
     * 3) Implement the writeOutput() method 
     */

  }

}
