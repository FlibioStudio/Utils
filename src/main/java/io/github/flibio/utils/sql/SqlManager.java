package io.github.flibio.utils.sql;

import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.service.sql.SqlService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

import javax.sql.DataSource;

public class SqlManager {

    private Logger logger;
    private String datasource;
    private SqlService sql;

    private Connection con;

    protected SqlManager(Logger logger, String datasource) {
        this.logger = logger;
        this.datasource = datasource;
        this.sql = Sponge.getServiceManager().provide(SqlService.class).get();
    }

    private void openConnection() {
        try {
            DataSource source = sql.getDataSource(datasource);
            con = source.getConnection();
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }
    }

    private void reconnect() {
        try {
            if (con != null && !con.isClosed())
                con.close();
            openConnection();
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }
    }

    /**
     * Executes an update to the database. Recommended to run in an async
     * thread.
     * 
     * @param sql The sql to execute.
     * @param vars The variables to replace in the sql. Replaced in
     *        chronological order.
     * @return If the update was successful or not.
     */
    public boolean executeUpdate(String sql, String... vars) {
        try {
            reconnect();
            PreparedStatement ps = con.prepareStatement(sql);
            for (int i = 0; i < vars.length; i++) {
                ps.setString(i + 1, vars[i]);
            }
            return (ps.executeUpdate() > 0);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
            return false;
        }
    }

    /**
     * Runs a query on the database. Recommended to run in an async thread.
     * 
     * @param sql The sql to run.
     * @param vars The variables to replace in the sql. Replaced in
     *        chronological order.
     * @return The ResultSet retrieved from the query.
     */
    public Optional<ResultSet> executeQuery(String sql, String... vars) {
        try {
            reconnect();
            PreparedStatement ps = con.prepareStatement(sql);
            for (int i = 0; i < vars.length; i++) {
                ps.setString(i + 1, vars[i]);
            }
            return Optional.of(ps.executeQuery());
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Queries the database and retrieves a column's data.
     * 
     * @param columnName The column to retrieve that data of.
     * @param type The type of data to retrieve.
     * @param sql The sql to run.
     * @param vars The variables to replace in sql. Replaced in chronological
     *        order.
     * @return The column's data, if it was found.
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> queryType(String columnName, Class<T> type, String sql, String... vars) {
        try {
            Optional<ResultSet> rOpt = executeQuery(sql, vars);
            if (rOpt.isPresent()) {
                ResultSet rs = rOpt.get();
                rs.next();
                Object raw = rs.getObject(columnName);
                if (raw.getClass().equals(type)) {
                    return Optional.of((T) raw);
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            logger.error(e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Queries the database and checks if a row exists.
     * 
     * @param sql The sql to run.
     * @param vars The variables to replace in the sql. Replaced in
     *        chronological order.
     * @return If the row was found or not.
     */
    public boolean queryExists(String sql, String... vars) {
        try {
            Optional<ResultSet> rOpt = executeQuery(sql, vars);
            if (rOpt.isPresent()) {
                return rOpt.get().next();
            }
            return false;
        } catch (Exception e) {
            logger.error(e.getMessage());
            return false;
        }
    }

}