package com.bank.progress.repository;

import com.bank.progress.domain.HolidayEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HolidayRepository extends JpaRepository<HolidayEntity, Long> {

    boolean existsByHolidayDate(LocalDate date);

    List<HolidayEntity> findByHolidayDateBetween(LocalDate start, LocalDate end);
}
