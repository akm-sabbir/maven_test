package org.wkp.kb;

import org.wkp.model.ProbabilityModel;

public abstract class AbstractKB
{
  public ProbabilityModel generateModel() { return null; };
  
  public ProbabilityModel generateModel(ProbabilityModel dmMM, boolean cooccurrences) { return null;};
}
