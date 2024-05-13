# Cassandra JDBC Driver with Enhanced SSL Capabilities

EventConnect's fork of the Cassandra JDBC driver is built on top of the DataGrip and DbSchema Cassandra drivers. This fork adds enhanced SSL capabilities to the driver.

Unlike the original implementation, this driver provides a standard cipher suite, which is customizable, during initialization of SSLOptions. This cipher suite helped us to eliminate timeout related issues during connection.

Forked from [Cassandra JDBC Driver](https://github.com/DataGrip/cassandra-jdbc-driver), which is based on [DbSchema Cassandra driver](https://bitbucket.org/dbschema/cassandra-jdbc-driver/src/master/)

## Building JAR
```
# Linux, MacOs
./gradlew shadowJar

# Windows
gradlew.bat shadowJar
```

You'll find it in `jar` directory. You can alternatively use the pre-built JAR of version 1.5.

## Usage

To use this driver in DataGrip, create a new driver and add the JAR file in the 'Data Sources and Drivers' dialog. For more information, please refer to the [DataGrip Documentation](https://www.jetbrains.com/help/datagrip/jdbc-drivers.html#configure_a_jdbc_driver_for_an_existing_data_source). 

After you add your JAR file, choose `com.dbschema.CassandraJdbcDriver` as the driver class.

Make sure to choose this driver for your Cassandra data source in the Data Sources and Drivers > Your Data Source > General > Driver dropdown.

### Configuration

This driver is configured using environment variables. DataGrip allows you to specify environment variables for each data source and driver. Disable `Use SSL` checkbox in the SSH/SSL tab.

Go to Data Sources and Drivers > Your Data Source > Advanced > Environment Variables (or VM environment) to configure SSL settings.

The following environment variables are supported:

| **Name**                          | Description                                                | Type | Default                                                       |
|-----------------------------------|------------------------------------------------------------|------|---------------------------------------------------------------|
| `CASS_CLIENT_ENABLE_SSL`          | Enable SSL                                                 | bool | `false`                                                       |
| `CASS_CLIENT_KEYSTORE`            | Path to keystore file                                      | text | `null`                                                        |
| `CASS_CLIENT_KEYSTORE_PASSWORD`   | Keystore password                                          | text | `null`                                                        |
| `CASS_CLIENT_TRUSTSTORE`          | Path to truststore file                                    | text | `null`                                                        |
| `CASS_CLIENT_TRUSTSTORE_PASSWORD` | Truststore password                                        | text | `null`                                                        |
| `CASS_CLIENT_KEY_ALIAS`           | Keystore private key alias                                 | text | `null`                                                        |
| `CASS_CLIENT_CIPHER_SUITE`        | SSL cipher suite, each item must be seperated with a comma | list | `TLS_RSA_WITH_AES_128_CBC_SHA, TLS_RSA_WITH_AES_256_CBC_SHA`  |

In a minimal setup, you must set `CASS_CLIENT_ENABLE_SSL` to `true` and provide the keystore and truststore files and their passwords.
