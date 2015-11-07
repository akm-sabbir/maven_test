package org.wkp.model;

import gov.nih.nlm.nls.utils.Trie;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class TokenMatrix implements Serializable
{
  /**
	 * 
	 */
  private static final long serialVersionUID = 1L;

  private Map <Integer, String> index_token =
			new HashMap <Integer, String> ();

  private Trie <Integer> trie_token = new Trie <Integer> ();

  private Map <Integer, Map <Integer, Double>> tm =
    		new HashMap <Integer, Map <Integer, Double>> ();
  
  private int token_index = 0;
  
  public Map <Integer, Map <Integer, Double>> getTM()
  { return tm; }

  public Map <Integer, String> getIndexToken()
  { return index_token; }

  public Integer getIndexToken(String token)
  {
    Integer index = trie_token.get(token);

    if (index == null)
    {
      index = token_index;
      trie_token.insert(token, token_index);
      token_index++;

      index_token.put(index, token);
    }

    return index;
  }
  
  public Trie <Integer> getTrieToken()
  { return trie_token; }
}
