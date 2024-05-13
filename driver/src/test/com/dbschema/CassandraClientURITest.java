package com.dbschema;

import com.datastax.driver.core.ConsistencyLevel;
import org.junit.Test;

import java.util.List;
import java.util.Properties;

import static org.junit.Assert.*;

/**
 * @author Liudmila Kornilova
 **/
public class CassandraClientURITest {

    @Test(expected = IllegalArgumentException.class)
    public void testUriForDifferentDb() {
        new CassandraClientURI("jdbc:postgresql://localhost:54332/guest", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUriWithInvalidParameters() {
        new CassandraClientURI("jdbc:cassandra://localhost:9042?name=cassandra", null);
    }

    @Test
    public void testSimpleUri() {
        CassandraClientURI uri = new CassandraClientURI("jdbc:cassandra://localhost:9042", null);
        List<String> hosts = uri.hosts;
        assertEquals(1, hosts.size());
        assertEquals("localhost:9042", hosts.get(0));
    }

    @Test
    public void testUriWithUserName() {
        CassandraClientURI uri = new CassandraClientURI("jdbc:cassandra://localhost:9042/?user=cassandra", null);
        List<String> hosts = uri.hosts;
        assertEquals(1, hosts.size());
        assertEquals("localhost:9042", hosts.get(0));
        assertEquals("cassandra", uri.getUsername());
    }

    @Test
    public void testOptionsInProperties() {
        Properties properties = new Properties();
        properties.put("user", "NameFromProperties");
        properties.put("password", "PasswordFromProperties");
        CassandraClientURI uri = new CassandraClientURI(
                "jdbc:cassandra://localhost:9042/?user=cassandra&password=cassandra",
                properties);
        List<String> hosts = uri.hosts;
        assertEquals(1, hosts.size());
        assertEquals("localhost:9042", hosts.get(0));
        assertEquals("NameFromProperties", uri.getUsername());
        assertEquals("PasswordFromProperties", uri.password);
    }


    @Test
    public void testSslEnabledOptionTrue() {
        Properties properties = new Properties();
        properties.put("sslenabled", "true");
        CassandraClientURI uri = new CassandraClientURI(
                "jdbc:cassandra://localhost:9042/?name=cassandra&password=cassandra",
                properties);
        assertTrue(uri.sslEnabled);
    }

    @Test
    public void testSslEnabledOptionFalse() {
        Properties properties = new Properties();
        properties.put("sslenabled", "false");
        CassandraClientURI uri = new CassandraClientURI(
                "jdbc:cassandra://localhost:9042/?name=cassandra&password=cassandra",
                properties);
        assertFalse(uri.sslEnabled);
    }

    @Test
    public void testNullSslEnabledOptionFalse() {
        Properties properties = new Properties();
        CassandraClientURI uri = new CassandraClientURI(
            "jdbc:cassandra://localhost:9042/?name=cassandra&password=cassandra",
            properties);
        assertFalse(uri.sslEnabled);
    }

    @Test
    public void testConsistencyLevel() {
        Properties properties = new Properties();
        CassandraClientURI uri = new CassandraClientURI("jdbc:cassandra://localhost:9042/?consistencyLevel=serial", properties);
        assertEquals(ConsistencyLevel.SERIAL, uri.consistencyLevel);
    }

    @Test
    public void testUnknownConsistencyLevel() {
        Properties properties = new Properties();
        CassandraClientURI uri = new CassandraClientURI("jdbc:cassandra://localhost:9042/?consistencyLevel=unknown", properties);
        assertEquals(ConsistencyLevel.LOCAL_ONE, uri.consistencyLevel);
    }

    @Test
    public void testConsistencyLevelInProperties() {
        Properties properties = new Properties();
        properties.put("consistencyLevel", "EACH_QUORUM");
        CassandraClientURI uri = new CassandraClientURI("jdbc:cassandra://localhost:9042/?", properties);
        assertEquals(ConsistencyLevel.EACH_QUORUM, uri.consistencyLevel);
    }
}