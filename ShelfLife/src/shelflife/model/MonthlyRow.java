package shelflife.model;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Revenue and gross profit for one calendar month. */
public final class MonthlyRow {

    private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("MMM yy", Locale.ENGLISH);

    private final YearMonth month;
    private final BigDecimal revenue;
    private final BigDecimal profit;

    public MonthlyRow(YearMonth month, BigDecimal revenue, BigDecimal profit) {
        this.month = month;
        this.revenue = revenue;
        this.profit = profit;
    }

    public YearMonth getMonth() {
        return month;
    }

    public String getMonthLabel() {
        return month.format(LABEL);
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public BigDecimal getProfit() {
        return profit;
    }
}
