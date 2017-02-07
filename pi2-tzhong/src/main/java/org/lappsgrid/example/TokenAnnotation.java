package org.lappsgrid.example;

import org.lappsgrid.api.ProcessingService;

import static org.lappsgrid.discriminator.Discriminators.Uri;
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

import java.util.List;
import java.util.Map;

/**
 * For tutorial step #3, writing getMetadata()
 */
public class TokenAnnotation implements ProcessingService
{
    /**
     * The Json String required by getMetadata()
     */
    private String metadata;


    public TokenAnnotation() {

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
        requires.addFormat(Uri.LAPPS);            // LIF (form)
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

        // Step #4#5: Create a new View and tokenize the previous annotations
        View view = container.getView(0);
        View view1 = container.newView();
        List<Annotation> annotations = view.getAnnotations();
        int numSentence = 0;
        for (Annotation a: annotations) {
          String sentence = a.getFeature("Sentence");
          String[] words = sentence.trim().replaceAll("[-+.^:,?]", "").split("\\s+");
          int id = -1;
          int start = 0;
          for (String word : words) {
              start = sentence.indexOf(word, start);
              if (start < 0) {
                  return new Data<String>(Uri.ERROR, "Unable to match word: " + word).asJson();
              }
              int end = start + word.length();
              Annotation tmpa = view1.newAnnotation("tok" + (++id), Uri.TOKEN, start, end);
              tmpa.addFeature(Features.Token.WORD, word);
              tmpa.addFeature("Sentence Number(0 means Question)", numSentence + "");
              numSentence++;
          }
        }


        // Step #6: Update the view's metadata. Each view contains metadata about the
        // annotations it contains, in particular the name of the tool that produced the
        // annotations.
        view1.addContains(Uri.TOKEN, this.getClass().getName(), "TokenAnnotation");

        // Step #7: Create a DataContainer with the result.
        data = new DataContainer(container);

        // Step #8: Serialize the data object and return the JSON.
        return data.asPrettyJson();
    }
}