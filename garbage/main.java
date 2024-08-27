import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

// Enum for Waste Type
enum WasteType {
    ORGANIC,
    RECYCLABLE,
    NON_RECYCLABLE
}

// Class for Waste Segregation Logic
class WasteSegregation {
    static WasteType segregateWaste(int proximityReading, int colorReading, int metalDetectionReading) {
        if (proximityReading < 50 && colorReading < 100 && metalDetectionReading < 200) {
            return WasteType.ORGANIC;
        } else if (proximityReading > 150 && colorReading > 200 && metalDetectionReading > 275) {
            return WasteType.NON_RECYCLABLE;
        } else {
            return WasteType.RECYCLABLE;
        }
    }
}

// Swing UI for Smart Dustbin
class WasteSegregatorUI extends JFrame {

    private JTabbedPane tabbedPane;
    private JPanel inputPanel, resultPanel, reportPanel;
    private JLabel statusLabel, energyStatusLabel;
    private JButton startSegregationButton, reportButton, exportButton;
    private JTextArea resultTextField, suggestionTextArea, reportTextArea;
    private JTextField proximityField, colorField, metalDetectionField;
    private JCheckBox randomizeCheckbox;
    private Timer energySavingTimer;
    private boolean isEnergySavingMode = false;
    private DefaultTableModel logTableModel;
    private JTable logTable;

