package ru.lwerl;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by lWeRl on 06.06.2017.
 * Magnit test problem
 */
public class Main {

    public static final String TABLE = "test";
    public static final String DB_NAME = "magnit";
    public static final String XML1_FILE_NAME = "1.xml";
    public static final String XML2_FILE_NAME = "2.xml";

    private String hostname;
    private String port;
    private String name;
    private String password;
    private int n;
    private Connection connection;


    public Main(String hostname, String port, String name, String password, Integer n) {
        this.hostname = hostname;
        this.port = port;
        this.name = name;
        this.password = password;
        this.n = n;
    }

    public Main() {
    }

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
//        new Main("localhost", "5432", "postgres", "Nerevar1N_*", 1_000_000).run();
        new Main("localhost", "5432", "postgres", "Nerevar1N_*", 10).run();
        System.out.println("Time: " + ((System.currentTimeMillis() - start) / 1000) + " s " + ((System.currentTimeMillis() - start) % 1000) + " ms");
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

    private void setConnection() {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Driver not found.");
        }
        try {
            connection = DriverManager.getConnection("jdbc:postgresql://" + hostname + ":" + port + "/" + DB_NAME, name, password);
        } catch (SQLException e) {
            System.err.println("Connect to DB failed.");
        }
        if (connection != null) {
            System.out.println("Connection to DB established.");
        }
    }

    private void insertToDB() {
        try {

            connection.setAutoCommit(false);
            connection.createStatement().execute("TRUNCATE TABLE " + TABLE);
            String insertTableSQL = "INSERT INTO " + TABLE + "(field) VALUES (?)";
            PreparedStatement statement = connection.prepareStatement(insertTableSQL);
            for (int i = 1; i <= n; i++) {
                statement.setInt(1, i);
                statement.addBatch();
            }

            statement.executeBatch();
            connection.commit();
            connection.setAutoCommit(true);

            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                connection.close();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        }
    }

    private void createXML() {
        try {
            JAXBContext context = JAXBContext.newInstance(Entries.class, Entry.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            Entries entries = new Entries();
            Statement statement = connection.createStatement();
            statement.execute("SELECT field FROM test");
            ResultSet resultSet = statement.getResultSet();
            while (resultSet.next()) {
                entries.getEntry().add(new Entry(String.valueOf(resultSet.getInt(1))));
            }
            statement.close();
            BufferedOutputStream xml1 = new BufferedOutputStream(new FileOutputStream(XML1_FILE_NAME));
            marshaller.marshal(entries, xml1);
            xml1.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void parseXML() {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            Source xslt = new StreamSource(new File("transform.xslt"));
            Transformer transformer = factory.newTransformer(xslt);
            Source source = new StreamSource(new BufferedInputStream(new FileInputStream(XML1_FILE_NAME)));
            transformer.transform(source, new StreamResult(new BufferedOutputStream(new FileOutputStream(XML2_FILE_NAME))));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void printAverage() {
        try {
            BufferedInputStream xml2 = new BufferedInputStream(new FileInputStream(XML2_FILE_NAME));
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(xml2);
            NodeList nodeList = document.getElementsByTagName("entry");
            long sum = 0;
            for (int i = 0; i < nodeList.getLength(); i++) {
                Element current = (Element) nodeList.item(i);
                sum += Integer.valueOf(current.getAttribute("field"));
            }
            System.out.println("Average: " + ((double) sum / nodeList.getLength()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
        setConnection();
        insertToDB();
        createXML();
        parseXML();
        printAverage();
    }

    @XmlRootElement
    static class Entries {

        private List<Entry> entry;

        Entries() {
            entry = new ArrayList<>();
        }

        public List<Entry> getEntry() {
            return entry;
        }

        public void setEntry(List<Entry> entry) {
            this.entry = entry;
        }
    }

    @XmlRootElement
    static class Entry {

        private String field;

        Entry(String field) {
            this.field = field;
        }

        public Entry() {
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }
    }
}
