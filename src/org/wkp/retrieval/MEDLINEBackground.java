package org.wkp.retrieval;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.wkp.model.ProbabilityModel;
import org.wkp.model.TokenMatrix;
import org.wkp.utils.Util;

import monq.jfa.AbstractFaAction;
import monq.jfa.CallbackException;
import monq.jfa.Dfa;
import monq.jfa.DfaRun;
import monq.jfa.Nfa;
import monq.jfa.ReaderCharSource;
import monq.jfa.Xml;

public class MEDLINEBackground
{
  private static Dfa dfa = null;

  private static TokenMatrix tm = null;
  
  private static Map <Integer, Double> count =
		  new HashMap <Integer, Double> ();

  private static double total_count = 0;

  private static double alpha = 1.0;

  private static AbstractFaAction get_text = new AbstractFaAction()
  {
    public void invoke(StringBuffer yytext,int start,DfaRun runner)
  	throws CallbackException
	{
      for (Map.Entry <Integer, Double> entry : Util.splitText(Xml.splitElement(yytext, start).get(Xml.CONTENT), tm).entrySet())
      {
        if (count.get(entry.getKey()) == null)
        { count.put(entry.getKey(), entry.getValue()); }
        else
        { count.put(entry.getKey(), count.get(entry.getKey()) + entry.getValue()); }
        
        total_count += entry.getValue();
      }
	}
  };

  static
  {
    try
    {     
  	  Nfa nfa = new Nfa(Nfa.NOTHING);
  	  nfa.or(Xml.GoofedElement("ArticleTitle"), get_text);
  	  nfa.or(Xml.GoofedElement("AbstractText"), get_text);  
  	  dfa = nfa.compile(DfaRun.UNMATCHED_COPY);
    }
	catch (Exception e)
	{ throw new Error("ExtractMedlineCitation error!", e); }
  }

  public static void main (String [] argc)
  throws FileNotFoundException, IOException, ClassNotFoundException
  {
	// Load data models
    ProbabilityModel dm = null;  

	{
      ObjectInputStream i = new ObjectInputStream(new GZIPInputStream(new FileInputStream(argc[1])));
      dm = (ProbabilityModel)i.readObject();
      i.close();
  	}

	tm = dm.getTokenMatrix();

  	DfaRun dfaRun = new DfaRun(dfa);
    dfaRun.setIn(new ReaderCharSource(new GZIPInputStream(new FileInputStream(argc[0])), "UTF-8"));
    dfaRun.filter();

    // Estimate the probabilities
    double denominator =
            Math.log((alpha * (double)count.size()) + (double)total_count);

    Map <Integer, Double> probs =
            new HashMap <Integer, Double> ();

    double sum = 0.0;

    // Write the final background model
    for (Map.Entry <Integer, Double> entry : count.entrySet())
    {
      Double probability = Math.log(alpha + entry.getValue()) - denominator;
      probs.put(entry.getKey(), probability);
      sum +=Math.exp(probability);
    }

    // Print the probability sum
    System.out.println("Tokens count: " + count.size());
    System.out.println("Token probability sum: " + sum);

	{
      ObjectOutputStream o = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(argc[2])));
	  o.writeObject(probs);
	  o.close();
	}
  }
}