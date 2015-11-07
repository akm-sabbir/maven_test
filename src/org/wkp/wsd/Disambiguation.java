package org.wkp.wsd;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.wkp.model.ConceptMatrix;
import org.wkp.model.TokenMatrix;
import org.wkp.utils.Util;


public class Disambiguation
{
  public static Map <Integer, Double> p_w_c = null;
  
  private static double lambda = 0.75;
  
  private static double lambda_log = Math.log(1 - lambda);
  private static double lambda_background_log = Math.log(lambda);
  
  private static double [] lambda_model = { 0.8130014075331744, 0.06488123958756446, 0.12211735287926112 };
  
  private static double ir_lambda = 0.25;
  
  private static double getProbability(Double prob, Double background)
  {
	if (prob != null)
	{ return Math.log(Math.exp(lambda_log + prob) + Math.exp(lambda_background_log + background)); }
	else
	{ return (lambda_background_log + background); }
  }
  
  public static double getLogProbability(Map <Integer, Double> profile, Map <Integer, Double> example)
  {
    double log_prob = 0.0;

    boolean match = false;

    for (Map.Entry <Integer, Double> entry : example.entrySet())
    {
      if (p_w_c.get(entry.getKey()) != null)
      {
    	log_prob += getProbability((profile == null ? null : profile.get(entry.getKey())), p_w_c.get(entry.getKey()));
        match = true;
      }
    }

    return (match ? log_prob : Double.NEGATIVE_INFINITY); 
  }
  
  public static double getLogProbabilityRetrieval(Map <Integer, Double> profile, Map <Integer, Double> example)
  {
    double log_prob = 0.0;

    boolean match = false;

    for (Map.Entry <Integer, Double> entry : profile.entrySet())
    {
      if (p_w_c.get(entry.getKey()) != null)
      {
    	if (example.get(entry.getKey()) != null)
    	{ log_prob += getProbability(profile.get(entry.getKey()), p_w_c.get(entry.getKey())); }
    	else
    	{ log_prob += p_w_c.get(entry.getKey()); }

        match = true;
      }
    }

    return (match ? log_prob : Double.NEGATIVE_INFINITY); 
  }

  public static double getCrossEntropy(Map <Integer, Double> profile, Map <Integer, Double> example, Map <Integer, Double> p_w_b) //, boolean print, TokenMatrix tm)
  {
    double cr = 0;

    double sum_freq_doc = 0;

    for (Double freq : example.values())
    { sum_freq_doc += freq; }

    for (Map.Entry <Integer, Double> entry : profile.entrySet())
    {
      if (p_w_b.get(entry.getKey()) != null)
      {
    	double d_count = (example.get(entry.getKey()) == null ? 0 : example.get(entry.getKey()));

        double query_prob = Math.exp(getProbability(profile.get(entry.getKey()), p_w_c.get(entry.getKey())));
        double doc_prob = Math.log((((1-ir_lambda)*d_count)/sum_freq_doc) + (ir_lambda * p_w_b.get(entry.getKey())));

        cr += query_prob * doc_prob;

        /*if (print)
        {
          System.out.println(entry.getKey() + "|" + tm.getIndexToken().get(entry.getKey()) + "|" + example.get(entry.getKey()) + "|" + sum_freq_doc + "|" + (example.get(entry.getKey())/sum_freq_doc) + "|" + getProbability(profile.get(entry.getKey()), p_w_c.get(entry.getKey())) + "|" + Math.exp(getProbability(profile.get(entry.getKey()), p_w_c.get(entry.getKey()))));
        }*/
      }
    }

    return cr;
  }

  public static double getCrossEntropyBerlanga(Map <Integer, Double> profile, Map <Integer, Double> example) //, boolean print, TokenMatrix tm)
  {
    double cr = 0;

    double sum_freq_doc = 0;

    for (Double freq : example.values())
    { sum_freq_doc += freq; }

    for (Map.Entry <Integer, Double> entry : profile.entrySet())
    {
      if (example.get(entry.getKey()) != null)
      {
        double query_prob = profile.get(entry.getKey());
        double doc_prob = Math.log(example.get(entry.getKey())/sum_freq_doc);

        cr += query_prob * doc_prob;
      }
    }

    return cr;
  }

  public static double getCount(Map <Integer, Double> profile, Map <Integer, Double> example)
  {
    int count = 0;

    for (Map.Entry <Integer, Double> entry : example.entrySet())
    {
      if (profile != null && profile.get(entry.getKey()) != null)
      {	count++; }
    }

    return count; 
  }

