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

package org.openlmis.requisition.batch.testutils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.openlmis.requisition.batch.dto.ObjectReferenceDto;
import org.openlmis.requisition.batch.dto.summary.OrderableDistrictSummaryDto;
import org.openlmis.requisition.batch.dto.summary.RequisitionSummaryLineItemDto;

public class RequisitionSummaryLineItemDtoDataBuilder {

  private static int instanceNumber = 0;

  private ObjectReferenceDto orderable;
  private Map<String, OrderableDistrictSummaryDto> zoneSummaries;

  /**
   * Used for creating new instance of {@link RequisitionSummaryLineItemDto}.
   */
  public RequisitionSummaryLineItemDtoDataBuilder() {
    instanceNumber++;

    orderable = new ObjectReferenceDtoDataBuilder()
        .withPath("orderables")
        .buildAsDto();
    zoneSummaries = new HashMap<>();
    zoneSummaries.put(
        "zone" + instanceNumber,
        new OrderableDistrictSummaryDtoDataBuilder().build());
  }

  public RequisitionSummaryLineItemDto build() {
    return new RequisitionSummaryLineItemDto(orderable, zoneSummaries);
  }

  /**
   * Sets orderable id.
   *
   * @param orderableId orderable id
   * @return builder object
   */
  public RequisitionSummaryLineItemDtoDataBuilder withOrderableId(UUID orderableId) {
    orderable = new ObjectReferenceDtoDataBuilder()
        .withPath("orderables")
        .withId(orderableId)
        .buildAsDto();
    return this;
  }

  public RequisitionSummaryLineItemDtoDataBuilder withZoneSummaries(
      Map<String, OrderableDistrictSummaryDto> zoneSummaries) {
    this.zoneSummaries = zoneSummaries;
    return this;
  }
}
