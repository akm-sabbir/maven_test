package org.wkp.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 *  Procedures to remove stopwords
 *  
 *  @author Antonio Jimeno
 *  @version 1.0
*/
public class CommonWords 
{
  private static Set <String> vector = new HashSet <String> ();

  static
  {  
    try
    {
      BufferedReader f = new BufferedReader(new InputStreamReader(CommonWords.class.getResourceAsStream("common_words")));
      String line;
      while ((line=f.readLine()) != null)
      { vector.add(line); }
      f.close();
    }
    catch (Exception e)
    { e.printStackTrace(); }
  }

  public static Set <String> getSet()
  { return Collections.unmodifiableSet(vector); }

  public static boolean checkWord(String string)
  { return vector.contains(string); }

  public static String cleanWords(String string)
  {
    StringBuilder result = new StringBuilder();
    
    String [] words = string.split(" ");
    
    for (int i=0; i<words.length; i++)
    { 
      if (!checkWord(words[i].toLowerCase()))
      { result.append(" ").append(words[i]); }
    }

    return result.toString();
  }

  public static void main(String [] argc)
  {  
    System.out.println(CommonWords.checkWord("like"));
    
    System.out.println(CommonWords.cleanWords("house and car"));
  }
}