package com.example.booking.repository;

import com.example.booking.model.Booking;
import com.example.booking.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // 1. Tìm tất cả các booking theo tên phòng và trạng thái (phục vụ kiểm tra trùng giờ)
    List<Booking> findByRoomNameIgnoreCaseAndStatus(String roomName, BookingStatus status);

    // 2. Đếm số booking CONFIRMED hiện có của 1 người (để kiểm tra quy tắc tối đa 2 lịch)
    long countByBookedByIgnoreCaseAndStatus(String bookedBy, BookingStatus status);

    // 3. Đếm số booking CONFIRMED của 1 người nhưng bỏ qua ID hiện tại (khi đang Edit)
    long countByBookedByIgnoreCaseAndStatusAndIdNot(String bookedBy, BookingStatus status, Long id);
}