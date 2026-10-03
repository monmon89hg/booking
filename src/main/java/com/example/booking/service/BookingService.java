package com.example.booking.service;

import com.example.booking.dto.BookingRequest;
import com.example.booking.model.Booking;
import com.example.booking.model.BookingStatus;
import com.example.booking.repository.BookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional // Đảm bảo mọi thao tác với database được bọc trong Transaction
public class BookingService {

    private final BookingRepository bookingRepository;

    // Tiêm (Inject) BookingRepository vào Service
    public BookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    // 1. Lấy tất cả bookings từ database
    @Transactional(readOnly = true)
    public List<Booking> findAll() {
        return bookingRepository.findAll();
    }

    // 2. Tìm 1 booking theo ID
    @Transactional(readOnly = true)
    public Booking findById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + id));
    }

    // 3. Kiểm tra trùng phòng (chỉ xét các booking CONFIRMED)
    @Transactional(readOnly = true)
    public boolean hasRoomOverlap(Long currentId, String roomName, LocalDateTime start, LocalDateTime end) {
        List<Booking> roomBookings = bookingRepository.findByRoomNameIgnoreCaseAndStatus(
                roomName.trim(), 
                BookingStatus.CONFIRMED
        );

        return roomBookings.stream()
                .filter(b -> currentId == null || !b.getId().equals(currentId))
                .anyMatch(b -> start.isBefore(b.getEndAt()) && end.isAfter(b.getStartAt()));
    }

    // 4. Đếm số booking CONFIRMED của 1 người (để kiểm tra quy tắc tối đa 2 lịch)
    @Transactional(readOnly = true)
    public long countActiveBookings(String bookedBy, Long excludeId) {
        if (excludeId == null) {
            return bookingRepository.countByBookedByIgnoreCaseAndStatus(
                    bookedBy.trim(), 
                    BookingStatus.CONFIRMED
            );
        } else {
            return bookingRepository.countByBookedByIgnoreCaseAndStatusAndIdNot(
                    bookedBy.trim(), 
                    BookingStatus.CONFIRMED, 
                    excludeId
            );
        }
    }

    // 5. Tạo mới booking và lưu vào MySQL
    public Booking create(BookingRequest req) {
        Booking booking = new Booking(
                null, // ID để null vì MySQL tự sinh bằng IDENTITY (auto-increment)
                req.getRoomName().trim(),
                req.getBookedBy().trim(),
                req.getStartAt(),
                req.getEndAt(),
                req.getPurpose().trim(),
                BookingStatus.CONFIRMED
        );
        return bookingRepository.save(booking);
    }

    // 6. Cập nhật booking theo ID và lưu vào MySQL
    public Booking update(Long id, BookingRequest req) {
        Booking booking = findById(id);
        booking.setRoomName(req.getRoomName().trim());
        booking.setBookedBy(req.getBookedBy().trim());
        booking.setStartAt(req.getStartAt());
        booking.setEndAt(req.getEndAt());
        booking.setPurpose(req.getPurpose().trim());
        return bookingRepository.save(booking);
    }

    // 7. Hủy booking: kiểm tra điều kiện trước 30 phút, đổi trạng thái thành CANCELLED
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
        bookingRepository.save(booking);
    }
}