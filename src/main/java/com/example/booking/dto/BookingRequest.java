package com.example.booking.dto;

import com.example.booking.model.Booking;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public class BookingRequest {

    @NotBlank(message = "Room name must not be blank")
    private String roomName;

    @NotBlank(message = "Booked by must not be blank")
    private String bookedBy;

    @NotNull(message = "Start date-time must be present")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startAt;

    @NotNull(message = "End date-time must be present")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime endAt;

    @NotBlank(message = "Purpose must not be blank")
    private String purpose;

    public static BookingRequest from(Booking booking) {
        BookingRequest req = new BookingRequest();
        req.setRoomName(booking.getRoomName());
        req.setBookedBy(booking.getBookedBy());
        req.setStartAt(booking.getStartAt());
        req.setEndAt(booking.getEndAt());
        req.setPurpose(booking.getPurpose());
        return req;
    }

    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    public String getBookedBy() { return bookedBy; }
    public void setBookedBy(String bookedBy) { this.bookedBy = bookedBy; }

    public LocalDateTime getStartAt() { return startAt; }
    public void setStartAt(LocalDateTime startAt) { this.startAt = startAt; }

    public LocalDateTime getEndAt() { return endAt; }
    public void setEndAt(LocalDateTime endAt) { this.endAt = endAt; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
}