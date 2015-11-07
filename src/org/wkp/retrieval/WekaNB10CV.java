package org.wkp.retrieval;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.wkp.wsd.Benchmark;

import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.Instances;
import weka.core.tokenizers.NGramTokenizer;
import weka.filters.unsupervised.attribute.StringToWordVector;

/**
 * 
 * @author Antonio Jimeno Yepes (antonio.jimeno@gmail.com)
 *
 */
public class WekaNB10CV
{

  public static void main (String [] argc) throws Exception
  {
	Configuration config = new PropertiesConfiguration("experiment.properties");
	  
	String examples_folder = config.getString("examples");
	
	double total_accuracy = 0;
	int count = 0;
	    
	for (Map.Entry<String, List <String>> entry : Benchmark.load(config.getString("benchmark")).entrySet())
	{
	  Instances instances = new Instances(new BufferedReader(new FileReader(new File(examples_folder, entry.getKey() + "_pmids_tagged.arff"))));

      // setting class attribute
      instances.setClassIndex(instances.numAttributes() - 1);

      NGramTokenizer tokenizer = new NGramTokenizer();
      tokenizer.setNGramMaxSize(1);
      tokenizer.setDelimiters(" \r\n\t.,;:'\"()?!/{}\\[\\]\\-0123456789");

      // Turn the strings into bag-of-words
      StringToWordVector filter = new StringToWordVector();

      filter.setLowerCaseTokens(true);
      filter.setWordsToKeep(1000000);
      
      filter.setTokenizer(tokenizer);
      filter.setUseStoplist(true);

      // Train the classifier
      filter.setInputFormat(instances);

      Instances instancesToken = weka.filters.Filter.useFilter(instances, filter);

      // 10 CV
      Evaluation eval = new Evaluation(instancesToken);
      eval.crossValidateModel(new NaiveBayes(), instancesToken, 10, new Random(1));
      System.out.println(entry.getKey() + "|Accuracy|" + eval.pctCorrect());
      total_accuracy += eval.pctCorrect();
      //System.out.println(eval.toClassDetailsString());
      count++;
	}
	
	System.out.println(total_accuracy/count);
  } 
}