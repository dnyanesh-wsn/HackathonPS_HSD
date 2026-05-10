/**
 * Fixes the ORDER_STATE_MISMATCH vulnerability where PaymentService continued processing
 * payments for orders in terminal states (FAILED, CANCELLED), and ChaosScheduler routed
 * settlement jobs without validating current order status first.
 *
 * @fix-for INV_SVC_TIMEOUT -> PARTIAL_RESERVATION -> ORDER_STATE_MISMATCH causal chain
 * @confidence HIGH - root cause is clearly a missing terminal-state guard in PaymentService
 *             and a missing pre-route status check in ChaosScheduler
 * @blast-radius MEDIUM - changes affect PaymentService.initiatePayment() entry point and
 *              ChaosScheduler.scheduleSettlement() routing logic; no schema changes required;
 *              existing PAID/CONFIRMED orders are unaffected; adds one new config key
 */

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class InventoryService {

    private static final Logger log = Logger.getLogger(InventoryService.class.getName());

    // Centralised set of states from which no further financial processing is permitted.
    // Defined here so both PaymentService and ChaosScheduler share one authoritative source.
    private static final Set<OrderStatus> TERMINAL_NON_PAYABLE_STATES =
            Collections.unmodifiableSet(                          // immutable to prevent accidental mutation at runtime
                    EnumSet.of(OrderStatus.FAILED, OrderStatus.CANCELLED));

    // Config key consumed by ChaosScheduler; value mirrors TERMINAL_NON_PAYABLE_STATES names.
    static final String SKIP_TERMINAL_STATES_CONFIG_KEY = "settlement.worker.skip-terminal-states"; // single source of truth for config key name

    // -------------------------------------------------------------------------
    // Inner collaborator stubs – replace with your actual injected dependencies
    // -------------------------------------------------------------------------

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final SettlementConfig settlementConfig;

    public InventoryService(OrderRepository orderRepository,
                            PaymentRepository paymentRepository,
                            SettlementConfig settlementConfig) {
        this.orderRepository   = orderRepository;   // injected; never null
        this.paymentRepository = paymentRepository; // injected; never null
        this.settlementConfig  = settlementConfig;  // injected; never null
    }

    // =========================================================================
    // FIX 1 – PaymentService.initiatePayment()
    // Guard: abort immediately when the order is in a terminal non-payable state.
    // Previously the mismatch was *detected* but not *acted upon*, allowing the
    // payment record to be persisted for a FAILED order (see causal chain step 4).
    // =========================================================================

    /**
     * Initiates payment for the given order.
     * Hard-aborts when the order is already in a terminal state to prevent
     * collecting money against an unfulfillable order (no stock was reserved).
     *
     * @param orderId the order for which payment is being initiated
     * @throws InvalidOrderStateException immediately if order is FAILED or CANCELLED
     */
    public PaymentResult initiatePayment(String orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId)); // fail-fast on missing order

        // PRIMARY FIX: enforce hard abort for terminal states BEFORE any payment work begins.
        // Previously PaymentService detected ORDER_STATE_MISMATCH but continued, persisting
        // a payment record at 09:21:15.965347100Z for a FAILED order (causal chain step 4).
        if (TERMINAL_NON_PAYABLE_STATES.contains(order.getStatus())) {
            log.severe(String.format(                              // log at SEVERE so ops alerts fire immediately
                    "ORDER_STATE_MISMATCH – aborting payment for order %s in terminal state %s. " +
                    "No payment record will be created.", orderId