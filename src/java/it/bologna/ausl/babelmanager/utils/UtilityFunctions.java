package it.bologna.ausl.babelmanager.utils;

/**
 *
 * @author Giuseppe De Marco (gdm)
 */
import it.bologna.ausl.babelmanager.exceptions.NotAuthorizedException;
import it.bologna.ausl.masterchefclient.JobParams;
import it.bologna.ausl.masterchefclient.PrimusCommanderParams;
import it.bologna.ausl.masterchefclient.WorkerData;
import it.bologna.ausl.primuscommanderclient.PrimusCommand;
import it.bologna.ausl.primuscommanderclient.PrimusCommandParams;
import it.bologna.ausl.primuscommanderclient.PrimusMessage;
import it.bologna.ausl.primuscommanderclient.RefreshActivitiesCommandParams;
import it.bologna.ausl.redis.RedisClient;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.apache.log4j.Logger;

public class UtilityFunctions {

    private static final Logger log = Logger.getLogger(UtilityFunctions.class);
    private static Context initContext;
    
//    public static Connection getDBConnection() throws SQLException, NamingException {
//        Context initContext = new InitialContext();
//        Context envContext = (Context) initContext.lookup("java:/comp/env");
//        DataSource ds = (DataSource) envContext.lookup("jdbc/SVdb");
//        Connection conn = ds.getConnection();
//        return conn;
//    }
    
    public synchronized static Connection getDBConnection() throws SQLException, NamingException {
        if (initContext == null)
            initContext = new InitialContext();
        Context envContext = (Context) initContext.lookup("java:/comp/env");
        DataSource ds = (DataSource) envContext.lookup("jdbc/SVdb");
        Connection conn = ds.getConnection();
        return conn;
    }

    public static String getPubblicParameter(Connection dbConn, String parameterName) throws SQLException {
        String query = "SELECT val_parametro FROM " + ApplicationParams.getPublicParametersTableName() + " WHERE nome_parametro = ?";
        PreparedStatement ps = dbConn.prepareStatement(query);
        ps.setString(1, parameterName);

        ResultSet result = ps.executeQuery();
        String value = null;

        if (result != null && result.next() == true) {
            value = result.getString(1);
        }
        return value;
    }

    /**
     * Controlla se l'applicazione è autorizzata e ne ritorna il prefisso da usare nella costruzione degli id
     * @param dbConn connessione
     * @param idApplicazione id applicazione della quale verificare l'autenticazione
     * @param token token corrispondente all'id applicazione della quale verificare l'autenticazione
     * @return se l'applicazione è autorizzata torna il prefisso da usare nella costruzione degli id, se non è autorizzata torna NotAuthorizedException
     * @throws it.bologna.ausl.babelmanager.exceptions.NotAuthorizedException se l'applicazione non è autorizzata
     * @throws java.sql.SQLException
     */
    public static String checkAuthentication(Connection dbConn, String idApplicazione, String token) throws NotAuthorizedException, SQLException {
        String sqlText = 
                    "SELECT prefix " +
                    "FROM " + ApplicationParams.getAuthenticationTable() + " " +
                    "WHERE id_applicazione = ? and token = ?";

        PreparedStatement ps = dbConn.prepareStatement(sqlText);
        ps.setString(1, idApplicazione);
        ps.setString(2, token);
        String query = ps.toString().substring(0, ps.toString().lastIndexOf("=") + 1) + " ******";
        log.debug("eseguo la query: " + query + " ...");
//            dbConn.setAutoCommit(true);
        ResultSet authenticationResultSet = ps.executeQuery();

        if (authenticationResultSet.next() == false) {
            String message = "applicazione: " + idApplicazione + " non autorizzata";
            log.error(message);
            throw new NotAuthorizedException(message);
        } else {
            String message = "applicazione: " + idApplicazione + " autorizzata";
            log.info(message);
            return authenticationResultSet.getString(1);
        }
    }

    public static String generateKey(int lenght) {
        char[] chars = "abcdefghijklmnopqrstuvwxyzABSDEFGHIJKLMNOPQRSTUVWXYZ1234567890".toCharArray();
        Random r = new Random();
        char[] id = new char[lenght];
        for (int i = 0; i < id.length; i++) {
            id[i] = chars[r.nextInt(chars.length)];
        }
        return new String(id);
    }

    public static void sendRefreshActivitiesCommand(List<String> users, String sessionId) throws Exception {
        log.info("users to refresh: " + Arrays.toString(users.toArray()));

        String masterChefHost = ApplicationParams.getRedisHost();
        String masteChefInQueue = ApplicationParams.getRedisInQueue();
        String server = ApplicationParams.getServerId();
        PrimusCommandParams par = new RefreshActivitiesCommandParams();
        PrimusCommand com = new PrimusCommand(par);

        PrimusMessage m = new PrimusMessage(users, "babel", com);

        JobParams j = new PrimusCommanderParams("1", "1", m);
        String tempQueue = UtilityFunctions.generateKey(32) + "_" + sessionId + "_" + server;
        WorkerData w = new WorkerData("babelmanager", "1", tempQueue, 5);
        w.addNewJob("1", "bag", j);
        RedisClient r = new RedisClient(masterChefHost, -1, tempQueue);

        r.put(w.getStringForRedis(), masteChefInQueue);
//        try {
//            String res = r.bpop(tempQueue, 30);
//        }
//        catch (Exception ex) {
//            log.error("errore nella lettura del risultato del masterchef", ex);
//            r.del(tempQueue);
//        }
    }
}
