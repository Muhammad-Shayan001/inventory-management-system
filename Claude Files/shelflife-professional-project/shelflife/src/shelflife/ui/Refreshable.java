package shelflife.ui;

/**
 * Interface implemented by UI panels that can refresh their visual state
 * from underlying data services when selected or modified.
 */
public interface Refreshable {
    void refreshData();
}
