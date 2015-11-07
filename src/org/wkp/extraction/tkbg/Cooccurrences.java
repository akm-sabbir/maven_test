package org.wkp.extraction.tkbg;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import monq.jfa.AbstractFaAction;
import monq.jfa.CallbackException;
import monq.jfa.Dfa;
import monq.jfa.DfaRun;
import monq.jfa.Nfa;
import monq.jfa.ReaderCharSource;
import monq.jfa.Xml;

import org.wkp.model.ConceptMatrix;
import org.wkp.model.TokenMatrix;
import org.wkp.utils.CommonWords;
import org.wkp.utils.Util;
import org.wkp.wsd.Disambiguation;

public class Cooccurrences
{
  private static Dfa dfa = null;

  public static void getStatistics(String folder_name) throws IOException
  {
	// Traverse the files in the folder
	File folder = new File (folder_name);
	
	if (folder.isDirectory() && folder.listFiles() != null)
	{
      for (File file : folder.listFiles())
      {
    	if (file.getName().endsWith(".ann"))
    	{
   	      DfaRun dfaRun = new DfaRun(dfa);
    	  dfaRun.setIn(new ReaderCharSource(new FileInputStream(file), "UTF-8"));
    	  dfaRun.filter();
    	}
      }
	}
  }

  private class Entity
  {
	private int start;
	private int end;
	private String string;

	public Entity (int start, int end, String string)
	{
      this.start = start;
      this.end = end;
      this.string = string;
	}

	public boolean equals(Object obj)
	{
	  Entity e = (Entity)obj;

	  if (this.start == e.start && this.end == e.end)
	  { return true; }
	  
	  return false;
	}
	
	// To check nested entities
	public boolean inEntity(Entity e)
	{
	  if (this.start >= e.start && this.end <= e.end)
	  { return true; }
	  
	  return false;
	}
	
	public int hashCode()
	{ return start + end + string.hashCode(); }
	
	public String getString()
	{ return string; }
	
	public String toString()
	{
	  return string + "|" + start + "|" + end;
	}
  }
  
  public static Map <String, Map <Integer, Double>> profiles =
		  new HashMap <String, Map <Integer, Double>> (); 

  public static TokenMatrix tm = null;
  public static ConceptMatrix cm = null;
  public static double [] lambda_model = { 1.0/3.0, 1.0/3.0, 1.0/3.0 };
  
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

  public static Map <String, Map <String, Integer>> map_term_concept = new HashMap <String, Map <String, Integer>> ();

  public static Map <String, Map <String, Integer>> map_concept_concept = new HashMap <String, Map <String, Integer>> ();

  public static Map <Entity, Set <String>> entity_concepts = new HashMap <Entity, Set <String>> (); 

  private static AbstractFaAction get_e = new AbstractFaAction()
  {
    public void invoke(StringBuffer yytext,int start,DfaRun runner)
  	throws CallbackException
	{
      Map <String, String> map = Xml.splitElement(yytext, start);
      //System.out.println(map.get(Xml.CONTENT) + "|" + map.get("cui"));

      int offset = Integer.parseInt(map.get("offset"));
      int len = Integer.parseInt(map.get("len"));

      Entity e = new Cooccurrences().new Entity (offset,  offset + len , map.get(Xml.CONTENT));

      Set <String> concepts = entity_concepts.get(e);

      if (concepts == null)
      {
    	concepts = new HashSet <String> ();
    	entity_concepts.put(e, concepts);
      }

      concepts.add(map.get("cui"));
	}
  };

  private static AbstractFaAction get_s_end = new AbstractFaAction()
  {
    public void invoke(StringBuffer yytext,int start,DfaRun runner)
  	throws CallbackException
	{
      Set <String> sentence_concepts = new HashSet <String> ();
      
      // Select entities with the largest span match
      Set <Entity> nested_set = new HashSet <Entity> ();
      
      for (Map.Entry <Entity, Set <String>> entry1 : entity_concepts.entrySet())
      {
        for (Map.Entry <Entity, Set <String>> entry2 : entity_concepts.entrySet())
        {
          if (entry1.getKey() != entry2.getKey() && entry1.getKey().inEntity(entry2.getKey()))
          { nested_set.add(entry1.getKey()); break; }
        }          	
      }
      
      // Remove the selected entities 
      for (Entity e : nested_set)
      { entity_concepts.remove(e); }

      // Add entities
      for (Map.Entry <Entity, Set <String>> entry : entity_concepts.entrySet())
      {
        System.out.println(entry.getKey());

        String term = entry.getKey().getString();

        // Add term-concept counts
      	if (term.length() > 1 && !CommonWords.checkWord(term) && !Util.isNumber(term))
      	{
      	  Map <String, Integer> concept_count = map_term_concept.get(term);

      	  if (concept_count == null)
      	  {
      	    concept_count = new HashMap <String, Integer> ();
      	    map_term_concept.put(term, concept_count);
      	  }

      	  // Disambiguate -- no disambiguate now
      	  String concept = disambiguate(doc, entry.getValue());

          {  
        	System.out.println("c:" + concept);
      	    // Populate map_term_concept
      	    if (concept_count.get(concept) == null)
      	    { concept_count.put(concept, 1); }
      	    else
      	    { concept_count.put(concept, concept_count.get(concept) + 1); }

      	    // Populate sentence_concept
            sentence_concepts.add(concept);
          }
        }
      }
      
      // Add concept-concept counts
      for (String c1 : sentence_concepts)
      {
        for (String c2 : sentence_concepts)
        {
          if (!c1.equals(c2))
          {
        	Map <String, Integer> map = map_concept_concept.get(c1);
        	
        	if (map == null)
        	{
        	  map = new HashMap <String, Integer> ();
        	  map_concept_concept.put(c1, map);
        	}
        	
        	if (map.get(c2) == null)
        	{ map.put(c2, 1); }
        	else
        	{ map.put(c2, map.get(c2) + 1); }
          }
        }
      }

      entity_concepts.clear();
      System.out.println("***************");
	}
  };

  private static AbstractFaAction get_context = new AbstractFaAction()
  {
    public void invoke(StringBuffer yytext,int start,DfaRun runner)
  	throws CallbackException
	{
      String text = Xml.splitElement(yytext, start).get(Xml.CONTENT);
      doc = Util.splitText(text, tm);
      System.out.println(text);
	}
  };
  
  static
  {
	try
	{
      Nfa nfa = new Nfa(Nfa.NOTHING);
      nfa.or(Xml.GoofedElement("context"), get_context);
      nfa.or(Xml.GoofedElement("e"), get_e);
      nfa.or(Xml.ETag("s"), get_s_end);
      dfa = nfa.compile(DfaRun.UNMATCHED_COPY);
	}
	catch (Exception e)
	{
	  e.printStackTrace();
	  System.exit(-1);
	}
  }
  
  public static void main (String [] argc)
  {
    Entity e1 = new Cooccurrences().new Entity (1, 10, "World war");
    Entity e2 = new Cooccurrences().new Entity (1, 10, "World war");
    Entity e3 = new Cooccurrences().new Entity (5, 10, "war");
    
    System.out.println(e1.inEntity(e2));
    System.out.println(e1.inEntity(e3));

    System.out.println(e3.inEntity(e1));
    System.out.println(e3.inEntity(e2));
  }
}