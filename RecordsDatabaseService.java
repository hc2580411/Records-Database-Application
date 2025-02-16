/*
 * RecordsDatabaseService.java
 *
 * The service threads for the records database server.
 * This class implements the database access service, i.e. opens a JDBC connection
 * to the database, makes and retrieves the query, and sends back the result.
 *
 * author: <2580411>
 *
 */

import java.io.*;
//import java.io.OutputStreamWriter;

import java.net.Socket;

import java.util.StringTokenizer;

import java.sql.*;
import javax.sql.rowset.*;
    //Direct import of the classes CachedRowSet and CachedRowSetImpl will fail becuase
    //these clasess are not exported by the module. Instead, one needs to impor
    //javax.sql.rowset.* as above.



public class RecordsDatabaseService extends Thread{

    private Socket serviceSocket = null;
    private String[] requestStr  = new String[2]; //One slot for artist's name and one for recordshop's name.
    private ResultSet outcome   = null;

	//JDBC connection
    private String USERNAME = Credentials.USERNAME;
    private String PASSWORD = Credentials.PASSWORD;
    private String URL      = Credentials.URL;



    //Class constructor
    public RecordsDatabaseService(Socket aSocket){
        this.serviceSocket = aSocket;
		//TO BE COMPLETED
		
    }


    //Retrieve the request from the socket
    public String[] retrieveRequest()
    {
        this.requestStr[0] = ""; //For artist
        this.requestStr[1] = ""; //For recordshop
		
		String tmp = "";
        try {
            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(serviceSocket.getInputStream()));

            String request = inFromClient.readLine().replace("#", tmp).trim();
            StringTokenizer st = new StringTokenizer(request,";");


            if (st.countTokens() > 1) {
                requestStr[0] = st.nextToken().trim();
                requestStr[1] = st.nextToken().trim();
            }



			//TO BE COMPLETED
			
         }catch(IOException e){
            System.out.println("Service thread " + this.getId() + ": " + e);
        }
        return this.requestStr;
    }


    //Parse the request command and execute the query
    public boolean attendRequest()
    {
        boolean flagRequestAttended = true;
		this.outcome = null;


        String sql = "SELECT record.title, record.label, record.genre, record.rrp, COUNT(recordcopy.recordID)" +
                "FROM record " +
                "INNER JOIN artist ON record.artistID = artist.artistID " +
                "INNER JOIN recordcopy ON record.recordID = recordcopy.recordID " +
                "INNER JOIN recordshop ON recordcopy.recordshopID = recordshop.recordshopID " +
                "WHERE artist.lastname = ? AND recordshop.city = ? " +
                "GROUP BY record.title, record.label, record.genre, record.rrp;"; //TO BE COMPLETED- Update this line as needed.


		
		try {
            Class.forName("org.postgresql.Driver");
            Connection connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            PreparedStatement stmt = connection.prepareStatement(sql);
			//Connet to the database
			//TO BE COMPLETED
            stmt.setString(1,requestStr[0]);
            stmt.setString(2,requestStr[1]);
			//Make the query
			//TO BE COMPLETED
            ResultSet rs = stmt.executeQuery();
            RowSetFactory aFactory = RowSetProvider.newFactory () ;
            CachedRowSet crs = aFactory.createCachedRowSet () ;
            crs.populate (rs) ;
			//Process query
			//TO BE COMPLETED -  Watch out! You may need to reset the iterator of the row set.
            this.outcome = crs;
            rs.close();
            stmt.close();
            connection.close();
			//Clean up
			//TO BE COMPLETED

		} catch (Exception e) {
            System.out.println(e);
            flagRequestAttended = false;
        }
        return flagRequestAttended;
    }



    //Wrap and return service outcome
    public void returnServiceOutcome(){
        try {
            ObjectOutputStream outcomeStreamWriter = new ObjectOutputStream(serviceSocket.getOutputStream());
            outcomeStreamWriter.writeObject(this.outcome);

            ResultSetMetaData metaData = outcome.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (outcome.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    if (i == columnCount){
                        System.out.print(outcome.getString(i));
                    }else{
                        System.out.print(outcome.getString(i) + " | ");
                    }

                }
                System.out.println();
            }
			//Return outcome
			//TO BE COMPLETED
			
            System.out.println("Service thread " + this.getId() + ": Service outcome returned; " + this.outcome);
            serviceSocket.close();
			//Terminating connection of the service socket
			//TO BE COMPLETED
			
			
        }catch (IOException e){
            System.out.println("Service thread " + this.getId() + ": " + e);
        }catch (SQLException e){
            System.out.println("Service thread " + this.getId() + ": Error while accessing ResultSet: " + e);
        }
    }


    //The service thread run() method
    public void run()
    {
		try {
			System.out.println("\n============================================\n");
            //Retrieve the service request from the socket
            this.retrieveRequest();
            System.out.println("Service thread " + this.getId() + ": Request retrieved: "
						+ "artist->" + this.requestStr[0] + "; recordshop->" + this.requestStr[1]);

            //Attend the request
            boolean tmp = this.attendRequest();


            //Send back the outcome of the request
            if (!tmp)
                System.out.println("Service thread " + this.getId() + ": Unable to provide service.");
            this.returnServiceOutcome();

        }catch (Exception e){
            System.out.println("Service thread " + this.getId() + ": " + e);
        }
        //Terminate service thread (by exiting run() method)
        System.out.println("Service thread " + this.getId() + ": Finished service.");
    }

}
