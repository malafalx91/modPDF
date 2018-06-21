<html>
<head>
<title>PDFReportPage <%=pdfreport.GenericUtil.getJavaVersion()%></title>
</head>
<body style="background-color:#ffffb5;">
   <h1>PDFGEN [<%=application.getInitParameter("markup")%>]</h1>
   <a href="PDFReportPage.jsp">vai a Report</a>
   <h3>Inserire gli attributi per la generazione del documento:</h3>
   <form method="POST" name="GeneraPDF" target="_blank" action="generatePDFMultiReport">
      <input type="hidden" name="templateDir" value="">
      <table border="0" cellpadding="0" cellspacing="0" style="border-collapse: collapse" width="901" id="AutoNumber1">
      <tr>
         <td>Numero template</td>
         <td><input type="text" name="ntemplate" size="2" value="2"></td>
         <td>Blank template</td>
         <td><input type="text" name="btemplate" size="15" value="tst\blank.rtf"></td>
      </tr>
      <tr>
         <td align="center">Nome del template0</td>
         <td><input type="text" name="template0" size="32" value="template0.rtf"></td>
         <td>
            <input type="radio" name="stfr0" value="S">Si
            <input type="radio" name="stfr0" value="N" checked>No
         </td>
         <td align="center">Tag 0</td>
         <td><input type="text" name="tag0" size="32" value="tag0"></td>
      </tr>
      <tr>
         <td  align="center">Nome del template1</td>
         <td><input type="text" name="template1" size="32" value="template1.rtf"></td>
         <td>
            <input type="radio" name="stfr1" value="S">Si
            <input type="radio" name="stfr1" value="N" checked>No
         </td>
         <td  align="center">Tag 1</td>
         <td><input type="text" name="tag1" size="32" value="tag1"></td>
      </tr>
      <tr>
         <td align="center">Nome del template2</td>
         <td><input type="text" name="template2" size="32" value="template2.rtf"></td>
         <td>
            <input type="radio" name="stfr2" value="S">Si
            <input type="radio" name="stfr2" value="N" checked>No
         </td>
         <td  align="center">Tag 2</td>
         <td><input type="text" name="tag2" size="32" value="tag2"></td>
      </tr>
      <tr>
         <td align="center">Nome del template3</td>
         <td><input type="text" name="template3" size="32" value="template3.rtf"></td>
         <td>
            <input type="radio" name="stfr3" value="S">Si
            <input type="radio" name="stfr3" value="N" checked>No
         </td>
         <td align="center">Tag 3</td>
         <td><input type="text" name="tag3" size="32" value="tag3"></td>
      </tr>
      <tr>
         <td  align="center">Nome del template4</td>
         <td><input type="text" name="template4" size="32" value="template4.rtf"></td>
         <td>
            <input type="radio" name="stfr4" value="S">Si
            <input type="radio" name="stfr4" value="N" checked>No
         </td>
         <td  align="center">Tag 4</td>
         <td><input type="text" name="tag4" size="32" value="tag4"></td>
      </tr>
      <tr>
         <td  align="center">Nome del template5</td>
         <td><input type="text" name="template5" size="32" value="template5.rtf"></td>
         <td>
            <input type="radio" name="stfr5" value="S">Si
            <input type="radio" name="stfr5" value="N" checked>No
         </td>
         <td  align="center">Tag 5</td>
         <td><input type="text" name="tag5" size="32" value="tag5"></td>
      </tr>
      <tr>
         <td  align="center">Nome del template6</td>
         <td><input type="text" name="template6" size="32" value="template6.rtf"></td>
         <td>
            <input type="radio" name="stfr6" value="S">Si
            <input type="radio" name="stfr6" value="N" checked>No
         </td>
         <td  align="center">Tag 6</td>
         <td><input type="text" name="tag6" size="32" value="tag6"></td>
      </tr>
      <tr>
         <td  align="center">Nome del template7</td>
         <td><input type="text" name="template7" size="32" value="template7.rtf"></td>
         <td>
            <input type="radio" name="stfr7" value="S">Si
            <input type="radio" name="stfr7" value="N" checked>No
         </td>
         <td  align="center">Tag 7</td>
         <td><input type="text" name="tag7" size="32" value="tag7"></td>
      </tr>
      <tr>
         <td  align="center">Nome del template8</td>
         <td><input type="text" name="template8" size="32" value="template8.rtf"></td>
         <td>
            <input type="radio" name="stfr8" value="S">Si
            <input type="radio" name="stfr8" value="N" checked>No
         </td>
         <td  align="center">Tag 8</td>
         <td><input type="text" name="tag8" size="32" value="tag8"></td>
      </tr>
      <tr>
         <td  align="center">Nome del template9</td>
         <td><input type="text" name="template9" size="32" value="template9.rtf"></td>
         <td>
            <input type="radio" name="stfr9" value="S">Si
            <input type="radio" name="stfr9" value="N" checked>No
         </td>
         <td  align="center">Tag 9</td>
         <td><input type="text" name="tag9" size="32" value="tag9"></td>
      </tr>
      <tr>
         <td  align="center">Dati XML</td>
         <td colspan="10"><textarea rows="19" name="data" cols="80">&lt;xml attr="aaaa"/&gt;</textarea></td>
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