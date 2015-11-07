package org.wkp.model;

import gov.nih.nlm.nls.utils.Trie;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.zip.GZIPInputStream;

public class ConceptMatrix implements Serializable
{
  /**
	 * 
	 */
  private static final long serialVersionUID = 1L;

  private int concept_index = 0;

  private Map <Integer, String> index_concept =
			new HashMap <Integer, String> ();

  private Trie <Integer> trie_concept = new Trie <Integer> ();

  private Map [] cm = new Map [5000000];

  public int getConceptIndex()
  { return concept_index; }

  public String getIndexConcept(int concept_id)
  { return index_concept.get(concept_id); }

  public Integer getIndexConcept(String token)
  {
    Integer index = trie_concept.get(token);

    if (index == null)
    {
      index = concept_index;
      trie_concept.insert(token, concept_index);
	  concept_index++;

	  index_concept.put(index, token);
	}

	return index;
  }

  public Map [] getCM()
  { return cm; }

  public void traversePath(int concept_id,
		                   double probability,
		                   int k,
		                   Stack <Integer> path,
		                   TokenMatrix tm,
		                   Map <Integer, List <Double>> token_probs
		                   )
  {
	if (k > 0)
	{
      if (cm[concept_id] != null)
      {
        for (Map.Entry <Integer, Double> entry : ((Map <Integer, Double>)cm[concept_id]).entrySet() )
        {
          path.push(entry.getKey());
          traversePath(entry.getKey(), probability + entry.getValue(), k-1, path, tm, token_probs);
          path.pop();
        }
      }
    }
	else
	{
      // Collect term probabilities and multiply here
	  //System.out.println(path);
	  //System.out.println(probability);

	  if (tm.getTM().get(concept_id) != null)
	  {
	    for (Map.Entry <Integer, Double> tp :  tm.getTM().get(concept_id).entrySet())
	    {
		  List <Double> prob = token_probs.get(tp.getKey());

		  if (prob == null)
		  {
            prob = new ArrayList <Double> ();
            token_probs.put(tp.getKey(), prob);
          }

		  prob.add(probability + tp.getValue());
	    }
	  }
	}
  }
  
  private static double getMax (List <Double> values)
  {
	double result = Double.NEGATIVE_INFINITY;

	for (Double value : values)
	{
	  if (value > result)
	  { result = value; }
	}

	return result;
  }
  
  private static double sumVector(List <Double> values)
  {
	if (values.size() == 0)
	{ return Double.NEGATIVE_INFINITY; }

    // Get the maximum element
    double max = getMax(values);

    if (Double.isInfinite(max))
    { return Double.NEGATIVE_INFINITY; }

    double result_sum = 0;

    for (Double value : values)
    {
      double val = value - max;

      if (!Double.isInfinite(val))
      { result_sum += Math.exp(val); }
    }

    return max + Math.log(result_sum);
  }

  
  public Map <Integer, Double> sumProbs(Map <Integer, List <Double>> token_probs)
  {
    Map <Integer, Double> result =
    		new HashMap <Integer, Double> ();
    
    for (Map.Entry <Integer, List <Double>> tp : token_probs.entrySet())
    { result.put(tp.getKey(), sumVector(tp.getValue())); }
    
    return result;
  }
  
  /**
   * Traverse the graph
   * 
   * @param concept_id
   * @param k
   */
  public Map <Integer, Double> traversePath(int concept_id, int k, TokenMatrix tm)
  {
	Stack <Integer> path = new Stack <Integer> ();
	
	Map <Integer, List <Double>> token_probs =
			new HashMap <Integer, List <Double>> ();
	
	if (k > 0)
	{
      if (cm[concept_id] != null)
      {
        for (Map.Entry <Integer, Double> entry : ((Map <Integer, Double>)cm[concept_id]).entrySet() )
        {
          path.push(entry.getKey());
          traversePath(entry.getKey(), entry.getValue(), k-1, path, tm, token_probs);
          path.pop();
        }
      }
    }
	
	// Sum 
	return sumProbs(token_probs);
  }

  /**
   * Print probabilities to the standard output 
   */
  public void print()
  {
	for (int i = 0; i < cm.length; i++)
	{
	  if (cm[i] != null)
	  {
        for (Map.Entry <Integer, Double> entry : ((Map <Integer, Double>)cm[i]).entrySet())
        {
          System.out.println(
        	  	  "P(" +
        	  	  index_concept.get(entry.getKey()) +
        		  "|" +
        		  index_concept.get(i) +
        		  ")|" +
        		  index_concept.get(entry.getKey()) +
        		  "|" +
        		  index_concept.get(i) +
        		  "|" +
        		  entry.getValue() +
        		  "|" + 
        		  Math.exp(entry.getValue())
        		  );
        }  
      }
	}
  }
  
  public static void main (String [] argc)
  throws IOException, ClassNotFoundException
  {
    ProbabilityModel dm = null;  

    {
      ObjectInputStream i = new ObjectInputStream(new GZIPInputStream(new FileInputStream(argc[0])));
      dm = (ProbabilityModel)i.readObject();
      i.close();
    }
    
    dm.getConceptMatrix().print();
  }
}