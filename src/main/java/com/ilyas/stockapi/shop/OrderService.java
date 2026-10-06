package com.ilyas.stockapi.shop;

import com.ilyas.stockapi.dto.SaleRequest;
import com.ilyas.stockapi.entity.Customer;
import com.ilyas.stockapi.entity.Delivery;
import com.ilyas.stockapi.entity.Product;
import com.ilyas.stockapi.entity.Sale;
import com.ilyas.stockapi.entity.SaleChannel;
import com.ilyas.stockapi.entity.SaleStatus;
import com.ilyas.stockapi.exception.BadRequestException;
import com.ilyas.stockapi.exception.ConflictException;
import com.ilyas.stockapi.exception.NotEnoughStockException;
import com.ilyas.stockapi.exception.NotFoundException;
import com.ilyas.stockapi.exception.TooManyRequestsException;
import com.ilyas.stockapi.repository.CustomerRepository;
import com.ilyas.stockapi.repository.ProductRepository;
import com.ilyas.stockapi.repository.SaleRepository;
import com.ilyas.stockapi.service.OrderStatusService;
import com.ilyas.stockapi.service.SaleService;
import com.ilyas.stockapi.shop.OrderDtos.OrderRequest;
import com.ilyas.stockapi.shop.OrderDtos.OrderView;
import com.ilyas.stockapi.shop.OrderDtos.TrackRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Orders placed in the online shop, paid cash on delivery. An order is a sale (channel ONLINE,
 * status NEW): it takes its products out of stock right away, through the same locked sale engine
 * as the counter, so the website and the counter can never sell the same last unit.
 */
@Service
@Transactional(readOnly = true)
public class OrderService {

    // No 0/O or 1/I, so a number read over the phone can't be misheard
    private static final String NUMBER_LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final SecureRandom random = new SecureRandom();
    private final SaleRepository saleRepository;
    private final SaleService saleService;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final OrderStatusService statuses;
    private final OrderRateLimiter rateLimiter;
    private final ShopProperties properties;

    public OrderService(SaleRepository saleRepository, SaleService saleService, CustomerRepository customerRepository,
                        ProductRepository productRepository, OrderStatusService statuses, OrderRateLimiter rateLimiter,
                        ShopProperties properties) {
        this.saleRepository = saleRepository;
        this.saleService = saleService;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.statuses = statuses;
        this.rateLimiter = rateLimiter;
        this.properties = properties;
    }

    // clientAddress: the visitor's IP, for the limit of orders per hour
    @Transactional
    public OrderView place(OrderRequest request, String clientAddress) {
        rateLimiter.check(clientAddress);
        long waiting = saleRepository.countByDeliveryPhoneAndStatusIn(request.phone(), List.of(SaleStatus.NEW));
        if (waiting >= properties.maxOpenOrdersPerPhone()) {
            throw new TooManyRequestsException("You already have " + waiting
                    + " orders waiting for our call: we'll contact you soon to confirm them");
        }

        // One line per product, with the quantities of repeated lines added up
        Map<Long, Integer> quantities = new TreeMap<>();
        request.items().forEach(line -> quantities.merge(line.productId(), line.quantity(), Integer::sum));
        for (Map.Entry<Long, Integer> line : quantities.entrySet()) {
            Product product = productRepository.findById(line.getKey())
                    .orElseThrow(() -> new BadRequestException("Product " + line.getKey() + " does not exist"));
            if (!product.isPublished()) {
                throw new BadRequestException(product.getName() + " is not sold online any more: remove it from your cart");
            }
            if (line.getValue() > ShopDtos.MAX_PER_ORDER) {
                throw new BadRequestException("At most " + ShopDtos.MAX_PER_ORDER + " of " + product.getName() + " per order");
            }
        }

        Sale sale = new Sale();
        sale.setCustomer(customerFor(request));
        sale.setChannel(SaleChannel.ONLINE);
        sale.setOrderNumber(newOrderNumber());
        sale.setDelivery(new Delivery(request.name(), request.phone(), request.city(), request.address(), request.note()));
        saleRepository.save(sale);
        try {
            // Prices always come from the database (unitPrice null), never from the browser
            saleService.addLines(sale, quantities.entrySet().stream()
                    .map(line -> new SaleRequest.Item(line.getKey(), line.getValue(), null))
                    .toList());
        } catch (NotEnoughStockException e) {
            // Without the exact stock, which the shop doesn't show
            throw new ConflictException("Not enough stock for " + e.getProductName()
                    + ": lower the quantity or remove it from your cart");
        }
        sale.setDeliveryFee(properties.deliveryFeeFor(sale.getTotal()));
        statuses.record(sale, SaleStatus.NEW, "Ordered online");
        return OrderView.from(sale, statuses.historyOf(sale.getId()));
    }

    // The same answer for a wrong number and a wrong phone, so order numbers can't be tried out
    public OrderView track(TrackRequest request) {
        return saleRepository.findByOrderNumber(request.orderNumber())
                .filter(sale -> sale.getChannel() == SaleChannel.ONLINE && sale.getDelivery().getPhone().equals(request.phone()))
                .map(sale -> OrderView.from(sale, statuses.historyOf(sale.getId())))
                .orElseThrow(() -> new NotFoundException("No order " + request.orderNumber() + " with this phone number"));
    }

    // A returning customer is found by phone; otherwise the order creates them
    private Customer customerFor(OrderRequest request) {
        return customerRepository.findFirstByPhoneOrderByIdAsc(request.phone()).orElseGet(() -> {
            Customer customer = new Customer();
            customer.setName(request.name());
            customer.setPhone(request.phone());
            return customerRepository.save(customer);
        });
    }

    // e.g. TS-7K3F9Q
    private String newOrderNumber() {
        String number;
        do {
            StringBuilder code = new StringBuilder("TS-");
            for (int i = 0; i < 6; i++) {
                code.append(NUMBER_LETTERS.charAt(random.nextInt(NUMBER_LETTERS.length())));
            }
            number = code.toString();
        } while (saleRepository.existsByOrderNumber(number));
        return number;
    }
}
