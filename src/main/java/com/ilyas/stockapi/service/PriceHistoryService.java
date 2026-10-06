package com.ilyas.stockapi.service;

import com.ilyas.stockapi.dto.PriceChangeResponse;
import com.ilyas.stockapi.entity.PriceChange;
import com.ilyas.stockapi.entity.Product;
import com.ilyas.stockapi.repository.PriceChangeRepository;
import com.ilyas.stockapi.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

/**
 * Records every price change and decides the struck-through "previous price" shown after a drop.
 *
 * The previous price is the lowest price of the 30 days before the drop, as European rules require
 * for announced price reductions. So raising a price just before lowering it can't make a fake
 * reduction: 9000, then 9999, then 9500 shows no reduction, since the product was at 9000 recently.
 * It is shown for 30 days after the drop (see Product.getPreviousPriceAt).
 */
@Service
@Transactional(readOnly = true)
public class PriceHistoryService {

    public static final Duration REFERENCE_PERIOD = Duration.ofDays(30);

    private final PriceChangeRepository priceChangeRepository;

    public PriceHistoryService(PriceChangeRepository priceChangeRepository) {
        this.priceChangeRepository = priceChangeRepository;
    }

    // Called by ProductService when an update changes the price; product is the locked, managed row
    @Transactional
    public void record(Product product, BigDecimal oldPrice, BigDecimal newPrice) {
        PriceChange change = new PriceChange();
        change.setProduct(product);
        change.setOldPrice(oldPrice);
        change.setNewPrice(newPrice);
        change.setChangedBy(CurrentUser.username());
        priceChangeRepository.save(change);

        product.setPreviousPrice(null);
        product.setPriceReducedAt(null);
        if (newPrice.compareTo(oldPrice) < 0) {
            BigDecimal lowest = priceChangeRepository.findLowestOldPriceSince(
                    product.getId(), change.getChangedAt().minus(REFERENCE_PERIOD));
            if (lowest != null && lowest.compareTo(newPrice) > 0) {
                product.setPreviousPrice(lowest);
                product.setPriceReducedAt(change.getChangedAt());
            }
        }
    }

    // Newest first
    public List<PriceChangeResponse> historyOf(Long productId) {
        return priceChangeRepository.findByProductIdOrderByChangedAtDescIdDesc(productId).stream()
                .map(PriceChangeResponse::from)
                .toList();
    }

    @Transactional
    public void deleteHistoryOf(Long productId) {
        priceChangeRepository.deleteByProductId(productId);
    }
}
