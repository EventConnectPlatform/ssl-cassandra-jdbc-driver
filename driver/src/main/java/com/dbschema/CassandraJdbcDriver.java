
package com.dbschema;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.CodecRegistry;
import com.datastax.driver.core.ParseUtils;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.AuthenticationException;
import com.datastax.driver.core.exceptions.InvalidQueryException;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.dbschema.codec.jbigdecimal.BigintCodec;
import com.dbschema.codec.jbytes.BlobCodec;
import com.dbschema.codec.jstring.InetCodec;
import com.dbschema.codec.jstring.DurationCodec;
import com.dbschema.codec.jlong.*;
import com.dbschema.codec.jsqldate.DateCodec;
import com.dbschema.codec.jsqltime.TimeCodec;
import com.dbschema.codec.jstring.UuidCodec;
import com.dbschema.codec.jstring.TimeuuidCodec;

import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

import static com.dbschema.CassandraClientURI.PREFIX;


/**
 * Minimal implementation of the JDBC standards for the Cassandra database.
 * This is customized for DbSchema database designer.
 * Connect to the database using a URL like :
 * jdbc:cassandra://host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[keyspace][?options]]
 * The URL excepting the jdbc: prefix is passed as it is to the Cassandra native Java driver.
 */

public class CassandraJdbcDriver implements Driver {
    private static final String RETURN_NULL_STRINGS_FROM_INTRO_QUERY_KEY = "cassandra.jdbc.return.null.strings.from.intro.query";

    static {
        try {
            DriverManager.registerDriver(new CassandraJdbcDriver());
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }


    /**
     * Connect to the database using a URL like :
     * jdbc:cassandra://host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[keyspace][?options]]
     * The URL excepting the jdbc: prefix is passed as it is to the Cassandra native Java driver.
     */
    public Connection connect(String url, Properties info) throws SQLException {
        if (url != null && acceptsURL(url)) {
            CassandraClientURI clientURI = new CassandraClientURI(url, info);
            try {
                Cluster cluster = clientURI.createCluster();
                registerCodecs(cluster);
                String keyspace = clientURI.keyspace;
                Session session;
                try {
                    if (keyspace != null && !keyspace.isEmpty()) session = tryToConnect(cluster, keyspace);
                    else session = cluster.connect();
                } catch (NoHostAvailableException | AuthenticationException | IllegalStateException e) {
                    throw new SQLException(e.getMessage(), e);
                }
                boolean returnNullStringsFromIntroQuery = Boolean.parseBoolean(info.getProperty(RETURN_NULL_STRINGS_FROM_INTRO_QUERY_KEY));
                return new CassandraConnection(session, this, returnNullStringsFromIntroQuery, clientURI.consistencyLevel);
            } catch (UnknownHostException e) {
                throw new SQLException(e.getMessage(), e);
            }
        }
        return null;
    }

    private Session tryToConnect(Cluster cluster, String keyspace) throws SQLException {
        if (!ParseUtils.isDoubleQuoted(keyspace)) keyspace = ParseUtils.doubleQuote(keyspace);
        try {
            return cluster.connect(keyspace);
        } catch (InvalidQueryException e) {
            throw new SQLException("Keyspace " + keyspace + " does not exist", e);
        }
    }

    private void registerCodecs(Cluster cluster) {
        CodecRegistry myCodecRegistry = cluster.getConfiguration().getCodecRegistry();
        myCodecRegistry.register(IntCodec.INSTANCE);
        myCodecRegistry.register(DecimalCodec.INSTANCE);
        myCodecRegistry.register(DoubleCodec.INSTANCE);
        myCodecRegistry.register(com.dbschema.codec.jlong.FloatCodec.INSTANCE);
        myCodecRegistry.register(SmallintCodec.INSTANCE);
        myCodecRegistry.register(TinyintCodec.INSTANCE);
        myCodecRegistry.register(VarintCodec.INSTANCE);
        myCodecRegistry.register(BlobCodec.INSTANCE);
        myCodecRegistry.register(com.dbschema.codec.jdouble.FloatCodec.INSTANCE);
        myCodecRegistry.register(com.dbschema.codec.jdouble.DecimalCodec.INSTANCE);
        myCodecRegistry.register(DateCodec.INSTANCE);
        myCodecRegistry.register(TimeCodec.INSTANCE);
        myCodecRegistry.register(BigintCodec.INSTANCE);
        myCodecRegistry.register(UuidCodec.INSTANCE);
        myCodecRegistry.register(DurationCodec.INSTANCE);
        myCodecRegistry.register(TimeuuidCodec.INSTANCE);
        myCodecRegistry.register(InetCodec.INSTANCE);
    }


    /**
     * URLs accepted are of the form: jdbc:cassandra://host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[keyspace][?options]]
     */
    @Override
    public boolean acceptsURL(String url) {
        return url.startsWith(PREFIX);
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
        return DriverPropertyInfoHelper.getPropertyInfo();
    }

    String getVersion() {
        return "1.5";
    }

    @Override
    public int getMajorVersion() {
        return 1;
    }

    @Override
    public int getMinorVersion() {
        return 5;
    }

    @Override
    public boolean jdbcCompliant() {
        return true;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

}
