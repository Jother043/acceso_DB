package com.example.demo;

import java.sql.*;

import conn.DataBaseConnection;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Main extends Application {

    private static final String QUERY = "SELECT productCode, MSRP FROM products WHERE productName = ?";
    private static final String QUERY2 = "SELECT * FROM customers";

    @Override
    public void start(Stage stage) throws Exception {
        // Crear controles de interfaz de usuario
        Label label = new Label("Ingrese el nombre del producto:");
        Label label2 = new Label("Búsqueda de productos por nombre");
        TextField textField = new TextField();
        Button button = new Button("Buscar");
        //Crear boton para terminar la aplicacion.
        Button button2 = new Button("Salir");
        Button button3 = new Button("Borrar cliente");
        button2.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                System.exit(0);
            }
        });
        button3.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                //abrir ventana para borrar cliente
                //crear ventana
                Stage stage2 = new Stage();
                //crear controles
                Label label3 = new Label("Ingrese el nombre del cliente a borrar:");
                TextField textField2 = new TextField();
                Button button4 = new Button("Borrar");
                Button button5 = new Button("Volver");
                Button button6 = new Button("Actualizar tabla");

                VBox root2 = new VBox(label3, textField2, button4, button5, crearTabla(), button6);
                button4.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        try {
                            Connection finalDB2 = DataBaseConnection.getInstance().getConexion();
                            borrarCliente(textField2.getText(), finalDB2);
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
                button5.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        stage2.close();

                    }
                });
                button6.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {

                    }
                });
                if(button6.isPressed()){
                    try {
                        Connection finalDB2 = DataBaseConnection.getInstance().getConexion();
                        actualizarTabla(finalDB2, crearTabla());
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
                Scene scene = new Scene(root2, 600, 600);
                stage.setScene(scene);
                stage.show();


            }
        });

        TableView<Producto> table = new TableView<>();
        TableColumn<Producto, String> productCode = new TableColumn<>("productCode");
        TableColumn<Producto, Double> msrp = new TableColumn<>("MSRP");
        table.getColumns().addAll(productCode, msrp);
        VBox root = new VBox(label, textField, button, table, label2);
        // Asignar las propiedades a las columnas
        productCode.setCellValueFactory(new PropertyValueFactory<>("productCode"));
        msrp.setCellValueFactory(new PropertyValueFactory<>("MSRP"));
        //Creamos la variable DB para poder cerrar la conexion.

        //Creamos un do while para que el botón de salir cierre la conexión.

        Connection finalDB = DataBaseConnection.getInstance().getConexion();
        // Establecer el controlador de eventos para el botón
        button.setOnAction(new EventHandler<ActionEvent>() {
            //Creamos un evento para que al presionar el botón de buscar, se ejecute la consulta.
            @Override
            public void handle(ActionEvent event) {
                buscarProducto(textField, table, finalDB);
            }

        });
        //si el botón de salir es presionado, se cierra la conexión.
        if (button2.isPressed()) {
            finalDB.close();
            System.out.println("Conexión cerrada");
        }
        if(button3.isPressed()){
            finalDB.close();
            System.out.println("Conexión cerrada");
        }

        // Mostrar la interfaz de usuario
        Scene scene = new Scene(root, 800, 800);
        stage.setScene(scene);
        stage.show();

        //agregamos botón para salir de la aplicación.
        root.getChildren().add(button2);
        root.getChildren().add(button3);


    }

    public static void main(String[] args) {
        launch(args);
    }

    public void buscarProducto(TextField textField, TableView<Producto> table, Connection finalDB) {

        try {
            // Ejecutar la consulta y obtener los resultados
            PreparedStatement stmt = finalDB.prepareStatement(QUERY);
            stmt.setString(1, textField.getText());
            ResultSet rs = stmt.executeQuery();

            // Limpiar la tabla existente
            table.getItems().clear();

            // Agregar los datos a la tabla
            while (rs.next()) {
                String id = rs.getString("productCode");
                Double msrp1 = rs.getDouble("MSRP");

                table.getItems().add(new Producto(id, textField.getText(), msrp1));
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

    }

    public void borrarCliente(String customersName, Connection finalDB2) {


        PreparedStatement stmt = null;
        try {
            // Deshabilitar la confirmación automática de la transacción
            finalDB2.setAutoCommit(false);

            // Iniciar la transacción
            System.out.println("Comenzando la transacción...");
            stmt = finalDB2.prepareStatement("START TRANSACTION ");

            // Elimina los pagos correspondientes al cliente
            String sql = "DELETE FROM payments WHERE customerNumber = (SELECT customerNumber FROM customers WHERE customerName = ? ORDER BY customerNumber DESC LIMIT 1)";
            stmt = finalDB2.prepareStatement(sql);
            stmt.setString(1, customersName);
            stmt.executeUpdate();

            // Elimina las órdenes correspondientes al cliente
            sql = "DELETE FROM orders WHERE customerNumber = (SELECT customerNumber FROM customers WHERE customerName = ? ORDER BY customerNumber DESC LIMIT 1)";
            stmt = finalDB2.prepareStatement(sql);
            stmt.setString(1, customersName);
            stmt.executeUpdate();

            // Elimina el cliente
            sql = "DELETE FROM customers WHERE customerName = ?";
            stmt = finalDB2.prepareStatement(sql);
            stmt.setString( 1, customersName);
            stmt.executeUpdate();

            finalDB2.commit();

            System.out.println("Transacción completada correctamente.");
        } catch (SQLException e) {
            // Deshacer la transacción en caso de algún error
            if (finalDB2 != null) {
                try {
                    finalDB2.rollback();
                } catch (SQLException ex) {
                    System.out.println("Error al deshacer la transacción: " + ex.getMessage());
                }
            }
            System.out.println("Error en la transacción: " + e.getMessage());
        } finally {
            // Cerrar los recursos JDBC
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }

    }

    public void actualizarTabla(Connection finalDB2, TableView<Customers> table2){

        try {
            // Ejecutar la consulta y obtener los resultados
            Statement stmt = finalDB2.createStatement();
            ResultSet rs = stmt.executeQuery(QUERY2);

            // Limpiar la tabla existente
            table2.getItems().clear();

            // Agregar los datos a la tabla
            while (rs.next()) {
                String customerName = rs.getString("customerName");
                String contactLastName = rs.getString("contactLastName");
                String contactFirstName = rs.getString("contactFirstName");
                String phone = rs.getString("phone");
                String addressLine1 = rs.getString("addressLine1");
                String addressLine2 = rs.getString("addressLine2");
                String city = rs.getString("city");
                String state = rs.getString("state");
                String postalCode = rs.getString("postalCode");
                String country = rs.getString("country");
                String salesRepEmployeeNumber = rs.getString("salesRepEmployeeNumber");
                String creditLimit = rs.getString("creditLimit");

                table2.getItems().add(new Customers(customerName, contactLastName, contactFirstName, phone, addressLine1, addressLine2, city, state, postalCode, country, salesRepEmployeeNumber, creditLimit));
            }



        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

    }

    public TableView<Customers> crearTabla() {
        TableView<Customers> table2 = new TableView<>();
        TableColumn<Customers, String> customerName = new TableColumn<>("customerName");
        TableColumn<Customers, String> contactLastName = new TableColumn<>("contactLastName");
        TableColumn<Customers, String> contactFirstName = new TableColumn<>("contactFirstName");
        TableColumn<Customers, String> phone = new TableColumn<>("phone");
        TableColumn<Customers, String> addressLine1 = new TableColumn<>("addressLine1");
        TableColumn<Customers, String> addressLine2 = new TableColumn<>("addressLine2");
        TableColumn<Customers, String> city = new TableColumn<>("city");
        TableColumn<Customers, String> state = new TableColumn<>("state");
        TableColumn<Customers, String> postalCode = new TableColumn<>("postalCode");
        TableColumn<Customers, String> country = new TableColumn<>("country");
        TableColumn<Customers, String> salesRepEmployeeNumber = new TableColumn<>("salesRepEmployeeNumber");
        TableColumn<Customers, String> creditLimit = new TableColumn<>("creditLimit");
        //crear tabla
        table2.getColumns().addAll(customerName, contactLastName, contactFirstName, phone, addressLine1, addressLine2, city, state, postalCode, country, salesRepEmployeeNumber, creditLimit);
        // Asignar las propiedades a las columnas
        customerName.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        contactLastName.setCellValueFactory(new PropertyValueFactory<>("contactLastName"));
        contactFirstName.setCellValueFactory(new PropertyValueFactory<>("contactFirstName"));
        phone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        addressLine1.setCellValueFactory(new PropertyValueFactory<>("addressLine1"));
        addressLine2.setCellValueFactory(new PropertyValueFactory<>("addressLine2"));
        city.setCellValueFactory(new PropertyValueFactory<>("city"));
        state.setCellValueFactory(new PropertyValueFactory<>("state"));
        postalCode.setCellValueFactory(new PropertyValueFactory<>("postalCode"));
        country.setCellValueFactory(new PropertyValueFactory<>("country"));
        salesRepEmployeeNumber.setCellValueFactory(new PropertyValueFactory<>("salesRepEmployeeNumber"));
        creditLimit.setCellValueFactory(new PropertyValueFactory<>("creditLimit"));
        return table2;
    }

}