package com.example;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import com.example.models.Farmer;
import com.example.models.Product;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.util.Optional;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import com.example.models.Customer;
import javafx.animation.FadeTransition;
import javafx.util.Duration;
import javafx.scene.shape.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Main extends Application {

    private BorderPane root;
    private StackPane leftContentPane;
    private Scene scene;
    private Stage primaryStage;
    private ObservableList<Product> productsList;
    private TableView<Product> productsTable;
    private Farmer demoFarmer;

    private static final String COLOR_PRIMARY_GREEN = "#22c55e"; // fresh green accent
    private static final String COLOR_BLACK = "#000000";
    private static final String COLOR_WHITE = "#ffffff";
    private static final String COLOR_GRAY_TEXT = "#6b7280";

    private List<CartItem> cartItems = new ArrayList<>();
    private Label cartBadge;
    private Customer currentCustomer;

    private static Connection dbConnection;

    private static class CartItem {
        private String name;
        private double price;
        private String unit;
        private int quantity;

        public CartItem(String name, double price, String unit, int quantity) {
            this.name = name;
            this.price = price;
            this.unit = unit;
            this.quantity = quantity;
        }

        public String getName() {
            return name;
        }

        public double getPrice() {
            return price;
        }

        public String getUnit() {
            return unit;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }
    }

    private void updateCartBadge() {
        if (cartBadge != null) {
            int totalItems = cartItems.stream().mapToInt(CartItem::getQuantity).sum();
            cartBadge.setText(String.valueOf(totalItems));
            cartBadge.setVisible(totalItems > 0);
        }
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.productsList = FXCollections.observableArrayList();

        // Initialize H2 database connection
        try {
            dbConnection = DriverManager.getConnection("jdbc:h2:~/farmers_customers_db;MODE=MySQL", "sa", "");
            System.out.println("H2 database connected successfully.");

            // Create tables if they do not exist
            String createFarmerTable = "CREATE TABLE IF NOT EXISTS Farmer (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(255), " +
                    "username VARCHAR(255), " +
                    "email VARCHAR(255), " +
                    "phone VARCHAR(50), " +
                    "farmName VARCHAR(255), " +
                    "farmLocation VARCHAR(255), " +
                    "password VARCHAR(255)" +
                    ")";
            String createCustomerTable = "CREATE TABLE IF NOT EXISTS Customer (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(255), " +
                    "username VARCHAR(255), " +
                    "email VARCHAR(255), " +
                    "phone VARCHAR(50), " +
                    "address VARCHAR(255), " +
                    "password VARCHAR(255)" +
                    ")";
            String createProductTable = "CREATE TABLE IF NOT EXISTS Product (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(255), " +
                    "price DOUBLE, " +
                    "description VARCHAR(1024), " +
                    "unit VARCHAR(50), " +
                    "quantity INT, " +
                    "farmerId BIGINT, " +
                    "imagePath VARCHAR(255), " +
                    "FOREIGN KEY (farmerId) REFERENCES Farmer(id)" +
                    ")";
            String createOrderTable = "CREATE TABLE IF NOT EXISTS Orders (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "customerId BIGINT, " +
                    "productId BIGINT, " +
                    "quantity INT, " +
                    "orderDate TIMESTAMP, " +
                    "status VARCHAR(50), " +
                    "FOREIGN KEY (customerId) REFERENCES Customer(id), " +
                    "FOREIGN KEY (productId) REFERENCES Product(id)" +
                    ")";

            dbConnection.createStatement().execute(createFarmerTable);
            dbConnection.createStatement().execute(createCustomerTable);
            dbConnection.createStatement().execute(createProductTable);
            dbConnection.createStatement().execute(createOrderTable);
            System.out.println("Tables created or already exist.");
        } catch (SQLException e) {
            System.err.println("Failed to connect to H2 database or create tables: " + e.getMessage());
            return;
        }

        // Create demo farmer
        this.demoFarmer = new Farmer(
                "John Smith",
                "johnsmith",
                "john@organicfarm.com",
                "555-0123",
                "Green Valley Organic Farm",
                "123 Farm Road, Green Valley, CA 90210",
                "password");

        // Add a demo product
        Product demoProduct = new Product(
                "Organic Tomatoes",
                4.99,
                "Fresh organic tomatoes grown with care. Perfect for salads and cooking.",
                "kg",
                50,
                demoFarmer.getId());
        demoProduct.setImagePath("/com/example/images/download.jpeg");
        productsList.add(demoProduct);

        root = new BorderPane();

        // --- Splash Animation Pane ---
        StackPane splashPane = new StackPane();
        splashPane.setStyle("-fx-background-color: white;");
        ImageView splashLogo = null;
        try {
            Image logo = new Image(getClass().getResourceAsStream("/com/example/images/logo.png"));
            splashLogo = new ImageView(logo);
            splashLogo.setFitHeight(160);
            splashLogo.setFitWidth(160);
            splashLogo.setPreserveRatio(true);
            splashLogo.setSmooth(true);
            splashLogo.setCache(true);
        } catch (Exception e) {
            // If logo fails to load, do nothing
        }
        if (splashLogo != null)
            splashPane.getChildren().add(splashLogo);
        splashPane.setAlignment(Pos.CENTER);

        Scene splashScene = new Scene(splashPane, 1060, 600);
        try {
            splashScene.getStylesheets().add(getClass().getResource("/com/example/styles.css").toExternalForm());
        } catch (Exception e) {
            System.err.println("Could not load styles.css: " + e.getMessage());
        }
        primaryStage.setScene(splashScene);
        primaryStage.setTitle("Farmers & Customers Interaction App");
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(560);
        primaryStage.show();

        // Animate splash (fade in, then fade out, then show login)
        javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(600),
                splashPane);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.setCycleCount(1);
        fadeIn.setOnFinished(ev -> {
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                    javafx.util.Duration.seconds(2.4));
            pause.setOnFinished(ev2 -> {
                javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(
                        javafx.util.Duration.millis(600), splashPane);
                fadeOut.setFromValue(1);
                fadeOut.setToValue(0);
                fadeOut.setCycleCount(1);
                fadeOut.setOnFinished(ev3 -> {
                    // After animation, show main login as pane
                    leftContentPane = new StackPane();
                    leftContentPane.setPadding(new Insets(32));
                    leftContentPane.setMaxWidth(520);
                    leftContentPane.getStyleClass().add("container-box");
                    leftContentPane.getChildren().add(createLoginChoicePane());

                    VBox leftPane = new VBox(leftContentPane);
                    leftPane.setPadding(new Insets(40));
                    leftPane.setAlignment(Pos.CENTER);
                    leftPane.setPrefWidth(540);
                    leftPane.setStyle("-fx-background-color: " + COLOR_WHITE + ";");
                    BorderPane.setAlignment(leftPane, Pos.CENTER);
                    root.setLeft(leftPane);

                    // Load image from resources
                    try {
                        Image localImage = new Image(
                                getClass().getResourceAsStream("/com/example/images/greenfield.jpeg"));
                        ImageView imageView = new ImageView(localImage);
                        imageView.setSmooth(true);
                        imageView.setCache(true);

                        StackPane rightPane = new StackPane(imageView);
                        rightPane.setStyle("-fx-background-color: #e6f7ff;"); // subtle soft background for contrast
                        rightPane.setPrefWidth(640);
                        rightPane.setAlignment(Pos.CENTER);
                        rightPane.setPadding(new Insets(40));
                        imageView.fitWidthProperty().bind(rightPane.widthProperty().subtract(100));
                        imageView.fitHeightProperty().bind(rightPane.heightProperty().subtract(80));
                        imageView.setPreserveRatio(false);
                        BorderPane.setAlignment(rightPane, Pos.CENTER);
                        root.setRight(rightPane);
                    } catch (Exception e) {
                        // If image loading fails, create a simple colored background
                        StackPane rightPane = new StackPane();
                        rightPane.setStyle("-fx-background-color: #e6f7ff;");
                        rightPane.setPrefWidth(640);
                        BorderPane.setAlignment(rightPane, Pos.CENTER);
                        root.setRight(rightPane);
                    }

                    scene = new Scene(root, 1060, 600);
                    try {
                        scene.getStylesheets().add(getClass().getResource("/com/example/styles.css").toExternalForm());
                    } catch (Exception e) {
                        System.err.println("Could not load styles.css: " + e.getMessage());
                    }

                    primaryStage.setScene(scene);
                });
                fadeOut.play();
            });
            pause.play();
        });
        fadeIn.play();
    }

    private VBox createLoginChoicePane() {
        VBox container = new VBox(28); // more spacious spacing
        container.setFillWidth(true);
        container.setMaxWidth(380);
        container.setAlignment(Pos.CENTER);

        // --- Add circular, centered logo at the top ---
        StackPane logoCircle = new StackPane();
        logoCircle.setAlignment(Pos.CENTER);
        logoCircle.setPrefSize(128, 128);
        logoCircle.setMaxSize(128, 128);
        logoCircle.setMinSize(128, 128);
        logoCircle.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 64;");
        ImageView logoView = null;
        try {
            Image logo = new Image(getClass().getResourceAsStream("/com/example/images/logo.png"));
            logoView = new ImageView(logo);
            logoView.setFitHeight(110);
            logoView.setFitWidth(110);
            logoView.setPreserveRatio(true);
            logoView.setSmooth(true);
            logoView.setCache(true);
            // Make logo circular
            javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(55, 55, 55);
            logoView.setClip(clip);
        } catch (Exception e) {
            // If logo fails to load, do nothing
        }
        if (logoView != null)
            logoCircle.getChildren().add(logoView);
        VBox.setMargin(logoCircle, new Insets(0, 0, 18, 0));

        Label title = new Label("Login as");
        title.getStyleClass().add("title-label");
        title.setAlignment(Pos.CENTER);
        title.setMaxWidth(Double.MAX_VALUE);
        title.setStyle("-fx-font-size: 22px;");

        Button customerBtn = new Button("Customer");
        Button farmerBtn = new Button("Farmer");
        customerBtn.getStyleClass().add("button-primary");
        farmerBtn.getStyleClass().add("button-primary");
        customerBtn.setMinWidth(90);
        farmerBtn.setMinWidth(90);
        customerBtn.setMaxWidth(Double.MAX_VALUE);
        farmerBtn.setMaxWidth(Double.MAX_VALUE);
        customerBtn.setAlignment(Pos.CENTER);
        farmerBtn.setAlignment(Pos.CENTER);
        customerBtn.setPrefHeight(40);
        farmerBtn.setPrefHeight(40);
        customerBtn.setStyle("-fx-font-size: 16px;");
        farmerBtn.setStyle("-fx-font-size: 16px;");

        VBox buttonBox = new VBox(16, customerBtn, farmerBtn);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setMaxWidth(Double.MAX_VALUE);

        Label signUpLink = new Label("Don't have an account? Sign up");
        signUpLink.getStyleClass().add("link-label");
        signUpLink.setAlignment(Pos.CENTER);
        signUpLink.setMaxWidth(Double.MAX_VALUE);
        signUpLink.setStyle("-fx-font-size: 15px;");

        customerBtn.setOnAction(e -> switchToLoginForm("Customer"));
        farmerBtn.setOnAction(e -> switchToLoginForm("Farmer"));
        signUpLink.setOnMouseClicked(e -> switchToSignUpForm());

        container.getChildren().addAll(
                logoCircle,
                title,
                buttonBox,
                signUpLink);

        // No ScrollPane, just a large, centered VBox
        VBox outer = new VBox(container);
        outer.setAlignment(Pos.CENTER);
        outer.setFillWidth(true);
        VBox.setVgrow(container, Priority.ALWAYS);
        return outer;
    }

    private void switchToLoginForm(String role) {
        VBox form = createLoginForm(role);
        swapLeftContent(form);
    }

    private void switchToSignUpForm() {
        VBox form = createSignUpForm();
        swapLeftContent(form);
    }

    private void showForgotPasswordDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Forgot Password");
        alert.setHeaderText(null);
        alert.setContentText("Please contact support at support@farmersapp.com to reset your password.");
        alert.showAndWait();
    }

    private void swapLeftContent(VBox newContent) {
        leftContentPane.getChildren().clear();
        leftContentPane.getChildren().add(newContent);
    }

    private VBox createLoginForm(String role) {
        // --- Add circular, centered logo at the top ---
        StackPane logoCircle = new StackPane();
        logoCircle.setAlignment(Pos.CENTER);
        logoCircle.setPrefSize(128, 128);
        logoCircle.setMaxSize(128, 128);
        logoCircle.setMinSize(128, 128);
        logoCircle.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 64;");
        ImageView logoView = null;
        try {
            Image logo = new Image(getClass().getResourceAsStream("/com/example/images/logo.png"));
            logoView = new ImageView(logo);
            logoView.setFitHeight(110);
            logoView.setFitWidth(110);
            logoView.setPreserveRatio(true);
            logoView.setSmooth(true);
            logoView.setCache(true);
            // Make logo circular
            javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(55, 55, 55);
            logoView.setClip(clip);
        } catch (Exception e) {
            // If logo fails to load, do nothing
        }
        if (logoView != null)
            logoCircle.getChildren().add(logoView);
        VBox.setMargin(logoCircle, new Insets(0, 0, 18, 0));

        // --- Minimized login form ---
        VBox container = new VBox(10); // reduced spacing
        container.setFillWidth(true);
        container.setMaxWidth(320);
        container.setAlignment(Pos.CENTER);
        container.getStyleClass().add("container-box");

        Label header = new Label(role + " Login");
        header.getStyleClass().add("title-label");
        VBox.setMargin(header, new Insets(0, 0, 6, 0));

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.setMaxWidth(Double.MAX_VALUE);
        usernameField.setPrefHeight(32);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setMaxWidth(Double.MAX_VALUE);
        passwordField.setPrefHeight(32);

        final TextField farmNameField = "Farmer".equals(role) ? new TextField() : null;
        if (farmNameField != null) {
            farmNameField.setPromptText("Farm Name");
            farmNameField.setMaxWidth(Double.MAX_VALUE);
            farmNameField.setPrefHeight(32);
            container.getChildren().addAll(header, usernameField, farmNameField, passwordField);
        } else {
            container.getChildren().addAll(header, usernameField, passwordField);
        }

        Button loginBtn = new Button("Sign In");
        loginBtn.getStyleClass().add("button-black");
        loginBtn.setMaxWidth(160);
        loginBtn.setMinWidth(120);
        loginBtn.setPrefHeight(34);
        VBox.setMargin(loginBtn, new Insets(10, 0, 0, 0));

        // Add login button action
        loginBtn.setOnAction(e -> {
            if ("Farmer".equals(role)) {
                Farmer farmer = new Farmer(
                        "Demo Farmer",
                        usernameField.getText(),
                        "demo@farm.com",
                        "1234567890",
                        farmNameField != null ? farmNameField.getText() : "Demo Farm",
                        "Demo Location",
                        passwordField.getText());
                showFarmerDashboard(farmer);
            } else {
                Customer customer = new Customer(
                        "Demo Customer",
                        usernameField.getText(),
                        "demo@customer.com",
                        "0987654321",
                        passwordField.getText());
                showCustomerDashboard(customer);
            }
        });

        Label backLink = new Label("← Back");
        backLink.getStyleClass().add("link-label");
        backLink.setOnMouseClicked(e -> swapLeftContent(createLoginChoicePane()));
        backLink.setMaxWidth(Double.MAX_VALUE);
        backLink.setAlignment(Pos.BOTTOM_LEFT);
        VBox.setMargin(backLink, new Insets(8, 0, 0, 0));

        container.getChildren().addAll(loginBtn, backLink);

        VBox outer = new VBox(logoCircle, container);
        outer.setAlignment(Pos.TOP_CENTER);
        outer.setSpacing(8);
        return outer;
    }

    private VBox createSignUpForm() {
        VBox container = new VBox(16);
        container.setFillWidth(true);
        container.setMaxWidth(380);
        container.getStyleClass().add("container-box");

        // --- Add circular, centered logo at the top ---
        StackPane logoCircle = new StackPane();
        logoCircle.setAlignment(Pos.CENTER);
        logoCircle.setPrefSize(120, 120);
        logoCircle.setMaxSize(120, 120);
        logoCircle.setMinSize(120, 120);
        logoCircle.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 60;");
        ImageView logoView = null;
        try {
            Image logo = new Image(getClass().getResourceAsStream("/com/example/images/logo.png"));
            logoView = new ImageView(logo);
            logoView.setFitHeight(128);
            logoView.setFitWidth(128);
            logoView.setPreserveRatio(true);
            logoView.setSmooth(true);
            logoView.setCache(true);
            // Make logo circular
            javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(48, 48, 48);
            logoView.setClip(clip);
        } catch (Exception e) {
            // If logo fails to load, do nothing
        }
        if (logoView != null)
            logoCircle.getChildren().add(logoView);
        VBox.setMargin(logoCircle, new Insets(0, 0, 24, 0));

        Label header = new Label("Create an Account");
        header.getStyleClass().add("title-label");

        Label backLink = new Label("← Back");
        backLink.getStyleClass().add("link-label");
        backLink.setOnMouseClicked(e -> swapLeftContent(createLoginChoicePane()));
        VBox.setMargin(backLink, new Insets(0, 0, 10, 0));

        TextField fullName = new TextField();
        fullName.setPromptText("Full Name");
        fullName.setMaxWidth(Double.MAX_VALUE);

        TextField username = new TextField();
        username.setPromptText("Username");
        username.setMaxWidth(Double.MAX_VALUE);

        TextField email = new TextField();
        email.setPromptText("Email");
        email.setMaxWidth(Double.MAX_VALUE);

        TextField phoneNumber = new TextField();
        phoneNumber.setPromptText("Phone Number");
        phoneNumber.setMaxWidth(Double.MAX_VALUE);

        TextField farmName = new TextField();
        farmName.setPromptText("Farm Name");
        farmName.setMaxWidth(Double.MAX_VALUE);

        TextField farmLocation = new TextField();
        farmLocation.setPromptText("Farm Location");
        farmLocation.setMaxWidth(Double.MAX_VALUE);

        PasswordField password = new PasswordField();
        password.setPromptText("Password");
        password.setMaxWidth(Double.MAX_VALUE);

        PasswordField confirmPassword = new PasswordField();
        confirmPassword.setPromptText("Confirm Password");
        confirmPassword.setMaxWidth(Double.MAX_VALUE);

        ToggleGroup roleGroup = new ToggleGroup();
        RadioButton customerRadio = new RadioButton("Customer");
        RadioButton farmerRadio = new RadioButton("Farmer");
        customerRadio.setToggleGroup(roleGroup);
        farmerRadio.setToggleGroup(roleGroup);
        customerRadio.setSelected(true);

        HBox roleBox = new HBox(32, customerRadio, farmerRadio);
        roleBox.setAlignment(Pos.CENTER_LEFT);

        Button signUpBtn = new Button("Sign Up");
        signUpBtn.getStyleClass().add("button-black");
        signUpBtn.setMaxWidth(205);
        signUpBtn.setMinWidth(205);
        VBox.setMargin(signUpBtn, new Insets(16, 0, 0, 0));

        // Center align the button
        HBox signUpButtonContainer = new HBox(signUpBtn);
        signUpButtonContainer.setAlignment(Pos.CENTER);
        signUpButtonContainer.setMaxWidth(Double.MAX_VALUE);

        signUpBtn.setOnAction(e -> {
            if (validateSignUpForm(fullName, username, email, phoneNumber, farmName, farmLocation,
                    password, confirmPassword, roleGroup)) {
                if (farmerRadio.isSelected()) {
                    Farmer farmer = new Farmer(
                            fullName.getText(),
                            username.getText(),
                            email.getText(),
                            phoneNumber.getText(),
                            farmName.getText(),
                            farmLocation.getText(),
                            password.getText());
                    showAlert("Success", "Farmer account created successfully!");
                    // Switch to login form after successful signup
                    swapLeftContent(createLoginForm("Farmer"));
                } else {
                    // Handle customer signup
                    showAlert("Success", "Customer account created successfully!");
                    // Switch to login form after successful signup
                    swapLeftContent(createLoginForm("Customer"));
                }
            }
        });

        container.getChildren().addAll(
                backLink,
                header,
                fullName,
                username,
                email,
                phoneNumber,
                farmName,
                farmLocation,
                password,
                confirmPassword,
                new Label("Sign up as:"),
                roleBox,
                signUpButtonContainer);

        // Wrap the container in a modern ScrollPane
        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.getStyleClass().add("modern-scroll-pane"); // <-- Add this style class

        VBox outer = new VBox(logoCircle, scrollPane);
        outer.setAlignment(Pos.TOP_CENTER);
        return outer;
        // Reminder: Add modern scrollbar styles to styles.css for .modern-scroll-pane
    }

    private boolean validateSignUpForm(TextField fullName, TextField username, TextField email,
            TextField phoneNumber, TextField farmName, TextField farmLocation,
            PasswordField password, PasswordField confirmPassword,
            ToggleGroup roleGroup) {
        if (fullName.getText().isEmpty() || username.getText().isEmpty() ||
                email.getText().isEmpty() || phoneNumber.getText().isEmpty() ||
                farmName.getText().isEmpty() || farmLocation.getText().isEmpty() ||
                password.getText().isEmpty() || confirmPassword.getText().isEmpty()) {
            showAlert("Error", "Please fill in all fields");
            return false;
        }

        if (!password.getText().equals(confirmPassword.getText())) {
            showAlert("Error", "Passwords do not match");
            return false;
        }

        if (roleGroup.getSelectedToggle() == null) {
            showAlert("Error", "Please select a role");
            return false;
        }

        return true;
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showFarmerDashboard(Farmer farmer) {
        BorderPane dashboardRoot = new BorderPane();
        dashboardRoot.setStyle("-fx-background-color: #f5f5f5;");

        // Top Navigation Bar
        HBox topBar = createTopBar(farmer);
        dashboardRoot.setTop(topBar);

        // Left Sidebar
        VBox sidebar = createSidebar();
        dashboardRoot.setLeft(sidebar);

        // Main Content Area with ScrollPane
        ScrollPane scrollPane = new ScrollPane(createMainContent(farmer));
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        dashboardRoot.setCenter(scrollPane);

        Scene dashboardScene = new Scene(dashboardRoot, 1200, 800);
        dashboardScene.getStylesheets().add(getClass().getResource("/com/example/styles.css").toExternalForm());

        primaryStage.setTitle("Farmer Dashboard - " + farmer.getFarmName());
        primaryStage.setScene(dashboardScene);
    }

    private HBox createTopBar(Farmer farmer) {
        HBox topBar = new HBox();
        topBar.setStyle("-fx-background-color: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        topBar.setPadding(new Insets(15));
        topBar.setSpacing(20);
        topBar.setAlignment(Pos.CENTER_LEFT);

        // --- Add logo at the left ---
        ImageView logoView = null;
        try {
            Image logo = new Image(getClass().getResourceAsStream("/com/example/images/logo.png"));
            logoView = new ImageView(logo);
            logoView.setFitHeight(128);
            logoView.setPreserveRatio(true);
            logoView.setSmooth(true);
            logoView.setCache(true);
            HBox.setMargin(logoView, new Insets(0, 18, 0, 0));
        } catch (Exception e) {
            // If logo fails to load, do nothing
        }

        Label welcomeLabel = new Label("Welcome, " + farmer.getFullName());
        welcomeLabel.setFont(Font.font("System", FontWeight.BOLD, 16));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Create profile picture container
        StackPane profileContainer = new StackPane();
        profileContainer.setStyle("-fx-background-color: #e5e7eb; -fx-background-radius: 20;");
        profileContainer.setPrefSize(40, 40);

        // Create profile picture
        ImageView profilePicture = new ImageView();
        try {
            Image profileImage = new Image(getClass().getResourceAsStream("/com/example/images/farmer-profile.png"));
            profilePicture.setImage(profileImage);
        } catch (Exception e) {
            // If image loading fails, show initials
            Label initials = new Label(farmer.getFullName().substring(0, 1));
            initials.setFont(Font.font("Roboto", FontWeight.BOLD, 20));
            initials.setTextFill(Color.WHITE);
            profileContainer.getChildren().add(initials);
        }

        profilePicture.setFitWidth(40);
        profilePicture.setFitHeight(40);
        profilePicture.setPreserveRatio(true);
        profilePicture.setSmooth(true);
        profileContainer.getChildren().add(profilePicture);

        // Add hover effect
        profileContainer.setOnMouseEntered(e -> {
            profileContainer.setStyle("-fx-background-color: #d1d5db; -fx-background-radius: 20; -fx-cursor: hand;");
        });
        profileContainer.setOnMouseExited(e -> {
            profileContainer.setStyle("-fx-background-color: #e5e7eb; -fx-background-radius: 20;");
        });

        // Add click handler to show profile menu
        profileContainer.setOnMouseClicked(e -> showProfileMenu(profileContainer, farmer));

        if (logoView != null) {
            topBar.getChildren().add(logoView);
        }
        topBar.getChildren().addAll(welcomeLabel, spacer, profileContainer);
        return topBar;
    }

    private void showProfileMenu(StackPane profileContainer, Farmer farmer) {
        ContextMenu profileMenu = new ContextMenu();

        MenuItem viewProfile = new MenuItem("View Profile");
        MenuItem editProfile = new MenuItem("Edit Profile");
        MenuItem settings = new MenuItem("Settings");
        SeparatorMenuItem separator = new SeparatorMenuItem();
        MenuItem logout = new MenuItem("Logout");

        // Style menu items
        String menuItemStyle = "-fx-font-family: 'Roboto'; -fx-font-size: 14px; -fx-padding: 8 16;";
        viewProfile.setStyle(menuItemStyle);
        editProfile.setStyle(menuItemStyle);
        settings.setStyle(menuItemStyle);
        logout.setStyle(menuItemStyle);

        // Add action handlers
        viewProfile.setOnAction(e -> showProfileDetails(farmer));
        editProfile.setOnAction(e -> showEditProfileDialog(farmer));
        settings.setOnAction(e -> showSettingsDialog());
        logout.setOnAction(e -> {
            primaryStage.setScene(scene);
            primaryStage.setTitle("Farmers & Customers Interaction App");
        });

        profileMenu.getItems().addAll(viewProfile, editProfile, settings, separator, logout);
        profileMenu.show(profileContainer, Side.BOTTOM, 0, 0);
    }

    private void showProfileDetails(Farmer farmer) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Profile Details");
        dialog.setHeaderText(farmer.getFullName() + "'s Profile");

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        // Profile picture
        StackPane profilePicture = new StackPane();
        profilePicture.setStyle("-fx-background-color: #e5e7eb; -fx-background-radius: 50;");
        profilePicture.setPrefSize(100, 100);

        ImageView profileImage = new ImageView();
        try {
            Image image = new Image(getClass().getResourceAsStream("/com/example/images/farmer-profile.png"));
            profileImage.setImage(image);
        } catch (Exception e) {
            Label initials = new Label(farmer.getFullName().substring(0, 1));
            initials.setFont(Font.font("Roboto", FontWeight.BOLD, 40));
            initials.setTextFill(Color.WHITE);
            profilePicture.getChildren().add(initials);
        }

        profileImage.setFitWidth(100);
        profileImage.setFitHeight(100);
        profileImage.setPreserveRatio(true);
        profileImage.setSmooth(true);
        profilePicture.getChildren().add(profileImage);

        // Profile details
        VBox details = new VBox(10);
        details.setStyle("-fx-font-family: 'Roboto';");

        Label nameLabel = new Label("Name: " + farmer.getFullName());
        Label farmLabel = new Label("Farm: " + farmer.getFarmName());
        Label emailLabel = new Label("Email: " + farmer.getEmail());
        Label phoneLabel = new Label("Phone: " + farmer.getPhoneNumber());
        Label locationLabel = new Label("Location: " + farmer.getFarmLocation());

        nameLabel.setFont(Font.font("Roboto", FontWeight.BOLD, 16));
        farmLabel.setFont(Font.font("Roboto", 14));
        emailLabel.setFont(Font.font("Roboto", 14));
        phoneLabel.setFont(Font.font("Roboto", 14));
        locationLabel.setFont(Font.font("Roboto", 14));

        details.getChildren().addAll(nameLabel, farmLabel, emailLabel, phoneLabel, locationLabel);

        content.getChildren().addAll(profilePicture, details);
        dialog.getDialogPane().setContent(content);

        ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(closeButton);

        dialog.showAndWait();
    }

    private void showEditProfileDialog(Farmer farmer) {
        Dialog<Farmer> dialog = new Dialog<>();
        dialog.setTitle("Edit Profile");
        dialog.setHeaderText("Update your profile information");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField(farmer.getFullName());
        TextField farmField = new TextField(farmer.getFarmName());
        TextField emailField = new TextField(farmer.getEmail());
        TextField phoneField = new TextField(farmer.getPhoneNumber());
        TextField locationField = new TextField(farmer.getFarmLocation());

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Farm:"), 0, 1);
        grid.add(farmField, 1, 1);
        grid.add(new Label("Email:"), 0, 2);
        grid.add(emailField, 1, 2);
        grid.add(new Label("Phone:"), 0, 3);
        grid.add(phoneField, 1, 3);
        grid.add(new Label("Location:"), 0, 4);
        grid.add(locationField, 1, 4);

        dialog.getDialogPane().setContent(grid);

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                farmer.setFullName(nameField.getText());
                farmer.setFarmName(farmField.getText());
                farmer.setEmail(emailField.getText());
                farmer.setPhoneNumber(phoneField.getText());
                farmer.setFarmLocation(locationField.getText());
                return farmer;
            }
            return null;
        });

        Optional<Farmer> result = dialog.showAndWait();
        result.ifPresent(updatedFarmer -> {
            showAlert("Success", "Profile updated successfully!");
        });
    }

    private void showSettingsDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Settings");
        dialog.setHeaderText("Account Settings");

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        // Add settings options
        CheckBox notificationsCheck = new CheckBox("Enable Notifications");
        CheckBox emailUpdatesCheck = new CheckBox("Receive Email Updates");
        CheckBox darkModeCheck = new CheckBox("Dark Mode");

        content.getChildren().addAll(notificationsCheck, emailUpdatesCheck, darkModeCheck);

        dialog.getDialogPane().setContent(content);

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        dialog.showAndWait();
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox(10);
        sidebar.setStyle(
                "-fx-background-color: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        sidebar.setPadding(new Insets(20));
        sidebar.setPrefWidth(200);

        Label menuLabel = new Label("Menu");
        menuLabel.setFont(Font.font("Roboto", FontWeight.BOLD, 16));
        VBox.setMargin(menuLabel, new Insets(0, 0, 20, 0));

        Button dashboardBtn = createMenuButton("Dashboard", true);
        Button productsBtn = createMenuButton("Products", false);
        Button ordersBtn = createMenuButton("Orders", false);
        Button messagesBtn = createMenuButton("Messages", false);
        Button settingsBtn = createMenuButton("Settings", false);

        // Add click handler for dashboard button
        dashboardBtn.setOnAction(e -> {
            // Update button styles
            dashboardBtn.setStyle("-fx-background-color: " + COLOR_PRIMARY_GREEN + "; -fx-text-fill: white;");
            productsBtn.setStyle(
                    "-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #e5e7eb; -fx-border-width: 1;");
            ordersBtn.setStyle(
                    "-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #e5e7eb; -fx-border-width: 1;");
            messagesBtn.setStyle(
                    "-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #e5e7eb; -fx-border-width: 1;");
            settingsBtn.setStyle(
                    "-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #e5e7eb; -fx-border-width: 1;");

            // Show dashboard page
            BorderPane dashboardRoot = (BorderPane) primaryStage.getScene().getRoot();
            ScrollPane scrollPane = new ScrollPane(createDashboardContent(demoFarmer));
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(true);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
            dashboardRoot.setCenter(scrollPane);
        });

        // Add click handler for products button
        productsBtn.setOnAction(e -> {
            // Update button styles
            dashboardBtn.setStyle(
                    "-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #e5e7eb; -fx-border-width: 1;");
            productsBtn.setStyle("-fx-background-color: " + COLOR_PRIMARY_GREEN + "; -fx-text-fill: white;");
            ordersBtn.setStyle(
                    "-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #e5e7eb; -fx-border-width: 1;");
            messagesBtn.setStyle(
                    "-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #e5e7eb; -fx-border-width: 1;");
            settingsBtn.setStyle(
                    "-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #e5e7eb; -fx-border-width: 1;");

            // Show products page
            BorderPane dashboardRoot = (BorderPane) primaryStage.getScene().getRoot();
            ScrollPane scrollPane = new ScrollPane(createMainContent(demoFarmer));
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(true);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
            dashboardRoot.setCenter(scrollPane);
        });

        // Add click handler for orders button
        ordersBtn.setOnAction(e -> {
            // Update button styles
            dashboardBtn.setStyle(
                    "-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #e5e7eb; -fx-border-width: 1;");
            productsBtn.setStyle(
                    "-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #e5e7eb; -fx-border-width: 1;");
            ordersBtn.setStyle("-fx-background-color: " + COLOR_PRIMARY_GREEN + "; -fx-text-fill: white;");
            messagesBtn.setStyle(
                    "-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #e5e7eb; -fx-border-width: 1;");
            settingsBtn.setStyle(
                    "-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #e5e7eb; -fx-border-width: 1;");

            // Show orders page
            BorderPane dashboardRoot = (BorderPane) primaryStage.getScene().getRoot();
            ScrollPane scrollPane = new ScrollPane(createOrdersPage(demoFarmer));
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(true);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
            dashboardRoot.setCenter(scrollPane);
        });

        // Add click handler for messages button
        messagesBtn.setOnAction(e -> {
            // Update button styles
            dashboardBtn.setStyle(
                    "-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #e5e7eb; -fx-border-width: 1;");
            productsBtn.setStyle(
                    "-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #e5e7eb; -fx-border-width: 1;");
            ordersBtn.setStyle(
                    "-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #e5e7eb; -fx-border-width: 1;");
            messagesBtn.setStyle("-fx-background-color: " + COLOR_PRIMARY_GREEN + "; -fx-text-fill: white;");
            settingsBtn.setStyle(
                    "-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #e5e7eb; -fx-border-width: 1;");

            // Show messages page
            BorderPane dashboardRoot = (BorderPane) primaryStage.getScene().getRoot();
            ScrollPane scrollPane = new ScrollPane(createMessagesPage());
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(true);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
            dashboardRoot.setCenter(scrollPane);
        });

        // Add click handler for settings button
        settingsBtn.setOnAction(e -> {
            // Update button styles
            dashboardBtn.setStyle(
                    "-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #e5e7eb; -fx-border-width: 1;");
            productsBtn.setStyle(
                    "-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #e5e7eb; -fx-border-width: 1;");
            ordersBtn.setStyle(
                    "-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #e5e7eb; -fx-border-width: 1;");
            messagesBtn.setStyle(
                    "-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #e5e7eb; -fx-border-width: 1;");
            settingsBtn.setStyle("-fx-background-color: " + COLOR_PRIMARY_GREEN + "; -fx-text-fill: white;");

            // Show settings page
            BorderPane dashboardRoot = (BorderPane) primaryStage.getScene().getRoot();
            ScrollPane scrollPane = new ScrollPane(createSettingsPage());
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(true);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
            dashboardRoot.setCenter(scrollPane);
        });

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button logoutBtn = new Button("Logout");
        logoutBtn.getStyleClass().add("button-danger");
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        logoutBtn.setOnAction(e -> {
            primaryStage.setScene(scene);
            primaryStage.setTitle("Farmers & Customers Interaction App");
        });

        sidebar.getChildren().addAll(
                menuLabel,
                dashboardBtn,
                productsBtn,
                ordersBtn,
                messagesBtn,
                settingsBtn,
                spacer,
                logoutBtn);

        return sidebar;
    }

    private Button createMenuButton(String text, boolean isSelected) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setFont(Font.font("Roboto", 14));
        if (isSelected) {
            button.setStyle("-fx-background-color: " + COLOR_PRIMARY_GREEN + "; -fx-text-fill: white;");
        } else {
            button.setStyle(
                    "-fx-background-color: white; -fx-text-fill: black; -fx-border-color: #e5e7eb; -fx-border-width: 1;");
        }
        return button;
    }

    private StackPane createMainContent(Farmer farmer) {
        StackPane mainContent = new StackPane();
        mainContent.setPadding(new Insets(20));

        // Products Management Section
        VBox productsSection = new VBox(20);
        productsSection.setStyle("-fx-background-color: white; -fx-background-radius: 10;");
        productsSection.setPadding(new Insets(20));

        // Header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Products Management");
        title.setFont(Font.font("Roboto", FontWeight.BOLD, 20));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addProductBtn = new Button("Add New Product");
        addProductBtn.getStyleClass().add("button-primary");
        addProductBtn.setFont(Font.font("Roboto", 14));

        header.getChildren().addAll(title, spacer, addProductBtn);

        // Products Table
        productsTable = new TableView<>();
        productsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        productsTable.setStyle("-fx-font-family: 'Roboto'; -fx-font-size: 14px;");

        TableColumn<Product, String> nameCol = new TableColumn<>("Product Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setAlignment(Pos.CENTER);
                    setFont(Font.font("Roboto", 14));
                }
            }
        });

        TableColumn<Product, Double> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        priceCol.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("$%.2f", item));
                    setAlignment(Pos.CENTER);
                    setFont(Font.font("Roboto", 14));
                }
            }
        });

        TableColumn<Product, String> unitCol = new TableColumn<>("Unit");
        unitCol.setCellValueFactory(new PropertyValueFactory<>("unit"));
        unitCol.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setAlignment(Pos.CENTER);
                    setFont(Font.font("Roboto", 14));
                }
            }
        });

        TableColumn<Product, Integer> quantityCol = new TableColumn<>("Quantity");
        quantityCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        quantityCol.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.toString());
                    setAlignment(Pos.CENTER);
                    setFont(Font.font("Roboto", 14));
                }
            }
        });

        TableColumn<Product, String> actionsCol = new TableColumn<>("Actions");
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox buttons = new HBox(10, editBtn, deleteBtn);

            {
                editBtn.getStyleClass().add("button-secondary");
                deleteBtn.getStyleClass().add("button-danger");
                buttons.setAlignment(Pos.CENTER);
                editBtn.setFont(Font.font("Roboto", 12));
                deleteBtn.setFont(Font.font("Roboto", 12));
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(buttons);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        // Set header font for all columns
        for (TableColumn<Product, ?> column : productsTable.getColumns()) {
            column.setStyle("-fx-font-family: 'Roboto'; -fx-font-size: 14px;");
        }

        productsTable.getColumns().addAll(nameCol, priceCol, unitCol, quantityCol, actionsCol);
        productsTable.setItems(productsList);

        // Add Product Dialog
        addProductBtn.setOnAction(e -> showAddProductDialog(farmer));

        productsSection.getChildren().addAll(header, productsTable);
        mainContent.getChildren().add(productsSection);

        return mainContent;
    }

    private void showAddProductDialog(Farmer farmer) {
        Dialog<Product> dialog = new Dialog<>();
        dialog.setTitle("Add New Product");
        dialog.setHeaderText("Enter product details");

        // Create the custom dialog content
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField();
        TextField priceField = new TextField();
        TextField unitField = new TextField();
        TextField quantityField = new TextField();
        TextArea descriptionArea = new TextArea();
        descriptionArea.setPrefRowCount(3);

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Price:"), 0, 1);
        grid.add(priceField, 1, 1);
        grid.add(new Label("Unit:"), 0, 2);
        grid.add(unitField, 1, 2);
        grid.add(new Label("Quantity:"), 0, 3);
        grid.add(quantityField, 1, 3);
        grid.add(new Label("Description:"), 0, 4);
        grid.add(descriptionArea, 1, 4);

        dialog.getDialogPane().setContent(grid);

        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                try {
                    if (nameField.getText().isEmpty() || unitField.getText().isEmpty()) {
                        throw new IllegalArgumentException("Name and unit cannot be empty");
                    }
                    return new Product(
                            nameField.getText(),
                            Double.parseDouble(priceField.getText()),
                            descriptionArea.getText(),
                            unitField.getText(),
                            Integer.parseInt(quantityField.getText()),
                            farmer.getId());
                } catch (NumberFormatException ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Invalid Input");
                    alert.setContentText("Please enter valid numbers for price and quantity.");
                    alert.showAndWait();
                    return null;
                } catch (IllegalArgumentException ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Invalid Input");
                    alert.setContentText(ex.getMessage());
                    alert.showAndWait();
                    return null;
                }
            }
            return null;
        });

        Optional<Product> result = dialog.showAndWait();
        result.ifPresent(product -> {
            productsList.add(product);
            farmer.addProduct(product);
        });
    }

    private void showCustomerDashboard(Customer customer) {
        this.currentCustomer = customer;
        BorderPane dashboard = new BorderPane();
        dashboard.setStyle("-fx-background-color: white;");

        // Top Bar
        HBox topBar = new HBox(20);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(15, 20, 15, 20));
        topBar.setStyle("-fx-background-color: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");

        // --- Add logo at the left ---
        ImageView logoView = null;
        try {
            Image logo = new Image(getClass().getResourceAsStream("/com/example/images/logo.png"));
            logoView = new ImageView(logo);
            logoView.setFitHeight(128);
            logoView.setPreserveRatio(true);
            logoView.setSmooth(true);
            logoView.setCache(true);
            HBox.setMargin(logoView, new Insets(0, 18, 0, 0));
        } catch (Exception e) {
            // If logo fails to load, do nothing
        }

        // Dashboard Button
        Button dashboardBtn = new Button("Dashboard");
        dashboardBtn.setStyle(
                "-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 15px; -fx-background-radius: 5px;");
        dashboardBtn.setOnAction(e -> {
            ScrollPane scrollPane = new ScrollPane(createCustomerDashboardContent(customer));
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(true);
            scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
            dashboard.setCenter(scrollPane);
        });

        Label welcomeLabel = new Label("Welcome, " + customer.getFullName());
        welcomeLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");

        HBox rightSection = new HBox(15);
        rightSection.setAlignment(Pos.CENTER_RIGHT);

        // Messages Button with Badge
        StackPane messagesContainer = new StackPane();
        Button messagesBtn = new Button("Messages");
        messagesBtn.setStyle(
                "-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 15px; -fx-background-radius: 5px;");

        Label messagesBadge = new Label("2");
        messagesBadge.setStyle(
                "-fx-background-color: #FF5252; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 2px 6px; -fx-background-radius: 10px;");
        messagesBadge.setVisible(true);

        StackPane.setAlignment(messagesBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(messagesBadge, new Insets(-5, -5, 0, 0));

        messagesContainer.getChildren().addAll(messagesBtn, messagesBadge);
        messagesBtn.setOnAction(e -> showCustomerMessages(customer));

        // Cart Button with Badge
        StackPane cartButtonContainer = new StackPane();
        Button cartButton = new Button("Cart");
        cartButton.setStyle(
                "-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 15px; -fx-background-radius: 5px;");

        cartBadge = new Label("0");
        cartBadge.setStyle(
                "-fx-background-color: #FF5252; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 2px 6px; -fx-background-radius: 10px;");
        cartBadge.setVisible(false);

        StackPane.setAlignment(cartBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(cartBadge, new Insets(-5, -5, 0, 0));

        cartButtonContainer.getChildren().addAll(cartButton, cartBadge);
        cartButton.setOnAction(e -> showCart());

        // Profile Button with Menu
        StackPane profileContainer = new StackPane();
        Button profileBtn = new Button("Profile");
        profileBtn.setStyle(
                "-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 15px; -fx-background-radius: 5px;");
        profileContainer.getChildren().add(profileBtn);

        // Create profile menu
        ContextMenu profileMenu = new ContextMenu();
        MenuItem viewProfile = new MenuItem("View Profile");
        MenuItem editProfile = new MenuItem("Edit Profile");
        MenuItem orderHistory = new MenuItem("Order History");
        MenuItem preferences = new MenuItem("Preferences");

        profileMenu.getItems().addAll(viewProfile, editProfile, orderHistory, preferences);

        profileBtn.setOnAction(e -> {
            profileMenu.show(profileBtn, Side.BOTTOM, 0, 0);
        });

        viewProfile.setOnAction(e -> showCustomerProfile(customer));
        editProfile.setOnAction(e -> showEditCustomerProfileDialog(customer));
        orderHistory.setOnAction(e -> showCustomerOrderHistory(customer));
        preferences.setOnAction(e -> showCustomerPreferences(customer));

        Button logoutBtn = new Button("Logout");
        logoutBtn.setStyle(
                "-fx-background-color: #FF5252; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 15px; -fx-background-radius: 5px;");
        logoutBtn.setOnAction(e -> {
            primaryStage.setScene(scene);
            primaryStage.setTitle("Farmers & Customers Interaction App");
        });

        rightSection.getChildren().addAll(messagesContainer, cartButtonContainer, profileContainer, logoutBtn);
        if (logoView != null) {
            topBar.getChildren().add(logoView);
        }
        topBar.getChildren().addAll(dashboardBtn, welcomeLabel, rightSection);
        HBox.setHgrow(rightSection, Priority.ALWAYS);

        // Main Content
        ScrollPane scrollPane = new ScrollPane(createCustomerDashboardContent(customer));
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        dashboard.setTop(topBar);
        dashboard.setCenter(scrollPane);

        primaryStage.setScene(new Scene(dashboard));
    }

    private void showCustomerMessages(Customer customer) {
        VBox messagesContent = new VBox(20);
        messagesContent.setPadding(new Insets(20));
        messagesContent.setStyle("-fx-background-color: white;");

        Label titleLabel = new Label("Messages");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");

        VBox messagesList = new VBox(10);
        messagesList.setStyle("-fx-background-color: #F5F5F5; -fx-padding: 15px; -fx-background-radius: 10px;");

        // Add sample messages
        messagesList.getChildren().addAll(
                createMessageItem("John's Organic Farm", "Your order #1001 has been delivered!", "2 hours ago", true),
                createMessageItem("Green Valley Farm", "New organic products available!", "5 hours ago", true),
                createMessageItem("Fresh Harvest Co.", "Thank you for your recent order!", "1 day ago", false),
                createMessageItem("Local Farmers Market", "Special weekend discounts available!", "2 days ago", false));

        Button newMessageBtn = new Button("New Message");
        newMessageBtn.setStyle(
                "-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px 20px; -fx-background-radius: 5px;");
        newMessageBtn.setOnAction(e -> showNewMessageDialog(customer));

        messagesContent.getChildren().addAll(titleLabel, messagesList, newMessageBtn);

        // Update the main content area
        BorderPane dashboardRoot = (BorderPane) primaryStage.getScene().getRoot();
        ScrollPane scrollPane = new ScrollPane(messagesContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        dashboardRoot.setCenter(scrollPane);
    }

    private void showNewMessageDialog(Customer customer) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("New Message");
        dialog.setHeaderText("Send a message to a farmer");

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        ComboBox<String> farmerComboBox = new ComboBox<>();
        farmerComboBox.getItems().addAll(
                "John's Organic Farm",
                "Green Valley Farm",
                "Fresh Harvest Co.",
                "Local Farmers Market");
        farmerComboBox.setPromptText("Select Farmer");
        farmerComboBox.setStyle("-fx-padding: 8px; -fx-background-radius: 5px;");

        TextArea messageArea = new TextArea();
        messageArea.setPromptText("Type your message here...");
        messageArea.setPrefRowCount(5);
        messageArea.setStyle("-fx-padding: 8px; -fx-background-radius: 5px;");

        content.getChildren().addAll(
                new Label("Select Farmer:"),
                farmerComboBox,
                new Label("Message:"),
                messageArea);

        dialog.getDialogPane().setContent(content);

        ButtonType sendButtonType = new ButtonType("Send", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(sendButtonType, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == sendButtonType) {
                String selectedFarmer = farmerComboBox.getValue();
                String message = messageArea.getText();

                if (selectedFarmer != null && !message.trim().isEmpty()) {
                    // In a real app, this would send the message to the selected farmer
                    showNotification("Message sent to " + selectedFarmer);
                    return null;
                } else {
                    showError("Error", "Please select a farmer and enter a message.");
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void showCustomerProfile(Customer customer) {
        VBox profileContent = new VBox(20);
        profileContent.setPadding(new Insets(20));
        profileContent.setStyle("-fx-background-color: white;");

        Label titleLabel = new Label("Profile Information");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");

        VBox infoBox = new VBox(15);
        infoBox.setStyle("-fx-background-color: #F5F5F5; -fx-padding: 20px; -fx-background-radius: 10px;");

        addProfileField(infoBox, "Full Name", customer.getFullName());
        addProfileField(infoBox, "Email", customer.getEmail());
        addProfileField(infoBox, "Phone", customer.getPhoneNumber());
        addProfileField(infoBox, "Location", customer.getLocation());
        addProfileField(infoBox, "Member Since", customer.getJoinDate());

        Button editButton = new Button("Edit Profile");
        editButton.setStyle(
                "-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px 20px; -fx-background-radius: 5px;");
        editButton.setOnAction(e -> showEditCustomerProfileDialog(customer));

        profileContent.getChildren().addAll(titleLabel, infoBox, editButton);

        // Update the main content area
        BorderPane dashboardRoot = (BorderPane) primaryStage.getScene().getRoot();
        ScrollPane scrollPane = new ScrollPane(profileContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        dashboardRoot.setCenter(scrollPane);
    }

    private void addProfileField(VBox container, String label, String value) {
        HBox field = new HBox(10);
        field.setAlignment(Pos.CENTER_LEFT);

        Label labelNode = new Label(label + ":");
        labelNode.setStyle("-fx-font-weight: bold; -fx-min-width: 120px;");

        Label valueNode = new Label(value);
        valueNode.setStyle("-fx-text-fill: #666;");

        field.getChildren().addAll(labelNode, valueNode);
        container.getChildren().add(field);
    }

    private void showEditCustomerProfileDialog(Customer customer) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Edit Profile");
        dialog.setHeaderText("Update Your Information");

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        TextField fullNameField = new TextField(customer.getFullName());
        fullNameField.setPromptText("Full Name");
        fullNameField.setStyle("-fx-padding: 8px; -fx-background-radius: 5px;");

        TextField emailField = new TextField(customer.getEmail());
        emailField.setPromptText("Email");
        emailField.setStyle("-fx-padding: 8px; -fx-background-radius: 5px;");

        TextField phoneField = new TextField(customer.getPhoneNumber());
        phoneField.setPromptText("Phone Number");
        phoneField.setStyle("-fx-padding: 8px; -fx-background-radius: 5px;");

        TextField locationField = new TextField(customer.getLocation());
        locationField.setPromptText("Location");
        locationField.setStyle("-fx-padding: 8px; -fx-background-radius: 5px;");

        content.getChildren().addAll(
                new Label("Full Name:"),
                fullNameField,
                new Label("Email:"),
                emailField,
                new Label("Phone Number:"),
                phoneField,
                new Label("Location:"),
                locationField);

        dialog.getDialogPane().setContent(content);

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                // Update customer information
                customer.setFullName(fullNameField.getText());
                customer.setEmail(emailField.getText());
                customer.setPhoneNumber(phoneField.getText());
                customer.setLocation(locationField.getText());

                // Refresh the profile view
                showCustomerProfile(customer);
                return null;
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void showCustomerOrderHistory(Customer customer) {
        VBox orderHistoryContent = new VBox(20);
        orderHistoryContent.setPadding(new Insets(20));
        orderHistoryContent.setStyle("-fx-background-color: white;");

        Label titleLabel = new Label("Order History");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");

        VBox ordersList = new VBox(10);
        ordersList.setStyle("-fx-background-color: #F5F5F5; -fx-padding: 15px; -fx-background-radius: 10px;");

        // Add sample orders (in a real app, these would come from a database)
        for (int i = 0; i < 5; i++) {
            HBox orderItem = new HBox(15);
            orderItem.setAlignment(Pos.CENTER_LEFT);
            orderItem.setStyle("-fx-background-color: white; -fx-padding: 15px; -fx-background-radius: 8px;");

            Label orderTitle = new Label("Order #" + (1000 + i));
            orderTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

            Label orderStatus = new Label(i == 0 ? "Delivered" : (i == 1 ? "In Transit" : "Processing"));
            orderStatus.setStyle("-fx-text-fill: " + (i == 0 ? "#2E7D32" : (i == 1 ? "#1976D2" : "#F57C00")) + ";");

            Label orderDate = new Label("2024-03-" + (10 + i));
            orderDate.setStyle("-fx-text-fill: #666;");

            Button viewDetailsBtn = new Button("View Details");
            viewDetailsBtn.setStyle(
                    "-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 5px 10px; -fx-background-radius: 5px;");

            orderItem.getChildren().addAll(orderTitle, orderStatus, orderDate, viewDetailsBtn);
            ordersList.getChildren().add(orderItem);
        }

        orderHistoryContent.getChildren().addAll(titleLabel, ordersList);

        // Update the main content area
        BorderPane dashboardRoot = (BorderPane) primaryStage.getScene().getRoot();
        ScrollPane scrollPane = new ScrollPane(orderHistoryContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        dashboardRoot.setCenter(scrollPane);
    }

    private void showCustomerPreferences(Customer customer) {
        VBox preferencesContent = new VBox(20);
        preferencesContent.setPadding(new Insets(20));
        preferencesContent.setStyle("-fx-background-color: white;");

        Label titleLabel = new Label("Preferences");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");

        VBox preferencesBox = new VBox(15);
        preferencesBox.setStyle("-fx-background-color: #F5F5F5; -fx-padding: 20px; -fx-background-radius: 10px;");

        // Notification Preferences
        Label notificationTitle = new Label("Notification Preferences");
        notificationTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        ToggleSwitch emailNotifications = new ToggleSwitch("Email Notifications");
        ToggleSwitch smsNotifications = new ToggleSwitch("SMS Notifications");
        ToggleSwitch orderUpdates = new ToggleSwitch("Order Updates");
        ToggleSwitch promotions = new ToggleSwitch("Promotions and Offers");

        // Privacy Settings
        Label privacyTitle = new Label("Privacy Settings");
        privacyTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        ToggleSwitch shareLocation = new ToggleSwitch("Share Location with Farmers");
        ToggleSwitch showProfile = new ToggleSwitch("Show Profile to Farmers");

        preferencesBox.getChildren().addAll(
                notificationTitle,
                emailNotifications,
                smsNotifications,
                orderUpdates,
                promotions,
                new Separator(),
                privacyTitle,
                shareLocation,
                showProfile);

        Button saveButton = new Button("Save Preferences");
        saveButton.setStyle(
                "-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px 20px; -fx-background-radius: 5px;");
        saveButton.setOnAction(e -> {
            // Save preferences logic here
            showNotification("Preferences saved successfully!");
        });

        preferencesContent.getChildren().addAll(titleLabel, preferencesBox, saveButton);

        // Update the main content area
        BorderPane dashboardRoot = (BorderPane) primaryStage.getScene().getRoot();
        ScrollPane scrollPane = new ScrollPane(preferencesContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        dashboardRoot.setCenter(scrollPane);
    }

    private VBox createCustomerDashboardContent(Customer customer) {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: white;");

        // Welcome Section
        VBox welcomeSection = new VBox(10);
        welcomeSection.setStyle("-fx-background-color: #F5F5F5; -fx-padding: 20px; -fx-background-radius: 10px;");

        Label welcomeTitle = new Label("Welcome back, " + customer.getFullName() + "!");
        welcomeTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");

        Label welcomeSubtitle = new Label("Here's what's happening with your orders and favorite products");
        welcomeSubtitle.setStyle("-fx-text-fill: #666; -fx-font-size: 16px;");

        welcomeSection.getChildren().addAll(welcomeTitle, welcomeSubtitle);

        // Quick Stats Section
        HBox statsSection = new HBox(20);
        statsSection.setAlignment(Pos.CENTER_LEFT);

        // Active Orders Card
        VBox activeOrdersCard = createStatCard(
                "Active Orders",
                "2",
                "Orders in progress",
                "📦",
                () -> showCustomerOrderHistory(customer));

        // Favorite Products Card
        VBox favoriteProductsCard = createStatCard(
                "Favorite Products",
                "5",
                "Saved items",
                "❤️",
                () -> showFavoriteProducts(customer));

        // Recent Activity Card
        VBox recentActivityCard = createStatCard(
                "Recent Activity",
                "3",
                "New updates",
                "🔄",
                () -> showRecentActivity(customer));

        statsSection.getChildren().addAll(activeOrdersCard, favoriteProductsCard, recentActivityCard);

        // Featured Products Section
        Label featuredTitle = new Label("Featured Products");
        featuredTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");

        FlowPane featuredProducts = new FlowPane();
        featuredProducts.setHgap(20);
        featuredProducts.setVgap(20);
        featuredProducts.setPrefWrapLength(800);

        // Add some sample featured products
        for (int i = 0; i < 4; i++) {
            VBox productCard = createProductCard(
                    "Organic " + (i % 2 == 0 ? "Tomatoes" : "Potatoes"),
                    "Fresh from local farms",
                    (i + 1) * 5.99,
                    "kg");
            featuredProducts.getChildren().add(productCard);
        }

        // Recent Orders Section
        Label ordersTitle = new Label("Recent Orders");
        ordersTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");

        VBox ordersList = new VBox(10);
        ordersList.setStyle("-fx-background-color: #F5F5F5; -fx-padding: 15px; -fx-background-radius: 10px;");

        // Add some sample orders
        for (int i = 0; i < 3; i++) {
            HBox orderItem = new HBox(15);
            orderItem.setAlignment(Pos.CENTER_LEFT);
            orderItem.setStyle("-fx-background-color: white; -fx-padding: 15px; -fx-background-radius: 8px;");

            Label orderTitle = new Label("Order #" + (1000 + i));
            orderTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

            Label orderStatus = new Label(i == 0 ? "Delivered" : (i == 1 ? "In Transit" : "Processing"));
            orderStatus.setStyle("-fx-text-fill: " + (i == 0 ? "#2E7D32" : (i == 1 ? "#1976D2" : "#F57C00")) + ";");

            Label orderDate = new Label("2024-03-" + (10 + i));
            orderDate.setStyle("-fx-text-fill: #666;");

            Button viewDetailsBtn = new Button("View Details");
            viewDetailsBtn.setStyle(
                    "-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 5px 10px; -fx-background-radius: 5px;");

            orderItem.getChildren().addAll(orderTitle, orderStatus, orderDate, viewDetailsBtn);
            ordersList.getChildren().add(orderItem);
        }

        // Add all sections to the content
        content.getChildren().addAll(
                welcomeSection,
                statsSection,
                featuredTitle,
                featuredProducts,
                ordersTitle,
                ordersList);

        return content;
    }

    private void showFavoriteProducts(Customer customer) {
        VBox favoritesContent = new VBox(20);
        favoritesContent.setPadding(new Insets(20));
        favoritesContent.setStyle("-fx-background-color: white;");

        Label titleLabel = new Label("Favorite Products");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");

        FlowPane productsGrid = new FlowPane();
        productsGrid.setHgap(20);
        productsGrid.setVgap(20);
        productsGrid.setPrefWrapLength(800);

        // Add sample favorite products
        for (int i = 0; i < 6; i++) {
            VBox productCard = createProductCard(
                    "Organic " + (i % 2 == 0 ? "Tomatoes" : "Potatoes"),
                    "Fresh from local farms",
                    (i + 1) * 5.99,
                    "kg");
            productsGrid.getChildren().add(productCard);
        }

        favoritesContent.getChildren().addAll(titleLabel, productsGrid);

        // Update the main content area
        BorderPane dashboardRoot = (BorderPane) primaryStage.getScene().getRoot();
        ScrollPane scrollPane = new ScrollPane(favoritesContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        dashboardRoot.setCenter(scrollPane);
    }

    private void showRecentActivity(Customer customer) {
        VBox activityContent = new VBox(20);
        activityContent.setPadding(new Insets(20));
        activityContent.setStyle("-fx-background-color: white;");

        Label titleLabel = new Label("Recent Activity");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");

        VBox activityList = new VBox(10);
        activityList.setStyle("-fx-background-color: #F5F5F5; -fx-padding: 15px; -fx-background-radius: 10px;");

        // Add sample activities
        activityList.getChildren().addAll(
                createActivityItem("Order #1001 has been delivered", "2 hours ago", true),
                createActivityItem("New product available: Organic Apples", "5 hours ago", true),
                createActivityItem("Order #1000 has been shipped", "1 day ago", false),
                createActivityItem("Price update: Organic Tomatoes", "2 days ago", false));

        activityContent.getChildren().addAll(titleLabel, activityList);

        // Update the main content area
        BorderPane dashboardRoot = (BorderPane) primaryStage.getScene().getRoot();
        ScrollPane scrollPane = new ScrollPane(activityContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        dashboardRoot.setCenter(scrollPane);
    }

    private void showCart() {
        BorderPane dashboardRoot = (BorderPane) primaryStage.getScene().getRoot();
        ScrollPane scrollPane = new ScrollPane(createCartContent());
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        dashboardRoot.setCenter(scrollPane);
    }

    private VBox createCartContent() {
        VBox cartSection = new VBox(20);
        cartSection.setStyle("-fx-background-color: white; -fx-padding: 20;");

        Label cartTitle = new Label("Shopping Cart");
        cartTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");

        VBox cartItems = new VBox(15);
        cartItems.setStyle("-fx-background-color: #F5F5F5; -fx-padding: 15px; -fx-background-radius: 10px;");

        // Add cart items
        for (CartItem item : this.cartItems) {
            cartItems.getChildren().add(createCartItem(item));
        }

        // Cart Summary
        VBox summary = new VBox(10);
        summary.setStyle(
                "-fx-background-color: white; -fx-padding: 20px; -fx-background-radius: 10px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");

        double subtotal = this.cartItems.stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();
        double shipping = 5.99;
        double total = subtotal + shipping;

        Label subtotalLabel = new Label(String.format("Subtotal: $%.2f", subtotal));
        Label shippingLabel = new Label(String.format("Shipping: $%.2f", shipping));
        Label totalLabel = new Label(String.format("Total: $%.2f", total));
        totalLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 18px;");

        Button checkoutBtn = new Button("Proceed to Checkout");
        checkoutBtn.setStyle(
                "-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px 20px; -fx-background-radius: 5px;");
        checkoutBtn.setOnAction(e -> showCheckoutDialog());

        summary.getChildren().addAll(subtotalLabel, shippingLabel, totalLabel, checkoutBtn);

        cartSection.getChildren().addAll(cartTitle, cartItems, summary);
        return cartSection;
    }

    private VBox createCartItem(CartItem item) {
        VBox itemBox = new VBox(10);
        itemBox.setStyle("-fx-background-color: white; -fx-padding: 15px; -fx-background-radius: 8px;");

        HBox itemHeader = new HBox(10);
        itemHeader.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(item.getName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        Label priceLabel = new Label(String.format("$%.2f/%s", item.getPrice(), item.getUnit()));
        priceLabel.setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold;");

        HBox quantityBox = new HBox(10);
        quantityBox.setAlignment(Pos.CENTER);

        Button minusBtn = new Button("-");
        minusBtn.setStyle(
                "-fx-background-color: #E0E0E0; -fx-text-fill: black; -fx-font-weight: bold; -fx-min-width: 30px; -fx-min-height: 30px; -fx-background-radius: 15px;");

        Label quantityLabel = new Label(String.valueOf(item.getQuantity()));
        quantityLabel.setStyle("-fx-font-weight: bold; -fx-min-width: 30px; -fx-alignment: center;");

        Button plusBtn = new Button("+");
        plusBtn.setStyle(
                "-fx-background-color: #E0E0E0; -fx-text-fill: black; -fx-font-weight: bold; -fx-min-width: 30px; -fx-min-height: 30px; -fx-background-radius: 15px;");

        Button removeBtn = new Button("Remove");
        removeBtn.setStyle(
                "-fx-background-color: #FF5252; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 5px 10px; -fx-background-radius: 5px;");

        quantityBox.getChildren().addAll(minusBtn, quantityLabel, plusBtn);

        minusBtn.setOnAction(e -> {
            if (item.getQuantity() > 1) {
                item.setQuantity(item.getQuantity() - 1);
                quantityLabel.setText(String.valueOf(item.getQuantity()));
                updateCartBadge();
            }
        });

        plusBtn.setOnAction(e -> {
            item.setQuantity(item.getQuantity() + 1);
            quantityLabel.setText(String.valueOf(item.getQuantity()));
            updateCartBadge();
        });

        removeBtn.setOnAction(e -> {
            cartItems.remove(item);
            itemBox.setVisible(false);
            itemBox.setManaged(false);
            updateCartBadge();
        });

        itemHeader.getChildren().addAll(nameLabel, priceLabel);
        itemBox.getChildren().addAll(itemHeader, quantityBox, removeBtn);

        return itemBox;
    }

    private void showCheckoutDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Checkout");
        dialog.setHeaderText("Enter Delivery Details");

        // Create the custom dialog content
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        TextField addressField = new TextField();
        addressField.setPromptText("Delivery Address");
        addressField.setStyle("-fx-padding: 8px; -fx-background-radius: 5px;");

        TextField phoneField = new TextField();
        phoneField.setPromptText("Phone Number");
        phoneField.setStyle("-fx-padding: 8px; -fx-background-radius: 5px;");

        TextArea notesField = new TextArea();
        notesField.setPromptText("Delivery Notes (Optional)");
        notesField.setPrefRowCount(3);
        notesField.setStyle("-fx-padding: 8px; -fx-background-radius: 5px;");

        content.getChildren().addAll(
                new Label("Delivery Address:"),
                addressField,
                new Label("Phone Number:"),
                phoneField,
                new Label("Delivery Notes:"),
                notesField);

        dialog.getDialogPane().setContent(content);

        // Add buttons
        ButtonType confirmButtonType = new ButtonType("Confirm Order", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

        // Style the dialog
        dialog.getDialogPane().setStyle("-fx-background-color: white;");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/com/example/styles.css").toExternalForm());

        // Handle the confirm button
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmButtonType) {
                // Process the order
                showOrderConfirmation();
                return null;
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void showOrderConfirmation() {
        // Clear the cart
        cartItems.clear();
        updateCartBadge();

        // Show confirmation message
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Order Confirmed");
        alert.setHeaderText("Thank you for your order!");
        alert.setContentText("Your order has been placed successfully. You will receive a confirmation email shortly.");
        alert.showAndWait();

        // Return to the main dashboard
        showCustomerDashboard(currentCustomer);
    }

    private void showFarmerDetails(Product product, VBox farmerDetails) {
        System.out.println("Showing farmer details for product: " + product.getName());
        System.out.println("Product farmer ID: " + product.getFarmerId());

        // Find the farmer who owns this product
        Farmer farmer = findFarmerById(product.getFarmerId());
        System.out.println("Found farmer: " + (farmer != null ? farmer.getFullName() : "null"));

        if (farmer != null) {
            farmerDetails.getChildren().clear();
            farmerDetails.setSpacing(10);
            farmerDetails.setPadding(new Insets(10));
            farmerDetails.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 5;");

            Label farmerName = new Label("Farmer: " + farmer.getFullName());
            farmerName.setFont(Font.font("System", FontWeight.BOLD, 14));

            Label farmName = new Label("Farm: " + farmer.getFarmName());
            farmName.setFont(Font.font("System", 12));

            Label contact = new Label("Contact: " + farmer.getPhoneNumber());
            contact.setFont(Font.font("System", 12));

            Label email = new Label("Email: " + farmer.getEmail());
            email.setFont(Font.font("System", 12));

            Label location = new Label("Location: " + farmer.getFarmLocation());
            location.setFont(Font.font("System", 12));

            Button contactButton = new Button("Contact Farmer");
            contactButton.getStyleClass().add("button-primary");
            contactButton.setMaxWidth(Double.MAX_VALUE);
            contactButton.setOnAction(e -> {
                showAlert("Contact Information",
                        "Farmer: " + farmer.getFullName() + "\n" +
                                "Farm: " + farmer.getFarmName() + "\n" +
                                "Phone: " + farmer.getPhoneNumber() + "\n" +
                                "Email: " + farmer.getEmail() + "\n" +
                                "Location: " + farmer.getFarmLocation());
            });

            farmerDetails.getChildren().addAll(farmerName, farmName, contact, email, location, contactButton);
            farmerDetails.setVisible(true);
            farmerDetails.setManaged(true);

            // Force layout update
            farmerDetails.requestLayout();
            System.out.println("Farmer details added to UI");
        } else {
            System.out.println("No farmer found for product");
            showError("Error", "Could not find farmer details");
        }
    }

    private Farmer findFarmerById(String farmerId) {
        // Return the demo farmer for the demo product
        return demoFarmer;
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private VBox createOrdersPage(Farmer farmer) {
        VBox ordersSection = new VBox(20);
        ordersSection.setStyle("-fx-background-color: white; -fx-background-radius: 10;");
        ordersSection.setPadding(new Insets(20));

        // Header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Orders Management");
        title.setFont(Font.font("Roboto", FontWeight.BOLD, 20));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(title, spacer);

        // Create Orders Table
        TableView<Order> ordersTable = new TableView<>();
        ordersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        ordersTable.setStyle("-fx-font-family: 'Roboto'; -fx-font-size: 14px;");

        // Customer Name Column
        TableColumn<Order, String> customerNameCol = new TableColumn<>("Customer Name");
        customerNameCol.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        customerNameCol.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setAlignment(Pos.CENTER);
                    setFont(Font.font("Roboto", 14));
                }
            }
        });

        // Location Column
        TableColumn<Order, String> locationCol = new TableColumn<>("Location");
        locationCol.setCellValueFactory(new PropertyValueFactory<>("location"));
        locationCol.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setAlignment(Pos.CENTER);
                    setFont(Font.font("Roboto", 14));
                }
            }
        });

        // Product Name Column
        TableColumn<Order, String> productNameCol = new TableColumn<>("Product Name");
        productNameCol.setCellValueFactory(new PropertyValueFactory<>("productName"));
        productNameCol.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setAlignment(Pos.CENTER);
                    setFont(Font.font("Roboto", 14));
                }
            }
        });

        // Quantity Column
        TableColumn<Order, Integer> quantityCol = new TableColumn<>("Quantity");
        quantityCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        quantityCol.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.toString());
                    setAlignment(Pos.CENTER);
                    setFont(Font.font("Roboto", 14));
                }
            }
        });

        // Status Column
        TableColumn<Order, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setAlignment(Pos.CENTER);
                    setFont(Font.font("Roboto", 14));
                }
            }
        });

        // Actions Column
        TableColumn<Order, String> actionsCol = new TableColumn<>("Actions");
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button acceptBtn = new Button("Accept");
            private final Button rejectBtn = new Button("Reject");
            private final HBox buttons = new HBox(10, acceptBtn, rejectBtn);

            {
                acceptBtn.getStyleClass().add("button-primary");
                rejectBtn.getStyleClass().add("button-danger");
                buttons.setAlignment(Pos.CENTER);
                acceptBtn.setFont(Font.font("Roboto", 12));
                rejectBtn.setFont(Font.font("Roboto", 12));
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(buttons);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        // Set header font for all columns
        for (TableColumn<Order, ?> column : ordersTable.getColumns()) {
            column.setStyle("-fx-font-family: 'Roboto'; -fx-font-size: 14px;");
        }

        ordersTable.getColumns().addAll(customerNameCol, locationCol, productNameCol, quantityCol, statusCol,
                actionsCol);

        // Add sample orders (in a real app, this would come from a database)
        ObservableList<Order> orders = FXCollections.observableArrayList(
                new Order("John Doe", "123 Main St, City", "Organic Tomatoes", 5, "Pending"),
                new Order("Jane Smith", "456 Oak Ave, Town", "Fresh Lettuce", 3, "Pending"),
                new Order("Mike Johnson", "789 Pine Rd, Village", "Organic Carrots", 2, "Pending"));
        ordersTable.setItems(orders);

        ordersSection.getChildren().addAll(header, ordersTable);
        return ordersSection;
    }

    // Order class to represent order data
    public static class Order {
        private final String customerName;
        private final String location;
        private final String productName;
        private final int quantity;
        private final String status;

        public Order(String customerName, String location, String productName, int quantity, String status) {
            this.customerName = customerName;
            this.location = location;
            this.productName = productName;
            this.quantity = quantity;
            this.status = status;
        }

        public String getCustomerName() {
            return customerName;
        }

        public String getLocation() {
            return location;
        }

        public String getProductName() {
            return productName;
        }

        public int getQuantity() {
            return quantity;
        }

        public String getStatus() {
            return status;
        }
    }

    private VBox createDashboardContent(Farmer farmer) {
        VBox dashboardSection = new VBox(20);
        dashboardSection.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 10;");
        dashboardSection.setPadding(new Insets(20));

        // Header with refresh button
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Dashboard Overview");
        title.setFont(Font.font("Roboto", FontWeight.BOLD, 28));

        Button refreshBtn = new Button("↻");
        refreshBtn.setStyle("-fx-background-color: " + COLOR_PRIMARY_GREEN
                + "; -fx-text-fill: white; -fx-font-size: 16px; -fx-min-width: 40px; -fx-min-height: 40px; -fx-background-radius: 20;");
        refreshBtn.setOnAction(e -> refreshDashboard());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(title, refreshBtn, spacer);

        // Statistics Cards Grid
        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(20);
        statsGrid.setVgap(20);
        statsGrid.setPadding(new Insets(20));

        // Total Products Card
        VBox totalProductsCard = createStatCard(
                "Total Products",
                String.valueOf(productsList.size()),
                "Products listed in your store",
                "📦",
                () -> showProductDetails());

        // Pending Orders Card
        VBox pendingOrdersCard = createStatCard(
                "Pending Orders",
                "3",
                "Orders awaiting your response",
                "⏳",
                () -> showPendingOrders());

        // Fulfilled Orders Card
        VBox fulfilledOrdersCard = createStatCard(
                "Fulfilled Orders",
                "12",
                "Successfully completed orders",
                "✅",
                () -> showFulfilledOrders());

        // Total Revenue Card
        VBox totalRevenueCard = createStatCard(
                "Total Revenue",
                "$1,234.56",
                "Total earnings from all orders",
                "💰",
                () -> showRevenueDetails());

        // New Revenue Card
        VBox newRevenueCard = createStatCard(
                "New Revenue",
                "$234.56",
                "Earnings from last 7 days",
                "📈",
                () -> showNewRevenueDetails());

        // New Messages Card
        VBox newMessagesCard = createStatCard(
                "New Messages",
                "5",
                "Unread customer inquiries",
                "📩",
                () -> showMessages());

        // Add cards to grid
        statsGrid.add(totalProductsCard, 0, 0);
        statsGrid.add(pendingOrdersCard, 1, 0);
        statsGrid.add(fulfilledOrdersCard, 2, 0);
        statsGrid.add(totalRevenueCard, 0, 1);
        statsGrid.add(newRevenueCard, 1, 1);
        statsGrid.add(newMessagesCard, 2, 1);

        // Recent Activity Section with refresh
        VBox recentActivitySection = new VBox(10);
        recentActivitySection.setPadding(new Insets(20));
        recentActivitySection.setStyle(
                "-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");

        HBox activityHeader = new HBox(10);
        Label recentActivityTitle = new Label("Recent Activity");
        recentActivityTitle.setFont(Font.font("Roboto", FontWeight.BOLD, 24));

        Button refreshActivityBtn = new Button("↻");
        refreshActivityBtn.setStyle("-fx-background-color: " + COLOR_PRIMARY_GREEN
                + "; -fx-text-fill: white; -fx-font-size: 14px; -fx-min-width: 30px; -fx-min-height: 30px; -fx-background-radius: 15;");
        refreshActivityBtn.setOnAction(e -> refreshRecentActivity());

        activityHeader.getChildren().addAll(recentActivityTitle, refreshActivityBtn);

        // Sample recent activities
        VBox activitiesList = new VBox(10);
        activitiesList.getChildren().addAll(
                createActivityItem("New order received from John Doe", "2 minutes ago", true),
                createActivityItem("Product 'Organic Tomatoes' stock updated", "1 hour ago", false),
                createActivityItem("New message from Jane Smith", "3 hours ago", true));

        recentActivitySection.getChildren().addAll(activityHeader, activitiesList);

        dashboardSection.getChildren().addAll(header, statsGrid, recentActivitySection);
        return dashboardSection;
    }

    private VBox createStatCard(String title, String value, String description, String icon, Runnable onClick) {
        VBox card = new VBox(10);
        card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        card.setPadding(new Insets(20));
        card.setPrefWidth(300);
        card.setPrefHeight(200);

        // Add hover effect
        card.setOnMouseEntered(e -> {
            card.setStyle(
                    "-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 15, 0, 0, 3); -fx-cursor: hand;");
            card.setTranslateY(-5);
        });
        card.setOnMouseExited(e -> {
            card.setStyle(
                    "-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
            card.setTranslateY(0);
        });

        // Add click handler
        card.setOnMouseClicked(e -> onClick.run());

        Label iconLabel = new Label(icon);
        iconLabel.setFont(Font.font("System", 36));
        iconLabel.setStyle("-fx-padding: 0 0 10 0;");

        Label valueLabel = new Label(value);
        valueLabel.setFont(Font.font("Roboto", FontWeight.BOLD, 20));

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Roboto", FontWeight.BOLD, 20));

        Label descriptionLabel = new Label(description);
        descriptionLabel.setFont(Font.font("Roboto", 14));
        descriptionLabel.setTextFill(Color.GRAY);

        // Add trend indicator if applicable
        if (title.contains("Revenue") || title.contains("Orders")) {
            HBox trendBox = new HBox(5);
            Label trendIcon = new Label("↑");
            trendIcon.setTextFill(Color.GREEN);
            trendIcon.setFont(Font.font("System", 16));
            Label trendText = new Label("12% from last week");
            trendText.setFont(Font.font("Roboto", 14));
            trendText.setTextFill(Color.GRAY);
            trendBox.getChildren().addAll(trendIcon, trendText);
            card.getChildren().addAll(iconLabel, valueLabel, titleLabel, descriptionLabel, trendBox);
        } else {
            card.getChildren().addAll(iconLabel, valueLabel, titleLabel, descriptionLabel);
        }

        return card;
    }

    private HBox createActivityItem(String activity, String time, boolean isNew) {
        HBox item = new HBox(10);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(10));
        item.setStyle("-fx-background-color: " + (isNew ? "#f0fdf4" : "white") + "; -fx-background-radius: 5;");

        // Add hover effect
        item.setOnMouseEntered(e -> {
            item.setStyle("-fx-background-color: " + (isNew ? "#dcfce7" : "#f8f9fa")
                    + "; -fx-background-radius: 5; -fx-cursor: hand;");
        });
        item.setOnMouseExited(e -> {
            item.setStyle("-fx-background-color: " + (isNew ? "#f0fdf4" : "white") + "; -fx-background-radius: 5;");
        });

        Label activityLabel = new Label(activity);
        activityLabel.setFont(Font.font("Roboto", 14));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label timeLabel = new Label(time);
        timeLabel.setFont(Font.font("Roboto", 12));
        timeLabel.setTextFill(Color.GRAY);

        item.getChildren().addAll(activityLabel, spacer, timeLabel);

        return item;
    }

    // Action handlers for card clicks
    private void showProductDetails() {
        // Switch to products page
        BorderPane dashboardRoot = (BorderPane) primaryStage.getScene().getRoot();
        ScrollPane scrollPane = new ScrollPane(createMainContent(demoFarmer));
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        dashboardRoot.setCenter(scrollPane);
    }

    private void showPendingOrders() {
        // Show pending orders dialog
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Pending Orders");
        alert.setHeaderText("Orders Awaiting Response");
        alert.setContentText("You have 3 pending orders that need your attention.");
        alert.showAndWait();
    }

    private void showFulfilledOrders() {
        // Show fulfilled orders dialog
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Fulfilled Orders");
        alert.setHeaderText("Completed Orders");
        alert.setContentText("You have successfully completed 12 orders.");
        alert.showAndWait();
    }

    private void showRevenueDetails() {
        // Show revenue details dialog
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Revenue Details");
        alert.setHeaderText("Total Revenue Breakdown");
        alert.setContentText(
                "Total Revenue: $1,234.56\nBreakdown by product category:\n- Vegetables: $500.00\n- Fruits: $400.00\n- Other: $334.56");
        alert.showAndWait();
    }

    private void showNewRevenueDetails() {
        // Show new revenue details dialog
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("New Revenue");
        alert.setHeaderText("Last 7 Days Revenue");
        alert.setContentText(
                "New Revenue: $234.56\nDaily breakdown:\n- Today: $50.00\n- Yesterday: $45.00\n- Previous days: $139.56");
        alert.showAndWait();
    }

    private void showMessages() {
        // Show messages dialog
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("New Messages");
        alert.setHeaderText("Unread Messages");
        alert.setContentText("You have 5 unread messages from customers.");
        alert.showAndWait();
    }

    private void refreshDashboard() {
        // Refresh all dashboard data
        BorderPane dashboardRoot = (BorderPane) primaryStage.getScene().getRoot();
        ScrollPane scrollPane = new ScrollPane(createDashboardContent(demoFarmer));
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        dashboardRoot.setCenter(scrollPane);
    }

    private void refreshRecentActivity() {
        // Refresh recent activity section
        // In a real app, this would fetch new data from the server
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Refresh");
        alert.setHeaderText("Recent Activity Updated");
        alert.setContentText("Recent activity has been refreshed.");
        alert.showAndWait();
    }

    private VBox createMessagesPage() {
        VBox messagesSection = new VBox(20);
        messagesSection.setStyle("-fx-background-color: white; -fx-background-radius: 10;");
        messagesSection.setPadding(new Insets(20));

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Customer Messages");
        title.setFont(Font.font("Roboto", FontWeight.BOLD, 29)); // Increased from 24
        title.setTextFill(Color.BLACK);

        Button refreshBtn = new Button("↻");
        refreshBtn.setStyle("-fx-background-color: " + COLOR_PRIMARY_GREEN
                + "; -fx-text-fill: white; -fx-font-size: 16px; -fx-min-width: 40px; -fx-min-height: 40px; -fx-background-radius: 20;");
        refreshBtn.setOnAction(e -> refreshMessages());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(title, refreshBtn, spacer);

        // Messages List
        VBox messagesList = new VBox(15);
        messagesList.setPadding(new Insets(10));

        // Sample messages (in a real app, these would come from a database)
        messagesList.getChildren().addAll(
                createMessageItem("John Doe", "I'm interested in your organic tomatoes. Do you have any available?",
                        "2 hours ago", true),
                createMessageItem("Jane Smith", "What's the minimum order quantity for your products?", "5 hours ago",
                        true),
                createMessageItem("Mike Johnson", "Can you deliver to Accra Central?", "1 day ago", false),
                createMessageItem("Sarah Wilson", "Do you offer bulk discounts?", "2 days ago", false));

        messagesSection.getChildren().addAll(header, messagesList);
        return messagesSection;
    }

    private VBox createMessageItem(String sender, String message, String time, boolean isUnread) {
        VBox messageItem = new VBox(10);
        messageItem.setStyle("-fx-background-color: " + (isUnread ? "#f0fdf4" : "white")
                + "; -fx-background-radius: 10; -fx-padding: 15;");

        // Add hover effect
        messageItem.setOnMouseEntered(e -> {
            messageItem.setStyle("-fx-background-color: " + (isUnread ? "#dcfce7" : "#f8f9fa")
                    + "; -fx-background-radius: 10; -fx-cursor: hand;");
        });
        messageItem.setOnMouseExited(e -> {
            messageItem.setStyle(
                    "-fx-background-color: " + (isUnread ? "#f0fdf4" : "white") + "; -fx-background-radius: 10;");
        });

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label senderLabel = new Label(sender);
        senderLabel.setFont(Font.font("Roboto", FontWeight.BOLD, 19)); // Increased from 14
        senderLabel.setTextFill(Color.BLACK);

        if (isUnread) {
            Label unreadLabel = new Label("•");
            unreadLabel.setTextFill(Color.web(COLOR_PRIMARY_GREEN));
            unreadLabel.setFont(Font.font("Roboto", FontWeight.BOLD, 25)); // Increased from 20
            header.getChildren().add(unreadLabel);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label timeLabel = new Label(time);
        timeLabel.setFont(Font.font("Roboto", 17)); // Increased from 12
        timeLabel.setTextFill(Color.BLACK);

        header.getChildren().addAll(senderLabel, spacer, timeLabel);

        Label messageLabel = new Label(message);
        messageLabel.setFont(Font.font("Roboto", 19)); // Increased from 14
        messageLabel.setTextFill(Color.BLACK);
        messageLabel.setWrapText(true);

        Button replyBtn = new Button("Reply");
        replyBtn.getStyleClass().add("button-primary");
        replyBtn.setMaxWidth(100);
        replyBtn.setOnAction(e -> showReplyDialog(sender));

        messageItem.getChildren().addAll(header, messageLabel, replyBtn);
        return messageItem;
    }

    private void showReplyDialog(String recipient) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Reply to " + recipient);
        dialog.setHeaderText("Write your reply");

        TextArea replyArea = new TextArea();
        replyArea.setPromptText("Type your message here...");
        replyArea.setPrefRowCount(5);
        replyArea.setWrapText(true);

        dialog.getDialogPane().setContent(replyArea);

        ButtonType sendButtonType = new ButtonType("Send", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(sendButtonType, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == sendButtonType) {
                return replyArea.getText();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(reply -> {
            showAlert("Message Sent", "Your reply has been sent to " + recipient);
        });
    }

    private void refreshMessages() {
        // In a real app, this would fetch new messages from the server
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Refresh");
        alert.setHeaderText("Messages Updated");
        alert.setContentText("Messages have been refreshed.");
        alert.showAndWait();
    }

    private VBox createSettingsPage() {
        VBox settingsSection = new VBox(20);
        settingsSection.setStyle("-fx-background-color: white; -fx-background-radius: 10;");
        settingsSection.setPadding(new Insets(20));

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Settings");
        title.setFont(Font.font("Roboto", FontWeight.BOLD, 24));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(title, spacer);

        // Settings Categories Grid
        GridPane categoriesGrid = new GridPane();
        categoriesGrid.setHgap(20);
        categoriesGrid.setVgap(20);
        categoriesGrid.setPadding(new Insets(20));

        // Create category buttons
        Button accountSettingsBtn = createSettingsCategoryButton("Account Settings", "Manage your personal information",
                "👤");
        Button farmInfoBtn = createSettingsCategoryButton("Farm Information", "Update your farm details", "🏡");
        Button produceDefaultsBtn = createSettingsCategoryButton("Produce Defaults",
                "Set default values for your products", "🌾");
        Button privacySecurityBtn = createSettingsCategoryButton("Privacy & Security", "Manage your privacy settings",
                "🔒");
        Button termsSupportBtn = createSettingsCategoryButton("Terms & Support", "View terms and get help", "📋");
        Button accountManagementBtn = createSettingsCategoryButton("Account Management", "Manage your account status",
                "⚙️");

        // Add click handlers
        Runnable showCategories = () -> showSettingsContent(new VBox(categoriesGrid), () -> {
        }); // disables back button on main
        accountSettingsBtn.setOnAction(e -> showSettingsContent(createAccountSettings(), showCategories));
        farmInfoBtn.setOnAction(e -> showSettingsContent(createFarmInformation(), showCategories));
        produceDefaultsBtn.setOnAction(e -> showSettingsContent(createProduceDefaults(), showCategories));
        privacySecurityBtn.setOnAction(e -> showSettingsContent(createPrivacySecurity(), showCategories));
        termsSupportBtn.setOnAction(e -> showSettingsContent(createTermsSupport(), showCategories));
        accountManagementBtn.setOnAction(e -> showSettingsContent(createAccountManagement(), showCategories));

        // Add buttons to grid
        categoriesGrid.add(accountSettingsBtn, 0, 0);
        categoriesGrid.add(farmInfoBtn, 1, 0);
        categoriesGrid.add(produceDefaultsBtn, 2, 0);
        categoriesGrid.add(privacySecurityBtn, 0, 1);
        categoriesGrid.add(termsSupportBtn, 1, 1);
        categoriesGrid.add(accountManagementBtn, 2, 1);

        // Content area for settings
        StackPane contentArea = new StackPane();
        contentArea.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 10;");
        contentArea.setPadding(new Insets(20));
        contentArea.setPrefHeight(400);

        // Initially show the categories grid
        contentArea.getChildren().add(categoriesGrid);

        settingsSection.getChildren().addAll(header, contentArea);
        return settingsSection;
    }

    private Button createSettingsCategoryButton(String title, String description, String icon) {
        VBox buttonContent = new VBox(5);
        buttonContent.setAlignment(Pos.CENTER);
        buttonContent.setPadding(new Insets(15));

        Label iconLabel = new Label(icon);
        iconLabel.setFont(Font.font("System", 29)); // Increased from 24
        iconLabel.setTextFill(Color.BLACK);

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Roboto", FontWeight.BOLD, 21)); // Increased from 16
        titleLabel.setTextFill(Color.BLACK);

        Label descLabel = new Label(description);
        descLabel.setFont(Font.font("Roboto", 17)); // Increased from 12
        descLabel.setTextFill(Color.BLACK);
        descLabel.setWrapText(true);

        buttonContent.getChildren().addAll(iconLabel, titleLabel, descLabel);

        Button button = new Button();
        button.setGraphic(buttonContent);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setPrefHeight(150);
        button.setStyle(
                "-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #e5e7eb; -fx-border-width: 1; -fx-border-radius: 10;");

        // Add hover effect
        button.setOnMouseEntered(e -> {
            button.setStyle(
                    "-fx-background-color: #f0fdf4; -fx-background-radius: 10; -fx-border-color: " + COLOR_PRIMARY_GREEN
                            + "; -fx-border-width: 1; -fx-border-radius: 10; -fx-cursor: hand;");
            titleLabel.setTextFill(Color.web(COLOR_PRIMARY_GREEN));
            descLabel.setTextFill(Color.web(COLOR_PRIMARY_GREEN));
            iconLabel.setTextFill(Color.web(COLOR_PRIMARY_GREEN));
        });
        button.setOnMouseExited(e -> {
            button.setStyle(
                    "-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #e5e7eb; -fx-border-width: 1; -fx-border-radius: 10;");
            titleLabel.setTextFill(Color.BLACK);
            descLabel.setTextFill(Color.BLACK);
            iconLabel.setTextFill(Color.BLACK);
        });

        return button;
    }

    private void showSettingsContent(VBox content, Runnable onBack) {
        BorderPane dashboardRoot = (BorderPane) primaryStage.getScene().getRoot();
        VBox wrapper = new VBox();
        wrapper.setAlignment(Pos.TOP_CENTER);
        wrapper.setPadding(new Insets(30, 0, 0, 0));
        wrapper.setSpacing(20);
        // Back button
        Button backBtn = new Button("← Back to Categories");
        backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + COLOR_PRIMARY_GREEN
                + "; -fx-font-weight: bold; -fx-font-size: 15px;");
        backBtn.setOnAction(e -> onBack.run());
        wrapper.getChildren().add(backBtn);
        // Centered content
        HBox centerBox = new HBox(content);
        centerBox.setAlignment(Pos.CENTER);
        wrapper.getChildren().add(centerBox);
        ScrollPane scrollPane = new ScrollPane(wrapper);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        dashboardRoot.setCenter(scrollPane);
    }

    private VBox createAccountSettings() {
        VBox content = new VBox(15);
        content.setAlignment(Pos.CENTER);
        content.setMaxWidth(420);
        content.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 30;");

        // Profile Picture
        HBox profileSection = new HBox(15);
        profileSection.setAlignment(Pos.CENTER_LEFT);

        StackPane profilePicture = new StackPane();
        profilePicture.setStyle("-fx-background-color: #e5e7eb; -fx-background-radius: 50;");
        profilePicture.setPrefSize(80, 80);

        ImageView profileImage = new ImageView();
        try {
            Image image = new Image(getClass().getResourceAsStream("/com/example/images/farmer-profile.png"));
            profileImage.setImage(image);
        } catch (Exception e) {
            Label initials = new Label(demoFarmer.getFullName().substring(0, 1));
            initials.setFont(Font.font("Roboto", FontWeight.BOLD, 32));
            initials.setTextFill(Color.WHITE);
            profilePicture.getChildren().add(initials);
        }

        profileImage.setFitWidth(80);
        profileImage.setFitHeight(80);
        profileImage.setPreserveRatio(true);
        profileImage.setSmooth(true);
        profilePicture.getChildren().add(profileImage);

        Button changePhotoBtn = new Button("Change Photo");
        changePhotoBtn.getStyleClass().add("button-secondary");
        VBox.setMargin(changePhotoBtn, new Insets(20, 0, 0, 0));

        profileSection.getChildren().addAll(profilePicture, changePhotoBtn);

        // Personal Information
        GridPane personalInfo = new GridPane();
        personalInfo.setHgap(10);
        personalInfo.setVgap(10);

        TextField fullNameField = new TextField(demoFarmer.getFullName());
        TextField emailField = new TextField(demoFarmer.getEmail());
        TextField phoneField = new TextField(demoFarmer.getPhoneNumber());
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter new password");

        Label fullNameLabel = new Label("Full Name:");
        Label emailLabel = new Label("Email:");
        Label phoneLabel = new Label("Phone:");
        Label passwordLabel = new Label("Password:");

        fullNameLabel.setTextFill(Color.web(COLOR_PRIMARY_GREEN));
        emailLabel.setTextFill(Color.web(COLOR_PRIMARY_GREEN));
        phoneLabel.setTextFill(Color.web(COLOR_PRIMARY_GREEN));
        passwordLabel.setTextFill(Color.web(COLOR_PRIMARY_GREEN));

        personalInfo.add(fullNameLabel, 0, 0);
        personalInfo.add(fullNameField, 1, 0);
        personalInfo.add(emailLabel, 0, 1);
        personalInfo.add(emailField, 1, 1);
        personalInfo.add(phoneLabel, 0, 2);
        personalInfo.add(phoneField, 1, 2);
        personalInfo.add(passwordLabel, 0, 3);
        personalInfo.add(passwordField, 1, 3);

        // Language Preference
        Label languageLabel = new Label("Language Preference:");
        languageLabel.setTextFill(Color.web(COLOR_PRIMARY_GREEN));
        ComboBox<String> languageCombo = new ComboBox<>();
        languageCombo.getItems().addAll("English", "French", "Spanish", "Arabic");
        languageCombo.setValue("English");
        languageCombo.setPromptText("Select Language");

        // 2FA Toggle
        ToggleSwitch twoFactorSwitch = new ToggleSwitch("Enable Two-Factor Authentication");

        Button saveBtn = new Button("Save Changes");
        saveBtn.getStyleClass().add("button-primary");
        saveBtn.setMaxWidth(200);

        content.getChildren().addAll(profileSection, personalInfo,
                languageLabel, languageCombo,
                twoFactorSwitch, saveBtn);

        return content;
    }

    private VBox createFarmInformation() {
        VBox content = new VBox(15);
        content.setAlignment(Pos.CENTER);
        content.setMaxWidth(420);
        content.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 30;");

        TextField farmNameField = new TextField(demoFarmer.getFarmName());
        farmNameField.setPromptText("Farm Name");

        Label farmNameLabel = new Label("Farm Name:");
        Label regionLabel = new Label("Region:");
        Label locationLabel = new Label("Specific Location:");

        farmNameLabel.setTextFill(Color.web(COLOR_PRIMARY_GREEN));
        regionLabel.setTextFill(Color.web(COLOR_PRIMARY_GREEN));
        locationLabel.setTextFill(Color.web(COLOR_PRIMARY_GREEN));

        // Location Selection
        ComboBox<String> regionCombo = new ComboBox<>();
        regionCombo.getItems().addAll("Greater Accra", "Ashanti", "Western", "Eastern", "Central", "Northern");
        regionCombo.setPromptText("Select Region");

        TextField specificLocation = new TextField();
        specificLocation.setPromptText("Specific Location/Address");

        Button viewMapBtn = new Button("View on Map");
        viewMapBtn.getStyleClass().add("button-secondary");

        Button saveBtn = new Button("Save Farm Information");
        saveBtn.getStyleClass().add("button-primary");
        saveBtn.setMaxWidth(200);

        content.getChildren().addAll(
                farmNameLabel, farmNameField,
                regionLabel, regionCombo,
                locationLabel, specificLocation,
                viewMapBtn, saveBtn);

        return content;
    }

    private VBox createProduceDefaults() {
        VBox content = new VBox(15);
        content.setAlignment(Pos.CENTER);
        content.setMaxWidth(420);
        content.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 30;");

        Label unitLabel = new Label("Preferred Unit:");
        Label currencyLabel = new Label("Default Currency:");
        Label priceRangeLabel = new Label("Price Range Guidance:");

        unitLabel.setTextFill(Color.web(COLOR_PRIMARY_GREEN));
        currencyLabel.setTextFill(Color.web(COLOR_PRIMARY_GREEN));
        priceRangeLabel.setTextFill(Color.web(COLOR_PRIMARY_GREEN));

        // Preferred Unit
        ComboBox<String> unitCombo = new ComboBox<>();
        unitCombo.getItems().addAll("Kilograms (kg)", "Bags", "Pounds (lbs)", "Crates", "Litres (L)");
        unitCombo.setPromptText("Select Preferred Unit");

        // Default Currency
        ComboBox<String> currencyCombo = new ComboBox<>();
        currencyCombo.getItems().addAll("Ghana Cedi (GHS)", "US Dollar (USD)", "Euro (EUR)", "British Pound (GBP)");
        currencyCombo.setPromptText("Select Currency");

        // Price Range
        HBox priceRange = new HBox(10);
        TextField minPrice = new TextField();
        minPrice.setPromptText("Minimum Price");
        TextField maxPrice = new TextField();
        maxPrice.setPromptText("Maximum Price");
        priceRange.getChildren().addAll(minPrice, maxPrice);

        CheckBox dynamicPricing = new CheckBox("Enable Dynamic Pricing");
        dynamicPricing.setTextFill(Color.web(COLOR_PRIMARY_GREEN));

        Button saveBtn = new Button("Save Preferences");
        saveBtn.getStyleClass().add("button-primary");
        saveBtn.setMaxWidth(200);

        content.getChildren().addAll(
                unitLabel, unitCombo,
                currencyLabel, currencyCombo,
                priceRangeLabel, priceRange,
                dynamicPricing, saveBtn);

        return content;
    }

    private VBox createPrivacySecurity() {
        VBox content = new VBox(15);
        content.setAlignment(Pos.CENTER);
        content.setMaxWidth(420);
        content.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 30;");

        // Location Visibility
        ToggleSwitch locationVisibility = new ToggleSwitch("Show Farm Location to Buyers");

        // Blocked Buyers
        VBox blockedBuyers = new VBox(10);
        Label blockedLabel = new Label("Blocked Buyers:");
        blockedLabel.setTextFill(Color.web(COLOR_PRIMARY_GREEN));
        ListView<String> blockedList = new ListView<>();
        blockedList.getItems().addAll("John Doe", "Jane Smith", "Mike Johnson");
        blockedList.setPrefHeight(100);

        Button unblockBtn = new Button("Unblock Selected");
        unblockBtn.getStyleClass().add("button-secondary");

        blockedBuyers.getChildren().addAll(
                blockedLabel,
                blockedList,
                unblockBtn);

        // Session History
        VBox sessionHistory = new VBox(10);
        Label sessionsLabel = new Label("Recent Sessions:");
        sessionsLabel.setTextFill(Color.web(COLOR_PRIMARY_GREEN));
        ListView<String> sessions = new ListView<>();
        sessions.getItems().addAll(
                "Login from Chrome - Accra, Ghana (2 hours ago)",
                "Login from Mobile App - Kumasi, Ghana (1 day ago)",
                "Login from Firefox - Tema, Ghana (3 days ago)");
        sessions.setPrefHeight(100);

        sessionHistory.getChildren().addAll(
                sessionsLabel,
                sessions);

        Button saveBtn = new Button("Save Privacy Settings");
        saveBtn.getStyleClass().add("button-primary");
        saveBtn.setMaxWidth(200);

        content.getChildren().addAll(
                locationVisibility,
                blockedBuyers,
                sessionHistory,
                saveBtn);

        return content;
    }

    private VBox createTermsSupport() {
        VBox content = new VBox(15);
        content.setAlignment(Pos.CENTER);
        content.setMaxWidth(420);
        content.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 30;");

        Button termsBtn = new Button("View Terms of Use");
        Button privacyBtn = new Button("View Privacy Policy");
        Button supportBtn = new Button("Contact Support");
        Button faqBtn = new Button("Help Center / FAQ");

        termsBtn.getStyleClass().add("button-secondary");
        privacyBtn.getStyleClass().add("button-secondary");
        supportBtn.getStyleClass().add("button-secondary");
        faqBtn.getStyleClass().add("button-secondary");

        content.getChildren().addAll(termsBtn, privacyBtn, supportBtn, faqBtn);
        return content;
    }

    private VBox createAccountManagement() {
        VBox content = new VBox(15);
        content.setAlignment(Pos.CENTER);
        content.setMaxWidth(420);
        content.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 30;");

        Button logoutBtn = new Button("Logout");
        Button deactivateBtn = new Button("Deactivate Account");
        Button deleteBtn = new Button("Delete Account");

        logoutBtn.getStyleClass().add("button-primary");
        deactivateBtn.getStyleClass().add("button-danger");
        deleteBtn.getStyleClass().add("button-danger");

        content.getChildren().addAll(logoutBtn, deactivateBtn, deleteBtn);
        return content;
    }

    // Custom Toggle Switch Control
    private class ToggleSwitch extends HBox {
        private final Label label;
        private final Button button;
        private boolean selected;

        public ToggleSwitch(String text) {
            label = new Label(text);
            label.setFont(Font.font("Roboto", 14));
            label.setTextFill(Color.web(COLOR_PRIMARY_GREEN));

            button = new Button();
            button.setPrefSize(50, 25);
            button.setStyle("-fx-background-color: #e5e7eb; -fx-background-radius: 12;");
            button.setOnAction(e -> toggle());

            setSpacing(10);
            setAlignment(Pos.CENTER_LEFT);
            getChildren().addAll(label, button);
        }

        private void toggle() {
            selected = !selected;
            button.setStyle(selected ? "-fx-background-color: " + COLOR_PRIMARY_GREEN + "; -fx-background-radius: 12;"
                    : "-fx-background-color: #e5e7eb; -fx-background-radius: 12;");
        }

        public boolean isSelected() {
            return selected;
        }
    }

    private VBox createCustomerMainContent() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: white;");

        // Featured Products Section
        Label featuredTitle = new Label("Featured Products");
        featuredTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");

        FlowPane featuredProducts = new FlowPane();
        featuredProducts.setHgap(20);
        featuredProducts.setVgap(20);
        featuredProducts.setPrefWrapLength(800);

        // Add some sample featured products
        for (int i = 0; i < 4; i++) {
            VBox productCard = createProductCard(
                    "Organic " + (i % 2 == 0 ? "Tomatoes" : "Potatoes"),
                    "Fresh from local farms",
                    (i + 1) * 5.99,
                    "kg");
            featuredProducts.getChildren().add(productCard);
        }

        // Recent Orders Section
        Label ordersTitle = new Label("Recent Orders");
        ordersTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");

        VBox ordersList = new VBox(10);
        ordersList.setStyle("-fx-background-color: #F5F5F5; -fx-padding: 15px; -fx-background-radius: 10px;");

        // Add some sample orders
        for (int i = 0; i < 3; i++) {
            HBox orderItem = new HBox(15);
            orderItem.setAlignment(Pos.CENTER_LEFT);
            orderItem.setStyle("-fx-background-color: white; -fx-padding: 15px; -fx-background-radius: 8px;");

            Label orderTitle = new Label("Order #" + (1000 + i));
            orderTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

            Label orderStatus = new Label(i == 0 ? "Delivered" : (i == 1 ? "In Transit" : "Processing"));
            orderStatus.setStyle("-fx-text-fill: " + (i == 0 ? "#2E7D32" : (i == 1 ? "#1976D2" : "#F57C00")) + ";");

            Label orderDate = new Label("2024-03-" + (10 + i));
            orderDate.setStyle("-fx-text-fill: #666;");

            orderItem.getChildren().addAll(orderTitle, orderStatus, orderDate);
            ordersList.getChildren().add(orderItem);
        }

        // Add all sections to the content
        content.getChildren().addAll(
                featuredTitle,
                featuredProducts,
                ordersTitle,
                ordersList);

        return content;
    }

    private VBox createProductCard(String name, String description, double price, String unit) {
        VBox card = new VBox(10);
        card.setStyle(
                "-fx-background-color: white; -fx-padding: 15px; -fx-background-radius: 10px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        card.setPrefWidth(200);
        card.setPrefHeight(250);

        // Product Image (placeholder)
        Rectangle imagePlaceholder = new Rectangle(170, 120);
        imagePlaceholder.setFill(Color.LIGHTGRAY);
        imagePlaceholder.setArcWidth(10);
        imagePlaceholder.setArcHeight(10);

        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        Label descLabel = new Label(description);
        descLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");
        descLabel.setWrapText(true);

        Label priceLabel = new Label(String.format("$%.2f/%s", price, unit));
        priceLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: #2E7D32;");

        Button addToCartBtn = new Button("Add to Cart");
        addToCartBtn.setStyle(
                "-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 15px; -fx-background-radius: 5px;");
        addToCartBtn.setOnAction(e -> {
            // Add to cart logic
            cartItems.add(new CartItem(name, price, unit, 1));
            updateCartBadge();
            showNotification("Added to cart: " + name);
        });

        card.getChildren().addAll(imagePlaceholder, nameLabel, descLabel, priceLabel, addToCartBtn);
        return card;
    }

    private void showNotification(String message) {
        VBox notification = new VBox();
        notification.setStyle("-fx-background-color: #2E7D32; -fx-padding: 15px; -fx-background-radius: 5px;");
        notification.setMaxWidth(300);

        Label messageLabel = new Label(message);
        messageLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        notification.getChildren().add(messageLabel);

        StackPane.setAlignment(notification, Pos.TOP_RIGHT);
        StackPane.setMargin(notification, new Insets(20));

        // Add to the root stack pane
        root.getChildren().add(notification);

        // Animate and remove after 3 seconds
        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), notification);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(500), notification);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setDelay(Duration.seconds(2.5));

        fadeIn.play();
        fadeOut.play();

        fadeOut.setOnFinished(e -> root.getChildren().remove(notification));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
