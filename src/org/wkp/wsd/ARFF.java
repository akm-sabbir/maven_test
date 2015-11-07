package org.wkp.wsd;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.wkp.model.TokenMatrix;


public class ARFF
{
  private static Pattern p = Pattern.compile("\"");

  public static List <Document> load (String file_name, TokenMatrix tm)
  throws IOException
  {
    List <Document> list = new LinkedList <Document> ();

    BufferedReader b = new BufferedReader(new FileReader(file_name));

    String line;

    while ((line = b.readLine()) != null)
    {
      String [] tokens = p.split(line);
      
      if (tokens.length > 1)
      {
        list.add(new Document(tokens[0].replaceAll(",", ""),
    		                  tokens[1],
    		                  tokens[2].substring(1),
    		                  tm
    		                  ));
      }
    }

    b.close();

    return list;
  }

  public static void main (String [] argc)
  throws IOException
  {
    for (Document doc : ARFF.load("/home/antonio/intecmevi/mihalcea/arff/argument_pmids_tagged.arff", new TokenMatrix()))
    {
      System.out.println(doc.getPMID());
      System.out.println(doc.getText());
      System.out.println(doc.getSense());
      System.out.println("----------------");
    }
  }
}