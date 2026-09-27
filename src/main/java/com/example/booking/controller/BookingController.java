package com.example.booking.controller;

import com.example.booking.dto.BookingRequest;
import com.example.booking.model.Booking;
import com.example.booking.model.BookingStatus;
import com.example.booking.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Duration;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    // 1. Danh sách booking
    @GetMapping
    public String list(Model model) {
        model.addAttribute("bookings", bookingService.findAll());
        return "bookings/list";
    }

    // 2. Mở form tạo mới
    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("booking", new BookingRequest());
        model.addAttribute("bookingId", null);
        return "bookings/form";
    }

    // 3. Xử lý submit tạo mới
    @PostMapping
    public String create(@Valid @ModelAttribute("booking") BookingRequest request,
                         BindingResult result,
                         Model model) {
        // Kiểm tra các quy tắc nghiệp vụ (Booking Policy)
        validateBookingPolicy(null, request, result);

        if (result.hasErrors()) {
            model.addAttribute("bookingId", null);
            return "bookings/form";
        }

        bookingService.create(request);
        return "redirect:/bookings";
    }

    // 4. Mở form chỉnh sửa
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Booking booking = bookingService.findById(id);
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return "redirect:/bookings"; // Không cho sửa booking đã hủy
        }
        model.addAttribute("booking", BookingRequest.from(booking));
        model.addAttribute("bookingId", id);
        return "bookings/form";
    }

    // 5. Xử lý submit cập nhật
    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("booking") BookingRequest request,
                         BindingResult result,
                         Model model) {
        validateBookingPolicy(id, request, result);

        if (result.hasErrors()) {
            model.addAttribute("bookingId", id);
            return "bookings/form";
        }

        bookingService.update(id, request);
        return "redirect:/bookings";
    }

    // 6. Xử lý hủy booking bằng POST
    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            bookingService.cancel(id);
        } catch (IllegalStateException ex) {
            // Nếu không đủ điều kiện hủy (dưới 30 phút), báo lỗi đỏ ở trang list
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/bookings";
    }

    // Hàm kiểm tra các chính sách đặt phòng theo đúng yêu cầu đề bài
    private void validateBookingPolicy(Long currentId, BookingRequest req, BindingResult result) {
        LocalDateTime now = LocalDateTime.now();

        // 1. startAt không được nằm trong quá khứ
        if (req.getStartAt() != null) {
            if (req.getStartAt().isBefore(now)) {
                result.rejectValue("startAt", "error.startAt", "Start date-time must not lie in the past.");
            }
        }

        // 2. Kiểm tra khoảng thời gian giữa startAt và endAt
        if (req.getStartAt() != null && req.getEndAt() != null) {
            // endAt phải sau startAt
            if (!req.getEndAt().isAfter(req.getStartAt())) {
                result.rejectValue("endAt", "error.endAt", "End date-time must be strictly after start date-time.");
            } else {
                // Tối đa 120 phút
                long minutes = Duration.between(req.getStartAt(), req.getEndAt()).toMinutes();
                if (minutes > 120) {
                    result.rejectValue("endAt", "error.endAt", "A single booking may last at most 120 minutes.");
                }
            }

            // 3. Trùng phòng đối với các booking đang CONFIRMED
            if (req.getRoomName() != null && !req.getRoomName().isBlank()) {
                if (bookingService.hasRoomOverlap(currentId, req.getRoomName(), req.getStartAt(), req.getEndAt())) {
                    result.rejectValue("roomName", "error.roomName", "Room is already booked during this time range.");
                }
            }
        }

        // 4. Một người chỉ được giữ tối đa 2 CONFIRMED bookings cùng lúc
        if (req.getBookedBy() != null && !req.getBookedBy().isBlank()) {
            if (bookingService.countActiveBookings(req.getBookedBy(), currentId) >= 2) {
                result.rejectValue("bookedBy", "error.bookedBy", "A person may hold at most two CONFIRMED bookings at a time.");
            }
        }
    }
}