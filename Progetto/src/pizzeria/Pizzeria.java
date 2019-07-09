package pizzeria;

import database.*;
import javafx.scene.paint.Color;
import services.TextualPrintServices;
import services.TimeServices;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.*;

import static database.Database.openDatabase;
import static database.Database.setLastUpdate;
import static services.TimeServices.getMinutes;
import static services.TimeServices.getNowMinutes;

@SuppressWarnings("deprecation")

public class Pizzeria {
	private String name;
	private String address;
	private LocalTime[] openings = new LocalTime[7];    // orari di apertura in tutti i giorni della settimana
	private LocalTime[] closings = new LocalTime[7];    // orari di chiusura in tutti i giorni della settimana
	private Oven[] ovens;
	private ArrayList<DeliveryMan> deliveryMen;
	private HashMap<String, Pizza> menu;
	private HashMap<String, String> pizzeriaIngredients;
	private HashMap<String,Order> orders;
	private int availablePlaces;
	private int numDailyOrders;
	private final int OVEN_MINUTES = 5;      // ogni 5 minuti
	private final int DELIVERYMAN_MINUTES = 10;   // ogni 10 minuti
	private final double SUPPL_PRICE;
	private final String userPizzeria;
	private final String pswPizzeria;

	/**
	 * La Pizzeria è il locale che riceve le ordinazioni e le evade nei tempi richiesti.
	 * @param name: nome identificativo della Pizzeria
	 * @param address: indirizzo della Pizzeria
	 *
	 * Inizializza anche il forno, con tutte le possibili infornate,
	 * una ArrayList di fattorini e una di ordini del giorno.
	 */

	public Pizzeria(String name, String address,
					LocalTime op1, LocalTime op2, LocalTime op3, LocalTime op4, LocalTime op5, LocalTime op6, LocalTime op7,
					LocalTime cl1, LocalTime cl2, LocalTime cl3, LocalTime cl4, LocalTime cl5, LocalTime cl6, LocalTime cl7) {
		this.userPizzeria = "pizzeria".toUpperCase();
		this.pswPizzeria = "password".toUpperCase();
		this.menu = new HashMap<>();
		this.pizzeriaIngredients = new HashMap<>();
		this.name = name;
		this.numDailyOrders = 0;
		this.orders = new LinkedHashMap<>();
		this.address = address;
		setDayOfTheWeek(op1,op2,op3,op4,op5,op6,op7,cl1,cl2,cl3,cl4,cl5,cl6,cl7);  // 1 = domenica, 2 = lunedi, ... 7 = sabato.
		this.deliveryMen = new ArrayList<>();
		this.SUPPL_PRICE = 0.5;
		this.availablePlaces = 8;
		/* Apre la connessione con il database */
		openDatabase();
		addDeliveryMan(new DeliveryMan("Musi", this));
		updatePizzeriaToday();
		//addDeliveryMan(new DeliveryMan("Zanzatroni", this));
	}

	/**
	 * Riempie i vettori della pizzeria contenenti gli orari
	 * di apertura e di chiusura per ogni giorno della settimana.
	 * Utilizzato nel costruttore della pizzeria, ma riutilizzabile in caso di cambiamenti.
	 * */
	private void setDayOfTheWeek(LocalTime op1, LocalTime op2, LocalTime op3, LocalTime op4, LocalTime op5, LocalTime op6, LocalTime op7, LocalTime cl1, LocalTime cl2, LocalTime cl3, LocalTime cl4, LocalTime cl5, LocalTime cl6, LocalTime cl7) {
		this.openings[0] = op1;
		this.openings[1] = op2;
		this.openings[2] = op3;
		this.openings[3] = op4;
		this.openings[4] = op5;
		this.openings[5] = op6;
		this.openings[6] = op7;
		this.closings[0] = cl1;
		this.closings[1] = cl2;
		this.closings[2] = cl3;
		this.closings[3] = cl4;
		this.closings[4] = cl5;
		this.closings[5] = cl6;
		this.closings[6] = cl7;
	}

