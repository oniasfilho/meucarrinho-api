package br.com.oniasfilho.meucarrinho.session;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShoppingSessionRepository extends JpaRepository<ShoppingSession, UUID> {

    @Query("select s from ShoppingSession s where s.id = :id and s.user.id = :userId")
    Optional<ShoppingSession> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("select s from ShoppingSession s where s.user.id = :userId order by s.createdAt desc")
    List<ShoppingSession> findAllByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId);

    @Query("""
        select s from ShoppingSession s
        where s.user.id = :userId and s.status = :status
        order by s.createdAt desc
        """)
    List<ShoppingSession> findAllByUserIdAndStatusOrderByCreatedAtDesc(
        @Param("userId") UUID userId,
        @Param("status") ShoppingSessionStatus status
    );

    long countByUser_Id(UUID userId);

    long countByUser_IdAndStatus(UUID userId, ShoppingSessionStatus status);
}

