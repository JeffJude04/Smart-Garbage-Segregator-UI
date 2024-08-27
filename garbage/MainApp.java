import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Random;


enum WasteType {
    ORGANIC,
    RECYCLABLE,
    NON_RECYCLABLE
}


class WasteSegregation {
    static WasteType segregateWaste(int proximityReading, int colorReading, int metalDetectionReading) {
        //segregation logic 
        
        if (proximityReading < 50 && colorReading < 100 && metalDetectionReading < 200) {
            return WasteType.ORGANIC;
        } else if (proximityReading > 150 && colorReading > 200 && metalDetectionReading > 275) {
            return WasteType.NON_RECYCLABLE;
        } else {
            return WasteType.RECYCLABLE;
        }
    }
}

// Swing UI 
class WasteSegregatorUI extends JFrame {

    private JLabel statusLabel;
    private JButton startSegregationButton;
    
    private JTextArea resultTextField;
    private JTextField proximityField;
    private JTextField colorField;
    private JTextField metalDetectionField;
    private JCheckBox randomizeCheckbox;

    public WasteSegregatorUI() {
    
        setTitle("Smart Waste Segregator");
        setSize(400, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        
        ImageIcon backgroundIcon = new ImageIcon("C:\\Users\\Jude Vincent\\DesktMAGE1.jpg"); // Update with your image file path
        JLabel backgroundLabel = new JLabel(backgroundIcon);
        setContentPane(backgroundLabel);
        setLayout(new BorderLayout());

        //UI components 
        statusLabel = new JLabel("Status: Not Started");
        statusLabel.setForeground(Color.BLUE); // Set color to blue
        startSegregationButton = new JButton("Start Segregation");
        startSegregationButton.setBackground(Color.GREEN); // Set background color to green
        resultTextField = new JTextArea(5, 20);
        resultTextField.setBackground(Color.YELLOW); // Set background color to yellow
        resultTextField.setEditable(false);
        resultTextField.setFont(new Font("Arial", Font.BOLD, 14)); // Set font size and style
        JScrollPane resultScrollPane = new JScrollPane(resultTextField); 
        proximityField = new JTextField();
        colorField = new JTextField();
        metalDetectionField = new JTextField();
        randomizeCheckbox = new JCheckBox("Randomize Values");
        randomizeCheckbox.setForeground(Color.RED); // Set color to red

        
        add(statusLabel, BorderLayout.NORTH);

    
        JPanel inputPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        inputPanel.setOpaque(false); // Make the panel transparent
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        inputPanel.add(new JLabel("Proximity Reading:"));
        inputPanel.add(proximityField);
        inputPanel.add(new JLabel("Color Reading:"));
        inputPanel.add(colorField);
        inputPanel.add(new JLabel("Metal Detection Reading:"));
        inputPanel.add(metalDetectionField);
        inputPanel.add(new JLabel("Randomize Values:"));
        inputPanel.add(randomizeCheckbox);
        add(inputPanel, BorderLayout.CENTER);

        
        JPanel resultPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        resultPanel.setOpaque(false); 
        resultPanel.add(resultScrollPane);
        resultPanel.add(startSegregationButton);
        add(resultPanel, BorderLayout.SOUTH);

        
        startSegregationButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (randomizeCheckbox.isSelected()) {
                    // Randomize sensor readings
                    Random random = new Random();
                    int proximityReading = random.nextInt(100);
                    int colorReading = random.nextInt(256);
                    int metalDetectionReading = random.nextInt(300); // Update based on your range
                    proximityField.setText(Integer.toString(proximityReading));
                    colorField.setText(Integer.toString(colorReading));
                    metalDetectionField.setText(Integer.toString(metalDetectionReading));
                }

                try {
                    // Get sensor readings
                    int proximityReading = Integer.parseInt(proximityField.getText());
                    int colorReading = Integer.parseInt(colorField.getText());
                    int metalDetectionReading = Integer.parseInt(metalDetectionField.getText());

                    // Segregate waste 
                    WasteType segregatedWaste = WasteSegregation.segregateWaste(proximityReading, colorReading,
                            metalDetectionReading);

                    // Display segregated waste 
                    resultTextField.setText("Result: " + segregatedWaste.toString());

                    // Store the readings and result in the database
                    storeDataInDatabase(proximityReading, colorReading, metalDetectionReading, segregatedWaste);

                    statusLabel.setText("Status: Segregation Completed");
                } catch (NumberFormatException ex) {
                    resultTextField.setText("Result: Error");
                    statusLabel.setText("Status: Error");
                }
            }
        });
    }

    private void storeDataInDatabase (int proximityReading, int colorReading, int metalDetectionReading, WasteType segregatedWaste) {
        String jdbcUrlstoreDataInDatabase = "jdbc:mysql://localhost:3306/wastesegregator";
        String username = "root";
        String password = "jeffjudev";

        try (Connection connection = DriverManager.getConnection(jdbcUrlstoreDataInDatabase, username, password)) {
            String insertQuery = "INSERT INTO logs (proximity_reading, color_reading, metal_detection_reading, waste_type) VALUES (?, ?, ?, ?)";
            try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
                preparedStatement.setInt(1, proximityReading);
                preparedStatement.setInt(2, colorReading);
                preparedStatement.setInt(3, metalDetectionReading);
                preparedStatement.setString(4, segregatedWaste.toString());
                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

// Main class
public class MainApp {
    public static void main(String[] args) {

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new WasteSegregatorUI().setVisible(true);
            }
        });
    }
}