/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pvehiculos;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.gt;
import static com.mongodb.client.model.Filters.lt;
import com.mongodb.client.model.Projections;
import static com.mongodb.client.model.Projections.include;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import org.bson.Document;

public class Pvehiculos {

    private static MongoClient mongoClient;
    private static MongoDatabase database;
    private static MongoCollection<Document> collection;

    //numeros desde mongo siempre serán en java double !!!
    public static void conexionMongo() {

        //NOS CONECTAMOS A MONGO
        mongoClient = new MongoClient("localhost", 27017);

        //NOS CONECTAMOS A UNA BD
        database = mongoClient.getDatabase("test");

        //se supone que no nos va a pedir credenciales, así que no los ponemos de momento
        collection = database.getCollection("vendas");

    }

    public static void desconexionMongo() {
        mongoClient.close();
    }

    public static Connection conexion = null;

    public static Connection getConexion() throws SQLException {
        String usuario = "hr";
        String password = "hr";
        String host = "localhost";
        String puerto = "1521";
        String sid = "orcl";
        String ulrjdbc = "jdbc:oracle:thin:" + usuario + "/" + password + "@" + host + ":" + puerto + ":" + sid;

        conexion = DriverManager.getConnection(ulrjdbc);
        return conexion;
    }

    public static void cerrar() throws SQLException {
        conexion.close();
    }

    public static void main(String[] args) throws SQLException {
        
        conexionMongo();
        FindIterable<Document> datosMongo = collection.find();

        //OBTENEMOS LOS DATOS DE MONGO
        for (Document z : datosMongo) {
            
            double id;
            String dni;
            String codveh;

            id = z.getDouble("_id");
            System.out.println("ID: " + id);
            dni = z.getString("dni");
            System.out.println("DNI: " + dni);
            codveh = z.getString("codveh");
            System.out.println("CODVEH: " + codveh);

            //SEGUIMOS CON OBJECTDB
            EntityManagerFactory emf = Persistence.createEntityManagerFactory("/home/oracle/Downloads/vehicli.odb");

            EntityManager em = emf.createEntityManager();

            //NOMEC EN CLIENTES
            em.getTransaction().begin();

            TypedQuery<Clientes> query
                    = em.createQuery("select p from Clientes p where p.dni = :value ", Clientes.class);

            Clientes objCliente = new Clientes();
            objCliente = query.setParameter("value", dni).getSingleResult();

            String nomec = objCliente.getNomec();
            int ncompras = objCliente.getNcompras();

            System.out.println("NOMEC: " + nomec);
            System.out.println("NCOMPRAS: " + ncompras);

            em.getTransaction().commit();

            //EL RESTO EN VEHÍCULOS
            em.getTransaction().begin();

            TypedQuery<Vehiculos> query2
                    = em.createQuery("select p from Vehiculos p where p.codveh = :value2 ", Vehiculos.class);
            Vehiculos objVehi = new Vehiculos();
            objVehi = query2.setParameter("value2", codveh).getSingleResult();

            int pf;
            int anomatricula = objVehi.getAnomatricula();
            int prezoorixe = objVehi.getPrezoorixe();
            String nomeveh = objVehi.getNomveh();
            System.out.println("ANOMATRICULA: " + anomatricula);
            System.out.println("PREZOORIXE: " + prezoorixe);
            System.out.println("NOMEVEH: " + nomeveh);

            System.out.println("************************");

            //CERRAR AL FINAL
            em.close();
            emf.close();

            //CALCULAMOS EL PRECIO FINAL
            if (ncompras > 0) {
                pf = prezoorixe - ((2019 - anomatricula) * 500) - 500;
            } else {
                pf = prezoorixe - ((2019 - anomatricula) * 500);
            }

            System.out.println("PF: " + pf);

            System.out.println("************************");

            getConexion();
            
            PreparedStatement ps = conexion.prepareStatement("insert into finalveh values(?,?,?,tipo_vehf(?,?))");

            ps.setDouble(1, id);
            ps.setString(2, dni);
            ps.setString(3, nomec);
            ps.setString(4, nomeveh);
            ps.setInt(5, pf);
            ps.executeUpdate();
           cerrar();
        }
         
        desconexionMongo();
    }
}
