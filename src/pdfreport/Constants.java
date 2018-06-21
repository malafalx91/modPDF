package pdfreport;

import javax.servlet.ServletContext;

public class Constants
{
   public static final String ownerPassword="itas99";
   private static final int defaultMaxTemplate=30;
   public static int getMaxTemplate(ServletContext context){
	   int maxTemplate = Constants.defaultMaxTemplate;
	   try{
		   maxTemplate = Integer.parseInt(context.getInitParameter("max_template"));
	   }catch (Exception ex){
		   maxTemplate = Constants.defaultMaxTemplate;
	   }
	   return maxTemplate;
   }
   
   // Primo carattere alfabetico dell'header necessario a Doc per riconoscere un Datamatrix generato con il nuovo processo di stampa;
   public static final String charRiconoscimento = "N";
   // Prefisso identificativo di 3 cifre posto all'inizio e alla fine dell'UUID che viene mostrato nel pDF generato
   public static final String prefissoUUID = "**#";
   // Suffisso identificativo di 3 cifre dell'UUID
   public static final String suffissoUUID = "#**";
}
