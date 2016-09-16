package tr.com.serkanozal.samba.demo.aws_lambda.db_connection;

import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import tr.com.serkanozal.samba.SambaField;
import tr.com.serkanozal.samba.SambaValueFactory;
import tr.com.serkanozal.samba.cache.SambaCacheType;

import com.amazonaws.services.lambda.runtime.Context; 

/**
 * This handler is an AWS Lamda function that reuses database connections 
 * if they are available on the local through <b>Samba</b>. 
 *
 * Creating database connection each time while accessing to database doesn't make sense 
 * because creating connection is not cheap operation and increases latency. 
 * By this behaviour, mostly database connections are reused 
 * according to background logic of AWS lambda under the hood as mentioned at
 * <a href="https://aws.amazon.com/blogs/compute/container-reuse-in-lambda">here</a>
 * and
 * <a href="https://www.linkedin.com/pulse/aws-lambda-container-lifetime-config-refresh-frederik-willaert">here</a>.
 */
public class DatabaseAccessHandler {

    private static final String DB_NAME = "SAMBA_DB_DEMO";
    private static final String DB_TABLE_NAME = "SAMBA_USER";
    
    public static final OutputStream DEV_NULL = new OutputStream() {
        public void write(int b) {}
    };
    
    static {
        System.setProperty("derby.stream.error.field", 
                           DatabaseAccessHandler.class.getName() + ".DEV_NULL"); 
    }
    
    // Since DB connections must be kept and accessed as Java objects 
    // directly without serializing/deserializing, DB connections are live objects.
    // Therefore, we keep them on the memory in the local cache.
    private final SambaField<Connection> dbConnectionField = 
            new SambaField<Connection>(SambaCacheType.LOCAL);
    private final DatabaseConnectionFactory dbConnectionFactory = new DatabaseConnectionFactory();
    
    public String handle(long id, Context context) {
        try {
            String fullName = getFullUsername(id);
            if (fullName == null) {
                throw new IllegalArgumentException("Unable to find user with id " + id);
            }
            return fullName;
        } catch (SQLException e) {
            throw new RuntimeException("Error occured while finding user with id " + id, e);
        }
    }
    
    private String getFullUsername(long id) throws SQLException {
        Connection conn = dbConnectionField.getOrCreate(dbConnectionFactory);
        final String DSL = 
                String.format("SELECT first_name, last_name FROM %s WHERE id=%d", 
                              DB_TABLE_NAME, id);
        Statement stmnt = conn.createStatement();
        ResultSet rs = stmnt.executeQuery(DSL);
        if (rs == null) {
            return null;
        }
        if (rs.next()) {
            return rs.getString("first_name") + " " + rs.getString("last_name");
        } else {
            return null;
        }
    }
    
    private static class DatabaseConnectionFactory implements SambaValueFactory<Connection> {

        @Override
        public Connection create() {
            try {
                // ********** NOTE **********
                // Change this with your own  external (remote) database connection URL.
                final String JDBC_URL = "jdbc:derby:memory:" + DB_NAME + ";create=true";
                
                Connection connection = DriverManager.getConnection(JDBC_URL);
                
                // ********** NOTE **********
                // Customize initialization or 
                // comment-out if creating table(s) and loading initial test data is not needed.
                init(connection);
                
                return connection;
            } catch (SQLException e) {
                throw new RuntimeException("Unable to get DB connection!", e);
            } 
        }
        
        private void init(Connection conn) throws SQLException {
            final String DDL = 
                    "CREATE TABLE " + DB_TABLE_NAME + " (" +
                            "id BIGINT NOT NULL, " +
                            "first_name VARCHAR(20) NOT NULL, " +
                            "last_name VARCHAR(20) NOT NULL, " +
                            "PRIMARY KEY (id))";
            Statement stmnt = conn.createStatement();
            stmnt.executeUpdate(DDL);
            for (int i = 1; i < 1000; i++) {
                final String DML = 
                        String.format(
                                "INSERT INTO %s VALUES (%d, 'FirstName-%d', 'LastName-%d')", 
                                DB_TABLE_NAME, i, i, i);
                stmnt = conn.createStatement();
                stmnt.executeUpdate(DML);
            }    
        }

        @Override
        public void destroy(Connection dbConnection) {
            try {
                dbConnection.close();
            } catch (SQLException e) {
                throw new RuntimeException("Unable to close DB connection!", e);
            }
        }
        
    }

}
