package it.bologna.ausl.babelmanager.services;

import static com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type.Int;
import it.bologna.ausl.babelmanager.exceptions.NotAuthorizedException;
import it.bologna.ausl.babelmanager.utils.Attivita;
import it.bologna.ausl.babelmanager.utils.ListAttivita;
import it.bologna.ausl.babelmanager.utils.UtilityFunctions;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

/**
 *
 * @author Guido Zoli
 */
public class GetAttivita extends HttpServlet {

private static final Logger log = Logger.getLogger(GetAttivita.class);

// 
private final String SEARCH_MULT = "search_mult";

    /**
     * Processa la richiesta GET (e POST) restituendo la lista di attivita presenti nel DB secondo i  parametri indicati
     * @param request servlet request
     * @param response servlet response
     * @param action
     * @throws ServletException if a servlet-specific error occurs
     * @throws java.io.UnsupportedEncodingException
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, UnsupportedEncodingException, IOException, ParseException {
    request.setCharacterEncoding("utf-8");    
//     configuro il logger per la console
//    BasicConfigurator.configure();
    log.info("--------------------------");
    log.info("Avvio servlet: " + getClass().getSimpleName());
    log.info("--------------------------");
    Timestamp current_date = new Timestamp(System.currentTimeMillis());
    Connection dbConn = null;
    PreparedStatement ps = null;
    ListAttivita listAttivita = null;
    
    // parametri obbligatori che devono essere passati
    String idapplicazione = null;
    String tokenapplicazione = null;
    
    // parametri opzionali
    String actiontype = null; // mi dice in base a quale parametro devo effetturare la ricerca
    
    String idesterno = null;
    String idattivita = null;
    String gruppo = null;
    String idsorgente = null;
    String idriferimento = null;
    
    // parametri eventualmente da aggiungere a quelli sopra per raffinare la ricerca
    String idutente = null; 
    // range di date
    String datada = null; 
    String dataa = null;


    String sqlText;
    int indexQuery;
        try {
            /*
            // leggo i dati di autenticazione dal file di configurazione
            String servUsername = config.getProperty("servUsername");
            String servPassword = config.getProperty("servPassword");

            // controllo se i dati di autenticazione della richiesta sono corretti
            String auth = request.getHeader("Authorization");
            if (!allowedUser(auth, servUsername, servPassword)) {
                // Utente non autorizzato
                response.setHeader("WWW-Authenticate", "BASIC realm=\"Update SV Servlet\"");
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            */

            // leggo le proprietà dal web.xml           
            String babelAttivitaTable = getServletContext().getInitParameter("pendingActivityTableName");

            if(babelAttivitaTable == null || babelAttivitaTable.equals("")) {
                String message = "Manca il nome della tabella da cui estrarre le attività. Indicare il nome della tabella all'interno del \"web.xml\"";
//                System.out.println(new java.util.Date(System.currentTimeMillis()).toString() + " : " + message);
                log.error(message);
                throw new ServletException(message);
            }          
            

            // leggo i parametri per l'aggiornamento della scrivania virtuale dalla richiesta HTTP

            // dati per l'autenticazione
            idapplicazione = request.getParameter("idapplicazione");
            tokenapplicazione = request.getParameter("tokenapplicazione");

            // dati per l'esecuzione della query
            actiontype = request.getParameter("actiontype");
            
            idesterno = request.getParameter("idesterno");          
            idattivita = request.getParameter("idattivita");
            gruppo = request.getParameter("gruppo");
            idsorgente = request.getParameter("idsorgente");
            idriferimento = request.getParameter("idriferimento");
            idutente = request.getParameter("idutente");        
            datada = request.getParameter("datada");          
            dataa = request.getParameter("dataa");  
            
            // conto i parametri passati e quelli letti, in modo da controllare se mi è stato passato uno o più parametri con nome errato
            Map<String, String[]> parameterMap = request.getParameterMap();
            int numParameter = parameterMap.size();
            int countParNotNull = 0;
            
