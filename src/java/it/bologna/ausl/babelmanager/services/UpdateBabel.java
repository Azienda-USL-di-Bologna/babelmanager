package it.bologna.ausl.babelmanager.services;

import it.bologna.ausl.babelmanager.exceptions.NotAuthorizedException;
import it.bologna.ausl.babelmanager.utils.UtilityFunctions;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 *
 * @author Giuseppe De Marco (gdm)
 */
public class UpdateBabel extends HttpServlet {

    private static final Logger log = Logger.getLogger(UpdateBabel.class);

    private static final String ONLY_CLEAN_ACTION = "only_clean_action";
    private static final String NORMAL_ACTION = "normal_action";

    private final String INSERT = "insert";
    private final String UPDATE = "update";
    private final String DELETE = "delete";
    private final String UPDATE_MULT = "update_mult";

    private final String UPDATE_PER_ID_ESTERNO = "update_per_idesterno";
    private final String UPDATE_PER_ID_ESTERNO_E_ID_UTENTE = "update_per_idesterno_e_idutente";
    private final String UPDATE_PER_GRUPPO = "update_per_gruppo";
    private final String UPDATE_PER_GRUPPO_E_ID_UTENTE = "update_per_gruppo_e_idutente";

    private final String UPDATE_PER_ID_SORGENTE = "update_per_idsorgente";
    private final String UPDATE_PER_ID_SORGENTE_E_ID_UTENTE = "update_per_idsorgente_e_idutente";

    private final String DELETE_MULT = "delete_mult";

    private final String DELETE_PER_ID_ESTERNO = "delete_per_idesterno";
    private final String DELETE_PER_ID_ESTERNO_E_ID_UTENTE = "delete_per_idesterno_e_idutente";
    private final String DELETE_PER_GRUPPO = "delete_per_gruppo";
    private final String DELETE_PER_GRUPPO_E_ID_UTENTE = "delete_per_gruppo_e_idutente";

    private final String DELETE_PER_ID_SORGENTE = "delete_per_idsorgente";
    private final String DELETE_PER_ID_SORGENTE_E_ID_UTENTE = "delete_per_idsorgente_e_idutente";

//private static int IDATTIVITA_MAX_LENGTH = 20;
    private final int SQL_TRUE = -1;
    private final int SQL_FALSE = 0;

    private final int ACTIVITY_STATE_NEW = 1;
    private final int ACTIVITY_STATE_ACCEPTED = 2;
    private final int ACTIVITY_STATE_ENDED = 3;

    private final String ACTIVITY_STATE_DYNAMIC = "1";
    private final String ACTIVITY_TYPE_STATIC = "2";
    private final String ACTIVITY_STATE_NOTIFY = "3";

    private final String SQL_EXCEPTION_DUPLICATED_ITEM = "23";

