package org.wkp.wsd;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

public class GetBabelNetBenchmark
{
  public static TreeMap <String, TreeMap<String, String>> getBenchmark(String file_name) throws IOException
  {
	Pattern p = Pattern.compile("\\|");
	  
	TreeMap <String, TreeMap<String, String>> benchmark =
			new TreeMap <String, TreeMap<String, String>> ();
	  
	BufferedReader b = new BufferedReader(new FileReader("/home/antonio/Dropbox/Berlanga_SIGIR_2014/mihalcea_dataset/mapping.txt"));
		
	String line;
	
	while ((line = b.readLine()) != null)
	{
      String [] tokens = p.split(line);
      
      if (tokens.length == 3)
      {
        TreeMap <String, String> term_code = benchmark.get(tokens[0]);
        
        if (term_code == null)
        {
          term_code = new TreeMap <String, String> ();
          benchmark.put(tokens[0], term_code);
        }
        
        term_code.put(tokens[1], tokens[2]);
      }
	}
		
	b.close();
	
	return benchmark;
  }
	
  public static void main (String [] argc)
  throws IOException
  {
    TreeMap <String, TreeMap<String, String>> benchmark = getBenchmark("/home/antonio/Dropbox/Berlanga_SIGIR_2014/mihalcea_dataset/mapping.txt");
    
    for (Map.Entry <String, TreeMap <String, String>> entry : benchmark.entrySet())
    {
      System.out.print(entry.getKey());
      
      for (Map.Entry <String, String> value : entry.getValue().entrySet())
      { System.out.print("\t"); System.out.print(value.getValue()); }
      
      System.out.println();
    }
  }
}