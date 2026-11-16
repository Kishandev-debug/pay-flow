
package com.payflow.wallet.repository;

import com.payflow.wallet.entity.WalletDeposit;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletDepositRepository
        extends JpaRepository<WalletDeposit, Long> {

    Optional<WalletDeposit> findByProviderAndProviderReference(
            String provider,
            String providerReference
    );

    boolean existsByProviderAndProviderReference(
            String provider,
            String providerReference
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM WalletDeposit d WHERE d.id = :depositId")
    Optional<WalletDeposit> findByIdForUpdate(
            @Param("depositId") Long depositId
    );
}
