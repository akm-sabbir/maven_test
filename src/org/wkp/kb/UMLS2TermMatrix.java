package org.wkp.kb;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import monq.jfa.ReSyntaxException;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.wkp.extraction.metamap.MetaMapCooccurrences;
import org.wkp.mc.MarkovChain;
import org.wkp.model.ConceptMatrix;
import org.wkp.model.ProbabilityModel;
import org.wkp.model.TokenMatrix;
import org.wkp.utils.CommonWords;
import org.wkp.utils.PorterStemmer;
import org.wkp.utils.Util;

public class UMLS2TermMatrix extends AbstractKB
{
  private static final Pattern ptoken = Util.getPToken();

  private static Pattern p = Pattern.compile("\\|");
  
  private static String terms_file = null;
  private static String relations_file = null;
  private static String cooccurrences_folder = null;

  static
  {
	try
	{
	  Configuration config = new PropertiesConfiguration("experiment.properties");
	  terms_file = config.getString("umls.terms");
	  relations_file = config.getString("umls.relations");
	  cooccurrences_folder = config.getString("umls.cooccurrences");
	}
	catch (ConfigurationException e)
	{ throw new ExceptionInInitializerError(e);	}
  }
  
  public static void main (String [] argc)
  throws IOException, ReSyntaxException, ClassNotFoundException
  {
	if (argc.length != 4)
	{
	  System.err.println("UMLSTermMatrix term_file_name relation_file_name serialized_model_file_names cooccurrences_(true/false)");
	  System.exit(-1);
	}

	/*DisambiguationModel dm =
			generateModel(argc[0], argc[1], new Boolean(argc[3]));

	// Serialize the model
	{
      ObjectOutputStream o = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(argc[2])));
      o.writeObject(dm);
	  o.close();
	}*/
  }

  public ProbabilityModel generateModel()

  {
    try
    {
	  return generateModel(terms_file,
		                     relations_file,
		                     null,
		                     null,
		                     false
		          );
	}
	catch (IOException e)
	{ throw new RuntimeException (e); }
	catch (ReSyntaxException e)
	{ throw new RuntimeException (e); }
	catch (ClassNotFoundException e)
	{ throw new RuntimeException (e);	}
  }

  public ProbabilityModel generateModel(ProbabilityModel dmMM,
		                                       boolean cooccurrences
		                                       )
  {
	try
	{
		return generateModel(terms_file,
		                     relations_file,
		                     dmMM,
		                     cooccurrences_folder,
		                     cooccurrences
		        );
	}
	catch (IOException e)
	{ throw new RuntimeException (e); }
	catch (ReSyntaxException e)
	{ throw new RuntimeException (e); }
	catch (ClassNotFoundException e)
	{ throw new RuntimeException (e);	}
  }
  
