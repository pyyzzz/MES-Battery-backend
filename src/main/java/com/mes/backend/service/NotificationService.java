package com.mes.backend.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mes.backend.dto.NotificationDto;
import com.mes.backend.dto.inventory.InventoryMaterialDto;
import com.mes.backend.entity.Bom;
import com.mes.backend.entity.Product;
import com.mes.backend.entity.WorkOrder;
import com.mes.backend.repository.WorkOrderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private static final int MAX_NOTIFICATIONS = 20;

    private final WorkOrderRepository workOrderRepository;
    private final InventoryService inventoryService;

    @Transactional(readOnly = true)
    public List<NotificationDto> getNotifications() {
        List<NotificationDto> workOrderNotifications = workOrderRepository.search(null, "COMPLETED", null).stream()
                .filter(order -> order.getCompletedAt() != null)
                .sorted(Comparator.comparing(WorkOrder::getCompletedAt).reversed())
                .map(this::toWorkOrderCompletedNotification)
                .toList();

        List<NotificationDto> materialNotifications = inventoryService.getMaterials(null, null, null, null).stream()
                .filter(material -> !"safe".equals(material.getStatus()))
                .map(this::toMaterialStockNotification)
                .toList();

        return Stream.concat(materialNotifications.stream(), workOrderNotifications.stream())
                .limit(MAX_NOTIFICATIONS)
                .toList();
    }

    private NotificationDto toWorkOrderCompletedNotification(WorkOrder order) {
        String productName = productName(order);
        return NotificationDto.builder()
                .id("work-order-completed-" + order.getId())
                .type("WORK_ORDER_COMPLETED")
                .severity("success")
                .title("작업지시가 완료되었습니다.")
                .message(order.getWorkOrderNo() + " / " + productName)
                .time(relativeTime(order.getCompletedAt()))
                .build();
    }

    private NotificationDto toMaterialStockNotification(InventoryMaterialDto material) {
        boolean danger = "danger".equals(material.getStatus());
        return NotificationDto.builder()
                .id("material-stock-" + material.getId())
                .type("MATERIAL_STOCK")
                .severity(danger ? "danger" : "warning")
                .title(danger ? "자재 재고가 소진되었습니다." : "자재 재고가 안전 기준 미만입니다.")
                .message(material.getName() + " 현재 " + formatQuantity(material.getStock()) + " "
                        + material.getUnit() + " / 안전재고 " + formatQuantity(material.getSafetyStock()) + " "
                        + material.getUnit())
                .time("현재")
                .build();
    }

    private String productName(WorkOrder order) {
        Bom bom = order.getBom();
        Product product = bom != null ? bom.getProduct() : null;
        return product != null && product.getProductName() != null ? product.getProductName() : "-";
    }

    private String formatQuantity(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private String relativeTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        long minutes = Math.max(0, Duration.between(dateTime, LocalDateTime.now()).toMinutes());
        if (minutes < 1) {
            return "방금 전";
        }
        if (minutes < 60) {
            return minutes + "분 전";
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + "시간 전";
        }
        return (hours / 24) + "일 전";
    }
}
