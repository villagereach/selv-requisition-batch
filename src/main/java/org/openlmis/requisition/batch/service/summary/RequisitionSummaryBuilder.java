/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2017 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details. You should have received a copy of
 * the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org.
 */

package org.openlmis.requisition.batch.service.summary;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import org.openlmis.requisition.batch.dto.ObjectReferenceDto;
import org.openlmis.requisition.batch.dto.summary.OrderableDistrictSummaryDto;
import org.openlmis.requisition.batch.dto.summary.OrderableVersionSummaryDto;
import org.openlmis.requisition.batch.dto.summary.RequisitionSummaryDto;
import org.openlmis.requisition.batch.dto.summary.RequisitionSummaryLineItemDto;
import org.openlmis.requisition.batch.repository.RequisitionQueryLineItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RequisitionSummaryBuilder {

  private static final String PROCESSING_PERIODS_RESOURCE = "processingPeriods";
  private static final String PROGRAMS_RESOURCE = "programs";
  private static final String ORDERABLES_RESOURCE = "orderables";

  @Value("${service.url}")
  private String serviceUrl;

  /**
   * Builds requisition summary from line items fetched by query.
   *
   * @param  lineItems          db line items
   * @param  programId          id of a program used for summary
   * @param  processingPeriodId id of a period used for summary
   * @return                    summary dto ready for sending as a response
   */
  public RequisitionSummaryDto build(List<RequisitionQueryLineItem> lineItems,
      UUID programId, UUID processingPeriodId) {

    Map<UUID, Map<String, Map<Integer, List<RequisitionQueryLineItem>>>> groupedLineItems =
        lineItems.stream()
            .collect(groupingBy(RequisitionQueryLineItem::getOrderableId,
                groupingBy(RequisitionQueryLineItem::getDistrictName,
                    groupingBy(RequisitionQueryLineItem::getOrderableVersionNumber, toList()))));

    Map<String, List<RequisitionQueryLineItem>> groupedByDistricts =
        lineItems.stream()
            .collect(groupingBy(RequisitionQueryLineItem::getDistrictName, toList()));

    return new RequisitionSummaryDto(
        new ObjectReferenceDto(programId, serviceUrl, PROGRAMS_RESOURCE),
        new ObjectReferenceDto(processingPeriodId, serviceUrl, PROCESSING_PERIODS_RESOURCE),
        groupedLineItems.entrySet().stream()
            .map(entry -> build(entry.getKey(), entry.getValue()))
            .collect(toList()),
        groupedByDistricts.entrySet().stream()
            .collect(toMap(
                Entry::getKey,
                entry -> entry.getValue().stream()
                    .flatMap(lineItem -> lineItem.getRequisitionIds().stream())
                    .collect(toSet()))),
        groupedByDistricts.entrySet().stream()
            .collect(toMap(
                Entry::getKey,
                entry -> entry.getValue().stream()
                    .flatMap(lineItem -> lineItem.getSupervisoryNodeIds().stream())
                    .collect(toSet()))));
  }

  private RequisitionSummaryLineItemDto build(UUID orderableId,
      Map<String, Map<Integer, List<RequisitionQueryLineItem>>> groupedByDistricts) {
    return new RequisitionSummaryLineItemDto(
        new ObjectReferenceDto(orderableId, serviceUrl, ORDERABLES_RESOURCE),
        groupedByDistricts.entrySet().stream()
            .collect(toMap(Entry::getKey, entry -> build(entry.getValue()))));
  }

  private OrderableDistrictSummaryDto build(
      Map<Integer, List<RequisitionQueryLineItem>> groupedByOrderableVersion) {
    return new OrderableDistrictSummaryDto(
        groupedByOrderableVersion.entrySet().stream()
            .map(entry -> build(entry.getKey(), entry.getValue()))
            .collect(toList()));
  }

  private OrderableVersionSummaryDto build(Integer orderableVersion,
      List<RequisitionQueryLineItem> lineItems) {
    return new OrderableVersionSummaryDto(orderableVersion,
        lineItems.stream()
            .filter(item -> item.getStockOnHand() != null)
            .mapToInt(RequisitionQueryLineItem::getStockOnHand)
            .sum(),
        lineItems.stream()
            .filter(item -> item.getRequestedQuantity() != null)
            .mapToInt(RequisitionQueryLineItem::getRequestedQuantity)
            .sum(),
        lineItems.stream()
            .filter(item -> item.getPacksToShip() != null)
            .mapToInt(RequisitionQueryLineItem::getPacksToShip)
            .sum());
  }
}
