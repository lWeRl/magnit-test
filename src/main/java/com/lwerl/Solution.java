package com.lwerl;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.sql.*;

/**
 * Created by lWeRl on 06.06.2017.
 * Magnit test problem
 */
public class Solution implements Serializable {

    public static final String ENTRY_NAME = "entry";
    public static final String FIELD_NAME = "field";
    public static final String TABLE_NAME = "test";

    public static final String XML1_FILE_NAME = "1.xml";
    public static final String XML2_FILE_NAME = "2.xml";
    public static final String XSLT_FILE_NAME = "transform.xslt";

    public static final String TRUNCATE_TABLE = "TRUNCATE TABLE " + TABLE_NAME;
    public static final String INSERT_FIELD_PREPARED = "INSERT INTO " + TABLE_NAME + "(" + FIELD_NAME + ") VALUES (?)";

    public static final int PAGE_SIZE = 250_000;

    private DbType type;
    private String hostname;
    private String port;
    private String dbName;
    private String name;
    private String password;
    private int n;

    public Solution(DbType type, String hostname, String port, String dbName, String name, String password, Integer n) {
        this.type = type;
        this.hostname = hostname;
        this.port = port;
        this.dbName = dbName;
        this.name = name;
        this.password = password;
        this.n = n;
    }

    public Solution() {
    }

    public DbType getType() {
        return type;
    }

    public void setType(DbType type) {
        this.type = type;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getN() {
        return n;
    }

    public void setN(Integer n) {
        this.n = n;
    }

    private Connection getConnection() throws ClassNotFoundException, SQLException {
        Class.forName(type.getDriverName());
        String connectionURL = type.getUrlPrefix() + hostname + ":" + port + "/" + dbName;
        return DriverManager.getConnection(connectionURL, name, password);
    }

    private void insertToDB(Connection connection) throws SQLException {
        connection.setAutoCommit(false);

        try (Statement statement = connection.createStatement()) {
            statement.execute(TRUNCATE_TABLE);
        }

        try (PreparedStatement statement = connection.prepareStatement(INSERT_FIELD_PREPARED)) {
            for (int i = 1; i <= n; i++) {
                statement.setInt(1, i);
                statement.addBatch();
                statement.executeBatch();
            }
        }

        connection.commit();
        connection.setAutoCommit(true);

    }

    private void createXML(Connection connection) throws JAXBException, SQLException, IOException {
        JAXBContext context = JAXBContext.newInstance(Entries.class, Entry.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");

        Entries entries = new Entries(n);
        try (PreparedStatement statement = connection.prepareStatement(type.getPagingPreparedQuery())) {
            int i;
            int offset = 0;
            do {
                statement.setInt(1, offset * PAGE_SIZE);
                try (ResultSet resultSet = statement.executeQuery()) {
                    i = 0;
                    while (resultSet.next()) {
                        entries.getEntry()[i + offset * PAGE_SIZE] = new Entry(resultSet.getInt(1));
                        i++;
                    }
                }
                offset++;
            } while (i == PAGE_SIZE);
        }

        try (BufferedOutputStream xml1 = new BufferedOutputStream(new FileOutputStream(XML1_FILE_NAME))) {
            marshaller.marshal(entries, xml1);
        }
    }

    private void warpXML() throws IOException, TransformerException {
        try (
                FileInputStream xsltFile = new FileInputStream(XSLT_FILE_NAME);
                BufferedInputStream inputXML = new BufferedInputStream(new FileInputStream(XML1_FILE_NAME));
                BufferedOutputStream outputXML = new BufferedOutputStream(new FileOutputStream(XML2_FILE_NAME))
        ) {
            TransformerFactory factory = TransformerFactory.newInstance();
            Source xslt = new StreamSource(xsltFile);
            Transformer transformer = factory.newTransformer(xslt);
            Source source = new StreamSource(inputXML);
            transformer.transform(source, new StreamResult(outputXML));
        }
    }

    private void printAverage() throws ParserConfigurationException, SAXException, IOException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser saxParser = factory.newSAXParser();

        class MyHandler extends DefaultHandler {
            private long sum = 0;

            public long getSum() {
                return sum;
            }

            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                if (qName.equalsIgnoreCase(ENTRY_NAME)) {
                    sum += Integer.valueOf(attributes.getValue(FIELD_NAME));
                }
            }
        }

        MyHandler handler = new MyHandler();

        try (BufferedInputStream inputXML = new BufferedInputStream(new FileInputStream(XML2_FILE_NAME))) {
            saxParser.parse(inputXML, handler);
        }
        System.out.println("Average: " + ((double) handler.getSum()) / n);
    }

    public void execute() {
        long start = System.currentTimeMillis();
        try (Connection connection = getConnection()) {
            System.out.println("Get connection time: " + ((System.currentTimeMillis() - start) / 1000) + " s " + ((System.currentTimeMillis() - start) % 1000) + " ms");
            insertToDB(connection);
            System.out.println("Insert to DB time: " + ((System.currentTimeMillis() - start) / 1000) + " s " + ((System.currentTimeMillis() - start) % 1000) + " ms");
            createXML(connection);
            System.out.println("Create XML time: " + ((System.currentTimeMillis() - start) / 1000) + " s " + ((System.currentTimeMillis() - start) % 1000) + " ms");
            warpXML();
            System.out.println("Warp XML time: " + ((System.currentTimeMillis() - start) / 1000) + " s " + ((System.currentTimeMillis() - start) % 1000) + " ms");
            printAverage();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Full time: " + ((System.currentTimeMillis() - start) / 1000) + " s " + ((System.currentTimeMillis() - start) % 1000) + " ms");
    }

    @XmlRootElement
    static class Entries implements Serializable {

        private Entry[] entry;

        Entries(int n) {
            entry = new Entry[n];
        }

        public Entries() {
        }

        public Entry[] getEntry() {
            return entry;
        }

        public void setEntry(Entry[] entry) {
            this.entry = entry;
        }
    }

    @XmlRootElement
    static class Entry implements Serializable {

        private int field;

        Entry(Integer field) {
            this.field = field;
        }

        public Entry() {
        }

        public Integer getField() {
            return field;
        }

        public void setField(Integer field) {
            this.field = field;
        }
    }
}
