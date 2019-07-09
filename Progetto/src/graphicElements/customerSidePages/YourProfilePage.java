package graphicElements.customerSidePages;

import graphicAlerts.GenericAlert;
import graphicElements.customerSidePages.newOrder.HomePage;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import pizzeria.Customer;
import database.Database;
import pizzeria.Pizzeria;

public class YourProfilePage {

    public void display(Stage window, Pizzeria pizzeria, Customer customer) {

        /*Label usernameLabel = new Label(" Username:");
        TextField usernameInput = new TextField();
        usernameInput.setText(customer.getUsername());
        HBox usernameBox = new HBox(50);
        usernameBox.getChildren().addAll(usernameLabel, usernameInput);
        usernameBox.setAlignment(Pos.CENTER);*/

        Label nameLabel = new Label("Nome:\t ");
        TextField nameInput = new TextField();
        nameInput.setPromptText("Il tuo nome");
        String name = Database.getInfoCustomerFromUsername(customer.getUsername(), 4);
        nameInput.setText(name);
        HBox nameBox = new HBox(50);
        nameBox.getChildren().addAll(nameLabel, nameInput);
        nameBox.setAlignment(Pos.CENTER);

        Label surnameLabel = new Label("Cognome: ");
        TextField surnameInput = new TextField();
        surnameInput.setPromptText("Il tuo cognome");
        String surname = Database.getInfoCustomerFromUsername(customer.getUsername(), 5);
        surnameInput.setText(surname);
        HBox surnameBox = new HBox(50);
        surnameBox.getChildren().addAll(surnameLabel, surnameInput);
        surnameBox.setAlignment(Pos.CENTER);

        Label addressLabel = new Label("Indirizzo: ");
        TextField addressInput = new TextField();
        addressInput.setPromptText("Il tuo indirizzo");
        String address = Database.getInfoCustomerFromUsername(customer.getUsername(), 6);
        addressInput.setText(address);
        HBox addressBox = new HBox(50);
        addressBox.getChildren().addAll(addressLabel, addressInput);
        addressBox.setAlignment(Pos.CENTER);

        /*Label passwordLabel = new Label("Password: ");
        TextField passwordInput = new TextField();
        HBox passwordBox = new HBox(50);
        passwordBox.getChildren().addAll(passwordLabel, passwordInput);
        passwordBox.setAlignment(Pos.CENTER);*/

        Button backButton = new Button("← Torna indietro");
        backButton.setId("backButton");
        backButton.setOnAction(e -> {
           HomePage homePage = new HomePage();
           homePage.display(window, pizzeria, customer);
        });

        Button confirmButton = new Button(" Conferma modifiche");
        confirmButton.setOnAction(e-> {
            //customer.setUsername(usernameInput.getText());
            customer.setName(nameInput.getText());
            customer.setSurname(surnameInput.getText());
            customer.setAddress(addressInput.getText());
            //customer.setPassword(passwordInput.getText());
            if(Database.addInfoCustomer(customer.getUsername(),customer.getName(),customer.getSurname(),customer.getAddress())) {
                HomePage homePage = new HomePage();
                homePage.display(window, pizzeria, customer);
            } else GenericAlert.display("Modifica dei dati non riuscita.");
        });

        HBox buttonBox = new HBox(10);
        buttonBox.getChildren().addAll(backButton, confirmButton);
        buttonBox.setAlignment(Pos.CENTER);

        VBox layout = new VBox(30);
        layout.getChildren().addAll(/*usernameBox,*/ nameBox, surnameBox, addressBox, buttonBox);
        layout.setAlignment(Pos.CENTER);
        Scene scene = new Scene(layout, 800, 600);
        window.setScene(scene);
        window.show();
    }

    //TODO: manca CSS
}
