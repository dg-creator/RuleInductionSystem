import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

class RuleStat {
    String ruleText;
    int length;
    int support;

    RuleStat(String ruleText, int length, int support) {
        this.ruleText = ruleText;
        this.length = length;
        this.support = support;
    }
}


public class RuleInductionGUI extends JFrame {
    private JTabbedPane tabbedPane;
    private Map<String, JTable> tables;
    private JButton loadButton, processButton, optimizeButton, downloadButton;
    private JLabel statusLabel;
    private List<File> selectedFiles;
    private Map<String, List<String>> generatedRules;

    public RuleInductionGUI() {
        UIManager.put("Button.focus", new Color(100, 149, 237)); // focus border color

        setTitle("Rule Induction System");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        tables = new LinkedHashMap<>();
        selectedFiles = new ArrayList<>();
        generatedRules = new LinkedHashMap<>();

        tabbedPane = new JTabbedPane();

        Color buttonColor = new Color(70, 130, 180);
        Color buttonTextColor = Color.BLACK;

        loadButton = new JButton("Load CSV Files");
        loadButton.putClientProperty("JButton.buttonType", "filled");
        loadButton.setBackground(buttonColor);
        loadButton.setForeground(buttonTextColor);

        processButton = new JButton("️Generate Rules");
        processButton.putClientProperty("JButton.buttonType", "filled");
        processButton.setBackground(buttonColor);
        processButton.setForeground(buttonTextColor);

        optimizeButton = new JButton("Optimize Rules");
        optimizeButton.putClientProperty("JButton.buttonType", "filled");
        optimizeButton.setBackground(buttonColor);
        optimizeButton.setForeground(buttonTextColor);

        downloadButton = new JButton("Download Data");
        downloadButton.putClientProperty("JButton.buttonType", "filled");
        downloadButton.setBackground(buttonColor);
        downloadButton.setForeground(buttonTextColor);

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
        controlPanel.setBackground(new Color(230, 230, 250));
        controlPanel.add(loadButton);
        controlPanel.add(processButton);
        controlPanel.add(optimizeButton);
        controlPanel.add(downloadButton);

        statusLabel = new JLabel("Select CSV files to start", SwingConstants.CENTER);
        statusLabel.setOpaque(true);
        statusLabel.setBackground(new Color(255, 250, 240)); // FloralWhite
        statusLabel.setForeground(Color.DARK_GRAY);

        getContentPane().setBackground(new Color(245, 245, 255)); // light lavender

        tabbedPane.setBackground(new Color(240, 248, 255)); // AliceBlue

        add(controlPanel, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        loadButton.addActionListener(e -> loadCSVFiles());
        processButton.addActionListener(e -> processAllTables());
        optimizeButton.addActionListener(e -> optimizeRules());
        downloadButton.addActionListener(e -> downloadStatistics());
    }

    private boolean matchesCondition(List<Object> row, JTable table, String conditionPart) {
        String[] conditions = conditionPart.split("and");
        for (String cond : conditions) {
            String[] parts = cond.trim().split("=");
            if (parts.length != 2) return false;

            String colName = parts[0].trim();
            String expectedValue = parts[1].trim();

            int colIndex = table.getColumnModel().getColumnIndex(colName);
            String actualValue = row.get(colIndex).toString().trim();

            if (!actualValue.equals(expectedValue)) return false;
        }
        return true;
    }

    private int calculateSupport(String rule) {
        String conditionPart = rule.split("->")[0].split(":")[1].trim(); // f1=1 and f2=0 ...
        if (conditionPart.equals("<no rule>")) return 0;

        String[] conditions = conditionPart.split("and");

        int supportCount = 0;
        for (JTable table : tables.values()) {
            List<List<Object>> data = getTableData(table);
            for (List<Object> row : data) {
                boolean match = true;
                for (String cond : conditions) {
                    cond = cond.trim();
                    String[] parts = cond.split("=");
                    String colName = parts[0].trim();
                    String expectedVal = parts[1].trim();

                    int colIndex = table.getColumnModel().getColumnIndex(colName);
                    String actualVal = row.get(colIndex).toString();

                    if (!actualVal.equals(expectedVal)) {
                        match = false;
                        break;
                    }
                }
                if (match) supportCount++;
            }
        }
        return supportCount;
    }


    private void optimizeRules() {
        Set<String> uniqueRules = new HashSet<>();
        for (List<String> rules : generatedRules.values()) {
            uniqueRules.addAll(rules);
        }

        int minLength = uniqueRules.stream()
                .mapToInt(rule -> rule.split("->")[0].split("and").length)
                .min()
                .orElse(0);

        List<String> minimalRules = uniqueRules.stream()
                .filter(rule -> rule.split("->")[0].split("and").length == minLength)
                .collect(Collectors.toList());

        List<RuleStat> ruleStats = new ArrayList<>();
        for (String rule : minimalRules) {
            int length = rule.split("->")[0].split("and").length;
            int support = calculateSupport(rule);
            ruleStats.add(new RuleStat(rule, length, support));
        }

        saveOptimizedRules(ruleStats);
        showRuleStatisticsPanel(ruleStats);
    }

    private void saveOptimizedRules(List<RuleStat> rules) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("optimized_rules.csv"))) {
            writer.write("Rule,Length,Support\n");
            for (RuleStat rule : rules) {
                String cleanRule = rule.ruleText.contains(":") ? rule.ruleText.split(":", 2)[1].trim() : rule.ruleText;
                writer.write(String.format("%s,%d,%d\n", cleanRule, rule.length, rule.support));
            }

            statusLabel.setText("Optimized rules saved to optimized_rules.csv.");
        } catch (IOException e) {
            statusLabel.setText("Error saving optimized rules to CSV.");
        }
    }

    private void loadCSVFiles() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedFiles.clear();
            tabbedPane.removeAll();
            tables.clear();

            for (File file : fileChooser.getSelectedFiles()) {
                JTable table = loadCSV(file);
                if (table != null) {
                    tables.put(file.getName(), table);
                    tabbedPane.add(file.getName(), new JScrollPane(table));
                }
            }

            if (!tables.isEmpty()) {
                processButton.setEnabled(true);
                statusLabel.setText("Files loaded. Click 'Generate Rules' to proceed.");
            }
        }
    }


    private JTable loadCSV(File file) {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            DefaultTableModel tableModel = new DefaultTableModel();
            JTable table = new JTable(tableModel);
            String line = br.readLine();
            if (line != null) {
                String[] headers = line.split(",");
                tableModel.setColumnIdentifiers(headers);
                while ((line = br.readLine()) != null) {
                    tableModel.addRow(line.split(","));
                }
            }
            return table;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error reading file: " + file.getName(), "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private void processAllTables() {
        generatedRules.clear();
        for (Map.Entry<String, JTable> entry : tables.entrySet()) {
            String tableName = entry.getKey();
            JTable table = entry.getValue();
            List<String> rules = generateRulesForTable(table);
            generatedRules.put(tableName, rules);
        }
        displayGeneratedRules();
        optimizeButton.setEnabled(true);
        statusLabel.setText("Rules generated. Click 'Optimize Rules' to minimize.");
    }

    private List<String> generateRulesForTable(JTable table) {
        List<String> rules = new ArrayList<>();
        int rowCount = table.getRowCount();
        int colCount = table.getColumnCount();
        for (int i = 0; i < rowCount; i++) {
            rules.add(generateRuleForRow(table, i, colCount));
        }
        return rules;
    }

    private String generateRuleForRow(JTable table, int rowIndex, int colCount) {
        List<List<Object>> originalData = getTableData(table);
        List<List<Object>> currentData = new ArrayList<>(originalData);
        Set<String> conditions = new LinkedHashSet<>();

        Object targetDecision = originalData.get(rowIndex).get(colCount - 1);

        final int featureStart = 1;
        final int decisionIndex = colCount - 1;

        while (!allRowsHaveSameDecision(currentData)) {
            int bestAttrIndex = -1;
            double minRM = Double.MAX_VALUE;

            for (int attrIndex = featureStart; attrIndex < decisionIndex; attrIndex++) {
                Object value = originalData.get(rowIndex).get(attrIndex);
                List<List<Object>> filtered = filterTable(currentData, attrIndex, value);

                if (filtered.isEmpty()) continue;

                long total = filtered.size();
                long consistent = filtered.stream()
                        .filter(row -> Objects.equals(row.get(decisionIndex), targetDecision))
                        .count();

                double rm = (double) (total - consistent) / total;

                if (rm < minRM || (rm == minRM && (bestAttrIndex == -1 || attrIndex < bestAttrIndex))) {
                    minRM = rm;
                    bestAttrIndex = attrIndex;
                }
            }

            if (bestAttrIndex == -1) break;

            Object value = originalData.get(rowIndex).get(bestAttrIndex);
            String condition = table.getColumnName(bestAttrIndex) + "=" + value;
            conditions.add(condition);
            currentData = filterTable(currentData, bestAttrIndex, value);
        }

        if (conditions.isEmpty()) {
            return String.format("r%d: <no rule> -> %s", rowIndex + 1, targetDecision);
        }

        return String.format("r%d: %s -> %s",
                rowIndex + 1,
                String.join(" and ", conditions),
                targetDecision.toString());
    }

    private void showRuleStatisticsPanel(List<RuleStat> rules) {
        int totalRules = rules.size();
        int totalLength = rules.stream().mapToInt(r -> r.length).sum();
        int totalSupport = rules.stream().mapToInt(r -> r.support).sum();
        int maxSupport = rules.stream().mapToInt(r -> r.support).max().orElse(0);
        double avgLength = totalRules > 0 ? (double) totalLength / totalRules : 0;
        double avgSupport = totalRules > 0 ? (double) totalSupport / totalRules : 0;

        String[] columnNames = {"Rule", "Rule Length", "Rule Support"};
        Object[][] data = new Object[totalRules][3];
        for (int i = 0; i < totalRules; i++) {
            RuleStat r = rules.get(i);
            data[i][0] = r.ruleText;
            data[i][1] = r.length;
            data[i][2] = r.support;
        }

        JTable ruleTable = new JTable(data, columnNames);
        ruleTable.setAutoCreateRowSorter(true);
        ruleTable.setFont(new Font("SansSerif", Font.PLAIN, 13));
        ruleTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 14));
        ruleTable.setRowHeight(26);
        JScrollPane scrollPane = new JScrollPane(ruleTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Rule Set"));

        JPanel statsPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        statsPanel.setBorder(BorderFactory.createTitledBorder("Statistics"));
        statsPanel.add(new JLabel("Number of rules: " + totalRules));
        statsPanel.add(new JLabel("Max rule support: " + maxSupport));
        statsPanel.add(new JLabel(String.format("Average rule length: %.1f", avgLength)));
        statsPanel.add(new JLabel(String.format("Average rule support: %.1f", avgSupport)));

        for (Component c : statsPanel.getComponents()) {
            if (c instanceof JLabel lbl) {
                lbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
            }
        }

        JButton saveBtn = new JButton("Save results to .txt");
        saveBtn.setBackground(new Color(70, 130, 180));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setFocusPainted(false);
        saveBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        saveBtn.addActionListener(e -> saveResultsToTextFile(rules, "results_algorithm_rm.txt"));

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(new Color(245, 245, 255));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(statsPanel, BorderLayout.EAST);
        mainPanel.add(saveBtn, BorderLayout.SOUTH);

        JDialog dialog = new JDialog(this, "Rule Summary", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.getContentPane().add(mainPanel);
        dialog.setSize(900, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void saveResultsToTextFile(List<RuleStat> rules, String filename) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write("Results for Algorithm RM\n");
            writer.write("Rules:\n");

            int totalLength = 0;
            int totalSupport = 0;
            int maxSupport = 0;

            for (RuleStat rule : rules) {
                writer.write(String.format("%s;length:%d;support:%d\n",
                        rule.ruleText, rule.length, rule.support));
                totalLength += rule.length;
                totalSupport += rule.support;
                maxSupport = Math.max(maxSupport, rule.support);
            }

            int ruleCount = rules.size();
            double avgLength = ruleCount > 0 ? (double) totalLength / ruleCount : 0;
            double avgSupport = ruleCount > 0 ? (double) totalSupport / ruleCount : 0;

            writer.write("\n");
            writer.write("Number of rules: " + ruleCount + "\n");
            writer.write("Max rule support: " + maxSupport + "\n");
            writer.write(String.format("Average rule length: %.1f\n", avgLength));
            writer.write(String.format("Average rule support: %.1f\n", avgSupport));

            statusLabel.setText("Saved to " + filename);
        } catch (IOException e) {
            statusLabel.setText("Failed to save to " + filename);
        }
    }

    private boolean allRowsHaveSameDecision(List<List<Object>> tableData) {
        return tableData.stream().map(row -> row.get(row.size() - 1)).distinct().count() == 1;
    }

    private List<List<Object>> getTableData(JTable table) {
        List<List<Object>> data = new ArrayList<>();
        DefaultTableModel model = (DefaultTableModel) table.getModel();

        for (int i = 0; i < model.getRowCount(); i++) {
            List<Object> row = new ArrayList<>();
            for (int j = 0; j < model.getColumnCount(); j++) {
                row.add(model.getValueAt(i, j));
            }
            data.add(row);
        }
        return data;
    }


    private List<List<Object>> filterTable(List<List<Object>> tableData, int attrIndex, Object value) {
        return tableData.stream()
                .filter(row -> row.get(attrIndex).equals(value))
                .collect(Collectors.toList());
    }


    private int selectBestAttribute(List<List<Object>> tableData, int decisionIndex) {
        int bestAttribute = -1;
        double bestScore = Double.MAX_VALUE;

        for (int attrIndex = 0; attrIndex < decisionIndex; attrIndex++) {
            final int currentAttrIndex = attrIndex;

            Map<Object, Long> valueCounts = tableData.stream()
                    .collect(Collectors.groupingBy(row -> row.get(currentAttrIndex), Collectors.counting()));

            double rmScore = 0.0;
            for (Map.Entry<Object, Long> entry : valueCounts.entrySet()) {
                Object attrValue = entry.getKey();
                List<List<Object>> filteredTable = filterTable(tableData, currentAttrIndex, attrValue);

                long distinctDecisions = filteredTable.stream()
                        .map(row -> row.get(decisionIndex))
                        .distinct()
                        .count();

                rmScore += (double) distinctDecisions / tableData.size();
            }

            if (rmScore < bestScore) {
                bestScore = rmScore;
                bestAttribute = attrIndex;
            }
        }
        return bestAttribute;
    }

    private void displayGeneratedRules() {
        StringBuilder message = new StringBuilder();

        for (Map.Entry<String, List<String>> entry : generatedRules.entrySet()) {
            String tableName = entry.getKey();
            List<String> rules = entry.getValue();

            message.append("📄 Table: ").append(tableName).append("\n");
            message.append("──────────────────────────────\n");
            for (String rule : rules) {
                message.append(rule).append("\n");
            }
            message.append("\n");
        }

        JTextArea textArea = new JTextArea(message.toString());
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(800, 500));

        JOptionPane.showMessageDialog(this, scrollPane, "Generated Rules", JOptionPane.INFORMATION_MESSAGE);
    }


    private void generateStatistics() {
        Set<String> uniqueRules = new HashSet<>();

        for (List<String> ruleList : generatedRules.values()) {
            uniqueRules.addAll(ruleList);
        }
        int numberOfUniqueRules = uniqueRules.size();

        List<Integer> ruleLengths = uniqueRules.stream()
                .map(rule -> rule.split("and").length)
                .collect(Collectors.toList());

        int minLength = ruleLengths.stream().min(Integer::compareTo).orElse(0);
        int maxLength = ruleLengths.stream().max(Integer::compareTo).orElse(0);
        double avgLength = ruleLengths.stream().mapToInt(Integer::intValue).average().orElse(0);

        saveStatisticsToCSV(numberOfUniqueRules, minLength, avgLength, maxLength, uniqueRules);
    }

    private void saveStatisticsToCSV(int numberOfUniqueRules, int minLength, double avgLength, int maxLength, Set<String> uniqueRules) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("statystyki.csv"))) {
            writer.write("Number of unique rules,Minimum length,Average length,Maximum length\n");

            writer.write(String.format("%d,%d,%.2f,%d\n", numberOfUniqueRules, minLength, avgLength, maxLength));

            writer.write("\nRow,Induced Rule\n");
            int rowNum = 1;
            for (String rule : uniqueRules) {
                writer.write(String.format(rule));
            }

            statusLabel.setText("Statistics saved to statystyki.csv.");
        } catch (IOException e) {
            statusLabel.setText("Error saving statistics to CSV.");
        }
    }

    private void downloadStatistics() {
        generateStatistics();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RuleInductionGUI().setVisible(true));
    }
}
