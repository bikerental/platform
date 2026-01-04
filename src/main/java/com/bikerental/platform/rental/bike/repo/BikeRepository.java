package com.bikerental.platform.rental.bike.repo;

import com.bikerental.platform.rental.bike.model.Bike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BikeRepository extends JpaRepository<Bike, Long> {

    /**
     * Find all bikes for a hotel, optionally filtered by status.
     */
    List<Bike> findByHotelId(Long hotelId);

    /**
     * Find all bikes for a hotel with a specific status.
     */
    List<Bike> findByHotelIdAndStatus(Long hotelId, Bike.BikeStatus status);

    /**
     * Count bikes for a hotel with a specific status.
     */
    long countByHotelIdAndStatus(Long hotelId, Bike.BikeStatus status);

    /**
     * Find a bike by hotel ID and bike number.
     */
    Optional<Bike> findByHotelIdAndBikeNumber(Long hotelId, String bikeNumber);

    /**
     * Find bikes for a hotel, optionally filtered by status and search query (bike number).
     * Search is case-insensitive and matches bike numbers containing the query string.
     * Sorted numerically by bike_number. For OOO bikes, use findOooBikesForExport
     * for proper ooo_since ordering.
     */
    @Query(value = "SELECT * FROM bikes b WHERE b.hotel_id = :hotelId " +
           "AND (:status IS NULL OR b.status = :status) " +
           "AND (:q IS NULL OR LOWER(b.bike_number) LIKE LOWER(CONCAT('%', :q, '%'))) " +
           "ORDER BY CAST(b.bike_number AS UNSIGNED) ASC",
           nativeQuery = true)
    List<Bike> findByHotelIdWithFilters(
            @Param("hotelId") Long hotelId,
            @Param("status") String status,
            @Param("q") String searchQuery
    );

    /**
     * Find bikes for a hotel filtered by OOO status, sorted by ooo_since ASC (oldest first),
     * nulls last, then bike_number ASC. Includes optional search query.
     */
    @Query(value = "SELECT * FROM bikes b WHERE b.hotel_id = :hotelId AND b.status = 'OOO' " +
           "AND (:q IS NULL OR LOWER(b.bike_number) LIKE LOWER(CONCAT('%', :q, '%'))) " +
           "ORDER BY CASE WHEN b.ooo_since IS NULL THEN 1 ELSE 0 END, b.ooo_since ASC, CAST(b.bike_number AS UNSIGNED) ASC",
           nativeQuery = true)
    List<Bike> findOooBikesWithFilters(
            @Param("hotelId") Long hotelId,
            @Param("q") String searchQuery
    );

    /**
     * Find all OOO bikes for a hotel, sorted by ooo_since ASC (oldest first), nulls last, then bike_number ASC.
     * Used for maintenance export.
     */
    @Query(value = "SELECT * FROM bikes b WHERE b.hotel_id = :hotelId AND b.status = 'OOO' " +
           "ORDER BY CASE WHEN b.ooo_since IS NULL THEN 1 ELSE 0 END, b.ooo_since ASC, CAST(b.bike_number AS UNSIGNED) ASC",
           nativeQuery = true)
    List<Bike> findOooBikesForExport(@Param("hotelId") Long hotelId);
}

