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

public class Aqua_feeder extends JFrame{
    private JLabel clockLabel;
    private JComboBox<String> foodTypeBox;
    private JSpinner amountSpinner;
    private JTextField feedTimeField;
    private DefaultListModel<String> scheduleModel;
    private JList<String> scheduleList;
    private JTextArea logArea;
    private JLabel weightLabel;

    private List<String> feedTimes = new ArrayList<>(); // stored as "HH:mm"
    private double currentFoodWeight = 500.0; // grams left in container (simulated)

    public Aqua_feeder() {
        setTitle("Automatic Aquarium Fish Feeder");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        //Clock 
        clockLabel = new JLabel("", SwingConstants.CENTER);
        clockLabel.setFont(new Font("TimesNewRoman", Font.BOLD, 40));
        clockLabel.setForeground(new Color(0, 150, 0));
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
        amountSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 50, 1)); // grams per feed
        weightLabel = new JLabel("Food remaining: " + currentFoodWeight + " g");

        foodPanel.add(new JLabel("Kind of food:"));
        foodPanel.add(foodTypeBox);
        foodPanel.add(Box.createVerticalStrut(10));
        foodPanel.add(new JLabel("Amount per feed (g):"));
        foodPanel.add(amountSpinner);
        foodPanel.add(Box.createVerticalStrut(10));
        foodPanel.add(weightLabel);

        centerPanel.add(schedulePanel);
        centerPanel.add(foodPanel);
        add(centerPanel, BorderLayout.CENTER);

        //Log
        logArea = new JTextArea(8, 50);
        logArea.setEditable(false);
        add(new JScrollPane(logArea), BorderLayout.SOUTH);

        //Button actions
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

        log("Fish feeder system started. Waiting for scheduled times...");
    }

    private void tick(){
        SimpleDateFormat clockFormat = new SimpleDateFormat("HH:mm:ss");
        SimpleDateFormat matchFormat = new SimpleDateFormat("HH:mm");
        Date now = new Date();

        clockLabel.setText(clockFormat.format(now));

        String currentMinute = matchFormat.format(now);
        if (now.getSeconds() == 0 && feedTimes.contains(currentMinute)) {
            feedFish();
        }
    }

    private void feedFish(){
        int amount = (Integer) amountSpinner.getValue();
        String food = (String) foodTypeBox.getSelectedItem();

        if (currentFoodWeight < amount) {
            log("WARNING: Not enough food left! (" + currentFoodWeight + "g remaining) Feed skipped.");
            return;
        }

        currentFoodWeight -= amount;
        weightLabel.setText("Food remaining: " + currentFoodWeight + " g");

        log(String.format("Fed fish: %d g of %s | Remaining: %.1f g", amount, food, currentFoodWeight));

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
