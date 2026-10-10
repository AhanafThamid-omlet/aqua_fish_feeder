package aqua_feeder;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.io.File;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class Aqua_feeder extends JFrame{
    private JLabel clockLabel;
    private JComboBox<String> foodTypeBox;
    private JSpinner amountSpinner;
    private JTextField feedTimeField;
    private JTextField setAmountField;
    private DefaultListModel<String> scheduleModel;
    private JList<String> scheduleList;
    private JTextArea logArea;
    private JLabel weightLabel;
    private static final String DATA_FILE = "feeder_data.properties";

    private List<String> feedTimes = new ArrayList<>(); // stored as "HH:mm"
    private double currentFoodWeight = 50.0; // grams left in container (simulated)
    
    public Aqua_feeder() {    
        setTitle("Automatic Aquarium Fish Feeder");
        
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        //Clock 
        clockLabel = new JLabel("", SwingConstants.CENTER);
        clockLabel.setFont(new Font("TimesNewRoman", Font.BOLD, 34));
        clockLabel.setForeground(new Color(0,150, 0));
        add(clockLabel, BorderLayout.NORTH);

        //Schedule setup 
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new GridLayout(1, 2, 10, 10));

        //Add feeding schedule
        JPanel schedulePanel = new JPanel();
        schedulePanel.setLayout(new BoxLayout(schedulePanel, BoxLayout.Y_AXIS));
        schedulePanel.setBorder(BorderFactory.createTitledBorder("Feeding Schedule"));

        JPanel addPanel = new JPanel();
        feedTimeField = new JTextField("00:00", 6);
        JButton addTimeBtn = new JButton("Add Time");
        addPanel.add(new JLabel("HH:mm:"));
        addPanel.add(feedTimeField);
        addPanel.add(addTimeBtn);

        scheduleModel = new DefaultListModel<>();
        scheduleList = new JList<>(scheduleModel);
        JButton removeTimeBtn = new JButton("Remove Selected");

        schedulePanel.add(addPanel);
        schedulePanel.add(new JScrollPane(scheduleList));
        schedulePanel.add(removeTimeBtn);

        //Food settings
        JPanel foodPanel = new JPanel();
        foodPanel.setLayout(new BoxLayout(foodPanel, BoxLayout.Y_AXIS));
        foodPanel.setBorder(BorderFactory.createTitledBorder("Food Settings"));

        foodTypeBox = new JComboBox<>(new String[]{"Flakes", "Pellets", "Granules"});
        amountSpinner = new JSpinner(new SpinnerNumberModel(5.0, 1.0, currentFoodWeight, 1.0)); // grams per feed
        weightLabel = new JLabel("Food remaining: " + currentFoodWeight + " g");
        
        

        foodPanel.add(new JLabel("Kind of food:"));
        foodPanel.add(foodTypeBox);
        foodPanel.add(Box.createVerticalStrut(10));
        foodPanel.add(new JLabel("Amount per feed (g):"));
        foodPanel.add(amountSpinner);
        foodPanel.add(Box.createVerticalStrut(10));
        foodPanel.add(weightLabel);
        
        JPanel setAmountPanel = new JPanel();
        setAmountField = new JTextField(6);
        JButton setAmountBtn = new JButton("Add Food");
        setAmountPanel.add(new JLabel("Add food (g):"));
        setAmountPanel.add(setAmountField);
        setAmountPanel.add(setAmountBtn);

        foodPanel.add(Box.createVerticalStrut(10));
        foodPanel.add(setAmountPanel);

        centerPanel.add(schedulePanel);
        centerPanel.add(foodPanel);
        add(centerPanel, BorderLayout.CENTER);

        //Log
        logArea = new JTextArea(8, 50);
        logArea.setEditable(false);
        add(new JScrollPane(logArea), BorderLayout.SOUTH);

        //Button actions
        
        setAmountBtn.addActionListener(e -> {
            try {
                double addAmount = Double.parseDouble(setAmountField.getText().trim());
                if (addAmount <= 0) {
                JOptionPane.showMessageDialog(this, "Enter an amount greater than 0.");
                return;
                }
                currentFoodWeight += addAmount; // 50 + 40 = 90
                weightLabel.setText("Food remaining: " + currentFoodWeight + " g");
                updateSpinnerMax();
                log("Added " + addAmount + " g of food. New total: " + currentFoodWeight + " g");
                saveData();
                setAmountField.setText("");
            } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter a valid number (e.g. 40).");
            }
        });
        
        addTimeBtn.addActionListener(e -> {
            String time = feedTimeField.getText().trim();
            if (time.matches("([01]\\d|2[0-3]):[0-5]\\d")) {
                feedTimes.add(time);
                scheduleModel.addElement(time);
                log("Scheduled feeding time added: " + time);
            } else {
                JOptionPane.showMessageDialog(this, "Enter time as HH:mm (e.g. 08:00)");
            }
        });

        removeTimeBtn.addActionListener(e -> {
            int idx = scheduleList.getSelectedIndex();
            if (idx != -1) {
                String removed = scheduleModel.get(idx);
                feedTimes.remove(removed);
                scheduleModel.remove(idx);
                log("Removed feeding time: " + removed);
            }
        });

        //Timer_checks clock every second (Timer + Clock + Sensors logic)
        javax.swing.Timer masterTimer = new javax.swing.Timer(1000, e -> tick());
        masterTimer.start();
        loadData(); 
        
        
        // Reflect loaded data in the UI
        weightLabel.setText("Food remaining: " + currentFoodWeight + " g");
        updateSpinnerMax();
        for (String t : feedTimes) {
            scheduleModel.addElement(t);
        }

        log("Fish feeder system started. Waiting for scheduled times...");
    }
    
    private void loadData() {
        Properties props = new Properties();
        File file = new File(DATA_FILE);

        if (!file.exists()) {
        // First time ever running — ask for starting amount
            String input = JOptionPane.showInputDialog(this,
                "No saved data found.\nEnter starting food amount (g):","Initial Setup", JOptionPane.QUESTION_MESSAGE);
            try {
                currentFoodWeight = Double.parseDouble(input.trim());
            } catch (Exception e) {
                currentFoodWeight = 50.0; // fallback default
            }
            return; // nothing else to load
        }

        try (FileInputStream in = new FileInputStream(file)) {
            props.load(in);

            String weightStr = props.getProperty("foodWeight");
            if (weightStr != null) {
            currentFoodWeight = Double.parseDouble(weightStr);
            }

            String times = props.getProperty("feedTimes");
            if (times != null && !times.isEmpty()) {
                for (String t : times.split(",")) {
                feedTimes.add(t);
                }
            }

            log("Loaded saved data: " + currentFoodWeight + "g remaining, "+ feedTimes.size() + " scheduled time(s).");

        } 
        catch (IOException e) {
            log("Error loading saved data.");
        }
    }
    
    private void saveData() {
        Properties props = new Properties();
        props.setProperty("foodWeight", String.valueOf(currentFoodWeight));
        props.setProperty("feedTimes", String.join(",", feedTimes));

        try (FileOutputStream out = new FileOutputStream(DATA_FILE)) {
            props.store(out, "Aqua Feeder Saved Data");
        } catch (IOException e) {
            log("ERROR: Could not save data - " + e.getMessage());
        }
    }
    
    private void updateSpinnerMax() {
        SpinnerNumberModel model = (SpinnerNumberModel) amountSpinner.getModel();
        model.setMaximum(currentFoodWeight);

        double currentValue = ((Number) amountSpinner.getValue()).doubleValue();
        if (currentValue > currentFoodWeight) {
            amountSpinner.setValue(currentFoodWeight);
        }
    }

    private void tick(){
        SimpleDateFormat clockFormat = new SimpleDateFormat("hh:mm:ss a");
        SimpleDateFormat matchFormat = new SimpleDateFormat("HH:mm");
        Date now = new Date();

        clockLabel.setText(clockFormat.format(now));

        String currentMinute = matchFormat.format(now);
        if (now.getSeconds() == 0 && feedTimes.contains(currentMinute)) {
            feedFish();
        }
    }

    private void feedFish(){
        double amount = ((Number) amountSpinner.getValue()).doubleValue();
        String food = (String) foodTypeBox.getSelectedItem();

        if (currentFoodWeight < amount) {
            log("WARNING: Not enough food left! (" + currentFoodWeight + "g remaining) Feed skipped.");
            return;
        }

        currentFoodWeight -= amount;
        weightLabel.setText("Food remaining: " + currentFoodWeight + " g");
        updateSpinnerMax();
        log(String.format("Fed fish: %.1f g of %s | Remaining: %.1f g", amount, food, currentFoodWeight));

        if (currentFoodWeight < 20) {
            log("ALERT: Food container is low! Please refill.");
        }
    }

    private void log(String message) {
        SimpleDateFormat ts = new SimpleDateFormat("HH:mm:ss");
        logArea.append("[" + ts.format(new Date()) + "] " + message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Aqua_feeder feeder = new Aqua_feeder();
            feeder.setVisible(true);
        });
    }
    
}
