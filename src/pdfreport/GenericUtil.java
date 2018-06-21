package pdfreport;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.net.URLDecoder;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.text.SimpleDateFormat;

import java.util.Date;

import java.util.Enumeration;
import java.util.UUID;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import net.windward.xmlreport.ProcessPdf;

import oracle.sql.CLOB;

public class GenericUtil {

	// crea una cartella
	public static void createFolder(String folderPath) {
		File filetest = new File(folderPath);
		File file;

		if (!filetest.exists()) {
			file = new File(folderPath);
			file.mkdir();
		}
	}

	// recupera un parametro dalla request
	public static String getFromRequest(HttpServletRequest request, String parameter) {
		if (request.getParameter(parameter) == null)
			return "";

		try {
			return URLDecoder.decode(request.getParameter(parameter), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return "";
		}
	}

	// crea la struttura delle cartelle per la memorizzazione dei file pdf
	public static String creaStrutturaCartelle(String pSpoolDir) {
		SimpleDateFormat sdfd = new SimpleDateFormat("d");
		SimpleDateFormat sdfm = new SimpleDateFormat("M");
		SimpleDateFormat sdfyyyy = new SimpleDateFormat("yyyy");

		String giorno = sdfd.format(new Date());
		String mese = sdfm.format(new Date());
		String anno = sdfyyyy.format(new Date());

		String workingdirectory = pSpoolDir + "/" + anno;
		createFolder(workingdirectory);
		workingdirectory += "/" + mese;
		createFolder(workingdirectory);
		workingdirectory += "/" + giorno;
		createFolder(workingdirectory);
		return workingdirectory;
	}

	// bonifica il nome del file
	public static String bonificaNomeFile(String pFileName) {
		String newFileName = pFileName;
		newFileName = newFileName.replaceAll(".rtf", "");
		newFileName = newFileName.replaceFirst("/", "");
		newFileName = newFileName.replaceAll("/", "_");
		newFileName = newFileName.replaceAll("\\\\", "");
		return newFileName;
	}

	// estrae il numero di polizza Eco se contenuto nei dati xml
	public static String getNumPoliEcoFromXml(String pXmlData) {
		if (pXmlData.indexOf("num_poli") == -1)
			return "";

		try {
			int startidx = pXmlData.indexOf("num_poli") + 10;
			String chunk = pXmlData.substring(startidx, startidx + 128);
			int endidx = chunk.indexOf("\"");
			return "_" + chunk.substring(0, endidx);
		} catch (Exception e) {
			return "";
		}
	}

	// estrae il numero di sinistro se contenuto nei dati xml
	public static String getNumSinFromXml(String pXmlData) {
		if (pXmlData.indexOf("sin_nsin") == -1)
			return "";

		try {
			int startidx = pXmlData.indexOf("sin_nsin") + 10;
			String chunk = pXmlData.substring(startidx, startidx + 128);
			int endidx = chunk.indexOf("\"");
			return "_" + chunk.substring(0, endidx);
		} catch (Exception e) {
			return "";
		}
	}

	// estrae il numero di polizza EcoBan se contenuto nei dati xml
	public static String getNumPoliEcoBanFromXml(String pXmlData) {
		String num_ordine = "";
		String num_polizza = "";

		try {
			if (pXmlData.indexOf("num_polizza") == -1)
				return "";

			int startidx = pXmlData.indexOf("num_polizza") + 11;
			String chunk = pXmlData.substring(startidx, startidx + 128);
			int endidx = chunk.indexOf("<");
			num_polizza = chunk.substring(1, endidx);

			if (pXmlData.indexOf("num_polizza_ord") == -1)
				return num_polizza;

			startidx = pXmlData.indexOf("num_polizza_ord") + 15;
			chunk = pXmlData.substring(startidx, startidx + 128);
			endidx = chunk.indexOf("<");
			num_ordine = chunk.substring(1, endidx);
		} catch (Exception e) {
			num_ordine = "";
			num_polizza = "";
		}

		return num_polizza + num_ordine;
	}

	// estrae il nome prodotto se contenuto nei dati xml
	public static String getProdottoEcoBanFromXml(String pXmlData) {
		try {
			if (pXmlData.indexOf("prodotto") == -1)
				return "";

			int startidx = pXmlData.indexOf("prodotto") + 8;
			String chunk = pXmlData.substring(startidx, startidx + 128);
			int endidx = chunk.indexOf("<");
			return chunk.substring(1, endidx);
		} catch (Exception e) {
			return "";
		}
	}

	// estrae il nome utente se contenuto nei dati xml
	public static String getUtenteEcoBanFromXml(String pXmlData) {
		try {
			if (pXmlData.indexOf("UserId") == -1)
				return "";

			int startidx = pXmlData.indexOf("UserId") + 6;
			String chunk = pXmlData.substring(startidx, startidx + 128);
			int endidx = chunk.indexOf("<");
			return chunk.substring(1, endidx);
		} catch (Exception e) {
			return "";
		}
	}

	// stampa i dati della request
	public static void debugStampaRequest(HttpServletRequest pRequest) {
		String pName = "";
		System.out.println("****************************************************");
		System.out.println("#DEBUG DATI REQUEST-->" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()));
		int i = 0;
		Enumeration<String> enume = pRequest.getParameterNames();
		while (enume.hasMoreElements()) {
			i++;
			pName = enume.nextElement().toString();
			if (!pName.equalsIgnoreCase("data"))
				System.out.println(pName + "=" + pRequest.getParameter(pName));
			if (i > 1000)
				break;
		}
		System.out.println("****************************************************");
	}