  private static Map <Integer, Double> combine (Map <Integer, Double> set1,
		                                        Map <Integer, Double> set2,
		                                        Map <Integer, Double> set3,
		                                        double [] lambda_model)
  {
	Map <Integer, Double> result = new HashMap <Integer, Double> ();

	Set <Integer> keys = new HashSet <Integer>  ();
	
	if (set1 != null)
	{ keys.addAll(set1.keySet()); }
	if (set2 != null)
	{ keys.addAll(set2.keySet()); }
	if (set3 != null)
	{ keys.addAll(set3.keySet()); }

	for (Integer key :  keys)
	{
      double v1 = (set1 == null || set1.get(key) == null ? 0 : Math.exp(set1.get(key)));
      double v2 = (set2 == null || set2.get(key) == null ? 0 : Math.exp(set2.get(key)));
      double v3 = (set3 == null || set3.get(key) == null ? 0 : Math.exp(set3.get(key)));

      double weight = Math.log((v1*lambda_model[0])+(v2*lambda_model[1]))+((v3*lambda_model[2]));
      
      if (!Double.isInfinite(weight))
      { result.put(key, weight); }
	}
	
	return result;
  }

  public static void main (String [] argc)
  throws IOException, ClassNotFoundException
  {
	TokenMatrix tm = null;
/*
	{
      ObjectInputStream i = new ObjectInputStream(new GZIPInputStream(new FileInputStream(argc[0])));
      tm = (TokenMatrix)i.readObject();
      i.close();
	}

	ConceptMatrix cm = null;

	{
      ObjectInputStream i = new ObjectInputStream(new GZIPInputStream(new FileInputStream(argc[1])));
      cm = (ConceptMatrix)i.readObject();
      i.close();
	}

	{
      ObjectInputStream i = new ObjectInputStream(new GZIPInputStream(new FileInputStream(argc[2])));
      p_w_c = (Map <Integer, Double>)i.readObject();
      i.close();
	}

	double [] cui_tm = null;

	{
      ObjectInputStream i = new ObjectInputStream(new GZIPInputStream(new FileInputStream(argc[3])));
      cui_tm = (double[])i.readObject();
      i.close();
	}

	Map <String, List <String>> benchmark = Benchmark.load(argc[4]);

	disambiguate(tm, cm, cui_tm, benchmark, lambda_model, argc[5]);*/
  }

  public static Map <Integer, Double> getProfile(String concept,
		                                         TokenMatrix tm,
                                                 ConceptMatrix cm,
                                                 double [] lambda_model
                                                 )
  {
	return combine(getProfile(concept, 0, tm, cm), // tm.getTM().get(cm.getIndexConcept(concept)),
			       getProfile(concept, 1, tm, cm), // cm.traversePath(cm.getIndexConcept(concept), 1, tm),
			       getProfile(concept, 2, tm, cm), // cm.traversePath(cm.getIndexConcept(concept), 2, tm),
	    		   lambda_model
	    		   );
  }
  
