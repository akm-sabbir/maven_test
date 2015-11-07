package org.wkp.wsd;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class Benchmark
{
  private static Pattern p = Pattern.compile("\t");
	
  public static Map <String, List <String>> load(String file_name)
  throws IOException
  {
	Map <String, List <String>> benchmark =
			new HashMap <String, List <String>> ();
	
	BufferedReader b = new BufferedReader(new FileReader(file_name));

	String line;
	
	while ((line = b.readLine()) != null)
	{
	  String [] tokens = p.split(line);
	  
	  List <String> list = new LinkedList <String> ();
	  
	  for (int i = 1; i < tokens.length; i++)
	  { list.add(tokens[i]); }
	  
	  benchmark.put(tokens[0], list);
	}
	
	b.close();
	
	return benchmark;
  }
  
  public static void main (String [] argc)
  throws IOException
  {
	Set <String> cuis = new HashSet <String> ();
	  
	for (Map.Entry <String, List <String>> entry : load("C:\\intecmevi\\MSHCorpus\\benchmark_mesh.txt").entrySet())
	{
	  for (String cui : entry.getValue())
	  { //System.out.println(cui);
        cuis.add(cui);		  
	  }
	}

	System.out.println(cuis.size());

	for (String cui : cuis)
	{ System.out.println(cui); }
  }
}