            if(actiontype != null)
                countParNotNull++;
            if(idesterno != null)
                countParNotNull++;
            if(idattivita != null)
                countParNotNull++;
            if(gruppo != null)
                countParNotNull++;
            if(idsorgente != null)
                countParNotNull++;
            if(idriferimento != null)
                countParNotNull++;
            if(idutente != null)
                countParNotNull++;
            if(datada != null)
                countParNotNull++;
            if(dataa != null)
                countParNotNull++;          
                
                
                    
            if (idapplicazione != null && idapplicazione.equals(""))
                idapplicazione = null;
            if (tokenapplicazione != null && tokenapplicazione.equals(""))
                tokenapplicazione = null;
            if (actiontype != null && actiontype.equals(""))
                actiontype = null;
            if (idesterno != null && idesterno.equals(""))
                idesterno = null;
            if (idattivita != null && idattivita.equals(""))
                idattivita = null;
            if (gruppo != null && gruppo.equals(""))                    
                gruppo = null;
            if (idsorgente != null && idsorgente.equals(""))                    
                idsorgente = null;
            if (idriferimento != null && idriferimento.equals(""))                    
                idriferimento = null;
            if (idutente != null && idutente.equals(""))
                idutente = null;
            if (datada != null && datada.equals(""))                    
                datada = null;
            if (dataa != null && dataa.equals(""))                    
                dataa = null;
        
            
            log.debug("dati ricevuti:");
            log.debug("idapplicazione: " + idapplicazione);
            log.debug("tokenapplicazione: " + tokenapplicazione);
            log.debug("actiontype: " + actiontype);
            log.debug("idesterno: " + idesterno);
            log.debug("idattivita: " + idattivita);
            log.debug("gruppo: " + gruppo);
            log.debug("idsorgente: " + idsorgente);
            log.debug("idriferimento: " + idriferimento);
            log.debug("idutente: " + idutente);                 
            log.debug("datada: " + datada);
            log.debug("dataa: " + dataa);
            
            // controllo se mi sono stati passati i dati per l'autenticazione
            if (idapplicazione == null || tokenapplicazione == null) {
                String message = "Dati di autenticazione errati, specificare i parametri \"idapplicazione\" e \"tokenapplicazione\" nella richiesta";
//                System.out.println(new java.util.Date(System.currentTimeMillis()).toString() + " : " + message);
                log.error(message);
                throw new ServletException(message);
            }


            // apro una connessione verso il db
            //dbConn = connectToPostgresDB(dbUrl, dbName, dbUsername, dbPassword);
            dbConn = UtilityFunctions.getDBConnection();
            dbConn.setAutoCommit(true);

