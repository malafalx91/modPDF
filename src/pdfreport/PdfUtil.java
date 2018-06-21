package pdfreport;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.StringTokenizer;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.idautomation.fontencoder.datamatrix.DataMatrixEncoder;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfCopy;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import com.lowagie.text.pdf.interfaces.PdfEncryptionSettings;

import itas.xml.NodeUtil;
import net.windward.datasource.dom4j.Dom4jDataSource;
import net.windward.format.TemplateParseException;
import net.windward.tags.TagException;
import net.windward.xmlreport.AlreadyProcessedException;
import net.windward.xmlreport.ProcessPdf;
import net.windward.xmlreport.ProcessReport;
import oracle.sql.BLOB;

public class PdfUtil {

	// genera la matrice per la lettura delle informazioni tramite scanner.
	public static ByteArrayOutputStream applicaMatrix(ByteArrayOutputStream pPdf, String pXmlData, String pSecPDF,
			String pEncription, String pFontDir, String pFontName, int pDefaultFontSize, PdfReader reader, UUID uuidTime, String vecchioTracciato, String pagineTotali) throws IOException,
	DocumentException, ParserConfigurationException, SAXException, ParserConfigurationException, Exception {

		BaseFont matrixFont = BaseFont.createFont(pFontDir + "/" + pFontName, BaseFont.CP1252, BaseFont.EMBEDDED);

		Font verdanaFont = FontFactory.getFont("Verdana", 10, Font.BOLD);

		String matrix = pXmlData.substring(pXmlData.indexOf("<matrix"));
		matrix = matrix.substring(0, matrix.indexOf("</matrix>") + 9);

		DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		org.w3c.dom.Document doc = db.parse(new ByteArrayInputStream(matrix.getBytes()));
		Node nodeMatrix = doc.getFirstChild();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PdfStamper stamp = new PdfStamper(reader, out);
		stamp.setFullCompression(); // ***
		// cripto il documento se necessario
		applicaSicurezza(stamp, pSecPDF, pEncription);


		int customMatrixSize = pDefaultFontSize;
		int customFooterSize = 4;
		float x = 0;
		float y = 0;
		float x1 = 0;
		float y1 = 0;
		boolean writeOnlyFirstPage = false;
		//boolean writeNumberPages = false;


		try {
			x = Float.parseFloat(NodeUtil.getAttributeValue(nodeMatrix, "x"));
		} catch (Exception e) {
			x = 0;
		}

		try {
			y = Float.parseFloat(NodeUtil.getAttributeValue(nodeMatrix, "y"));
		} catch (Exception e) {
			y = 0;
		}

		try {
			x1 = Float.parseFloat(NodeUtil.getAttributeValue(nodeMatrix, "x1"));
		} catch (Exception e) {
			x1 = 0;
		}

		try {
			y1 = Float.parseFloat(NodeUtil.getAttributeValue(nodeMatrix, "y1"));
		} catch (Exception e) {
			y1 = 0;
		}

		try {
			customMatrixSize = Integer.parseInt(NodeUtil.getAttributeValue(nodeMatrix, "size"));
		} catch (Exception e) {
			customMatrixSize = pDefaultFontSize;
		}

		try {
			customFooterSize = Integer.parseInt(NodeUtil.getAttributeValue(nodeMatrix, "sizeFooter"));
		} catch (Exception e) {
			customFooterSize = 4;
		}

		try {
			writeOnlyFirstPage = NodeUtil.getAttributeValue(nodeMatrix, "write").toString().equalsIgnoreCase("first");
		} catch (Exception e) {
			writeOnlyFirstPage = false;
		}

		/*
		try {
			writeNumberPages = !NodeUtil.getAttributeValue(nodeMatrix, "numpage").toString().equalsIgnoreCase("false");
		} catch (Exception e) {
			writeNumberPages = true;
		}
		 */

		/*
		 NumberFormat formatter = new DecimalFormat("000");
		 try {
			alternateText = NodeUtil.getAttributeValue(nodeMatrix, "text").toString();
		} catch (Exception e) {
			alternateText = "";
		}
		 */

		String alternateText = Constants.prefissoUUID + uuidTime.toString() + Constants.suffissoUUID;
		String hashCodeTracciato = Integer.toString(vecchioTracciato.hashCode());

		int i = 0;
		while (i < reader.getNumberOfPages()) {
			float altezza = y;
			i++;

			PdfContentByte over = stamp.getOverContent(i);
			over.beginText();
			over.setFontAndSize(matrixFont, customMatrixSize);
			over.setColorFill(Color.black);

			/*String testo = "";
			if (writeNumberPages)
				testo = formatter.format(i) + formatter.format(reader.getNumberOfPages()) + nodeMatrix.getTextContent();
			else
				testo = nodeMatrix.getTextContent();
			 */

			String pagineParziali = GenericUtil.numeroPagineTreCifre(i);
			//genera nuovo header
			String header = generaHeader(Constants.charRiconoscimento, uuidTime, pagineTotali, pagineParziali);

			//genera il tracciato finale che andrà inserito nel nuovo matrix
			String nuovoTracciato = generaNuovoTracciato(header, vecchioTracciato, hashCodeTracciato);

			StringTokenizer tokenizer = new StringTokenizer(PdfUtil.matrixEcoder(nuovoTracciato), "\n\r");
			while (tokenizer.hasMoreTokens()) {
				over.setTextMatrix(x, altezza);
				over.showText(tokenizer.nextToken());
				float p = (float) (3.9 * customMatrixSize / 4);
				altezza = altezza - p;
			}

			over.setFontAndSize(verdanaFont.getBaseFont(), customFooterSize);
			over.setColorFill(Color.black);
			over.setTextMatrix(x1, y1);

			// se il testo alternativo è diverso da null stampo quello altrimenti uso il
			// contenuto del nodo matrix
			if (!alternateText.equalsIgnoreCase(""))
				over.showText(alternateText);
			else
				over.showText(nuovoTracciato);

			over.endText();

			if (writeOnlyFirstPage)
				break;
		}
		stamp.close();
		return out;
	}

