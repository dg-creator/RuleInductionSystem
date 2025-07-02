import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RuleInductionGUITest {
    private RuleInductionGUI gui;

    @BeforeEach
    void setUp() {
        // Initialize the GUI instance without showing the window
        gui = new RuleInductionGUI();
    }

    /**
     * Utility to create a JTable from header and 2D data array.
     */
    private JTable createTable(String[] columns, Object[][] data) {
        DefaultTableModel model = new DefaultTableModel();
        model.setColumnIdentifiers(columns);
        for (Object[] row : data) {
            model.addRow(row);
        }
        return new JTable(model);
    }

    @Test
    void testCalculateSupport() throws Exception {
        // Prepare a simple table with a decision column
        String[] columns = {"f1", "f2", "d"};
        Object[][] data = {
                {"1", "X", "A"},
                {"1", "Y", "A"},
                {"2", "X", "B"},
                {"1", "X", "A"}
        };
        JTable table = createTable(columns, data);
        // inject into gui.tables map
        gui.tables.clear();
        gui.tables.put("test", table);

        // Rule: f1=1 and f2=X -> d = A
        String rule = "r1: f1=1 and f2=X -> d = A";
        Method calcSup = RuleInductionGUI.class.getDeclaredMethod("calculateSupport", String.class);
        calcSup.setAccessible(true);
        int support = (int) calcSup.invoke(gui, rule);
        // Rows matching f1=1 & f2=X: indices 0 and 3 -> support 2
        assertEquals(2, support, "Support should count matching rows across all tables");
    }

    @Test
    void testGenerateRuleForRow() throws Exception {
        // Prepare table where decision is determined by f1 only
        String[] columns = {"ID", "f1", "d"};
        Object[][] data = {
                {"r1", "X", "Yes"},
                {"r2", "X", "Yes"},
                {"r3", "Y", "No"}
        };
        JTable table = createTable(columns, data);

        Method gen = RuleInductionGUI.class.getDeclaredMethod("generateRuleForRow", JTable.class, int.class, int.class);
        gen.setAccessible(true);
        // row 0 should produce rule using f1=X
        String rule = (String) gen.invoke(gui, table, 0, table.getColumnCount());
        assertTrue(rule.contains("f1=X"), "Rule should include the f1=X condition");
        assertTrue(rule.endsWith("-> d = Yes"), "Rule should predict the correct decision");
    }

    @Test
    void testOptimizeRulesPicksShortest() throws Exception {
        // Simulate generatedRules with two rules for same row: one shorter
        gui.generatedRules.clear();
        gui.generatedRules.put("file1.csv", List.of(
                "r1: f1=1 and f2=0 -> d = A",
                "r1: f1=1 -> d = A"
        ));
        // Run optimize
        Method opt = RuleInductionGUI.class.getDeclaredMethod("optimizeRules");
        opt.setAccessible(true);
        opt.invoke(gui);

        // After optimization, lastOptimizedRules should contain only the shorter rule
        List<RuleStat> optimized = gui.lastOptimizedRules;
        assertEquals(1, optimized.size(), "Should keep only one optimized rule per row");
        assertTrue(optimized.get(0).ruleText.contains("f1=1 -> d = A"), "Should select the rule with minimal conditions");
    }
}

