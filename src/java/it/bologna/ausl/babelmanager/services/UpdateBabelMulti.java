package it.bologna.ausl.babelmanager.services;

import it.bologna.ausl.babelmanager.exceptions.NotAuthorizedException;
import it.bologna.ausl.babelmanager.utils.ApplicationParams;
import it.bologna.ausl.babelmanager.utils.UtilityFunctions;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 *
 * @author Giuseppe De Marco (gdm)
 */
public class UpdateBabelMulti extends HttpServlet {

    private static final Logger log = Logger.getLogger(UpdateBabelMulti.class);
    private final int ACTIVITY_STATE_NEW = 1;
    private final int SQL_TRUE = -1;
    private final int SQL_FALSE = 0;
    private final String SQL_EXCEPTION_DUPLICATED_ITEM = "23";

    private final int TYPE_STRING = 0;
    private final int TYPE_TIMESTAMP = 1;
    private final int TYPE_INT = 2;

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("utf-8");
        // configuro il logger per la console
//    BasicConfigurator.configure();
        log.info("-------------------------------");
        log.info("Avvio servlet: " + getClass().getSimpleName());
        log.info("-------------------------------");
        Timestamp current_date = new Timestamp(System.currentTimeMillis());
        Connection dbConn = null;
        PreparedStatement ps = null;
        String idapplicazione = null;
        String tokenapplicazione = null;
        String attivita = null;
        String setattivita = null;
        String setaccessoesclusivo = null;
        String archiviazione = null;