	private static String generaHeader(String charRiconoscimento, UUID uuidTime, String pagineTotali, String pagineParziali) {
		return charRiconoscimento + uuidTime + pagineTotali + pagineParziali;		
	}

	private static String generaNuovoTracciato(String header, String vecchioTracciato, String hashCodeTracciato) {
		return header + vecchioTracciato + hashCodeTracciato;
	}


	// genera la matrice per la lettura delle informazioni tramite scanner.
	public static ByteArrayOutputStream applicaBarCode(ByteArrayOutputStream pPdf, String pXmlData, String pSecPDF,
			String pEncription, String pFontDir, String pFontName, int pDefaultFontSize) throws IOException,
	DocumentException, ParserConfigurationException, SAXException, ParserConfigurationException, Exception {

		NumberFormat formatter = new DecimalFormat("000");
		BaseFont matrixFont = BaseFont.createFont(pFontDir + "/" + pFontName, BaseFont.CP1252, BaseFont.EMBEDDED);
		Font verdanaFont = FontFactory.getFont("Verdana", 10, Font.BOLD);

		String matrix = pXmlData.substring(pXmlData.indexOf("<barcode"));
		matrix = matrix.substring(0, matrix.indexOf("</barcode>") + 10);

		DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		org.w3c.dom.Document doc = db.parse(new ByteArrayInputStream(matrix.getBytes()));
		Node nodeMatrix = doc.getFirstChild();

		PdfReader reader = null;

		if (pSecPDF.equalsIgnoreCase("AA"))
			reader = new PdfReader(pPdf.toByteArray());
		else
			reader = new PdfReader(pPdf.toByteArray(), Constants.ownerPassword.getBytes());

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PdfStamper stamp = new PdfStamper(reader, out);
		stamp.setFullCompression(); // ***
		// cripto il documento se necessario
		applicaSicurezza(stamp, pSecPDF, pEncription);

		int i = 0;
		int customMatrixSize = pDefaultFontSize;
		int customFooterSize = 4;
		float x = 0;
		float y = 0;
		float x1 = 0;
		float y1 = 0;
		boolean writeOnlyFirstPage = false;
		boolean writeNumberPages = false;
		String alternateText = "";

		try {
			x = Float.parseFloat(NodeUtil.getAttributeValue(nodeMatrix, "x"));
		} catch (Exception e) {
			x = 0;
		}

		try {
			y = Float.parseFloat(NodeUtil.getAttributeValue(nodeMatrix, "y"));
		} catch (Exception e) {
			y = 0;
		}

		try {
			x1 = Float.parseFloat(NodeUtil.getAttributeValue(nodeMatrix, "x1"));
		} catch (Exception e) {
			x1 = 0;
		}

		try {
			y1 = Float.parseFloat(NodeUtil.getAttributeValue(nodeMatrix, "y1"));
		} catch (Exception e) {
			y1 = 0;
		}

		try {
			customMatrixSize = Integer.parseInt(NodeUtil.getAttributeValue(nodeMatrix, "size"));
		} catch (Exception e) {
			customMatrixSize = pDefaultFontSize;
		}

		try {
			customFooterSize = Integer.parseInt(NodeUtil.getAttributeValue(nodeMatrix, "sizeFooter"));
		} catch (Exception e) {
			customFooterSize = 4;
		}

		try {
			writeOnlyFirstPage = NodeUtil.getAttributeValue(nodeMatrix, "write").toString().equalsIgnoreCase("first");
		} catch (Exception e) {
			writeOnlyFirstPage = false;
		}

		try {
			writeNumberPages = !NodeUtil.getAttributeValue(nodeMatrix, "numpage").toString().equalsIgnoreCase("false");
		} catch (Exception e) {
			writeNumberPages = true;
		}

		try {
			alternateText = NodeUtil.getAttributeValue(nodeMatrix, "text").toString();
		} catch (Exception e) {
			alternateText = "";
		}

		while (i < reader.getNumberOfPages()) {
			i++;

			PdfContentByte over = stamp.getOverContent(i);
			over.beginText();
			over.setFontAndSize(matrixFont, customMatrixSize);
			over.setColorFill(Color.black);
			String testo = "";

			if (writeNumberPages)
				testo = formatter.format(i) + formatter.format(reader.getNumberOfPages()) + nodeMatrix.getTextContent();
			else
				testo = nodeMatrix.getTextContent();

			over.setTextMatrix(x, y);
			over.showText(testo);

			over.setFontAndSize(verdanaFont.getBaseFont(), customFooterSize);
			over.setColorFill(Color.black);
			over.setTextMatrix(x1, y1);

			// se il testo alternativo è diverso da null stampo quello altrimenti uso il
			// contenuto del nodo matrix
			if (!alternateText.equalsIgnoreCase(""))
				over.showText(alternateText);
			else
				over.showText(testo);

			over.endText();

			if (writeOnlyFirstPage)
				break;

		}
		stamp.close();
		return out;
	}

