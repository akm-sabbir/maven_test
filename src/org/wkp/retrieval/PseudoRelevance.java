package org.wkp.retrieval;

import gov.nih.nlm.nls.utils.Trie;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.wkp.utils.Util;

import monq.jfa.AbstractFaAction;
import monq.jfa.CallbackException;
import monq.jfa.Dfa;
import monq.jfa.DfaRun;
import monq.jfa.Nfa;
import monq.jfa.ReaderCharSource;
import monq.jfa.Xml;

/**
 * 
 * @inproceedings{tao2006regularized,
  title={Regularized estimation of mixture models for robust pseudo-relevance feedback},
  author={Tao, Tao and Zhai, ChengXiang},
  booktitle={Proceedings of the 29th annual international ACM SIGIR conference on Research and development in information retrieval},
  pages={162--169},
  year={2006},
  organization={ACM}
}
 * 
 * @author Antonio Jimeno Yepes (antonio.jimeno@gmail.com)
 *
 */
public class PseudoRelevance
{
  private static Map <Integer, Double> p_w_b =
		  new HashMap <Integer, Double> ();
	
  private static Dfa dfa = null;
  private static String PMID = null;

  private static Trie <Integer> trie = new Trie <Integer> ();
  
  private static int nu = 2000;

  private static StringBuilder text =
			  new StringBuilder();

  private static Map <String, Map <Integer, Double>> docs =
			  new HashMap <String, Map <Integer, Double>> ();
  
  private static Map <String, Map <Integer, Double>> docs_freq =
		  new HashMap <String, Map <Integer, Double>> ();
  
  private static Map <String, Double> doc_length =
		  new HashMap <String, Double> ();
  
  private static int token_count = 0;

  private static AbstractFaAction get_text = new AbstractFaAction()
  {
    public void invoke(StringBuffer yytext,int start,DfaRun runner)
  	throws CallbackException
	{
      Map <String, String> map = Xml.splitElement(yytext, start);
      text.append(" ").append(map.get(Xml.CONTENT));
	}
  };

  private static AbstractFaAction get_PMID = new AbstractFaAction()
  {
    public void invoke(StringBuffer yytext,int start,DfaRun runner)
  	throws CallbackException
	{
	  if (PMID == null)
	  {
		Map <String, String> map = Xml.splitElement(yytext, start);
	    PMID = map.get(Xml.CONTENT);
	  }
	}
  };

  private static AbstractFaAction end_document = new AbstractFaAction()
  {
    public void invoke(StringBuffer yytext,int start,DfaRun runner)
  	throws CallbackException
	{
      // Convert the text to a document profile
      Map <Integer, Double> doc = Util.splitTextCount(text.toString(), trie);
      docs.put(PMID, doc);
      
      Map <Integer, Double> doc_freq = new HashMap <Integer, Double> ();
      doc_freq.putAll(doc);
      docs_freq.put(PMID, doc_freq);
      
      // Add frequencies to the background probability
      for (Map.Entry <Integer, Double> entry : doc.entrySet())
      {
        Double count = p_w_b.get(entry.getKey());

        if (count == null)
        { p_w_b.put(entry.getKey(), entry.getValue()); }
        else
        { p_w_b.put(entry.getKey(), count + entry.getValue()); }

        token_count += entry.getValue();
      }

      PMID = null;
      text.setLength(0);
	}
  };

  static
  {
	try
	{
      Nfa nfa = new Nfa(Nfa.NOTHING);
      nfa.or(Xml.GoofedElement("AbstractText"), get_text);
      nfa.or(Xml.GoofedElement("ArticleTitle"), get_text);
      nfa.or(Xml.GoofedElement("PMID"), get_PMID);
      nfa.or(Xml.ETag("MedlineCitation"), end_document);
      dfa = nfa.compile(DfaRun.UNMATCHED_COPY);
	}
	catch (Exception e)
	{
      e.printStackTrace();
      System.exit(-1);
	}
  }

  private static double getKLDivergence (Map <Integer, Double> p_q, Map <Integer, Double> p_d, String pmid)
  {
    double kl = 0;

    for (Map.Entry <Integer, Double> entry : p_q.entrySet())
    {
      if (entry.getValue() > 1E-150 && p_w_b.get(entry.getKey()) != null)
      {
        double p_w_doc = (p_d.get(entry.getKey()) != null ? p_d.get(entry.getKey()) : (nu * p_w_b.get(entry.getKey()))/(doc_length.get(pmid) + nu));
        kl += entry.getValue() * Math.log(p_w_doc/entry.getValue());
      }
    }

    return kl;
  }

