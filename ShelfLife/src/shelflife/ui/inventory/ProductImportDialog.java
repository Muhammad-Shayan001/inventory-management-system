package shelflife.ui.inventory;

import shelflife.model.Product;
import shelflife.service.InventoryService;
import shelflife.ui.ToastNotification;
import shelflife.ui.theme.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * CSV Product Batch Import Dialog with file preview, schema validation,
 * error reporting, and duplicate conflict handling.
 */
public class ProductImportDialog extends JDialog {

    private final InventoryService inventoryService;
    private final DefaultTableModel tableModel;
    private final JTable previewTable;
    private final JLabel lblSummary;
    private final JRadioButton radSkip;
    private final JRadioButton radOverwrite;
    private final JButton btnImport;

    private final List<Product> validProducts = new ArrayList<>();
    private final List<String> errorMessages = new ArrayList<>();

    public ProductImportDialog(Frame parent, InventoryService inventoryService) {
        super(parent, "Import Products from CSV", true);
        this.inventoryService = inventoryService;

        setSize(780, 520);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(12, 12));

        // Top Banner
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(Theme.BG_WINDOW);
        topPanel.setBorder(new EmptyBorder(16, 20, 12, 20));

        JLabel lblTitle = new JLabel("CSV Product Catalog Import");
        lblTitle.setFont(Theme.FONT_PAGE_TITLE);
        lblTitle.setForeground(Theme.TEXT_PRIMARY);

        JLabel lblSub = new JLabel("Select a CSV file containing columns: productId, name, category, sellingPrice, lowStockThreshold, barcode, sku");
        lblSub.setFont(Theme.FONT_SUBTITLE);
        lblSub.setForeground(Theme.TEXT_SECONDARY);

        topPanel.add(lblTitle, BorderLayout.NORTH);
        topPanel.add(lblSub, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        // Center: Preview Table
        String[] cols = {"Product ID", "Name", "Category", "Selling Price", "Threshold", "Barcode", "SKU", "Status"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        previewTable = new JTable(tableModel);
        previewTable.setRowHeight(Theme.TABLE_ROW_HEIGHT);
        previewTable.setFont(Theme.FONT_TABLE_CELL);
        previewTable.getTableHeader().setFont(Theme.FONT_TABLE_HEADER);

        JScrollPane scrollPane = new JScrollPane(previewTable);
        scrollPane.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Theme.BORDER_LIGHT));
        add(scrollPane, BorderLayout.CENTER);

        // Bottom: File chooser button, conflict options, summary label, and Import button
        JPanel bottomPanel = new JPanel(new BorderLayout(12, 10));
        bottomPanel.setBorder(new EmptyBorder(12, 20, 16, 20));

        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        optionsPanel.setOpaque(false);

        JButton btnChoose = new JButton("Select CSV File...");
        btnChoose.setFont(Theme.FONT_BOLD);
        btnChoose.addActionListener(e -> chooseAndParseFile());

        JLabel lblConflict = new JLabel("On Duplicate ID:");
        lblConflict.setFont(Theme.FONT_BOLD);

        radSkip = new JRadioButton("Skip Duplicate", true);
        radOverwrite = new JRadioButton("Overwrite Existing", false);
        ButtonGroup bg = new ButtonGroup();
        bg.add(radSkip);
        bg.add(radOverwrite);

        optionsPanel.add(btnChoose);
        optionsPanel.add(Box.createHorizontalStrut(15));
        optionsPanel.add(lblConflict);
        optionsPanel.add(radSkip);
        optionsPanel.add(radOverwrite);

        lblSummary = new JLabel("No file selected. Click 'Select CSV File' to begin.");
        lblSummary.setFont(Theme.FONT_SUBTITLE);
        lblSummary.setForeground(Theme.TEXT_HINT);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        JButton btnCancel = new JButton("Cancel");
        btnCancel.setFont(Theme.FONT_REGULAR);
        btnCancel.addActionListener(e -> dispose());

