package shelflife.service;

import shelflife.model.Sale;
import shelflife.model.SaleReturn;
import shelflife.model.SaleReturnItem;

import java.util.List;

/**
 * Service contract for handling customer returns, refund arithmetic,
 * and stock restock/wastage disposition.
 */
public interface ReturnsService {

    Sale getSaleForReturn(String saleId) throws Exception;

    List<SaleReturn> getReturnsForSale(String saleId);

    List<SaleReturn> getAllReturns();

    SaleReturn processReturn(String saleId, List<SaleReturnItem> itemsToReturn, String reason, String processedBy) throws Exception;

    void reload();
}
