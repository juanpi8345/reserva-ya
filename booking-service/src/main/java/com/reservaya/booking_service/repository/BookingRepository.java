package com.reservaya.booking_service.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.reservaya.booking_service.entity.Booking;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

	@Query("""
			    SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END
			    FROM Booking b
			    WHERE b.resourceId = :resourceId
			      AND b.checkIn < :checkOut
			      AND b.checkOut > :checkIn
				  AND b.status IN ('PENDING', 'CONFIRMED')
			""")
	boolean existsOverlappingBooking(UUID resourceId, LocalDateTime checkIn, LocalDateTime checkOut);

	List<Booking> findByUserId(Long userId);
	Optional<Booking> findByUserIdAndId(Long userId, UUID id);
}
