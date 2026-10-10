package com.fitness.agent.repository;

import com.fitness.agent.model.DietRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * 每日饮食记录仓库。
 */
public interface DietRecordRepository extends JpaRepository<DietRecord, Long> {

    List<DietRecord> findByUserIdAndRecordDateOrderByCreatedAtAsc(Long userId, LocalDate recordDate);

    List<DietRecord> findByUserIdOrderByRecordDateDesc(Long userId);
}
