package org.wkp.retrieval;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.wkp.model.ConceptMatrix;
import org.wkp.model.ProbabilityModel;
import org.wkp.model.TokenMatrix;
import org.wkp.utils.Util;
import org.wkp.wsd.Disambiguation;

import monq.jfa.AbstractFaAction;
import monq.jfa.CallbackException;
import monq.jfa.Dfa;
import monq.jfa.DfaRun;
import monq.jfa.Nfa;
import monq.jfa.ReaderCharSource;
import monq.jfa.Xml;

/**
 * 
 * Evaluate the concept profiles ranking MEDLINE citations
 * 
 * @author Antonio Jimeno Yepes (antonio.jimeno@gmail.com)
 *
 */
public class RankCitations
{
  private static Dfa dfa = null;
  private static String PMID = null;

  private static StringBuilder text =
		  new StringBuilder();

  private static Map <String, Map <Integer, Double>> docs =
		  new HashMap <String, Map <Integer, Double>> ();
  
  private static Map <Integer, Double> p_w_b =
		  new HashMap <Integer, Double> ();
  
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
      Map <Integer, Double> doc = Util.splitTextCount(text.toString(), tm);
      docs.put(PMID, doc);

      for (Map.Entry <Integer, Double> t_c : doc.entrySet())
      {
    	if (p_w_b.get(t_c.getKey()) != null)
    	{ p_w_b.put(t_c.getKey(), p_w_b.get(t_c.getKey()) + t_c.getValue()); }
    	else
    	{ p_w_b.put(t_c.getKey(), t_c.getValue()); }
    	
    	token_count += t_c.getValue();
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

  private static Map <String, Map <Integer, Double>> profiles =
			  new HashMap <String, Map <Integer, Double>> ();

  //Lazy generation of disambiguation profiles
  private static Map <Integer, Double> getProfile(String concept, TokenMatrix tm, ConceptMatrix cm, double [] lambda_model)
  {
	Map <Integer, Double> profile = profiles.get(concept);

    if (profile != null)
    { return profile; }
    else
    {
      profile = Disambiguation.getProfile(concept, tm, cm, lambda_model);
      profiles.put(concept, profile);
      return profile;
    }
  }

  private static TokenMatrix tm = null;
  private static ConceptMatrix cm = null;
  private static double [] cui_tm = null;
  
  private static Map <Integer, Double> doc_back = null;

  public static void main (String [] argc)
  throws IOException, ClassNotFoundException
  {
    List <String> concepts = new LinkedList <String> ();

    BufferedReader b = new BufferedReader(new FileReader(argc[2]));

    String line;

    while ((line = b.readLine()) != null)
    {
      if (line.trim().length() > 0 && line.startsWith("C"))
      concepts.add(line.trim());
    }
    
    b.close();

	// Load data models
    ProbabilityModel dm = null;  

	{
      ObjectInputStream i = new ObjectInputStream(new GZIPInputStream(new FileInputStream(argc[0])));
      dm = (ProbabilityModel)i.readObject();
      i.close();
  	}

	tm = dm.getTokenMatrix();
	cm = dm.getConceptMatrix();
	cui_tm = dm.getPC();

	{
      ObjectInputStream i = new ObjectInputStream(new GZIPInputStream(new FileInputStream(argc[1])));
      Disambiguation.p_w_c = (Map <Integer, Double>)i.readObject();
      i.close();
	}

    //double [] lambda_model = { 0.6654269532365059, 0.06781272015123889, 0.26676032661225524 };

    //double [] lambda_model = { 0.97, 0.01, 0.02 };
	double [] lambda_model = { 0.8315, 0.0711, 0.0975 };
	//double [] lambda_model = { 1.0, 0.0, 0.0 };

    // Load citations and predict MeSH categories
    DfaRun dfaRun = new DfaRun(dfa);
    //dfaRun.setIn(new ReaderCharSource(System.in, "UTF-8"));
    dfaRun.setIn(new ReaderCharSource(new GZIPInputStream(new FileInputStream(argc[3])), "UTF-8"));
    dfaRun.filter();

    // Get the background probability
    for (Map.Entry <Integer, Double> back : p_w_b.entrySet())
    { p_w_b.put(back.getKey(), back.getValue()/(double)token_count); }

	// Load concept profiles
    for (String concept: concepts)
    {
      profiles.put(concept, getProfile(concept, tm, cm, lambda_model));

      // For each profile, print the probability
      for (Map.Entry <String, Map <Integer, Double>> profile : profiles.entrySet())
      //{ System.out.println(PMID + "|" + profile.getKey() + "|" + Disambiguation.getLogProbabilityRetrieval(profile.getValue(), doc)); }
      {
    	for (Map.Entry <String, Map <Integer, Double>> doc : docs.entrySet())
    	{
          boolean print = false;

    	  //if (doc.getKey().equals("23044227") || doc.getKey().equals("22981681"))
    	  {
    	    //print = true;
    	    //System.out.println(doc.getKey() + "|" + profile.getKey() + "|" + Disambiguation.getCrossEntropy(profile.getValue(), doc.getValue(), print, tm));
    	    System.out.println(doc.getKey() + "|" + profile.getKey() + "|" + Disambiguation.getCrossEntropy(profile.getValue(), doc.getValue(), p_w_b));
          }
    	}
      }

      profiles.clear();
      //break;
    }
  }
}