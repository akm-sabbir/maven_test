package org.wkp.wsd;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import monq.jfa.AbstractFaAction;
import monq.jfa.CallbackException;
import monq.jfa.Dfa;
import monq.jfa.DfaRun;
import monq.jfa.Nfa;
import monq.jfa.ReaderCharSource;
import monq.jfa.Xml;

public class GetBabelNetARFF
{
  private static String pmid = null;
  private static String sense = null;
  private static String text = null;

  private static Map <String, String> map = new HashMap <String, String> ();
  
  private static Dfa dfa = null;
  
  private static BufferedWriter w = null;

  private static AbstractFaAction get_context = new AbstractFaAction()
  {
    public void invoke(StringBuffer yytext,int start,DfaRun ruinstancenner)
  	throws CallbackException
	{
      Map <String, String> map = Xml.splitElement(yytext, start);
      text = map.get(Xml.CONTENT).trim()
    		  .replaceAll("<head>","").replaceAll("</head>","")
    		  .replaceAll("<math>","").replaceAll("</math>","")
    		  .replaceAll("/"," ")
    		  .replaceAll("#"," ")
    		  .replaceAll("%", "%25")
    		  .replaceAll("\\xAD", "")
    		  .replaceAll("°", " ")
    		  .replaceAll("£", " ")
    		  .replaceAll("¥", " ")
    		  .replaceAll("$", " ")
    		  .replaceAll("±", " ")
    		  .replaceAll(",", " ")
    		  .replaceAll("\\\"", " ")
    		  .replaceAll("'", " ")
    		  ;
	}
  };

  private static AbstractFaAction get_instance_end = new AbstractFaAction()
  {
    public void invoke(StringBuffer yytext,int start,DfaRun runner)
  	throws CallbackException
	{
      try
      {
    	if (sense != null)
    	{
		  //w.write(pmid);
		  w.write("1");
		  w.write(",\"");
		  w.write(text);
		  w.write("\",");
		  w.write(sense);
		  w.newLine();
    	}
	  }
      catch (IOException e)
      { throw new CallbackException (e.getMessage()); }
	}
  };

  private static AbstractFaAction get_answer = new AbstractFaAction()
  {
    public void invoke(StringBuffer yytext,int start,DfaRun runner)
  	throws CallbackException
	{
      Map <String, String> map_answer = Xml.splitElement(yytext, start);
      
      pmid = map_answer.get("instance");
      sense	 = map.get(map_answer.get("senseid"));
      
      //System.out.println(map_answer.get("instance"));
      //System.out.println(map_answer.get("senseid"));
      //System.out.println(map.get(map_answer.get("senseid")));
	}
  };

  static
  {
	try
	{
      Nfa nfa = new Nfa(Nfa.NOTHING);
      nfa.or(Xml.GoofedElement("context"), get_context);
      nfa.or("<answer (.*/>)!", get_answer);
      nfa.or(Xml.ETag("instance"), get_instance_end);
      dfa = nfa.compile(DfaRun.UNMATCHED_COPY);
      
      
	}
	catch (Exception e)
	{
	  e.printStackTrace();
	  System.exit(-1);
	}
  }

	
  public static void main (String [] argc) throws IOException
  {
	String output_folder = "/home/antonio/intecmevi/mihalcea/babelnet_benchmark";
	String instance_folder = "/home/antonio/intecmevi/mihalcea/examples";

	TreeMap <String, TreeMap <String, String>> benchmark =
			GetBabelNetBenchmark.getBenchmark("/home/antonio/Dropbox/Berlanga_SIGIR_2014/mihalcea_dataset/mapping.txt");
	
	for (Map.Entry <String, TreeMap <String, String>> entry : benchmark.entrySet())
	{
      w = new BufferedWriter(new FileWriter (new File(output_folder, entry.getKey() + "_pmids_tagged.arff")));
      
      // Get ids to Mx
      StringBuilder id_list = new StringBuilder();
      StringBuilder mx_list = new StringBuilder();
      
      map = new HashMap <String, String> ();
      
      int m_count = 1;
      for (Map.Entry<String, String> sm : entry.getValue().entrySet())
      {
    	if (id_list.length() != 0)
    	{
    	  id_list.append("_");
    	  mx_list.append(", ");
    	}
    	
  	    id_list.append(sm.getValue());
  	    mx_list.append("M"+m_count);
  	    
  	    map.put(sm.getKey(), "M"+m_count);

    	m_count++;
      }
      
      w.write("@RELATION " + id_list.toString());
      w.newLine(); w.newLine();
      w.write("@ATTRIBUTE PMID integer");
      w.newLine();
      w.write("@ATTRIBUTE citation string");
      w.newLine();
      w.write("@ATTRIBUTE classXXXX {" + mx_list.toString() + "}");
      w.newLine();w.newLine();
      w.write("@DATA");
      w.newLine();
      
      DfaRun dfaRun = new DfaRun(dfa);
      dfaRun.setIn(new ReaderCharSource(new FileInputStream(new File(instance_folder, entry.getKey() + ".n.train")), "UTF-8"));
      dfaRun.filter();
      
      w.flush();
      w.close();
	}
  }
}