    /**
     * Processa le richieste (GET/POST/DELETE). Se la richiesta è GET o POST
     * inserisce una nuova attivita nel DB; se la richiesta è DELETE cancella
     * l'attività dalla scrivania
     *
     * @param request servlet request
     * @param response servlet response
     * @param action
     * @throws ServletException if a servlet-specific error occurs
     * @throws java.io.UnsupportedEncodingException
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response, String action) throws ServletException, UnsupportedEncodingException {
        request.setCharacterEncoding("utf-8");
//     configuro il logger per la console
//        BasicConfigurator.configure();
        log.info("--------------------------");
        log.info("Avvio servlet: " + getClass().getSimpleName());
        log.info("--------------------------");
        Timestamp current_date = new Timestamp(System.currentTimeMillis());
        Connection dbConn = null;
        PreparedStatement ps = null;
        String idapplicazione = null;
        String tokenapplicazione = null;
        String idattivita = null;
        String idesterno = null;
        String idutenti = null;
        String idtipiattivita = null;
        String descrizioneattivita = null;
        String urlcommand = null;
        String datain = null;
        String statoattivita = null;
        String noteattivita = null;
        String actiontype = null;
        String labelurlcommand = null;
        String urlcommand2 = null;
        String labelurlcommand2 = null;
        String urlcommand3 = null;
        String labelurlcommand3 = null;
        String uuidanteprima = null;
        String provenienza = null;
        String oggettoattivita = null;
        String priorita = null;
        String datascadenza = null;
        String customapp1 = null;
        String customapp2 = null;
        String customapp3 = null;
        String customapp4 = null;
        String customapp5 = null;
        String permesso = null;
        String idsorgente = null;
        String tiposorgente = null;
        String idriferimento = null;
        String tiporiferimento = null;
        String gruppo = null;

        String updatestorico = null;

        String setattivita = null;
        String archiviazione;
        String setaccessoesclusivo = null;

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
            String authenticationTable = getServletContext().getInitParameter("authenticationTable");
            String pendingActivityTableName = getServletContext().getInitParameter("pendingActivityTableName");
            String doneActivityTableName = getServletContext().getInitParameter("doneActivityTableName");
            String activitiesSetTableName = getServletContext().getInitParameter("activitiesSetTableName");
            String usersTableName = getServletContext().getInitParameter("usersTableName");

            if (pendingActivityTableName == null || pendingActivityTableName.equals("")) {
                String message = "Manca il nome della tabella in cui inserire le attività. Indicare il nome della tabella all'interno del \"web.xml\"";
//                System.out.println(new java.util.Date(System.currentTimeMillis()).toString() + " : " + message);
                log.error(message);
                throw new ServletException(message);
            } else if (doneActivityTableName == null || doneActivityTableName.equals("")) {
                String message = "Manca il nome della tabella in cui inserire le attività svolte. Indicare il nome della tabella all'interno del \"web.xml\"";
//                System.out.println(new java.util.Date(System.currentTimeMillis()).toString() + " : " + message);
                log.error(message);
                throw new ServletException(message);
            } else if (activitiesSetTableName == null || activitiesSetTableName.equals("")) {
                String message = "Manca il nome della tabella che raccoglie i set di attività. Indicare il nome della tabella all'interno del \"web.xml\"";
//                System.out.println(new java.util.Date(System.currentTimeMillis()).toString() + " : " + message);
                log.error(message);
                throw new ServletException(message);
            }

            // leggo i parametri per l'aggiornamento della scrivania virtuale dalla richiesta HTTP
            // dati per l'autenticazione
            idapplicazione = request.getParameter("idapplicazione");
            tokenapplicazione = request.getParameter("tokenapplicazione");

            // dati per l'esecuzione della query
            idattivita = request.getParameter("idattivita");
            idesterno = request.getParameter("idesterno");
            idutenti = request.getParameter("idutenti");
            idtipiattivita = request.getParameter("idtipiattivita");
            descrizioneattivita = request.getParameter("descrizioneattivita");
            urlcommand = request.getParameter("urlcommand");
            //String statoattivita = request.getParameter("statoattivita");
            datain = request.getParameter("datain");
            statoattivita = request.getParameter("statoattivita");
            noteattivita = request.getParameter("noteattivita");

            // tipo di operazione da eseguire (possibili valori: "insert" o "delete")
            actiontype = request.getParameter("actiontype");

            // nuovi
            labelurlcommand = request.getParameter("labelurlcommand");
            urlcommand2 = request.getParameter("urlcommand2");
            labelurlcommand2 = request.getParameter("labelurlcommand2");
            urlcommand3 = request.getParameter("urlcommand3");
            labelurlcommand3 = request.getParameter("labelurlcommand3");
            uuidanteprima = request.getParameter("uuidanteprima");
            provenienza = request.getParameter("provenienza");
            oggettoattivita = request.getParameter("oggettoattivita");
            priorita = request.getParameter("priorita");
            datascadenza = request.getParameter("datascadenza");

            customapp1 = request.getParameter("customapp1");
            customapp2 = request.getParameter("customapp2");
            customapp3 = request.getParameter("customapp3");
            customapp4 = request.getParameter("customapp4");
            customapp5 = request.getParameter("customapp5");

            permesso = request.getParameter("permesso");

            idsorgente = request.getParameter("idsorgente");
            tiposorgente = request.getParameter("tiposorgente");
            idriferimento = request.getParameter("idriferimento");
            tiporiferimento = request.getParameter("tiporiferimento");
            gruppo = request.getParameter("gruppo");

            updatestorico = request.getParameter("updatestorico");

            setattivita = request.getParameter("setattivita");
            archiviazione = request.getParameter("archiviazione");
            setaccessoesclusivo = request.getParameter("setaccessoesclusivo");
            int setaccessoesclusivo_int = -1;
            //fine nuovi

            if (idapplicazione != null && idapplicazione.equals("")) {
                idapplicazione = null;
            }
            if (tokenapplicazione != null && tokenapplicazione.equals("")) {
                tokenapplicazione = null;
            }
            if (idattivita != null && idattivita.equals("")) {
                idattivita = null;
            }
            if (idesterno != null && idesterno.equals("")) {
                idesterno = null;
            }
            if (idutenti != null && idutenti.equals("")) {
                idutenti = null;
            }
            if (idtipiattivita != null && idtipiattivita.equals("")) {
                idtipiattivita = null;
            }
            if (descrizioneattivita != null && descrizioneattivita.equals("")) {
                descrizioneattivita = null;
            }
            if (urlcommand != null && urlcommand.equals("")) {
                urlcommand = null;
            }
            if (datain != null && datain.equals("")) {
                datain = null;
            }
            if (statoattivita != null && statoattivita.equals("")) {
                statoattivita = null;
            }
            if (noteattivita != null && noteattivita.equals("")) {
                noteattivita = null;
            }
            if (actiontype != null && actiontype.equals("")) {
                actiontype = null;
            }
            if (labelurlcommand != null && labelurlcommand.equals("")) {
                labelurlcommand = null;
            }
            if (urlcommand2 != null && urlcommand2.equals("")) {
                urlcommand2 = null;
            }
            if (labelurlcommand2 != null && labelurlcommand2.equals("")) {
                labelurlcommand2 = null;
            }
            if (urlcommand3 != null && urlcommand3.equals("")) {
                urlcommand3 = null;
            }
            if (labelurlcommand3 != null && labelurlcommand3.equals("")) {
                labelurlcommand3 = null;
            }
            if (uuidanteprima != null && uuidanteprima.equals("")) {
                uuidanteprima = null;
            }
            if (provenienza != null && provenienza.equals("")) {
                provenienza = null;
            }
            if (oggettoattivita != null && oggettoattivita.equals("")) {
                oggettoattivita = null;
            }
            if (priorita != null && priorita.equals("")) {
                priorita = null;
            }
            if (datascadenza != null && datascadenza.equals("")) {
                datascadenza = null;
            }
            if (provenienza != null && provenienza.equals("")) {
                provenienza = null;
            }

            if (customapp1 != null && customapp1.equals("")) {
                customapp1 = null;
            }
            if (customapp2 != null && customapp2.equals("")) {
                customapp2 = null;
            }
            if (customapp3 != null && customapp3.equals("")) {
                customapp3 = null;
            }
            if (customapp4 != null && customapp4.equals("")) {
                customapp4 = null;
            }
            if (customapp5 != null && customapp5.equals("")) {
                customapp5 = null;
            }
            if (permesso != null && permesso.equals("")) {
                permesso = null;
            }

            if (idsorgente != null && idsorgente.equals("")) {
                idsorgente = null;
            }
            if (tiposorgente != null && tiposorgente.equals("")) {
                tiposorgente = null;
            }
            if (idriferimento != null && idriferimento.equals("")) {
                idriferimento = null;
            }
            if (tiporiferimento != null && tiporiferimento.equals("")) {
                tiporiferimento = null;
            }
            if (gruppo != null && gruppo.equals("")) {
                gruppo = null;
            }

            if (updatestorico != null && updatestorico.equals("")) {
                updatestorico = null;
            }

            if (setattivita != null && setattivita.equals("")) {
                setattivita = null;
            }
            if (archiviazione != null && archiviazione.equals("")) {
                archiviazione = null;
            }
            if (setaccessoesclusivo != null && setaccessoesclusivo.equals("")) {
                setaccessoesclusivo = null;
            }

            log.debug("dati ricevuti:");
            log.debug("idapplicazione: " + idapplicazione);
            log.debug("idattivita: " + idattivita);
            log.debug("idesterno: " + idesterno);
            log.debug("idutenti: " + idutenti);
            log.debug("idtipiattivita: " + idtipiattivita);
            log.debug("descrizioneattivita: " + descrizioneattivita);
            log.debug("urlcommand: " + urlcommand);
            log.debug("datain: " + datain);
            log.debug("statoattivita: " + statoattivita);
            log.debug("noteattivita: " + noteattivita);
            log.debug("actiontype: " + actiontype);
            log.debug("labelurlcommand: " + labelurlcommand);
            log.debug("urlcommand2: " + urlcommand2);
            log.debug("labelurlcommand2: " + labelurlcommand2);
            log.debug("urlcommand3: " + urlcommand3);
            log.debug("labelurlcommand3: " + labelurlcommand3);
            log.debug("uuidanteprima: " + uuidanteprima);
            log.debug("provenienza: " + provenienza);
            log.debug("oggettoattivita: " + oggettoattivita);
            log.debug("priorita: " + priorita);
            log.debug("datascadenza: " + datascadenza);
            log.debug("customapp1: " + customapp1);
            log.debug("customapp2: " + customapp2);
            log.debug("customapp3: " + customapp3);
            log.debug("customapp4: " + customapp4);
            log.debug("customapp5: " + customapp5);
            log.debug("permesso: " + permesso);
            log.debug("idsorgente: " + idsorgente);
            log.debug("tiposorgente: " + tiposorgente);
            log.debug("idriferimento: " + idriferimento);
            log.debug("tiporiferimento: " + tiporiferimento);
            log.debug("gruppo: " + gruppo);

            log.debug("updatestorico: " + updatestorico);

            log.debug("setattivita: " + setattivita);
            log.debug("archiviazione: " + archiviazione);
            log.debug("setaccessoesclusivo: " + setaccessoesclusivo);

            // se la rischiesta è stata fatta con method "DELETE" assumo che si voglia eseguire un DELETE_MULT
            if (action.equalsIgnoreCase(ONLY_CLEAN_ACTION)) {
                actiontype = DELETE_MULT;
            }

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
            } catch (NotAuthorizedException ex) {
                try {
                    dbConn.close();
                } catch (Exception subEx) {
                }
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            // scrivo sulla scrivania
            int updatedRows = 0;
            if (actiontype == null) {
                String message = "Manca il parametro \"actiontype\". Indicarlo nei parametri della richiesta";
//                System.out.println(new java.util.Date(System.currentTimeMillis()).toString() + " : " + message);
                log.error(message);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                try {
                    dbConn.close();
                } catch (Exception ex) {
                }
                return;
            }

            if (actiontype.equalsIgnoreCase(UPDATE_PER_ID_ESTERNO) || actiontype.equalsIgnoreCase(DELETE_PER_ID_ESTERNO)
                    || actiontype.equalsIgnoreCase(UPDATE_PER_ID_ESTERNO_E_ID_UTENTE) || actiontype.equalsIgnoreCase(DELETE_PER_ID_ESTERNO_E_ID_UTENTE)) {
                if (idesterno == null || idesterno.equals("")) {
                    String message = "Manca il parametro \"idesterno\". Indicarlo nei parametri della richiesta";
//                System.out.println(new java.util.Date(System.currentTimeMillis()).toString() + " : " + message);
                    log.error(message);
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    try {
                        dbConn.close();
                    } catch (Exception ex) {
                    }
                    return;
                }
            } else if (actiontype.equalsIgnoreCase(UPDATE_PER_GRUPPO) || actiontype.equalsIgnoreCase(DELETE_PER_GRUPPO)
                    || actiontype.equalsIgnoreCase(UPDATE_PER_GRUPPO_E_ID_UTENTE) || actiontype.equalsIgnoreCase(DELETE_PER_GRUPPO_E_ID_UTENTE)) {
                if (gruppo == null || gruppo.equals("")) {
                    String message = "Manca il parametro \"gruppo\". Indicarlo nei parametri della richiesta";
//                System.out.println(new java.util.Date(System.currentTimeMillis()).toString() + " : " + message);
                    log.error(message);
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    try {
                        dbConn.close();
                    } catch (Exception ex) {
                    }
                    return;
                }
            } else if (actiontype.equalsIgnoreCase(UPDATE_PER_ID_SORGENTE) || actiontype.equalsIgnoreCase(DELETE_PER_ID_SORGENTE)
                    || actiontype.equalsIgnoreCase(UPDATE_PER_ID_SORGENTE_E_ID_UTENTE) || actiontype.equalsIgnoreCase(DELETE_PER_ID_SORGENTE_E_ID_UTENTE)) {
                if (idsorgente == null || idsorgente.equals("")) {
                    String message = "Manca il parametro \"idsorgente\". Indicarlo nei parametri della richiesta";
//                System.out.println(new java.util.Date(System.currentTimeMillis()).toString() + " : " + message);
                    log.error(message);
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    try {
                        dbConn.close();
                    } catch (Exception ex) {
                    }
                    return;
                }
            } else {
                if (idattivita == null || idattivita.equals("")) {
                    String message = "Manca il parametro \"idattivita\". Indicarlo nei parametri della richiesta";
                    //                System.out.println(new java.util.Date(System.currentTimeMillis()).toString() + " : " + message);
                    log.error(message);
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    try {
                        dbConn.close();
                    } catch (Exception ex) {
                    }
                    return;
                }
            }
            // completo l'id attività aggiungendoci come prefisso l'idapplicazione
            idattivita = prefix + idattivita;

            if (setaccessoesclusivo != null) {
                if (setaccessoesclusivo.equalsIgnoreCase("true")) {
                    setaccessoesclusivo_int = SQL_TRUE;
                } else if (setaccessoesclusivo.equalsIgnoreCase("false")) {
                    setaccessoesclusivo_int = SQL_FALSE;
                } else {
                    String message = "Parametro \"setaccessoesclusivo\" errato: i valori possibili sono \"true\" o \"false\"";
//                    System.out.println(new java.util.Date(System.currentTimeMillis()).toString() + " : " + message);
                    log.error(message);
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    try {
                        dbConn.close();
                    } catch (Exception ex) {
                    }
                    return;
                }
            }
            dbConn.setAutoCommit(false);

            // aggiungo il set_attivita se passato e se non esiste già
            if (setattivita != null && actiontype.equals(INSERT)) {
                Savepoint savepoint = null;
                sqlText = "INSERT INTO " + activitiesSetTableName
                        + "(id_set_attivita, descrizione_set" + (setaccessoesclusivo != null ? ", accesso_me" : "") + ")"
                        + "VALUES (?, ?" + (setaccessoesclusivo != null ? ", ?" : "") + ") ON CONFLICT (id_set_attivita) DO NOTHING";

                ps = dbConn.prepareStatement(sqlText);
                ps.setString(1, setattivita);
                ps.setString(2, setattivita);
                if (setaccessoesclusivo != null) {
                    ps.setInt(3, setaccessoesclusivo_int);
                }
                try {
                    savepoint = dbConn.setSavepoint("inserting_set");
                    ps.executeUpdate();
                    dbConn.releaseSavepoint(savepoint);
                } catch (SQLException sQLException) {
                    // se il set esiste già ignoro l'eccezione
                    if (sQLException.getSQLState().startsWith(SQL_EXCEPTION_DUPLICATED_ITEM)) {
                        dbConn.rollback(savepoint);
                    } else {
                        throw sQLException;
                    }
                }

            }

            List<String> usersToRefresh = getUsersForRefreshActivitiesCommand(dbConn, request.getSession().getId(), pendingActivityTableName, actiontype, idutenti, idattivita, idesterno, gruppo, idsorgente, tiposorgente);
            if (!usersToRefresh.contains(idutenti) && idutenti != null) {
                usersToRefresh.add(idutenti);
            }
////////////////////////////////////
            if (actiontype.equalsIgnoreCase(INSERT)) {
                log.info("Inserimento nuova attivita'");
                // controllo che siano stati passati tutti i parametri necessari

                if (idtipiattivita == null || idtipiattivita.equals("")) {
                    String message = "Manca il parametro \"idtipiattivita\". Indicarlo nei parametri della richiesta";
//                    System.out.println(new java.util.Date(System.currentTimeMillis()).toString() + " : " + message);
                    log.error(message);
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    try {
                        dbConn.close();
                    } catch (Exception ex) {
                    }
                    return;
                } else if (!idtipiattivita.equals(ACTIVITY_TYPE_STATIC) && (idutenti == null || idutenti.equals(""))) {
                    String message = "Manca il parametro \"idutenti\". Indicarlo nei parametri della richiesta";
//                    System.out.println(new java.util.Date(System.currentTimeMillis()).toString() + " : " + message);
                    log.error(message);
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    try {
                        dbConn.close();
                    } catch (Exception ex) {
                    }
                    return;
                }
//                else if(oggettoattivita == null || oggettoattivita.equals("")) {
//                    String message = "Manca il parametro \"oggettoattivita\". Indicarlo nei parametri della richiesta";
////                    System.out.println(new java.util.Date(System.currentTimeMillis()).toString() + " : " + message);
//                    log.error(message);
//                    response.sendError(HttpServletResponse.SC_BAD_REQUEST);
//                    try {
//                        dbConn.close();
//                    }
//                    catch (Exception ex) {
//                    }
//                    return;
//                }

                sqlText = "INSERT INTO " + pendingActivityTableName
                        + " (id_attivita, id_applicazione, id_utente, tipo_attivita"
                        + (descrizioneattivita != null ? ", descrizione_attivita" : "")
                        + ", data_in"
                        + (noteattivita != null ? ", note_attivita" : "")
                        + ", stato_attivita"
                        + (urlcommand != null ? ", url_command_1" : "") + (labelurlcommand != null ? ", label_url_command_1" : "")
                        + (urlcommand2 != null ? ", url_command_2" : "") + (labelurlcommand2 != null ? ", label_url_command_2" : "")
                        + (urlcommand3 != null ? ", url_command_3" : "") + (labelurlcommand3 != null ? ", label_url_command_3" : "")
                        + (uuidanteprima != null ? ", uuid_anteprima" : "")
                        + (provenienza != null ? ", provenienza" : "")
                        + (datascadenza != null ? ", data_scadenza" : "")
                        + (priorita != null ? ", priorita" : "")
                        + (setattivita != null ? ", id_set_attivita" : "")
                        + (oggettoattivita != null ? ", oggetto_attivita" : "")
                        + (idesterno != null ? ", id_esterno" : "")
                        + (customapp1 != null ? ", custom_app_1" : "")
                        + (customapp2 != null ? ", custom_app_2" : "")
                        + (customapp3 != null ? ", custom_app_3" : "")
                        + (customapp4 != null ? ", custom_app_4" : "")
                        + (customapp5 != null ? ", custom_app_5" : "")
                        + (permesso != null ? ", permesso" : "")
                        + (idsorgente != null ? ", id_sorgente" : "")
                        + (tiposorgente != null ? ", tipo_sorgente" : "")
                        + (idriferimento != null ? ", id_riferimento" : "")
                        + (tiporiferimento != null ? ", tipo_riferimento" : "")
                        + (gruppo != null ? ", gruppo" : "")
                        + ") VALUES (?, ?, ?, ?"
                        + (descrizioneattivita != null ? ", ?" : "")
                        + ", ?" + (noteattivita != null ? ", ?" : "")
                        + ", ?"
                        + (urlcommand != null ? ", ?" : "") + (labelurlcommand != null ? ", ?" : "")
                        + (urlcommand2 != null ? ", ?" : "") + (labelurlcommand2 != null ? ", ?" : "")
                        + (urlcommand3 != null ? ", ?" : "") + (labelurlcommand3 != null ? ", ?" : "")
                        + (uuidanteprima != null ? ", ?" : "")
                        + (provenienza != null ? ", ?" : "")
                        + (datascadenza != null ? ", ?" : "")
                        + (priorita != null ? ", ?" : "")
                        + (setattivita != null ? ", ?" : "")
                        + (oggettoattivita != null ? ", ?" : "")
                        + (idesterno != null ? ", ?" : "")
                        + (customapp1 != null ? ", ?" : "")
                        + (customapp2 != null ? ", ?" : "")
                        + (customapp3 != null ? ", ?" : "")
                        + (customapp4 != null ? ", ?" : "")
                        + (customapp5 != null ? ", ?" : "")
                        + (permesso != null ? ", ?" : "")
                        + (idsorgente != null ? ", ?" : "")
                        + (tiposorgente != null ? ", ?" : "")
                        + (idriferimento != null ? ", ?" : "")
                        + (tiporiferimento != null ? ", ?" : "")
                        + (gruppo != null ? ", ?" : "")
                        + ")";
                ps = dbConn.prepareStatement(sqlText);
                indexQuery = 1;
                ps.setString(indexQuery++, idattivita);
                ps.setString(indexQuery++, idapplicazione);
                ps.setString(indexQuery++, idutenti);
                ps.setInt(indexQuery++, Integer.parseInt(idtipiattivita));
                if (descrizioneattivita != null) {
                    ps.setString(indexQuery++, descrizioneattivita);
                }
                // se non è stata passata la data (datain) setto la data corrente
                if (datain == null) {
                    ps.setTimestamp(indexQuery++, current_date);
                } else {
                    ps.setTimestamp(indexQuery++, new Timestamp(new SimpleDateFormat("dd-MM-yy HH:mm:ss").parse(datain).getTime()));
                }
                if (noteattivita != null) {
                    ps.setString(indexQuery++, noteattivita);
                }
                ps.setInt(indexQuery++, ACTIVITY_STATE_NEW);

                if (urlcommand != null) {
                    ps.setString(indexQuery++, urlcommand);
                }
                if (labelurlcommand != null) {
                    ps.setString(indexQuery++, labelurlcommand);
                }
                if (urlcommand2 != null) {
                    ps.setString(indexQuery++, urlcommand2);
                }
                if (labelurlcommand2 != null) {
                    ps.setString(indexQuery++, labelurlcommand2);
                }
                if (urlcommand3 != null) {
                    ps.setString(indexQuery++, urlcommand3);
                }
                if (labelurlcommand3 != null) {
                    ps.setString(indexQuery++, labelurlcommand3);
                }
                if (uuidanteprima != null) {
                    ps.setString(indexQuery++, uuidanteprima);
                }
                if (provenienza != null) {
                    ps.setString(indexQuery++, provenienza);
                }
                if (datascadenza != null) {
                    ps.setTimestamp(indexQuery++, new Timestamp(new SimpleDateFormat("dd-MM-yy HH:mm:ss").parse(datascadenza).getTime()));
                }
                if (priorita != null) {
                    ps.setInt(indexQuery++, Integer.parseInt(priorita));
                }
                if (setattivita != null) {
                    ps.setString(indexQuery++, setattivita);
                }
                if (oggettoattivita != null) {
                    ps.setString(indexQuery++, oggettoattivita);
                }
                if (idesterno != null) {
                    ps.setString(indexQuery++, idesterno);
                }
                if (customapp1 != null) {
                    ps.setString(indexQuery++, customapp1);
                }
                if (customapp2 != null) {
                    ps.setString(indexQuery++, customapp2);
                }
                if (customapp3 != null) {
                    ps.setString(indexQuery++, customapp3);
                }
                if (customapp4 != null) {
                    ps.setString(indexQuery++, customapp4);
                }
                if (customapp5 != null) {
                    ps.setString(indexQuery++, customapp5);
                }
                if (permesso != null) {
                    ps.setString(indexQuery++, permesso);
                }
                if (idsorgente != null) {
                    ps.setString(indexQuery++, idsorgente);
                }
                if (tiposorgente != null) {
                    ps.setString(indexQuery++, tiposorgente);
                }
                if (idriferimento != null) {
                    ps.setString(indexQuery++, idriferimento);
                }
                if (tiporiferimento != null) {
                    ps.setString(indexQuery++, tiporiferimento);
                }
                if (gruppo != null) {
                    ps.setString(indexQuery++, gruppo);
                }

//                System.out.println(new java.util.Date(System.currentTimeMillis()).toString() + " : " + "eseguo la query: " + ps.toString() + " ...");
                log.info("eseguo la query: " + ps.toString() + " ...");
                try {
                    updatedRows = ps.executeUpdate();
                } catch (SQLException sQLException) {
                    if (!sQLException.getSQLState().startsWith(SQL_EXCEPTION_DUPLICATED_ITEM)) {
                        throw sQLException;
                    }
                }
            } else if (actiontype.equalsIgnoreCase(UPDATE)
                    || actiontype.equalsIgnoreCase(UPDATE_MULT)
                    || actiontype.equalsIgnoreCase(UPDATE_PER_ID_ESTERNO)
                    || actiontype.equalsIgnoreCase(UPDATE_PER_ID_ESTERNO_E_ID_UTENTE)
                    || actiontype.equalsIgnoreCase(UPDATE_PER_GRUPPO)
                    || actiontype.equalsIgnoreCase(UPDATE_PER_GRUPPO_E_ID_UTENTE)
                    || actiontype.equalsIgnoreCase(UPDATE_PER_ID_SORGENTE)
                    || actiontype.equalsIgnoreCase(UPDATE_PER_ID_SORGENTE_E_ID_UTENTE)) {

                log.info("Aggiornamento attivita':");

                // costruisco la stringa da concatenare alla query per l'update dei valori con i vari campi passati
                String updateString = (idtipiattivita != null ? ", tipo_attivita = ?" : "") + (datain != null ? ", data_in = ?" : "") + (descrizioneattivita != null ? ", descrizione_attivita = ?" : "") + (statoattivita != null ? ", stato_attivita = ?" : "") + (noteattivita != null ? ", note_attivita = ?" : "")
                        + (urlcommand != null ? ", url_command_1 = ?" : "") + (labelurlcommand != null ? ", label_url_command_1 = ?" : "") + (urlcommand2 != null ? ", url_command_2 = ?" : "") + (labelurlcommand2 != null ? ", label_url_command_2 = ?" : "") + (urlcommand3 != null ? ", url_command_3 = ?" : "") + (labelurlcommand3 != null ? ", label_url_command_3 = ?" : "")
                        + (uuidanteprima != null ? ", uuid_anteprima = ?" : "") + (provenienza != null ? ", provenienza = ?" : "")
                        + (datascadenza != null ? ", data_scadenza = ?" : "") + (priorita != null ? ", priorita = ?" : "") + (oggettoattivita != null ? ", oggetto_attivita = ?" : "")
                        + (customapp1 != null ? ", custom_app_1 = ?" : "") + (customapp2 != null ? ", custom_app_2 = ?" : "") + (customapp3 != null ? ", custom_app_3 = ?" : "") + (customapp4 != null ? ", custom_app_4 = ?" : "") + (customapp5 != null ? ", custom_app_5 = ?" : "")
                        + (permesso != null ? ", permesso = ?" : "")
                        + (idriferimento != null ? ", id_riferimento = ?" : "") + (tiporiferimento != null ? ", tipo_riferimento = ?" : "");
                if (!actiontype.equalsIgnoreCase(UPDATE_PER_ID_ESTERNO) && !actiontype.equalsIgnoreCase(UPDATE_PER_ID_ESTERNO_E_ID_UTENTE)) {
                    updateString += (idesterno != null ? ", id_esterno = ?" : "");
                }

                if (!actiontype.equalsIgnoreCase(UPDATE_PER_GRUPPO) && !actiontype.equalsIgnoreCase(UPDATE_PER_GRUPPO_E_ID_UTENTE)) {
                    updateString += (gruppo != null ? ", gruppo = ?" : "");
                }

                if (!actiontype.equalsIgnoreCase(UPDATE_PER_ID_SORGENTE) && !actiontype.equalsIgnoreCase(UPDATE_PER_ID_SORGENTE_E_ID_UTENTE)) {
                    updateString += (idsorgente != null ? ", id_sorgente = ?" : "") + (tiposorgente != null ? ", tipo_sorgente = ?" : "");
                }

                if (!actiontype.equalsIgnoreCase(UPDATE_PER_ID_ESTERNO_E_ID_UTENTE)
                        && !actiontype.equalsIgnoreCase(UPDATE_PER_GRUPPO_E_ID_UTENTE)
                        && !actiontype.equalsIgnoreCase(UPDATE_PER_ID_SORGENTE_E_ID_UTENTE)) {
                    updateString += (idutenti != null ? ", id_utente = ?" : "");
                }

                // se la stringa è vuota vuol dire che non mi è stato passato nessun valore.
                // non eseguo la query di aggiornamento, ma ritorno comunque un "ok"
                if (updateString.length() == 0) {
                    String message = "Non è stato passato alcun valore da aggiornare, non è stata eseguita nessuna query";
//                    System.out.println(new java.util.Date(System.currentTimeMillis()).toString() + " : " + message);
                    log.info(message);
                    response.setStatus(HttpServletResponse.SC_OK);
                    try {
                        dbConn.close();
                    } catch (Exception ex) {
                    }
                    return;
                } // altrimenti costruisco la query per l'aggiornamento concatenando la stringa creata prima
                else {
                    // tolgo la prima virgola dalla stringa (es. ", idtipiattivita = ?, datain = ?", diventa "idtipiattivita = ?, datain = ?")
                    updateString = updateString.substring(2);

                    sqlText = "UPDATE " + pendingActivityTableName
                            + " SET " + updateString;
                    if (actiontype.equalsIgnoreCase(UPDATE_PER_ID_ESTERNO)) {
                        sqlText += " WHERE id_esterno = ?";
                    } else if (actiontype.equalsIgnoreCase(UPDATE_PER_ID_ESTERNO_E_ID_UTENTE)) {
                        sqlText += " WHERE id_esterno = ? and id_utente = ?";
                    } else if (actiontype.equalsIgnoreCase(UPDATE_PER_GRUPPO)) {
                        sqlText += " WHERE gruppo = ?";
                    } else if (actiontype.equalsIgnoreCase(UPDATE_PER_GRUPPO_E_ID_UTENTE)) {
                        sqlText += " WHERE gruppo = ? and id_utente = ?";
                    } else if (actiontype.equalsIgnoreCase(UPDATE_PER_ID_SORGENTE)) {
                        sqlText += " WHERE id_sorgente = ?" + (tiposorgente != null ? " and tipo_sorgente = ?" : "");
                    } else if (actiontype.equalsIgnoreCase(UPDATE_PER_ID_SORGENTE_E_ID_UTENTE)) {
                        sqlText += " WHERE id_sorgente = ? and id_utente = ?" + (tiposorgente != null ? " and tipo_sorgente = ?" : "");
                    } else {
                        sqlText += " WHERE id_attivita" + (actiontype.equalsIgnoreCase(UPDATE_MULT) ? " like " : " = ") + "?";
                    }
                    ps = dbConn.prepareStatement(sqlText);

                    indexQuery = 1;
                    if (idtipiattivita != null) {
                        ps.setInt(indexQuery++, Integer.parseInt(idtipiattivita));
                    }
                    if (datain != null) {
                        ps.setTimestamp(indexQuery++, new Timestamp(new SimpleDateFormat("dd-MM-yy HH:mm:ss").parse(datain).getTime()));
                    }
                    if (descrizioneattivita != null) {
                        ps.setString(indexQuery++, descrizioneattivita.equalsIgnoreCase("null") ? null : descrizioneattivita);
                    }
                    if (statoattivita != null) {
                        ps.setString(indexQuery++, statoattivita);
                    }
                    if (noteattivita != null) {
                        ps.setString(indexQuery++, noteattivita.equalsIgnoreCase("null") ? null : noteattivita);
                    }
                    if (urlcommand != null) {
                        ps.setString(indexQuery++, urlcommand.equalsIgnoreCase("null") ? null : urlcommand);
                    }
                    if (labelurlcommand != null) {
                        ps.setString(indexQuery++, labelurlcommand.equalsIgnoreCase("null") ? null : labelurlcommand);
                    }
                    if (urlcommand2 != null) {
                        ps.setString(indexQuery++, urlcommand2.equalsIgnoreCase("null") ? null : urlcommand2);
                    }
                    if (labelurlcommand2 != null) {
                        ps.setString(indexQuery++, labelurlcommand2.equalsIgnoreCase("null") ? null : labelurlcommand2);
                    }
                    if (urlcommand3 != null) {
                        ps.setString(indexQuery++, urlcommand3.equalsIgnoreCase("null") ? null : urlcommand3);
                    }
                    if (labelurlcommand3 != null) {
                        ps.setString(indexQuery++, labelurlcommand3.equalsIgnoreCase("null") ? null : labelurlcommand3);
                    }
                    if (uuidanteprima != null) {
                        ps.setString(indexQuery++, uuidanteprima.equalsIgnoreCase("null") ? null : uuidanteprima);
                    }
                    if (provenienza != null) {
                        ps.setString(indexQuery++, provenienza.equalsIgnoreCase("null") ? null : provenienza);
                    }
                    if (datascadenza != null) {
                        ps.setTimestamp(indexQuery++, new Timestamp(new SimpleDateFormat("dd-MM-yy HH:mm:ss").parse(datascadenza).getTime()));
                    }
                    if (priorita != null) {
                        ps.setInt(indexQuery++, Integer.parseInt(priorita));
                    }
                    if (oggettoattivita != null) {
                        ps.setString(indexQuery++, oggettoattivita.equalsIgnoreCase("null") ? null : oggettoattivita);
                    }
                    if (customapp1 != null) {
                        ps.setString(indexQuery++, customapp1.equalsIgnoreCase("null") ? null : customapp1);
                    }
                    if (customapp2 != null) {
                        ps.setString(indexQuery++, customapp2.equalsIgnoreCase("null") ? null : customapp2);
                    }
                    if (customapp3 != null) {
                        ps.setString(indexQuery++, customapp3.equalsIgnoreCase("null") ? null : customapp3);
                    }
                    if (customapp4 != null) {
                        ps.setString(indexQuery++, customapp4.equalsIgnoreCase("null") ? null : customapp4);
                    }
                    if (customapp5 != null) {
                        ps.setString(indexQuery++, customapp5.equalsIgnoreCase("null") ? null : customapp5);
                    }
                    if (permesso != null) {
                        ps.setString(indexQuery++, permesso.equalsIgnoreCase("null") ? null : permesso);
                    }

                    if (idriferimento != null) {
                        ps.setString(indexQuery++, idriferimento);
                    }
                    if (tiporiferimento != null) {
                        ps.setString(indexQuery++, tiporiferimento);
                    }

                    if (!actiontype.equalsIgnoreCase(UPDATE_PER_ID_ESTERNO) && !actiontype.equalsIgnoreCase(UPDATE_PER_ID_ESTERNO_E_ID_UTENTE) && idesterno != null) {
                        ps.setString(indexQuery++, idesterno);
                    }
                    if (!actiontype.equalsIgnoreCase(UPDATE_PER_GRUPPO) && !actiontype.equalsIgnoreCase(UPDATE_PER_GRUPPO_E_ID_UTENTE) && gruppo != null) {
                        ps.setString(indexQuery++, gruppo);
                    }
                    if (!actiontype.equalsIgnoreCase(UPDATE_PER_ID_SORGENTE) && !actiontype.equalsIgnoreCase(UPDATE_PER_ID_SORGENTE_E_ID_UTENTE) && idsorgente != null) {
                        ps.setString(indexQuery++, idsorgente);
                        if (tiposorgente != null) {
                            ps.setString(indexQuery++, tiposorgente);
                        }
                    }
                    if (!actiontype.equalsIgnoreCase(UPDATE_PER_ID_ESTERNO_E_ID_UTENTE) && !actiontype.equalsIgnoreCase(UPDATE_PER_GRUPPO_E_ID_UTENTE) && !actiontype.equalsIgnoreCase(UPDATE_PER_ID_SORGENTE_E_ID_UTENTE) && idutenti != null) {
                        ps.setString(indexQuery++, idutenti);
                    }

                    if (actiontype.equalsIgnoreCase(UPDATE_PER_ID_ESTERNO)) {
                        ps.setString(indexQuery++, idesterno);
                    } else if (actiontype.equalsIgnoreCase(UPDATE_PER_GRUPPO)) {
                        ps.setString(indexQuery++, gruppo);
                    } else if (actiontype.equalsIgnoreCase(UPDATE_PER_ID_SORGENTE)) {
                        ps.setString(indexQuery++, idsorgente);
                        if (tiposorgente != null) {
                            ps.setString(indexQuery++, tiposorgente);
                        }
                    } else if (actiontype.equalsIgnoreCase(UPDATE_PER_ID_ESTERNO_E_ID_UTENTE)) {
                        ps.setString(indexQuery++, idesterno);
                        ps.setString(indexQuery++, idutenti);
                    } else if (actiontype.equalsIgnoreCase(UPDATE_PER_GRUPPO_E_ID_UTENTE)) {
                        ps.setString(indexQuery++, gruppo);
                        ps.setString(indexQuery++, idutenti);
                    } else if (actiontype.equalsIgnoreCase(UPDATE_PER_ID_SORGENTE_E_ID_UTENTE)) {
                        ps.setString(indexQuery++, idsorgente);
                        ps.setString(indexQuery++, idutenti);
                        if (tiposorgente != null) {
                            ps.setString(indexQuery++, tiposorgente);
                        }
                    } else {
                        ps.setString(indexQuery++, actiontype.equalsIgnoreCase(UPDATE_MULT) ? idattivita.replace("*", "%") : idattivita);
                    }
//                    System.out.println(new java.util.Date(System.currentTimeMillis()).toString() + " : " + "eseguo la query: " + ps.toString() + " ...");
                    log.info("eseguo la query: " + ps.toString() + " ...");
                    updatedRows = ps.executeUpdate();

                    // se richiesto aggiorno anche le attività nello storico
                    if (updatestorico != null && updatestorico.equalsIgnoreCase("true")) {
                        sqlText = "UPDATE " + doneActivityTableName
                                + " SET " + updateString;
                        if (actiontype.equalsIgnoreCase(UPDATE_PER_ID_ESTERNO)) {
                            sqlText += " WHERE id_esterno = ?";
                        } else if (actiontype.equalsIgnoreCase(UPDATE_PER_ID_ESTERNO_E_ID_UTENTE)) {
                            sqlText += " WHERE id_esterno = ? and id_utente = ?";
                        } else if (actiontype.equalsIgnoreCase(UPDATE_PER_GRUPPO)) {
                            sqlText += " WHERE gruppo = ?";
                        } else if (actiontype.equalsIgnoreCase(UPDATE_PER_GRUPPO_E_ID_UTENTE)) {
                            sqlText += " WHERE gruppo = ? and id_utente = ?";
                        } else if (actiontype.equalsIgnoreCase(UPDATE_PER_ID_SORGENTE)) {
                            sqlText += " WHERE id_sorgente = ?" + (tiposorgente != null ? " and tipo_sorgente = ?" : "");
                        } else if (actiontype.equalsIgnoreCase(UPDATE_PER_ID_SORGENTE_E_ID_UTENTE)) {
                            sqlText += " WHERE id_sorgente = ? and id_utente = ?" + (tiposorgente != null ? " and tipo_sorgente = ?" : "");
                        } else {
                            sqlText += " WHERE id_attivita" + (actiontype.equalsIgnoreCase(UPDATE_MULT) ? " like " : " = ") + "?";
                        }
                        ps = dbConn.prepareStatement(sqlText);

                        indexQuery = 1;
                        if (idtipiattivita != null) {
                            ps.setInt(indexQuery++, Integer.parseInt(idtipiattivita));
                        }
                        if (datain != null) {
                            ps.setTimestamp(indexQuery++, new Timestamp(new SimpleDateFormat("dd-MM-yy HH:mm:ss").parse(datain).getTime()));
                        }
                        if (descrizioneattivita != null) {
                            ps.setString(indexQuery++, descrizioneattivita.equalsIgnoreCase("null") ? null : descrizioneattivita);
                        }
                        if (statoattivita != null) {
                            ps.setString(indexQuery++, statoattivita.equalsIgnoreCase("null") ? null : statoattivita);
                        }
                        if (noteattivita != null) {
                            ps.setString(indexQuery++, noteattivita.equalsIgnoreCase("null") ? null : noteattivita);
                        }
                        if (urlcommand != null) {
                            ps.setString(indexQuery++, urlcommand.equalsIgnoreCase("null") ? null : urlcommand);
                        }
                        if (labelurlcommand != null) {
                            ps.setString(indexQuery++, labelurlcommand.equalsIgnoreCase("null") ? null : labelurlcommand);
                        }
                        if (urlcommand2 != null) {
                            ps.setString(indexQuery++, urlcommand2.equalsIgnoreCase("null") ? null : urlcommand2);
                        }
                        if (labelurlcommand2 != null) {
                            ps.setString(indexQuery++, labelurlcommand2.equalsIgnoreCase("null") ? null : labelurlcommand2);
                        }
                        if (urlcommand3 != null) {
                            ps.setString(indexQuery++, urlcommand3.equalsIgnoreCase("null") ? null : urlcommand3);
                        }
                        if (labelurlcommand3 != null) {
                            ps.setString(indexQuery++, labelurlcommand3.equalsIgnoreCase("null") ? null : labelurlcommand3);
                        }
                        if (uuidanteprima != null) {
                            ps.setString(indexQuery++, uuidanteprima.equalsIgnoreCase("null") ? null : uuidanteprima);
                        }
                        if (provenienza != null) {
                            ps.setString(indexQuery++, provenienza.equalsIgnoreCase("null") ? null : provenienza);
                        }
                        if (datascadenza != null) {
                            ps.setTimestamp(indexQuery++, new Timestamp(new SimpleDateFormat("dd-MM-yy HH:mm:ss").parse(datascadenza).getTime()));
                        }
                        if (priorita != null) {
                            ps.setInt(indexQuery++, Integer.parseInt(priorita));
                        }
                        if (oggettoattivita != null) {
                            ps.setString(indexQuery++, oggettoattivita.equalsIgnoreCase("null") ? null : oggettoattivita);
                        }
                        if (customapp1 != null) {
                            ps.setString(indexQuery++, customapp1.equalsIgnoreCase("null") ? null : customapp1);
                        }
                        if (customapp2 != null) {
                            ps.setString(indexQuery++, customapp2.equalsIgnoreCase("null") ? null : customapp2);
                        }
                        if (customapp3 != null) {
                            ps.setString(indexQuery++, customapp3.equalsIgnoreCase("null") ? null : customapp3);
                        }
                        if (customapp4 != null) {
                            ps.setString(indexQuery++, customapp4.equalsIgnoreCase("null") ? null : customapp4);
                        }
                        if (customapp5 != null) {
                            ps.setString(indexQuery++, customapp5.equalsIgnoreCase("null") ? null : customapp5);
                        }
                        if (permesso != null) {
                            ps.setString(indexQuery++, permesso.equalsIgnoreCase("null") ? null : permesso);
                        }

                        if (idriferimento != null) {
                            ps.setString(indexQuery++, idriferimento);
                        }
                        if (tiporiferimento != null) {
                            ps.setString(indexQuery++, tiporiferimento);
                        }

                        if (!actiontype.equalsIgnoreCase(UPDATE_PER_ID_ESTERNO) && !actiontype.equalsIgnoreCase(UPDATE_PER_ID_ESTERNO_E_ID_UTENTE) && idesterno != null) {
                            ps.setString(indexQuery++, idesterno);
                        }
                        if (!actiontype.equalsIgnoreCase(UPDATE_PER_GRUPPO) && !actiontype.equalsIgnoreCase(UPDATE_PER_GRUPPO_E_ID_UTENTE) && gruppo != null) {
                            ps.setString(indexQuery++, gruppo);
                        }
                        if (!actiontype.equalsIgnoreCase(UPDATE_PER_ID_SORGENTE) && !actiontype.equalsIgnoreCase(UPDATE_PER_ID_SORGENTE_E_ID_UTENTE) && idsorgente != null) {
                            ps.setString(indexQuery++, idsorgente);
                            if (tiposorgente != null) {
                                ps.setString(indexQuery++, tiposorgente);
                            }
                        }
                        if (!actiontype.equalsIgnoreCase(UPDATE_PER_ID_ESTERNO_E_ID_UTENTE) && !actiontype.equalsIgnoreCase(UPDATE_PER_GRUPPO_E_ID_UTENTE) && !actiontype.equalsIgnoreCase(UPDATE_PER_ID_SORGENTE_E_ID_UTENTE) && idutenti != null) {
                            ps.setString(indexQuery++, idutenti);
                        }

                        if (actiontype.equalsIgnoreCase(UPDATE_PER_ID_ESTERNO)) {
                            ps.setString(indexQuery++, idesterno);
                        } else if (actiontype.equalsIgnoreCase(UPDATE_PER_GRUPPO)) {
                            ps.setString(indexQuery++, gruppo);
                        } else if (actiontype.equalsIgnoreCase(UPDATE_PER_ID_SORGENTE)) {
                            ps.setString(indexQuery++, idsorgente);
                            if (tiposorgente != null) {
                                ps.setString(indexQuery++, tiposorgente);
                            }
                        } else if (actiontype.equalsIgnoreCase(UPDATE_PER_ID_ESTERNO_E_ID_UTENTE)) {
                            ps.setString(indexQuery++, idesterno);
                            ps.setString(indexQuery++, idutenti);
                        } else if (actiontype.equalsIgnoreCase(UPDATE_PER_GRUPPO_E_ID_UTENTE)) {
                            ps.setString(indexQuery++, gruppo);
                            ps.setString(indexQuery++, idutenti);
                        } else if (actiontype.equalsIgnoreCase(UPDATE_PER_ID_SORGENTE_E_ID_UTENTE)) {
                            ps.setString(indexQuery++, idsorgente);
                            ps.setString(indexQuery++, idutenti);
                            if (tiposorgente != null) {
                                ps.setString(indexQuery++, tiposorgente);
                            }
                        } else {
                            ps.setString(indexQuery++, actiontype.equalsIgnoreCase(UPDATE_MULT) ? idattivita.replace("*", "%") : idattivita);
                        }
                        //                    System.out.println(new java.util.Date(System.currentTimeMillis()).toString() + " : " + "eseguo la query: " + ps.toString() + " ...");
                        log.info("eseguo la query: " + ps.toString() + " ...");
                        updatedRows += ps.executeUpdate();
                    }
                }
            } // cancello dalla scrivania (inserisco nella tabella delle attività svolte)
            else if (actiontype.equalsIgnoreCase(DELETE)
                    || actiontype.equals(DELETE_MULT)
                    || actiontype.equals(DELETE_PER_ID_ESTERNO)
                    || actiontype.equals(DELETE_PER_ID_ESTERNO_E_ID_UTENTE)
                    || actiontype.equals(DELETE_PER_GRUPPO)
                    || actiontype.equals(DELETE_PER_GRUPPO_E_ID_UTENTE)
                    || actiontype.equals(DELETE_PER_ID_SORGENTE)
                    || actiontype.equals(DELETE_PER_ID_SORGENTE_E_ID_UTENTE)) {
                log.info("Cancellazione attivita'");
                boolean okToDelete = false;
                if (archiviazione == null || !archiviazione.equalsIgnoreCase("false")) {
                    sqlText = "INSERT INTO " + doneActivityTableName + " "
                            + "(id_attivita,id_utente,tipo_attivita,id_applicazione,descrizione_attivita,url_command_1,stato_attivita,data_in,"
                            + "data_presaincarico,data_out,note_attivita,url_command_2,url_command_3,label_url_command_1,label_url_command_2,"
                            + "label_url_command_3,uuid_anteprima,provenienza,data_scadenza,priorita,oggetto_attivita, id_set_attivita,id_esterno,"
                            + "custom_app_1,custom_app_2,custom_app_3,custom_app_4,custom_app_5,permesso,id_sorgente,tipo_sorgente,id_riferimento,"
                            + "tipo_riferimento,gruppo) "
                            + "SELECT id_attivita,id_utente,tipo_attivita,id_applicazione,descrizione_attivita,url_command_1,stato_attivita,data_in,"
                            + "data_presaincarico,data_out,note_attivita,url_command_2,url_command_3,label_url_command_1,label_url_command_2,"
                            + "label_url_command_3,uuid_anteprima,provenienza,data_scadenza,priorita,oggetto_attivita, id_set_attivita,id_esterno,"
                            + "custom_app_1,custom_app_2,custom_app_3,custom_app_4,custom_app_5,permesso,id_sorgente,tipo_sorgente,id_riferimento,"
                            + "tipo_riferimento,gruppo "
                            + "FROM " + pendingActivityTableName;
                    if (actiontype.equalsIgnoreCase(DELETE_PER_ID_ESTERNO)) {
                        sqlText += " WHERE id_esterno = ?";
                    } else if (actiontype.equalsIgnoreCase(DELETE_PER_ID_ESTERNO_E_ID_UTENTE)) {
                        sqlText += " WHERE id_esterno = ? and id_utente = ?";
                    } else if (actiontype.equalsIgnoreCase(DELETE_PER_GRUPPO)) {
                        sqlText += " WHERE gruppo = ?";
                    } else if (actiontype.equalsIgnoreCase(DELETE_PER_GRUPPO_E_ID_UTENTE)) {
                        sqlText += " WHERE gruppo = ? and id_utente = ?";
                    } else if (actiontype.equalsIgnoreCase(DELETE_PER_ID_SORGENTE)) {
                        sqlText += " WHERE id_sorgente = ?" + (tiposorgente != null ? " and tipo_sorgente = ?" : "");
                    } else if (actiontype.equalsIgnoreCase(DELETE_PER_ID_SORGENTE_E_ID_UTENTE)) {
                        sqlText += " WHERE id_sorgente = ? and id_utente = ?" + (tiposorgente != null ? " and tipo_sorgente = ?" : "");
                    } else {
                        sqlText += " WHERE id_attivita" + (actiontype.equalsIgnoreCase(DELETE_MULT) ? " like " : " = ") + "?";
                    }
                    //String totalQuery = sqlText;
                    ps = dbConn.prepareStatement(sqlText, new String[]{"id_attivita_seq"});
                    if (actiontype.equalsIgnoreCase(DELETE_PER_ID_ESTERNO)) {
                        ps.setString(1, idesterno);
                    } else if (actiontype.equalsIgnoreCase(DELETE_PER_ID_ESTERNO_E_ID_UTENTE)) {
                        ps.setString(1, idesterno);
                        ps.setString(2, idutenti);
                    } else if (actiontype.equalsIgnoreCase(DELETE_PER_GRUPPO)) {
                        ps.setString(1, gruppo);
                    } else if (actiontype.equalsIgnoreCase(DELETE_PER_GRUPPO_E_ID_UTENTE)) {
                        ps.setString(1, gruppo);
                        ps.setString(2, idutenti);
                    } else if (actiontype.equalsIgnoreCase(DELETE_PER_ID_SORGENTE)) {
                        ps.setString(1, idsorgente);
                        if (tiposorgente != null) {
                            ps.setString(2, tiposorgente);
                        }
                    } else if (actiontype.equalsIgnoreCase(DELETE_PER_ID_SORGENTE_E_ID_UTENTE)) {
                        ps.setString(1, idsorgente);
                        ps.setString(2, idutenti);
                        if (tiposorgente != null) {
                            ps.setString(3, tiposorgente);
                        }
                    } else {
                        ps.setString(1, actiontype.equalsIgnoreCase(DELETE_MULT) ? idattivita.replace("*", "%") : idattivita);
                    }
                    String lastQuery = ps.toString();
                    log.debug("sto eseguendo la query: " + lastQuery);
                    //                System.out.println("sto eseguendo la query: " + lastQuery);
                    int res = ps.executeUpdate();
                    if (res > 0) {
                        ResultSet generatedKeys = ps.getGeneratedKeys();
                        String idToUpdate = "";
                        while (generatedKeys.next()) {
                            int id = generatedKeys.getInt("id_attivita_seq");
                            idToUpdate += "id_attivita_seq = " + id + " or ";
                        }
                        if (!idToUpdate.isEmpty()) {
                            idToUpdate = idToUpdate.substring(0, idToUpdate.length() - 4);
                            sqlText = "UPDATE " + doneActivityTableName
                                    + " SET stato_attivita = ?" + ((noteattivita != null && !noteattivita.equals("")) ? ", note_attivita = ?" : " ") + ", data_out = ? "
                                    + "WHERE " + idToUpdate;
                            //totalQuery += "\n...\n" + sqlText;
                            ps = dbConn.prepareStatement(sqlText);
                            indexQuery = 1;
                            ps.setInt(indexQuery++, ACTIVITY_STATE_ENDED);
                            if (noteattivita != null && !noteattivita.equals("")) {
                                ps.setString(indexQuery++, noteattivita);
                            }
                            ps.setTimestamp(indexQuery++, current_date);
                            lastQuery = ps.toString();
                            log.debug("sto eseguendo la query: " + lastQuery);
                            //                    System.out.println("sto eseguendo la query: " + lastQuery);
                            res = ps.executeUpdate();
                            if (res > 0) {
                                okToDelete = true;
                            }
                        }
                    }
                } else {
                    okToDelete = true;
                }
//                if (!transctionOk)
//                    throw new ServletException("fallita la query: " + lastQuery);

                if (okToDelete) {
                    sqlText = "DELETE "
                            + " FROM " + pendingActivityTableName;
                    if (actiontype.equalsIgnoreCase(DELETE_PER_ID_ESTERNO)) {
                        sqlText += " WHERE id_esterno = ?";
                    } else if (actiontype.equalsIgnoreCase(DELETE_PER_ID_ESTERNO_E_ID_UTENTE)) {
                        sqlText += " WHERE id_esterno = ? and id_utente = ?";
                    } else if (actiontype.equalsIgnoreCase(DELETE_PER_GRUPPO)) {
                        sqlText += " WHERE gruppo = ?";
                    } else if (actiontype.equalsIgnoreCase(DELETE_PER_GRUPPO_E_ID_UTENTE)) {
                        sqlText += " WHERE gruppo = ? and id_utente = ?";
                    } else if (actiontype.equalsIgnoreCase(DELETE_PER_ID_SORGENTE)) {
                        sqlText += " WHERE id_sorgente = ?" + (tiposorgente != null ? " and tipo_sorgente = ?" : "");
                    } else if (actiontype.equalsIgnoreCase(DELETE_PER_ID_SORGENTE_E_ID_UTENTE)) {
                        sqlText += " WHERE id_sorgente = ? and id_utente = ?" + (tiposorgente != null ? " and tipo_sorgente = ?" : "");
                    } else {
                        sqlText += " WHERE id_attivita" + (actiontype.equalsIgnoreCase(DELETE_MULT) ? " like " : " = ") + "?";
                    }

                    //totalQuery += "\n...\n" + sqlText;
                    ps = dbConn.prepareStatement(sqlText);
                    if (actiontype.equalsIgnoreCase(DELETE_PER_ID_ESTERNO)) {
                        ps.setString(1, idesterno);
                    } else if (actiontype.equalsIgnoreCase(DELETE_PER_ID_ESTERNO_E_ID_UTENTE)) {
                        ps.setString(1, idesterno);
                        ps.setString(2, idutenti);
                    } else if (actiontype.equalsIgnoreCase(DELETE_PER_GRUPPO)) {
                        ps.setString(1, gruppo);
                    } else if (actiontype.equalsIgnoreCase(DELETE_PER_GRUPPO_E_ID_UTENTE)) {
                        ps.setString(1, gruppo);
                        ps.setString(2, idutenti);
                    } else if (actiontype.equalsIgnoreCase(DELETE_PER_ID_SORGENTE)) {
                        ps.setString(1, idsorgente);
                        if (tiposorgente != null) {
                            ps.setString(2, tiposorgente);
                        }
                    } else if (actiontype.equalsIgnoreCase(DELETE_PER_ID_SORGENTE_E_ID_UTENTE)) {
                        ps.setString(1, idsorgente);
                        ps.setString(2, idutenti);
                        if (tiposorgente != null) {
                            ps.setString(3, tiposorgente);
                        }
                    } else {
                        ps.setString(1, actiontype.equalsIgnoreCase(DELETE_MULT) ? idattivita.replace("*", "%") : idattivita);
                    }
                    log.debug("sto eseguendo la query: " + ps.toString());
//                        System.out.println("sto eseguendo la query: " + lastQuery);
                    int res = ps.executeUpdate();
                    updatedRows = res;
//                        if (res > 0) {
//                            transctionOk = true;
//                        }
                }
            }

//            System.out.println(new java.util.Date(System.currentTimeMillis()).toString() + " : " + "eseguo il commit dell'operazione...");
            log.info("eseguo il commit dell'operazione...");
            dbConn.commit();

            if (updatedRows == 0) {
                String message = "Nessuna attività modificata/cancellata/aggiunta";
                log.warn(message);
//                System.out.println(new java.util.Date(System.currentTimeMillis()).toString() + " : " + message);
            } else {
                String message = updatedRows + " attività modificate/cancellate/aggiunte";
                log.warn(message);
//                System.out.println(new java.util.Date(System.currentTimeMillis()).toString() + " : " + message);
            }

            sendRefreshActivitiesCommand(dbConn, request.getSession().getId(), usersToRefresh, usersTableName);

            try ( // tutto ok, genero l'html di conferma
                    PrintWriter out = response.getWriter()) {
                out.println("<html>");
                out.println("<head>");
                out.println("<title>" + getClass().getName() + "</title>");
                out.println("</head>");
                out.println("<body>");
                out.println("<h1>operazione completata</h1>");
                if (updatedRows == 0) {
                    out.println("nessuna attività modificata/cancellata/aggiunta");
                } else {
                    out.println(updatedRows + " attività modificate/cancellate/aggiunte");
                }
                out.println("<br/>");
                out.println("query eseguita:");
                out.println("<br/>");
                out.println(ps.toString());
                out.println("</body>");
                out.println("</html>");
            }
        } catch (Exception ex) {
            log.fatal("Errore", ex);
            log.info("Stampo i parametri della richiesta:");
            log.info("idapplicazione: " + idapplicazione);
            log.info("idattivita: " + idattivita);
            log.info("idesterno: " + idesterno);
            log.info("idutenti: " + idutenti);
            log.info("idtipiattivita: " + idtipiattivita);
            log.info("descrizioneattivita: " + descrizioneattivita);
            log.info("urlcommand: " + urlcommand);
            log.info("datain: " + datain);
            log.info("statoattivita: " + statoattivita);
            log.info("noteattivita: " + noteattivita);
            log.info("actiontype: " + actiontype);
            log.info("labelurlcommand: " + labelurlcommand);
            log.info("urlcommand2: " + urlcommand2);
            log.info("labelurlcommand2: " + labelurlcommand2);
            log.info("urlcommand3: " + urlcommand3);
            log.info("labelurlcommand3: " + labelurlcommand3);
            log.info("uuidanteprima: " + uuidanteprima);
            log.info("provenienza: " + provenienza);
            log.info("oggettoattivita: " + oggettoattivita);
            log.info("priorita: " + priorita);
            log.info("datascadenza: " + datascadenza);
            log.info("customapp1: " + customapp1);
            log.info("customapp2: " + customapp2);
            log.info("customapp3: " + customapp3);
            log.info("customapp4: " + customapp4);
            log.info("customapp5: " + customapp5);
            log.info("permesso: " + permesso);
            log.info("idsorgente: " + idsorgente);
            log.info("tiposorgente: " + tiposorgente);
            log.info("idriferimento: " + idriferimento);
            log.info("tiporiferimento: " + tiporiferimento);
            log.info("gruppo: " + gruppo);

            log.info("updatestorico: " + updatestorico);

            log.info("setattivita: " + setattivita);
            log.info("setaccessoesclusivo: " + setaccessoesclusivo);

            try {
                if (dbConn != null) {
                    dbConn.rollback();
                }
            } catch (SQLException subEx) {
//                subEx.printStackTrace(System.out);
                log.fatal("Errore nel rollback dell'operazione", subEx);
//                log.fatal(subEx);
            }
            throw new ServletException(ex);
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
                if (dbConn != null) {
                    dbConn.close();
                }
            } catch (Exception subEx) {
                log.fatal("errore nella chiusura delle connessioni al database:", subEx);
//                log.fatal(subEx);
            }
        }
    }

    /**
     * Controlla che i dati di autenticazione nell'header della richiesta
     * corrispondano a i dati passati
     *
     * @param auth parte dell'header contenente i dati di autenticazione della
     * richiesta
     * @param username username autorizzato
     * @param password autorizzata
     * @return true se username e password letti da "auth" corrsipondono a
     * quelli passati, false altrimenti
     * @throws IOException
     */
    protected boolean allowedUser(String auth, String username, String password) throws IOException {

        if (auth == null) {
            return false;
        }

        // no auth
        if (!auth.toUpperCase().startsWith("BASIC ")) {
            return false;
        }
        // we only do BASIC
        // Get encoded user and password, comes after "BASIC "
        String userpassEncoded = auth.substring(6);
        // Decode it, using any base 64 decoder
//        sun.misc.BASE64Decoder dec = new sun.misc.BASE64Decoder();

        String userpassDecoded = new String(Base64.getDecoder().decode(userpassEncoded));
//        String userpassDecoded = new String(dec.decodeBuffer(userpassEncoded));
        // Check our user list to see if that user and password are "allowed"

        String userpassword = username + ":" + password;
        return userpassDecoded.equals(userpassword);
    }

//    private Connection connectToPostgresDB(String dbUrl, String dbName, String username, String password) throws SQLException {
//    Connection dbConn = null;
//
//        // Carico il driver JDBC per la connessione con il database
//        try {
//            Class.forName("org.postgresql.Driver");
//        }
//        catch (ClassNotFoundException ex) {
//            ex.printStackTrace(System.out);
//            return null;
//        }
//
//        // Controllo se il nome utente va usato o meno per la connessione
//        if (username == null) {
//            // La connessione non richiede nome utente e password
//            dbConn = DriverManager.getConnection(dbUrl + dbName);
//        }
//        else {
//           // La connessione richiede nome utente, controllo se necessita anche della password
//           if (password == null) {
//           // La connessione non necessita di password
//           dbConn = DriverManager.getConnection(dbUrl + dbName + "?user=" + username);
//           }
//            else {
//                // La connessione necessita della password
//                dbConn = DriverManager.getConnection(dbUrl + dbName + "?user=" + username + "&password=" + password);
//           }
//        }
//
//        DatabaseMetaData dbMetaData = dbConn.getMetaData();
//        //System.out.println("Connessione a " + dbMetaData.getDatabaseProductName() + " " + dbMetaData.getDatabaseProductVersion() + " riuscita");
//        return dbConn;
//    }
    private List<String> getUsersForRefreshActivitiesCommand(Connection dbConn, String sessionId, String pendingActivityTableName, String acionType, String idUtente, String idAttivita, String idEsterno, String gruppo, String idSorgente, String tipoOggetto) throws ServletException {
        // calcolo degli utenti da notificare
        List<String> users = new ArrayList<>();
        try {
            String carattereConfronto = null;
            String nomeColonnaConfronto = null;
            String id = null;
            switch (acionType) {
                case UPDATE:
                case DELETE:
                case INSERT:
                    nomeColonnaConfronto = "id_attivita";
                    carattereConfronto = "=";
                    id = idAttivita;
                    break;
                case UPDATE_MULT:
                case DELETE_MULT:
                    nomeColonnaConfronto = "id_attivita";
                    carattereConfronto = "like";
                    id = idAttivita;
                    id = id.replace("*", "%");
                    break;
                case UPDATE_PER_ID_ESTERNO:
                case DELETE_PER_ID_ESTERNO:
                case UPDATE_PER_ID_ESTERNO_E_ID_UTENTE:
                case DELETE_PER_ID_ESTERNO_E_ID_UTENTE:
                    nomeColonnaConfronto = "id_esterno";
                    carattereConfronto = "=";
                    id = idEsterno;
                    break;
                case UPDATE_PER_GRUPPO:
                case DELETE_PER_GRUPPO:
                case UPDATE_PER_GRUPPO_E_ID_UTENTE:
                case DELETE_PER_GRUPPO_E_ID_UTENTE:
                    nomeColonnaConfronto = "gruppo";
                    carattereConfronto = "=";
                    id = gruppo;
                    break;
                case UPDATE_PER_ID_SORGENTE:
                case DELETE_PER_ID_SORGENTE:
                case UPDATE_PER_ID_SORGENTE_E_ID_UTENTE:
                case DELETE_PER_ID_SORGENTE_E_ID_UTENTE:
                    nomeColonnaConfronto = "id_sorgente";
                    carattereConfronto = "=";
                    id = idSorgente;
                    break;
            }

            String sqlText = "SELECT DISTINCT id_utente"
                    + " FROM " + pendingActivityTableName
                    + " WHERE " + nomeColonnaConfronto + " " + carattereConfronto + " ?";
            if (idUtente != null && !idUtente.equals("")) {
                sqlText += " and id_utente = ?";
            }
            // temporaneo, da togliere quando aggiorneremo i processi (ora non scrivono il tipoOggetto)
            tipoOggetto = null;
            if (tipoOggetto != null && !tipoOggetto.equals("")) {
                sqlText += " and tipo_sorgente = ?";
            }
            PreparedStatement ps = dbConn.prepareStatement(sqlText);
            int indexQuery = 1;
            ps.setString(indexQuery++, id);
            if (idUtente != null && !idUtente.equals("")) {
                ps.setString(indexQuery++, idUtente);
            }
            if (tipoOggetto != null && !tipoOggetto.equals("")) {
                ps.setString(indexQuery++, tipoOggetto);
            }

            log.info("eseguo la query: " + ps.toString());
            ResultSet result = ps.executeQuery();
            log.info("query res: ");
            while (result.next()) {
                log.info(result.getString(1));
                users.add(result.getString(1));
            }
        } catch (Exception ex) {
            throw new ServletException(ex);
        }
        return users;
    }

