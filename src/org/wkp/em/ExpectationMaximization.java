package org.wkp.em;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.wkp.model.ConceptMatrix;
import org.wkp.model.ProbabilityModel;
import org.wkp.model.TokenMatrix;
import org.wkp.wsd.Benchmark;
import org.wkp.wsd.Disambiguation;


public class ExpectationMaximization
{
  public static double [] getLambda(ProbabilityModel dm,
		                            String benchmark_file_name,
		                            String instance_folder_name)
  throws IOException
  {
	TokenMatrix tm = dm.getTokenMatrix();
	ConceptMatrix cm = dm.getConceptMatrix();
	double [] cui_tm = dm.getPC();
	
	Map <String, List <String>> benchmark = Benchmark.load(benchmark_file_name);

    double [] lambda = { 1/3.0, 1/3.0, 1/3.0 };

    Disambiguation.print_term_prob(tm,
            cm,
            cui_tm,
            benchmark,
            lambda
           );
    
    // Initial log likelihood of the model
    // Loglikelihood of the initial model
    double log_prob = Disambiguation.disambiguate(tm,
                                                  cm,
	    		                                  cui_tm,
	    		                                  benchmark,
	    		                                  lambda,
	    		                                  instance_folder_name);

    System.out.println("log_prob: " + log_prob);
/*
    double [] new_lambda = lambda;
    double new_log_prob = log_prob;

    int count = 0;

    do
    {
      lambda = new_lambda;
      log_prob = new_log_prob;

      System.out.println("Lambda: " + Arrays.toString(lambda));

      // Estimate the new lambdas
      new_lambda = Disambiguation.estimate_lambda(tm,
              									  cm,
              									  cui_tm,
              									  benchmark,
              									  lambda,
                                                  instance_folder_name
              	);

      System.out.println("lambda_proposal: " + Arrays.toString(new_lambda));

      // Apply the new lambdas
      new_log_prob = Disambiguation.disambiguate(tm,
                                                 cm,
	                                             cui_tm,
	                                             benchmark,
	                                             new_lambda,
	                                             instance_folder_name);

      System.out.println("new log_prob: " + new_log_prob);
	      
      count++;
    }
    while (log_prob > new_log_prob && count < 10);
*/
    Disambiguation.print_term_prob(tm,
                                   cm,
                                   cui_tm,
                                   benchmark,
                                   lambda
                                  );

    System.out.println("Lambda: " + Arrays.toString(lambda));
    System.out.println("log_prob: " + log_prob);

	return lambda;
  }

  public static void main (String [] argc)
  throws FileNotFoundException, IOException, ClassNotFoundException
  {
    ProbabilityModel dm = null;  

	{
      ObjectInputStream i = new ObjectInputStream(new GZIPInputStream(new FileInputStream(argc[0])));
      dm = (ProbabilityModel)i.readObject();
      i.close();
  	}

	{
      ObjectInputStream i = new ObjectInputStream(new GZIPInputStream(new FileInputStream(argc[1])));
      Disambiguation.p_w_c = (Map <Integer, Double>)i.readObject();
      i.close();
	}

	getLambda(dm, argc[2], argc[3]);
  }
}