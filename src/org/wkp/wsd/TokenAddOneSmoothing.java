package org.wkp.wsd;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.wkp.model.TokenMatrix;
import org.wkp.utils.CommonWords;
import org.wkp.utils.PorterStemmer;
import org.wkp.utils.Util;

/**
 * 
 * Generate the background probablitity
 * 
 * @author Antonio Jimeno Yepes (antonio.jimeno@gmail.com)
 *
 */
public class TokenAddOneSmoothing
{
  private static Pattern p = Pattern.compile("\\|");
  private static final Pattern ptoken = Util.getPToken();
  
  private static Map <Integer, Double> probs =
		  new HashMap <Integer, Double> ();

  public static void main (String [] argc)
  throws FileNotFoundException, IOException, ClassNotFoundException
  {
    PorterStemmer stemmer = new PorterStemmer();

    BufferedReader b = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(argc[0]))));
    
    Map <Integer, Integer> map =
  		  new HashMap <Integer, Integer> ();

    double alpha = 1.0;

    // Load the token matrix
    TokenMatrix tm = null;

    {
      ObjectInputStream i = new ObjectInputStream(new GZIPInputStream(new FileInputStream(argc[1])));
      tm = (TokenMatrix)i.readObject();
      i.close();
  	}

    int total_number_tokens = 0;

    String line;

    while ((line = b.readLine()) != null)
    {
      String [] tokens = p.split(line);

      if (tokens[1].equals("ENG"))
      {
        for (String token : ptoken.split(tokens[14].toLowerCase()))
        {
          if (token.trim().length() > 0 && !CommonWords.checkWord(token) && !Util.isNumber(token))
          {
            // Look for a stemmer
            String token_stemmed = stemmer.stem(token);

            Integer index = tm.getTrieToken().get(token_stemmed);

            if (index != null)
            {
              if (map.get(index) == null)
              { map.put(index, 1); }
              else
              { map.put(index, map.get(index) + 1); }

              total_number_tokens++;
            }
          }
        }
      }
    }

    b.close();

    double sum = 0.0;

    double denominator = Math.log((alpha * (double)map.size()) + (double)total_number_tokens);

    for (Map.Entry <Integer, Integer> entry : map.entrySet())
    {
      Double probability = Math.log(alpha + (double)entry.getValue()) - denominator;
      probs.put(entry.getKey(), probability);
      sum +=Math.exp(probability);
    }

    // Print the probability sum
    System.out.println("Token probability sum: " + sum);

    int i = 0;

    for (Map.Entry <Integer, Double> entry : Util.mapIntegerDoubleSortDesc(probs))
    {
      if (i == 100) break;
      System.out.println(i + "|" + entry.getKey() + "|" +  tm.getIndexToken().get(entry.getKey()) + "|" + entry.getValue() + "|" + map.get(entry.getKey()));
      i++;
    }

	{
      ObjectOutputStream o = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(argc[2])));
	  o.writeObject(probs);
	  o.close();
	}
  }
}