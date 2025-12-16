package com.bikerental.platform.rental.rentals.repo;

import com.bikerental.platform.rental.rentals.model.RentalItem;
import com.bikerental.platform.rental.rentals.model.RentalItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for RentalItem entities.
 */
@Repository
public interface RentalItemRepository extends JpaRepository<RentalItem, Long> {

    /**
     * Find all items for a specific rental.
     */
    List<RentalItem> findByRentalRentalId(Long rentalId);

    /**
     * Check if a bike has an item with a specific status.
     * Used for I1 invariant pre-check: existsByBikeIdAndStatus(bikeId, RENTED)
     */
    boolean existsByBikeIdAndStatus(Long bikeId, RentalItemStatus status);

    /**
     * Find a rental item for a bike with a specific status.
     */
    Optional<RentalItem> findByBikeIdAndStatus(Long bikeId, RentalItemStatus status);

    /**
     * Find all items for a bike (historical data).
     */
    List<RentalItem> findByBikeId(Long bikeId);

    /**
     * Count items by status for a rental.
     */
    long countByRentalRentalIdAndStatus(Long rentalId, RentalItemStatus status);

    /**
     * Find all RENTED items for a rental (for return-all operation).
     */
    List<RentalItem> findByRentalRentalIdAndStatus(Long rentalId, RentalItemStatus status);
}