  // Disambiguate and return the log-likelihood
  public static double disambiguate(TokenMatrix tm,
		                            ConceptMatrix cm,
		                            double [] cui_tm,
		                            Map <String, List <String>> benchmark,
		                            double [] lambda_model,
		                            String folder
		                            )
  throws IOException
  {
    double log_likelihood = 0;
    
    double sum_acc = 0;
    int count = 0;
	  
	for (Map.Entry <String, List <String>> terms : benchmark.entrySet())
	{
	  //if (!terms.getKey().equals("Fish")) { continue;}
	  // Look at just one concept
	  //if (!terms.getKey().equals("Borrelia")) { continue;}

	  String term = terms.getKey();

  	  // Load concept profiles: cui, word, probability
	  Map <String, Map <Integer, Double>> profiles = new HashMap <String, Map <Integer, Double>> ();

	  for (String concept : terms.getValue())
	  {
		// Combine the term model of the concept
		profiles.put(concept,
				     combine(getProfile(concept, 0, tm, cm), //tm.getTM().get(cm.getIndexConcept(concept)),
				    		 getProfile(concept, 1, tm, cm), //cm.traversePath(cm.getIndexConcept(concept), 1, tm),
				    		 getProfile(concept, 2, tm, cm), //cm.traversePath(cm.getIndexConcept(concept), 2, tm),
				    		 lambda_model
				    		 )
		);
	  }

	  String file_name = (new File(folder, term + "_pmids_tagged.arff")).getAbsolutePath();

	  int correct = 0;
	  int total = 0;

	  for (Document doc : ARFF.load(file_name, tm))
      {
        double max = Double.NEGATIVE_INFINITY;
        String sense = "";

        for (Map.Entry <String, Map <Integer, Double>> entry : profiles.entrySet())
        {
          double log_prob = getLogProbability(entry.getValue(), doc.getTokenCount()) + cui_tm[cm.getIndexConcept(entry.getKey())];

          if (log_prob > max)
          { max = log_prob; sense = entry.getKey(); }
        }

        String final_sense = "M" + (benchmark.get(term).indexOf(sense) + 1);

        if (final_sense.equals(doc.getSense()))
        { correct++; }

        total++;
        System.out.println(max + "|" + doc.getSense() + "|" + final_sense);
        // Add the selected probability
        log_likelihood += max;
      }

	  System.out.println("Accuracy|" + term + "|" + ((double)correct / total));
	  
	  count++;
	  sum_acc += ((double)correct / total);
	}
	
	System.out.println("Accuracy: " + (sum_acc / count));
	System.out.println("Count: " + count);

	return log_likelihood;
  }

