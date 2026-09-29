package com.aquarium.identity.repository;

import com.aquarium.identity.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, UUID> {

    List<UserAddress> findByUserId(UUID userId);

    /** Địa chỉ mặc định lên đầu, sau đó mới nhất trước. */
    List<UserAddress> findByUserIdOrderByIsDefaultDescCreatedAtDesc(UUID userId);

    /** Luôn lọc theo chủ sở hữu — người khác nhận 404 thay vì sửa được địa chỉ không phải của mình. */
    Optional<UserAddress> findByIdAndUserId(UUID id, UUID userId);

    long countByUserId(UUID userId);

    Optional<UserAddress> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE UserAddress a SET a.isDefault = false WHERE a.userId = :userId AND a.isDefault = true")
    int clearDefault(@Param("userId") UUID userId);
}
