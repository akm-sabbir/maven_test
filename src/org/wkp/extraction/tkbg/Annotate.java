package org.wkp.extraction.tkbg;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import monq.jfa.AbstractFaAction;
import monq.jfa.CallbackException;
import monq.jfa.Dfa;
import monq.jfa.DfaRun;
import monq.jfa.Nfa;
import monq.jfa.ReaderCharSource;
import monq.jfa.Xml;
import monq.jfa.actions.Printf;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class Annotate
{
  private static Dfa dfa = null;
  private static Dfa dfaAnnotation = null;

  private static StanfordCoreNLP pipeline = null;
  
  private static AbstractFaAction get_context = new AbstractFaAction()
  {
    public void invoke(StringBuffer yytext,int start,DfaRun runner)
  	throws CallbackException
	{
      Map <String, String> map = Xml.splitElement(yytext, start);
      String text = map.get(Xml.CONTENT).trim()
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
    		  .replaceAll("±", " ");
      
      Annotation annotation = new Annotation(text);      
      
      // Annotate sentences
      pipeline.annotate(annotation);
      
      int sentence_id = 1;
      
      yytext.append("<annotation>");
      
      List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
      
      if (sentences != null && sentences.size() > 0)
      {
        for (CoreMap sentence : sentences)
        {
          yytext.append("<s id=\"" + sentence_id + "\">");
          yytext.append("<text>");
          yytext.append(sentence.toString());
          yytext.append("</text>");
          // Annotation
          yytext.append("<es>");
    		String a="http://krono.act.uji.es/annotator/q=" + sentence.toString().replaceAll(" ", "%20");
      	  
      	  try
          {
      	    URL url = new URL(a);
      	    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      	    Reader in = new InputStreamReader(
      	    		conn.getInputStream());
      	    yytext.append(processAnnotation(in));
            in.close();
            conn.disconnect();
          }
          catch (Exception e)
          { e.printStackTrace();
            System.err.println(a);
          }
          yytext.append("</es>");
          yytext.append("</s>");
          sentence_id++;
        }
      }
      
      yytext.append("</annotation>");
	}
  };

  static
  {
	try
	{
      Nfa nfa = new Nfa(Nfa.NOTHING);
      nfa.or(Xml.GoofedElement("context"), get_context);
      dfa = nfa.compile(DfaRun.UNMATCHED_COPY);
      
      Nfa nfaAnnotation = new Nfa(Nfa.NOTHING);
      nfaAnnotation.or(Xml.GoofedElement("e"), new Printf("%0"));
      dfaAnnotation = nfaAnnotation.compile(DfaRun.UNMATCHED_DROP);
	}
	catch (Exception e)
	{
      e.printStackTrace();
      System.exit(-1);
	}
  }

  private static String processAnnotation(Reader in)
  throws IOException
  {
	StringBuffer buffer = new StringBuffer();
    DfaRun dfaRun = new DfaRun(dfaAnnotation);
    dfaRun.setIn(new ReaderCharSource(in));
	dfaRun.filter(buffer);
    return buffer.toString();
  }
  
  /*private static URLConnection annotate (String sentence)
  throws IOException
  {
	String a="http://krono.act.uji.es/annotator/q=" + sentence.replace(" ", "%20");
    URL url = new URL(a);
    URLConnection conn = url.openConnection();
    return new InputStreamReader(
    		conn.getInputStream());
  }*/
  
  public static void main (String [] argc)
  throws IOException, URISyntaxException
  {
	System.setProperty("http.keepAlive", "false");
	  
    Properties props = new Properties();
    props.put("annotators", "tokenize, ssplit");
    pipeline = new StanfordCoreNLP(props);    
	  
    File folder = new File("/home/antonio/intecmevi/mihalcea/examples");
    
    Set <String> completed = new HashSet <String> ();
    completed.add("disc.n.train");
    completed.add("stress.n.train");
    completed.add("nature.n.train");
    completed.add("difference.n.train");
    completed.add("material.n.train");
    completed.add("chair.n.train");
    completed.add("mouth.n.train");
    completed.add("plan.n.train");
    completed.add("spade.n.train");
    completed.add("stress.n.train.ann");
	completed.add("spade.n.train.ann");
	completed.add("grip.n.train");
	completed.add("dyke.n.train");
	completed.add("sort.n.train");
	completed.add("performance.n.train");
	completed.add("paper.n.train");
	completed.add("bar.n.train");
	completed.add("channel.n.train");
	completed.add("fatigue.n.train");

	// Revise this one
	completed.add("bank.n.train");
 
    if (folder.isDirectory() && folder.listFiles() != null)
    {
      for (File file : folder.listFiles())
      {
        System.out.println(file.getName());
        
        //if (!completed.contains(file.getName()) && file.getName().endsWith(".n.train"))
        if (file.getName().equals("bank.n.train"))
        {
          DfaRun dfaRun = new DfaRun(dfa);
          dfaRun.setIn(new ReaderCharSource(new FileInputStream(file), "UTF-8"));
          dfaRun.filter(new PrintStream(file.getAbsolutePath() + ".ann"));
          //System.exit(-1);
        }
      }
    }
  }
}