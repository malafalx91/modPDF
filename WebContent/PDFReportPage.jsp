<html>
<head>
   <title>PDFReportPage <%=pdfreport.GenericUtil.getJavaVersion()%></title>
</head>
<body style="background-color:#ffffb5;">
   <h1>PDFGEN [<%=application.getInitParameter("markup")%>]</h1>
   <a href="PDFMultiReportPage.jsp">vai a Multi Report</a>
   <h3>Inserire gli attributi per la generazione del documento:</h3>
   <form method="POST" name="GeneraPDF" target="_blank" action="generatepdfreport">
      
      <input type="hidden" name="templateDir" value="">
      <table border="0" cellpadding="0" cellspacing="0" style="border-collapse: collapse" width="901" id="AutoNumber1">
      <tr>
         <td>Id template</td>
         <td><input type="text" name="id" value=""></td>
      </tr>
      <tr>
         <td>User</td>
         <td><input type="text" name="user" value=""></td>     
      </tr>
      <tr>
         <td>Password</td>
         <td><input type="password" name="password" value=""></td>    
      </tr>
      <tr>
         <td>host</td>
         <td><input type="text" name="host" value=""></td>   
      </tr>
      <tr>
         <td>port</td>
         <td><input type="text" name="port" value=""></td>      
      </tr>
      <tr>
         <td>sid</td>
         <td><input type="text" name="sid" value=""></td>  
      </tr>
      <tr>
         <td width="182" align="center">Nome del template</td>
         <!-- <td width="716"><input type="text" name="template" size="32" value="adpersonam.docx"></td> -->
         <td width="716"><input type="text" name="template" size="32" value=""></td>
      </tr>
      <tr>
         <td width="182" align="center">Dati XML</td>
         <td width="716"><textarea rows="19" name="data" cols="80"></textarea></td>
      </tr>
      </table>
      <p>
      <input type="submit" name="Submit" value="Genera PDF">
      <input type="reset" value="Reset"> </p>
   </form>
   <span style="font-family:verdana;font-size:12px">Versione 2.4.2 del 02/04/2015></span><br>
   <span style="font-family:verdana;font-size:8px">[<%=pdfreport.GeneratePDFReport.getAvailableSpace()%> GB]</span>   
</body>
</html>