  private static double getMax (List <Double> values)
  {
	double result = Double.NEGATIVE_INFINITY;

	for (Double val : values)
	{
	  if (val > result)
	  { result = val; }
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

    for (Double v : values)
    {
      double val = v - max;

      if (!Double.isInfinite(val))
      { result_sum += Math.exp(val); }
    }

    return max + Math.log(result_sum);
  }
  
  private static Map <Integer, Map <Integer, Map <Integer, Double>>> cps =
		  new HashMap <Integer, Map <Integer, Map <Integer, Double>>> ();
  
  public static void clearCPS()
  { cps.clear(); }
  
  public static boolean in_cps = false;
  
  public static Map <Integer, Double> getProfile(String concept,
		                                  int step,
		                                  TokenMatrix tm,
		                                  ConceptMatrix cm)
  {
	if (in_cps)
	{
	  int c_id = cm.getIndexConcept(concept);
	
	  Map <Integer, Map <Integer, Double>> steps = 
			cps.get(c_id);
			
	  if (steps == null)
	  {
        steps = new HashMap <Integer, Map <Integer, Double>> ();
        cps.put(c_id, steps);
	  }

	  Map <Integer, Double> profile = steps.get(step);
	  
	  if (profile == null)
	  {
	    if (step == 0)
	    { profile = tm.getTM().get(cm.getIndexConcept(concept)); }
	    else
	    { profile = cm.traversePath(cm.getIndexConcept(concept), step, tm); }

	    steps.put(step, profile);
	  
	    return profile;
	  }
	  else
	  { return profile; } 
	}
	else
	{
      if (step == 0)
	  { return tm.getTM().get(cm.getIndexConcept(concept)); }
	  else
	  { return cm.traversePath(cm.getIndexConcept(concept), step, tm); }
	}
  }
  
  // Disambiguate and return the log-likelihood
  public static double [] estimate_lambda(TokenMatrix tm,
		                                  ConceptMatrix cm,
		                                  double [] cui_tm,
		                                  Map <String, List <String>> benchmark,
		                                  double [] lambda_model,
		                                  String folder
		                                  )
  throws IOException
  {
	double [] new_lambda = new double [lambda_model.length];

    List <Double> val1 = new ArrayList <Double> ();
    List <Double> val2 = new ArrayList <Double> ();
    List <Double> val3 = new ArrayList <Double> ();
    
    int count1 = 0;
    int count2 = 0;
    int count3 = 0;
	
	for (Map.Entry <String, List <String>> terms : benchmark.entrySet())
	{
	  // Look at just one concept
	  //if (!terms.getKey().equals("Fish")) { continue;}

	  String term = terms.getKey();

  	  // Load concept profiles: cui, word, probability
	  Map <String, Map <Integer, Double>> profiles = new HashMap <String, Map <Integer, Double>> ();

	  for (String concept : terms.getValue())
	  {
		// Combine the term model of the concept
		profiles.put(concept,
					 combine(getProfile(concept, 0, tm, cm), // tm.getTM().get(cm.getIndexConcept(concept)),
				    		 getProfile(concept, 1, tm, cm), // cm.traversePath(cm.getIndexConcept(concept), 1, tm),
				    		 getProfile(concept, 2, tm, cm), // cm.traversePath(cm.getIndexConcept(concept), 2, tm),
				    		 lambda_model
				    		 )
		);
	  }

	  String file_name = (new File(folder, term + "_pmids_tagged.arff")).getAbsolutePath();

	  int correct = 0;
	  int total = 0;

	  for (Document doc : ARFF.load(file_name, tm))
      {
        double max = Double.NEGATIVE_INFINITY;
        String sense = "";

        for (Map.Entry <String, Map <Integer, Double>> entry : profiles.entrySet())
        {
          double log_prob = getLogProbability(entry.getValue(), doc.getTokenCount()) + cui_tm[cm.getIndexConcept(entry.getKey())];

          if (log_prob > max)
          { max = log_prob; sense = entry.getKey(); }
        }

        String final_sense = "M" + (benchmark.get(term).indexOf(sense) + 1);

        if (final_sense.equals(doc.getSense()))
        { correct++; }

        // Model 1
        Map<Integer, Double> p1 = getProfile(sense, 0, tm, cm); //tm.getTM().get(cm.getIndexConcept(sense));
        // Model 2
        Map <Integer, Double> p2 = getProfile(sense, 1, tm, cm); //cm.traversePath(cm.getIndexConcept(sense), 1, tm);
        // Model 3
        Map <Integer, Double> p3 = getProfile(sense, 2, tm, cm); //cm.traversePath(cm.getIndexConcept(sense), 2, tm);

        val1.add(getLogProbability(p1, doc.getTokenCount()));
        val2.add(getLogProbability(p2, doc.getTokenCount()));
        val3.add(getLogProbability(p3, doc.getTokenCount()));
        
        count1 += getCount(p1, doc.getTokenCount());
        count2 += getCount(p2, doc.getTokenCount());
        count3 += getCount(p3, doc.getTokenCount());

        total++;
      }

	  System.out.println("Accuracy|" + term + "|" + ((double)correct / total));
	}

	double sum1 = Math.exp(sumVector(val1));
	double sum2 = Math.exp(sumVector(val2));
	double sum3 = Math.exp(sumVector(val3));
	
	int sum_count = count1 + count2 + count3;
	
	new_lambda[0] = sum1*lambda_model[0] + 0.3*(count1/(double)sum_count);
	new_lambda[1] = sum2*lambda_model[1] + 0.3*(count2/(double)sum_count);
	new_lambda[2] = sum3*lambda_model[2] + 0.3*(count3/(double)sum_count);
	
	System.out.println("sums|" + sum1 + "|" + sum2 + "|" + sum3);
	System.out.println("counts|" + count1 + "|" + count2 + "|" + count3);

	double sum = 0;

	for (double l : new_lambda)
	{ sum+=l; }
	
    for (int i = 0; i < new_lambda.length; i ++)
    { new_lambda[i] /= sum; }

	return new_lambda;
  }
  
  // Disambiguate and return the log-likelihood
  public static void print_term_prob(TokenMatrix tm,
		                             ConceptMatrix cm,
		                             double [] cui_tm,
		                             Map <String, List <String>> benchmark,
		                             double [] lambda_model
		                            )
  throws IOException
  {
	for (Map.Entry <String, List <String>> terms : benchmark.entrySet())
	{
	  for (String concept : terms.getValue())
	  {
		int i = 0;
		  
		// Combine the term model of the concept
		for (Map.Entry <Integer, Double> entry : Util.mapIntegerDoubleSortDesc(combine(tm.getTM().get(cm.getIndexConcept(concept)),
				    		                                            cm.traversePath(cm.getIndexConcept(concept), 1, tm),
				    		                                            cm.traversePath(cm.getIndexConcept(concept), 2, tm),
				    		                                            lambda_model
				    		                                           )))
		{
		  if (!entry.getValue().isInfinite())
		  {
		    System.out.println(terms.getKey() + "|"
		                     + concept + "|"
                             + entry.getKey() + "|"
                             + tm.getIndexToken().get(entry.getKey()) + "|"
                             + entry.getValue() + "|"
                             + Math.exp(entry.getValue()));
		  }
		  
		  i++;
		  
		  if (i==100) break;
		}
	  }
	}
  }
}
