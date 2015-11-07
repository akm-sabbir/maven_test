package org.wkp.mc;

import java.util.Map;

/**
 * 
 * 
 * @author Antonio Jimeno Yepes (antonio.jimeno@gmail.com)
 *
 */
public class MarkovChain
{
  private static double getMax (double [] values, int count)
  {
	double result = Double.NEGATIVE_INFINITY;

	for (int i = 0; i < count; i++)
	{
	  if (values[i] > result)
	  { result = values[i]; }
	}

	return result;
  }

  private static double sumVector(double [] values, int count)
  {
	if (count == 0)
	{ return Double.NEGATIVE_INFINITY; }
	
    // Get the maximum element
    double max = getMax(values, count);

    if (Double.isInfinite(max))
    { return Double.NEGATIVE_INFINITY; }
    
    double result_sum = 0;

    for (int i = 0; i < count; i++)
    {
      double val = values[i] - max;

      if (!Double.isInfinite(val))
      { result_sum += Math.exp(val); }
    }

    return max + Math.log(result_sum);
  }
  private static double diverge(Map <Integer, Map <Integer, Double>> COM){
  	double var_for_convergence = Double.NEGATIVE_INFINITY;
	//double [] result = new double[]
 	if(var_for_convergence < 0)
		System.out.println(var_for_convergence);
	else 
		System.out.println("it is okay and positive");
	return var_for_convergence;
  }
  private static double [] converge (Map <Integer, Map <Integer, Double>> tm,
		                             double [] values)
  {
    double [] result = new double[values.length];

    double [] aux = new double[values.length];
    
    int aux_count;
    
    for (int i = 0; i < result.length; i++)
    { result[i] = Double.NEGATIVE_INFINITY; }
    
    // Vector matrix multiplication
    //for (int col_id = 0; col_id < result.length; col_id++)
    for (Map.Entry <Integer, Map <Integer, Double>> col : tm.entrySet())
    {
      //if (col_id % 1000 == 0) { System.out.println(col_id); }

      aux_count = 0;

      for (Map.Entry <Integer, Double> row : col.getValue().entrySet())
      {
        Double tm_v = row.getValue();
          
  	    if (tm_v != null && !Double.isInfinite(values[row.getKey()]))
        {
    	  double val = values[row.getKey()] + tm_v;
    	  
    	  if (!Double.isInfinite(val))
    	  {
    	    aux[aux_count] = val;
    	    aux_count++;
    	  }
    	}
      }

      result[col.getKey()] = sumVector(aux, aux_count);
    }

	return result;
  }

  /**
   * 
   * @param tm
   * @return
   */
  public static double[] converge(Map <Integer, Map <Integer, Double>> tm, int num_concepts)
  {
	double [] values = new double [num_concepts];

    // Initialize the probabilities. All concepts have the same probability
    for (int i = 0; i < num_concepts; i++)
    { values[i] = Math.log(1.0/num_concepts); }
    
	//System.out.println("sum: " + sumVector(values));

	for (int i = 0; i < 100; i++)
	{
	  System.out.println("Iteration: " + i);
	  double [] new_values = converge (tm, values);
	  
	  double delta = 0;
	  
	  for (int j = 0; j < values.length; j++)
	  { delta += Math.abs(Math.exp(values[i]) - Math.exp(new_values[j])); }
	  
	  System.out.println(delta);
	  
	  values = new_values;
	  
	  //System.out.println(Arrays.toString(values));
	}

	return values;
  }

  public static void main (String [] argc)
  {
	/*Map [] matrix = 
			new Map [3];

	Map <Integer, Double> row0 = new HashMap <Integer, Double> ();
	matrix[0] = row0;
	row0.put(0, Math.log(0.75));
	row0.put(1, Math.log(0.10));
	row0.put(2, Math.log(0.15));

	Map <Integer, Double> row1 = new HashMap <Integer, Double> ();
	matrix[1] = row1;
	row1.put(0, Math.log(0.10));
	row1.put(1, Math.log(0.65));
	row1.put(2, Math.log(0.25));

	Map <Integer, Double> row2 = new HashMap <Integer, Double> ();
	matrix[2] = row2;
	row2.put(0, Math.log(0.33));
	row2.put(1, Math.log(0.33));
	row2.put(2, Math.log(0.34));

	double [] result = converge(matrix, 3);

    System.out.println(Arrays.toString(result));

    System.out.println(Math.exp(result[0]) + Math.exp(result[1]) + Math.exp(result[2]));*/
  }
}