  private static Map <Integer, Double> get_p_t(Map <Integer, Double> p_q, List <String> top_d)
  {
	double nu = 100000;

    double discount = 0.99;

    double r = 0;

	Map <Integer, Double> p_t =
			new HashMap <Integer, Double> ();

	// Initialize p_q
	//p_t.putAll(p_q);
	// Get all the candidate term ids from the query and the documents
	Set <Integer> init_terms = new HashSet <Integer> ();

	init_terms.addAll(p_q.keySet());

	// Set random values
	for (String doc : top_d)
	{ init_terms.addAll(docs.get(doc).keySet()); }

	// Normalize
	for (Integer t : init_terms)
	{ p_t.put(t, 1.0/init_terms.size()); }

	int count = 0;
	
	double [] alpha_d = new double [top_d.size()];
	
	for (int i = 0; i < alpha_d.length; i++)
	{ alpha_d[i] = 0.1; }
	
    //while (count < 10)
    do
    {
      r = 0;
 
      nu = nu * discount;
      Map <Integer, Double> [] p_z = new Map [top_d.size()];
      
  	  for (int i = 0; i < alpha_d.length; i++)
  	  { p_z[i] = new HashMap <Integer, Double> (); }
    	
      // E-step
      for (Map.Entry <Integer, Double> entry : p_t.entrySet())
      {
    	for (int i = 0; i < alpha_d.length; i++)
    	{
    	  double background = (p_w_b.get(entry.getKey()) != null ? p_w_b.get(entry.getKey()) : 0);

    	  double value = (alpha_d[i] * entry.getValue())/(alpha_d[i]*entry.getValue() + ((1-alpha_d[i])* background) );
    	  p_z[i].put(entry.getKey(), value);
    	}
      }

      // M-step
      p_t.clear();

      // alpha_d estimation
      for (int i = 0; i < alpha_d.length; i++)
      {
        double sum = 0.0;

        Map <Integer, Double> doc_freq = docs_freq.get(top_d.get(i));

        for (Map.Entry <Integer, Double> entry : p_z[i].entrySet())
        {
          if (doc_freq.get(entry.getKey()) != null)
          { sum += entry.getValue() * doc_freq.get(entry.getKey()); }
        }

        alpha_d[i] = sum/doc_length.get(top_d.get(i));
      }

      System.out.println(Arrays.toString(alpha_d));

      // p_t estimation
      double denominator = nu;

      for (int i = 0; i < alpha_d.length; i++)
      {
        Map <Integer, Double> doc_freq = docs_freq.get(top_d.get(i));

        for (Map.Entry <Integer, Double> entry : p_z[i].entrySet())
        {
          if (doc_freq.get(entry.getKey()) != null)
          { denominator += entry.getValue() * doc_freq.get(entry.getKey()); }
        }
      }

      r = denominator - nu;

      // Enter the prior from p_q
      for (Map.Entry <Integer, Double> entry : p_q.entrySet())
      { p_t.put(entry.getKey(), nu * entry.getValue()); }

      // Add counting from documents
      for (int i = 0; i < alpha_d.length; i++)
      {
        Map <Integer, Double> doc_freq = docs_freq.get(top_d.get(i));

        for (Map.Entry <Integer, Double> entry : p_z[i].entrySet())
        {
          if (doc_freq.get(entry.getKey()) != null)
          {
       	    double value = entry.getValue() * doc_freq.get(entry.getKey());
       	    
       	    if (p_t.get(entry.getKey()) != null)
       	    { p_t.put(entry.getKey(), p_t.get(entry.getKey()) + value); }
       	    else
       	    { p_t.put(entry.getKey(), value); }
          }
        }
      }
      
      // Divide by the denominator
      for (Map.Entry <Integer, Double> entry : p_t.entrySet())
      { p_t.put(entry.getKey(), entry.getValue() / denominator); }

      count++;

      System.out.println("r:" + r);
      System.out.println("nu:" + nu);
    }
    //while (r < nu);
    while (r < nu && count < 1000);

    return p_t;
  }

