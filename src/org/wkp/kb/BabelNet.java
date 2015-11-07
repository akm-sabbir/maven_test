package org.wkp.kb;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.wkp.extraction.tkbg.Cooccurrences;
import org.wkp.mc.MarkovChain;
import org.wkp.model.ConceptMatrix;
import org.wkp.model.ProbabilityModel;
import org.wkp.model.TokenMatrix;
import org.wkp.utils.CommonWords;
import org.wkp.utils.PorterStemmer;
import org.wkp.utils.Util;

/**
 * Generate model for BabelNet
 * 
 * @author Antonio Jimeno Yepes (antonio.jimeno@gmail.com)
 *
 */
public class BabelNet extends AbstractKB
{
  private static final Pattern p = Pattern.compile("\\|");
  private static final Pattern p_comma = Pattern.compile(",");
  private static final Pattern ptoken = Util.getPToken();

  private static String terms_file = null;
  private static String relations_file = null;
  private static String cooccurrences_folder = null;

  private static String p_w_c_file = null;
  
  static
  {
	try
	{
	  Configuration config = new PropertiesConfiguration("experiment.properties");
	  terms_file = config.getString("babelnet.terms");
	  relations_file = config.getString("babelnet.relations");
	  cooccurrences_folder = config.getString("babelnet.cooccurrences");

	  p_w_c_file = config.getString("p_w_c");
	}
	catch (ConfigurationException e)
	{ throw new ExceptionInInitializerError(e);	}
  }
  
  public ProbabilityModel generateModel(ProbabilityModel dmMM, boolean cooccurrences)
  {
	ProbabilityModel dm = new ProbabilityModel();
	
	try
	{
	  if (cooccurrences)
	  {
	    Cooccurrences.tm = dmMM.getTokenMatrix();
	    Cooccurrences.cm = dmMM.getConceptMatrix();

	    Cooccurrences.map_concept_concept.clear();
	    Cooccurrences.map_term_concept.clear();

	    Cooccurrences.profiles.clear();

	    Cooccurrences.getStatistics(cooccurrences_folder);

	    Cooccurrences.tm = null;
	    Cooccurrences.cm = null;
	    Cooccurrences.profiles.clear();
	  }
		
      TokenMatrix tm = new TokenMatrix ();
	  ConceptMatrix cm = new ConceptMatrix();
		
	  dm.setTokenMatrix(tm);
	  dm.setConceptMatrix(cm);

      PorterStemmer stemmer = new PorterStemmer();

      Map <Integer, Double> p_w_c = new HashMap <Integer, Double> ();
    
      String line;

      BufferedReader b = new BufferedReader(new FileReader(terms_file));

      int count = 0;
      long total_count = 0;
    
      while ((line = b.readLine()) != null)
      {
        count++;

        String [] tokens = p.split(line);
      
        if (tokens.length == 2)
        {
          int cui = cm.getIndexConcept(tokens[0]);

          Map <Integer, Double> concept_terms = tm.getTM().get(cui);

          if (concept_terms == null)
          {
            concept_terms = new HashMap <Integer, Double> ();
            tm.getTM().put(cui, concept_terms);
          }

	      // Decompose the terms
	      for (String token : ptoken.split(tokens[1].toLowerCase()))
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
            
              if (p_w_c.get(index) == null)
              { p_w_c.put(index, 1.0); }
              else
              { p_w_c.put(index, p_w_c.get(index) + 1.0); }
            
              total_count++;
	        }
	      }

	      if (count % 10000 == 0)
	      { System.out.println(count); }
        }
      }

      b.close();

      // Add frequencies from corpora before turning frequencies to probabilities
      if (cooccurrences)
      {
        for (Map.Entry <String, Map <String, Integer>> entry : Cooccurrences.map_term_concept.entrySet())
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
    
      // p_w_c
      /*Map <Integer, Double> p_w_c_final = new HashMap <Integer, Double> ();
    
      for (Map.Entry <Integer, Double> map : p_w_c.entrySet())
      { p_w_c_final.put(map.getKey(), Math.log((1.0 + map.getValue())/(double)(p_w_c.size() + total_count)) ); }
    
      ObjectOutputStream o = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(p_w_c_file)));
      o.writeObject(p_w_c_final);
      o.close();*/

      BufferedReader brel = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(relations_file))));

      Integer cui_from = null;
    
      count = 0;
    
      while ((line=brel.readLine()) != null)
      {
        line = line.trim();

        if (line.trim().length() == 0)
        { cui_from = null; }
        if (line.startsWith("bn:s"))
        {
    	  //System.out.println("1|" + line.replaceAll("^bn:s", ""));
    	  cui_from = cm.getIndexConcept(line.replaceAll("^bn:s", ""));
        }
        else if (line.startsWith("skos:related"))
        {
    	  //System.out.println("2|" + line.substring(13, line.length() - 1));
    	  for (String code : p_comma.split(line.substring(13, line.length() - 1)))
          {
    	    //System.out.println("3|" + code.trim().substring(4));

            Integer cui2 = cm.getIndexConcept(code.trim().substring(4));

            Map <Integer, Double> to = cm.getCM()[cui_from];

            if (to == null)
            {
              to = new HashMap <Integer, Double> ();
              cm.getCM()[cui_from] = to;
            }

            to.put(cui2, (to.get(cui2) == null ? 1.0 : to.get(cui2) + 1.0));
    	  }
        }
      
        count++;
        if (count % 10000 == 0) System.out.println(count);
      }

      brel.close();
    
      // Add the frequencies from corpora
      if (cooccurrences)
      {
        for (Map.Entry <String, Map <String, Integer>> entry : Cooccurrences.map_concept_concept.entrySet())
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
      
      Map <Integer, Map <Integer, Double>> tm_col = new HashMap <Integer, Map <Integer, Double>> ();

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

      dm.setPC(MarkovChain.converge(tm_col, cm.getConceptIndex()));
	} 
	catch (IOException e)
	{ throw new RuntimeException(e); }

    return dm;
  }

  public ProbabilityModel generateModel()
  {
	return generateModel(null, false);
  }

/*  public ProbabilityModel generateModel(ProbabilityModel dmMM, boolean cooccurrences)
  {
	try
	{ return generateModel(false); }
	catch (IOException e)
	{ throw new RuntimeException(e); }
  }*/
}