	// stampa i valori della sicurezza pdf
	public static void debugStampaValoriSecReport(ProcessPdf pReport) {
		System.out.println("#ATTRIBUTI DI STAMPA");
		System.out.println("ALLOW_ALL=" + ProcessPdf.ALLOW_ALL);
		System.out.println("ALLOW_ASSEMBLY=" + ProcessPdf.ALLOW_ASSEMBLY);
		System.out.println("ALLOW_COPY=" + ProcessPdf.ALLOW_COPY);
		System.out.println("ALLOW_DEGRADED_PRINTING=" + ProcessPdf.ALLOW_DEGRADED_PRINTING);
		System.out.println("ALLOW_FILL_IN=" + ProcessPdf.ALLOW_FILL_IN);
		System.out.println("ALLOW_MOD_ANNOTATIONS=" + ProcessPdf.ALLOW_MOD_ANNOTATIONS);
		System.out.println("ALLOW_MOD_CONTENTS=" + ProcessPdf.ALLOW_MOD_CONTENTS);
		System.out.println("ALLOW_PRINTING=" + ProcessPdf.ALLOW_PRINTING);
		System.out.println("ALLOW_SCREEN_READERS=" + ProcessPdf.ALLOW_SCREEN_READERS);
	}

	// recupera i dati xml dalla request o dal database
	public static String getDatiXml(HttpServletRequest pRequest, String pTemplate) {
		// variabili per generazione pdf con dati xml passati via db
		String dbId = pRequest.getParameter("id");
		String dbUser = pRequest.getParameter("user");
		String dbPswd = pRequest.getParameter("password");
		String dbHost = pRequest.getParameter("host");
		String dbPort = pRequest.getParameter("port");
		String dbSid = pRequest.getParameter("sid");
		String datiXml = "";

		if (pTemplate.equalsIgnoreCase("")) {
			return "<xml><errmsg>" + "template non valido (null)" + "</errmsg><prov>PF</prov></xml>";
		}

		if (dbId == null || dbId.equalsIgnoreCase(""))
			datiXml = pRequest.getParameter("data");
		else {
			try {
				datiXml = caricaXmlDaDb(dbId, dbUser, dbPswd, dbHost, dbPort, dbSid);
			} catch (Exception e) {
				datiXml = "<xml><errmsg>" + e.getMessage() + "</errmsg><prov>PF</prov></xml>";
			}
		}
		return datiXml;
	}

	// recupera il nome del template dalla request o dal db
	public static String getDatiTemplate(HttpServletRequest pRequest) {
		String dbId = pRequest.getParameter("id");
		String dbUser = pRequest.getParameter("user");
		String dbPswd = pRequest.getParameter("password");
		String dbHost = pRequest.getParameter("host");
		String dbPort = pRequest.getParameter("port");
		String dbSid = pRequest.getParameter("sid");
		String template = "";

		if (dbId == null || dbId.equalsIgnoreCase(""))
			template = pRequest.getParameter("template");
		else {
			try {
				template = caricaTplDaDb(dbId, dbUser, dbPswd, dbHost, dbPort, dbSid);
			} catch (SQLException e) {
				template = "";
			}
		}
		return template;
	}

	// accede al db per recuperare il template
	public static String caricaTplDaDb(String pDbId, String pDbUser, String pDbPswd, String pDbHost, String pDbPort,
			String pDbSid) throws SQLException {
		Connection conn = getConnection(pDbUser, pDbPswd, pDbHost, pDbPort, pDbSid);

		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery("select template from tb_pdfgen where id = " + pDbId);
		rs.next();
		String res = rs.getString(1);
		disconnect(conn);
		return (res);
	}

	// accede al db per recuperare i dati xml
	public static String caricaXmlDaDb(String pDbId, String pDbUser, String pDbPswd, String pDbHost, String pDbPort,
			String pDbSid) throws SQLException {

		Connection conn = getConnection(pDbUser, pDbPswd, pDbHost, pDbPort, pDbSid);

		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery("select xml from tb_pdfgen where id = " + pDbId);
		rs.next();
		CLOB c = (CLOB) rs.getClob(1);
		String res = c.getSubString(1, (int) c.length());
		disconnect(conn);
		res = res.replaceFirst(">", "><MAIN>");
		return (res + "</MAIN>");
	}

