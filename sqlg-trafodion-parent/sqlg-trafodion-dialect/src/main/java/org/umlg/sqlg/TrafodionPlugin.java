package org.umlg.sqlg;

import org.umlg.sqlg.sql.dialect.SqlDialect;
import org.umlg.sqlg.sql.dialect.TrafodionDialect;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * Created by Bin Lin on 8/25/17.
 */

public class TrafodionPlugin implements SqlgPlugin {

    @Override
    public boolean canWorkWith(DatabaseMetaData metaData) throws SQLException {
        return metaData.getDatabaseProductName().toLowerCase().contains("trafodion");
    }

    @Override
    public String getDriverFor(String connectionUrl) {
        return connectionUrl.startsWith("jdbc:t4jdbc") ? "org.trafodion.jdbc.t4.T4Driver" : null;
    }

    @Override
    public SqlDialect instantiateDialect() {
        return new TrafodionDialect();
    }
}
