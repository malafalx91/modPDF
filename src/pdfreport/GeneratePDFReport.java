package pdfreport;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;

import com.datastax.driver.core.utils.UUIDs;
import com.lowagie.text.FontFactory;
import com.lowagie.text.pdf.PdfReader;

public class GeneratePDFReport extends HttpServlet {

	private static final long serialVersionUID = -6698857404794302616L;

	public void init(ServletConfig config) throws ServletException {
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

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		// variabili per generazione pdf con dati xml passati via request
		String compagnia = GenericUtil.getFromRequest(request, "compagnia");
		String userid = GenericUtil.getFromRequest(request, "userid");
		String codagen = GenericUtil.getFromRequest(request, "codagen");
		String idrapporto = GenericUtil.getFromRequest(request, "idrapporto");
		String templateDir = GenericUtil.getFromRequest(request, "templateDir");

		// variabili per generazione pdf con dati xml passati via db
		String dbId = GenericUtil.getFromRequest(request, "id");

		// variabili di configurazione della servlet
		String path_propfile = this.getServletContext().getInitParameter("path_propfile").toString();
		String rootTemplateDir = this.getServletContext().getInitParameter("rootTemplateDir").toString();
		String spooldir = this.getServletContext().getInitParameter("spooldir").toString();
		String debug = this.getServletContext().getInitParameter("debug").toString();
		String savePDF = this.getServletContext().getInitParameter("savePDF").toString();
		String saveXMLData = this.getServletContext().getInitParameter("saveXMLData").toString();
		String forceGC = this.getServletContext().getInitParameter("forceGC").toString();
		String secAllowAll = this.getServletContext().getInitParameter("secAllowAll").toString();
		String secAllowPrint = this.getServletContext().getInitParameter("secAllowPrint").toString();
		String pEncription = this.getServletContext().getInitParameter("secEncription").toString();

		// variabili per i font
		// posizione cartella font
		String fontDir = this.getServletContext().getRealPath("") + "\\font";
		// posizione cartella loghi
		String loghiDir = this.getServletContext().getRealPath("") + "\\loghi";
		String fontNameMatrix = this.getServletContext().getInitParameter("fontNameMatrix").toString();
		int defaultFontSizeMatrix = Integer.valueOf(this.getServletContext().getInitParameter("defaultFontSizeMatrix"));
		String fontNameBarcode = this.getServletContext().getInitParameter("fontNameBarcode").toString();
		int defaultFontSizeBarcode = Integer
				.valueOf(this.getServletContext().getInitParameter("defaultFontSizeBarcode"));

		String secPDF = "";

		if (secAllowAll.equalsIgnoreCase("Y"))
			secPDF = "AA";
		else if (secAllowPrint.equalsIgnoreCase("Y"))
			secPDF = "AP";
		else
			secPDF = "AN";

		// recupero template e xml da db o da request (se sid viene passato allora da
		// db)
		String template = GenericUtil.getDatiTemplate(request);
		String data = GenericUtil.getDatiXml(request, template);
		//estraggo nome template
		String nomeTemplate = GenericUtil.estrazioneNomeTemplate(template);

		// se i dati contengono il nodo errore cambio il template di riferimento
		if (data.indexOf("errmsg") > -1)
			template = "errore.rtf";

		// se i dati contengono il nodo "<loghi/>" lo sostituisco con il contenuto del
		// file loghi.xml
		int loghiPos = data.indexOf("<loghi/>");
		if (loghiPos > -1)
			data = PdfUtil.aggiungiLoghi(data, loghiDir);

		// se è attivo il debug stampo i dati della request
		if (debug.equalsIgnoreCase("Y"))
			GenericUtil.debugStampaRequest(request);

		// creazione cartelle stoccaggio pdf
		String workingdirectory = GenericUtil.creaStrutturaCartelle(spooldir);

		if (compagnia != null) {
			workingdirectory += "\\" + compagnia + '_' + codagen;
			GenericUtil.createFolder(workingdirectory);
		}

		// creo il nome del file che verrà generato. E' lo stesso per pdf, xml e log
		// estensione esclusa
		String nome_file_output = workingdirectory + "\\" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

		if (idrapporto != null && !idrapporto.equalsIgnoreCase(""))
			nome_file_output += "_" + idrapporto;
		if (userid != null && !userid.equalsIgnoreCase(""))
			nome_file_output += "_" + userid;
		if (template != null && !template.equalsIgnoreCase(""))
			nome_file_output += "_" + GenericUtil.bonificaNomeFile(template);
		if (compagnia != null && !compagnia.equalsIgnoreCase(""))
			nome_file_output += "_" + compagnia;
		if (codagen != null && !codagen.equalsIgnoreCase(""))
			nome_file_output += "_" + codagen;
		if (data != null && !data.equalsIgnoreCase(""))
			nome_file_output += GenericUtil.getNumPoliEcoFromXml(data);
		if (data != null && !data.equalsIgnoreCase(""))
			nome_file_output += GenericUtil.getNumSinFromXml(data);
		if (dbId != null && !dbId.equalsIgnoreCase(""))
			nome_file_output += "_" + "id=" + dbId;

		// scarico xml su file
		if (saveXMLData.equalsIgnoreCase("Y"))
			GenericUtil.salvaXmlSuFile(data, nome_file_output + ".xml");

		// inizio generazione PDF
		ServletOutputStream so = response.getOutputStream();

		try {
			// genero la stampa
			ByteArrayOutputStream baos = PdfUtil.generaReportPdf(rootTemplateDir + templateDir + "/" + template, data,
					path_propfile, secPDF, pEncription, debug.equalsIgnoreCase("Y"));

			// prova
			// PdfUtil.salvaPdfSuFile (baos, nome_file_output + "temp1.pdf");

			// se la provenienza non è specificata la consideriamo bozza
			if (data.toUpperCase().indexOf("<PROV>TRF</PROV>") < 0 && data.toUpperCase().indexOf("<PROV>TR</PROV>") < 0
					&& data.toUpperCase().indexOf("<PROV>PF</PROV>") < 0) {
				baos = PdfUtil.applicaWatemarkBozzaDaApprovare(baos, secPDF, pEncription, true);
			} else {
				// aggiungo il watermark "bozza da approvare" se richiesto con il finale
				if (data.toUpperCase().indexOf("<PROV>TRF</PROV>") >= 0)
					baos = PdfUtil.applicaWatemarkBozzaDaApprovare(baos, secPDF, pEncription, false);
				if (data.toUpperCase().indexOf("<PROV>TR</PROV>") >= 0)
					baos = PdfUtil.applicaWatemarkBozzaDaApprovare(baos, secPDF, pEncription, true);
			}

			PdfReader reader = null;

			if (secPDF.equalsIgnoreCase("AA"))
				reader = new PdfReader(baos.toByteArray());
			else
				reader = new PdfReader(baos.toByteArray(), Constants.ownerPassword.getBytes());

			// Genera codice UUID
			UUID uuidTime = UUIDs.timeBased();
			// Seleziona dall'xml solo il matrix
			String matrix = data.substring(data.indexOf("<matrix"));
			matrix = matrix.substring(0, matrix.indexOf("</matrix>") + 9);
			// Estrae il tracciato dal matrix
			String vecchioTracciato = GenericUtil.estraiVecchioTracciato(matrix);
			String pagineTotali = GenericUtil.numeroPagineTreCifre(reader.getNumberOfPages());
			// Inserisce nel DB nella tabella "GDOC_STAMPE_UUID" delle nuove componenti del matrix
			boolean isInserted = GenericUtil.salvaUuidTracciatoDb(uuidTime, vecchioTracciato, nomeTemplate, pagineTotali, this);

			if (isInserted) {
				System.out.println("Inserimento dati nella tabella GDOC_STAMPE_UUID eseguito con SUCCESSO");
			}
			else {
				System.out.println("Inserimento dati nella tabella GDOC_STAMPE_UUID FALLITO");
			}

			// verifico se applicare trasformazione matrix
			if (data.indexOf("<matrix") > 0)
				baos = PdfUtil.applicaMatrix(baos, data, secPDF, pEncription, fontDir, fontNameMatrix,
						defaultFontSizeMatrix, reader, uuidTime, vecchioTracciato, pagineTotali);

			// verifico se applicare il barcode
			if (data.indexOf("<barcode") > 0)
				baos = PdfUtil.applicaBarCode(baos, data, secPDF, pEncription, fontDir, fontNameBarcode,
						defaultFontSizeBarcode);

			// restituzione risultato servlet
			if (dbId == null || dbId.equalsIgnoreCase("")) {
				response.setContentType("application/pdf");
				response.setContentLength(baos.size());
				baos.writeTo(so);
			} else {
				// se sto facendo un bypass tramite db scrivo il pdf nel db e ritorno solo un messaggio di ok
				PdfUtil.savePdfDb(baos, request);
				response.setContentType("text/html; charset=windows-1252");
				String s = "+OK_0000" + dbId;
				so.write(s.getBytes());
			}

			// scarico pdf su file
			if (savePDF.equalsIgnoreCase("Y"))
				GenericUtil.salvaPdfSuFile(baos, nome_file_output + ".pdf");

			if (baos != null)
				baos = null;
		} catch (Exception e) {
			// salvo eventuale eccezione su file e la mando al browser
			GenericUtil.salvaLogfSuFile(e.getMessage(), nome_file_output + ".log");

			response.setContentType("text/html; charset=windows-1252");

			String s = "<html>";
			s += "<head><title>****GeneratePDFReport****</title></head>";
			s += "<body>" + StringEscapeUtils.escapeHtml(e.getMessage()) + "</body>";
			s += "</html>";
			so.write(s.getBytes());
		}
		so.flush();
		so.close();

		// forzo il garbage collector
		if (forceGC.equalsIgnoreCase("Y")) {
			if (!(dbId == null || dbId.equalsIgnoreCase("")))
				System.gc();
		}
	}


	/**
	 * Ritorna lo spazio libero presente sulle unità visibili a pdfgen
	 * 
	 * @return
	 */
	public static String getAvailableSpace() {
		String s = "";
		try {
			File[] roots = File.listRoots();
			for (int index = 0; index < roots.length; index++) {
				String root = roots[index].toString();
				double bytes = roots[index].getUsableSpace();
				s += root.replace(":\\", "=") + new DecimalFormat("###0.000").format(bytes / 1024 / 1024 / 1024)
						+ "GB, ";
			}
			return s;
		} catch (Exception ee) {
			return "not available";
		}
	}

	/**
	 * Utile per eseguire i test e popolare la jsp con un xml di prova in modo automatico.
	 * Es. <td width="716"><textarea rows="19" name="data" cols="80"><%=pdfreport.GeneratePDFReport.getXMLTest()%></textarea></td>
	 * nella PDFReportPage.jsp
	 */
	public static String getXMLTest() {
		String xmlFilePath = "C:\\PDFReportServer\\template_docx\\M10235663.xml";
		String xml = "";
		try {
			byte[] bytes = Files.readAllBytes(Paths.get(xmlFilePath));
			xml = new String(bytes, Charset.forName("UTF-8"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return xml;
	}


}