	// connessione oracle
	public static Connection getConnection(String pDbUser, String pDbPswd, String pDbHost, String pDbPort,
			String pDbSid) throws SQLException {
		String logonDatabase = "jdbc:oracle:thin:@" + pDbHost + ":" + pDbPort + ":" + pDbSid;

		DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());
		Connection conn = DriverManager.getConnection(logonDatabase, pDbUser, pDbPswd);
		conn.setAutoCommit(false);
		return conn;
	}

	// disconnessione oracle
	public static void disconnect(Connection pConn) throws SQLException {
		/*
		 * ATTENZIONE: oracle di default esegue il commit dei dati durante la
		 * disconnect!
		 */
		pConn.close();
	}

	// scrive il file xml su disco
	public static void salvaXmlSuFile(String pDataXml, String pNomeFile) throws FileNotFoundException, IOException {
		FileOutputStream xmlfo = new FileOutputStream(pNomeFile);
		xmlfo.write(pDataXml.getBytes());
		xmlfo.flush();
		xmlfo.close();
	}

	// salva il pdf su disco
	public static void salvaPdfSuFile(ByteArrayOutputStream pPdf, String pNomeFile)
			throws FileNotFoundException, IOException {
		FileOutputStream fo = new FileOutputStream(pNomeFile);
		pPdf.writeTo(fo);
		fo.flush();
		fo.close();
	}

	// salva il log su disco
	public static void salvaLogfSuFile(String pLog, String pNomeFile) throws FileNotFoundException, IOException {
		String dateTime = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		FileWriter log = new FileWriter(pNomeFile);
		if (pLog != null)
			log.write(dateTime + "-->" + pLog);
		log.close();
		System.out.println("Exception=" + pLog);
	}

	public static String getJavaVersion() {
		try {
			return System.getProperty("java.version");
		} catch (Exception e) {
			return "";
		}
	}

	public static void setLicenceEnvKey(HttpServlet servlet) {
		String propertyKey = "WindwardReports.properties.filename";
		if(System.getProperty(propertyKey) == null || System.getProperty(propertyKey).isEmpty()) {
			String path_propfile   = servlet.getServletContext().getInitParameter("path_propfile").toString();
			System.setProperty(propertyKey, path_propfile);	
		}
	}

	public static Connection getConnectionLog (String url, String usr, String pwd) throws SQLException {
		String logonDatabase = url;
		DriverManager.registerDriver (new oracle.jdbc.driver.OracleDriver ());
		Connection conn = DriverManager.getConnection (logonDatabase, usr, pwd);
		conn.setAutoCommit (false);
		return conn;
	}

	public static String numeroPagineTreCifre(int numberOfPages) {
		String risultatoTreCifre = null;

		if(numberOfPages > 0 && numberOfPages < 10) {
			risultatoTreCifre = ("00" + String.valueOf(numberOfPages));
		}
		else if(numberOfPages > 9 && numberOfPages< 100){
			risultatoTreCifre = ("0" + String.valueOf(numberOfPages));
		}
		else if(numberOfPages>99 && numberOfPages <1000) {
			risultatoTreCifre = String.valueOf(numberOfPages);
		}
		else {
			risultatoTreCifre = "ERR";
		}

		return risultatoTreCifre;
	}
	//Estrae il tracciato dal matrix
	public static String estraiVecchioTracciato(String matrix) {
		return matrix.substring(matrix.indexOf(">") + 1, matrix.indexOf("</matrix>"));
	}
	//Inserimento nel DB nella tabella "GDOC_STAMPE_UUID" delle nuove componenti del matrix
	public static boolean salvaUuidTracciatoDb(UUID uuidTime, String vecchioTracciato, String pdfName, String pagineTotali, HttpServlet servlet) {

		String dbUrl = servlet.getServletContext ().getInitParameter ("dbUrl").toString ();	
		String dbUsr = servlet.getServletContext ().getInitParameter ("dbUsr").toString ();	
		String dbPwd = servlet.getServletContext ().getInitParameter ("dbPwd").toString ();
		Connection conn = null;
		PreparedStatement statement = null;
		try {
			conn = GenericUtil.getConnectionLog(dbUrl, dbUsr, dbPwd);

			String sqlInsert = "INSERT INTO GDOC_STAMPE_UUID (UUID, TRACCIATO, PDF_NAME, PAGINE, DATA_INS) VALUES (?, ?, ?, ?, sysdate)";
			statement =conn.prepareStatement(sqlInsert);
			statement.setString(1, uuidTime.toString());
			statement.setString(2, vecchioTracciato);
			statement.setString(3, pdfName);
			statement.setInt(4, Integer.valueOf(pagineTotali));

			statement.executeQuery();
			return true;
		} catch (SQLException e) {			
			e.printStackTrace();
			return false;
		} finally {
			try {
				if(statement != null) statement.close();
				if (conn != null) conn.close();
			} catch (SQLException e) {				
				e.printStackTrace();
			}
		}

	}
	//Estrae il nome del template
	public static String estrazioneNomeTemplate(String template) {
		return template.substring(template.indexOf("\\") + 1, template.indexOf("."));
	}

}

