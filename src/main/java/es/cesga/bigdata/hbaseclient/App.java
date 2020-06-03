package es.cesga.bigdata.hbaseclient;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes; 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;

public class App {

    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws IOException {

        Configuration conf = HBaseConfiguration.create();
        // Option 1 (recommended): Place hbase-site.xml on src/main/resources
        // Option 2: Configure the hbase connection properties here:
        // - Zookeeper addresses
        //conf.set("hbase.zookeeper.quorum","10.112.13.19,10.112.13.18,10.112.13.17");
        conf.set("hbase.zookeeper.quorum","10.112.14.18,10.112.14.19,10.121.13.219");
        // - Non-default znode parent (e.g. HDP with security disabled)
        //conf.set("zookeeper.znode.parent", "/hbase-unsecure");

        // Create a connection (you can use a try with resources block)
        Connection connection = ConnectionFactory.createConnection(conf);

        // Creating a table with a random name
        Admin admin = connection.getAdmin();
        UUID uuid = UUID.randomUUID();
        TableName tableName = TableName.valueOf(uuid.toString());
        HTableDescriptor tableDescriptor = new HTableDescriptor(tableName);
        HColumnDescriptor cf = new HColumnDescriptor(Bytes.toBytes("cf"));
        tableDescriptor.addFamily(cf);
        logger.info("Creating table {}", tableName);
        admin.createTable(tableDescriptor);

        // Using the table
        Table table = connection.getTable(tableName);

        logger.info("Adding a row");
        Put put = new Put(Bytes.toBytes("0001"));
        put.addColumn(Bytes.toBytes("cf"), Bytes.toBytes("name"), Bytes.toBytes("Javier"));
        table.put(put);

        for (int i = 2; i < 10; i++) {
            Put p = new Put(Bytes.toBytes("000" + i));
            p.addColumn(Bytes.toBytes("cf"), Bytes.toBytes("name"), Bytes.toBytes("Javier-" + i));
            table.put(p);
        }

        logger.info("Reading a row");
        Get get = new Get(Bytes.toBytes("0001"));
        Result r = table.get(get);
        byte[] value = r.getValue(Bytes.toBytes("cf"), Bytes.toBytes("name"));
        logger.info("Result: Row: {} Value: {}", r, Bytes.toString(value));

        logger.info("Full scan");
        Scan scan = new Scan();
        ResultScanner scanner = table.getScanner(scan);
        for (Result res : scanner) {
            logger.info("Row: {}", res);
        }
        scanner.close();

        logger.info("Restricted Scan");
        Scan scan2 = new Scan();
        scan2.addColumn(Bytes.toBytes("cf"), Bytes.toBytes("name"))
                .setStartRow(Bytes.toBytes("0003"))
                .setStopRow(Bytes.toBytes("0005"));
        ResultScanner scanner2 = table.getScanner(scan2);
        for (Result res2 : scanner2) {
            System.out.println(res2);
            byte[] value2 = res2.getValue(Bytes.toBytes("cf"), Bytes.toBytes("name"));
            System.out.println("Value: " + Bytes.toString(value2));
        }
        scanner2.close();

        // Close the table
        table.close();

        // Delete the table
        logger.info("Disabling table {}", tableName);
        admin.disableTable(tableName);
        logger.info("Deleting table {}", tableName);
        admin.deleteTable(tableName);

        // Close the connection
        connection.close();

        System.exit(0);
    }
}
