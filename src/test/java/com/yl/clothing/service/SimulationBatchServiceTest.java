package com.yl.clothing.service;

import com.yl.clothing.entity.Customer;
import com.yl.clothing.enums.OrderStatus;
import com.yl.clothing.mapper.OrderMapper;
import com.yl.clothing.mapper.SimulationMapper;
import com.yl.clothing.mapper.UserMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SimulationBatchServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-26T00:00:00Z"), ZoneId.of("Asia/Shanghai"));

    @Test void paymentAndDeliveryUseExactlyNineSuccessOutcomesAndOneFailure() {
        RandomGenerator random = mock(RandomGenerator.class);
        for (int outcome = 0; outcome < 10; outcome++) {
            when(random.nextInt(10)).thenReturn(outcome);
            assertThat(OrderStatus.PENDING_PAYMENT.next(random))
                    .isEqualTo(outcome < 9 ? OrderStatus.PENDING_SHIPMENT : OrderStatus.CANCELLED);
            assertThat(OrderStatus.SHIPPED.next(random))
                    .isEqualTo(outcome < 9 ? OrderStatus.COMPLETED : OrderStatus.REFUNDED);
        }
        clearInvocations(random);
        assertThat(OrderStatus.PENDING_SHIPMENT.next(random)).isEqualTo(OrderStatus.SHIPPED);
        for (var terminal : List.of(OrderStatus.COMPLETED, OrderStatus.CANCELLED, OrderStatus.REFUNDED))
            assertThat(terminal.next(random)).isEqualTo(terminal);
        verifyNoInteractions(random);
    }

    @Test void phonePrecheckCollisionsAreRetriedBeforeInserting() {
        UserMapper users = mock(UserMapper.class);
        when(users.channelIds()).thenReturn(List.of(1L, 2L, 3L, 4L));
        when(users.phoneExists(anyString())).thenReturn(true, true, false);
        doAnswer(call -> { call.getArgument(0, Customer.class).setId(42L); return null; }).when(users).insertSimulated(any());
        var service = new SimulationBatchService(mock(OrderMapper.class), users, mock(SimulationMapper.class), CLOCK, new Random(87));
        var result = service.generateData(7L, true);
        assertThat(result.usersCreated()).isBetween(1, 2);
        verify(users, times(result.usersCreated() + 2)).phoneExists(anyString());
        var captured = ArgumentCaptor.forClass(Customer.class);
        verify(users, times(result.usersCreated())).insertSimulated(captured.capture());
        captured.getAllValues().forEach(customer -> {
            assertThat(customer.getPhone()).matches("[0-9]{11}");
            assertThat(customer.getSimulationRunId()).isEqualTo(7L);
        });
        verify(users, atLeast(result.usersCreated())).addChannel(eq(42L), anyLong());
    }

    @Test void aPhoneClaimedAfterThePrecheckIsRetried() {
        UserMapper users = mock(UserMapper.class);
        when(users.channelIds()).thenReturn(List.of(1L, 2L, 3L, 4L));
        AtomicInteger attempts = new AtomicInteger();
        doAnswer(call -> {
            if (attempts.incrementAndGet() == 1) throw new DuplicateKeyException("injected phone collision");
            call.getArgument(0, Customer.class).setId(42L);
            return null;
        }).when(users).insertSimulated(any());
        var service = new SimulationBatchService(mock(OrderMapper.class), users, mock(SimulationMapper.class), CLOCK, new Random(87));
        var result = service.generateData(7L, true);
        assertThat(result.usersCreated()).isBetween(1, 2);
        assertThat(attempts.get()).isEqualTo(result.usersCreated() + 1);
    }

    @Test void exhaustedPhoneCandidatesFailAfterABoundedNumberOfAttempts() {
        UserMapper users = mock(UserMapper.class);
        when(users.channelIds()).thenReturn(List.of(1L));
        when(users.phoneExists(anyString())).thenReturn(true);
        var service = new SimulationBatchService(mock(OrderMapper.class), users, mock(SimulationMapper.class), CLOCK, new Random(87));
        assertThatThrownBy(() -> service.generateData(7L, true)).isInstanceOf(IllegalStateException.class);
        verify(users, times(20)).phoneExists(anyString());
        verify(users, never()).insertSimulated(any());
        verify(users, never()).addChannel(anyLong(), anyLong());
    }

    @Test void fourExistingChannelsCanAllBeAssignedWithoutDuplicates() {
        UserMapper users = mock(UserMapper.class);
        when(users.channelIds()).thenReturn(List.of(1L, 2L, 3L, 4L));
        doAnswer(call -> { call.getArgument(0, Customer.class).setId(42L); return null; }).when(users).insertSimulated(any());
        Random random = new Random(87) {
            @Override public int nextInt(int origin, int bound) {
                return origin == 1 && bound == 5 ? 4 : origin;
            }
        };
        var service = new SimulationBatchService(mock(OrderMapper.class), users, mock(SimulationMapper.class), CLOCK, random);
        assertThat(service.generateData(7L, true).usersCreated()).isEqualTo(1);
        var channels = ArgumentCaptor.forClass(Long.class);
        verify(users, times(4)).addChannel(eq(42L), channels.capture());
        assertThat(channels.getAllValues()).containsExactlyInAnyOrder(1L, 2L, 3L, 4L);
    }
}