            // controllo se l'applicazione è autorizzata
            String prefix;
            try {
                prefix = UtilityFunctions.checkAuthentication(dbConn, idapplicazione, tokenapplicazione);
            }
            catch (NotAuthorizedException ex) {
                try {
                    dbConn.close();
                }
                catch (Exception subEx) {
                }
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            
            // controllo se mi è stato passato uno o più parametri con nome errato (il + 2 è dato dal conteggio dei parametri obbligatori idapplicazione e tokenapplicazione)
            if(numParameter > countParNotNull + 2){
                String message = "Il nome di uno o più parametri è sbagliato, oppure il valore di uno o più parametri è null";
//                System.out.println(new java.util.Date(System.currentTimeMillis()).toString() + " : " + message);
                log.error(message);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                try {
                    dbConn.close();
                }
                catch (Exception ex) {
                }
                return;
            }
            
            // completo l'id attività aggiungendoci come prefisso l'idapplicazione
            if (idattivita != null)            
                idattivita = prefix + idattivita;
                
             if (actiontype != null) {
                if (!actiontype.equalsIgnoreCase(SEARCH_MULT)) {
                    String message = "Il valore del parametro \"actiontype\" è sbagliato.";
//                System.out.println(new java.util.Date(System.currentTimeMillis()).toString() + " : " + message);
                    log.error(message);
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    try {
                        dbConn.close();
                    }
                    catch (Exception ex) {
                    }
                    return;
                }
            }
             
            log.info("Ricerca attivita':");

            // altrimenti costruisco la query per l'aggiornamento concatenando la stringa creata primA
            ListAttivita res = new ListAttivita();                                      

            sqlText = "SELECT "
                    + "id_attivita, " //in realtà devo restituire id_attivita - prefix
                    + "id_utente, tipo_attivita, " // non restituiamo tipo_applicazione, perchè per definizione sarà "aliseo"
                    + "descrizione_attivita, url_command_1, stato_attivita, data_in, " // data_in è l'unica data che passiamo
                    + "note_attivita, url_command_2, url_command_3, label_url_command_1, label_url_command_2, label_url_command_3, "
                    + "uuid_anteprima, provenienza, priorita, oggetto_attivita, id_set_attivita, id_esterno, custom_app_1, custom_app_2, "
                    + "custom_app_3,  custom_app_4, custom_app_5, permesso, id_sorgente, tipo_sorgente, id_riferimento, tipo_riferimento, gruppo  "                            
                    + "FROM " + babelAttivitaTable + " WHERE id_applicazione = '" + idapplicazione + "'";  

            if (idesterno != null)
                sqlText += " AND id_esterno = '" + idesterno + "'";
            if (idattivita != null)
                if(actiontype!= null)    // sono già sicuro che se è diverso da null è uguale a SEARCH_MULT perchè l'ho controllato sopra                    
                    sqlText += " AND id_attivita LIKE '" + idattivita.replace("*", "%") + "'";
                else
                    sqlText += " AND id_attivita = '" + idattivita + "'";
            if (gruppo != null)
                sqlText += " AND gruppo = '" + gruppo + "'";
            if (idsorgente != null)
                sqlText += " AND id_sorgente = '" + idsorgente + "'";
            if (idriferimento != null)
                sqlText += " AND id_riferimento = '" + idriferimento + "'";
            if (idutente != null)                   
                 sqlText += " AND id_utente = '" + idutente + "'";
            //se sono specificate le date restringo la ricerca al range di date indicato
            if (datada != null)
                 sqlText += " AND data_in >= '" + new Timestamp(new SimpleDateFormat("dd-MM-yy HH:mm:ss").parse(datada).getTime()) + "'";
            if (dataa != null)
                 sqlText += " AND data_in <= '" + new Timestamp(new SimpleDateFormat("dd-MM-yy HH:mm:ss").parse(dataa).getTime()) + "'";

            ps = dbConn.prepareStatement(sqlText);

            log.info("eseguo la query: " + ps.toString() + " ...");

            ResultSet results = ps.executeQuery();


            while (results.next()){
                int index = 1;

                String idAttivita = results.getString(index++).substring(prefix.length());
                String idUtente = results.getString(index++);
                int tipoAttivita = results.getInt(index++);
                String descrizioneAttivita = results.getString(index++);
                String urlCommand1 = results.getString(index++);
                String statoAttivita = results.getString(index++);
                String dataIn =  new SimpleDateFormat("dd-MM-yy HH:mm:ss").format(results.getTimestamp(index++));
                String noteAttivita = results.getString(index++);
                String urlCommand2 = results.getString(index++);
                String urlCommand3 = results.getString(index++);
                String labelUrlCommand1 = results.getString(index++);
                String labelUrlCommand2 = results.getString(index++);
                String labelUrlCommand3 = results.getString(index++);
                String uuidAnteprima = results.getString(index++);
                String provenienza = results.getString(index++);
                String priorita = results.getString(index++);
                String oggettoAttivita = results.getString(index++);
                String idSetAttivita = results.getString(index++);
                String idEsterno = results.getString(index++);
                String customApp1 = results.getString(index++);
                String customApp2 = results.getString(index++);
                String customApp3 = results.getString(index++);
                String customApp4 = results.getString(index++);
                String customApp5 = results.getString(index++);
                String permesso = results.getString(index++);
                String idSorgente = results.getString(index++);
                String tipoSorgente = results.getString(index++);
                String idRiferimento = results.getString(index++);
                String tipoRiferimento = results.getString(index++);
                String gruppo1 = results.getString(index++);

                Attivita a = new Attivita();
                a.setIdAttivita(idAttivita);
                a.setIdUtente(idUtente);
                a.setTipoAttivita(tipoAttivita);
                a.setDescrizioneAttivita(descrizioneAttivita);
                a.setUrlCommand1(urlCommand1);
                a.setUrlCommand2(urlCommand2);
                a.setUrlCommand3(urlCommand3);
                a.setStatoAttivita(statoAttivita);
                a.setDataIn(dataIn);
                a.setNoteAttivita(noteAttivita);
                a.setLabelUrlCommand1(labelUrlCommand1);
                a.setLabelUrlCommand2(labelUrlCommand2);
                a.setLabelUrlCommand3(labelUrlCommand3);
                a.setUuidAnteprima(uuidAnteprima);
                a.setProvenienza(provenienza);
                a.setPriorita(priorita);
                a.setOggettoAttivita(oggettoAttivita);
                a.setIdSetAttivita(idSetAttivita);
                a.setIdEsterno(idEsterno);
                a.setCustomApp1(customApp1);
                a.setCustomApp2(customApp2);
                a.setCustomApp3(customApp3);
                a.setCustomApp4(customApp4);
                a.setCustomApp5(customApp5);
                a.setPermesso(permesso);
                a.setIdSorgente(idSorgente);
                a.setTipoSorgente(tipoSorgente);
                a.setIdRiferimento(idRiferimento);
                a.setTipoRiferimento(tipoRiferimento);
                a.setGruppo(gruppo1);  

                res.addAttivita(a);
            }  
            listAttivita = new ListAttivita();
            listAttivita = res;                           
                      
        }
        catch (Exception ex) {
            log.fatal("Errore",ex);
            log.info("Stampo i parametri della richiesta:");
            log.info("idapplicazione: " + idapplicazione);
            log.info("idattivita: " + idattivita);
            log.info("idesterno: " + idesterno);
            log.info("idutenti: " + idutente);           
            log.info("datada: " + datada);    
            log.info("dataa: " + dataa); 
            throw new ServletException(ex);
        }
        finally {
            try {
                if(ps != null)
                    ps.close();
                if (dbConn != null)
                    dbConn.close();
            }
            catch (Exception subEx) {
                log.fatal("errore nella chiusura delle connessioni al database:", subEx);
//                log.fatal(subEx);
            }
        }
        
        response.setContentType("application/json");
        try (PrintWriter out = response.getWriter()) {
            out.print(listAttivita.getJSONString());
        }
    }

   
    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    
    /** Ricerca una lista di attività secondo i parametri passati con metodo GET alla richiesta, e le restituisce in formato json  
     *
     * Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (UnsupportedEncodingException ex) {
            java.util.logging.Logger.getLogger(GetAttivita.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParseException ex) {
            java.util.logging.Logger.getLogger(GetAttivita.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /** Ricerca una lista di attività secondo i parametri passati con metodo POST alla richiesta, e le restituisce in formato json 
     *
     * Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (UnsupportedEncodingException ex) {
            java.util.logging.Logger.getLogger(GetAttivita.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParseException ex) {
            java.util.logging.Logger.getLogger(GetAttivita.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


    /**
     * Returns a short description of the servlet.
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Servlet per la ricerca delle attività dalla scrivania virtuale";
    }// </editor-fold>
 }