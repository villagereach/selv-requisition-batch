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

package org.openlmis.requisition.batch.repository.custom.impl;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.hibernate.SQLQuery;
import org.hibernate.type.IntegerType;
import org.hibernate.type.PostgresUUIDType;
import org.hibernate.type.StringType;
import org.openlmis.requisition.batch.repository.RequisitionQueryLineItem;
import org.springframework.stereotype.Repository;

@Repository
public class RequisitionSummaryRepositoryCustomImpl {

  private static final String REQUISITION_SUMMARY_SQL = "SELECT z.name AS districtname,"
      + " li.orderableid, li.orderableversionnumber,"
      + " SUM(li.requestedquantity) AS requestedquantity,"
      + " SUM(li.stockonhand) AS stockonhand,"
      + " SUM(li.packsToShip) AS packstoship,"
      + " string_agg(cast(r.id as text), ',') AS requisitionids,"
      + " string_agg(cast(r.supervisorynodeid as text), ',') AS supervisorynodeids"
      + " FROM requisition.requisitions AS r"
      + " INNER JOIN requisition.requisition_line_items AS li ON li.requisitionid = r.id"
      + " INNER JOIN referencedata.facilities AS f ON f.id = r.facilityid"
      + " INNER JOIN referencedata.geographic_zones AS z ON z.id = f.geographiczoneid"
      + " WHERE r.supervisorynodeid IN ('%s') AND r.processingperiodid = '%s'"
      + " AND r.programid = '%s' AND r.status in ('AUTHORIZED', 'IN_APPROVAL')"
      + " GROUP BY z.name, li.orderableid,"
      + " li.orderableversionnumber, r.processingperiodid, r.programid;";

  @PersistenceContext
  private EntityManager entityManager;

  /**
   * Fetches requisition summary line items from data base.
   *
   * @param  processingPeriodId period id for summary
   * @param  programId          program id for summary
   * @param  supervisoryNodeIds ids of node supervised by user
   * @return                    grouped data for requisition summary
   */
  public List<RequisitionQueryLineItem> getRequisitionSummaries(UUID processingPeriodId,
      UUID programId, Set<UUID> supervisoryNodeIds) {
    Query query = entityManager.createNativeQuery(
        String.format(REQUISITION_SUMMARY_SQL,
            supervisoryNodeIds.stream().map(UUID::toString).collect(joining("','")),
            processingPeriodId,
            programId));
    addScalars(query);
    return toQueryLineItems(Collections.checkedList(query.getResultList(), Object[].class));
  }

  private List<RequisitionQueryLineItem> toQueryLineItems(List<Object[]> result) {
    return result.stream()
        .map(fields -> new RequisitionQueryLineItem((String) fields[0], (UUID) fields[1],
            (Integer) fields[2], (Integer) fields[3], (Integer) fields[4], (Integer) fields[5],
            Arrays.stream(((String) fields[6]).split(","))
                .map(UUID::fromString)
                .collect(toList()),
            Arrays.stream(((String) fields[7]).split(","))
                .map(UUID::fromString)
                .collect(toList())
        ))
        .collect(toList());
  }

  private void addScalars(Query query) {
    SQLQuery sql = query.unwrap(SQLQuery.class);
    sql.addScalar("districtname", StringType.INSTANCE);
    sql.addScalar("orderableid", PostgresUUIDType.INSTANCE);
    sql.addScalar("orderableversionnumber", IntegerType.INSTANCE);
    sql.addScalar("requestedquantity", IntegerType.INSTANCE);
    sql.addScalar("stockonhand", IntegerType.INSTANCE);
    sql.addScalar("packstoship", IntegerType.INSTANCE);
    sql.addScalar("requisitionids", StringType.INSTANCE);
    sql.addScalar("supervisorynodeids", StringType.INSTANCE);
  }
}
