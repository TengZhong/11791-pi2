package org.lappsgrid.example;

import org.lappsgrid.api.ProcessingService;
import org.lappsgrid.discriminator.Discriminators.Uri;

import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.DataContainer;
import org.lappsgrid.serialization.Serializer;
import org.lappsgrid.serialization.lif.Annotation;
import org.lappsgrid.serialization.lif.Container;
import org.lappsgrid.serialization.lif.View;
import org.lappsgrid.vocabulary.Features;

// additional API for metadata
import org.lappsgrid.metadata.IOSpecification;
import org.lappsgrid.metadata.ServiceMetadata;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * For tutorial step #3, writing getMetadata()
 */
public class AnswerScoring implements ProcessingService
{
    /**
     * The Json String required by getMetadata()
     */
    private String metadata;


    public AnswerScoring() {

        metadata = generateMetadata();

    }

    private String generateMetadata() {
        // Create and populate the metadata object
        ServiceMetadata metadata = new ServiceMetadata();

        // Populate metadata using setX() methods
        metadata.setName(this.getClass().getName());
        metadata.setDescription("Whitespace tokenizer");
        metadata.setVersion("1.0.0-SNAPSHOT");
        metadata.setVendor("http://www.lappsgrid.org");
        metadata.setLicense(Uri.APACHE2);

        // JSON for input information
        IOSpecification requires = new IOSpecification();
        requires.addFormat(Uri.TEXT);           // Plain text (form)
        requires.addFormat(Uri.LIF);            // LIF (form)
        requires.addLanguage("en");             // Source language
        requires.setEncoding("UTF-8");

        // JSON for output information
        IOSpecification produces = new IOSpecification();
        produces.addFormat(Uri.LAPPS);          // LIF (form) synonymous to LIF
        produces.addAnnotation(Uri.TOKEN);      // Tokens (contents)
        requires.addLanguage("en");             // Target language
        produces.setEncoding("UTF-8");

        // Embed I/O metadata JSON objects
        metadata.setRequires(requires);
        metadata.setProduces(produces);

        // Serialize the metadata to a string and return
        Data<ServiceMetadata> data = new Data<ServiceMetadata>(Uri.META, metadata);
        return data.asPrettyJson();
    }

    @Override
    /**
     * getMetadata simply returns metadata populated in the constructor
     */
    public String getMetadata() {
        return metadata;
    }

    @Override
    public String execute(String input) {
        // Step #1: Parse the input.
        Data data = Serializer.parse(input, Data.class);

        // Step #2: Check the discriminator
        final String discriminator = data.getDiscriminator();
        if (discriminator.equals(Uri.ERROR)) {
            // Return the input unchanged.
            return input;
        }

        // Step #3: Extract the text.
        Container container = null;
        if (discriminator.equals(Uri.TEXT)) {
            container = new Container();
            container.setText(data.getPayload().toString());
        }
        else if (discriminator.equals(Uri.LAPPS)) {
            container = new Container((Map) data.getPayload());
        }
        else {
            // This is a format we don't accept.
            String message = String.format("Unsupported discriminator type: %s", discriminator);
            return new Data<String>(Uri.ERROR, message).asJson();
        }

        // Step #4#5: Create a new View
        View view2 = container.getView(2); // from NGramAnnotation
        View view3 = container.newView();
        try {
          List<Annotation> annotations = view2.getAnnotations();
          
          // extract info about the question
          Annotation questionAnnotation = annotations.get(0);
          String sentence = questionAnnotation.getId();
          Set<String> qGram1 = questionAnnotation.getFeatureSet("1-gram");
          Set<String> qGram2 = questionAnnotation.getFeatureSet("2-gram");
          Set<String> qGram3 = questionAnnotation.getFeatureSet("3-gram");
          Annotation q_tmp = view3.newAnnotation(sentence, Uri.TOKEN);
          q_tmp.addFeature("qGram1", qGram1);
          q_tmp.addFeature("qGram2", qGram2);
          q_tmp.addFeature("qGram3", qGram3);
          
          // get scores for each answers
          int len = annotations.size();
          for (int i = 1; i < len; i++) {
            Annotation tmpAnnotation = annotations.get(i);
            String score = tmpAnnotation.getFeature("Score");
            String tmpSentence = tmpAnnotation.getId();
            Set<String> tmpGram1 = tmpAnnotation.getFeatureSet("1-gram");
            Set<String> tmpGram2 = tmpAnnotation.getFeatureSet("2-gram");
            Set<String> tmpGram3 = tmpAnnotation.getFeatureSet("3-gram");
            
            Set<String> intersection1 = new HashSet<String>(tmpGram1), union1 = 
                    new HashSet<String>(tmpGram1);
            intersection1.retainAll(qGram1);
            union1.addAll(qGram1);
            Set<String> intersection2 = new HashSet<String>(tmpGram2), union2 = 
                    new HashSet<String>(tmpGram2);
            intersection2.retainAll(qGram2);
            union2.addAll(qGram2);
            Set<String> intersection3 = new HashSet<String>(tmpGram3), union3 = 
                    new HashSet<String>(tmpGram3);
            intersection3.retainAll(qGram3);
            union3.addAll(qGram3);
            
            float gram1Score = (float)intersection1.size() / union1.size();
            float gram2Score = (float)intersection2.size() / union2.size();
            float gram3Score = (float)intersection3.size() / union3.size();
            
            Annotation ans_tmp = view3.newAnnotation(tmpSentence, Uri.TOKEN);
            ans_tmp.addFeature("gram1Score", gram1Score + "");
            ans_tmp.addFeature("gram2Score", gram2Score + "");
            ans_tmp.addFeature("gram3Score", gram3Score + "");
            ans_tmp.addFeature("sum of score", gram1Score + gram2Score + gram3Score + "");
            ans_tmp.addFeature("Score", score);
          }
        } catch (Exception e) {
          System.out.println(e.getStackTrace());
        }
        
           

        // Step #6: Update the view's metadata. Each view contains metadata about the
        // annotations it contains, in particular the name of the tool that produced the
        // annotations.
        view3.addContains(Uri.TOKEN, this.getClass().getName(), "AnswerScoring");

        // Step #7: Create a DataContainer with the result.
        data = new DataContainer(container);

        // Step #8: Serialize the data object and return the JSON.
        return data.asPrettyJson();
    }
}