package pizzeria;
import javafx.scene.paint.Color;

import java.sql.*;

public class PizzaDB {

	public static PreparedStatement putPizza(Connection con, String nome, String ingred, double prezzo){
		PreparedStatement preparedStatement = null;
		try {
			preparedStatement = con.prepareStatement("insert into sql7293749.Pizze values ('" + nome + "', '" + ingred + "', '" + prezzo + "');");
		} catch(SQLException sqle){
			Database.missingConnection();
		}
		return preparedStatement;
	}

	public static PreparedStatement removePizza(Connection con, String nome){
		PreparedStatement preparedStatement = null;
		try {
			preparedStatement = con.prepareStatement("delete from sql7293749.Pizze where nome = '" + nome + "';");
		} catch(SQLException sqle){
			Database.missingConnection();
			//System.out.println("Problema SQL rimozione pizza");
		}
		return preparedStatement;
	}

	public static PreparedStatement getPizzeDB(Connection con){
		PreparedStatement preparedStatement = null;
		try {
			preparedStatement = con.prepareStatement("select * from sql7293749.Pizze");
		} catch (NullPointerException | SQLException e){
			/* Chiude il programma, se non c'è connessione. */
			Database.missingConnection();
		}
		return preparedStatement;
	}
}