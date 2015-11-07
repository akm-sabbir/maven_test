package org.wkp.retrieval;

import gov.nih.nlm.nls.utils.Trie;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.wkp.utils.Util;
import org.wkp.wsd.Disambiguation;

import monq.jfa.AbstractFaAction;
import monq.jfa.CallbackException;
import monq.jfa.Dfa;
import monq.jfa.DfaRun;
import monq.jfa.Nfa;
import monq.jfa.ReaderCharSource;
import monq.jfa.Xml;

public class RankCitationsBerlanga
{
  private static Dfa dfa = null;
  private static String PMID = null;

  private static Trie <Integer> trie = new Trie <Integer> ();

  private static Pattern p_space = Pattern.compile(" ");
  private static Pattern p_colon = Pattern.compile(":");

  private static StringBuilder text =
			  new StringBuilder();

  private static Map <String, Map <Integer, Double>> docs =
			  new HashMap <String, Map <Integer, Double>> ();

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
      Map <Integer, Double> doc = Util.splitTextCountNoStemmer(text.toString(), trie);

      docs.put(PMID, doc);
      
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
	
  public static void main (String [] argc)
  throws IOException
  {
    List <String> concepts = new LinkedList <String> ();

    {
	BufferedReader b = new BufferedReader(new FileReader(argc[0]));

	String line;

	while ((line = b.readLine()) != null)
	{
	  if (line.trim().length() > 0 && line.startsWith("C"))
	  concepts.add(line.trim());
	}
	    
	b.close();
    }
	  
    
    
    Map <String, Map<Integer, Double>> cui_profile =
    		new HashMap <String, Map <Integer, Double>> ();

    {
    int token_count = 0;
    // Load the models
    BufferedReader b = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(argc[1]))));

    String line = null;
    
    while ((line = b.readLine()) != null)
    {
      String [] tokens = p_space.split(line);

      String cui = tokens[0];

      Map <Integer, Double> profile = new HashMap <Integer, Double> ();

      cui_profile.put(cui, profile);

      for (int i=1; i< tokens.length; i++)
      {
        String [] tp = p_colon.split(tokens[i]);

        if (tp.length == 2)
        { 
          // Get index_id
          Integer index = trie.get(tp[0]);

          if (index == null)
          {
            index = token_count++;
            trie.insert(tp[0], index);
          }

          //System.out.println(tokens[i]);
          profile.put(index, new Double(tp[1]));
        }
      }
    }

    b.close();
    }

    // Load citations and predict MeSH categories
    DfaRun dfaRun = new DfaRun(dfa);
    //dfaRun.setIn(new ReaderCharSource(System.in, "UTF-8"));
    dfaRun.setIn(new ReaderCharSource(new GZIPInputStream(new FileInputStream(argc[2])), "UTF-8"));
    dfaRun.filter();
    
    for (String concept: concepts)
    {
      Map <Integer, Double> profile = cui_profile.get(concept); 

      for (Map.Entry <String, Map <Integer, Double>> doc : docs.entrySet())
      { System.out.println(doc.getKey() + "|" + concept + "|" + Disambiguation.getCrossEntropyBerlanga(profile, doc.getValue())); }
    }
  }
}