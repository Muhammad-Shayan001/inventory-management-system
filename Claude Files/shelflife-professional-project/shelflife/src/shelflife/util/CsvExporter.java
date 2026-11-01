package shelflife.util;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.Component;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * Utility for exporting tables and reports to Microsoft Excel compatible CSV files.
 * Emits UTF-8 Byte Order Mark (BOM: \uFEFF) to guarantee Excel opens reports with
 * correct encoding for currency symbols and international characters.
 */
public final class CsvExporter {

    private CsvExporter() {}

    /**
     * Exports a JTable to a CSV file.
     */
    public static void exportTable(JTable table, File targetFile) throws Exception {
        if (table == null || targetFile == null) {
            throw new IllegalArgumentException("Table and target file cannot be null.");
        }

        TableModel model = table.getModel();
        int colCount = model.getColumnCount();
        int rowCount = model.getRowCount();

        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(targetFile), StandardCharsets.UTF_8))) {
            // Write UTF-8 BOM for Microsoft Excel compatibility
            pw.write('\uFEFF');

            // Header row
            for (int col = 0; col < colCount; col++) {
                String header = model.getColumnName(col);
                pw.print(escapeCsv(header));
                if (col < colCount - 1) pw.print(",");
            }
            pw.println();

            // Data rows
            for (int row = 0; row < rowCount; row++) {
                for (int col = 0; col < colCount; col++) {
                    Object val = model.getValueAt(row, col);
                    pw.print(escapeCsv(val != null ? val.toString() : ""));
                    if (col < colCount - 1) pw.print(",");
                }
                pw.println();
            }
            pw.flush();
        }
    }

    /**
     * Prompts the user with a file chooser and exports the given JTable.
     */
    public static void exportWithFileChooser(Component parent, JTable table, String defaultFileName) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(defaultFileName.endsWith(".csv") ? defaultFileName : defaultFileName + ".csv"));
        int res = chooser.showSaveDialog(parent);
        if (res == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                exportTable(table, file);
                JOptionPane.showMessageDialog(parent,
                        "Report exported successfully to:\n" + file.getAbsolutePath(),
                        "Export Successful",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(parent,
                        "Failed to export report: " + ex.getMessage(),
                        "Export Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static String escapeCsv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n") || val.contains("\r")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }
}
