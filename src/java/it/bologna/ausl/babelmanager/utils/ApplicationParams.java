package it.bologna.ausl.babelmanager.utils;

import java.sql.Connection;
import java.sql.SQLException;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import org.apache.log4j.Logger;

/**
 *
 * @author gdm
 */
public class ApplicationParams {
    private static final Logger log = Logger.getLogger(ApplicationParams.class);
    
    private static String appId;
    private static String appToken;
    private static String serverId;
    private static String mongoUri;
    private static String redisHost;
    private static String publicParametersTableName;
    private static String redisInQueue;
    private static String authenticationTable;
    
    public static void initApplicationParams(ServletContext context) throws SQLException, NamingException, ServletException {
        try (Connection dbConn = UtilityFunctions.getDBConnection()) {
            appId = context.getInitParameter("appid");
            appToken = context.getInitParameter("apptoken");
            publicParametersTableName = context.getInitParameter("ParametersTableName");
            readAuthenticationTable(context);
            serverId = serverId = UtilityFunctions.getPubblicParameter(dbConn, "serverIdentifier");
            //mongoUri = context.getInitParameter("mongo" + serverId);
            mongoUri = UtilityFunctions.getPubblicParameter(dbConn, "mongoConnectionString");
            //redisHost = context.getInitParameter("redis" + serverId);
            redisHost = UtilityFunctions.getPubblicParameter(dbConn, "masterChefHost");
            //redisInQueue = context.getInitParameter("redisinqueue" + serverId);
            redisInQueue = UtilityFunctions.getPubblicParameter(dbConn, "masterChefPushingQueue");
        }
    }

    private static void readAuthenticationTable(ServletContext context) throws ServletException {
        authenticationTable = context.getInitParameter("AuthenticationTable");
        if(authenticationTable == null || authenticationTable.equals("")) {
            String message = "Manca il nome della tabella per l'autenticazione. Indicarlo nel file \"web.xml\"";
            log.error(message);
            throw new ServletException(message);
        }
    }

    public static String getAppId() {
        return appId;
    }

    public static void setAppId(String appId) {
        ApplicationParams.appId = appId;
    }

    public static String getAppToken() {
        return appToken;
    }

    public static void setAppToken(String appToken) {
        ApplicationParams.appToken = appToken;
    }

    public static String getPublicParametersTableName() {
        return publicParametersTableName;
    }

    public static void setPublicParametersTableName(String publicParametersTableName) {
        ApplicationParams.publicParametersTableName = publicParametersTableName;
    }

    public static String getServerId() {
        return serverId;
    }

    public static void setServerId(String serverId) {
        ApplicationParams.serverId = serverId;
    }

    public static String getMongoUri() {
        return mongoUri;
    }

    public static void setMongoUri(String mongoUri) {
        ApplicationParams.mongoUri = mongoUri;
    }

    public static String getRedisHost() {
        return redisHost;
    }

    public static void setRedisHost(String redisHost) {
        ApplicationParams.redisHost = redisHost;
    }

    public static String getRedisInQueue() {
        return redisInQueue;
    }

    public static void setRedisInQueue(String redisInQueue) {
        ApplicationParams.redisInQueue = redisInQueue;
    }

    public static String getAuthenticationTable() {
        return authenticationTable;
    }

    public static void setAuthenticationTable(String authenticationTable) {
        ApplicationParams.authenticationTable = authenticationTable;
    }
}
