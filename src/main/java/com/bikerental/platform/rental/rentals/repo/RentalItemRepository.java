package com.bikerental.platform.rental.rentals.repo;

import com.bikerental.platform.rental.rentals.model.RentalItem;
import com.bikerental.platform.rental.rentals.model.RentalItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for RentalItem entities.
 * Bike-scoped queries include hotelId for multi-tenant safety.
 */
@Repository
public interface RentalItemRepository extends JpaRepository<RentalItem, Long> {

    /**
     * Find all items for a specific rental.
     */
    List<RentalItem> findByRentalRentalId(Long rentalId);

    /**
     * Check if a bike has an item with a specific status within a hotel.
     * Used for I1 invariant pre-check: existsByHotelIdAndBikeIdAndStatus(hotelId, bikeId, RENTED)
     * Hotel scoping provides defense-in-depth for multi-tenant isolation.
     */
    @Query("SELECT CASE WHEN COUNT(ri) > 0 THEN true ELSE false END FROM RentalItem ri " +
           "WHERE ri.rental.hotelId = :hotelId AND ri.bikeId = :bikeId AND ri.status = :status")
    boolean existsByHotelIdAndBikeIdAndStatus(
            @Param("hotelId") Long hotelId,
            @Param("bikeId") Long bikeId,
            @Param("status") RentalItemStatus status
    );

    /**
     * Find a rental item for a bike with a specific status within a hotel.
     * Hotel scoping ensures multi-tenant isolation.
     */
    @Query("SELECT ri FROM RentalItem ri " +
           "WHERE ri.rental.hotelId = :hotelId AND ri.bikeId = :bikeId AND ri.status = :status")
    Optional<RentalItem> findByHotelIdAndBikeIdAndStatus(
            @Param("hotelId") Long hotelId,
            @Param("bikeId") Long bikeId,
            @Param("status") RentalItemStatus status
    );

    /**
     * Find all items for a bike within a hotel (historical data).
     * Hotel scoping ensures multi-tenant isolation.
     */
    @Query("SELECT ri FROM RentalItem ri " +
           "WHERE ri.rental.hotelId = :hotelId AND ri.bikeId = :bikeId")
    List<RentalItem> findByHotelIdAndBikeId(
            @Param("hotelId") Long hotelId,
            @Param("bikeId") Long bikeId
    );

    /**
     * Count items by status for a rental.
     */
    long countByRentalRentalIdAndStatus(Long rentalId, RentalItemStatus status);

    /**
     * Find all RENTED items for a rental (for return-all operation).
     */
    List<RentalItem> findByRentalRentalIdAndStatus(Long rentalId, RentalItemStatus status);
}