	// applica il watemark della bozza ad un pdf
	public static ByteArrayOutputStream applicaWatemarkBozzaDaApprovare(ByteArrayOutputStream pPdf, String pSecPDF,
			String pEncription, boolean EliminaUltimaPagina) throws IOException, DocumentException, Exception {
		// Watermark
		PdfReader reader = null;

		// leggo il documento in ingresso
		if (pSecPDF.equalsIgnoreCase("AA"))
			reader = new PdfReader(pPdf.toByteArray());
		else
			reader = new PdfReader(pPdf.toByteArray(), Constants.ownerPassword.getBytes());

		int n = reader.getNumberOfPages();

		// creo uno stamper su cui andrò a scrivere il watemark
		ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
		PdfStamper stamp = new PdfStamper(reader, baos1);
		stamp.setFullCompression();// ***
		// cripto il documento se necessario
		applicaSicurezza(stamp, pSecPDF, pEncription);

		int i = 0;
		PdfContentByte under;
		BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.EMBEDDED);

		// scrivo "bozza da approvare" su tutte le pagine"
		while (i < n) {
			i++;
			under = stamp.getUnderContent(i);
			under.beginText();
			under.setTextMatrix(30, 30);
			under.setFontAndSize(bf, 30);
			under.setColorFill(Color.LIGHT_GRAY);
			under.showTextAligned(Element.ALIGN_LEFT,
					"--- --- --- --- --- ---  BOZZA  DA  APPROVARE  --- --- --- --- --- ---", 27, 52, 50);
			under.endText();
		}
		stamp.close();

