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
     */
    @Query("SELECT b FROM Bike b WHERE b.hotelId = :hotelId " +
           "AND (:status IS NULL OR b.status = :status) " +
           "AND (:q IS NULL OR LOWER(b.bikeNumber) LIKE LOWER(CONCAT('%', :q, '%'))) " +
           "ORDER BY b.bikeNumber")
    List<Bike> findByHotelIdWithFilters(
            @Param("hotelId") Long hotelId,
            @Param("status") Bike.BikeStatus status,
            @Param("q") String searchQuery
    );
}

