package com.mes.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mes.backend.dto.inventory.InventoryLotDto;
import com.mes.backend.dto.inventory.InventoryLotSummaryDto;
import com.mes.backend.dto.inventory.InventoryLotUsageDto;
import com.mes.backend.dto.inventory.InventoryMaterialDto;
import com.mes.backend.dto.inventory.InventoryMaterialLotSummaryDto;
import com.mes.backend.dto.inventory.InventoryMaterialSummaryDto;
import com.mes.backend.dto.inventory.InventoryTransactionDto;
import com.mes.backend.dto.inventory.InventoryTransactionSummaryDto;
import com.mes.backend.entity.Employee;
import com.mes.backend.entity.Material;
import com.mes.backend.entity.MaterialLot;
import com.mes.backend.entity.MaterialTransaction;
import com.mes.backend.entity.ProductLot;
import com.mes.backend.repository.EmployeeRepository;
import com.mes.backend.repository.MaterialLotRepository;
import com.mes.backend.repository.MaterialRepository;
import com.mes.backend.repository.MaterialTransactionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InventoryService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter LOT_PREFIX_FORMAT = DateTimeFormatter.ofPattern("yyMMddHHmm");
    private static final String INBOUND = "INBOUND";
    private static final String CONSUME = "CONSUME";
    private static final String CONSUMPTION = "CONSUMPTION";

    private final MaterialRepository materialRepository;
    private final MaterialLotRepository materialLotRepository;
    private final MaterialTransactionRepository materialTransactionRepository;
    private final EmployeeRepository employeeRepository;

    @Transactional(readOnly = true)
    public List<InventoryMaterialDto> getMaterials(String startDate, String endDate, String status, String keyword) {
        LocalDate start = parseDate(startDate);
        LocalDate end = parseDate(endDate);
        String normalizedStatus = normalize(status);
        String normalizedKeyword = normalize(keyword);

        return materialRepository.findAllForInventoryPage().stream()
                .map(this::toMaterialDto)
                .filter(material -> matchesStartDate(parseNullableDate(material.getRegisteredAt()), start))
                .filter(material -> matchesEndDate(parseNullableDate(material.getRegisteredAt()), end))
                .filter(material -> normalizedStatus == null || normalizedStatus.equals(normalize(material.getStatus())))
                .filter(material -> normalizedKeyword == null || materialMatchesKeyword(material, normalizedKeyword))
                .toList();
    }

    @Transactional(readOnly = true)
    public InventoryMaterialSummaryDto getMaterialSummary(String startDate, String endDate, String status, String keyword) {
        List<InventoryMaterialDto> materials = getMaterials(startDate, endDate, status, keyword);
        return InventoryMaterialSummaryDto.builder()
                .total(materials.size())
                .safe(materials.stream().filter(material -> "safe".equals(material.getStatus())).count())
                .warning(materials.stream().filter(material -> "warning".equals(material.getStatus())).count())
                .danger(materials.stream().filter(material -> "danger".equals(material.getStatus())).count())
                .build();
    }

    @Transactional
    public InventoryMaterialDto inboundMaterial(Long materialId, BigDecimal quantity, Long employeeId) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new IllegalArgumentException("Inbound quantity must be greater than zero.");
        }
        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new IllegalArgumentException("Material not found: " + materialId));
        Employee employee = resolveEmployee(employeeId);
        LocalDateTime now = LocalDateTime.now();
        String prefix = "ML-" + LOT_PREFIX_FORMAT.format(now);
        long sequence = materialLotRepository.countByMaterialLotNoStartingWith(prefix) + 1;

        MaterialLot materialLot = materialLotRepository.save(MaterialLot.builder()
                .material(material)
                .materialLotNo(prefix + "-" + sequence)
                .initialQuantity(quantity)
                .currentQuantity(quantity)
                .lotStatus("WAITING")
                .receiptDate(now)
                .build());
        material.getMaterialLots().add(materialLot);

        materialTransactionRepository.save(MaterialTransaction.builder()
                .materialLot(materialLot)
                .employee(employee)
                .transactionType(INBOUND)
                .quantity(quantity)
                .transactionAt(now)
                .build());

        return toMaterialDto(material);
    }

    @Transactional(readOnly = true)
    public List<InventoryTransactionDto> getTransactions(String startDate, String endDate, String type, String keyword) {
        LocalDate start = parseDate(startDate);
        LocalDate end = parseDate(endDate);
        String normalizedType = normalizeTransactionFilter(type);
        String normalizedKeyword = normalize(keyword);
        Map<Long, StockSnapshot> snapshotsByTransactionId = calculateStockSnapshots();

        return materialTransactionRepository.findAllForInventoryTransactionPage().stream()
                .map(transaction -> toTransactionDto(transaction, snapshotsByTransactionId.get(transaction.getId())))
                .filter(transaction -> matchesStartDate(parseNullableDateTime(transaction.getOccurredAt()), start))
                .filter(transaction -> matchesEndDate(parseNullableDateTime(transaction.getOccurredAt()), end))
                .filter(transaction -> normalizedType == null || normalizedType.equals(normalize(transaction.getType())))
                .filter(transaction -> normalizedKeyword == null || transactionMatchesKeyword(transaction, normalizedKeyword))
                .toList();
    }

    @Transactional(readOnly = true)
    public InventoryTransactionSummaryDto getTransactionSummary(String startDate, String endDate, String type, String keyword) {
        List<InventoryTransactionDto> transactions = getTransactions(startDate, endDate, type, keyword);
        BigDecimal inbound = transactions.stream()
                .filter(transaction -> INBOUND.equals(transaction.getType()))
                .map(InventoryTransactionDto::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal consumption = transactions.stream()
                .filter(transaction -> CONSUMPTION.equals(transaction.getType()))
                .map(InventoryTransactionDto::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return InventoryTransactionSummaryDto.builder()
                .inbound(inbound)
                .consumption(consumption)
                .ratio(rate(consumption, inbound))
                .build();
    }

    @Transactional(readOnly = true)
    public List<InventoryLotDto> getLots(String startDate, String endDate, String status, String keyword) {
        LocalDate start = parseDate(startDate);
        LocalDate end = parseDate(endDate);
        String normalizedStatus = normalize(status);
        String normalizedKeyword = normalize(keyword);
        Map<Long, List<MaterialTransaction>> transactionsByLotId = transactionsByLotId();

        return materialLotRepository.findAllForInventoryLotPage().stream()
                .map(lot -> toLotDto(lot, transactionsByLotId.getOrDefault(lot.getId(), List.of())))
                .filter(lot -> matchesStartDate(parseNullableDateTime(lot.getInboundAt()), start))
                .filter(lot -> matchesEndDate(parseNullableDateTime(lot.getInboundAt()), end))
                .filter(lot -> normalizedStatus == null || normalizedStatus.equals(normalize(lot.getStatus())))
                .filter(lot -> normalizedKeyword == null || lotMatchesKeyword(lot, normalizedKeyword))
                .toList();
    }

    @Transactional(readOnly = true)
    public InventoryLotSummaryDto getLotSummary(String startDate, String endDate, String status, String keyword) {
        List<InventoryLotDto> lots = getLots(startDate, endDate, status, keyword);
        return InventoryLotSummaryDto.builder()
                .total(lots.size())
                .inUse(lots.stream().filter(lot -> "IN_USE".equals(lot.getStatus())).count())
                .waiting(lots.stream().filter(lot -> "WAITING".equals(lot.getStatus())).count())
                .defect(lots.stream().filter(lot -> "DEFECT".equals(lot.getStatus())).count())
                .build();
    }

    private InventoryMaterialDto toMaterialDto(Material material) {
        List<MaterialLot> lots = sortedMaterialLots(material);
        BigDecimal stock = lots.stream()
                .map(MaterialLot::getCurrentQuantity)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String status = inventoryStatus(stock, material.getSafetyStock());
        MaterialLot latestInboundLot = lots.stream()
                .filter(lot -> lot.getReceiptDate() != null)
                .max(Comparator.comparing(MaterialLot::getReceiptDate))
                .orElse(null);

        return InventoryMaterialDto.builder()
                .id(material.getId())
                .code(valueOrEmpty(material.getMaterialCode()))
                .name(valueOrEmpty(material.getMaterialName()))
                .stock(stock)
                .safetyStock(material.getSafetyStock() != null ? material.getSafetyStock() : BigDecimal.ZERO)
                .unit(valueOrEmpty(material.getUnit()))
                .status(status)
                .registeredAt(formatDate(material.getRegisteredAt()))
                .lastInboundAt(formatDateTime(latestInboundLot != null ? latestInboundLot.getReceiptDate() : null))
                .location("Material Warehouse")
                .lotNo(latestInboundLot != null ? valueOrEmpty(latestInboundLot.getMaterialLotNo()) : "")
                .lots(lots.stream().map(this::toMaterialLotSummaryDto).toList())
                .build();
    }

    private InventoryMaterialLotSummaryDto toMaterialLotSummaryDto(MaterialLot lot) {
        return InventoryMaterialLotSummaryDto.builder()
                .id(lot.getId())
                .lotNo(valueOrEmpty(lot.getMaterialLotNo()))
                .inboundAt(formatDateTime(lot.getReceiptDate()))
                .initialQuantity(safe(lot.getInitialQuantity()))
                .currentQuantity(safe(lot.getCurrentQuantity()))
                .status(lotStatus(lot))
                .build();
    }

    private InventoryTransactionDto toTransactionDto(MaterialTransaction transaction, StockSnapshot snapshot) {
        MaterialLot materialLot = transaction.getMaterialLot();
        Material material = materialLot != null ? materialLot.getMaterial() : null;
        ProductLot productLot = transaction.getProductLot();
        String type = transactionType(transaction.getTransactionType());

        return InventoryTransactionDto.builder()
                .id(transaction.getId())
                .occurredAt(formatDateTime(transaction.getTransactionAt()))
                .type(type)
                .materialCode(material != null ? valueOrEmpty(material.getMaterialCode()) : "")
                .materialName(material != null ? valueOrEmpty(material.getMaterialName()) : "")
                .unit(material != null ? valueOrEmpty(material.getUnit()) : "")
                .materialLotNo(materialLot != null ? valueOrEmpty(materialLot.getMaterialLotNo()) : "")
                .productLotNo(productLot != null ? valueOrEmpty(productLot.getProductLotNo()) : "-")
                .quantity(safe(transaction.getQuantity()))
                .beforeStock(snapshot != null ? snapshot.beforeStock() : BigDecimal.ZERO)
                .afterStock(snapshot != null ? snapshot.afterStock() : BigDecimal.ZERO)
                .worker(transaction.getEmployee() != null ? valueOrEmpty(transaction.getEmployee().getEmployeeName()) : "")
                .note(INBOUND.equals(type) ? "Material inbound" : "Material consumed")
                .build();
    }

    private InventoryLotDto toLotDto(MaterialLot lot, List<MaterialTransaction> transactions) {
        Material material = lot.getMaterial();
        BigDecimal initial = safe(lot.getInitialQuantity());
        BigDecimal remaining = safe(lot.getCurrentQuantity());
        BigDecimal consumed = initial.subtract(remaining);
        if (consumed.signum() < 0) {
            consumed = BigDecimal.ZERO;
        }
        LocalDateTime updatedAt = transactions.stream()
                .map(MaterialTransaction::getTransactionAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(lot.getReceiptDate());

        return InventoryLotDto.builder()
                .id(lot.getId())
                .inboundAt(formatDateTime(lot.getReceiptDate()))
                .status(lotStatus(lot))
                .lotNo(valueOrEmpty(lot.getMaterialLotNo()))
                .materialCode(material != null ? valueOrEmpty(material.getMaterialCode()) : "")
                .materialName(material != null ? valueOrEmpty(material.getMaterialName()) : "")
                .unit(material != null ? valueOrEmpty(material.getUnit()) : "")
                .totalStock(initial)
                .consumed(consumed)
                .remaining(remaining)
                .consumptionRate(rate(consumed, initial))
                .updatedAt(formatDateTime(updatedAt))
                .usages(transactions.stream()
                        .filter(transaction -> CONSUME.equals(transaction.getTransactionType()))
                        .map(this::toLotUsageDto)
                        .toList())
                .build();
    }

    private InventoryLotUsageDto toLotUsageDto(MaterialTransaction transaction) {
        ProductLot productLot = transaction.getProductLot();
        return InventoryLotUsageDto.builder()
                .occurredAt(formatDateTime(transaction.getTransactionAt()))
                .productLotNo(productLot != null ? valueOrEmpty(productLot.getProductLotNo()) : "-")
                .quantity(safe(transaction.getQuantity()))
                .build();
    }

    private Map<Long, StockSnapshot> calculateStockSnapshots() {
        List<MaterialTransaction> transactions = materialTransactionRepository.findAllForInventoryTransactionPage();
        Map<Long, List<MaterialTransaction>> grouped = transactions.stream()
                .filter(transaction -> transaction.getMaterialLot() != null)
                .collect(Collectors.groupingBy(transaction -> transaction.getMaterialLot().getId(), LinkedHashMap::new, Collectors.toList()));
        Map<Long, StockSnapshot> snapshots = new LinkedHashMap<>();

        for (List<MaterialTransaction> lotTransactions : grouped.values()) {
            List<MaterialTransaction> chronological = lotTransactions.stream()
                    .sorted(Comparator
                            .comparing(MaterialTransaction::getTransactionAt, Comparator.nullsLast(Comparator.naturalOrder()))
                            .thenComparing(MaterialTransaction::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();
            MaterialLot lot = chronological.get(0).getMaterialLot();
            boolean hasInbound = chronological.stream().anyMatch(transaction -> INBOUND.equals(transaction.getTransactionType()));
            BigDecimal stock = hasInbound ? BigDecimal.ZERO : safe(lot.getInitialQuantity());
            for (MaterialTransaction transaction : chronological) {
                BigDecimal before = stock;
                if (INBOUND.equals(transaction.getTransactionType())) {
                    stock = stock.add(safe(transaction.getQuantity()));
                } else if (CONSUME.equals(transaction.getTransactionType())) {
                    stock = stock.subtract(safe(transaction.getQuantity()));
                }
                snapshots.put(transaction.getId(), new StockSnapshot(before, stock));
            }
        }
        return snapshots;
    }

    private Map<Long, List<MaterialTransaction>> transactionsByLotId() {
        return materialTransactionRepository.findAllForInventoryTransactionPage().stream()
                .filter(transaction -> transaction.getMaterialLot() != null)
                .collect(Collectors.groupingBy(transaction -> transaction.getMaterialLot().getId(), LinkedHashMap::new, Collectors.toList()));
    }

    private List<MaterialLot> sortedMaterialLots(Material material) {
        if (material.getMaterialLots() == null) {
            return List.of();
        }
        return material.getMaterialLots().stream()
                .sorted(Comparator
                        .comparing(MaterialLot::getReceiptDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(MaterialLot::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private Employee resolveEmployee(Long employeeId) {
        if (employeeId != null) {
            return employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));
        }
        Optional<Employee> admin = employeeRepository.findByUsername("admin");
        if (admin.isPresent()) {
            return admin.get();
        }
        return employeeRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Employee is required for material inbound."));
    }

    private boolean materialMatchesKeyword(InventoryMaterialDto material, String keyword) {
        return contains(material.getCode(), keyword) || contains(material.getName(), keyword);
    }

    private boolean transactionMatchesKeyword(InventoryTransactionDto transaction, String keyword) {
        return contains(transaction.getMaterialCode(), keyword)
                || contains(transaction.getMaterialName(), keyword)
                || contains(transaction.getMaterialLotNo(), keyword)
                || contains(transaction.getProductLotNo(), keyword);
    }

    private boolean lotMatchesKeyword(InventoryLotDto lot, String keyword) {
        return contains(lot.getLotNo(), keyword)
                || contains(lot.getMaterialCode(), keyword)
                || contains(lot.getMaterialName(), keyword);
    }

    private boolean contains(String value, String keyword) {
        return normalize(value) != null && normalize(value).contains(keyword);
    }

    private String inventoryStatus(BigDecimal stock, BigDecimal safetyStock) {
        if (stock == null || stock.signum() <= 0) {
            return "danger";
        }
        if (safetyStock != null && stock.compareTo(safetyStock) < 0) {
            return "warning";
        }
        return "safe";
    }

    private String lotStatus(MaterialLot lot) {
        BigDecimal currentQuantity = safe(lot.getCurrentQuantity());
        if (currentQuantity.signum() <= 0) {
            return "DEFECT";
        }
        String status = normalize(lot.getLotStatus());
        if (status != null && (status.contains("progress") || status.contains("in_use") || status.contains("생산중"))) {
            return "IN_USE";
        }
        return "WAITING";
    }

    private String transactionType(String transactionType) {
        if (CONSUME.equals(transactionType)) {
            return CONSUMPTION;
        }
        return INBOUND;
    }

    private String normalizeTransactionFilter(String type) {
        String normalized = normalize(type);
        if ("consume".equals(normalized)) {
            return normalize(CONSUMPTION);
        }
        return normalized;
    }

    private boolean matchesStartDate(LocalDate value, LocalDate start) {
        return start == null || (value != null && !value.isBefore(start));
    }

    private boolean matchesEndDate(LocalDate value, LocalDate end) {
        return end == null || (value != null && !value.isAfter(end));
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private double rate(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.signum() <= 0) {
            return 0;
        }
        return numerator.multiply(BigDecimal.valueOf(100))
                .divide(denominator, 1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private String formatDate(LocalDateTime value) {
        return value != null ? DATE_FORMAT.format(value) : "";
    }

    private String formatDateTime(LocalDateTime value) {
        return value != null ? DATE_TIME_FORMAT.format(value) : "";
    }

    private LocalDate parseDate(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Date format must be yyyy-MM-dd: " + value);
        }
    }

    private LocalDate parseNullableDate(String value) {
        return isBlank(value) ? null : LocalDate.parse(value);
    }

    private LocalDate parseNullableDateTime(String value) {
        return isBlank(value) ? null : LocalDateTime.parse(value, DATE_TIME_FORMAT).toLocalDate();
    }

    private String normalize(String value) {
        return isBlank(value) ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String valueOrEmpty(String value) {
        return value != null ? value : "";
    }

    private record StockSnapshot(BigDecimal beforeStock, BigDecimal afterStock) {
    }
}
