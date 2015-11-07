package org.wkp.extraction.metamap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.wkp.model.ConceptMatrix;
import org.wkp.model.ProbabilityModel;
import org.wkp.model.TokenMatrix;
import org.wkp.utils.CommonWords;
import org.wkp.utils.Util;
import org.wkp.wsd.Disambiguation;

import monq.jfa.AbstractFaAction;
import monq.jfa.CallbackException;
import monq.jfa.CompileDfaException;
import monq.jfa.Dfa;
import monq.jfa.DfaRun;
import monq.jfa.Nfa;
import monq.jfa.ReSyntaxException;
import monq.jfa.ReaderCharSource;
import monq.jfa.Xml;

/**
 * Extract concept-concept and term-concept co-occurrences
 * 
 * @author Antonio Jimeno Yepes (antonio.jimeno@gmail.com)
 *
 */
public class MetaMapCooccurrences
{
  private static boolean in_mc = false;

  private static boolean filter_semantic_type = false;

  private static String matched_candidate = null;
  private static String cui_candidate = null;

  //private static Map <String, Map <String, Integer>> map_concept_term = new HashMap <String, Map <String, Integer>> (); 
  public static Map <String, Map <String, Integer>> map_term_concept = new HashMap <String, Map <String, Integer>> ();
  
  private static Map <String, Set <String>> map_phrase_term_concept = new HashMap <String, Set <String>> ();
  
  //private static Map <String, Map <String, Integer>> map_sentence_concept = new HashMap <String, Map <String, Integer>> ();
  
  public static Map <String, Map <String, Integer>> map_concept_concept = new HashMap <String, Map <String, Integer>> ();
  
  private static Set <String> sentence_concept = new HashSet <String> ();

  private static AbstractFaAction get_utterance_start = new AbstractFaAction()
  {
    public void invoke(StringBuffer yytext,int start,DfaRun runner)
  	throws CallbackException
	{

	}
  };

  private static AbstractFaAction get_utterance_end = new AbstractFaAction()
  {
    public void invoke(StringBuffer yytext,int start,DfaRun runner)
  	throws CallbackException
	{
      // Estimate co-occurrences at the sentence level
      //System.out.println("=======================");
      
      for (String cui1 : sentence_concept)
      {
        for (String cui2 : sentence_concept)
        {
          if (!cui1.equals(cui2))
          {
        	Map <String, Integer> map = map_concept_concept.get(cui1);
        	
        	if (map == null)
        	{
        	  map = new HashMap <String, Integer> ();
        	  map_concept_concept.put(cui1, map);
        	}
        	
        	if (map.get(cui2) == null)
        	{ map.put(cui2, 1); }
        	else
        	{ map.put(cui2, map.get(cui2) + 1); }
          }
        }
      }
      
      sentence_concept.clear();
    }
  };

  private static AbstractFaAction get_phrase_start = new AbstractFaAction()
  {
    public void invoke(StringBuffer yytext,int start,DfaRun runner)
  	throws CallbackException
	{

    }
  };

  public static Map <String, Map <Integer, Double>> profiles =
		  new HashMap <String, Map <Integer, Double>> (); 
  
  public static TokenMatrix tm = null;
  public static ConceptMatrix cm = null;
  public static double [] lambda_model = { 0.95, 0.025, 0.025 };
  
  private static Map <Integer, Double> doc = null;
  
  // Lazy generation of disambiguation profiles
  private static Map <Integer, Double> getProfile(String concept)
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

  private static String disambiguate(Map <Integer, Double> doc,
		                             Set <String> concepts)
  {
    double max = Double.NEGATIVE_INFINITY;
    String sense = "";

    for (String concept : concepts)
    {
      Map <Integer, Double> profile = getProfile(concept);
      double log_prob = Disambiguation.getLogProbability(profile, doc);

      if (log_prob > max)
      { max = log_prob; sense = concept; }
    }

    return sense;
  }

  private static AbstractFaAction get_phrase_end = new AbstractFaAction()
  {
    public void invoke(StringBuffer yytext,int start,DfaRun runner)
  	throws CallbackException
	{
      // Check for ambiguous words
      for (Map.Entry <String, Set <String>> term : map_phrase_term_concept.entrySet())
      {
    	if (term.getKey().length() > 1 && !CommonWords.checkWord(term.getKey()) && !Util.isNumber(term.getKey()))
    	{
    	  Map <String, Integer> concept_count = map_term_concept.get(term.getKey());

    	  if (concept_count == null)
    	  {
    	    concept_count = new HashMap <String, Integer> ();
    	    map_term_concept.put(term.getKey(), concept_count);
    	  }

    	  // Disambiguate -- no disambiguate now
    	  String concept = disambiguate(doc,
    			                      term.getValue());
    	
    	  // Populate map_term_concept
    	  if (concept_count.get(concept) == null)
    	  { concept_count.put(concept, 1); }
    	  else
    	  { concept_count.put(concept, concept_count.get(concept) + 1); }

    	  // Populate sentence_concept
          sentence_concept.add(concept);
    	}
      }

      map_phrase_term_concept.clear();
    }
  };

  private static AbstractFaAction get_mc_start = new AbstractFaAction()
  {
    public void invoke(StringBuffer yytext,int start,DfaRun runner)
  	throws CallbackException
	{
      in_mc = true;
	}
  };

  private static AbstractFaAction get_mc_end = new AbstractFaAction()
  {
    public void invoke(StringBuffer yytext,int start,DfaRun runner)
  	throws CallbackException
	{
        in_mc = false;
    }
  };

