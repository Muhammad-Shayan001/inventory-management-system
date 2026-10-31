package shelflife.example;

import shelflife.service.DefaultReportService;
import shelflife.service.ReportService;
import shelflife.storage.FileManager;
import shelflife.ui.reports.DashboardReportsPanel;

import javax.swing.*;
import java.nio.file.Paths;

public class DashboardDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

                FileManager fileManager = new FileManager(Paths.get("data"));
                ReportService reportService = new DefaultReportService(fileManager);

                JFrame frame = new JFrame("ShelfLife - Dashboard & Reports (Member 4)");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setSize(1000, 650);
                frame.setLocationRelativeTo(null);

                DashboardReportsPanel panel = new DashboardReportsPanel(reportService);
                frame.add(panel);

                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
