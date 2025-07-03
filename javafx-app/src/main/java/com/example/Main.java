package com.example;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
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

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.productsList = FXCollections.observableArrayList();

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
            Image localImage = new Image(getClass().getResourceAsStream("/com/example/images/greenfield.jpeg"));
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

        primaryStage.setTitle("Farmers & Customers Interaction App");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(560);

        primaryStage.show();
    }

    private VBox createLoginChoicePane() {
        VBox container = new VBox(28);
        container.setFillWidth(true);
        container.setMaxWidth(500);

        Label title = new Label("Login as");
        title.getStyleClass().add("title-label");

        Button customerBtn = new Button("Customer");
        Button farmerBtn = new Button("Farmer");
        customerBtn.getStyleClass().add("button-primary");
        farmerBtn.getStyleClass().add("button-primary");
        customerBtn.setMinWidth(180);
        farmerBtn.setMinWidth(180);
        customerBtn.setMaxWidth(Double.MAX_VALUE);
        farmerBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(customerBtn, Priority.ALWAYS);
        HBox.setHgrow(farmerBtn, Priority.ALWAYS);

        HBox buttonBox = new HBox(48, customerBtn, farmerBtn);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        buttonBox.setFillHeight(true);
        buttonBox.setMaxWidth(Double.MAX_VALUE);

        CheckBox rememberMe = new CheckBox("Remember me");
        rememberMe.setStyle("-fx-font-size: 14px;");
        rememberMe.setTextFill(Color.web(COLOR_GRAY_TEXT));

        Label forgotPassword = new Label("Forgot password?");
        forgotPassword.getStyleClass().add("link-label");

        HBox bottomLinks = new HBox();
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        bottomLinks.getChildren().addAll(rememberMe, spacer, forgotPassword);
        bottomLinks.setAlignment(Pos.CENTER_LEFT);

        Label signUpLink = new Label("Don't have an account? Sign up");
        signUpLink.getStyleClass().add("link-label");

        customerBtn.setOnAction(e -> switchToLoginForm("Customer"));
        farmerBtn.setOnAction(e -> switchToLoginForm("Farmer"));
        signUpLink.setOnMouseClicked(e -> switchToSignUpForm());
        forgotPassword.setOnMouseClicked(e -> showForgotPasswordDialog());

        container.getChildren().addAll(
                title,
                buttonBox,
                bottomLinks,
                new Region(),
                signUpLink);
        VBox.setVgrow(container.getChildren().get(container.getChildren().size() - 2), Priority.ALWAYS);
        VBox.setVgrow(signUpLink, Priority.NEVER);

        // Wrap the container in a ScrollPane
        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        return new VBox(scrollPane);
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
        VBox container = new VBox(18);
        container.setFillWidth(true);
        container.setMaxWidth(380);
        container.getStyleClass().add("container-box");

        Label header = new Label(role + " Login");
        header.getStyleClass().add("title-label");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.setMaxWidth(Double.MAX_VALUE);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setMaxWidth(Double.MAX_VALUE);

        final TextField farmNameField = "Farmer".equals(role) ? new TextField() : null;
        if (farmNameField != null) {
            farmNameField.setPromptText("Farm Name");
            farmNameField.setMaxWidth(Double.MAX_VALUE);
            container.getChildren().addAll(header, usernameField, farmNameField, passwordField);
        } else {
            container.getChildren().addAll(header, usernameField, passwordField);
        }

        CheckBox rememberMe = new CheckBox("Remember me");
        rememberMe.setStyle("-fx-font-size: 14px;");
        rememberMe.setTextFill(Color.web(COLOR_GRAY_TEXT));

        Label forgotPassword = new Label("Forgot password?");
        forgotPassword.getStyleClass().add("link-label");
        forgotPassword.setOnMouseClicked(e -> showForgotPasswordDialog());

        HBox bottomBox = new HBox();
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        bottomBox.getChildren().addAll(rememberMe, spacer, forgotPassword);
        bottomBox.setAlignment(Pos.CENTER_LEFT);

        Button loginBtn = new Button("Sign In");
        loginBtn.getStyleClass().add("button-black");
        loginBtn.setMaxWidth(205);
        loginBtn.setMinWidth(205);
        VBox.setMargin(loginBtn, new Insets(16, 0, 0, 0));

        // Center align the button
        HBox buttonContainer = new HBox(loginBtn);
        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.setMaxWidth(Double.MAX_VALUE);

        // Add login button action
        loginBtn.setOnAction(e -> {
            if ("Farmer".equals(role)) {
                // For demo purposes, we'll create a farmer object and show the dashboard
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
                // Create a customer object and show the customer dashboard
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
        VBox.setVgrow(backLink, Priority.ALWAYS);
        backLink.setMaxWidth(Double.MAX_VALUE);
        backLink.setAlignment(Pos.BOTTOM_LEFT);
        VBox.setMargin(backLink, new Insets(10, 0, 0, 0));

        container.getChildren().addAll(bottomBox, buttonContainer, backLink);

        // Wrap the container in a ScrollPane
        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        return new VBox(scrollPane);
    }

    private VBox createSignUpForm() {
        VBox container = new VBox(16);
        container.setFillWidth(true);
        container.setMaxWidth(380);
        container.getStyleClass().add("container-box");

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

        // Wrap the container in a ScrollPane
        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        return new VBox(scrollPane);
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

        Label welcomeLabel = new Label("Welcome, " + farmer.getFullName());
        welcomeLabel.setFont(Font.font("System", FontWeight.BOLD, 16));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button profileBtn = new Button("Profile");
        profileBtn.getStyleClass().add("button-primary");

        topBar.getChildren().addAll(welcomeLabel, spacer, profileBtn);
        return topBar;
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox(10);
        sidebar.setStyle(
                "-fx-background-color: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        sidebar.setPadding(new Insets(20));
        sidebar.setPrefWidth(200);

        Label menuLabel = new Label("Menu");
        menuLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        VBox.setMargin(menuLabel, new Insets(0, 0, 20, 0));

        Button dashboardBtn = createMenuButton("Dashboard", true);
        Button productsBtn = createMenuButton("Products", false);
        Button ordersBtn = createMenuButton("Orders", false);
        Button messagesBtn = createMenuButton("Messages", false);
        Button settingsBtn = createMenuButton("Settings", false);

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
        button.getStyleClass().add(isSelected ? "button-primary" : "button-secondary");
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
        title.setFont(Font.font("System", FontWeight.BOLD, 20));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addProductBtn = new Button("Add New Product");
        addProductBtn.getStyleClass().add("button-primary");

        header.getChildren().addAll(title, spacer, addProductBtn);

        // Products Table
        productsTable = new TableView<>();
        productsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Product, String> nameCol = new TableColumn<>("Product Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Product, Double> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));

        TableColumn<Product, String> unitCol = new TableColumn<>("Unit");
        unitCol.setCellValueFactory(new PropertyValueFactory<>("unit"));

        TableColumn<Product, Integer> quantityCol = new TableColumn<>("Quantity");
        quantityCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<Product, String> actionsCol = new TableColumn<>("Actions");
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox buttons = new HBox(10, editBtn, deleteBtn);

            {
                editBtn.getStyleClass().add("button-secondary");
                deleteBtn.getStyleClass().add("button-danger");
                buttons.setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttons);
            }
        });

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
        BorderPane dashboardRoot = new BorderPane();
        dashboardRoot.setStyle("-fx-background-color: #f5f5f5;");

        // Top Navigation Bar
        HBox topBar = createCustomerTopBar(customer);
        dashboardRoot.setTop(topBar);

        // Main Content Area with ScrollPane
        ScrollPane scrollPane = new ScrollPane(createCustomerMainContent());
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        dashboardRoot.setCenter(scrollPane);

        Scene dashboardScene = new Scene(dashboardRoot, 1200, 800);
        dashboardScene.getStylesheets().add(getClass().getResource("/com/example/styles.css").toExternalForm());

        primaryStage.setTitle("Customer Dashboard - " + customer.getFullName());
        primaryStage.setScene(dashboardScene);
    }

    private HBox createCustomerTopBar(Customer customer) {
        HBox topBar = new HBox();
        topBar.setStyle("-fx-background-color: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        topBar.setPadding(new Insets(15));
        topBar.setSpacing(20);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label welcomeLabel = new Label("Welcome, " + customer.getFullName());
        welcomeLabel.setFont(Font.font("System", FontWeight.BOLD, 16));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button profileBtn = new Button("Profile");
        profileBtn.getStyleClass().add("button-primary");

        Button logoutBtn = new Button("Logout");
        logoutBtn.getStyleClass().add("button-danger");
        logoutBtn.setOnAction(e -> {
            primaryStage.setScene(scene);
            primaryStage.setTitle("Farmers & Customers Interaction App");
        });

        topBar.getChildren().addAll(welcomeLabel, spacer, profileBtn, logoutBtn);
        return topBar;
    }

    private VBox createCustomerMainContent() {
        VBox mainContent = new VBox(20);
        mainContent.setPadding(new Insets(20));
        mainContent.setStyle("-fx-background-color: white; -fx-background-radius: 10;");

        // Header
        Label title = new Label("Available Products");
        title.setFont(Font.font("System", FontWeight.BOLD, 24));
        VBox.setMargin(title, new Insets(0, 0, 20, 0));

        // Products Grid
        GridPane productsGrid = new GridPane();
        productsGrid.setHgap(20);
        productsGrid.setVgap(20);
        productsGrid.setPadding(new Insets(20));

        // Sample products (in a real app, this would come from a database)
        int row = 0;
        int col = 0;
        for (Product product : productsList) {
            VBox productCard = createProductCard(product);
            productsGrid.add(productCard, col, row);
            col++;
            if (col > 2) { // 3 products per row
                col = 0;
                row++;
            }
        }

        mainContent.getChildren().addAll(title, productsGrid);
        return mainContent;
    }

    private VBox createProductCard(Product product) {
        VBox card = new VBox(10);
        card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        card.setPadding(new Insets(15));
        card.setMaxWidth(300);

        // Product Image
        ImageView productImage = new ImageView();
        if (product.getImagePath() != null) {
            try {
                Image image = new Image(getClass().getResourceAsStream(product.getImagePath()));
                productImage.setImage(image);
            } catch (Exception e) {
                System.err.println("Error loading image: " + e.getMessage());
                try {
                    productImage.setImage(
                            new Image(getClass().getResourceAsStream("/com/example/images/default-product.png")));
                } catch (Exception ex) {
                    System.err.println("Error loading default image: " + ex.getMessage());
                }
            }
        }
        productImage.setFitWidth(250);
        productImage.setFitHeight(200);
        productImage.setPreserveRatio(true);
        productImage.setSmooth(true);

        // Product Details
        Label nameLabel = new Label(product.getName());
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 16));

        Label priceLabel = new Label(String.format("Price: $%.2f", product.getPrice()));
        priceLabel.setFont(Font.font("System", 14));

        Label quantityLabel = new Label("Available: " + product.getQuantity() + " " + product.getUnit());
        quantityLabel.setFont(Font.font("System", 14));

        // Bid Section
        TextField bidField = new TextField();
        bidField.setPromptText("Enter your bid");
        bidField.setMaxWidth(150);

        Button bidButton = new Button("Place Bid");
        bidButton.getStyleClass().add("button-primary");

        Button buyButton = new Button("Buy Now");
        buyButton.getStyleClass().add("button-black");

        HBox buttonBox = new HBox(10, bidButton, buyButton);
        buttonBox.setAlignment(Pos.CENTER);

        // Farmer Details (initially hidden)
        VBox farmerDetails = new VBox(5);
        farmerDetails.setVisible(false);
        farmerDetails.setManaged(false);
        farmerDetails.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 5;");
        farmerDetails.setPadding(new Insets(10));

        // Add action handlers
        bidButton.setOnAction(e -> {
            try {
                double bidAmount = Double.parseDouble(bidField.getText());
                if (bidAmount > 0) {
                    showAlert("Bid Placed", "Your bid of $" + bidAmount + " has been placed for " + product.getName());
                } else {
                    showError("Invalid Bid", "Please enter a valid bid amount");
                }
            } catch (NumberFormatException ex) {
                showError("Invalid Bid", "Please enter a valid number");
            }
        });

        buyButton.setOnAction(e -> {
            System.out.println("Buy Now button clicked");
            showFarmerDetails(product, farmerDetails);
        });

        card.getChildren().addAll(
                productImage,
                nameLabel,
                priceLabel,
                quantityLabel,
                bidField,
                buttonBox,
                farmerDetails);

        return card;
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

    public static void main(String[] args) {
        launch(args);
    }
}
