package org.wkp.retrieval;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import monq.jfa.AbstractFaAction;
import monq.jfa.CallbackException;
import monq.jfa.Dfa;
import monq.jfa.DfaRun;
import monq.jfa.Nfa;
import monq.jfa.ReaderCharSource;
import monq.jfa.Xml;

public class MEDLINEConvertToWeb
{
  private static String PMID = null;

  private static StringBuilder text =
		  new StringBuilder();
  
  private static BufferedWriter w = null; 

  private static Dfa dfa = null;
  
  private static AbstractFaAction get_pmid = new AbstractFaAction()
  {
  	public void invoke(StringBuffer yytext,int start,DfaRun runner)
  	throws CallbackException
  	{
  	  if (PMID == null)
  	  {	PMID = Xml.splitElement(yytext, start).get(Xml.CONTENT); }
	}
  };
  
  private static AbstractFaAction get_text = new AbstractFaAction()
  {
  	public void invoke(StringBuffer yytext,int start,DfaRun runner)
  	throws CallbackException
  	{
  	  Map <String, String> map = Xml.splitElement(yytext, start);

  	  text.append(" ")
  	      .append(map.get(Xml.CONTENT));
	}
  };
  
  private static AbstractFaAction get_medline_citation = new AbstractFaAction()
  {
  	public void invoke(StringBuffer yytext,int start,DfaRun runner)
  	throws CallbackException
  	{
      try
      {
		w.write("<DOC>");
	    w.newLine();
	    w.write("<DOCNO> " + PMID + "</DOCNO>");
	    w.newLine();
	    w.newLine();
	    w.write(text.toString().trim());
	    w.newLine();
	    w.newLine();
	    w.write("</DOC>");
	    w.newLine();
	  }
      catch (IOException e)
      { e.printStackTrace(); }
      
      text.setLength(0);
      PMID = null;
	}
  };

  static
  {
    try
    {     
  	  Nfa nfa = new Nfa(Nfa.NOTHING);
  	  nfa.or(Xml.GoofedElement("PMID"), get_pmid);
  	  nfa.or(Xml.GoofedElement("ArticleTitle"), get_text);
  	  nfa.or(Xml.GoofedElement("AbstractText"), get_text);
  	  nfa.or(Xml.ETag("MedlineCitation"), get_medline_citation);  	  
  	  dfa = nfa.compile(DfaRun.UNMATCHED_COPY);
    }
	catch (Exception e)
	{ throw new Error("ExtractMedlineCitation error!", e); }
  }

  public static void main (String [] argc)
  throws IOException
  {
	w = new BufferedWriter(new FileWriter(argc[1]));

    // Filter the file
  	DfaRun dfaRun = new DfaRun(dfa);
    dfaRun.setIn(new ReaderCharSource(new GZIPInputStream(new FileInputStream(argc[0])), "UTF-8"));
    dfaRun.filter();

    w.close();
  }
}