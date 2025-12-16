package com.bikerental.platform.rental.rentals.repo;

import com.bikerental.platform.rental.rentals.model.Rental;
import com.bikerental.platform.rental.rentals.model.RentalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Rental entities.
 * All queries are scoped by hotelId for multi-tenancy.
 */
@Repository
public interface RentalRepository extends JpaRepository<Rental, Long> {

    /**
     * Find all rentals for a hotel.
     */
    List<Rental> findByHotelId(Long hotelId);

    /**
     * Find all rentals for a hotel with a specific status.
     */
    List<Rental> findByHotelIdAndStatus(Long hotelId, RentalStatus status);

    /**
     * Find rentals for a hotel with status in a list (e.g., ACTIVE and OVERDUE).
     */
    List<Rental> findByHotelIdAndStatusIn(Long hotelId, List<RentalStatus> statuses);

    /**
     * Find a rental by ID, ensuring it belongs to the given hotel.
     */
    Optional<Rental> findByRentalIdAndHotelId(Long rentalId, Long hotelId);

    /**
     * Count rentals by status for a hotel.
     */
    long countByHotelIdAndStatus(Long hotelId, RentalStatus status);

    /**
     * Find active and overdue rentals for overview, ordered by overdue first then by dueAt.
     */
    @Query("SELECT r FROM Rental r WHERE r.hotelId = :hotelId " +
           "AND r.status IN :statuses " +
           "ORDER BY CASE WHEN r.status = 'OVERDUE' THEN 0 ELSE 1 END, r.dueAt ASC")
    List<Rental> findActiveAndOverdueOrderedByUrgency(
            @Param("hotelId") Long hotelId,
            @Param("statuses") List<RentalStatus> statuses
    );
}