        btnImport = new JButton("Import Valid Records");
        btnImport.setFont(Theme.FONT_BOLD);
        btnImport.setBackground(Theme.ACCENT_PRIMARY);
        btnImport.setForeground(Color.WHITE);
        btnImport.setEnabled(false);
        btnImport.addActionListener(e -> executeImport());

        btnPanel.add(btnCancel);
        btnPanel.add(btnImport);

        JPanel leftBottom = new JPanel(new BorderLayout(0, 6));
        leftBottom.setOpaque(false);
        leftBottom.add(optionsPanel, BorderLayout.NORTH);
        leftBottom.add(lblSummary, BorderLayout.SOUTH);

        bottomPanel.add(leftBottom, BorderLayout.CENTER);
        bottomPanel.add(btnPanel, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void chooseAndParseFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Product Catalog CSV");
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            parseCsvFile(chooser.getSelectedFile());
        }
    }

    private void parseCsvFile(File file) {
        tableModel.setRowCount(0);
        validProducts.clear();
        errorMessages.clear();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            int lineNum = 0;
            while ((line = br.readLine()) != null) {
                lineNum++;
                line = line.trim();
                if (line.isEmpty()) continue;
                if (lineNum == 1 && line.toLowerCase().contains("productid")) {
                    continue; // Skip header
                }

                List<String> tokens = parseCsvLine(line);
                if (tokens.size() >= 4) {
                    try {
                        String id = tokens.get(0).trim().toUpperCase();
                        String name = tokens.get(1).trim();
                        String cat = tokens.get(2).trim();
                        BigDecimal price = new BigDecimal(tokens.get(3).trim()).setScale(2, RoundingMode.HALF_UP);
                        int thresh = tokens.size() > 4 && !tokens.get(4).trim().isEmpty() ? Integer.parseInt(tokens.get(4).trim()) : 10;
                        String barcode = tokens.size() > 5 ? tokens.get(5).trim() : "";
                        String sku = tokens.size() > 6 ? tokens.get(6).trim() : "";

                        if (id.isEmpty() || name.isEmpty() || price.compareTo(BigDecimal.ZERO) < 0) {
                            tableModel.addRow(new Object[]{id, name, cat, price, thresh, barcode, sku, "INVALID: Missing data"});
                            errorMessages.add("Line " + lineNum + ": Missing ID or invalid price");
                        } else {
                            Product p = new Product(id, name, cat, price, thresh);
                            p.setBarcode(barcode);
                            p.setSku(sku);
                            p.setActive(true);
                            validProducts.add(p);
                            tableModel.addRow(new Object[]{id, name, cat, price, thresh, barcode, sku, "VALID"});
                        }
                    } catch (Exception ex) {
                        tableModel.addRow(new Object[]{tokens.get(0), "Parse Error", "", "", "", "", "", "ERROR: " + ex.getMessage()});
                        errorMessages.add("Line " + lineNum + ": " + ex.getMessage());
                    }
                } else {
                    errorMessages.add("Line " + lineNum + ": Expected at least 4 columns");
                }
            }

            lblSummary.setText("Found " + validProducts.size() + " valid products. Errors: " + errorMessages.size());
            btnImport.setEnabled(!validProducts.isEmpty());

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to read CSV: " + ex.getMessage(), "Read Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void executeImport() {
        int imported = 0;
        int skipped = 0;
        int updated = 0;
        boolean overwrite = radOverwrite.isSelected();

        for (Product p : validProducts) {
            Product existing = inventoryService.getProductById(p.getProductId());
            if (existing != null) {
                if (overwrite) {
                    try {
                        inventoryService.updateProduct(p);
                        updated++;
                    } catch (Exception e) {}
                } else {
                    skipped++;
                }
            } else {
                try {
                    inventoryService.addProduct(p);
                    imported++;
                } catch (Exception e) {}
            }
        }

        ToastNotification.show(getParent(), "Import Completed: " + imported + " added, " + updated + " updated, " + skipped + " skipped.", ToastNotification.Type.SUCCESS);
        dispose();
    }

    private List<String> parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString().trim());
        return tokens;
    }
}