  private static AbstractFaAction get_candidate_cui = new AbstractFaAction()
  {
    public void invoke(StringBuffer yytext,int start,DfaRun runner)
  	throws CallbackException
	{
      if (in_mc)
      {
    	Map <String, String> map = Xml.splitElement(yytext, start);
        cui_candidate = map.get(Xml.CONTENT);
      }
	}
  };

  private static AbstractFaAction get_matched_candidate = new AbstractFaAction()
  {
    public void invoke(StringBuffer yytext,int start,DfaRun runner)
  	throws CallbackException
	{
      if (in_mc)
      {
        Map <String, String> map = Xml.splitElement(yytext, start);
        matched_candidate = map.get(Xml.CONTENT);
      }
	}
  };

  private static AbstractFaAction get_candidate_end = new AbstractFaAction()
  {
    public void invoke(StringBuffer yytext,int start,DfaRun runner)
  	throws CallbackException
	{
      if (in_mc)
      {
        if (!filter_semantic_type)
        {
          matched_candidate = matched_candidate.toLowerCase();
        
          // Enter term
          // Enter concept
          Set <String> concepts = map_phrase_term_concept.get(matched_candidate);
          
          if (concepts == null)
          {
        	concepts = new HashSet <String> ();
        	map_phrase_term_concept.put(matched_candidate, concepts);
          }

       	  concepts.add(cui_candidate);
        }

        filter_semantic_type = false;
      }
	}
  };

  private static AbstractFaAction get_semantic_type = new AbstractFaAction()
  {
    public void invoke(StringBuffer yytext,int start,DfaRun runner)
  	throws CallbackException
	{
      if (in_mc)
      {
        Map <String, String> map = Xml.splitElement(yytext, start);
        //System.out.println(map.get(Xml.CONTENT) + "|" + matched_candidate);

        if (map.get(Xml.CONTENT).equals("qlco") ||
            map.get(Xml.CONTENT).equals("qnco") ||
            map.get(Xml.CONTENT).equals("ftcn") ||
            map.get(Xml.CONTENT).equals("idcn")
            )
        	
        { filter_semantic_type = true; }
      }
	}
  };
 
  public static void getStatistics(String folder_name)
  throws ReSyntaxException
  {
    Nfa nfa = new Nfa(Nfa.NOTHING);
    nfa.or(Xml.STag("Utterance"), get_utterance_start);
    nfa.or(Xml.ETag("Utterance"), get_utterance_end);
	    
    nfa.or(Xml.STag("Phrase"), get_phrase_start);
    nfa.or(Xml.ETag("Phrase"), get_phrase_end);
    nfa.or(Xml.STag("MappingCandidates"), get_mc_start);
    nfa.or(Xml.ETag("MappingCandidates"), get_mc_end);
    nfa.or(Xml.ETag("Candidate"), get_candidate_end);
    nfa.or(Xml.GoofedElement("CandidateCUI"), get_candidate_cui);
    nfa.or(Xml.GoofedElement("CandidateMatched"), get_matched_candidate);
    nfa.or(Xml.GoofedElement("SemType"), get_semantic_type);

    File folder = new File(folder_name);

    int count = 1;
    
    if (folder.listFiles() != null)
    {
      for (File file : folder.listFiles())
      {
        if (file.getName().endsWith("metamap.gz"))
        {
    	  try
    	  {
   	        StringBuilder text = new StringBuilder();

            String line;
    		
            BufferedReader b = new BufferedReader(new FileReader(new File(folder, file.getName().replaceAll(".metamap.gz", ""))));

            while ((line = b.readLine()) != null)
            { text.append(" ").append(line); }
    		
            b.close();
            
            doc = Util.splitText(text.toString(), tm);
    		  
            System.out.println(count++ + "/" + (folder.listFiles().length/2));
            Dfa dfa = nfa.compile(DfaRun.UNMATCHED_COPY);
            DfaRun dfaRun = new DfaRun(dfa);
            dfaRun.setIn(new ReaderCharSource(new GZIPInputStream(new FileInputStream(file)), "UTF-8"));
            dfaRun.filter();
    	  }
    	  catch (Exception e)
    	  { e.printStackTrace(); System.err.println(file.getName()); }
        }
    	
    	//if (count==100) break;
      }
    }
  }

  public static void main (String [] argc)
  throws IOException, CompileDfaException, ReSyntaxException, ClassNotFoundException
  {
    ProbabilityModel dm = null;  

	{
      ObjectInputStream i = new ObjectInputStream(new GZIPInputStream(new FileInputStream(argc[0])));
      dm = (ProbabilityModel)i.readObject();
      i.close();
	}

	tm = dm.getTokenMatrix();
	cm = dm.getConceptMatrix();
	//cui_tm = dm.getPC();
		
    ObjectInputStream i = new ObjectInputStream(new GZIPInputStream(new FileInputStream(argc[1])));
    Disambiguation.p_w_c = (Map <Integer, Double>)i.readObject();
    i.close();

	getStatistics("C:\\datasets\\MSHCorpus\\PMID_text");

    System.out.println("*****************************************");

    System.out.println("Terms: " + map_term_concept.size());
    for (Map.Entry <String, Map <String, Integer>> entry : map_term_concept.entrySet())
    {
      //System.out.println(entry.getKey());

      for (Map.Entry <String, Integer> concept : entry.getValue().entrySet())
      { System.out.println("T|" + entry.getKey() + "|" + concept.getKey() + "|" + concept.getValue()); }
    }

    System.out.println("*****************************************");

    for (Map.Entry <String, Map <String, Integer>> cc : map_concept_concept.entrySet())
    {
      System.out.println(cc.getKey());

      for (Map.Entry <String, Integer> concept : cc.getValue().entrySet())
      {
    	System.out.println("C|" + cc.getKey() + "|" + concept.getKey() + "|" + concept.getValue());
      }
    }
  }
}