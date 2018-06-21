package pdfreport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.datastax.driver.core.utils.UUIDs;
import com.lowagie.text.FontFactory;
import com.lowagie.text.pdf.PdfReader;

public class GeneratePDFMultiReport extends HttpServlet{

	private static final long serialVersionUID = 3849724655633902748L;


	public void init(ServletConfig config) throws ServletException{
		super.init(config);
		//rif.: https://stackoverflow.com/questions/24007492/why-doesnt-fontfactory-getfontknown-font-name-floatsize-work
		FontFactory.registerDirectories();

		//in teoria ProcessReport.init() va fatta una sola volta. Ora invece viene eseguita in 
		//pdfreport.PdfUtil.generaReportPdf(...)
		//per ogni documento generato!
		/*
        try {
        	GenericUtil.setLicenceEnvKey(this);
			ProcessReport.init();
		} catch (Exception e) {
			throw new ServletException("Errore in generaReportPdf[ProcessReport.init()][" + e.getMessage() + "]");
		}
		 */
	}

	public void doGet(HttpServletRequest request, 
			HttpServletResponse response) throws ServletException, IOException{
		doPost(request, response);
	}

	public void doPost(HttpServletRequest request, 
			HttpServletResponse response) throws ServletException, IOException{   

		int ntemplate = 0;        
		String [] templates = new String [Constants.getMaxTemplate(this.getServletContext())];
		String [] tags      = new String [Constants.getMaxTemplate(this.getServletContext())];
		String [] stfr      = new String [Constants.getMaxTemplate(this.getServletContext())];

		if (!GenericUtil.getFromRequest(request, "ntemplate").equalsIgnoreCase(""))
			ntemplate      = Integer.valueOf(request.getParameter("ntemplate"));

		String btemplate   = GenericUtil.getFromRequest(request, "btemplate");
		String data        = GenericUtil.getFromRequest(request, "data");
		String compagnia   = GenericUtil.getFromRequest(request, "compagnia");
		String userid      = GenericUtil.getFromRequest(request, "userid");
		String codagen     = GenericUtil.getFromRequest(request, "codagen");
		String idrapporto  = GenericUtil.getFromRequest(request, "idrapporto");
		String templateDir = GenericUtil.getFromRequest(request, "templateDir");

		String path_propfile   = this.getServletContext().getInitParameter("path_propfile").toString();
		String rootTemplateDir = this.getServletContext().getInitParameter("rootTemplateDir").toString();
		String spooldir        = this.getServletContext ().getInitParameter ("spooldir").toString ();
		String debug           = this.getServletContext ().getInitParameter ("debug").toString ();
		String savePDF         = this.getServletContext ().getInitParameter ("savePDF").toString ();
		String saveXMLData     = this.getServletContext ().getInitParameter ("saveXMLData").toString ();
		//String saveMatrixXMLData = this.getServletContext ().getInitParameter ("saveMatrixXMLData").toString ();        
		String secAllowAll     = this.getServletContext ().getInitParameter ("secAllowAll").toString ();
		String secAllowPrint   = this.getServletContext ().getInitParameter ("secAllowPrint").toString ();
		String pEncription     = this.getServletContext ().getInitParameter ("secEncription").toString ();

		// variabili per i font
		//posizione cartella font
		String fontDir = this.getServletContext().getRealPath("") + "\\font";
		//posizione cartella loghi
		String loghiDir = this.getServletContext().getRealPath("") + "\\loghi";       
		String fontNameMatrix = this.getServletContext ().getInitParameter ("fontNameMatrix").toString ();
		int defaultFontSizeMatrix= Integer.valueOf(this.getServletContext ().getInitParameter ("defaultFontSizeMatrix"));
		String fontNameBarcode= this.getServletContext ().getInitParameter ("fontNameBarcode").toString ();
		int defaultFontSizeBarcode= Integer.valueOf(this.getServletContext ().getInitParameter ("defaultFontSizeBarcode"));

		String secPDF = "";

		if (secAllowAll.equalsIgnoreCase("Y"))
			secPDF = "AA";
		else if (secAllowPrint.equalsIgnoreCase("Y"))
			secPDF = "AP";
		else
			secPDF = "AN";        

		for (int i = 0; i < ntemplate; i++){
			templates [i] = request.getParameter("template" + i);
			tags      [i] = request.getParameter("tag" + i);
			stfr      [i] = request.getParameter("stfr" + i);
		}

		//se i dati contengono il nodo "<loghi/>" lo sostituisco con il contenuto del file loghi.xml
		int loghiPos = data.indexOf("<loghi/>");
		if (loghiPos > -1)
			data = PdfUtil.aggiungiLoghi (data, loghiDir);

		if (debug.equalsIgnoreCase ("Y"))
			GenericUtil.debugStampaRequest(request);

		//creazione cartelle stoccaggio pdf
		String workingdirectory = GenericUtil.creaStrutturaCartelle (spooldir);

		if (compagnia != null){
			workingdirectory += "\\" + compagnia + '_' + codagen;
			GenericUtil.createFolder (workingdirectory);
		}

		//creo il nome del file che verra' generato. E' lo stesso per pdf, xml e log estensione esclusa
		String nome_file_output = workingdirectory + "\\" + new SimpleDateFormat ("yyyyMMdd_HHmmss").format (new Date());

		if (idrapporto != null && !idrapporto.equalsIgnoreCase("")) nome_file_output += "_" + idrapporto;
		if (userid != null     && !userid.equalsIgnoreCase(""))     nome_file_output += "_" + userid;
		if (compagnia != null  && !compagnia.equalsIgnoreCase(""))  nome_file_output += "_" + compagnia;
		if (codagen != null    && !codagen.equalsIgnoreCase(""))    nome_file_output += "_" + codagen;
		if (data != null       && !data.equalsIgnoreCase("")){
			nome_file_output += "_" + GenericUtil.getProdottoEcoBanFromXml (data);
			nome_file_output += "_" + GenericUtil.getNumPoliEcoBanFromXml (data);
			nome_file_output += "_" + GenericUtil.getUtenteEcoBanFromXml (data);          
		}

		// scarico xml su file
		if (saveXMLData.equalsIgnoreCase ("Y"))
			GenericUtil.salvaXmlSuFile (data, nome_file_output + ".xml");

		// inizio generazione PDF
		ServletOutputStream so = response.getOutputStream ();

		try{
			ByteArrayOutputStream baos [] = new ByteArrayOutputStream [Constants.getMaxTemplate(this.getServletContext())];

			// processo con winwards dei template 
			for (int i = 0; i < ntemplate; i++){
				//genero la stampa
				baos [i] = PdfUtil.generaReportPdf(rootTemplateDir + templateDir + "/" + templates [i],
						data.replaceFirst ("<template/>", "<template ix=\"" + i + "\" tag=\"" + tags [i] + "\"/>"),
						path_propfile,
						secPDF,
						pEncription,
						debug.equalsIgnoreCase ("Y"));
			}

			// accodamento in un unico PDF dei template processati
			ByteArrayOutputStream baos2 = PdfUtil.concatenaPdf (baos,
					rootTemplateDir + templateDir + "/" + btemplate,
					stfr,
					request,
					secPDF
					);

			PdfReader reader = null;

			if (secPDF.equalsIgnoreCase("AA"))
				reader = new PdfReader(baos2.toByteArray());
			else
				reader = new PdfReader(baos2.toByteArray(), Constants.ownerPassword.getBytes());

			// Genera codice UUID
			UUID uuidTime = UUIDs.timeBased();
			// Seleziona dall'xml solo il matrix
			String matrix = data.substring(data.indexOf("<matrix"));
			matrix = matrix.substring(0, matrix.indexOf("</matrix>") + 9);
			// Estrae il tracciato dal matrix
			String vecchioTracciato = GenericUtil.estraiVecchioTracciato(matrix);
			String pagineTotali = GenericUtil.numeroPagineTreCifre(reader.getNumberOfPages());
			// Inserisce nel DB nella tabella "GDOC_STAMPE_UUID" delle nuove componenti del matrix
			boolean isInserted = GenericUtil.salvaUuidTracciatoDb(uuidTime, vecchioTracciato, nome_file_output, pagineTotali, this);

			if (isInserted) {
				System.out.println("Inserimento dati nella tabella GDOC_STAMPE_UUID eseguito con SUCCESSO");
			}
			else {
				System.out.println("Inserimento dati nella tabella GDOC_STAMPE_UUID FALLITO");
			}

			//verifico se applicare trasformazione matrix
			if (data.indexOf ("<matrix") > 0)
				baos2 = PdfUtil.applicaMatrix (baos2, data, secPDF, pEncription, fontDir, fontNameMatrix, defaultFontSizeMatrix, reader, uuidTime, vecchioTracciato, pagineTotali);

			//verifico se applicare il barcode
			if (data.indexOf ("<barcode") > 0)
				baos2 = PdfUtil.applicaBarCode(baos2, data, secPDF, pEncription, fontDir, fontNameBarcode, defaultFontSizeBarcode);

			// restituzione risultato servlet
			response.setContentType ("application/pdf");          
			response.setContentLength (baos2.size());
			baos2.writeTo (so);

			// scarico pdf su file
			if (savePDF.equalsIgnoreCase ("Y"))
				GenericUtil.salvaPdfSuFile (baos2, nome_file_output + ".pdf");

		} 
		catch (Exception e){       
			//salvo eventuale eccezione su file e la mando al browser
			GenericUtil.salvaLogfSuFile (e.getMessage (), nome_file_output +  ".log");

			response.setContentType ("text/html; charset=windows-1252");

			String s = "<html>"; 
			s+= "<head><title>****GeneratePDFReport****</title></head>";
			s+= "<body>" + e.getMessage () + "</body>";
			s+= "</html>";        
			so.write(s.getBytes());
		}
		so.flush();
		so.close();                            
	}
}
