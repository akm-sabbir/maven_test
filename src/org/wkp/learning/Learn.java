package org.wkp.learning;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import monq.jfa.ReSyntaxException;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.wkp.em.ExpectationMaximization;
import org.wkp.extraction.metamap.MetaMapCooccurrences;
import org.wkp.kb.AbstractKB;
import org.wkp.model.ConceptMatrix;
import org.wkp.model.ProbabilityModel;
import org.wkp.model.TokenMatrix;
import org.wkp.wsd.Disambiguation;

/**
 * 
 * @author Antonio Jimeno Yepes (antonio.jimeno@gmail.com)
 */
public class Learn
{
  public static void main (String [] argc)
  throws IOException, ReSyntaxException, ClassNotFoundException, InstantiationException, IllegalAccessException, ConfigurationException
  {
    int iterations = 10;

    Configuration config = new PropertiesConfiguration("experiment.properties");
    
    Class c = Class.forName(config.getString("kb.class"));
    AbstractKB kb = (AbstractKB)c.newInstance();
    
    // Initalize
    //  Generate initial model
    //
    //ProbabilityModel dm = UMLS2TermMatrix.generateModel(argc[0], argc[1]);
    ProbabilityModel dm = kb.generateModel();

	{
      ObjectInputStream i = new ObjectInputStream(new GZIPInputStream(new FileInputStream(config.getString("p_w_c"))));
      Disambiguation.p_w_c = (Map <Integer, Double>)i.readObject();
      i.close();
	}

    // EM to obtain lambdas
    double [] lambda =
    		ExpectationMaximization.getLambda(dm,
    				                          config.getString("benchmark"),
    				                          config.getString("examples")
    );
    Disambiguation.in_cps = false;
    Disambiguation.clearCPS();

	for (int i = 0; i < iterations; i++)
	{
	  MetaMapCooccurrences.lambda_model = lambda;
      // Initalize
	  //  Generate initial model
	  //ProbabilityModel newdm = UMLS2TermMatrix.generateModel(argc[0], argc[1], dm, argc[5], true);
	  ProbabilityModel newdm = kb.generateModel(dm, true);
	  
	  dm = newdm;
	  
	  
	  System.out.println("TM: " + dm.getTokenMatrix().getTM().size());

      // EM to obtain lambdas
  	  Disambiguation.in_cps = true;
	  lambda =
	  		ExpectationMaximization.getLambda(dm,
	  				                          config.getString("benchmark"),
	  				                          config.getString("examples")
	  );
      Disambiguation.in_cps = false;
	  Disambiguation.clearCPS();
	  
	}

	// Serialize the model
	{
      ObjectOutputStream o = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(config.getString("output"))));
      o.writeObject(dm);
	  o.close();
	}
  }
}