package com.bikerental.platform.rental.auth.repo;

import com.bikerental.platform.rental.auth.model.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, Long> {

    Optional<Hotel> findByHotelCode(String hotelCode);

    boolean existsByHotelCode(String hotelCode);
}
