package org.lappsgrid.example;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import org.lappsgrid.api.WebService;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.Serializer;
import org.lappsgrid.serialization.lif.Annotation;
import org.lappsgrid.serialization.lif.Container;
import org.lappsgrid.serialization.lif.View;


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
    BufferedWriter bw;
    try {
      bw = new BufferedWriter(new FileWriter(filePath));
      bw.write(outputJson);
      bw.close();
    } catch (Exception ex) {
      System.out.println(ex.getStackTrace());
    } 
  } 
  
  
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
    String stageInput = getPipelineInput();
    String intermediateOutput = "";
    for(WebService service: getPipelineStages()) {
      intermediateOutput = service.execute(stageInput);
      stageInput = intermediateOutput;
    }
    setOutput(intermediateOutput);
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
    File inputDir = new File(inputPath);
    for (File file: inputDir.listFiles()) {
      setPipelineInput(file.getAbsolutePath());
      QAPipeline pp = new QAPipeline();
      pp.addService(new TestEleAnnotation());
      pp.addService(new TokenAnnotation());
      pp.addService(new NGramAnnotation());
      pp.addService(new AnswerScoring());
      pp.addService(new Evaluation());
      pp.runPipeline();
      
      Data data = Serializer.parse(getOutput(), Data.class);
      Container container = new Container((Map) data.getPayload());
      View view4 = container.getView(4); // Evaluation
      View view3 = container.getView(3); // AnswerScoring
      String processedOutput = "";
      
      // extract P@N
      List<Annotation> annotations4 = view4.getAnnotations();
      String PAtN = annotations4.get(0).getFeature("P@N"); // we use P@3
      processedOutput = PAtN + "\n";
      
      // extract scores for all answers
      List<Annotation> annotations3 = view3.getAnnotations();
      PriorityQueue<String> pq = new PriorityQueue<String>(annotations3.size(), new scoreComparator());      
      for (int i = 1; i < annotations3.size(); i++) { // the first is question
        String typeAndScore = annotations3.get(i).getFeature("thisType") + " " +
                annotations3.get(i).getFeature("sum of score");
        pq.offer(typeAndScore);
      }      
      while (pq.size() > 0) {
        processedOutput += pq.poll() + "\n";
      }
      
      // write to output
      pp.writeOutput(outputPath, processedOutput);
      
    }       
  }
}


class scoreComparator<String> implements Comparator<String> {
  public int compare(String s1, String s2) {
    Double score1 = Double.parseDouble(((java.lang.String) s1).split(" ")[1]);
    Double score2 = Double.parseDouble(((java.lang.String) s2).split(" ")[1]);
    return -1 * Double.compare(score1, score2);
  }
}