	public HashMap<String,Order> getOrders() {
		try {
			this.orders = OrderDB.getOrders(this, this.orders); //FIXME @ZANA SENZA QUESTO "UGUALE" NON FUNZIONA
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return this.orders;
	}

	/** Aggiunge l'ordine, completato, a quelli che la pizzeria deve evadere. Richiama i vari aggiornamenti. */
	public void addInfoOrder(Order order) {
		OrderDB.putOrder(order);
		order.setCompletedDb(this,order.getNumPizze(),order.getTime());	// FIXME @fetch: vediamo così
		/* Sostituisce l'ordine come era stato aggiunto inizialmente (vuoto) con quello definitivo. */
		this.orders.remove(order.getOrderCode());
		this.orders.put(order.getOrderCode(),order);
	}

	/** Aggiunge la pizza specificata al menu della pizzeria. */
	private void addPizza(Pizza pizza){
		this.menu.put(pizza.getName(false),pizza);
	}

	public void addDeliveryMan(DeliveryMan deliveryMan){
		this.deliveryMen.add(deliveryMan);
	}

	/** Aggiorna quotidianamente il menu e ripristina il vettore di infornate, ad ogni apertura della pizzeria */
	public void updatePizzeriaToday() {
		// FIXME:	(RISOLTO: SI PUO TOGLIERE)
		//  creare in db una tabella con alcuni dati della pizzeria (orari di apertura/chiusura? indirizzo?...):
		//  in particolare una data di ultimo aggiornamento: ogni volta che la pizzeria vuole visualizzare gli ordini o
		//  che un cliente vuole effettuare un nuovo ordine, si controlla se la data di ultimo aggiornamento corrisponde:
		//  se non corrisponde, si aggiorna tutto (si richiama questo metodo update()) e si aggiorna la data nel DB.

		setIngredientsPizzeria();
		createMenu();
		int closeMinutes = getMinutes(getClosingToday());
		int openMinutes = getMinutes(getOpeningToday());
		this.ovens = new Oven[(closeMinutes - openMinutes) / this.OVEN_MINUTES];    // minutiTotali/5
		for (int i = 0; i < this.ovens.length; i++) {
			this.ovens[i] = new Oven(this.availablePlaces);
		}
		Date last = Database.getLastUpdate();
		Date today = new Date();
		if (last.getDate() != today.getDate()) {
			setLastUpdate(last);
		}
		getOrders();
	}

	public Date getOpeningToday(){
		Calendar cal = new GregorianCalendar();
		int todayDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);  // oggi
		Date op = new Date();
		op.setHours(this.openings[todayDayOfWeek-1].getHour());
		op.setMinutes(this.openings[todayDayOfWeek-1].getMinute());
		return op;
	}

