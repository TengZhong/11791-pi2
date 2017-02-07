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

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * For tutorial step #3, writing getMetadata()
 */
public class Evaluation implements ProcessingService
{
    /**
     * The Json String required by getMetadata()
     */
    private String metadata;


    public Evaluation() {

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
        View view3 = container.getView(3); // from AnserScoring
        View view4 = container.newView();
        List<Annotation> annotations = view3.getAnnotations();
        int len = annotations.size(), N = 0; // N is number of correct documents
        PriorityQueue<Annotation> pq = new PriorityQueue<Annotation>((Collection<? extends org.lappsgrid.serialization.lif.Annotation>) new myComparator());
        for (int i = 1; i < len; i++) { // skip the question
          Annotation tmpAnnotation = annotations.get(i);
          if (Integer.parseInt(tmpAnnotation.getFeature("Score")) == 1) {
            N++;
          }
          pq.offer(tmpAnnotation);
        }
        
        float prec = 0; 
        for (int i = 0; i < N; i++) {
          Annotation tmp = pq.poll();
          if (tmp.getFeature("Score").equals("1")) prec++;
        }
        
        Annotation a = view4.newAnnotation("Precision at " + N, Uri.TOKEN);
        a.addFeature("P@" + N, String.valueOf(prec / N));
        

        // Step #6: Update the view's metadata. Each view contains metadata about the
        // annotations it contains, in particular the name of the tool that produced the
        // annotations.
        view4.addContains(Uri.TOKEN, this.getClass().getName(), "Evaluation");

        // Step #7: Create a DataContainer with the result.
        data = new DataContainer(container);

        // Step #8: Serialize the data object and return the JSON.
        return data.asPrettyJson();
    }
    
    
    
    private class myComparator<Annotation> implements Comparator<Annotation> {
      @Override
      public int compare(Annotation a1, Annotation a2) {
        float f1 = Float.parseFloat(((org.lappsgrid.serialization.lif.Annotation) a1).getFeature("sum of score"));
        float f2 = Float.parseFloat(((org.lappsgrid.serialization.lif.Annotation) a2).getFeature("sum of score"));
        if (f1 < f2) {
          return 1;
        } else if (f1 > f2) {
          return -1;
        } else {
          return 0;
        }
      }
    }
    
    
}

