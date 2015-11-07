package org.wkp.retrieval;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public class GenerateQueries
{
  private static Pattern p = Pattern.compile("\\|");

  public static void main (String [] argc)
  throws IOException
  {
    // Load queries
    LinkedList <String> cuis = new LinkedList <String> ();
    
    Map <String, StringBuilder> cui_text =
    		new HashMap <String, StringBuilder> ();

    {
      String line;

      BufferedReader b = new BufferedReader(new FileReader("c:\\datasets\\amia_set\\cui_mesh.txt"));
      while ((line = b.readLine()) != null)
      {
    	cuis.add(p.split(line)[0]);
        cui_text.put(p.split(line)[0], new StringBuilder ());
      }
      b.close();
    }

    BufferedReader b =
  		  new BufferedReader(
   				  new InputStreamReader(
   						  new GZIPInputStream(new FileInputStream("C:\\datasets\\2009AB\\META\\MRCONSO.RRF.gz"))));

    String line;

    while ((line = b.readLine()) != null)
    {
      String [] tokens = p.split(line);

      if (tokens[1].equals("ENG"))
      {
    	if (cui_text.get(tokens[0]) != null)
    	{
    	  cui_text.get(tokens[0]).append(" ")
    	                         .append(tokens[14].toLowerCase());
    	}
      }
    }

    b.close();

    for (String cui : cuis)
    { System.out.println(cui + "|" + cui_text.get(cui)); }
  }
}