	public Date getClosingToday(){
		Calendar cal = new GregorianCalendar();
		int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);  // oggi
		Date cl = new Date();
		cl.setHours(this.closings[dayOfWeek-1].getHour());
		cl.setMinutes(this.closings[dayOfWeek-1].getMinute());
		return cl;
	}

	/** Una tantum: vengono aggiunti a "pizzeriaIngredients" tutti gli ingredienti utilizzabili. */
	private void setIngredientsPizzeria(){
		try {
			for(String s : ToppingDB.getToppings(this.pizzeriaIngredients).keySet()){
				this.pizzeriaIngredients.put(s,s);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/** Una tantum: viene creato il menu della pizzeria; ad ogni pizza vengono aggiunti i rispettivi toppings. */
	private void createMenu() {
		try {
			for(String s : PizzaDB.getPizzeDB(menu).keySet()){
				addPizza(menu.get(s));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public HashMap<String, Pizza> getMenu() {
		return this.menu;
	}

	public HashMap<String, String> getIngredientsPizzeria() {
		return this.pizzeriaIngredients;
	}

	/** Crea un nuovo ordine e aggiorna il numero di ordini giornalieri. */
	public Order initializeNewOrder() {
		Order order;
		getOrders();
		order = new Order(Database.countOrdersDB());
		Database.addNewVoidOrderToDB(order);
		return order;
	}

	/** Su interfaces.TextualInterface dà il benvenuto al cliente, fornendo le informazioni essenziali della pizzeria. */
	public String helloThere(){
		String opTime = TimeServices.timeStamp(getOpeningToday().getHours(), getOpeningToday().getMinutes());
		String clTime = TimeServices.timeStamp(getClosingToday().getHours(), getClosingToday().getMinutes());
		StringBuilder hello = new StringBuilder("\n");
		hello.append(TextualPrintServices.colorSystemOut("\nBenvenuto!\n", Color.GREEN,true,true));
		hello.append(TextualPrintServices.colorSystemOut("\nPIZZERIA ", Color.ORANGE,false,false));
		hello.append(TextualPrintServices.colorSystemOut("\"" + this.name + "\"\n\t",Color.RED,true,false));
		hello.append(TextualPrintServices.colorSystemOut(this.address,Color.ORANGE,false,false));
		if(getOpeningToday().equals(getClosingToday()))
			hello.append(TextualPrintServices.colorSystemOut("\n\tOGGI CHIUSO", Color.RED, true, false));
		else {
			hello.append(TextualPrintServices.colorSystemOut("\n\tApertura oggi: ", Color.ORANGE, false, false));
			hello.append(TextualPrintServices.colorSystemOut(opTime + " - " + clTime, Color.RED, true, false));
		}
		hello.append("\n").append(TextualPrintServices.getLine());
		return hello.toString();
	}

	/** Da interfaces.TextualInterface, permette di stampare a video il menu completo. */
	public String printMenu() {
		String line = TextualPrintServices.getLine();
		TextualPrintServices.paintMenuString();
		StringBuilder s = new StringBuilder();
		for (String a : this.menu.keySet()) {
			s.append("\n").append(this.menu.get(a).toString());
		}
		return s.toString() + "\n" + line;
	}

	/** Controlla che la pizzeria sia aperta in un determinato orario, nella giornata odierna. */
	public boolean isOpen(Date d){
		int openTime = getMinutes(getOpeningToday());
		int closeTime = getMinutes(getClosingToday());
		int requestTime = getMinutes(d);

		return (requestTime >= openTime && requestTime < closeTime);
	}

	/** ritorna l'indice della casella temporale (forno) desiderata. */
	public int findTimeBoxOven(int oraDesiderata, int minutiDesiderati){
		int openMinutes = getMinutes(getOpeningToday());
		int desiredMinutes = getMinutes(oraDesiderata,minutiDesiderati);
		return (desiredMinutes - openMinutes)/this.OVEN_MINUTES;
	}

	/** ritorna l'indice della casella temporale (fattorino) desiderata. */
	public int findTimeBoxDeliveryMan(int oraDesiderata, int minutiDesiderati){
		int openMinutes = getMinutes(getOpeningToday());
		int desiredMinutes = getMinutes(oraDesiderata,minutiDesiderati);
		return (desiredMinutes - openMinutes)/this.DELIVERYMAN_MINUTES;
	}

	/** restituisce il primo fattorino della pizzeria che sia disponibile all'orario indicato. */
	public DeliveryMan aFreeDeliveryMan(int oraDesiderata, int minutiDesiderati){
		for(DeliveryMan man : this.deliveryMen){
			if(man.getDeliveryManTimes()[findTimeBoxDeliveryMan(oraDesiderata,minutiDesiderati)].isFree()){
				return man;
			}
		}
		return null;
	}

	/** Restituisce tutti gli orari in cui la pizzeria potrebbe garantire la consegna di "tot" pizze.
	 * la var "scarto" risponde all'eventualità che la pizzeria sia già aperta al momento attuale. */
	public ArrayList<String> availableTimes(int tot){
		ArrayList<String> availables = new ArrayList<>();
		int now = getNowMinutes();
		int restaAperta = TimeServices.calculateOpeningMinutesPizzeria(this);
		int esclusiIniziali = TimeServices.calculateStartIndex(this, now, tot);     // primo orario da visualizzare (in minuti)

		for(int i = esclusiIniziali; i < restaAperta; i++) {    // considera i tempi minimi di preparazione e consegna
			if(i % 5 == 0) {
				if (this.ovens[i / 5].getAvailablePlaces() + this.ovens[(i / 5) - 1].getAvailablePlaces() >= tot) {
					for (DeliveryMan a : this.deliveryMen) {
						if (a.getDeliveryManTimes()[i / 10].isFree()) {
							int newMinutes = getMinutes(getOpeningToday()) + i;   // NON POSSO PARTIRE DA TROVACASELLA MENO 1: RISCHIO ECCEZIONE
							int ora = newMinutes / 60;
							int min = newMinutes % 60;
							String nuovoOrario = TimeServices.timeStamp(ora,min);
							availables.add(nuovoOrario + "  ");
							break;
						}
					}
				}
			}
		}
		if(availables.size() > 0) {
			return availables;
		} else {
			/* se l'ordine inizia in un orario ancora valido, ma impiega troppo tempo e diventa troppo tardi: */
			String spiacenti = "\nSpiacenti: si è fatto tardi, la pizzeria è ormai in chiusura. Torna a trovarci!\n";
			System.out.println(TextualPrintServices.colorSystemOut(spiacenti,Color.RED,false,false));
			return null;
		}
	}

	/** Controlla che la pizzeria possa garantire la consegna di "tot" pizze all'orario "d",
	 * in base alla disponibilità di forno e fattorini. */
	public void updateOvenAndDeliveryMan(Date d, int tot, Order order) {
		// PRIMA CONDIZIONE PER LE INFORNATE, SUCCESSIVA SUI FATTORINI
		int disp = this.ovens[findTimeBoxOven(d.getHours(), d.getMinutes())].getAvailablePlaces();
		if(disp < tot){
			this.ovens[findTimeBoxOven(d.getHours(), d.getMinutes())].insertPizzas(disp);
			this.ovens[findTimeBoxOven(d.getHours(), d.getMinutes())-1].insertPizzas(tot-disp);
		} else {
			this.ovens[findTimeBoxOven(d.getHours(), d.getMinutes())].insertPizzas(tot);
		}
		if(aFreeDeliveryMan(d.getHours(), d.getMinutes()) != null)
			aFreeDeliveryMan(d.getHours(), d.getMinutes()).assignDelivery(findTimeBoxDeliveryMan(d.getHours(), d.getMinutes()));
		else System.out.println("PROBLEMA IN PIZZERIA.UPDATEOVENANDDELIVERYMAN() per " + order.getOrderCode());    //fixme: questo significa problema GRAVE
	}

	public double getSUPPL_PRICE() {
		return this.SUPPL_PRICE;
	}

	public Oven[] getOvens() {
		return this.ovens;
	}

	/** In interfaces.TextualInterface, elenca tutte gli ingredienti che l'utente può scegliere, per modificare una pizza. */
	public String possibleAddictions() {
		StringBuilder possibiliIngr = new StringBuilder();
		int i = 0;
		for (String ingr : getIngredientsPizzeria().values()) {
			if (i % 10 == 0)
				possibiliIngr.append("\n\t");
			possibiliIngr.append(ingr.toLowerCase().replace("_", " ")).append(", ");
			i++;
		}
		return possibiliIngr.toString();
	}

	/** Verifica che sia possibile cuocere le pizze nell'infornata richiesta e in quella appena precedente. */
	public boolean checkTimeBoxOven(int ora, int minuti, int tot) {
		int postiDisponibiliQuestaInfornata = this.ovens[findTimeBoxOven(ora, minuti)].getAvailablePlaces();
		int postiDisponibiliPrecedenteInfornata = this.ovens[findTimeBoxOven(ora, minuti) - 1].getAvailablePlaces();
		return (postiDisponibiliQuestaInfornata + postiDisponibiliPrecedenteInfornata >= tot);
	}

	public int getAvailablePlaces() {
		return this.availablePlaces;
	}

	public int getOVEN_MINUTES() {
		return this.OVEN_MINUTES;
	}

	public int getDELIVERYMAN_MINUTES() {
		return this.DELIVERYMAN_MINUTES;
	}

	public String checkLogin(String user, String psw) throws SQLException {
		if(user.equals(this.userPizzeria) && psw.equals(this.pswPizzeria)){
			/* se è la pizzeria, allora accede come tale */
			return "P";
		} else if (CustomerDB.getCustomer(user,psw)){
			/* se è un utente identificato, accede come tale */
			return "OK";
		} else {
			/* se la combinazione utente-password è errata */
			return "NO";
		}
	}

	/** Controlla, prima di un nuovo ordine, se sei ancora in tempo prima che la pizzeria chiuda. */
	public String checkTimeOrder() {
		int nowMin = getNowMinutes();
		int openMin = getMinutes(getOpeningToday());
		int closeMin = getMinutes(getClosingToday());
		if(closeMin <= nowMin || openMin == closeMin)
			return "CLOSED";
		if(closeMin - nowMin >= 0)// TODO: risistemare alla fine (mettere 20)!! Ho settato a 0 per poter lavorare anche alle 23:50!!!
			return "OPEN";
		else
			return "CLOSING";
	}

	public String canCreateAccount(String mailAddress, String newUser, String newPsw, String confPsw) {
		if(newPsw.equals(confPsw)){
			if(newUser.length() > 2 && newPsw.length() > 2) {
				/* se si registra correttamente, va bene */
				try {
					if (CustomerDB.getCustomer(newUser.toUpperCase(),newPsw) || checkMail(mailAddress))
						return "EXISTING";
					else
						return "OK";
				} catch (SQLException e) {
					return "OK";	// è sicuro ???????????????
				}
			} else
				/* password troppo breve */
				return "SHORT";
		} else {
			/* se la password non viene confermata correttamente */
			return "DIFFERENT";
		}
	}

	public boolean checkMail(String mail){
		return (CustomerDB.getInfoCustomerFromMailAddress(mail,1) != null);
	}

	public Order CustomerLastOrder(Customer customer) {
		Order last = null;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date;
		try {
			date = sdf.parse("1970-01-01 00:00:00");
			for (Order order : this.getOrders().values()) {
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
}