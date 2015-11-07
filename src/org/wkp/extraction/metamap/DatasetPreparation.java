package org.wkp.extraction.metamap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Extract the documents and place them in files with name <PMID>.txt
 * 
 * @author Antonio Jimeno (antonio.jimeno@gmail.com)
 *
 */
public class DatasetPreparation
{
  public static void main (String [] argc)
  throws IOException
  {
    File folder = new File("C:\\datasets\\MSHCorpus");

    for (File file : folder.listFiles())
    {
      if (file.getName().endsWith(".arff"))
      {
    	System.out.println(file.getName());

    	BufferedReader b = new BufferedReader(new FileReader(file));

        String line;

        while ((line = b.readLine()) != null)
        {
          if (line.trim().length() > 0 && !line.startsWith("@"))
          {
        	String PMID = line.substring(0,line.indexOf(","));
        	String text = line.substring(line.indexOf(",")+2, line.length()-4).replace("<e>","").replaceAll("</e>", "");
        	
        	BufferedWriter w = new BufferedWriter(new FileWriter("C:\\datasets\\MSHCorpus\\PMID_text\\" + PMID + ".txt"));
        	w.write(new String(text.getBytes("US-ASCII")));
        	w.newLine();
        	w.flush();
            w.close();
          }
        }

        b.close();
      }
    }
  }
}