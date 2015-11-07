package org.wkp.wsd;

import gov.nih.nlm.nls.utils.Constants;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.wkp.model.TokenMatrix;
import org.wkp.utils.CommonWords;
import org.wkp.utils.PorterStemmer;
import org.wkp.utils.Util;


public class Document
{
  private String pmid;
  private String text;
  private String sense;

  private Map <Integer, Double> token_count =
		  new HashMap <Integer, Double> ();

  private static final Pattern ptoken = Pattern.compile(Constants.tokenizationExpression);
  
  private static PorterStemmer stemmer = new PorterStemmer();

  private void splitText(TokenMatrix tm)
  {
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
  }

  public Document(String pmid, String text, String sense, TokenMatrix tm)
  {
	this.pmid = pmid;
	this.text = text;
	this.sense = sense;
	
	splitText(tm);
  }

  public String getPMID()
  { return pmid; }

  public String getText()
  { return text; }

  public String getSense()
  { return sense; }

  public Map <Integer, Double> getTokenCount()
  { return token_count; }
}