  public static ProbabilityModel generateModel(String term_file_name,
		                                          String relation_file_name,
		                                          ProbabilityModel dmMM,
		                                          String folder_name,
		                                          boolean cooccurrences
		                                          )
  throws IOException, ReSyntaxException, ClassNotFoundException
  {
	ProbabilityModel dm = new ProbabilityModel();

    TokenMatrix tm = new TokenMatrix ();
	ConceptMatrix cm = new ConceptMatrix();
	
	dm.setTokenMatrix(tm);
	dm.setConceptMatrix(cm);

    PorterStemmer stemmer = new PorterStemmer();
    
    if (cooccurrences)
    {
      MetaMapCooccurrences.tm = dmMM.getTokenMatrix();
      MetaMapCooccurrences.cm = dmMM.getConceptMatrix();

      MetaMapCooccurrences.map_concept_concept.clear();
      MetaMapCooccurrences.map_term_concept.clear();

      MetaMapCooccurrences.profiles.clear();

      MetaMapCooccurrences.getStatistics(folder_name);

      MetaMapCooccurrences.tm = null;
      MetaMapCooccurrences.cm = null;
      MetaMapCooccurrences.profiles.clear();
    }

    {
	  // Load concepts
      BufferedReader b =
    		  new BufferedReader(
    				  new InputStreamReader(
    						  new GZIPInputStream(new FileInputStream(term_file_name))));

      String line;

      int i = 0;

      while ((line = b.readLine()) != null && i < 1000000000)
      {
        String [] tokens = p.split(line);

        if (tokens[1].equals("ENG"))
        {
   	      i++;

          int cui = cm.getIndexConcept(tokens[0]);

          Map <Integer, Double> concept_terms = tm.getTM().get(cui);

          if (concept_terms == null)
          {
            concept_terms = new HashMap <Integer, Double> ();
            tm.getTM().put(cui, concept_terms);
          }

	      // Decompose the terms
	      for (String token : ptoken.split(tokens[14].toLowerCase()))
	      {
   	        if (token.trim().length() > 0 && !CommonWords.checkWord(token) && !Util.isNumber(token))
	        {
	          // Look for a stemmer
	          String token_stemmed = stemmer.stem(token);

              Integer index = tm.getIndexToken(token_stemmed);

              if (concept_terms.get(index) == null)
              { concept_terms.put(index, 1.0); }
              else
              { concept_terms.put(index, concept_terms.get(index) + 1.0); }
	        }
	      }

	      if (i % 10000 == 0)
	      { System.out.println(i); }
	    }
      }

      b.close();
    }

    // Add frequencies from corpora before turning frequencies to probabilities
    if (cooccurrences)
    {
      for (Map.Entry <String, Map <String, Integer>> entry : MetaMapCooccurrences.map_term_concept.entrySet())
      {
    	for (Map.Entry <String, Integer> cf : entry.getValue().entrySet())
    	{
          int cui = cm.getIndexConcept(cf.getKey());

          Map <Integer, Double> concept_terms = tm.getTM().get(cui);

          // Only consider it if there is a concept related to the terms already
          if (concept_terms != null)
          {
            for (String token : ptoken.split(entry.getKey().toLowerCase()))
	        {
   	          if (token.trim().length() > 0 && !CommonWords.checkWord(token) && !Util.isNumber(token))
	          {
	            // Look for a stemmer
	            String token_stemmed = stemmer.stem(token);

                Integer index = tm.getIndexToken(token_stemmed);
            
                // Consider it only if the token was already linked to it
                if (concept_terms.get(index) != null)
                { concept_terms.put(index, concept_terms.get(index) + cf.getValue()); }
	          }
	        }
          }
    	}
      }
    }

    //Normalize the terms
    for (Map.Entry <Integer, Map <Integer, Double>> entry : tm.getTM().entrySet())
  	{
      double sum = 0;

      for (Map.Entry <Integer, Double> entry2 : entry.getValue().entrySet())
      { sum += entry2.getValue(); }

      for (Map.Entry <Integer, Double> entry2 : entry.getValue().entrySet())
      { entry2.setValue(Math.log(entry2.getValue()/sum)); }
  	}

    double sum = 0;
    
    for (Map.Entry <Integer, Double> entry : tm.getTM().get(cm.getIndexConcept("C0009264")).entrySet())
    { System.out.println(tm.getIndexToken().get(entry.getKey()) + "|" + entry.getKey() + "|" + Math.exp(entry.getValue()));
      sum += Math.exp(entry.getValue());
    }

    System.out.println(sum);
    
    System.out.println("Done loading terms!");

    // UMLS2012AB MRREL relation type statistics
    //    3888020 CHD
    //    3888020 PAR
    //     599827 QB
    //    1645606 RB
    //    1645606 RN
    //    11050048 RO
    //    1585214 RQ
    //    21178996 SIB
    //    5311782 SY

    //    AQ: allowed qualifier
    //    CHD: has child (narrower hierarchical term)
    //    DEL: deleted concept
    //    PAR: has parent (broader hierarchical term)
    //    QB: can be qualifier by
    //    RB: has a broader relationship
    //    RL: has similar or like relationship
    //    RN: has narrower relationship
    //    RO: has relationship other than synonymous, narrower or broader
    //    RQ: related and possibly synonymous
    //    SIB: has sibling
    //    SY: source-asserted synonymy
	// Load concept relations
	{
	  BufferedReader b =
			  new BufferedReader(
					  new InputStreamReader(
							  new GZIPInputStream(new FileInputStream(relation_file_name))));

      String line;

      int count = 0;

      while ((line = b.readLine()) != null && count < 100000000)
      {
        String [] tokens = p.split(line);

        // Consider it just in one direction
        if (!tokens[0].equals(tokens[4])) // && (tokens[0].equals("C0009264") || tokens[4].equals("C0009264")))
        {
          if (tokens[3].equals("PAR") ||
        	  tokens[3].equals("RB") ||
        	  tokens[3].equals("RN") ||
       	      tokens[3].equals("RO")) 
          {
            count++;

            Integer cui1 = cm.getIndexConcept(tokens[0]);
            Integer cui2 = cm.getIndexConcept(tokens[4]);

            Map <Integer, Double> to = cm.getCM()[cui1];

            if (to == null)
            {
              to = new HashMap <Integer, Double> ();
              cm.getCM()[cui1] = to;
            }

            if (to.get(cui2) == null)
            { to.put(cui2, 1.0); }
            else
            { to.put(cui2, to.get(cui2) + 1.0); }

            if (count % 10000 == 0) System.out.println(count);
          }
        }
      }

      System.out.println("Get the probabilities");
      Map <Integer, Map <Integer, Double>> tm_col = new HashMap <Integer, Map <Integer, Double>> ();

      // Add the frequencies from corpora
      if (cooccurrences)
      {
        for (Map.Entry <String, Map <String, Integer>> entry : MetaMapCooccurrences.map_concept_concept.entrySet())
        {
          Integer cui1 = cm.getIndexConcept(entry.getKey());

          Map <Integer, Double> to = cm.getCM()[cui1];

          // Only if the relation existed for cui1
          if (to != null)
          {
            for (Map.Entry <String, Integer> cf : entry.getValue().entrySet())
            {
        	  Integer cui2 = cm.getIndexConcept(cf.getKey());

        	  // Only if there was already a relation to this concept
              if (to.get(cui2) != null)
              { to.put(cui2, to.get(cui2) + cf.getValue()); }
              else
              { to.put(cui2, new Double(cf.getValue())); }
            }
          }
        }
      }

      // Turn frequencies into probabilities
      for (int i = 0; i < cm.getConceptIndex(); i++)
      {
    	double sumC = 0;

    	if (cm.getCM()[i] != null)
    	{
          for (Map.Entry <Integer, Double> entry2 : ((Map <Integer, Double>)cm.getCM()[i]).entrySet())
          { sumC += entry2.getValue(); }

          for (Map.Entry <Integer, Double> entry2 : ((Map <Integer, Double>)cm.getCM()[i]).entrySet())
          {
        	Map <Integer, Double> col = tm_col.get(entry2.getKey());

        	if (col == null)
        	{
        	  col = new HashMap <Integer, Double> ();
        	  tm_col.put(entry2.getKey(), col);
        	}

        	double value = Math.log(entry2.getValue()/sumC);
        	col.put(i, value);
        	entry2.setValue(value);
          }
    	}
      }

	  b.close();

	  System.out.println("Done!");

      System.out.println("Count: " + count);
      System.out.println("Concept count: " + cm.getConceptIndex());
      System.out.println("Done!");

      dm.setPC(MarkovChain.converge(tm_col, cm.getConceptIndex()));
    }

	return dm;
  }
}