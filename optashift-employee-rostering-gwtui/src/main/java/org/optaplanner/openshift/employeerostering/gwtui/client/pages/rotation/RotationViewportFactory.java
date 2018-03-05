/*
 * Copyright (C) 2018 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.optaplanner.openshift.employeerostering.gwtui.client.pages.rotation;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.jboss.errai.ioc.client.api.ManagedInstance;
import org.optaplanner.openshift.employeerostering.gwtui.client.rostergrid.grid.CssGridLinesFactory;
import org.optaplanner.openshift.employeerostering.gwtui.client.rostergrid.grid.TicksFactory;
import org.optaplanner.openshift.employeerostering.gwtui.client.rostergrid.list.ListElementViewPool;
import org.optaplanner.openshift.employeerostering.gwtui.client.rostergrid.model.Lane;
import org.optaplanner.openshift.employeerostering.gwtui.client.rostergrid.model.LinearScale;
import org.optaplanner.openshift.employeerostering.gwtui.client.rostergrid.model.SubLane;
import org.optaplanner.openshift.employeerostering.gwtui.client.rostergrid.model.Viewport;
import org.optaplanner.openshift.employeerostering.gwtui.client.rostergrid.powers.CollisionFreeSubLaneBuilder;
import org.optaplanner.openshift.employeerostering.gwtui.client.tenant.TenantStore;
import org.optaplanner.openshift.employeerostering.gwtui.client.util.TimingUtils;
import org.optaplanner.openshift.employeerostering.shared.shift.Shift;
import org.optaplanner.openshift.employeerostering.shared.spot.Spot;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

public class RotationViewportFactory {

    @Inject
    private TenantStore tenantStore;

    @Inject
    private ListElementViewPool<ShiftBlobView> shiftBlobViewPool;

    @Inject
    private ManagedInstance<ShiftBlobView> shiftBlobViews;

    @Inject
    private CssGridLinesFactory cssGridLinesFactory;

    @Inject
    private TicksFactory<Long> ticksFactory;

    @Inject
    private TimingUtils timingUtils;

    @Inject
    private CollisionFreeSubLaneBuilder conflictFreeSubLanesBuilder;

    public Viewport<Long> getViewport(final Map<Spot, List<Shift>> shiftsBySpot) {

        return timingUtils.time("Rotation viewport instantiation", () -> {

            shiftBlobViewPool.init(2000L, shiftBlobViews::get);

            final Integer durationInWeeks = tenantStore.getCurrentTenant().getConfiguration().getTemplateDuration();
            final Long durationTimeInMinutes = durationInWeeks * 7 * 24 * 60L;

            final LinearScale<Long> scale = new Infinite60MinutesScale(durationTimeInMinutes);
            final LocalDateTime baseDate = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC);

            final List<Lane<Long>> lanes = buildLanes(shiftsBySpot, baseDate, scale);

            return new RotationViewport(tenantStore.getCurrentTenantId(),
                                        baseDate,
                                        shiftBlobViewPool::get,
                                        scale,
                                        cssGridLinesFactory.newWithSteps(2L, 24L),
                                        ticksFactory.newTicks(scale, 4L, 24L),
                                        lanes);
        });
    }

    private List<Lane<Long>> buildLanes(final Map<Spot, List<Shift>> shiftsBySpot,
                                        final LocalDateTime baseDate,
                                        final LinearScale<Long> scale) {

        return shiftsBySpot.entrySet().stream()
                .sorted(comparing(e -> e.getKey().getName())) //FIXME: Should sorting be done on the ViewportView?
                .map(spot -> newSpotLane(baseDate, scale, spot.getValue(), spot.getKey()))
                .collect(toList());
    }

    private SpotLane newSpotLane(final LocalDateTime baseDate,
                                 final LinearScale<Long> scale,
                                 final List<Shift> shifts,
                                 final Spot spot) {

        final List<SubLane<Long>> subLanes = conflictFreeSubLanesBuilder.buildSubLanes(
                shifts.stream().map(shift -> new ShiftBlob(shift, baseDate, scale)));

        return new SpotLane(spot, subLanes);
    }
}