    public WasteSegregatorUI() {
        setTitle("Smart Waste Segregator");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Create tabbed pane
        tabbedPane = new JTabbedPane();

        // Input Panel
        inputPanel = new JPanel();
        inputPanel.setLayout(new GridLayout(5, 2, 10, 10));
        inputPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        proximityField = new JTextField();
        colorField = new JTextField();
        metalDetectionField = new JTextField();
        randomizeCheckbox = new JCheckBox("Randomize Values");

        inputPanel.add(new JLabel("Proximity Reading:"));
        inputPanel.add(proximityField);
        inputPanel.add(new JLabel("Color Reading:"));
        inputPanel.add(colorField);
        inputPanel.add(new JLabel("Metal Detection Reading:"));
        inputPanel.add(metalDetectionField);
        inputPanel.add(new JLabel("Randomize Values:"));
        inputPanel.add(randomizeCheckbox);

        startSegregationButton = new JButton("Start Segregation");
        startSegregationButton.addActionListener(new StartSegregationAction());
        inputPanel.add(startSegregationButton);

        tabbedPane.addTab("Input", inputPanel);

        // Result Panel
        resultPanel = new JPanel();
        resultPanel.setLayout(new BorderLayout(10, 10));
        resultPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        resultTextField = new JTextArea(5, 40);
        suggestionTextArea = new JTextArea(5, 40);
        resultTextField.setEditable(false);
        suggestionTextArea.setEditable(false);
        resultTextField.setBorder(BorderFactory.createTitledBorder("Segregation Result"));
        suggestionTextArea.setBorder(BorderFactory.createTitledBorder("Suggestion"));

        resultPanel.add(resultTextField, BorderLayout.NORTH);
        resultPanel.add(suggestionTextArea, BorderLayout.CENTER);

        tabbedPane.addTab("Results", resultPanel);

        // Report Panel
        reportPanel = new JPanel();
        reportPanel.setLayout(new BorderLayout(10, 10));
        reportPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        reportTextArea = new JTextArea(20, 60);
        reportTextArea.setEditable(false);
        reportTextArea.setBorder(BorderFactory.createTitledBorder("Generated Report"));

        logTableModel = new DefaultTableModel(new String[]{"Date", "Proximity", "Color", "Metal", "Waste Type"}, 0);
        logTable = new JTable(logTableModel);
        JScrollPane logScrollPane = new JScrollPane(logTable);

        reportButton = new JButton("Generate Report");
        reportButton.addActionListener(new GenerateReportAction());
        exportButton = new JButton("Export to CSV");
        exportButton.addActionListener(new ExportCSVAction());

        JPanel reportButtonPanel = new JPanel();
        reportButtonPanel.add(reportButton);
        reportButtonPanel.add(exportButton);

        reportPanel.add(logScrollPane, BorderLayout.CENTER);
        reportPanel.add(reportButtonPanel, BorderLayout.SOUTH);

        tabbedPane.addTab("Report", reportPanel);

        // Add components to the frame
        add(tabbedPane, BorderLayout.CENTER);

        // Status and Energy Management
        JPanel statusPanel = new JPanel(new GridLayout(2, 1));
        statusLabel = new JLabel("Status: Not Started");
        energyStatusLabel = new JLabel("Energy Mode: Normal");

        statusPanel.add(statusLabel);
        statusPanel.add(energyStatusLabel);

        add(statusPanel, BorderLayout.SOUTH);

        // Energy management timer
        energySavingTimer = new Timer();
        energySavingTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                isEnergySavingMode = !isEnergySavingMode;
                SwingUtilities.invokeLater(() -> energyStatusLabel.setText("Energy Mode: " + (isEnergySavingMode ? "Saving" : "Normal")));
            }
        }, 0, 60000);
    }

    private class StartSegregationAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (randomizeCheckbox.isSelected()) {
                Random random = new Random();
                int proximityReading = random.nextInt(100);
                int colorReading = random.nextInt(256);
                int metalDetectionReading = random.nextInt(300);
                proximityField.setText(Integer.toString(proximityReading));
                colorField.setText(Integer.toString(colorReading));
                metalDetectionField.setText(Integer.toString(metalDetectionReading));
            }

            try {
                int proximityReading = Integer.parseInt(proximityField.getText());
                int colorReading = Integer.parseInt(colorField.getText());
                int metalDetectionReading = Integer.parseInt(metalDetectionField.getText());

                WasteType segregatedWaste = WasteSegregation.segregateWaste(proximityReading, colorReading, metalDetectionReading);

                // Update result text field and suggestion area
                resultTextField.setText("Result: " + segregatedWaste.toString());

                String suggestion = getSegregationSuggestion(segregatedWaste);
                suggestionTextArea.setText("Suggestion: " + suggestion);

                // Store the readings and result in the database
                storeDataInDatabase(proximityReading, colorReading, metalDetectionReading, segregatedWaste);

                // Update the log table
                updateLogTable();

                statusLabel.setText("Status: Segregation Completed");

            } catch (NumberFormatException ex) {
                resultTextField.setText("Result: Error");
                suggestionTextArea.setText("Suggestion: Please enter valid numbers.");
                statusLabel.setText("Status: Error");
            }
        }
    }

    private String getSegregationSuggestion(WasteType wasteType) {
        switch (wasteType) {
            case ORGANIC:
                return "Dispose in organic waste bin.";
            case RECYCLABLE:
                return "Dispose in recyclable waste bin.";
            case NON_RECYCLABLE:
                return "Dispose in non-recyclable waste bin.";
            default:
                return "Unknown waste type.";
        }
    }

    private void storeDataInDatabase(int proximityReading, int colorReading, int metalDetectionReading, WasteType segregatedWaste) {
        String jdbcUrl = "jdbc:mysql://localhost:3306/wastesegregator";
        String username = "root";
        String password = "jeffjudev";

        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
            String insertQuery = "INSERT INTO logs (timestamp, proximity_reading, color_reading, metal_detection_reading, waste_type) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
                preparedStatement.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                preparedStatement.setInt(2, proximityReading);
                preparedStatement.setInt(3, colorReading);
                preparedStatement.setInt(4, metalDetectionReading);
                preparedStatement.setString(5, segregatedWaste.toString());
                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateLogTable() {
        // Clear existing rows
        logTableModel.setRowCount(0);

        String jdbcUrl = "jdbc:mysql://localhost:3306/wastesegregator";
        String username = "root";
        String password = "jeffjudev";

        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
            String query = "SELECT * FROM logs";
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery(query)) {

                while (resultSet.next()) {
                    String date = resultSet.getTimestamp("timestamp").toString();
                    int proximityReading = resultSet.getInt("proximity_reading");
                    int colorReading = resultSet.getInt("color_reading");
                    int metalDetectionReading = resultSet.getInt("metal_detection_reading");
                    String wasteType = resultSet.getString("waste_type");

                    logTableModel.addRow(new Object[]{date, proximityReading, colorReading, metalDetectionReading, wasteType});
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private class GenerateReportAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            generateReport();
        }
    }

    private class ExportCSVAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            exportToCSV();
        }
    }

    private void generateReport() {
        StringBuilder reportBuilder = new StringBuilder();
        reportBuilder.append("Smart Waste Segregator Report\n");
        reportBuilder.append("=================================\n");
        reportBuilder.append("Date Range: ");
        // Add logic to determine date range
        reportBuilder.append("\n\n");

        reportBuilder.append("Summary:\n");
        reportBuilder.append("Total Entries: ").append(getTotalEntries()).append("\n");
        reportBuilder.append("Average Proximity Reading: ").append(getAverageReading("proximity_reading")).append("\n");
        reportBuilder.append("Average Color Reading: ").append(getAverageReading("color_reading")).append("\n");
        reportBuilder.append("Average Metal Detection Reading: ").append(getAverageReading("metal_detection_reading")).append("\n");
        reportBuilder.append("Waste Type Distribution:\n");
        reportBuilder.append("Organic: ").append(getWasteTypePercentage(WasteType.ORGANIC)).append("%\n");
        reportBuilder.append("Recyclable: ").append(getWasteTypePercentage(WasteType.RECYCLABLE)).append("%\n");
        reportBuilder.append("Non-Recyclable: ").append(getWasteTypePercentage(WasteType.NON_RECYCLABLE)).append("%\n");

        reportTextArea.setText(reportBuilder.toString());
    }

    private int getTotalEntries() {
        int totalEntries = 0;
        String jdbcUrl = "jdbc:mysql://localhost:3306/wastesegregator";
        String username = "root";
        String password = "jeffjudev";

        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
            String query = "SELECT COUNT(*) FROM logs";
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery(query)) {
                if (resultSet.next()) {
                    totalEntries = resultSet.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return totalEntries;
    }

    private double getAverageReading(String columnName) {
        double averageReading = 0;
        String jdbcUrl = "jdbc:mysql://localhost:3306/wastesegregator";
        String username = "root";
        String password = "jeffjudev";

        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
            String query = "SELECT AVG(" + columnName + ") FROM logs";
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery(query)) {
                if (resultSet.next()) {
                    averageReading = resultSet.getDouble(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return averageReading;
    }

    private double getWasteTypePercentage(WasteType wasteType) {
        double percentage = 0;
        String jdbcUrl = "jdbc:mysql://localhost:3306/wastesegregator";
        String username = "root";
        String password = "jeffjudev";

        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
            String totalQuery = "SELECT COUNT(*) FROM logs";
            String typeQuery = "SELECT COUNT(*) FROM logs WHERE waste_type = ?";
            
            try (Statement statement = connection.createStatement();
                 ResultSet totalResultSet = statement.executeQuery(totalQuery)) {
                int totalEntries = totalResultSet.next() ? totalResultSet.getInt(1) : 1;

                try (PreparedStatement preparedStatement = connection.prepareStatement(typeQuery)) {
                    preparedStatement.setString(1, wasteType.toString());
                    try (ResultSet typeResultSet = preparedStatement.executeQuery()) {
                        int typeCount = typeResultSet.next() ? typeResultSet.getInt(1) : 0;
                        percentage = (typeCount / (double) totalEntries) * 100;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return percentage;
    }

    private void exportToCSV() {
        String filePath = "waste_log.csv";
        try (FileWriter fileWriter = new FileWriter(filePath);
             PrintWriter printWriter = new PrintWriter(fileWriter)) {

            printWriter.println("Date,Proximity,Color,Metal,Waste Type");

            String jdbcUrl = "jdbc:mysql://localhost:3306/wastesegregator";
            String username = "root";
            String password = "jeffjudev";

            try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
                String query = "SELECT * FROM logs";
                try (Statement statement = connection.createStatement();
                     ResultSet resultSet = statement.executeQuery(query)) {

                    while (resultSet.next()) {
                        String date = resultSet.getTimestamp("timestamp").toString();
                        int proximityReading = resultSet.getInt("proximity_reading");
                        int colorReading = resultSet.getInt("color_reading");
                        int metalDetectionReading = resultSet.getInt("metal_detection_reading");
                        String wasteType = resultSet.getString("waste_type");

                        printWriter.printf("%s,%d,%d,%d,%s%n", date, proximityReading, colorReading, metalDetectionReading, wasteType);
                    }
                }
            }

            JOptionPane.showMessageDialog(this, "Data exported to " + filePath, "Export Successful", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException | SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error exporting data.", "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

// Main Class
public class main{
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new WasteSegregatorUI().setVisible(true));
    }
}
