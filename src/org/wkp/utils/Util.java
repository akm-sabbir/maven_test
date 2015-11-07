package org.wkp.utils;

import gov.nih.nlm.nls.utils.Constants;
import gov.nih.nlm.nls.utils.Trie;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.wkp.model.TokenMatrix;

public class Util
{
  public static final Pattern ptoken = Pattern.compile(Constants.tokenizationExpression);
	
  public static Pattern getPToken()
  { return Pattern.compile(Constants.tokenizationExpression); }
	
  public static boolean isNumber(String string)
  {
	try
	{
	  Double.parseDouble(string);
	  return true;
	}
	catch (Exception e)
	{ return false; }
  }
  
  public static List <Map.Entry <String, Double>> mapValueDoubleSortDesc(Map <String, Double> map)
  {
    List <Map.Entry <String, Double>> scoreRank =
   	    new ArrayList <Map.Entry <String, Double>> (map.entrySet());

    Collections.sort(scoreRank, new Comparator <Map.Entry <String, Double>> ()
    {
      public int compare(Map.Entry <String, Double> o1, Map.Entry <String, Double> o2)
      { return o2.getValue().compareTo(o1.getValue()); }
    });
	  
	return scoreRank;
  }

  public static List <Map.Entry <String, Integer>> mapValueIntegerSortDesc(Map <String, Integer> map)
  {
    List <Map.Entry <String, Integer>> scoreRank =
   	    new ArrayList <Map.Entry <String, Integer>> (map.entrySet());

    Collections.sort(scoreRank, new Comparator <Map.Entry <String, Integer>> ()
    {
      public int compare(Map.Entry <String, Integer> o1, Map.Entry <String, Integer> o2)
      { return o2.getValue().compareTo(o1.getValue()); }
    });
	  
	return scoreRank;
  }

  public static List <Map.Entry <Integer, Double>> mapIntegerDoubleSortDesc(Map <Integer, Double> map)
  {
    List <Map.Entry <Integer, Double>> scoreRank =
   	    new ArrayList <Map.Entry <Integer, Double>> (map.entrySet());

    Collections.sort(scoreRank, new Comparator <Map.Entry <Integer, Double>> ()
    {
      public int compare(Map.Entry <Integer, Double> o1, Map.Entry <Integer, Double> o2)
      { return o2.getValue().compareTo(o1.getValue()); }
    });

	return scoreRank;
  }

  public static Map <Integer, Double> splitText(String text, TokenMatrix tm)
  {
    Map <Integer, Double> token_count = new HashMap <Integer, Double> ();

    PorterStemmer stemmer = new PorterStemmer();
    
	for (String token : ptoken.split(text.toLowerCase()))
	{
	 if (token.trim().length() > 0 && !CommonWords.checkWord(token) && !Util.isNumber(token))
     {
        String token_stemmed = stemmer.stem(token);
		Integer tt = tm.getTrieToken().get(token_stemmed);
		
		if (tt != null)
		{ token_count.put(tt, 1.0); }
	  }
	}
	
	return token_count;
  }

  public static Map <Integer, Double> splitTextCount(String text, TokenMatrix tm)
  {
    Map <Integer, Double> token_count = new HashMap <Integer, Double> ();

    PorterStemmer stemmer = new PorterStemmer();
    
	for (String token : ptoken.split(text.toLowerCase()))
	{
	  if (token.trim().length() > 0 && !CommonWords.checkWord(token) && !Util.isNumber(token))
      {
        String token_stemmed = stemmer.stem(token);

	    Integer tt = tm.getIndexToken(token_stemmed);

	    if (tt != null)
	    {
		  if (token_count.get(tt) == null)
		  { token_count.put(tt, 1.0); }
		  else
		  { token_count.put(tt, token_count.get(tt) + 1.0); }
	    }
	  }
	}

	return token_count;
  }

  public static Map <Integer, Double> splitTextCount(String text, Trie <Integer> trie)
  {
    Map <Integer, Double> token_count = new HashMap <Integer, Double> ();

    PorterStemmer stemmer = new PorterStemmer();

	for (String token : ptoken.split(text.toLowerCase()))
	{
	 if (token.trim().length() > 0 && !CommonWords.checkWord(token) && !Util.isNumber(token))
     {
		String token_stemmed = stemmer.stem(token);

		Integer tt = trie.get(token_stemmed);

		if (tt == null)
		{
		  tt = trie.size();
		  trie.insert(token_stemmed, tt);
		}

        if (token_count.get(tt) == null)
        { token_count.put(tt, 1.0); }
        else
        { token_count.put(tt, token_count.get(tt) + 1.0); }
	  }
	}

	return token_count;
  }
  
  public static Map <Integer, Double> splitTextCountNoStemmer(String text, Trie <Integer> trie)
  {
    Map <Integer, Double> token_count = new HashMap <Integer, Double> ();

	for (String token : ptoken.split(text.toLowerCase()))
	{
	 if (token.trim().length() > 0 && !CommonWords.checkWord(token) && !Util.isNumber(token))
     {
		Integer tt = trie.get(token);
		
		if (tt != null)
		{
		  if (token_count.get(tt) == null)
		  { token_count.put(tt, 1.0); }
		  else
		  { token_count.put(tt, token_count.get(tt) + 1.0); }
		}
	  }
	}

	return token_count;
  }
  
  public static Map <Integer, Double> textToVectorEnglish(String text, Trie <Integer> trie)
  {
	Map <Integer, Double> vector =
		new HashMap <Integer, Double> ();

	PorterStemmer stemmer = new PorterStemmer();
	
    for (String token : ptoken.split(text.toLowerCase()))
    {
	  if (token.trim().length() > 0 && !CommonWords.checkWord(token) && !Util.isNumber(token))
	  {
		String token_stemmed = stemmer.stem(token);
		//String token_stemmed = token;

        Integer index = trie.get(token_stemmed);

        if (index != null)
        {
          if (vector.get(index) == null)
          { vector.put(index, 1.0); }
          else
          { vector.put(index, vector.get(index) + 1.0); }
        }
	  }    
    }

    return vector;
  }
}