        try {
            // leggo le proprietà dal web.xml
            String authenticationTable = getServletContext().getInitParameter("authenticationTable");
            String pendingActivityTableName = getServletContext().getInitParameter("pendingActivityTableName");
            String doneActivityTableName = getServletContext().getInitParameter("doneActivityTableName");
            String activitiesSetTableName = getServletContext().getInitParameter("activitiesSetTableName");
            String usersTableName = getServletContext().getInitParameter("usersTableName");

            if (pendingActivityTableName == null || pendingActivityTableName.equals("")) {
                String message = "Manca il nome della tabella in cui inserire le attività. Indicare il nome della tabella all'interno del \"web.xml\"";
                log.error(message);
                throw new ServletException(message);
            } else if (doneActivityTableName == null || doneActivityTableName.equals("")) {
                String message = "Manca il nome della tabella in cui inserire le attività svolte. Indicare il nome della tabella all'interno del \"web.xml\"";
                log.error(message);
                throw new ServletException(message);
            } else if (activitiesSetTableName == null || activitiesSetTableName.equals("")) {
                String message = "Manca il nome della tabella che raccoglie i set di attività. Indicare il nome della tabella all'interno del \"web.xml\"";
                log.error(message);
                throw new ServletException(message);
            }

            // leggo i parametri per l'aggiornamento della scrivania virtuale dalla richiesta HTTP
            // dati per l'autenticazione
            idapplicazione = request.getParameter("idapplicazione");
            tokenapplicazione = request.getParameter("tokenapplicazione");

            // dati per l'aggiornamento di Babel
            attivita = request.getParameter("attivita");
            setattivita = request.getParameter("setattivita");
            setaccessoesclusivo = request.getParameter("setaccessoesclusivo");
            archiviazione = request.getParameter("archiviazione");

            if (idapplicazione != null && idapplicazione.equals("")) {
                idapplicazione = null;
            }
            if (tokenapplicazione != null && tokenapplicazione.equals("")) {
                tokenapplicazione = null;
            }
            if (attivita != null && attivita.equals("")) {
                attivita = null;
            }
            if (setattivita != null && setattivita.equals("")) {
                setattivita = null;
            }

            int setaccessoesclusivo_int = -1;
            if (setaccessoesclusivo != null) {
                if (setaccessoesclusivo.equalsIgnoreCase("true")) {
                    setaccessoesclusivo_int = SQL_TRUE;
                } else if (setaccessoesclusivo.equalsIgnoreCase("false")) {
                    setaccessoesclusivo_int = SQL_FALSE;
                } else {
                    String message = "Parametro \"setaccessoesclusivo\" errato: i valori possibili sono \"true\" o \"false\"";
                    log.error(message);
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    try {
                        dbConn.close();
                    } catch (Exception ex) {
                    }
                    return;
                }
            }
            if (archiviazione != null && archiviazione.equals("")) {
                archiviazione = null;
            }

            log.debug("dati ricevuti:");
            log.debug("idapplicazione: " + idapplicazione);
            log.debug("attivita: " + attivita);
            log.debug("setattivita: " + setattivita);
            log.debug("setaccessoesclusivo: " + setaccessoesclusivo);
            log.debug("archiviazione: " + archiviazione);

            // controllo se mi sono stati passati i dati per l'autenticazione
            if (idapplicazione == null || tokenapplicazione == null) {
                String message = "Dati di autenticazione errati, specificare i parametri \"idapplicazione\" e \"tokenapplicazione\" nella richiesta";
                log.error(message);
                throw new ServletException(message);
            }

            if (attivita == null) {
                String message = "Manca il parametro \"attivita\". Indicarlo nei parametri della richiesta";
                log.error(message);
                throw new ServletException(message);
            }

            if (setattivita == null) {
                String message = "Manca il parametro \"set\". Indicarlo nei parametri della richiesta";
                log.error(message);
                throw new ServletException(message);
            }

            if (archiviazione == null) {
                String message = "Manca il parametro \"archiviazione\". Indicarlo nei parametri della richiesta";
                log.error(message);
                throw new ServletException(message);
            }

            // apro una connessione verso il db
            //dbConn = connectToPostgresDB(dbUrl, dbName, dbUsername, dbPassword);
            dbConn = UtilityFunctions.getDBConnection();

            // controllo se l'applicazione è autorizzata
            dbConn.setAutoCommit(true);
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
            dbConn.setAutoCommit(false);
            // aggiungo il set_attivita se non esiste già
            Savepoint savepoint = null;
            String sqlText = "INSERT INTO " + activitiesSetTableName
                    + "(id_set_attivita, descrizione_set" + (setaccessoesclusivo != null ? ", accesso_me" : "") + ")"
                    + "VALUES (?, ?" + (setaccessoesclusivo != null ? ", ?" : "") + ")";
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

//            String aaa = "select * from attivita where a=? and b=? and c=? and d=? and e=?";
//            PreparedStatement prepareStatement = dbConn.prepareStatement(aaa);
//            prepareStatement.setString(1, "");
//            prepareStatement.setTimestamp(2, new Timestamp(new SimpleDateFormat("dd-MM-yy").parse("25-03-2012").getTime()));
//            prepareStatement.setNull(3, java.sql.Types.INTEGER);
//            prepareStatement.setTimestamp(4, null);
//            prepareStatement.setNull(5, java.sql.Types.CHAR);
//            System.out.println(prepareStatement.toString());
            sqlText = "select * from allinea_attivita(array[";

            JSONArray attivitaArray = (JSONArray) JSONValue.parse(attivita);
            // NomeParamentro(String), ValoreParametro(Dipende(String o Timestamp)), TipoParamentro(int())
            Object[] param;
            ArrayList params = new ArrayList();
            int line = 0;
            for (Iterator it = attivitaArray.iterator(); it.hasNext();) {
                JSONObject attivitaMap = (JSONObject) it.next();

                String idattivita = (String) attivitaMap.get("idattivita"); //
                String idutenti = (String) attivitaMap.get("idutenti");
                String idtipiattivita = (String) attivitaMap.get("idtipiattivita");//
                // id_applicazione //
                String descrizioneattivita = (String) attivitaMap.get("descrizioneattivita");
                String urlcommand = (String) attivitaMap.get("urlcommand");
                String statoattivita = (String) attivitaMap.get("statoattivita"); //
                String datain = (String) attivitaMap.get("datain"); //
                // data_presa_in_carico
                // data_out
                String noteattivita = (String) attivitaMap.get("noteattivita");
                String urlcommand2 = (String) attivitaMap.get("urlcommand2");
                String urlcommand3 = (String) attivitaMap.get("urlcommand3");
                String labelurlcommand = (String) attivitaMap.get("labelurlcommand");
                String labelurlcommand2 = (String) attivitaMap.get("labelurlcommand2");
                String labelurlcommand3 = (String) attivitaMap.get("labelurlcommand3");
                String uuidanteprima = (String) attivitaMap.get("uuidanteprima");
                String provenienza = (String) attivitaMap.get("provenienza");
                String datascadenza = (String) attivitaMap.get("datascadenza");
                String priorita = (String) attivitaMap.get("priorita"); // int
                if (priorita != null && priorita.equals("")) {
                    priorita = null;
                }
                String oggettoattivita = (String) attivitaMap.get("oggettoattivita");
                String idesterno = (String) attivitaMap.get("idesterno");

                String customapp1 = (String) attivitaMap.get("customapp1");
                String customapp2 = (String) attivitaMap.get("customapp2");
                String customapp3 = (String) attivitaMap.get("customapp3");
                String customapp4 = (String) attivitaMap.get("customapp4");
                String customapp5 = (String) attivitaMap.get("customapp5");

                String permesso = (String) attivitaMap.get("permesso");

                String idsorgente = (String) attivitaMap.get("idsorgente");
                String tiposorgente = (String) attivitaMap.get("tiposorgente");
                String idriferimento = (String) attivitaMap.get("idriferimento");
                String tiporiferimento = (String) attivitaMap.get("tiporiferimento");
                String gruppo = (String) attivitaMap.get("gruppo");

                log.debug("processo la riga:\n" + attivitaMap.toJSONString());
                if (idattivita == null) {
                    log.error("parametro idattivita mancante, la riga verrà ignorata");
                    log.info("valori:\n" + attivitaMap.toJSONString());
                } else if (idtipiattivita == null) {
                    log.error("parametro idtipiattivita mancante, la riga verrà ignorata");
                    log.info("valori:\n" + attivitaMap.toJSONString());
                } else {
                    StringBuilder builder = null;
                    line++;
                    if (line == 1) {
                        builder = new StringBuilder("(");
                    } else {
                        builder = new StringBuilder(",(");
                    }

                    // completo l'id attività aggiungendoci come prefisso l'idapplicazione
                    idattivita = prefix + "_" + idattivita;
                    builder.append("?");
                    param = new Object[]{idattivita, Types.VARCHAR};
                    params.add(param);

                    builder.append(",?");
                    param = new Object[]{idutenti, Types.VARCHAR};
                    params.add(param);

                    builder.append(",?");
                    param = new Object[]{idtipiattivita, Types.VARCHAR};
                    params.add(param);

                    builder.append(",?");
                    param = new Object[]{idapplicazione, Types.VARCHAR};
                    params.add(param);

                    builder.append(",?");
                    param = new Object[]{descrizioneattivita, Types.VARCHAR};
                    params.add(param);

                    builder.append(",?");
                    param = new Object[]{urlcommand, Types.VARCHAR};
                    params.add(param);

                    builder.append(",?");
                    if (statoattivita != null) {
                        param = new Object[]{statoattivita, Types.VARCHAR};
                    } else {
                        param = new Object[]{String.valueOf(ACTIVITY_STATE_NEW), Types.VARCHAR};
                    }
                    params.add(param);

                    builder.append(",?");
                    if (datain != null) {
                        param = new Object[]{new Timestamp(new SimpleDateFormat("dd-MM-yy").parse(datain).getTime()), Types.TIMESTAMP};
                    } else {
                        param = new Object[]{current_date, Types.TIMESTAMP};
                    }
                    params.add(param);

                    builder.append(",?");
                    param = new Object[]{null, Types.TIMESTAMP}; // data_presa_in_carico
                    params.add(param);

                    builder.append(",?");
                    param = new Object[]{null, Types.TIMESTAMP}; //data_out
                    params.add(param);

                    builder.append(",?");
                    param = new Object[]{noteattivita, Types.VARCHAR};
                    params.add(param);

                    builder.append(",?");
                    param = new Object[]{urlcommand2, Types.VARCHAR};
                    params.add(param);

                    builder.append(",?");
                    param = new Object[]{urlcommand3, Types.VARCHAR};
                    params.add(param);

                    builder.append(",?");
                    param = new Object[]{labelurlcommand, Types.VARCHAR};
                    params.add(param);

                    builder.append(",?");
                    param = new Object[]{labelurlcommand2, Types.VARCHAR};
                    params.add(param);

                    builder.append(",?");
                    param = new Object[]{labelurlcommand3, Types.VARCHAR};
                    params.add(param);

                    builder.append(",?");
                    param = new Object[]{uuidanteprima, Types.VARCHAR};
                    params.add(param);

                    builder.append(",?");
                    param = new Object[]{provenienza, Types.VARCHAR};
                    params.add(param);

                    builder.append(",?");
                    if (datascadenza != null) {
                        param = new Object[]{new Timestamp(new SimpleDateFormat("dd-MM-yy").parse(datascadenza).getTime()), Types.TIMESTAMP};
                    } else {
                        param = new Object[]{null, Types.TIMESTAMP};
                    }
                    params.add(param);

                    builder.append(",?");
                    if (priorita != null) {
                        param = new Object[]{Integer.parseInt(priorita), Types.INTEGER};
                    } else {
                        param = new Object[]{null, Types.INTEGER};
                    }
                    params.add(param);

                    builder.append(",?");
                    param = new Object[]{oggettoattivita, Types.VARCHAR};
                    params.add(param);

                    builder.append(",?");
                    param = new Object[]{setattivita, Types.VARCHAR};
                    params.add(param);

                    builder.append(",?");
                    param = new Object[]{idesterno, Types.VARCHAR};
                    params.add(param);

                    builder.append(",?");
                    param = new Object[]{customapp1, Types.VARCHAR};
                    params.add(param);

                    builder.append(",?");
                    param = new Object[]{customapp2, Types.VARCHAR};
                    params.add(param);

                    builder.append(",?");
                    param = new Object[]{customapp3, Types.VARCHAR};
                    params.add(param);

                    builder.append(",?");
                    param = new Object[]{customapp4, Types.VARCHAR};
                    params.add(param);

                    builder.append(",?");
                    param = new Object[]{customapp5, Types.VARCHAR};
                    params.add(param);

                    builder.append(",?");
                    param = new Object[]{permesso, Types.VARCHAR};
                    params.add(param);

                    builder.append(",?");
                    param = new Object[]{idsorgente, Types.VARCHAR};
                    params.add(param);

                    builder.append(",?");
                    param = new Object[]{tiposorgente, Types.VARCHAR};
                    params.add(param);

                    builder.append(",?");
                    param = new Object[]{idriferimento, Types.VARCHAR};
                    params.add(param);

                    builder.append(",?");
                    param = new Object[]{tiporiferimento, Types.VARCHAR};
                    params.add(param);

                    builder.append(",?");
                    param = new Object[]{gruppo, Types.VARCHAR};
                    params.add(param);

                    // aggiungiamo il valore null per la colonna ts_col che non viene passata dall'applicazione
                    builder.append(",?");
                    param = new Object[]{null, Types.ARRAY};
                    params.add(param);

                    builder.append(")");
                    sqlText += builder;
                }
            }
            sqlText += "]::attivita[],'" + setattivita + "'," + Boolean.parseBoolean(archiviazione) + ")";
            ps = dbConn.prepareStatement(sqlText);

            for (int i = 1; i < params.size() + 1; i++) {
                param = (Object[]) params.get(i - 1);
                Object value = param[0];
                int type = (Integer) param[1];

                if (param[0] == null) {
                    ps.setNull(i, type);
                } else {
                    switch (type) {
                        case Types.INTEGER:
                            int intValue = (Integer) value;
                            ps.setInt(i, intValue);
                            break;
                        case Types.TIMESTAMP:
                            Timestamp timeStampValue = (Timestamp) value;
                            ps.setTimestamp(i, timeStampValue);
                            break;
                        case Types.VARCHAR:
                            String stringValue = (String) value;
                            ps.setString(i, stringValue);
                    }
                }
            }
            log.debug("eseguo la query: " + ps.toString());
            ps.execute();
            dbConn.commit();

            sendRefreshActivitiesCommand(dbConn, request.getSession().getId(), pendingActivityTableName, doneActivityTableName, setattivita, usersTableName);

            //            String sqlText = "select * from allinea_attivita(array[";
//            select * from allinea_attivita(array[
//('honita_processUUID:Protocollo_in_uscita--0.0.8--5;activityUUID:Protocollo_in_uscita--0.0.8--5--Redazione--it1f263a16-a23d-4ee2-992c-788044ceb0e0--mainActivityInstance--noLoop;activityName:Redazione;userId:tascone','tascone',1,'bonita','Redazione','[ProctonUrl]?CMD=Redazione;Protocollo_in_uscita--0.0.8--5;Protocollo_in_uscita--0.0.8--5--Redazione--it1f263a16-a23d-4ee2-992c-788044ceb0e0--mainActivityInstance--noLoop;T^1H]WCVHixolJjXpK/p','1','2012-03-01 18:01:29.688',null,null,'','','','Avvia Redazione','','','','',null,null,'','stocazzo'),
//('ionita_processUUID:Protocollo_in_uscita--0.0.8--5;activityUUID:Protocollo_in_uscita--0.0.8--5--Redazione--it1f263a16-a23d-4ee2-992c-788044ceb0e0--mainActivityInstance--noLoop;activityName:Redazione;userId:tascone','tascone',1,'bonita','Redazione','[ProctonUrl]?CMD=Redazione;Protocollo_in_uscita--0.0.8--5;Protocollo_in_uscita--0.0.8--5--Redazione--it1f263a16-a23d-4ee2-992c-788044ceb0e0--mainActivityInstance--noLoop;T^1H]WCVHixolJjXpK/p','1','2012-03-01 18:01:29.688',null,null,'','','','Avvia Redazione','','','','',null,null,'','stocazzo')]::attivita[],
//'stocazzo', true)
//
//
//select * from allinea_attivita(array[]::attivita[], 'stocazzo', true)
        } catch (Exception ex) {
            log.error("Errore", ex);
            log.info("Stampo i parametri della richiesta:");
            log.info("idapplicazione: " + idapplicazione);
            log.info("attivita: " + attivita);
            log.info("setattivita: " + setattivita);
            log.info("setaccessoesclusivo: " + setaccessoesclusivo);
            log.info("archiviazione: " + archiviazione);
            try {
                if (dbConn != null) {
                    dbConn.rollback();
                }
            } catch (SQLException subEx) {
                log.fatal("Errore nel rollback dell'operazione", subEx);
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
            }
        }
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Servlet " + getClass().getSimpleName() + "</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>Operazione eseguita correttamente</h1>");
            out.println("</body>");
            out.println("</html>");
        }
    }

    private void sendRefreshActivitiesCommand(Connection dbConn, String sessionId, String pendingActivityTableName, String doneActivityTableName, String setAttivita, String usersTableName) throws ServletException {
        // calcolo degli utenti da notificare, saranno tutti gli utenti delle attività nel set
        String sqlText = 
                "WITH utenti AS ("
                + " SELECT DISTINCT id_utente"
                + " FROM " + pendingActivityTableName
                + " WHERE id_set_attivita = ?"
                + " UNION "
                + " SELECT DISTINCT id_utente"
                + " FROM " + doneActivityTableName
                + " WHERE id_set_attivita = ?"
                + " )"
                + " SELECT cf"
                + " FROM " + usersTableName + " u"
                + " JOIN utenti on utenti.id_utente = u.id_utente";
        List<String> users = new ArrayList<>();
        try {
            PreparedStatement ps = dbConn.prepareStatement(sqlText);
            ps.setString(1, setAttivita);
            ps.setString(2, setAttivita);
            log.info("eseguo la query: " + ps.toString());
            ResultSet result = ps.executeQuery();
            while (result.next()) {
                users.add(result.getString(1));
            }
        } catch (Exception ex) {
            throw new ServletException(ex);
        }

        if (!users.isEmpty()) {
            try {
                UtilityFunctions.sendRefreshActivitiesCommand(users, sessionId);
            } catch (Exception ex) {
                throw new ServletException(ex);
            }
        } else {
            log.warn("no users to refresh");
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
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
        processRequest(request, response);
    }

    /**
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
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>
}