		// copia finale (no ultima pagina se il documento ha più di una pagina)
		if (n > 1 && EliminaUltimaPagina)
			return duplicaPdf(baos1.toByteArray(), 1, -1, pSecPDF, pEncription);
		else
			return baos1;
	}

	// scrive il pdf nel db quando i dati sono recuperati dal db
	public static void savePdfDb(ByteArrayOutputStream pBaos, HttpServletRequest pRequest)
			throws SQLException, IOException {
		String dbId = pRequest.getParameter("id");
		String dbUser = pRequest.getParameter("user");
		String dbPswd = pRequest.getParameter("password");
		String dbHost = pRequest.getParameter("host");
		String dbPort = pRequest.getParameter("port");
		String dbSid = pRequest.getParameter("sid");

		BLOB pdfBlob;

		Connection conn = GenericUtil.getConnection(dbUser, dbPswd, dbHost, dbPort, dbSid);

		Statement stmt = conn.createStatement();
		stmt.execute("update tb_pdfgen t set pdf = empty_blob () where t.id = " + dbId);

		stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery("select pdf from tb_pdfgen t where t.id = " + dbId + " for update");
		rs.next();
		pdfBlob = (BLOB) rs.getBlob(1);

		OutputStream os = pdfBlob.getBinaryOutputStream(1);
		byte[] ba = pBaos.toByteArray();
		os.write(ba, 0, ba.length);
		os.flush();
		os.close();
		conn.commit();
		GenericUtil.disconnect(conn);
	}

	// genera un nuovo report
	public static ByteArrayOutputStream generaReportPdf(String pRtfFilePath, String pXmlData,
			String pPropertiesFilePath, String pSecPDF, String pEncription, boolean pDebug)
					throws FileNotFoundException, IOException, TemplateParseException, TagException, AlreadyProcessedException,
					Exception {

		FileInputStream templateFis = new FileInputStream(pRtfFilePath);
		ByteArrayInputStream xmlBais = new ByteArrayInputStream(pXmlData.getBytes());
		ByteArrayOutputStream pdfOutputBaos = new ByteArrayOutputStream(1024 * 1024 * 2);

		//TODO:
		// generally called once when your app starts
		// -> move it into GeneratePDFReport and GeneratePDFMultiReport (init method)

		// generazione pdf da template *.rtf e file dati *.xml
		System.setProperty("WindwardReports.properties.filename", pPropertiesFilePath);
		try {
			ProcessReport.init();
		} catch (Exception e) {
			templateFis.close();
			throw new Exception("Errore in generaReportPdf[ProcessReport.init()][" + e.getMessage() + "]");
		}

		ProcessPdf proc = new ProcessPdf(templateFis, pdfOutputBaos);
		//proc.setFontLevel(ProcessPdf.FONT_INTERNAL); //da verdana bold del template diventa helvetica bold nel pdf
		//proc.setFontLevel(ProcessPdf.FONT_EMBEDED); //da verdana bold del template diventa verdana nel pdf (non bold)

		if (pSecPDF.equalsIgnoreCase("AA")) {
			proc.setSecurity(ProcessPdf.ALLOW_ALL);
		} else if (pSecPDF.equalsIgnoreCase("AP")) {
			proc.setSecurity(ProcessPdf.ALLOW_PRINTING);
			proc.setOwnerPassword(Constants.ownerPassword);
		} else {
			proc.setSecurity(ProcessPdf.ALLOW_SCREEN_READERS);
			proc.setOwnerPassword(Constants.ownerPassword);
		}

		proc.processSetup();
		proc.processData(new Dom4jDataSource(xmlBais), "");
		proc.processComplete();

		/* era così:
		ProcessPdf report = new ProcessPdf(ba, rtf, baos);

		if (pSecPDF.equalsIgnoreCase("AA")) {
			report.setSecurity(ProcessPdf.ALLOW_ALL);
		} else if (pSecPDF.equalsIgnoreCase("AP")) {
			report.setSecurity(ProcessPdf.ALLOW_PRINTING);
			report.setOwnerPassword(Constants.ownerPassword);
		} else {
			report.setSecurity(ProcessPdf.ALLOW_SCREEN_READERS);
			report.setOwnerPassword(Constants.ownerPassword);
		}
		report.process();

		 */

		templateFis.close(); // 20091204 GD
		xmlBais.close();

		// duplicare la stampa e applicare la sicurezza (operazione necessaria per
		// applicare la sicurezza)
		return duplicaPdf(pdfOutputBaos.toByteArray(), 1, 1000, pSecPDF, pEncription);
	}

	public static ByteArrayOutputStream duplicaPdf(byte[] sourcePdf, int da, int a, String pSecPDF, String pEncription)
			throws IOException, DocumentException, Exception {
		PdfReader reader = null;
		ByteArrayOutputStream outPdf = new ByteArrayOutputStream();

		if (pSecPDF.equalsIgnoreCase("AA"))
			reader = new PdfReader(sourcePdf);
		else
			reader = new PdfReader(sourcePdf, Constants.ownerPassword.getBytes());

		Document document1 = new Document(reader.getPageSize(1));



		PdfCopy copy = new PdfCopy(document1, outPdf);
		copy.setFullCompression();
		// cripto il documento se necessario
		applicaSicurezza(copy, pSecPDF, pEncription);

		document1.open();

		if (a < 0)
			a = reader.getNumberOfPages() + a;

		// copio le pagine da ... a ...
		for (int j = da; (j <= a && j <= reader.getNumberOfPages()); j++)
			copy.addPage(copy.getImportedPage(reader, j));

		document1.close();
		return outPdf;
	}

	/**
	 * Cripta e setta le proprietà di sicurezza del documento in base al parametro
	 * "pSecPDF"
	 * 
	 * @param doc
	 * @param pSecPDF
	 * @throws DocumentException
	 */
	private static void applicaSicurezza(PdfEncryptionSettings doc, String pSecPDF, String secEncription)
			throws DocumentException, Exception {
		// se viene permesso tutto il documento non viene mai criptato
		if (pSecPDF.equalsIgnoreCase("AA"))
			return;

		int encription;

		if (secEncription.equalsIgnoreCase("M"))
			encription = PdfCopy.STANDARD_ENCRYPTION_40;
		else if (secEncription.equalsIgnoreCase("F"))
			encription = PdfCopy.STANDARD_ENCRYPTION_128;
		else if (secEncription.equalsIgnoreCase("E"))
			encription = PdfCopy.ENCRYPTION_AES_128;
		else
			throw new Exception("Valore parametro \"secEncription\" non supportato!");

		if (pSecPDF.equalsIgnoreCase("AP")) // stampa permessa
			doc.setEncryption(null, Constants.ownerPassword.getBytes(), PdfCopy.ALLOW_PRINTING, encription);
		else if (pSecPDF.equalsIgnoreCase("AN")) // solo visualizzazione
			doc.setEncryption(null, Constants.ownerPassword.getBytes(), PdfCopy.ALLOW_SCREENREADERS, encription);
		else
			throw new Exception("Valore parametro \"pSecPDF\" non supportato!");

	}

	// ritorna un unico documento pdf a partire da più pdf
	public static ByteArrayOutputStream concatenaPdf(ByteArrayOutputStream pBaos[], String pRtfFilePath, String[] pStfr,
			HttpServletRequest pRequest, String pSecPDF) throws DocumentException, FileNotFoundException, IOException,
	TemplateParseException, TagException, AlreadyProcessedException,

	Exception {
		PdfReader reader = null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		if (pSecPDF.equalsIgnoreCase("AA"))
			reader = new PdfReader(pBaos[0].toByteArray());
		else
			reader = new PdfReader(pBaos[0].toByteArray(), Constants.ownerPassword.getBytes());

		Document document1 = new Document(reader.getPageSize(1));

		PdfCopy copy = new PdfCopy(document1, baos);

		if (pSecPDF.equalsIgnoreCase("AA")) {
			// se permesso tutto non impostare alcun livello di cripazione in pdfcopy
		}
		/*
		 * copy.setEncryption (null, null, PdfCopy.ALLOW_ASSEMBLY | PdfCopy.ALLOW_COPY |
		 * PdfCopy.ALLOW_DEGRADED_PRINTING | PdfCopy.ALLOW_FILL_IN|
		 * PdfCopy.ALLOW_MODIFY_ANNOTATIONS| PdfCopy.ALLOW_MODIFY_CONTENTS|
		 * PdfCopy.ALLOW_PRINTING| PdfCopy.ALLOW_SCREENREADERS ,
		 * PdfCopy.DO_NOT_ENCRYPT_METADATA);
		 */
		else if (pSecPDF.equalsIgnoreCase("AP")) {
			copy.setEncryption(null, Constants.ownerPassword.getBytes(), PdfCopy.ALLOW_PRINTING,
					PdfCopy.ENCRYPTION_AES_128);
		} else {
			copy.setEncryption(null, Constants.ownerPassword.getBytes(), PdfCopy.ALLOW_SCREENREADERS,
					PdfCopy.ENCRYPTION_AES_128);
		}

		copy.setFullCompression();
		document1.open();

		int npage = 0;
		ByteArrayOutputStream baosBlk = new ByteArrayOutputStream();
		FileInputStream rtfBlank = new FileInputStream(pRtfFilePath);

		// preparazione della pagina di blank per stampa fronte retro
		ProcessPdf reportBlank;
		ByteArrayInputStream baBlank = new ByteArrayInputStream("<xml/>".getBytes());

		try {
			reportBlank = new ProcessPdf(baBlank, rtfBlank, baosBlk);
		} catch (Exception e) {
			throw new Exception("Errore in concatenaPdf[reportBlank = new ProcessPdf (baBlank, rtfBlank, baosBlk)]["
					+ e.getMessage() + "]");
		}

		reportBlank.process();

		int ntemplate = Integer.valueOf(pRequest.getParameter("ntemplate"));

		for (int i = 0; i < ntemplate; i++) {
			PdfReader reader1 = null;

			if (pSecPDF.equalsIgnoreCase("AA"))
				reader1 = new PdfReader(pBaos[i].toByteArray());
			else
				reader1 = new PdfReader(pBaos[i].toByteArray(), Constants.ownerPassword.getBytes());

			int m = reader1.getNumberOfPages();
			for (int j = 1; j <= m; j++) {
				copy.addPage(copy.getImportedPage(reader1, j));
				npage++;
			}

			if (pStfr[i].equalsIgnoreCase("S")) {
				if ((npage % 2) != 0) {
					PdfReader pdfReaderBlank = new PdfReader(baosBlk.toByteArray());
					copy.addPage(copy.getImportedPage(pdfReaderBlank, 1));
					npage = 0;
				}
			}
		}

		document1.close();
		return baos;
	}

	public static String matrixEcoder(String dataToEncode) {
		return new DataMatrixEncoder().fontEncode(dataToEncode);
	}

	public static String aggiungiLoghi(String pData, String pLoghiDir) {
		String s = null;
		String t = "";
		try {
			BufferedReader br = new BufferedReader(new FileReader(pLoghiDir + "\\loghi.xml"));
			while ((s = br.readLine()) != null)
				t += s;
			br.close();
		} catch (Exception e) {
			return e.getMessage();
		}

		return pData.replaceFirst("<loghi/>", t);
	}

	/*
	 * public static ByteArrayOutputStream scriviTesto (String pTesto, float pX,
	 * float pY, ByteArrayOutputStream pPdf, String pSecPDF) throws IOException,
	 * DocumentException {
	 * 
	 * //System.out.println(pTesto); PdfReader reader = null;
	 * 
	 * //PdfReader reader = new PdfReader (pPdf.toByteArray()); if
	 * (pSecPDF.equalsIgnoreCase("AA")) reader = new PdfReader (pPdf.toByteArray());
	 * else reader = new PdfReader (pPdf.toByteArray(),
	 * Constants.ownerPassword.getBytes());
	 * 
	 * int n = reader.getNumberOfPages();
	 * 
	 * ByteArrayOutputStream baos1 = new ByteArrayOutputStream (); PdfStamper stamp
	 * = new PdfStamper (reader, baos1);
	 * 
	 * int i = 0; PdfContentByte under; PdfContentByte over;
	 * 
	 * //BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI,
	 * BaseFont.EMBEDDED); BaseFont bf =
	 * BaseFont.createFont("c:/IDAutomationDMatrix.ttf", BaseFont.CP1252,
	 * BaseFont.EMBEDDED); under = stamp.getOverContent(1); under.beginText();
	 * under.setFontAndSize (bf, 4); under.setColorFill (Color.black);
	 * 
	 * StringTokenizer tokenizer = new StringTokenizer (pTesto, "\n"); while
	 * (tokenizer.hasMoreTokens()) { under.setTextMatrix (pX, pY); under.showText
	 * (tokenizer.nextToken()); pY = pY - Float.parseFloat("3.9"); }
	 * 
	 * under.endText();
	 * 
	 * stamp.close ();
	 * 
	 * return baos1; }
	 */
}
