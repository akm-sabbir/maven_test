package org.wkp.wsd;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.wkp.model.TokenMatrix;


public class Experiments
{
  public static void main (String [] argc)
  throws IOException, ClassNotFoundException
  {
	if (argc.length != 3)
	{
	  System.err.println("Experiments [folder_name] [benchmark_file_name] [token_matrix_file_name]");
	  System.exit(-1);
	}

	//argc[0] - "C:\\intecmevi\\MSHCorpus"
	//argc[1] - "C:\\intecmevi\\MSHCorpus\\benchmark_mesh.txt"

    String folder_name = argc[0];

    Map <String, List <String>> benchmark = Benchmark.load(argc[1]);

    ObjectInputStream i = new ObjectInputStream(new GZIPInputStream(new FileInputStream(argc[2])));
    TokenMatrix tm = (TokenMatrix)i.readObject();
    i.close();    

    File folder = new File(folder_name);

    if (folder.isDirectory() && folder.listFiles() != null)
    {
      for (File file : folder.listFiles())
      {
        if (file.getName().endsWith(".arff"))
        {
          String term = file.getName().split("_")[0];

          System.out.println(term + "|" + benchmark.get(term));

          System.out.println(file.getName());
          ARFF.load(file.getAbsolutePath(), tm);
        }
      }
    }
  }
}