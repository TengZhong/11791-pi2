package org.lappsgrid.example;

import java.util.List;

import org.lappsgrid.api.WebService;
import org.lappsgrid.discriminator.Discriminators.Uri;
import org.lappsgrid.serialization.Data;

public abstract class Pipeline {
  
  private List<WebService> Stages;
  private Data input;
  private String output;
  
  public List<WebService> getPipelineStages()
  {
    return Stages;
  }
  
  public void addService(WebService newService)
  {
    Stages.add(newService);
  }
  
  public void setPipelineInput(String docText)
  {
    input = new Data<>(Uri.TEXT, docText);
  }
  
  public String getPipelineInput()
  {
    return input.asJson();
  }
  
  private void setOutput(String jsonText)
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