  public static void main (String [] argc)
  throws IOException, ClassNotFoundException
  {
	if (argc.length != 2)
	{
	  System.err.println("PseudoRelevance [XML file] [query_file]");
      System.exit(-1);
	}

    // Load citations 
    DfaRun dfaRun = new DfaRun(dfa);
    //dfaRun.setIn(new ReaderCharSource(System.in, "UTF-8"));
    //dfaRun.setIn(new ReaderCharSource(new GZIPInputStream(new FileInputStream("c:\\datasets\\amia_set\\citations.test.xml.gz")), "UTF-8"));
    dfaRun.setIn(new ReaderCharSource(new GZIPInputStream(new FileInputStream(argc[0])), "UTF-8"));
    dfaRun.filter();

    // Estimate the background probability
    for (Map.Entry <Integer, Double> entry : p_w_b.entrySet())
    { p_w_b.put(entry.getKey(), entry.getValue()/(double)token_count); }
    
    // Estimate the word_document probability based on a Dirichlet Model
    for (Map.Entry <String, Map <Integer, Double>> entry : docs.entrySet())
    {
      Map <Integer, Double> doc = entry.getValue();
      
      int d_count = 0;
      for (Double v : doc.values()) { d_count += v; }
      
      doc_length.put(entry.getKey(), (double)d_count);

      for (Map.Entry <Integer, Double> t_c : doc.entrySet())
      {
    	double prob = (t_c.getValue() + (nu * p_w_b.get(t_c.getKey())))/(double)(d_count + nu);
    	doc.put(t_c.getKey(), prob);
      }
    }

    // Load queries
    LinkedList <String> queries = new LinkedList <String> ();
    LinkedList <String> cuis = new LinkedList <String> ();

    String line;
    
    //BufferedReader b = new BufferedReader(new FileReader("c:\\datasets\\amia_set\\cui_mesh_terms.txt"));
    BufferedReader b = new BufferedReader(new FileReader(argc[1]));
    
    while ((line = b.readLine()) != null)
    {
      cuis.add(line.split("\\|")[0]);
      queries.add(line.split("\\|")[1]);
    }
    
    b.close();    

    int count = 0;
    
    // Run retrieval
    for (String query : queries)
    {
      Map <Integer, Double> p_q = Util.splitTextCount(query, trie);
      
      Map <String, Double> doc_score =
    		  new HashMap <String, Double> ();

      double qsum = 0;
      for (Double val : p_q.values()) { qsum += val;}

      for (Map.Entry <Integer, Double> entry : p_q.entrySet())
      {	p_q.put(entry.getKey(), entry.getValue()/qsum); }

      for (Map.Entry<String, Map <Integer, Double>> doc : docs.entrySet())
      {
    	double kl = getKLDivergence(p_q, doc.getValue(), doc.getKey());
    	
    	if (!Double.isInfinite(kl))
    	{
    	  //System.out.println(doc.getKey() + "|" + cuis.get(count)  + "|" + kl);
    	  doc_score.put(doc.getKey(), kl);
    	}
      }

      List <String> top_docs = new ArrayList <String> ();

      // Rank
      List <Map.Entry <String, Double>> doc_sorted = Util.mapValueDoubleSortDesc(doc_score);
      
      // Get the top 10
      for (int i = 0; i < 10; i++)
      { top_docs.add(doc_sorted.get(i).getKey()); }

      for (Map.Entry <Integer, Double> entry : p_q.entrySet())
      { System.out.println("q:" + entry.getKey() + "|" + entry.getValue()); }

      Map <Integer, Double> p_q_t = get_p_t(p_q, top_docs);
      
      for (Map.Entry <Integer, Double> entry : p_q_t.entrySet())
      { System.out.println("q_t:" + entry.getKey() + "|" + entry.getValue()); }
      
      // Retrieval with the new model
      for (Map.Entry<String, Map <Integer, Double>> doc : docs.entrySet())
      {
    	double kl = getKLDivergence(p_q_t, doc.getValue(), doc.getKey());

    	if (!Double.isInfinite(kl))
    	{ System.out.println(doc.getKey() + "|" + cuis.get(count)  + "|" + kl); }
      }

      count++;
      
//      break;
    }
  }
}