package services;

import database.Database;
import enums.AccountPossibilities;
import enums.LoginPossibilities;
import graphicAlerts.GenericAlert;
import javafx.scene.paint.Color;
import pizzeria.Customer;
import pizzeria.Order;
import pizzeria.Pizzeria;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * Fornisce molteplici servizi, grafici o funzionali, in grado di supportare
 * il buon funzionamento del programma (e tuttavia non direttamente imputabili
 * a nessuna delle classi fino ad ora definite).
 * */
@SuppressWarnings("deprecation")
public class PizzeriaServices {

	public static String getHistory(boolean isGraphicRequest) {
		StringBuilder history = new StringBuilder("\n");
		try {
			Scanner fileIn = new Scanner(new File("Progetto/src/pizzeria/history.txt"));
			while (fileIn.hasNextLine()) {
				String line = fileIn.nextLine();
				if (line.startsWith("*")) {
					line = line.substring(1);	/* toglie l'asterisco dai titoletti */
					if(isGraphicRequest)
						history.append(line.toUpperCase()).append("\n");
					else
						history.append(TextualColorServices.colorSystemOut(line + "\n", Color.YELLOW, true, true));
				} else history.append(line).append("\n");
			}
		} catch (FileNotFoundException fnfe){
			String err = "Spiacenti: sezione History non disponibile.";
			if(isGraphicRequest) GenericAlert.display(err);
			else System.out.println(TextualColorServices.colorSystemOut(err, Color.RED,false,false));
		}
		return history.toString();
	}

	public static HashMap<String, Order> sortOrders(HashMap<String, Order> orders) {
		Set<Map.Entry<String, Order>> entries = orders.entrySet();
		Comparator<Map.Entry<String, Order>> valueComparator = (o1, o2) -> {
			Order v1 = o1.getValue();
			Order v2 = o2.getValue();
			return v1.compareTo(v2);
		};
		List<Map.Entry<String, Order>> listOfEntries = new ArrayList<>(entries); // Sort method needs a List, so let's first convert Set to List
		Collections.sort(listOfEntries, valueComparator);// sorting HashMap by values using comparator
		// copying entries from List to Map
		LinkedHashMap<String, Order> sortedByValue = new LinkedHashMap<>(listOfEntries.size());
		for(Map.Entry<String, Order> entry : listOfEntries){
			sortedByValue.put(entry.getKey(), entry.getValue());
		}
		orders = sortedByValue;
		return orders;
	}

	public static Order CustomerLastOrder(Customer customer, Pizzeria pizzeria) {
		Order last = null;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date;
		try {
			date = sdf.parse("1970-01-01 00:00:00");
			for (Order order : pizzeria.getOrders().values()) {
				if (order.getCustomer().getUsername().equals(customer.getUsername())) {
					if (order.getTime().getTime() > date.getTime()) {
						last = order;
						date = order.getTime();
					}
				}
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return last;
	}

	public static AccountPossibilities canCreateAccount(String mailAddress, String newUser, String newPsw, String confPsw) {
		if(newPsw.equals(confPsw)){
			if(newUser.length() > 2 && newPsw.length() > 2) {
				/* se si registra correttamente, va bene */
				try {
					if (Database.getCustomers(newUser.toUpperCase(),newPsw) || Database.checkMail(mailAddress))
						return AccountPossibilities.EXISTING;
					else
						return AccountPossibilities.OK;
				} catch (SQLException e) {
					return AccountPossibilities.OK;	// è sicuro ???????????????
				}
			} else
				/* password troppo breve */
				return AccountPossibilities.SHORT;
		} else {
			/* se la password non viene confermata correttamente */
			return AccountPossibilities.DIFFERENT;
		}
	}

	public static LoginPossibilities checkLogin(Pizzeria pizzeria, String user, String psw) throws SQLException {
		if(user.equals(pizzeria.getUserPizzeria()) && psw.equals(pizzeria.getPswPizzeria())){
			/* se è la pizzeria, allora accede come tale */
			return LoginPossibilities.PIZZERIA;
		} else if (Database.getCustomers(user,psw)){
			/* se è un utente identificato, accede come tale */
			return LoginPossibilities.OK;
		} else {
			/* se la combinazione utente-password è errata */
			return LoginPossibilities.NO;
		}
	}
}