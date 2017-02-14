package org.lappsgrid.example;

import java.util.ArrayList;
import java.util.List;

import org.lappsgrid.api.WebService;
import org.lappsgrid.discriminator.Discriminators.Uri;
import org.lappsgrid.serialization.Data;

public abstract class Pipeline {
  
  private static List<WebService> Stages = new ArrayList<WebService>();
  private static Data input;
  public static String output;
  
  public static List<WebService> getPipelineStages()
  {
    return Stages;
  }
  
  public static void addService(WebService newService)
  {
    Stages.add(newService);
  }
  
  public static void setPipelineInput(String docText)
  {
    input = new Data<>(Uri.TEXT, docText);
  }
  
  public static String getPipelineInput()
  {
    return input.asJson();
  }
  
  protected static void setOutput(String jsonText)
  {
    output = jsonText;
  }
  
  public String getOutput()
  {
    return output;
  }
  
  abstract String readInput(String filePath);
  
  abstract void writeOutput(String filePath, String outputJson);
  
  abstract void runPipeline();
  
}
