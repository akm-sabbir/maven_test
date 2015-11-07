package org.wkp.model;

import java.io.Serializable;

public class ProbabilityModel implements Serializable
{
  /**
	 * 
	 */
  private static final long serialVersionUID = 1L;

  private TokenMatrix tm = null;
  private ConceptMatrix cm = null;
  private double [] p_c = null;
  
  public void setTokenMatrix (TokenMatrix tm)
  { this.tm = tm; }
  
  public TokenMatrix getTokenMatrix ()
  { return tm; }

  public void setConceptMatrix (ConceptMatrix cm)
  { this.cm = cm; }
  
  public ConceptMatrix getConceptMatrix ()
  { return cm; }
  
  public void setPC (double [] p_c)
  { this.p_c = p_c; }
  
  public double [] getPC ()
  { return p_c; }

}
