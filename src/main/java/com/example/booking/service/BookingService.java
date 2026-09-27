package com.example.booking.service;

import com.example.booking.dto.BookingRequest;
import com.example.booking.model.Booking;
import com.example.booking.model.BookingStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class BookingService {
    private final List<Booking> store = new ArrayList<>();
    private final AtomicLong sequence = new AtomicLong(1);

    public List<Booking> findAll() {
        return new ArrayList<>(store);
    }

    public Booking findById(Long id) {
        return store.stream()
                .filter(b -> b.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + id));
    }

    // Kiểm tra trùng lịch cùng phòng (chỉ xét CONFIRMED, bỏ qua booking đang xét nếu đang Edit)
    public boolean hasRoomOverlap(Long currentId, String roomName, LocalDateTime start, LocalDateTime end) {
        return store.stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .filter(b -> currentId == null || !b.getId().equals(currentId))
                .filter(b -> b.getRoomName().equalsIgnoreCase(roomName.trim()))
                .anyMatch(b -> start.isBefore(b.getEndAt()) && end.isAfter(b.getStartAt()));
    }

    // Đếm số booking CONFIRMED hiện tại của 1 người
    public long countActiveBookings(String bookedBy, Long excludeId) {
        return store.stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .filter(b -> excludeId == null || !b.getId().equals(excludeId))
                .filter(b -> b.getBookedBy().equalsIgnoreCase(bookedBy.trim()))
                .count();
    }

    public Booking create(BookingRequest req) {
        Booking booking = new Booking(
                sequence.getAndIncrement(),
                req.getRoomName().trim(),
                req.getBookedBy().trim(),
                req.getStartAt(),
                req.getEndAt(),
                req.getPurpose().trim(),
                BookingStatus.CONFIRMED
        );
        store.add(booking);
        return booking;
    }

    public Booking update(Long id, BookingRequest req) {
        Booking booking = findById(id);
        booking.setRoomName(req.getRoomName().trim());
        booking.setBookedBy(req.getBookedBy().trim());
        booking.setStartAt(req.getStartAt());
        booking.setEndAt(req.getEndAt());
        booking.setPurpose(req.getPurpose().trim());
        return booking;
    }

    // Hủy booking (Kiểm tra điều kiện trước 30 phút, không xóa record mà chỉ đổi status)
    public void cancel(Long id) {
        Booking booking = findById(id);
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking is already cancelled.");
        }
        LocalDateTime now = LocalDateTime.now();
        if (Duration.between(now, booking.getStartAt()).toMinutes() < 30) {
            throw new IllegalStateException("Booking can only be cancelled at least 30 minutes before start time.");
        }
        booking.setStatus(BookingStatus.CANCELLED);
    }
}