    private void sendRefreshActivitiesCommand(Connection dbConn, String sessionId, List<String> users, String usersTableName) throws ServletException {
        if (!users.isEmpty()) {
            try {
                String segnaposto = "";
                for (int i = 0; i < users.size(); i++) {
                    segnaposto += "?,";
                }
                segnaposto = segnaposto.substring(0, segnaposto.length() - 1);
                String q = "SELECT cf FROM " + usersTableName + " where id_utente in (" + segnaposto + ")";
                List<String> codiciFiscali = new ArrayList<>();
                ResultSet rs = null;
                try (PreparedStatement ps = dbConn.prepareStatement(q)) {
                    for (int i = 0; i < users.size(); i++) {
                        ps.setString(i + 1, users.get(i));
                    }
                    rs = ps.executeQuery();
                    while (rs.next()) {
                        codiciFiscali.add(rs.getString(1));
                    }
                } finally {
                    if (rs != null) {
                        rs.close();
                    }
                }

                UtilityFunctions.sendRefreshActivitiesCommand(codiciFiscali, sessionId);
            } catch (Exception ex) {
                throw new ServletException(ex);
            }
        } else {
            log.warn("no users to refresh");
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Iserisce, aggiorna o cancella un attività secondo i parametri passati con
     * metodo GET alla richiesta
     *
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response, NORMAL_ACTION);
    }

    /**
     * Iserisce, aggiorna o cancella un attività secondo i parametri passati con
     * metodo POST alla richiesta
     *
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response, NORMAL_ACTION);
    }

    /**
     * Cancella attività multiple dalla scrivania virtuale secondo una
     * condizione "like" sul'idattivita
     *
     * Handles the HTTP <code>DELETE</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response, ONLY_CLEAN_ACTION);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Servlet per l'inserimento, aggiornamento o cancellazione delle attività dalla scrivania virtuale";
    }// </editor-fold>
}
